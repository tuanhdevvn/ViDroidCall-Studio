# 📚 BÁCH KHOA TOÀN THƯ HƯỚNG DẪN SINH DỮ LIỆU HUẤN LUYỆN NLU
### (Dành cho Trợ lý Ảo Tiếng Việt Đa Vùng Miền & Người Cao Tuổi - ViDroidCall Studio)

---

## 1. HỆ THỐNG INTENT & SCHEMA QUY CHUẨN (BẤT BIẾN)

### 1.1. System Prompt Dùng Chung (Bắt buộc cho mọi mẫu Fine-tune):
```text
Bạn là bộ phân tích NLU trích xuất ý định (intent) và tham số (arguments). Các intent hỗ trợ: [set_alarm, set_timer, open_app, open_map, call_contact, send_sms, search_video, play_music, clarify, unsupported]. Chỉ trả về JSON duy nhất: {"intent": string, "arguments": object, "risk_level": "low"|"medium"|"high", "status": "success"|"needs_clarification"|"invalid"|"unsupported", "requires_confirmation": boolean}.
```

---

### 1.2. Bảng Ma Trận Quy Định `risk_level`, `status` & `requires_confirmation`:

| Intent | Ngữ cảnh / Tình huống | `risk_level` | `status` | `requires_confirmation` | Ghi chú & Hành vi Android |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **`set_alarm`** | Giờ hợp lệ (0-23h, 0-59p) | `"low"` | `"success"` | `false` | Đặt báo thức hệ thống qua AlarmClock |
| **`set_alarm`** | Giờ vượt ngưỡng (25h, 70p) | `"low"` | `"invalid"` | `false` | Thông báo giọng nói thời gian không hợp lệ |
| **`set_timer`** | Hẹn giờ đếm ngược | `"low"` | `"success"` | `false` | Bắt đầu bộ đếm giờ hệ thống |
| **`open_map`** | Tìm đường, mở bản đồ | `"low"` | `"success"` | `false` | Mở Google Maps theo destination |
| **`open_app`** | Mở ứng dụng | `"low"` | `"success"` | `false` | Mở package app cài đặt trên máy |
| **`search_video`** | Tìm kiếm video YouTube | `"low"` | `"success"` | `false` | Mở app YouTube hoặc web tìm kiếm |
| **`play_music`** | Phát bài hát, thể loại nhạc | `"low"` | `"success"` | `false` | Phát nhạc qua MediaStore/Zing/Spotify |
| **`call_contact`** | Danh bạ người thân, bạn bè | `"low"` | `"success"` | `true` | Cần người dùng xác nhận trước khi gọi |
| **`call_contact`** | Số cứu trợ khẩn cấp (`113, 114, 115, 111, 911`) | `"high"` | `"success"` | `true` | Ưu tiên cao, gọi cấp cứu/cứu hỏa/công an |
| **`send_sms`** | Có người nhận (có/chưa có nội dung) | `"medium"` | `"success"` | `true` | Mở màn hình SMS sẵn sàng soạn/gửi tin |
| **`clarify`** | Câu cộc lốc thiếu thông tin bắt buộc | `"low"` | `"needs_clarification"` | `false` | Hỏi lại để bổ sung trường còn thiếu |
| **`unsupported`** | Hỏi thời tiết, trò chuyện, tra cứu | `"low"` | `"unsupported"` | `false` | Thông báo chưa hỗ trợ tính năng này |
| **`unsupported`** | Chuyển khoản, xóa dữ liệu, tắt máy | `"high"` | `"unsupported"` | `false` | Từ chối thực hiện tác vụ nguy hiểm |

---

### 1.3. Chi tiết Arguments & Ràng buộc kiểu dữ liệu của 10 Intent:

1. **`set_alarm`**:
   - `arguments`: `{"hour": int (0-23), "minute": int (0-59), "label": string (nếu có)}`
   - *Lưu ý:* Tuyệt đối không dùng trường `date`. `hour` và `minute` bắt buộc là Integer thuần túy (không bọc trong dấu ngoặc kép).

