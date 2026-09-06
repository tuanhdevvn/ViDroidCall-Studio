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

## Voice assistant needs a GGUF file

The APK already includes Sherpa-ONNX STT and Fast-Path rules, but the
assistant screen **does not start the microphone** until a `.gguf` model is
loaded (`NluModelState.Ready`). Tapping the mic without a model shows a toast
and does not record.

Place `qwen3-nlu-run-006-Q4_K_M.gguf` in the device **Download** folder.

Weights: [Hugging Face Qwen3 0.6B NLU](https://huggingface.co/tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6).
See [README](../README.md) for `adb push` into `/sdcard/Download/`.

After the model is ready, short commands still use Fast-Path (no Llama.cpp).
Llama.cpp runs only when Fast-Path does not match.

## Third-party native binaries

Prebuilt `.so` files and ONNX models are unmodified upstream artifacts.
How they were obtained and how to replace them:
[THIRD_PARTY_BINARIES.md](THIRD_PARTY_BINARIES.md).
