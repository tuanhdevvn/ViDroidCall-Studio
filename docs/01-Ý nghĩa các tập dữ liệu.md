# Giải thích các tập dữ liệu cho người mới

> **Cách nhớ nhanh:** TRAIN = học · VALIDATION = thi thử · TEST = thi thật

## 1. Tổng quan

| Tập dữ liệu | Số câu | Ý nghĩa dễ nhớ |
|---|---:|---|
| Train | 350 | Cho AI học từ các ví dụ |
| Validation | 50 | Thi thử để điều chỉnh hệ thống |
| Test | 60 | Thi thật để báo cáo kết quả |
| Test OOD | 20 | Kiểm tra với tình huống lạ / ngoài miền |
| Test Ambiguous | 20 | Kiểm tra khi câu nói mơ hồ hoặc thiếu thông tin |

Tổng cộng: **500 câu**.

---

## 2. Train — tập để AI học

`Train` là tập dữ liệu mà AI được phép học từ đó.

Ví dụ:

- `"Đặt báo thức lúc 6 giờ"` → `set_alarm`
- `"Mai gọi tôi dậy lúc 7 giờ"` → `set_alarm`
- `"Mở YouTube cho tôi"` → `open_app`

Có thể hiểu đơn giản:

> **Train = tài liệu học + bài tập luyện tập.**

Trong bộ dữ liệu này có **350 câu train**.

---

## 3. Validation — tập thi thử

`Validation` dùng để kiểm tra hệ thống trong quá trình phát triển.

Sau khi AI học từ tập train, ta đưa cho AI những câu khác để xem hệ thống hoạt động tốt hay chưa.

Ví dụ:

> `"6 giờ sáng mai nhớ đánh thức tôi nhé"`

Nếu AI nhận đúng là `set_alarm` thì tốt. Nếu nhận sai nhiều, ta có thể điều chỉnh cấu hình, ngưỡng hoặc quy trình.

Có thể hiểu:

> **Validation = thi thử để xem cần điều chỉnh gì.**

Trong bộ dữ liệu này có **50 câu validation**.

---

## 4. Test — tập thi thật

`Test` là tập dùng để đánh giá chính thức sau khi hệ thống đã được phát triển xong.

Ví dụ câu test:

> `"Gọi cho mẹ giúp tôi"`

Kết quả mong đợi:

```text
intent = call_contact
contact = Mẹ
```

Trong bộ dữ liệu này có **60 câu test**.

Điều quan trọng:

> **AI không được học trước các câu test.**

Nếu AI đã nhìn thấy câu test trong lúc học, kết quả đánh giá sẽ không còn đáng tin cậy.

---

## 5. Test OOD — kiểm tra tình huống lạ

`OOD` là viết tắt của **Out-of-Distribution**.

Hiểu đơn giản:

> **Test OOD = kiểm tra AI khi gặp tình huống lạ hoặc ngoài phạm vi quen thuộc.**

Ví dụ hệ thống hỗ trợ:

- đặt báo thức;
- hẹn giờ;
- gọi điện;
- gửi SMS;
- mở bản đồ;
- mở ứng dụng.

Nhưng người dùng nói:

> `"Chuyển cho Nam 5 triệu đồng."`

Đây không phải chức năng hệ thống hỗ trợ. Một hệ thống tốt phải nhận ra yêu cầu này là ngoài phạm vi thay vì cố thực hiện.

Trong bộ dữ liệu này có **20 câu test OOD**.

---

## 6. Test Ambiguous — kiểm tra câu mơ hồ

`Ambiguous` nghĩa là **mơ hồ hoặc thiếu thông tin**.

Ví dụ:

> `"Nhắn cho Nam"`

Hệ thống biết người nhận là Nam nhưng chưa biết nội dung tin nhắn.

Thay vì tự bịa nội dung, hệ thống nên nhận ra còn thiếu `message`.

Ví dụ kết quả:

```json
{
  "intent": "clarify",
  "arguments": {
    "missing": ["message"]
  }
}
```

Có thể hiểu:

> **Test Ambiguous = kiểm tra xem AI có biết rằng mình chưa có đủ thông tin hay không.**

Trong bộ dữ liệu này có **20 câu test ambiguous**.

---

## 7. Cách nhớ nhanh

| Tập | Cách nhớ |
|---|---|
| Train | Học |
| Validation | Thi thử |
| Test | Thi thật |
| Test OOD | Thi với tình huống lạ |
| Test Ambiguous | Thi với câu mơ hồ / thiếu thông tin |

---

## 8. Nguyên tắc quan trọng nhất

> **Train được phép cho AI học. Test thì không.**

Nếu dữ liệu test bị đưa vào train, hiện tượng này được gọi là **data leakage** — rò rỉ dữ liệu.

Khi đó, kết quả test có thể rất cao nhưng không phản ánh đúng khả năng thực tế của mô hình.