2. **`set_timer`**:
   - `arguments`: `{"duration": int, "unit": "seconds"|"minutes"|"hours", "label": string (nếu có)}`
   - *Lưu ý:* `duration` bắt buộc là Integer, `unit` chỉ nhận đúng 1 trong 3 giá trị tiếng Anh: `"seconds"`, `"minutes"`, `"hours"`.

3. **`open_map`**:
   - `arguments`: `{"destination": string}`

4. **`open_app`**:
   - `arguments`: `{"app_name": string}`

5. **`call_contact`**:
   - `arguments`: `{"contact": string}`

6. **`send_sms`**:
   - `arguments`: `{"contact": string, "message": string (tùy chọn)}`
     - *Có cả người nhận và nội dung*: `{"contact": "Nam", "message": "mai tôi qua"}`
     - *Chỉ có người nhận*: `{"contact": "Nam"}` *(Không sinh trường message nếu người dùng chưa nói nội dung)*

7. **`search_video`**:
   - `arguments`: `{"query": string}`

8. **`play_music`**:
   - `arguments`: `{"song_name": string, "artist": string, "genre": string}` *(chỉ xuất các key có thông tin)*
     - *Có tên bài hát & ca sĩ*: `{"song_name": "Diễm Xưa", "artist": "Khánh Ly"}`
     - *Chỉ có tên bài hát*: `{"song_name": "Lạc Trôi"}`
     - *Theo thể loại*: `{"genre": "nhạc bolero"}`
     - *Phát nhạc chung chung*: `{}`

9. **`clarify`**:
   - `arguments`: `{"missing": ["contact" | "query" | "destination" | "app_name" | "song_name" | "hour" | "duration"]}`
   - *Lưu ý:* Trường `missing` **bắt buộc luôn là Mảng (Array of String)**.

10. **`unsupported`**:
    - `arguments`: `{}`

---

### 1.4. Bộ 5 Nguyên Tắc Vàng Chống Schema Drifting (Trôi dạt định dạng):
1. **Ép kiểu Số nguyên (Integer):** `hour`, `minute`, `duration` phải là số nguyên, không để dạng chuỗi `"15"`.
2. **Không xuất Key rỗng:** Chỉ xuất các key có dữ liệu thực tế. Tuyệt đối không xuất `"message": ""` hay `"artist": null`.
3. **Mảng missing cố định:** Trường `missing` luôn là mảng string `["query"]`, không được trả về `"query"`.
4. **Lọc sạch Entity (Slot Cleaning):** Trích xuất tên người, địa điểm, từ khóa sạch sẽ, cắt bỏ toàn bộ filler words và kính ngữ thừa (*"Bác muốn tìm video dạy làm bánh chưng"* $\rightarrow$ `query: "cách làm bánh chưng"`).
5. **JSON Valid 100%:** Chỉ dùng dấu ngoặc kép chuẩn `""`, không có trailing comma, không markdown tick trong JSON output.

---

### 1.5. Quy Tắc Chuẩn Hóa Thời Gian & Đại Lượng (Time & Unit Normalization):

| Câu nói tiếng Việt | Trích xuất chuẩn | Giải thích |
| :--- | :--- | :--- |
| *"6 giờ sáng"*, *"6h"* | `{hour: 6, minute: 0}` | Giờ sáng hệ 24h |
| *"7 rưỡi tối"*, *"7 giờ 30 tối"* | `{hour: 19, minute: 30}` | 7h + 12 = 19h |
| *"10 giờ đêm"*, *"22h"* | `{hour: 22, minute: 0}` | 10h + 12 = 22h |
| *"8 giờ kém 15 sáng"* | `{hour: 7, minute: 45}` | Giờ kém buổi sáng |
| *"10 giờ kém 20 tối"* | `{hour: 21, minute: 40}` | Giờ kém buổi tối |
| *"nửa tiếng"* | `{duration: 30, unit: "minutes"}` | 30 phút |
| *"tiếng rưỡi"*, *"1 giờ rưỡi"* | `{duration: 90, unit: "minutes"}` | Quy về phút cho đếm giờ chuẩn |
| *"45 giây"* | `{duration: 45, unit: "seconds"}` | Đơn vị giây |
| *"2 tiếng"*, *"2 giờ đồng hồ"* | `{duration: 2, unit: "hours"}` | Đơn vị giờ |

