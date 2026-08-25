# 📦 Hướng Dẫn Tải & Nạp Mô Hình AI GGUF (On-Device NLU)

Tài liệu này hướng dẫn chi tiết cách tải các phiên bản mô hình AI NLU (định dạng `.gguf`) và nạp vào thiết bị Android để chạy trợ lý ảo offline 100% không cần kết nối mạng.

---

## 🔗 1. Kho Lưu Trữ Mô Hình (Google Drive)

Toàn bộ các phiên bản mô hình GGUF đã được tối ưu và đóng gói sẵn tại thư mục Google Drive chính thức của dự án:

👉 **[Truy cập Google Drive chứa Models GGUF](https://drive.google.com/drive/folders/1nmWkENo5Oo_fYT5k5dA9e8-Mm3Napln2)**

```text
Link Drive: https://drive.google.com/drive/folders/1nmWkENo5Oo_fYT5k5dA9e8-Mm3Napln2
```

---

## 📊 2. Lựa Chọn Phiên Bản Mô Hình Phù Hợp

Trong thư mục Drive có các phiên bản mô hình với dung lượng và mức lượng tử hóa (Quantization) khác nhau:

| Tên File Mô Hình | Dung lượng | Khuyến nghị thiết bị | Ưu điểm |
| :--- | :---: | :--- | :--- |
| `qwen2.5-0.5b-nlu-q8_0.gguf` | **~500 MB** | **Máy phổ thông / RAM 3GB - 4GB** *(Khuyên dùng)* | Nhẹ, phản hồi cực nhanh, độ chính xác NLU cao |
| `qwen2.5-0.5b-nlu-q4_k_m.gguf`| **~350 MB** | **Máy cấu hình yếu / RAM 2GB - 3GB** | Dung lượng siêu nhỏ, nạp nhanh |
| `qwen2.5-1.5b-nlu-q8_0.gguf` | **~1.6 GB** | **Máy tầm trung / RAM 6GB trở lên** | Hiểu ngữ cảnh sâu, xử lý câu phức tạp vượt trội |
| `qwen2.5-1.5b-nlu-q4_k_m.gguf`| **~1.1 GB** | **Máy RAM 4GB - 6GB** | Cân bằng hoàn hảo giữa tốc độ và độ chính xác |

---

## 📲 3. Các Cách Nạp Mô Hình Vào Điện Thoại

Ứng dụng **ViDroidCall Studio** được thiết kế tự động quét tìm file `.gguf` tại **thư mục Download (`/sdcard/Download/`)** của máy.

### Cách 1: Nạp qua lệnh ADB từ máy tính (Nhanh nhất cho Developer)

1. Kết nối điện thoại với máy tính qua cáp USB và bật **Gỡ lỗi USB (USB Debugging)**.
2. Tải file `.gguf` từ Drive về máy tính (ví dụ lưu tại thư mục `Downloads` máy tính).
3. Mở Terminal / Command Prompt và chạy lệnh:

```bash
# Đẩy file mô hình vào thư mục Download của điện thoại
adb push /duong_dan_tren_may_tinh/qwen2.5-0.5b-nlu-q8_0.gguf /sdcard/Download/
```

> **Ví dụ cụ thể trên MacOS:**
> ```bash
> adb push ~/Downloads/qwen2.5-0.5b-nlu-q8_0.gguf /sdcard/Download/
> ```

---

### Cách 2: Tải trực tiếp trên điện thoại (Dành cho người dùng cuối)

1. Dùng trình duyệt (Chrome, Cốc Cốc, Samsung Internet) trên điện thoại mở link Google Drive ở mục 1.
2. Chọn file mô hình cần dùng (ví dụ: `qwen2.5-0.5b-nlu-q8_0.gguf`) và nhấn **Tải xuống (Download)**.
3. Khi tải xong, file sẽ tự động nằm trong thư mục **Download (Tải về)** của điện thoại.

---

### Cách 3: Chép file qua cáp USB (MTP - Truyền tệp)

1. Cắm cáp kết nối điện thoại với máy tính, chọn chế độ **Truyền tệp (File Transfer / MTP)** trên thanh thông báo điện thoại.
2. Mở trình quản lý file trên máy tính (Finder trên Mac hoặc File Explorer trên Windows).
3. Copy file `.gguf` vừa tải và dán (Paste) vào thư mục:
   ```text
   Bộ nhớ trong (Internal Storage) -> Download
   ```

---

## ✅ 4. Kiểm Tra Trạng Thái Kích Hoạt Trên Ứng Dụng

Sau khi nạp file vào `/sdcard/Download/`, mở ứng dụng **ViDroidCall Studio**:

1. **Màn hình chính (Assistant Tab):**
   - Phía trên cùng xuất hiện huy hiệu màu xanh lá: **`🟢 Trợ lý AI đã sẵn sàng`**.
2. **Màn hình Cài đặt (Settings Tab):**
   - Mục **Mô hình AI On-Device** sẽ hiển thị thẻ trạng thái:
     - **Tên file:** `qwen2.5-0.5b-nlu-q8_0.gguf`
     - **Trạng thái:** Đã nạp thành công vào RAM và sẵn sàng suy luận.

---

## 🛠️ 5. Xử Lý Sự Cố Thường Gặp (Troubleshooting)

### Q1: Ứng dụng báo "🔴 Chưa có mô hình AI"?
- **Nguyên nhân:** File chưa nằm đúng thư mục `Download` hoặc file bị lưu sai đuôi mở rộng (ví dụ bị trình duyệt đổi thành `.bin` hoặc `.gguf.txt`).
- **Khắc phục:** Dùng ứng dụng *Quản lý tệp (My Files)* trên điện thoại, đổi tên file sao cho đúng đuôi `.gguf`.

### Q2: Muốn đổi sang mô hình khác hoặc xoá mô hình cũ?
- **Cách xóa file cũ qua ADB:**
  ```bash
  adb shell "rm -f /sdcard/Download/*.gguf"
  ```
- Sau đó nạp file mô hình mới vào và mở lại ứng dụng.
