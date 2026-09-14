# Changelog

All notable changes to ViDroidCall Studio are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.0] - 2026-09-14

### Added

- Trợ lý nổi: overlay đè lên app khác, công tắc Cài đặt (mặc định tắt), cấp quyền micro / thông báo / hiện trên ứng dụng khác
- Wake word và trợ lý hệ thống Android (foreground service, thông báo đang chờ, RecognitionService)
- Hộp thoại nổi: 7 trạng thái, sóng âm, xác nhận gọi/SMS, ẩn số điện thoại trên overlay
- Intent `search_web` trên Fast-Path
- Trọng số NLU GGUF trong `models/` (Git LFS), tên file cố định `qwen3-nlu-run-Q4_K_M.gguf`

### Changed

- Một công tắc Trợ lý nổi (không tách nhiều hàng)
- Màn trợ lý: ẩn thẻ JSON NLU, căn nút Home giữa, sửa hộp xác nhận bị cắt chữ
- Pre-warm STT khi service nổi chạy để giảm độ trễ nghe

### Fixed

- Crash/lag khi bật tắt Trợ lý nổi; xung đột micro; wake word / mic xanh mất do overlay
- Placeholder “Hãy nói gì đó...” bị che khi đang nghe

### Docs

- Zipformer VI: CC BY-NC-ND 4.0; link Kotlin / Jetpack / org.json rõ giấy phép

## [1.0.1] - 2026-09-08

### Changed

- Menu bar: larger Home/mic control, no glow border around the bar (PR #44)
- Restore `CONTRIBUTING.md` for the GitHub community tab
- README: microphone starts only after a GGUF model is loaded

## [1.0.0] - 2026-09-05

First public open-source release (Apache License 2.0).

### Added

- Offline Vietnamese speech-to-text with Sherpa-ONNX Zipformer 30M Int8 and Silero VAD
- Hybrid NLU: Fast-Path rule matching (sub-5ms) plus on-device Llama.cpp / Qwen3 0.6B GGUF for complex utterances
- Native actions: call contact, SMS, alarm, timer, open app, maps, play music, search video
- Elderly-friendly UX: large type, 3-step permission guides, action confirmation, 4-stage speech card, TTS feedback
- Command history, theme and font-size settings, onboarding
- GitHub Actions CI (compile + unit tests)

### License

- Project source is licensed under Apache-2.0 (`LICENSE`, `NOTICE`)
- Third-party components are listed in `OPEN_SOURCE_LICENSES.md`
- NLU GGUF weights: `models/qwen3-nlu-run-Q4_K_M.gguf` (Git LFS)
