# 📱 TÀI LIỆU KỸ THUẬT TÍCH HỢP MÔ HÌNH NLU GGUF VÀO ANDROID
> **Dành cho:** Kỹ sư Android & Trợ lý AI phát triển ứng dụng di động  
> **Model:** `qwen2.5-1.5b-nlu-q8_0.gguf` (Định dạng chuẩn `llama.cpp` / GGUF)

---

## 📌 1. THÔNG SỐ KỸ THUẬT MÔ HÌNH (MODEL SPECIFICATIONS)

* **Tên mô hình:** `Qwen2.5-1.5B-NLU-Q8_0`
* **Kiến trúc gốc:** `Qwen2ForCausalLM` (1.54 tỷ tham số)
* **Loại nén (Quantization):** `Q8_0` (8-bit Quantized)
* **Kích thước file:** `~1.64 GB` (Chạy mượt trên điện thoại Android từ 4GB RAM trở lên)
* **Vị trí file:** `models/qwen2.5-1.5b-nlu-q8_0.gguf`
* **Định dạng Chat Template:** `ChatML` (`<|im_start|>...<|im_end|>`)
* **Thời gian suy luận (Inference Time):** ~200ms - 400ms trên chip Snapdragon / MediaTek.

---

## 🧠 2. SYSTEM PROMPT BẮT BUỘC (MANDATORY SYSTEM PROMPT)

Khi gọi mô hình trong App Android, **BẮT BUỘC** phải truyền `System Prompt` cố định sau:

```text
Bạn là bộ phân tích NLU trích xuất ý định (intent) và tham số (arguments). Các intent hỗ trợ: [set_alarm, set_timer, open_app, open_map, call_contact, send_sms, clarify, unsupported]. Chỉ trả về JSON duy nhất: {"intent": string, "arguments": object, "risk_level": "low"|"medium"|"high", "status": "success"|"needs_clarification"|"invalid"|"unsupported", "requires_confirmation": boolean}.
```

---

## 📐 3. ĐẶC TẢ JSON ĐẦU RA (OUTPUT SCHEMA SPECIFICATION)

Mô hình đảm bảo chỉ trả về chuỗi JSON thuần túy có cấu trúc:

```json
{
  "intent": "string",
  "arguments": { ... },
  "risk_level": "low" | "medium" | "high",
  "status": "success" | "needs_clarification" | "invalid" | "unsupported",
  "requires_confirmation": boolean
}
```

### Chi tiết 8 Intent và Cấu trúc Tham số:

| # | Intent | Arguments Schema | Trạng thái (Status) & Hành vi Android |
| :--- | :--- | :--- | :--- |
| **1** | `set_alarm` | `hour` (int 0-23), `minute` (int 0-59), `date` (string, opt), `label` (string, opt) | • `success`: Giờ hợp lệ $\rightarrow$ Tạo báo thức.<br>• `invalid`: Giờ $\ge 24$ hoặc phút $\ge 60$ $\rightarrow$ Báo lỗi giờ không hợp lệ. |
| **2** | `set_timer` | `duration` (int > 0), `unit` ("seconds" \| "minutes" \| "hours"), `label` (string, opt) | • `success` $\rightarrow$ Khởi động bộ đếm ngược. |
| **3** | `open_map` | `destination` (string) | • `success` $\rightarrow$ Mở Google Maps điều hướng. |
| **4** | `open_app` | `app_name` (string) | • `success` $\rightarrow$ Mở Package ứng dụng tương ứng. |
| **5** | `call_contact`| `contact` (string: tên danh bạ hoặc số điện thoại) | • `risk_level: "high"` nếu là 113, 114, 115.<br>• `requires_confirmation: true` $\rightarrow$ Hiện xác nhận trước khi quay số. |
| **6** | `send_sms` | `contact` (string), `message` (string: nội dung trích xuất sạch) | • `requires_confirmation: true` $\rightarrow$ Hiện popup xác nhận gửi tin nhắn. |
| **7** | `clarify` | `missing`: mảng các tham số bị thiếu `["contact"]`, `["message"]`, `["hour"]`, `["duration"]` | • `status: "needs_clarification"` $\rightarrow$ Trợ lý hỏi lại người dùng phần còn thiếu. |
| **8** | `unsupported`| `{}` | • `status: "unsupported"` $\rightarrow$ Thông báo lệnh nằm ngoài phạm vi hỗ trợ. |

---

## 🤖 4. MẪU DỮ LIỆU ĐẦU VÀO VÀ ĐẦU RA (FEW-SHOT EXAMPLES)

### Ví dụ 1: Gửi tin nhắn (send_sms)
* **User Input:** `"Nhắn tin cho mẹ là con đang về rồi"`
* **Output:**
```json
{
  "intent": "send_sms",
  "arguments": {
    "contact": "mẹ",
    "message": "con đang về rồi"
  },
  "risk_level": "medium",
  "status": "success",
  "requires_confirmation": true
}
```

### Ví dụ 2: Thiếu thông tin (clarify)
* **User Input:** `"Nhắn tin cho bác Nam"`
* **Output:**
```json
{
  "intent": "clarify",
  "arguments": {
    "missing": ["message"]
  },
  "risk_level": "medium",
  "status": "needs_clarification",
  "requires_confirmation": false
}
```

### Ví dụ 3: Báo thức sai giờ (invalid status)
* **User Input:** `"Cài báo thức lúc 25 giờ 70 phút"`
* **Output:**
```json
{
  "intent": "set_alarm",
  "arguments": {
    "hour": 25,
    "minute": 70
  },
  "risk_level": "low",
  "status": "invalid",
  "requires_confirmation": false
}
```

---

## ☕ 5. HƯỚNG DẪN CODE KOTLIN MAPPING TRÊN ANDROID (ANDROID DISPATCHER)

```kotlin
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import org.json.JSONObject

class NluActionDispatcher(private val context: Context) {

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
            e.printStackTrace()
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
        // Mapping tên app sang Package Name thực tế trên Android
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
        // Tích hợp Text-To-Speech (TTS) phát âm thanh phản hồi cho người dùng
        println("🔊 TTS Phản hồi: $text")
    }
}
```

---

## 📋 6. PROMPT DÀNH CHO BẠN KHI CHAT VỚI AI TRONG PROJECT ANDROID:

> *💡 Bạn chỉ cần copy đoạn dưới đây dán vào phiên làm việc tiếp theo với AI khi code Android:*

```text
Tôi đã có sẵn file mô hình AI NLU định dạng GGUF (qwen2.5-1.5b-nlu-q8_0.gguf) chạy offline thông qua llama.cpp trên Android.

Mô hình này nhận câu nói tiếng Việt và trả về duy nhất chuỗi JSON NLU gồm 8 Intent:
1. set_alarm (hour, minute, date, label)
2. set_timer (duration, unit, label)
3. open_map (destination)
4. open_app (app_name)
5. call_contact (contact)
6. send_sms (contact, message)
7. clarify (missing)
8. unsupported ()

Hãy hỗ trợ tôi:
1. Cấu hình thư viện llama.cpp trên Android Studio (CMake / JNI hoặc Android Binding).
2. Viết Service chạy ngầm nạp file .gguf và hàm nhận diện văn bản -> thực thi Native Android Action (báo thức, gọi điện, nhắn tin, bản đồ, mở app).
```