---

## 2. BỘ KỸ NĂNG SINH DỮ LIỆU CHUYÊN SÂU THEO NGỮ CẢNH (12 DOMAINS)

---

### 🏮 KỸ NĂNG 1: Tín Ngưỡng, Phong Tục, Giỗ Chạp & Lễ Tết Truyền Thống
* *"Cháu ơi đặt giúp bác báo thức 4 rưỡi sáng mai đi lễ Chùa Hương"* $\rightarrow$ `set_alarm` {hour: 4, minute: 30, label: "đi lễ Chùa Hương"}
* *"Chỉ đường đến Đền Hùng Phú Thọ giùm tui"* $\rightarrow$ `open_map` {destination: "Đền Hùng Phú Thọ"}
* *"Hẹn giờ 30 phút nhắc bà tắt tuần nhang thắp hương"* $\rightarrow$ `set_timer` {duration: 30, unit: "minutes", label: "tắt tuần nhang thắp hương"}
* *"Chỉ đường đến Nhà thờ Đức Bà"* $\rightarrow$ `open_map` {destination: "Nhà thờ Đức Bà"}
* *"Nhắn cho con dâu nhớ mua hoa cúc và nải chuối về cúng rằm"* $\rightarrow$ `send_sms` {contact: "con dâu", message: "nhớ mua hoa cúc và nải chuối về cúng rằm"}
* *"Nhắn tin cho thím Bảy mai sang nhà ăn giỗ cụ"* $\rightarrow$ `send_sms` {contact: "thím Bảy", message: "mai sang nhà ăn giỗ cụ"}
* *"Báo thức 5 giờ sáng đi chợ mua thịt gà làm cỗ giỗ"* $\rightarrow$ `set_alarm` {hour: 5, minute: 0, label: "đi chợ mua thịt gà làm cỗ giỗ"}
* *"Gọi điện thoại cho anh Hai ở quê chúc Tết"* $\rightarrow$ `call_contact` {contact: "anh Hai ở quê"}
* *"Chỉ đường ra Nghĩa trang Liệt sĩ để bà đi viếng mộ"* $\rightarrow$ `open_map` {destination: "Nghĩa trang Liệt sĩ"}
* *"Hẹn 8 tiếng nhắc trông nồi bánh chưng"* $\rightarrow$ `set_timer` {duration: 8, unit: "hours", label: "trông nồi bánh chưng"}

---

### 💊 KỸ NĂNG 2: Sức Khỏe Tuổi Già, Thuốc Men, Bệnh Viện & Dưỡng Sinh
* *"Hẹn 30 phút nhắc bác uống thuốc huyết áp sau ăn"* $\rightarrow$ `set_timer` {duration: 30, unit: "minutes", label: "uống thuốc huyết áp sau ăn"}
* *"Hẹn giờ 1 tiếng nữa nhắc bà đo lại đường huyết"* $\rightarrow$ `set_timer` {duration: 1, unit: "hours", label: "đo lại đường huyết"}
* *"Đặt báo thức 8 giờ tối uống thuốc bổ xương khớp"* $\rightarrow$ `set_alarm` {hour: 20, minute: 0, label: "uống thuốc bổ xương khớp"}
* *"Hẹn 20 phút nhắc nhỏ thuốc mắt"* $\rightarrow$ `set_timer` {duration: 20, unit: "minutes", label: "nhỏ thuốc mắt"}
* *"Đặt chuông 5 giờ sáng mai để bà dậy đi tập dưỡng sinh ngoài bờ hồ"* $\rightarrow$ `set_alarm` {hour: 5, minute: 0, label: "đi tập dưỡng sinh ngoài bờ hồ"}
* *"Bấm giờ 45 phút đi bộ công viên"* $\rightarrow$ `set_timer` {duration: 45, unit: "minutes", label: "đi bộ công viên"}
* *"Gọi điện cho Bác sĩ Hùng viện lão khoa"* $\rightarrow$ `call_contact` {contact: "Bác sĩ Hùng viện lão khoa"}
* *"Mở bản đồ chỉ đường tới Bệnh viện Quân Y 108"* $\rightarrow$ `open_map` {destination: "Bệnh viện Quân Y 108"}
* *"Chỉ đường ra Trạm y tế phường Giảng Võ"* $\rightarrow$ `open_map` {destination: "Trạm y tế phường Giảng Võ"}

