package com.example.ViDroidCall_Studio.data.nlu

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.util.AppResolver
import org.json.JSONObject

/**
 * Điều phối và thực thi các Native Android Actions dựa trên kết quả JSON NLU
 */
class NluActionDispatcher(
    private val context: Context?,
    private val enableAppLaunch: Boolean = true,
    private val onActionError: (String) -> Unit = {},
    private val onSpeakFeedback: (String) -> Unit = {}
) {
    // Secondary constructor for backwards compatibility with tests and callers
    constructor(
        context: Context?,
        enableAppLaunch: Boolean = true,
        onSpeakFeedback: (String) -> Unit
    ) : this(context, enableAppLaunch, {}, onSpeakFeedback)
    private val mainHandler by lazy {
        try {
            Handler(Looper.getMainLooper())
        } catch (e: Exception) {
            null
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        val handler = mainHandler
        if (handler != null) {
            handler.post(action)
        } else {
            action()
        }
    }

    private fun runDelayed(delayMs: Long, action: () -> Unit) {
        val handler = mainHandler
        if (handler != null) {
            handler.postDelayed({
                try {
                    action()
                } catch (e: Exception) {
                    Log.e(TAG, "ACTION_FAILED Lỗi khi thực thi hành động trễ: ${e.message}")
                }
            }, delayMs)
        }
    }

    /**
     * Phân tích và thực thi trực tiếp từ chuỗi JSON NLU
     */
    fun executeNluResponse(jsonString: String) {
        val nluResult = NluJsonParser.parse(jsonString)
        val action = NativeAction.fromNluResult(nluResult)
        dispatch(action)
    }

    /**
     * Xử lý điều phối hành động NativeAction
     */
    fun dispatch(action: NativeAction) {
        logLifecycle("ACTION_DISPATCH", "intent=${action.intentName}, requiresConfirmation=${action.requiresConfirmation}")

        // 1. Phát phản hồi giọng nói (TTS)
        val speech = action.getSpeechFeedbackText()
        if (speech.isNotBlank()) {
            speakText(speech)
        }

        // 2. Kiểm tra nếu là hành động thông tin/chưa hỗ trợ
        when (action) {
            is NativeAction.Informational -> {
                showToast(action.message)
                return
            }
            is NativeAction.Unsupported -> {
                showToast(action.message)
                return
            }
            else -> {}
        }

        // 3. Nếu không kích hoạt mở app ngoài (chế độ preview/test)
        if (!enableAppLaunch) {
            return
        }

        // 4. Nếu hành động yêu cầu xác nhận -> Ghi log (UI sẽ hiển thị dialog)
        if (action.requiresConfirmation) {
            logLifecycle("ACTION_CONFIRM_REQUIRED", "intent=${action.intentName}")
            return
        }

        // 5. Nếu hành động an toàn không cần xác nhận -> Chờ 800ms rồi thực thi
        runDelayed(800) {
            executeNativeAction(action)
        }
    }

    /**
     * Thực thi trực tiếp hành động Native (sau khi đã delay hoặc sau khi người dùng nhấn Xác nhận)
     */
    fun executeNativeAction(action: NativeAction) {
        val targetContext = context ?: run {
            logLifecycle("ACTION_FAILED", "intent=${action.intentName}, error=Context is null")
            return
        }

        logLifecycle("ACTION_EXECUTING", "intent=${action.intentName}")

        try {
            when (action) {
                is NativeAction.OpenApp -> executeOpenApp(targetContext, action)
                is NativeAction.CallContact -> executeCallContact(targetContext, action)
                is NativeAction.SendSms -> executeSendSms(targetContext, action)
                is NativeAction.OpenMap -> executeOpenMap(targetContext, action)
                is NativeAction.SetAlarm -> executeSetAlarm(targetContext, action)
                is NativeAction.SetTimer -> executeSetTimer(targetContext, action)
                is NativeAction.SearchVideo -> executeSearchVideo(targetContext, action)
                is NativeAction.PlayMusic -> executePlayMusic(targetContext, action)
                else -> {
                    logLifecycle("ACTION_SUCCESS", "intent=${action.intentName}")
                }
            }
        } catch (e: ActivityNotFoundException) {
            val errorMsg = "Thiết bị không có ứng dụng phù hợp để thực hiện hành động này."
            logLifecycle("ACTION_FAILED", "intent=${action.intentName}, error=ActivityNotFoundException: ${e.message}")
            showToast(errorMsg)
            onActionError(errorMsg)
        } catch (e: SecurityException) {
            val errorMsg = "Ứng dụng chưa được cấp quyền thực hiện hành động này."
            logLifecycle("ACTION_FAILED", "intent=${action.intentName}, error=SecurityException: ${e.message}")
            showToast(errorMsg)
            onActionError(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Có lỗi xảy ra khi thực hiện hành động: ${e.localizedMessage}"
            logLifecycle("ACTION_FAILED", "intent=${action.intentName}, error=${e.message}")
            showToast(errorMsg)
            onActionError(errorMsg)
        }
    }

    private fun executeOpenApp(context: Context, action: NativeAction.OpenApp) {
        val cleanName = AppResolver.cleanAppName(action.appName)
        if (cleanName.isBlank()) {
            val error = "Vui lòng chỉ định tên ứng dụng cần mở."
            showToast(error)
            onActionError(error)
            return
        }
        val displayAppName = AppResolver.getDisplayAppName(action.appName)

        showToast("🚀 Đang mở ứng dụng $displayAppName...")
        speakText("Đang mở ứng dụng $displayAppName")

        // 1. Kiểm tra System Intent trước
        val systemIntent = AppResolver.resolveSystemIntent(cleanName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (systemIntent != null && systemIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(systemIntent)
            logLifecycle("ACTION_SUCCESS", "intent=open_app, type=system_intent")
            return
        }

        // 2. Tra cứu package name đã cài đặt
        val packageName = AppResolver.resolvePackageName(context, action.appName)
        if (packageName != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                logLifecycle("ACTION_SUCCESS", "intent=open_app, package=$packageName")
                return
            }
        }

        val notFoundMsg = "Không tìm thấy ứng dụng ${action.appName} trên thiết bị."
        showToast(notFoundMsg)
        speakText("Không tìm thấy ứng dụng ${action.appName} trên thiết bị")
        onActionError(notFoundMsg)
        logLifecycle("ACTION_FAILED", "intent=open_app, error=App not installed: ${action.appName}")
    }

    private fun executeCallContact(context: Context, action: NativeAction.CallContact) {
        val target = if (action.phoneNumber.isNotBlank()) action.phoneNumber else action.contact
        if (target.isBlank()) {
            val error = "Không tìm thấy thông tin số điện thoại để thực hiện cuộc gọi."
            showToast(error)
            speakText("Vui lòng cung cấp số điện thoại hoặc tên liên hệ")
            onActionError(error)
            return
        }

        showToast("📞 Đang mở cuộc gọi tới: $target...")
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(target)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialIntent)
        logLifecycle("ACTION_SUCCESS", "intent=call_contact, target=$target")
    }

    private fun executeSendSms(context: Context, action: NativeAction.SendSms) {
        val target = if (action.phoneNumber.isNotBlank()) action.phoneNumber else action.contact
        showToast("💬 Đang mở tin nhắn gửi tới: $target...")

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(target)}")
            putExtra("sms_body", action.message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(smsIntent)
        logLifecycle("ACTION_SUCCESS", "intent=send_sms, target=$target")
    }

    private fun executeOpenMap(context: Context, action: NativeAction.OpenMap) {
        if (action.destination.isBlank()) {
            val error = "Vui lòng cung cấp địa điểm bạn muốn tìm trên bản đồ."
            showToast(error)
            onActionError(error)
            return
        }

        showToast("🗺️ Đang mở bản đồ tới: ${action.destination}...")
        val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(action.destination))

        val googleMapsIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (googleMapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(googleMapsIntent)
            logLifecycle("ACTION_SUCCESS", "intent=open_map, provider=google_maps")
        } else {
            val genericMapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (genericMapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(genericMapIntent)
                logLifecycle("ACTION_SUCCESS", "intent=open_map, provider=generic_map")
            } else {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(action.destination))
                val webIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                logLifecycle("ACTION_SUCCESS", "intent=open_map, provider=browser")
            }
        }
    }

    private fun executeSetAlarm(context: Context, action: NativeAction.SetAlarm) {
        if (action.hour < 0 || action.hour > 23 || action.minute < 0 || action.minute > 59) {
            val error = "Thời gian đặt báo thức không hợp lệ (${action.hour}:${action.minute})."
            showToast(error)
            speakText("Thời gian đặt báo thức không hợp lệ")
            onActionError(error)
            return
        }

        showToast("⏰ Đang đặt báo thức lúc ${action.hour}:${action.minute}...")
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (alarmIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(alarmIntent)
            logLifecycle("ACTION_SUCCESS", "intent=set_alarm, time=${action.hour}:${action.minute}")
        } else {
            val errorMsg = "Thiết bị không có ứng dụng phù hợp để thực hiện hành động này."
            logLifecycle("ACTION_FAILED", "intent=set_alarm, error=No activity found to handle ACTION_SET_ALARM")
            showToast(errorMsg)
            onActionError(errorMsg)
        }
    }

    private fun executeSetTimer(context: Context, action: NativeAction.SetTimer) {
        if (action.durationSeconds <= 0) {
            val error = "Thời lượng hẹn giờ không hợp lệ."
            showToast(error)
            speakText("Thời lượng hẹn giờ không hợp lệ")
            onActionError(error)
            return
        }

        showToast("⏳ Đang hẹn giờ ${action.displayDuration} ${action.unitText}...")
        val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, action.durationSeconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (timerIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(timerIntent)
            logLifecycle("ACTION_SUCCESS", "intent=set_timer, duration=${action.durationSeconds}s")
        } else {
            val errorMsg = "Thiết bị không có ứng dụng phù hợp để thực hiện hành động này."
            logLifecycle("ACTION_FAILED", "intent=set_timer, error=No activity found to handle ACTION_SET_TIMER")
            showToast(errorMsg)
            onActionError(errorMsg)
        }
    }

    private fun executeSearchVideo(context: Context, action: NativeAction.SearchVideo) {
        if (action.query.isBlank()) return
        showToast("🎬 Đang tìm video: ${action.query}...")

        val queryUri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(action.query))
        val ytIntent = Intent(Intent.ACTION_VIEW, queryUri).apply {
            setPackage("com.google.android.youtube")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (ytIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(ytIntent)
            logLifecycle("ACTION_SUCCESS", "intent=search_video, app=youtube")
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, queryUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            logLifecycle("ACTION_SUCCESS", "intent=search_video, app=browser")
        }
    }

    private fun executePlayMusic(context: Context, action: NativeAction.PlayMusic) {
        showToast("🎵 Đang mở trình phát nhạc...")
        if (action.musicQuery.isNotBlank()) {
            val searchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
                if (action.songName.isNotBlank()) putExtra(MediaStore.EXTRA_MEDIA_TITLE, action.songName)
                if (action.artist.isNotBlank()) putExtra(MediaStore.EXTRA_MEDIA_ARTIST, action.artist)
                putExtra(android.app.SearchManager.QUERY, action.musicQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
                logLifecycle("ACTION_SUCCESS", "intent=play_music, type=media_search")
                return
            }
        }

        val musicAppIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MUSIC)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (musicAppIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(musicAppIntent)
            logLifecycle("ACTION_SUCCESS", "intent=play_music, type=music_app")
        } else {
            val query = action.musicQuery.ifBlank { "nhạc tuyển chọn" }
            val ytMusicUri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
            val ytMusicIntent = Intent(Intent.ACTION_VIEW, ytMusicUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(ytMusicIntent)
            logLifecycle("ACTION_SUCCESS", "intent=play_music, type=youtube_fallback")
        }
    }

    private fun showToast(msg: String) {
        val targetContext = context ?: return
        runOnMainThread {
            try {
                Toast.makeText(targetContext, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun speakText(text: String) {
        try {
            Log.d(TAG, "🔊 TTS: $text")
        } catch (e: Throwable) {
            // Ignore Android Log stub in JVM unit test
        }
        runOnMainThread {
            onSpeakFeedback(text)
        }
    }

    private fun logLifecycle(event: String, details: String) {
        try {
            Log.i(TAG, "$event $details")
        } catch (e: Throwable) {
            // Ignore in pure JVM unit tests
        }
    }

    companion object {
        private const val TAG = "NluActionDispatcher"
    }
}
