# Mô hình NLU (GGUF)

File trọng số Qwen3 NLU dùng khi chạy trợ lý trên máy thật:

`qwen3-nlu-run-006-Q4_K_M.gguf` (~397 MB)

- Nguồn: [tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6](https://huggingface.co/tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6)
- Giấy phép: Apache-2.0 (fine-tune trên [Qwen3](https://github.com/QwenLM/Qwen3))
- Git: lưu bằng **Git LFS** (GitHub không nhận file thường > 100 MB)

## Clone

Cần [Git LFS](https://git-lfs.com/) trước khi clone (hoặc `git lfs pull` sau khi clone):

```bash
brew install git-lfs   # macOS
git lfs install
git clone https://github.com/tuanhdevvn/ViDroidCall-Studio.git
cd ViDroidCall-Studio
git lfs pull
```

Ứng dụng **không** đọc file từ thư mục này khi Run trên điện thoại. Sau khi clone, đẩy file vào Download trên máy:

```bash
adb push models/qwen3-nlu-run-006-Q4_K_M.gguf /sdcard/Download/
```

Không đưa GGUF vào `app/src/main/assets` — APK sẽ quá lớn và mỗi lần cài debug sẽ chậm.