---

### 🍲 KỸ NĂNG 3: Nấu Nướng Dân Dã, Sắc Thuốc Bắc & Công Việc Nội Trợ
* *"Hẹn 2 tiếng nhắc tôi tắt bếp sắc thuốc Bắc"* $\rightarrow$ `set_timer` {duration: 2, unit: "hours", label: "tắt bếp sắc thuốc Bắc"}
* *"Bấm giờ 90 phút canh ấm thuốc nam"* $\rightarrow$ `set_timer` {duration: 90, unit: "minutes", label: "canh ấm thuốc nam"}
* *"Hẹn giờ 45 phút nhắc tắt nồi cá kho tộ"* $\rightarrow$ `set_timer` {duration: 45, unit: "minutes", label: "tắt nồi cá kho tộ"}
* *"Bấm giờ 20 phút luộc gà cúng"* $\rightarrow$ `set_timer` {duration: 20, unit: "minutes", label: "luộc gà cúng"}
* *"Hẹn 3 tiếng ninh nồi nước dùng bún bò"* $\rightarrow$ `set_timer` {duration: 3, unit: "hours", label: "ninh nồi nước dùng bún bò"}
* *"Hẹn 15 phút canh nước sôi pha trà mạn"* $\rightarrow$ `set_timer` {duration: 15, unit: "minutes", label: "canh nước sôi pha trà mạn"}

---

### 🌾 KỸ NĂNG 4: Đời Sống Làng Quê, Nông Nghiệp & Mùa Vụ
* *"Hẹn 3 tiếng nữa nhắc ra tắt máy bơm nước ruộng"* $\rightarrow$ `set_timer` {duration: 3, unit: "hours", label: "ra tắt máy bơm nước ruộng"}
* *"Đặt báo thức 4 giờ sáng mai dậy đi gặt lúa sớm"* $\rightarrow$ `set_alarm` {hour: 4, minute: 0, label: "đi gặt lúa sớm"}
* *"Hẹn giờ 1 tiếng nhắc cào thóc ngoài sân phơi kẻo mưa"* $\rightarrow$ `set_timer` {duration: 1, unit: "hours", label: "cào thóc ngoài sân phơi"}
* *"Nhắn cho chú Bảy mượn cái máy cắt cỏ"* $\rightarrow$ `send_sms` {contact: "chú Bảy", message: "mượn cái máy cắt cỏ"}
* *"Chỉ đường ra bến đò Cồn Khương"* $\rightarrow$ `open_map` {destination: "bến đò Cồn Khương"}

---

### 👶 KỸ NĂNG 5: Chăm Sóc Cháu Nhỏ & Gia Đình Đa Thế Hệ
* *"Hẹn 15 phút nhắc tắt nồi cháo sườn cho cu Tí"* $\rightarrow$ `set_timer` {duration: 15, unit: "minutes", label: "tắt nồi cháo sườn cho cu Tí"}
* *"Đặt báo thức 4 rưỡi chiều đi đón cháu ngoại ở trường mầm non"* $\rightarrow$ `set_alarm` {hour: 16, minute: 30, label: "đi đón cháu ngoại ở trường mầm non"}
* *"Nhắn cho mẹ cu Bin là chiều nay ông đón cháu rồi nhé"* $\rightarrow$ `send_sms` {contact: "mẹ cu Bin", message: "chiều nay ông đón cháu rồi nhé"}
* *"Bật app Zing MP3 mở bài hát ru Bắc Bộ cho cháu ngủ"* $\rightarrow$ `open_app` {app_name: "Zing MP3"}

---

