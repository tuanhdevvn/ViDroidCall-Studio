# Bộ câu test NLU (~50 câu)

Dùng để kiểm tra 13 intent của ViDroidCall trên thiết bị thật.

**Cách chạy:** nói từng câu bằng nút micro tab Home, hoặc Trợ lý nổi (“Trợ lý ơi”).  
`call_contact` và `send_sms` luôn hiện hộp xác nhận. `open_map`, `search_video`, `search_web` có thể cần mạng.

Chuẩn bị sẵn danh bạ **mẹ / bố / anh Tuấn / chị Lan / bạn Hùng** (hoặc đổi tên cho khớp máy).

---

## 1. `greeting` — chào hỏi

| # | Câu nói | Kỳ vọng |
|---|---------|---------|
| 1 | Xin chào | Chào lại, không làm thao tác |
| 2 | Hello | Như trên |
| 3 | Alo | Như trên |

## 2. `goodbye` — tạm biệt

| # | Câu nói | Kỳ vọng |
|---|---------|---------|
| 4 | Tạm biệt | Tạm biệt / đóng hội thoại |
| 5 | Hẹn gặp lại | Như trên |

## 3. `call_contact` — gọi điện (cần xác nhận)

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 6 | Gọi cho mẹ | contact = mẹ |
| 7 | Gọi điện cho anh Tuấn | contact = anh Tuấn |
| 8 | Alo cho bạn Hùng | contact = bạn Hùng |
| 9 | Gọi 0912345678 | số trực tiếp |
| 10 | Gọi cấp cứu | 115 |
| 11 | Gọi 113 | 113 |

## 4. `send_sms` — nhắn tin (cần xác nhận)

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 12 | Nhắn tin cho mẹ | contact = mẹ, chưa có nội dung |
| 13 | Gửi tin nhắn cho bố: Con đang ở trường | bố + nội dung |
| 14 | Nhắn tin cho mẹ là con đang về rồi | mẹ + “con đang về rồi” |
| 15 | Nhắn mẹ con về muộn nhé | mẹ + nội dung |

## 5. `set_alarm` — báo thức

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 16 | Đặt báo thức 6 giờ 30 phút sáng | 06:30 |
| 17 | Báo thức bảy giờ rưỡi tối | 19:30 |
| 18 | Báo thức 2 giờ kém 10 chiều | 13:50 |
| 19 | Nhắc tôi 7 giờ sáng | 07:00 |

## 6. `set_timer` — hẹn giờ

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 20 | Hẹn giờ 15 phút | 15 phút |
| 21 | Hẹn giờ nửa tiếng | 30 phút |
| 22 | Hẹn giờ hai mươi giây | 20 giây |
| 23 | Hẹn giờ một tiếng | 1 giờ |

## 7. `open_app` — mở ứng dụng

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 24 | Mở ứng dụng YouTube | youtube |
| 25 | Mở Zalo | zalo |
| 26 | Mở Facebook | facebook |
| 27 | Mở Shopee | shopee |
| 28 | Mở máy tính | calculator |

## 8. `open_map` — bản đồ / chỉ đường

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 29 | Chỉ đường đến Hồ Gươm | Hồ Gươm |
| 30 | Quán phở gần tôi | địa điểm gần |
| 31 | Cây xăng gần nhất | POI gần nhất |
| 32 | Chỉ đường về nhà | nhà |

## 9. `search_video` — YouTube

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 33 | Tìm video Sơn Tùng trên YouTube | Sơn Tùng |
| 34 | Xem video nấu phở | nấu phở |
| 35 | Tìm clip tập thể dục cho người già | query tương ứng |

## 10. `play_music` — phát nhạc

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 36 | Phát nhạc Trịnh Công Sơn | Trịnh Công Sơn |
| 37 | Phát nhạc Sơn Tùng | Sơn Tùng |
| 38 | Bật nhạc | mở trình phát, không query |

## 11. `search_web` — tìm web

Câu dạng “là ai / là gì” **không được** thành `call_contact`.

| # | Câu nói | Slot kỳ vọng |
|---|---------|--------------|
| 39 | Hà Anh Tuấn là ai | search_web |
| 40 | VNeID là gì | search_web |
| 41 | Hôm nay Hà Nội có mưa không | search_web |
| 42 | Thời tiết ngày mai | search_web |
| 43 | Giá vàng hôm nay | search_web |
| 44 | Anh Tuấn là ai | search_web (không gọi điện) |

## 12. `clarify` — thiếu thông tin

App hỏi lại, **không** tự gọi / nhắn / hẹn giờ.

| # | Câu nói | Thiếu gì |
|---|---------|----------|
| 45 | Gọi điện | thiếu contact |
| 46 | Nhắn tin | thiếu contact |
| 47 | Đặt báo thức | thiếu giờ |
| 48 | Tra cứu đi | thiếu query |

## 13. `unsupported` — ngoài phạm vi

Kỳ vọng: “Xin lỗi, tôi chưa hỗ trợ…”

| # | Câu nói |
|---|--------|
| 49 | Tắt đèn phòng khách |
| 50 | Đặt vé máy bay đi Đà Nẵng ngày mai |

---

## Câu phụ (dễ nhầm intent)

| # | Câu nói | Đúng | Dễ nhầm thành |
|---|---------|------|----------------|
| 51 | Gọi cho chị Lan | `call_contact` | — |
| 52 | Chị Lan là ai | `search_web` | `call_contact` |

---

## Gợi ý chạy tay

1. Nói lần lượt câu 1 → 50 trên tab Home.
2. Với câu 6–15: hủy một lần, rồi xác nhận chạy một lần.
3. Câu 44 và 52 là safety-net: hỏi “là ai” không được mở cuộc gọi.
