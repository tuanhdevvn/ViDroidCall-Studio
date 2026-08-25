# 📑 ĐẶC TẢ SCHEMA NLU CHUẨN 10 INTENT
### Hệ Thống Trợ Lý Ảo Tiếng Việt Đa Vùng Miền & Người Cao Tuổi (ViDroidCall Studio)

---

## I. CẤU TRÚC JSON ĐẦU RA TỔNG QUAN

Mọi câu lệnh sau khi đi qua mô hình AI (On-Device SLM) đều được chuẩn hóa thành một đối tượng JSON duy nhất với **5 trường dữ liệu bất biến**:

```json
{
  "intent": "set_alarm",
  "arguments": {},
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

### Ý nghĩa các trường:
* **`intent`** *(string)*: Ý định chính của người dùng (1 trong 10 intent).
* **`arguments`** *(object)*: Các tham số trích xuất được từ câu lệnh.
* **`risk_level`** *(enum)*: Mức độ rủi ro (`low` = an toàn, `medium` = thay đổi dữ liệu/gửi tin nhắn, `high` = khẩn cấp/nguy hiểm).
* **`status`** *(enum)*: Trạng thái thực thi (`success` = thành công, `needs_clarification` = thiếu dữ liệu, `invalid` = dữ liệu sai quy chuẩn, `unsupported` = ngoài phạm vi).
* **`requires_confirmation`** *(boolean)*: Có bắt buộc app hiển thị hộp thoại xác nhận trước khi thực hiện hay không (`true`/`false`).

---

## II. BẢNG TỔNG HỢP MA TRẬN 10 INTENT

| STT | Intent | Mục đích tác vụ | Tham số (`arguments`) | Rủi ro | Xác nhận | Hành vi Android Native |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **1** | `set_alarm` | Cài đặt báo thức | `hour`, `minute`, `label` | `low` | `false` | Gọi `AlarmClock.ACTION_SET_ALARM` |
| **2** | `set_timer` | Hẹn giờ đếm ngược | `duration`, `unit`, `label` | `low` | `false` | Gọi `AlarmClock.ACTION_SET_TIMER` |
| **3** | `open_map` | Mở bản đồ / Chỉ đường | `destination` | `low` | `false` | Mở Google Maps theo địa điểm |
| **4** | `open_app` | Mở ứng dụng trên máy | `app_name` | `low` | `false` | Khởi chạy App Package tương ứng |
| **5** | `call_contact` | Gọi điện thoại | `contact` | `low` / `high` | **`true`** | Gọi số khẩn cấp (`113, 114, 115`) hoặc mở bàn phím gọi |
| **6** | `send_sms` | Soạn / Gửi tin nhắn SMS | `contact`, `message` *(tùy chọn)* | `medium` | **`true`** | Mở ứng dụng SMS và paste sẵn nội dung (nếu có) |
| **7** | `search_video` | Tìm kiếm video YouTube | `query` | `low` | `false` | Mở YouTube App / URL tìm kiếm |
| **8** | `play_music` | Phát nhạc / Bài hát | `song_name`, `artist`, `genre` | `low` | `false` | Điều khiển phát nhạc qua MediaStore/Zing/Spotify |
| **9** | `clarify` | Yêu cầu làm rõ thông tin | `missing` *(danh sách trường thiếu)* | `low` | `false` | Trợ lý hỏi lại người dùng câu cụ thể |
| **10** | `unsupported` | Ngoài phạm vi hỗ trợ | `{}` | `low` / `high` | `false` | Phản hồi từ chối an toàn / giải thích |

---

## III. CHI TIẾT SCHEMA & VÍ DỤ TỪNG INTENT

---

### 1. `set_alarm` (Cài đặt báo thức)
* **Ý nghĩa:** Đặt chuông báo thức theo giờ và phút.
* **Arguments:**
  * `hour`: Số nguyên `0-23` *(Bắt buộc)*
  * `minute`: Số nguyên `0-59` *(Mặc định là 0 nếu không nói phút)*
  * `label`: Chuỗi tên nhãn báo thức *(Tùy chọn)*

*Ví dụ câu lệnh: "Cháu ơi đặt giúp bác báo thức 5 rưỡi sáng mai đi tập dưỡng sinh ngoài bờ hồ"*
```json
{
  "intent": "set_alarm",
  "arguments": {
    "hour": 5,
    "minute": 30,
    "label": "đi tập dưỡng sinh ngoài bờ hồ"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

---

### 2. `set_timer` (Hẹn giờ đếm ngược)
* **Ý nghĩa:** Hẹn giờ nấu ăn, sắc thuốc, tưới cây, tập thể dục...
* **Arguments:**
  * `duration`: Số nguyên thời lượng *(Bắt buộc)*
  * `unit`: `"seconds"` | `"minutes"` | `"hours"` *(Bắt buộc)*
  * `label`: Chuỗi nội dung hẹn giờ *(Tùy chọn)*

*Ví dụ câu lệnh: "Hẹn giờ 45 phút nhắc bà tắt nồi cá kho tộ"*
```json
{
  "intent": "set_timer",
  "arguments": {
    "duration": 45,
    "unit": "minutes",
    "label": "tắt nồi cá kho tộ"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

---

### 3. `open_map` (Mở bản đồ / Chỉ đường)
* **Ý nghĩa:** Tìm đường đi đến địa chỉ, bệnh viện, nhà chùa, bến xe...
* **Arguments:**
  * `destination`: Chuỗi địa điểm cần tới *(Bắt buộc)*

*Ví dụ câu lệnh: "Chỉ đường đến Bệnh viện Quân Y 108 giúp tôi"*
```json
{
  "intent": "open_map",
  "arguments": {
    "destination": "Bệnh viện Quân Y 108"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

---

### 4. `open_app` (Mở ứng dụng)
* **Ý nghĩa:** Khởi chạy các ứng dụng cài đặt trên thiết bị (Zalo, VNeID, VssID, Camera...).
* **Arguments:**
  * `app_name`: Tên ứng dụng *(Bắt buộc)*

*Ví dụ câu lệnh: "Mở app VNeID để bác xuất trình căn cước"*
```json
{
  "intent": "open_app",
  "arguments": {
    "app_name": "VNeID"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

---

### 5. `call_contact` (Gọi điện thoại)
* **Ý nghĩa:** Thực hiện cuộc gọi tới danh bạ người thân hoặc số cứu trợ khẩn cấp.
* **Arguments:**
  * `contact`: Tên người nhận hoặc số điện thoại *(Bắt buộc)*
* **Lưu ý Rủi ro:** Danh bạ thường $\rightarrow$ `risk_level: "low"`; Số khẩn cấp (`113, 114, 115, 111, 911`) $\rightarrow$ `risk_level: "high"`. Luôn yêu cầu `requires_confirmation: true`.

*Ví dụ 1 (Danh bạ thường): "Gọi điện thoại cho anh Hai ở quê"*
```json
{
  "intent": "call_contact",
  "arguments": {
    "contact": "anh Hai ở quê"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": true
}
```

*Ví dụ 2 (Số khẩn cấp): "Gọi ngay 115 bác bị đau ngực quá"*
```json
{
  "intent": "call_contact",
  "arguments": {
    "contact": "115"
  },
  "risk_level": "high",
  "status": "success",
  "requires_confirmation": true
}
```

---

### 6. `send_sms` (Gửi tin nhắn SMS)
* **Ý nghĩa:** Soạn tin nhắn đến người nhận trong danh bạ.
* **Arguments:**
  * `contact`: Tên người nhận *(Bắt buộc)*
  * `message`: Nội dung tin nhắn *(Chỉ xuất hiện khi người dùng có đọc nội dung)*

*Trường hợp 1 (Có cả người nhận và nội dung): "Nhắn cho con dâu nhớ mua hoa cúc về cúng rằm"*
```json
{
  "intent": "send_sms",
  "arguments": {
    "contact": "con dâu",
    "message": "nhớ mua hoa cúc về cúng rằm"
  },
  "risk_level": "medium",
  "status": "success",
  "requires_confirmation": true
}
```

*Trường hợp 2 (Chỉ có người nhận): "Nhắn tin cho Nam"*
```json
{
  "intent": "send_sms",
  "arguments": {
    "contact": "Nam"
  },
  "risk_level": "medium",
  "status": "success",
  "requires_confirmation": true
}
```

---

### 7. `search_video` (Tìm kiếm video YouTube)
* **Ý nghĩa:** Tìm kiếm các video giải trí, cải lương, tin tức, bài giảng Phật pháp, dạy nấu ăn trên YouTube.
* **Arguments:**
  * `query`: Từ khóa nội dung video *(Đã được làm sạch khỏi từ đệm thừa)*

*Ví dụ câu lệnh: "Tìm video trích đoạn cải lương Lan và Điệp trên YouTube"*
```json
{
  "intent": "search_video",
  "arguments": {
    "query": "trích đoạn cải lương Lan và Điệp"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

---

### 8. `play_music` (Phát nhạc / Bài hát)
* **Ý nghĩa:** Phát nhạc theo tên bài hát, nghệ sĩ hoặc thể loại qua các app âm nhạc.
* **Arguments:**
  * `song_name`: Tên bài hát *(Tùy chọn)*
  * `artist`: Tên ca sĩ/nhạc sĩ *(Tùy chọn)*
  * `genre`: Thể loại nhạc *(Tùy chọn)*

*Ví dụ 1 (Có tên bài & ca sĩ): "Mở bài hát Diễm Xưa của Khánh Ly"*
```json
{
  "intent": "play_music",
  "arguments": {
    "song_name": "Diễm Xưa",
    "artist": "Khánh Ly"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

*Ví dụ 2 (Theo thể loại): "Bật liên khúc nhạc vàng bolero không lời"*
```json
{
  "intent": "play_music",
  "arguments": {
    "genre": "liên khúc nhạc vàng bolero không lời"
  },
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

---

### 9. `clarify` (Yêu cầu làm rõ thông tin)
* **Ý nghĩa:** Kích hoạt khi câu lệnh của người dùng bị thiếu đối tượng thực thi bắt buộc (câu nói cụt lủn).
* **Arguments:**
  * `missing`: Mảng chứa các trường còn thiếu (`["contact" | "query" | "destination" | "app_name" | "song_name" | "hour" | "duration"]`)
  * `status`: Luôn là `"needs_clarification"`

*Ví dụ 1: "Gọi điện thoại" -> Thiếu người nhận*
```json
{
  "intent": "clarify",
  "arguments": {
    "missing": ["contact"]
  },
  "risk_level": "low",
  "status": "needs_clarification",
  "requires_confirmation": false
}
```

*Ví dụ 2: "Đặt báo thức cho tôi" -> Thiếu giờ/phút*
```json
{
  "intent": "clarify",
  "arguments": {
    "missing": ["hour", "minute"]
  },
  "risk_level": "low",
  "status": "needs_clarification",
  "requires_confirmation": false
}
```

---

### 10. `unsupported` (Ngoài phạm vi hỗ trợ / Từ chối an toàn)
* **Ý nghĩa:** Nhận diện và từ chối các yêu cầu ngoài phạm vi (hỏi thời tiết, chuyển tiền, hỏi đáp kiến thức, tác vụ nguy hiểm), giúp mô hình **hoàn toàn không bị ảo giác (hallucination)**.
* **Arguments:** `{}`
* **Status:** Luôn là `"unsupported"`

*Ví dụ 1 (Chém gió / Thời tiết): "Hôm nay Hà Nội có mưa không cháu"*
```json
{
  "intent": "unsupported",
  "arguments": {},
  "risk_level": "low",
  "status": "unsupported",
  "requires_confirmation": false
}
```

*Ví dụ 2 (Tác vụ tài chính nguy hiểm): "Chuyển cho Nam 5 triệu đồng qua Vietcombank"*
```json
{
  "intent": "unsupported",
  "arguments": {},
  "risk_level": "high",
  "status": "unsupported",
  "requires_confirmation": false
}
```

---

## IV. CÁC NGUYÊN TẮC KỸ THUẬT QUAN TRỌNG

1. **Chuẩn hóa Kiểu Dữ Liệu:**
   - Các trường số (`hour`, `minute`, `duration`) **bắt buộc là số nguyên (Integer)**, không để dạng chuỗi `"15"`.
   - `missing` trong `clarify` **bắt buộc luôn là Mảng (Array of String)**.
2. **Loại bỏ Key Rỗng (Zero Empty Key):**
   - Chỉ xuất các key có dữ liệu thực tế (không bao giờ sinh `"message": ""` hay `"artist": null`).
3. **Bảo Mật & An Toàn Người Dùng:**
   - Mọi hành vi gửi tin nhắn (`send_sms`) và gọi điện (`call_contact`) đều được gán `requires_confirmation: true` để tránh người cao tuổi bị gọi nhầm hoặc vô tình gửi tin nhắn.
