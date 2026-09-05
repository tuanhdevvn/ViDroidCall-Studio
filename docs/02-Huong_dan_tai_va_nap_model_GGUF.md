# 📦 Hướng Dẫn Tải & Nạp Mô Hình AI GGUF (On-Device NLU)

Tài liệu này hướng dẫn tải mô hình AI NLU (định dạng `.gguf`) và nạp vào thiết bị Android. Ứng dụng chạy offline 100% sau khi file đã nằm trên máy.

**Mã nguồn app:** [github.com/tuanhdevvn/ViDroidCall-Studio](https://github.com/tuanhdevvn/ViDroidCall-Studio)

---

## 🔗 1. Kho mô hình (Hugging Face)

Bản dùng cho demo / nộp bài:

👉 **[tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6](https://huggingface.co/tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6)**

| File | Dung lượng | Ghi chú |
| :--- | :---: | :--- |
| `qwen3-nlu-run-006-Q4_K_M.gguf` | **~397 MB** | Fine-tune Qwen3 0.6B, lượng tử hóa Q4_K_M — **bản chính thức** |

Trang Files trên Hugging Face: chọn file `.gguf` → Download.

---

## 📲 2. Nạp vào điện thoại

Ứng dụng quét file `.gguf` tại **Download (`/sdcard/Download/`)**. Tên ưu tiên trong code: `qwen3-nlu-run-006-Q4_K_M.gguf`.

### Cách 1: ADB

1. Kết nối USB, bật **Gỡ lỗi USB**.
2. Tải `qwen3-nlu-run-006-Q4_K_M.gguf` từ Hugging Face về máy tính.
3. Chạy:

```bash
adb push ~/Downloads/qwen3-nlu-run-006-Q4_K_M.gguf /sdcard/Download/
```

Nên xóa file `.gguf` cũ trên máy trước khi demo, tránh app nạp nhầm bản Qwen2.5.

### Cách 2: Tải trên điện thoại

1. Mở [repo Hugging Face](https://huggingface.co/tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6) trên trình duyệt điện thoại.
2. **Files** → `qwen3-nlu-run-006-Q4_K_M.gguf` → Download.
3. File nằm trong thư mục **Download**.

### Cách 3: Cáp USB (MTP)

Copy file `.gguf` vào `Bộ nhớ trong → Download`.

---

## ✅ 3. Kiểm tra trên ứng dụng

1. **Assistant:** huy hiệu **`🟢 Trợ lý AI đã sẵn sàng`**.
2. **Cài đặt → Mô hình AI On-Device:** tên file `qwen3-nlu-run-006-Q4_K_M.gguf`.

Chưa có GGUF vẫn dùng được STT và Fast-Path.

---

## 🛠️ 4. Sự cố thường gặp

### Báo "🔴 Chưa có mô hình AI"
- File chưa đúng thư mục Download, hoặc trình duyệt đổi đuôi (`.gguf.txt`).
- Đổi tên thành `qwen3-nlu-run-006-Q4_K_M.gguf`. Cấp quyền quản lý tệp nếu app yêu cầu.

### Đổi / xóa model cũ

```bash
adb shell "rm -f /sdcard/Download/*.gguf"
```

Sau đó nạp lại file từ Hugging Face và mở lại ứng dụng.