### 🏛️ KỸ NĂNG 6: Thủ Tục Hành Chính, Lương Hưu & Hội Đoàn Địa Phương
* *"Đặt báo thức 7h sáng mùng 5 đi lĩnh lương hưu ở ủy ban xã"* $\rightarrow$ `set_alarm` {hour: 7, minute: 0, label: "đi lĩnh lương hưu ở ủy ban xã"}
* *"Chỉ đường đến Bảo hiểm Xã hội quận Đống Đa"* $\rightarrow$ `open_map` {destination: "Bảo hiểm Xã hội quận Đống Đa"}
* *"Mở ứng dụng VssID tra cứu thẻ bảo hiểm"* $\rightarrow$ `open_app` {app_name: "VssID"}
* *"Mở app VNeID để bác xuất trình căn cước"* $\rightarrow$ `open_app` {app_name: "VNeID"}
* *"Bấm máy gọi cho ông Tổ trưởng dân phố"* $\rightarrow$ `call_contact` {contact: "ông Tổ trưởng dân phố"}
* *"Nhắn cho cô tổ phó dân phố tối nay bác bận không đi họp được"* $\rightarrow$ `send_sms` {contact: "cô tổ phó dân phố", message: "tối nay bác bận không đi họp được"}
* *"Gọi cho chú Năm bạn cờ tướng"* $\rightarrow$ `call_contact` {contact: "chú Năm bạn cờ tướng"}

---

### 🚨 KỸ NĂNG 7: Tình Huống Khẩn Cấp, Sơ Cứu & Trợ Giúp Nhanh
* *"Gọi ngay 115 bác bị đau ngực quá"* $\rightarrow$ `call_contact` {contact: "115"}, `risk_level: "high"`, `requires_confirmation: true`
* *"Bấm máy gọi 114 có cháy ở đầu ngõ"* $\rightarrow$ `call_contact` {contact: "114"}, `risk_level: "high"`, `requires_confirmation: true`
* *"Gọi công an 113 giúp tôi"* $\rightarrow$ `call_contact` {contact: "113"}, `risk_level: "high"`, `requires_confirmation: true`
* *"Gọi điện thoại gấp cho con trai"* $\rightarrow$ `call_contact` {contact: "con trai"}, `risk_level: "low"`, `requires_confirmation: true`
* *"Cháu ơi nhắn tin cấp cứu cho con gái là mẹ bị ngã"* $\rightarrow$ `send_sms` {contact: "con gái", message: "mẹ bị ngã"}, `risk_level: "medium"`, `requires_confirmation: true`

---

### 🎬 KỸ NĂNG 8: Mở & Tìm Kiếm Video trên YouTube (search_video)
* *"Mở YouTube tìm hài Hoài Linh Chí Tài"* $\rightarrow$ `search_video` {query: "hài Hoài Linh Chí Tài"}
* *"Tìm video trích đoạn cải lương Lan và Điệp trên YouTube"* $\rightarrow$ `search_video` {query: "trích đoạn cải lương Lan và Điệp"}
* *"Bật YouTube tìm phim hoạt hình Tom và Jerry cho cháu"* $\rightarrow$ `search_video` {query: "phim hoạt hình Tom và Jerry"}
* *"Mở video hướng dẫn cắm hoa bàn thờ ngày Tết"* $\rightarrow$ `search_video` {query: "hướng dẫn cắm hoa bàn thờ ngày Tết"}
* *"Tìm video highlights trận bóng đá Việt Nam hôm qua trên YouTube"* $\rightarrow$ `search_video` {query: "highlights trận bóng đá Việt Nam hôm qua"}
* *"Mở YouTube xem thời sự VTV1 19 giờ hôm nay"* $\rightarrow$ `search_video` {query: "thời sự VTV1 19 giờ hôm nay"}
* *"Tìm video bài tập dịch cân kinh chữa đau lưng"* $\rightarrow$ `search_video` {query: "bài tập dịch cân kinh chữa đau lưng"}
* *"Mở YouTube bài giảng Thầy Thích Trúc Thái Minh"* $\rightarrow$ `search_video` {query: "bài giảng Thầy Thích Trúc Thái Minh"}
* *"Tìm video dạy cách làm bánh chưng ngày Tết"* $\rightarrow$ `search_video` {query: "dạy cách làm bánh chưng ngày Tết"}

