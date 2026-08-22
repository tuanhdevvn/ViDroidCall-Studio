package com.example.emma_vidroidcall.data.nlu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import org.json.JSONObject

/**
 * Điều phối các Native Android Actions từ kết quả JSON của NLU (Theo mục 5 trong ANDROID_INTEGRATION_SPEC.md)
 */
class NluActionDispatcher(
    private val context: Context,
    private val onSpeakFeedback: (String) -> Unit = {}
) {

    fun executeNluResponse(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val intent = json.optString("intent")
            val status = json.optString("status")
            val args = json.optJSONObject("arguments") ?: JSONObject()

            if (status == "needs_clarification") {
                val missing = args.optJSONArray("missing")
                speakText("Bạn vui lòng cung cấp thêm thông tin về $missing")
                return
            }

            if (status == "invalid") {
                speakText("Thời gian bạn yêu cầu không hợp lệ, vui lòng kiểm tra lại.")
                return
            }

            if (status == "unsupported") {
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
                else -> speakText("Không nhận diện được yêu cầu.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xử lý NLU Response: ${e.message}", e)
        }
    }

    private fun handleSetAlarm(args: JSONObject) {
        val hour = args.optInt("hour")
        val minute = args.optInt("minute")
        val label = args.optString("label", "Báo thức AI")

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleSetTimer(args: JSONObject) {
        val duration = args.optInt("duration")
        val unit = args.optString("unit", "minutes")
        val seconds = when (unit) {
            "hours" -> duration * 3600
            "minutes" -> duration * 60
            else -> duration
        }
        val label = args.optString("label", "Hẹn giờ")

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleSendSms(args: JSONObject) {
        val contact = args.optString("contact")
        val message = args.optString("message")

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$contact")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleCall(args: JSONObject) {
        val contact = args.optString("contact")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$contact")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleOpenMap(args: JSONObject) {
        val destination = args.optString("destination")
        val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(mapIntent)
    }

    private fun handleOpenApp(args: JSONObject) {
        val appName = args.optString("app_name")
        val pkg = when (appName.lowercase()) {
            "zalo" -> "com.zing.zalo"
            "facebook" -> "com.facebook.katana"
            "youtube" -> "com.google.android.youtube"
            "tiktok" -> "com.ss.android.ugc.trill"
            "chrome" -> "com.android.chrome"
            else -> null
        }
        if (pkg != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
    }

    private fun speakText(text: String) {
        Log.d(TAG, "🔊 TTS Phản hồi: $text")
        onSpeakFeedback(text)
    }

    companion object {
        private const val TAG = "NluActionDispatcher"
    }
}
