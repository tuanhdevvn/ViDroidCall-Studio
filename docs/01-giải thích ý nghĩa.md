GIẢI THÍCH CÁC TẬP DỮ LIỆU
CHO NGƯỜI MỚI
Train • Validation • Test • Test OOD • Test Ambiguous
Cách nhớ nhanh: TRAIN = học • VALIDATION = thi thử • TEST = thi thật
Tài liệu này giải thích theo cách đơn giản các loại tập dữ liệu được dùng trong bộ dữ liệu ViDroidCall. Hãy hình dung AI giống như một sinh viên: cần có tài liệu để học, bài thi thử để điều chỉnh, và bài thi thật để đánh giá năng lực.
1. Bức tranh tổng thể
Tập dữ liệu	Số câu	Ý nghĩa dễ nhớ
Train	350	Cho AI học từ các ví dụ
Validation	50	Thi thử để điều chỉnh hệ thống
Test	60	Thi thật để báo cáo kết quả
Test OOD	20	Kiểm tra với tình huống lạ / ngoài miền
Test Ambiguous	20	Kiểm tra khi câu nói mơ hồ hoặc thiếu thông tin
2. Train - tập để học
Train là tập dữ liệu mà AI được phép nhìn thấy và học từ đó. Nếu muốn AI nhận biết câu nào là đặt báo thức, gọi điện hay mở ứng dụng, ta cung cấp nhiều ví dụ đúng trong tập train.
"Đặt báo thức lúc 6 giờ"      -> set_alarm
"Mai gọi tôi dậy lúc 7 giờ" -> set_alarm
"Mở YouTube cho tôi"        -> open_app

Hãy coi train giống như sách giáo khoa và bài tập ôn luyện. Trong kế hoạch 500 câu, train có 350 câu.
Ghi nhớ: TRAIN = HỌC.
3. Validation - tập để thi thử
Sau khi AI học từ train, ta cần kiểm tra xem cách xây dựng hệ thống có đang đi đúng hướng hay không. Validation là tập dùng trong quá trình phát triển để thử nghiệm và điều chỉnh.
•	Có thể dùng kết quả validation để chọn cấu hình hoặc ngưỡng phù hợp.
•	Nếu hệ thống làm chưa tốt, có thể quay lại điều chỉnh rồi kiểm tra tiếp.
•	Không nên dùng validation làm kết quả cuối cùng để công bố hiệu năng.
Ghi nhớ: VALIDATION = THI THỬ.
4. Test - tập để thi thật
Test là tập dùng để đánh giá chính thức sau khi quá trình phát triển đã hoàn thành. AI không được học trước các câu test.
Ví dụ câu test: "Gọi cho mẹ giúp tôi"
Kết quả mong đợi: intent = call_contact, contact = Mẹ

Nếu AI đã nhìn thấy câu test trong lúc học, kết quả sẽ không còn phản ánh đúng năng lực thật. Điều này giống như sinh viên được xem trước đề thi cuối kỳ rồi mới đi thi.
Ghi nhớ: TEST = THI THẬT.
Nguyên tắc quan trọng nhất: Train được phép cho AI học. Test thì không.
5. Test OOD - kiểm tra tình huống lạ
OOD là viết tắt của Out-of-Distribution. Với người mới, chỉ cần hiểu đây là những tình huống khác hoặc nằm ngoài phạm vi mà hệ thống thường xử lý.
Ví dụ ViDroidCall hỗ trợ gọi điện, SMS, báo thức, hẹn giờ, bản đồ và mở ứng dụng. Nếu người dùng nói:
"Chuyển cho Nam 5 triệu đồng."

Đây không phải chức năng được hỗ trợ. Một hệ thống tốt cần nhận ra yêu cầu ngoài phạm vi thay vì cố thực hiện. Test OOD giúp kiểm tra khả năng xử lý các tình huống như vậy.
Ghi nhớ: TEST OOD = THI VỚI TÌNH HUỐNG LẠ.
6. Test Ambiguous - kiểm tra câu mơ hồ
Ambiguous nghĩa là mơ hồ hoặc thiếu thông tin. Tập này kiểm tra xem AI có biết nhận ra rằng mình chưa có đủ dữ liệu để thực hiện yêu cầu hay không.
Người dùng: "Nhắn cho Nam"
Đã biết: contact = Nam
Còn thiếu: message (nội dung tin nhắn)
Kết quả đúng: intent = clarify, missing = ["message"]

AI không nên tự bịa nội dung tin nhắn. Nó cần hỏi lại hoặc đánh dấu phần thông tin còn thiếu.
Ghi nhớ: TEST AMBIGUOUS = THI XEM AI CÓ BIẾT "CHƯA ĐỦ THÔNG TIN" HAY KHÔNG.
7. Cách nhớ bằng ví dụ đi học
Bước	Tập	Ví dụ đi học
1	TRAIN	Học bài từ tài liệu và ví dụ.
2	VALIDATION	Thi thử để biết cần điều chỉnh gì.
3	TEST	Thi thật để đánh giá kết quả chính thức.
4	TEST OOD	Gặp câu hỏi lạ để xem phản ứng có an toàn/hợp lý không.
5	TEST AMBIGUOUS	Gặp câu hỏi thiếu dữ kiện để xem có biết hỏi lại không.
8. Data leakage - lỗi người mới cần tránh
Data leakage (rò rỉ dữ liệu) xảy ra khi thông tin từ tập test vô tình lọt vào quá trình train hoặc điều chỉnh mô hình. Khi đó điểm test có thể rất cao nhưng không đáng tin cậy.
•	Không đưa câu test hoặc câu paraphrase gần giống của test vào train.
•	Không nhìn lỗi trên test rồi sửa mô hình riêng để vượt qua chính các câu đó.
•	Nên tách và khóa tập test trước khi augmentation hoặc huấn luyện.
Tóm tắt 1 dòng: Train để học -> Validation để điều chỉnh -> Test để đánh giá -> OOD và Ambiguous để kiểm tra các tình huống khó hơn.