---

### 🎵 KỸ NĂNG 9: Thưởng Thức Âm Nhạc & Phát Bài Hát (play_music)
* *"Mở bài hát Diễm Xưa của Khánh Ly"* $\rightarrow$ `play_music` {song_name: "Diễm Xưa", artist: "Khánh Ly"}
* *"Bật bài Cắt Đôi Nỗi Sầu"* $\rightarrow$ `play_music` {song_name: "Cắt Đôi Nỗi Sầu"}
* *"Phát bài hát Nối Lại Tình Xưa ca sĩ Như Quỳnh"* $\rightarrow$ `play_music` {song_name: "Nối Lại Tình Xưa", artist: "Như Quỳnh"}
* *"Bật cho bà bài Lòng Mẹ"* $\rightarrow` `play_music` {song_name: "Lòng Mẹ"}
* *"Mở giúp bác tuyển tập nhạc Trịnh Công Sơn"* $\rightarrow` `play_music` {genre: "nhạc Trịnh Công Sơn"}
* *"Bật liên khúc nhạc vàng bolero không lời"* $\rightarrow` `play_music` {genre: "liên khúc nhạc vàng bolero không lời"}
* *"Phát nhạc ru em bé ngủ"* $\rightarrow` `play_music` {genre: "nhạc ru em bé ngủ"}
* *"Bật nhạc nhẹ thư giãn dưỡng sinh"* $\rightarrow` `play_music` {genre: "nhạc nhẹ thư giãn dưỡng sinh"}
* *"Mở nhạc cách mạng tiền chiến hào hùng"* $\rightarrow` `play_music` {genre: "nhạc cách mạng tiền chiến hào hùng"}
* *"Mở nhạc lên"* / *"Bật nhạc đi cháu"* $\rightarrow` `play_music` {}

---

### 📻 KỸ NĂNG 10: Ứng Dụng Khác, Nghe Đài & Báo Chí (open_app)
* *"Bật ứng dụng VTV Go mở kênh VTV1"* $\rightarrow` `open_app` {app_name: "VTV Go"}
* *"Mở cái app Radio Đài Tiếng nói Việt Nam cho bà"* $\rightarrow` `open_app` {app_name: "Radio"}
* *"Vào ứng dụng Báo Mới đọc tin tức buổi sáng"* $\rightarrow` `open_app` {app_name: "Báo Mới"}
* *"Mở Zalo cho bác"* $\rightarrow` `open_app` {app_name: "Zalo"}
* *"Bật máy ảnh chụp kiểu ảnh"* $\rightarrow` `open_app` {app_name: "máy ảnh"}

---

### ❓ KỸ NĂNG 11: Yêu Cầu Làm Rõ Khi Thiếu Thông Tin (clarify)
Chỉ kích hoạt khi câu lệnh hoàn toàn thiếu đối tượng thực thi:
* *"Gọi điện thoại cho tôi"* / *"Bấm máy gọi đi"* $\rightarrow$ `clarify` {missing: ["contact"]}
* *"Nhắn tin đi"* / *"Gửi tin nhắn"* $\rightarrow$ `clarify` {missing: ["contact"]}
* *"Tìm video trên YouTube"* / *"Mở video"* $\rightarrow$ `clarify` {missing: ["query"]}
* *"Chỉ đường"* / *"Tìm đường đi"* $\rightarrow$ `clarify` {missing: ["destination"]}
* *"Mở ứng dụng"* / *"Bật app lên"* $\rightarrow$ `clarify` {missing: ["app_name"]}
* *"Đặt báo thức"* / *"Cài báo thức"* $\rightarrow$ `clarify` {missing: ["hour", "minute"]}
* *"Hẹn giờ giúp tôi"* / *"Bấm giờ đếm ngược"* $\rightarrow$ `clarify` {missing: ["duration", "unit"]}

---

