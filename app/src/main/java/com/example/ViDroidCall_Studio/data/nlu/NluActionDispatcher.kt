package com.example.ViDroidCall_Studio.data.nlu

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
import org.json.JSONObject

/**
 * Điều phối các Native Android Actions từ kết quả JSON của NLU (Theo mục 5 trong ANDROID_INTEGRATION_SPEC.md)
 */
class NluActionDispatcher(
    private val context: Context?,
    private val enableAppLaunch: Boolean = false,
    private val onSpeakFeedback: (String) -> Unit = {}
) {
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
                    Log.e(TAG, "Lỗi khi thực thi hành động: ${e.message}")
                }
            }, delayMs)
        }
    }

    fun executeNluResponse(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val intent = json.optString("intent")
            val status = json.optString("status")
            val args = json.optJSONObject("arguments") ?: JSONObject()

            if (status == "needs_clarification") {
                val missing = args.optJSONArray("missing")
                showToast("Bạn vui lòng cung cấp thêm thông tin về $missing")
                speakText("Bạn vui lòng cung cấp thêm thông tin")
                return
            }

            if (status == "invalid") {
                showToast("Thời gian yêu cầu không hợp lệ.")
                speakText("Thời gian bạn yêu cầu không hợp lệ, vui lòng kiểm tra lại.")
                return
            }

            if (status == "unsupported") {
                showToast("Chưa hỗ trợ tính năng này.")
                speakText("Xin lỗi, tôi chưa hỗ trợ tính năng này.")
                return
            }

            when (intent) {
                "set_alarm" -> handleSetAlarm(args)
                "set_timer" -> handleSetTimer(args)
                "call_contact" -> handleCall(args)
                "send_sms" -> handleSendSms(args)
                "open_map" -> handleOpenMap(args)
                "open_app" -> handleOpenApp(args)
                "search_video" -> handleSearchVideo(args)
                "play_music" -> handlePlayMusic(args)
                "greeting" -> handleGreeting()
                "goodbye" -> handleGoodbye()
                else -> speakText("Đã phân tích xong câu lệnh.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xử lý NLU Response: ${e.message}", e)
        }
    }

    private fun handleSetAlarm(args: JSONObject) {
        try {
            val hour = args.optInt("hour")
            val minute = args.optInt("minute")
            val label = args.optString("label", "Báo thức AI")

            showToast("⏰ Đang đặt báo thức lúc $hour:$minute...")
            if (minute > 0) {
                speakText("Đang đặt báo thức lúc $hour giờ $minute phút")
            } else {
                speakText("Đang đặt báo thức lúc $hour giờ")
            }
            
            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_HOUR, hour)
                            putExtra(AlarmClock.EXTRA_MINUTES, minute)
                            putExtra(AlarmClock.EXTRA_MESSAGE, label)
                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context?.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi mở báo thức: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đặt báo thức: ${e.message}")
        }
    }

    private fun handleSetTimer(args: JSONObject) {
        try {
            val duration = args.optInt("duration")
            val unit = args.optString("unit", "minutes")
            val seconds = when (unit) {
                "hours" -> duration * 3600
                "minutes" -> duration * 60
                else -> duration
            }
            val label = args.optString("label", "Hẹn giờ")

            val unitText = when (unit) {
                "hours" -> "giờ"
                "minutes" -> "phút"
                "seconds" -> "giây"
                else -> unit
            }

            showToast("⏳ Đang hẹn giờ $duration $unitText...")
            speakText("Đang hẹn giờ $duration $unitText")
            
            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                            putExtra(AlarmClock.EXTRA_MESSAGE, label)
                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context?.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi mở hẹn giờ: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hẹn giờ: ${e.message}")
        }
    }

    private fun handleSendSms(args: JSONObject) {
        try {
            val contact = args.optString("contact")
            val message = args.optString("message")

            showToast("💬 Đang mở tin nhắn gửi tới: $contact...")
            if (contact.isNotBlank()) {
                speakText("Đang mở ứng dụng gửi tin nhắn tới $contact")
            } else {
                speakText("Đang mở ứng dụng tin nhắn")
            }

            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$contact")
                            putExtra("sms_body", message)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context?.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi gửi SMS: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi SMS: ${e.message}")
        }
    }

    private fun handleCall(args: JSONObject) {
        try {
            val contact = args.optString("contact")
            showToast("📞 Đang mở cuộc gọi tới: $contact...")
            if (contact.isNotBlank()) {
                speakText("Đang thực hiện cuộc gọi tới $contact")
            } else {
                speakText("Đang mở ứng dụng cuộc gọi")
            }

            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$contact")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context?.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi gọi điện: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi điện: ${e.message}")
        }
    }

    private fun handleOpenMap(args: JSONObject) {
        try {
            val destination = args.optString("destination")
            showToast("🗺️ Đang mở bản đồ tới: $destination...")
            if (destination.isNotBlank()) {
                speakText("Đang mở bản đồ chỉ đường tới $destination")
            } else {
                speakText("Đang mở ứng dụng bản đồ")
            }

            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context?.startActivity(mapIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi mở bản đồ: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở bản đồ: ${e.message}")
        }
    }

    private fun handleOpenApp(args: JSONObject) {
        try {
            val rawAppName = args.optString("app_name").trim()
            if (rawAppName.isBlank()) {
                speakText("Xin lỗi, bạn muốn mở ứng dụng nào?")
                return
            }

            val cleanAppName = rawAppName.lowercase()
                .replace("ứng dụng", "")
                .replace("app", "")
                .trim()

            showToast("🚀 Đang mở ứng dụng $rawAppName...")
            speakText("Đang mở ứng dụng $rawAppName")

            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val targetContext = context ?: return@runDelayed

                        // 1. System Intent đặc biệt
                        val systemIntent = when (cleanAppName) {
                            "camera", "máy ảnh", "chụp ảnh" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            "cài đặt", "settings" -> Intent(Settings.ACTION_SETTINGS)
                            "gọi điện", "điện thoại", "danh bạ" -> Intent(Intent.ACTION_DIAL)
                            "tin nhắn", "sms" -> Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_MESSAGING)
                            }
                            else -> null
                        }

                        if (systemIntent != null) {
                            systemIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            if (systemIntent.resolveActivity(targetContext.packageManager) != null) {
                                targetContext.startActivity(systemIntent)
                                return@runDelayed
                            }
                        }

                        // 2. Tra cứu Package Name từ danh sách phổ biến
                        var pkg = when (cleanAppName) {
                            "zalo" -> "com.zing.zalo"
                            "facebook" -> "com.facebook.katana"
                            "youtube" -> "com.google.android.youtube"
                            "tiktok" -> "com.ss.android.ugc.trill"
                            "chrome" -> "com.android.chrome"
                            "shopee" -> "com.shopee.vn"
                            "messenger" -> "com.facebook.orca"
                            "instagram" -> "com.instagram.android"
                            "telegram" -> "org.telegram.messenger"
                            "viber" -> "com.viber.voip"
                            "lazada" -> "com.lazada.android"
                            "momo" -> "com.mservice.momopay"
                            "spotify" -> "com.spotify.music"
                            else -> null
                        }

                        // 3. Tra cứu động qua PackageManager theo tên hiển thị của App đã cài đặt trên thiết bị
                        if (pkg == null) {
                            pkg = findPackageByLabel(targetContext, cleanAppName)
                        }

                        if (pkg != null) {
                            val launchIntent = targetContext.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            if (launchIntent != null) {
                                targetContext.startActivity(launchIntent)
                            } else {
                                speakText("Không thể mở ứng dụng $rawAppName")
                            }
                        } else {
                            speakText("Không tìm thấy ứng dụng $rawAppName trên thiết bị")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi mở app: ${e.message}")
                        speakText("Có lỗi xảy ra khi mở ứng dụng $rawAppName")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở ứng dụng: ${e.message}")
        }
    }

    private fun findPackageByLabel(context: Context, cleanAppName: String): String? {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            
            var bestMatch: String? = null
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                if (label == cleanAppName) {
                    return info.activityInfo.packageName
                }
                if (label.contains(cleanAppName) || cleanAppName.contains(label)) {
                    bestMatch = info.activityInfo.packageName
                }
            }
            bestMatch
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tìm kiếm package theo tên: ${e.message}")
            null
        }
    }

    private fun handleSearchVideo(args: JSONObject) {
        try {
            val query = args.optString("query").trim()
            if (query.isBlank()) {
                speakText("Xin lỗi, bạn muốn tìm video gì trên YouTube?")
                return
            }

            showToast("🎬 Đang tìm video: $query...")
            speakText("Đang tìm video $query trên YouTube")

            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val targetContext = context ?: return@runDelayed
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))).apply {
                            setPackage("com.google.android.youtube")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (webIntent.resolveActivity(targetContext.packageManager) != null) {
                            targetContext.startActivity(webIntent)
                        } else {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            targetContext.startActivity(browserIntent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi tìm video YouTube: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xử lý tìm video: ${e.message}")
        }
    }

    private fun handlePlayMusic(args: JSONObject) {
        try {
            val songName = args.optString("song_name").trim()
            val artist = args.optString("artist").trim()
            val genre = args.optString("genre").trim()

            val musicQuery = buildString {
                if (songName.isNotBlank()) append(songName)
                if (artist.isNotBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(artist)
                }
                if (genre.isNotBlank() && isBlank()) append(genre)
            }.trim()

            if (musicQuery.isNotBlank()) {
                showToast("🎵 Đang phát: $musicQuery...")
                speakText("Đang phát $musicQuery")
            } else {
                showToast("🎵 Đang mở trình phát nhạc...")
                speakText("Đang mở trình phát nhạc")
            }

            if (enableAppLaunch) {
                runDelayed(800) {
                    try {
                        val targetContext = context ?: return@runDelayed
                        if (musicQuery.isNotBlank()) {
                            val searchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
                                putExtra(MediaStore.EXTRA_MEDIA_TITLE, songName)
                                putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
                                putExtra(android.app.SearchManager.QUERY, musicQuery)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            if (searchIntent.resolveActivity(targetContext.packageManager) != null) {
                                targetContext.startActivity(searchIntent)
                                return@runDelayed
                            }
                        }

                        val musicAppIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_MUSIC)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (musicAppIntent.resolveActivity(targetContext.packageManager) != null) {
                            targetContext.startActivity(musicAppIntent)
                        } else {
                            val youtubeMusicIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(musicQuery.ifBlank { "nhạc tuyển chọn" }))).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            targetContext.startActivity(youtubeMusicIntent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi phát nhạc: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xử lý phát nhạc: ${e.message}")
        }
    }

    private fun handleGreeting() {
        showToast("👋 Xin chào! Tôi có thể giúp gì cho bạn?")
        speakText("Xin chào bạn, tôi có thể giúp gì cho bạn?")
    }

    private fun handleGoodbye() {
        showToast("👋 Tạm biệt và hẹn gặp lại!")
        speakText("Tạm biệt bạn, hẹn gặp lại nhé!")
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
            // Ignore Android Log stub error in plain JVM unit tests
        }
        runOnMainThread {
            onSpeakFeedback(text)
        }
    }

    companion object {
        private const val TAG = "NluActionDispatcher"
    }
}
