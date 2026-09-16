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

The weights file is stored in this Git repo under `models/` via Git LFS
(`qwen3-nlu-run-Q4_K_M.gguf`). Run `git lfs pull` after clone.

The app still loads the file from the device **Download** folder (not from
the source tree). Copy it onto the phone:

```bash
adb push models/qwen3-nlu-run-Q4_K_M.gguf /sdcard/Download/
```

See [models/README.md](../models/README.md).

After the model is ready, short commands still use Fast-Path (no Llama.cpp).
Llama.cpp runs only when Fast-Path does not match.

## Floating assistant (Trợ lý nổi)

The in-app Home mic and the overlay share STT + NLU. Overlay is **off by
default**. Enable it in Settings after install:

1. Push the GGUF file (needed when Fast-Path does not match).
2. Toggle **Trợ lý nổi** and grant microphone, notifications (API 33+), and
   “Display over other apps”.
3. Say **“Trợ lý ơi”** or **“Trợ lý”** with the screen on and unlocked, or tap the
   persistent notification / **Nói câu lệnh** action.

Wake-word spotting uses Sherpa-ONNX (no GGUF). Only those two phrases (plus STT
spelling `trợ lí`) are accepted — not “alo/hey/vidroidcall…”. The overlay pauses
wake word while it is visible, and while the device is locked or the screen is off.

**Memory (hybrid / Option D):** Closing the overlay unloads the GGUF/NLU native
model from RAM (`NluEngineManager.releaseModel`). Sherpa STT and system TTS stay
warm so the next open is fast. Wake-word models stay loaded while the master
switch is on. Turning off **Trợ lý nổi** stops the foreground service and
releases remaining managers.

Details: [SCHEMA.md](SCHEMA.md) §8, [README](../README.md).

## Third-party native binaries

Prebuilt `.so` files and ONNX models are unmodified upstream artifacts.
How they were obtained and how to replace them:
[THIRD_PARTY_BINARIES.md](THIRD_PARTY_BINARIES.md).

NLU JSON, SQLite, DataStore, and navigation:
[SCHEMA.md](SCHEMA.md).