### 🚫 KỸ NĂNG 12: Nhận Diện Tình Huống Ngoài Phạm Vi & Từ Chối An Toàn (unsupported)
Cực kỳ quan trọng để chống Hallucination (ảo giác) của mô hình:
* **Hỏi thời tiết & Kiến thức đời sống:**
  * *"Hôm nay Hà Nội có mưa không cháu"* $\rightarrow$ `unsupported` {}, `risk_level: "low"`
  * *"Thủ đô của nước Pháp là thành phố nào"* $\rightarrow$ `unsupported` {}, `risk_level: "low"`
  * *"Giải phương trình bậc hai x bình cộng 2x trừ 3 bằng 0"* $\rightarrow$ `unsupported` {}, `risk_level: "low"`
* **Trò chuyện & Chém gió:**
  * *"Kể cho bà nghe một câu chuyện cười"* $\rightarrow$ `unsupported` {}, `risk_level: "low"`
  * *"Bạn có người yêu chưa"* $\rightarrow$ `unsupported` {}, `risk_level: "low"`
* **Tác vụ Tài chính & Phần cứng Nguy hiểm:**
  * *"Chuyển cho Nam 5 triệu đồng qua Vietcombank"* $\rightarrow$ `unsupported` {}, `risk_level: "high"`
  * *"Xóa hết toàn bộ ảnh và dữ liệu trong máy"* $\rightarrow$ `unsupported` {}, `risk_level: "high"`
  * *"Tắt nguồn điện thoại ngay lập tức"* $\rightarrow$ `unsupported` {}, `risk_level: "high"`

---

## 3. CÁC ĐẶC TRƯNG NGÔN NGỮ ĐA DẠNG & THÁCH THỨC ASR

### 3.1. Kính ngữ & Đại từ xưng hô gia tộc phong phú:
* *Kính ngữ*: Cháu ơi, nhờ con, bác muốn, bà nhờ, thím nhờ, làm ơn, giùm tui, giúp ông, phiền cháu...
* *Đại từ*: ông nội, bà ngoại, bác Hai, chú Bảy, thím Tám, cô Út, cậu Ba, dì Năm, anh Cả, chị Tư, cu Tí, bé Bông, cu Bin, ba yêu, mẹ yêu, vợ yêu, chồng yêu, con gái, con trai, ông sui, bà sui, thông gia, bạn nối khố.

### 3.2. Cấu trúc câu Đảo ngữ & Khẩu ngữ tự nhiên:
* Đảo vị trí thời gian / địa điểm / đối tượng lên trước:
  * *"6 giờ sáng mai, kêu tui dậy nha"* $\rightarrow$ `set_alarm`
  * *"Mẹ tôi, gọi cho bà ấy"* $\rightarrow$ `call_contact`
  * *"Bệnh viện Bạch Mai, chỉ đường qua đó giúp bác"* $\rightarrow$ `open_map`
  * *"Tắt nồi cá kho, hẹn 30 phút nữa"* $\rightarrow$ `set_timer`

### 3.3. Nhiễu ASR, Nói ngập ngừng & Không dấu:
* Ngập ngừng: *"À... ừm... cháu ơi... gọi... gọi cho bác Nam với..."*
* Ngọng l/n nhẹ: *"đặt báo thức lăm giờ sáng mai", "nhắn cho nương"*
* Tiếng Việt không dấu: *"dat bao thuc 6h sang", "chi duong den cho ben thanh", "nhan tin cho me la con ve roi"*

---

## 4. MA TRẬN PHÂN BỐ DỮ LIỆU TỐI ƯU (TỔNG 40K - 100K MẪU)

