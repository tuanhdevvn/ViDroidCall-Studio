# Changelog

All notable changes to ViDroidCall Studio are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.2] - 2026-09-17

Bản sau **1.1.1**: lịch sử / câu lệnh hay dùng, ổn định NLU, giấy phép OLP PoF, trọng số Qwen3 mới.

### Added

- Tab Lịch sử: khối **Câu lệnh hay dùng** — 5 lối tắt theo tần suất 3 ngày, đóng băng đến hết ngày, chạm là chạy `NativeAction` (không qua NLU)
- Script gieo 100 câu demo người già vào SQLite trên máy (`scripts/seed_elderly_demo.py`)
- Bộ ~50 câu test đủ 13 intent trên thiết bị (`docs/NLU_TEST_UTTERANCES.md`)

### Changed

- Một SQLite (`vidroidcall_commands.db`) cho 10 câu lịch sử mới nhất và 5 lệnh hay dùng (làm mới snapshot 1 lần/ngày)
- Lịch sử: icon/nhãn cho chào hỏi, tạm biệt, hỏi lại, `search_web` / `search_video` / `play_music`; `unsupported` không dùng nhóm Hệ thống
- Khối hay dùng: ẩn nhãn buổi sáng/chiều/tối, bỏ nền thẻ trắng loãng
- Home: hiện lại dòng *AI đang phân tích* dưới câu lệnh; tắt sóng pulse trên logo menu bar
- Trọng số NLU Git LFS: `models/qwen3-nlu-Q4_K_M.gguf` (thay `qwen3-nlu-run-Q4_K_M.gguf`); app ưu tiên đúng tên file này
- SPDX, copyright và bản thông báo Apache 2.0 trên từng tệp mã nguồn (Kotlin, Gradle, XML, script, CI)

### Fixed

- Không văng process lần nạp GGUF đầu sau cài mới / xóa dữ liệu (không chồng Sherpa, không load GGUF trùng)
- Đặt báo thức vẫn chạy khi GGUF gán `status: invalid` (kể cả “báo thức 5 giờ”)
- CI biên dịch lại được: `backup_rules.xml` / `data_extraction_rules.xml` không còn comment Apache lồng comment mẫu

### Docs

- README: tách sơ đồ kiến trúc trong app và Trợ lý nổi
- `THIRD_PARTY_BINARIES.md`: `.so`/weights không sửa; Kotlin Sherpa chỉ thêm notice license; GGUF nằm trong `models/` (Git LFS)

## [1.1.1] - 2026-09-16

### Added

- Lưu mẫu sai NLU sau câu GGUF: thẻ JSON trên màn trợ lý, danh sách / xem JSON / xóa / share file trong Cài đặt. Fast-Path không lưu.

### Changed

- Trợ lý nổi chỉ kích hoạt bằng wake word **「trợ lý」 / 「trợ lý ơi」** hoặc nút trên thông báo (bỏ VoiceInteraction / ASSIST hệ thống)
- Overlay: scrim chỉ khi hiện sheet; TTS không bị cắt sớm; xác nhận Hủy/Xác nhận dạng hội thoại; warm RAM / unload GGUF khi cần

### Fixed

- Overlay tối màn hình trước khi popup; độ trễ STT sau wake; dismiss cắt TTS; UX xác nhận gọi/SMS trên overlay

### Docs

- README, SCHEMA, BUILD, CONTRIBUTING: Trợ lý nổi; ảnh screenshot overlay đang lắng nghe

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
