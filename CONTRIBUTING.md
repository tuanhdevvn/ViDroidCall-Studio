# Contributing to ViDroidCall Studio

Thank you for helping improve this project. Contributions are licensed under the
[Apache License 2.0](LICENSE).

## Report a bug

Open an issue at [GitHub Issues](https://github.com/tuanhdevvn/Emma-ViDroidCall/issues).
Include device model, Android version, and steps to reproduce.

## Build locally

See [docs/BUILD.md](docs/BUILD.md) for full requirements. Short version:

```bash
git clone https://github.com/tuanhdevvn/Emma-ViDroidCall.git
cd Emma-ViDroidCall
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

You need **JDK 21** and an Android SDK with API 36+. Android Studio is optional;
the Gradle wrapper is enough.

Speech-to-text and Fast-Path work without a GGUF file. On-device LLM needs a
`.gguf` model in the device Download folder. See
[docs/02-Huong_dan_tai_va_nap_model_GGUF.md](docs/02-Huong_dan_tai_va_nap_model_GGUF.md).

## Pull requests

1. Create a branch from `main`.
2. Keep changes focused; match existing Kotlin style.
3. Run `./gradlew testDebugUnitTest` before opening a PR.
4. Describe the problem and how you verified the fix.