| Phân nhóm Intent / Dữ liệu | Tỷ lệ đề xuất | Mô tả chi tiết |
| :--- | :---: | :--- |
| **8 Intent Tác Vụ Chính** (`alarm`, `timer`, `map`, `app`, `call`, `sms`, `video`, `music`) | **70%** | Chia đều ~8.75% cho mỗi intent. Đầy đủ các chủ đề đời sống, phong tục, sức khỏe, gia đình, nông thôn. |
| **Unsupported (Negative Sampling / OOD)** | **15%** | Câu hỏi thời tiết, chém gió, ngân hàng, kiến thức, tác vụ nguy hiểm $\rightarrow$ Chống ảo giác tuyệt đối. |
| **Clarify (Câu mơ hồ / thiếu slot)** | **5% - 8%** | Câu lệnh ngắn cụt lủn thiếu thông tin bắt buộc $\rightarrow$ Rèn luyện khả năng phát hiện thiếu slot. |
| **Nhiễu Micro / ASR / Không Dấu / Ngập Ngừng** | **10%** | Mô phỏng âm thanh micro thực tế, tiếng Việt không dấu, nói lắp, sai lỗi chính tả nhẹ. |

---

## 5. MẪU DỮ LIỆU ĐẦU RA JSONL

```json
{"messages": [{"role": "system", "content": "Bạn là bộ phân tích NLU trích xuất ý định (intent) và tham số (arguments). Các intent hỗ trợ: [set_alarm, set_timer, open_app, open_map, call_contact, send_sms, search_video, play_music, clarify, unsupported]. Chỉ trả về JSON duy nhất: {\"intent\": string, \"arguments\": object, \"risk_level\": \"low\"|\"medium\"|\"high\", \"status\": \"success\"|\"needs_clarification\"|\"invalid\"|\"unsupported\", \"requires_confirmation\": boolean}."}, {"role": "user", "content": "Cháu ơi hẹn giúp bác 2 tiếng nữa nhắc ra tắt máy bơm nước ruộng nghen"}, {"role": "assistant", "content": "{\"intent\":\"set_timer\",\"arguments\":{\"duration\":2,\"unit\":\"hours\",\"label\":\"tắt máy bơm nước ruộng\"},\"risk_level\":\"low\",\"status\":\"success\",\"requires_confirmation\":false}"}]}
```

---

## 6. PROMPT TEMPLATE CHO BATCH GENERATOR (DÙNG CHO LLM/SCRIPT)

Khi dùng API GPT-4o, Claude hoặc script Python để sinh hàng loạt mẫu, nạp trực tiếp prompt sau:

```text
Bạn là chuyên gia sinh dữ liệu huấn luyện NLU cho trợ lý ảo tiếng Việt ViDroidCall Studio.
Nhiệm vụ: Sinh 50 mẫu JSONL hội thoại đa dạng (đặc biệt văn phong người cao tuổi, đa vùng miền Bắc-Trung-Nam, đảo ngữ, tiếng lóng, khẩu ngữ).

Tuân thủ nghiêm ngặt 10 Intent và Schema sau:
- set_alarm: arguments: {"hour": int, "minute": int, "label": string}
- set_timer: arguments: {"duration": int, "unit": "seconds"|"minutes"|"hours", "label": string}
- open_map: arguments: {"destination": string}
- open_app: arguments: {"app_name": string}
- call_contact: arguments: {"contact": string} (risk_level: "high" nếu 113, 114, 115, 111, 911; "low" nếu danh bạ; requires_confirmation: true)
- send_sms: arguments: {"contact": string, "message": string (nếu có)} (risk_level: "medium", requires_confirmation: true)
- search_video: arguments: {"query": string}
- play_music: arguments: {"song_name": string, "artist": string, "genre": string}
- clarify: arguments: {"missing": ["contact"|"query"|"destination"|"app_name"|"song_name"|"hour"|"duration"]} (status: "needs_clarification")
- unsupported: arguments: {} (status: "unsupported", risk_level: "low" hoặc "high")

Mỗi dòng là 1 JSON duy nhất dạng:
{"messages": [{"role": "system", "content": "Bạn là bộ phân tích NLU trích xuất ý định (intent) và tham số (arguments). Các intent hỗ trợ: [set_alarm, set_timer, open_app, open_map, call_contact, send_sms, search_video, play_music, clarify, unsupported]. Chỉ trả về JSON duy nhất: {\"intent\": string, \"arguments\": object, \"risk_level\": \"low\"|\"medium\"|\"high\", \"status\": \"success\"|\"needs_clarification\"|\"invalid\"|\"unsupported\", \"requires_confirmation\": boolean}."}, {"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]}
```
