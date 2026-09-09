# Hướng dẫn Đóng góp (Contributing Guide)

Cảm ơn bạn đã quan tâm tới **ViDroidCall Studio**. Mọi đóng góp (mã nguồn, tài liệu, báo lỗi) được cấp phép theo [Apache License 2.0](LICENSE).

Thank you for helping improve this project. Contributions are licensed under the [Apache License 2.0](LICENSE).

---

## Báo lỗi (Report a bug)

Mở issue tại [GitHub Issues](https://github.com/tuanhdevvn/ViDroidCall-Studio/issues).

Please include:

* Device model and Android version
* App version (or commit SHA)
* Steps to reproduce
* Expected vs actual behavior
* Logcat snippet if the crash is in Kotlin / native code

Không dán số điện thoại, danh bạ, hay nội dung tin nhắn thật vào issue.

---

## Chạy dự án cục bộ (Build locally)

Hướng dẫn đầy đủ: [docs/BUILD.md](docs/BUILD.md). Tóm tắt:

```bash
git clone https://github.com/tuanhdevvn/ViDroidCall-Studio.git
cd ViDroidCall-Studio
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Yêu cầu: **JDK 21**, Android SDK (`compileSdk` 37 / `minSdk` 26). Android Studio không bắt buộc; Gradle wrapper là đủ.

Trợ lý giọng nói **chỉ bắt đầu nghe** khi đã nạp file GGUF vào thư mục Download của máy:

[`models/qwen3-nlu-run-Q4_K_M.gguf`](models/qwen3-nlu-run-Q4_K_M.gguf) (Git LFS; `adb push` sang Download trên máy)

Chi tiết `adb push` và Fast-Path: [README.md](README.md).

---

## Pull request

1. Fork (nếu cần) và tạo nhánh từ `main`.
2. Đặt tên nhánh theo convention để CI chạy trên push:
   * `feature/...` — tính năng mới
   * `bugfix/...` — sửa lỗi
3. Giữ thay đổi tập trung; bám style Kotlin / Compose hiện có.
4. Chạy `./gradlew testDebugUnitTest` trước khi mở PR.
5. Mô tả vấn đề, cách sửa, và cách bạn đã kiểm tra.

PR nhắm vào nhánh `main`. GitHub Actions sẽ biên dịch Kotlin và chạy unit test.

---

## Phạm vi đóng góp nên tránh (Out of scope)

* **Không commit** file `.gguf` ngoài `models/` (Git LFS), hay file `.bin` linh tinh. Trọng số NLU chính thức là `models/qwen3-nlu-run-Q4_K_M.gguf` (ghi đè cùng tên khi cập nhật, không đổi tên file).
* **Không commit** `local.properties`, `.idea/`, hay khóa máy local.
* **Không sửa** binary native chưa ghi rõ nguồn. File `.so` / ONNX đi kèm: [docs/THIRD_PARTY_BINARIES.md](docs/THIRD_PARTY_BINARIES.md).
* **Không đụng** copyright Xiaomi trong `com.k2fsa.sherpa.onnx`. File của nhóm dùng header `SPDX-License-Identifier: Apache-2.0`.

---

## Liên hệ tài liệu (Docs)

| Tài liệu | Nội dung |
| :--- | :--- |
| [README.md](README.md) | Tổng quan, kiến trúc, cài đặt |
| [docs/BUILD.md](docs/BUILD.md) | JDK, Gradle, GGUF |
| [CHANGELOG.md](CHANGELOG.md) | Lịch sử phiên bản |
| [OPEN_SOURCE_LICENSES.md](OPEN_SOURCE_LICENSES.md) | Thư viện bên thứ ba |
| [LICENSE](LICENSE) / [NOTICE](NOTICE) | Apache-2.0 |
