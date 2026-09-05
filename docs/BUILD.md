# Build from source

ViDroidCall Studio is an Android app. You can compile it with the Gradle wrapper;
Android Studio is not required.

## Requirements

| Tool | Version |
| :--- | :--- |
| JDK | **21** (Temurin or equivalent) |
| Android SDK | `compileSdk` 37 / `targetSdk` 36 / `minSdk` 26 |
| Gradle | Wrapper in the repo (`./gradlew`) — do not install a separate Gradle |
| Optional | USB-debugged Android device or emulator for `installDebug` |

Confirm Java:

```bash
java -version
```

If `JAVA_HOME` points to an older JDK, set it to JDK 21 before building.

## Clone and compile

```bash
git clone https://github.com/tuanhdevvn/ViDroidCall-Studio.git
cd ViDroidCall-Studio
chmod +x gradlew
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
./gradlew installDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

The APK runs independently of the source tree after install. Native libraries
and the Zipformer STT model ship inside the APK (`jniLibs` + `assets`).

## What works without extra downloads

- Speech-to-text (Sherpa-ONNX Zipformer + Silero VAD)
- Fast-Path NLU (rules in `app/src/main/assets/fast_path_rules.json`)
- Native actions (call, SMS, alarm, timer, apps, maps, music)

## Optional: on-device LLM (GGUF)

Complex utterances that miss Fast-Path are sent to Llama.cpp. Place
`qwen3-nlu-run-006-Q4_K_M.gguf` in the device **Download** folder.

Weights: [Hugging Face Qwen3 0.6B NLU](https://huggingface.co/tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6).
Steps: [02-Huong_dan_tai_va_nap_model_GGUF.md](02-Huong_dan_tai_va_nap_model_GGUF.md).

Without a GGUF file the assistant still listens and executes Fast-Path commands.

## Third-party native binaries

Prebuilt `.so` files and ONNX models are unmodified upstream artifacts.
How they were obtained and how to replace them:
[THIRD_PARTY_BINARIES.md](THIRD_PARTY_BINARIES.md).
