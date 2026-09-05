# Changelog

All notable changes to ViDroidCall Studio are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-05

First public open-source release (Apache License 2.0).

### Added

- Offline Vietnamese speech-to-text with Sherpa-ONNX Zipformer 30M Int8 and Silero VAD
- Hybrid NLU: Fast-Path rule matching (sub-5ms) plus on-device Llama.cpp / Qwen2.5 GGUF for complex utterances
- Native actions: call contact, SMS, alarm, timer, open app, maps, play music, search video
- Elderly-friendly UX: large type, 3-step permission guides, action confirmation, 4-stage speech card, TTS feedback
- Command history, theme and font-size settings, onboarding
- GitHub Actions CI (compile + unit tests)

### License

- Project source is licensed under Apache-2.0 (`LICENSE`, `NOTICE`)
- Third-party components are listed in `OPEN_SOURCE_LICENSES.md`
