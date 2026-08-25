package com.example.ViDroidCall_Studio.data.nlu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.util.Log
import android.widget.Toast
import org.json.JSONObject

/**
 * Điều phối các Native Android Actions từ kết quả JSON của NLU (Theo mục 5 trong ANDROID_INTEGRATION_SPEC.md)
 */
class NluActionDispatcher(
    private val context: Context,
    private val onSpeakFeedback: (String) -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun executeNluResponse(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val intent = json.optString("intent")
            val status = json.optString("status")
            val args = json.optJSONObject("arguments") ?: JSONObject()

            if (status == "needs_clarification") {
                val missing = args.optJSONArray("missing")
                showToast("Bạn vui lòng cung cấp thêm thông tin về $missing")
                speakText("Bạn vui lòng cung cấp thêm thông tin về $missing")
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
            
            mainHandler.postDelayed({
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, label)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi mở báo thức: ${e.message}")
                }
            }, 800)
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

            showToast("⏳ Đang hẹn giờ $duration $unit...")
            
            mainHandler.postDelayed({
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, label)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi mở hẹn giờ: ${e.message}")
                }
            }, 800)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hẹn giờ: ${e.message}")
        }
    }

    private fun handleSendSms(args: JSONObject) {
        try {
            val contact = args.optString("contact")
            val message = args.optString("message")

            showToast("💬 Đang mở tin nhắn gửi tới: $contact...")

            mainHandler.postDelayed({
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$contact")
                        putExtra("sms_body", message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi gửi SMS: ${e.message}")
                }
            }, 800)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi SMS: ${e.message}")
        }
    }

    private fun handleCall(args: JSONObject) {
        try {
            val contact = args.optString("contact")
            showToast("📞 Đang mở cuộc gọi tới: $contact...")

            mainHandler.postDelayed({
                try {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$contact")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi gọi điện: ${e.message}")
                }
            }, 800)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi điện: ${e.message}")
        }
    }

    private fun handleOpenMap(args: JSONObject) {
        try {
            val destination = args.optString("destination")
            showToast("🗺️ Đang mở bản đồ tới: $destination...")

            mainHandler.postDelayed({
                try {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(mapIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi mở bản đồ: ${e.message}")
                }
            }, 800)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở bản đồ: ${e.message}")
        }
    }

    private fun handleOpenApp(args: JSONObject) {
        try {
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
                showToast("🚀 Đang mở ứng dụng $appName...")
                mainHandler.postDelayed({
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi mở app: ${e.message}")
                    }
                }, 800)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở ứng dụng: ${e.message}")
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
        mainHandler.post {
            try {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun speakText(text: String) {
        Log.d(TAG, "🔊 TTS: $text")
        onSpeakFeedback(text)
    }

    companion object {
        private const val TAG = "NluActionDispatcher"
    }
}
