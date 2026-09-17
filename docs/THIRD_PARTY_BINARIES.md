# Third-party binaries bundled in this repository

Native `.so` files and speech-model weights in this repo are **unmodified**
upstream artifacts. ViDroidCall Studio does not patch their code or weights.
They are included so `./gradlew assembleDebug` produces a runnable APK
without a separate native build.

The Kotlin files under `com/k2fsa/sherpa/onnx/` are an upstream API copy.
Their logic is unchanged; SPDX and the Apache 2.0 notice were added so each
file states the license (see [Kotlin API copy](#kotlin-api-copy)).

Maven/Gradle already supplies Jetpack, Kotlin, and `llamacpp-kotlin`.
GGUF NLU weights are stored in `models/` via Git LFS (~378 MB).

## Native libraries (`app/src/main/jniLibs`)

JNI build of [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) plus
[ONNX Runtime](https://github.com/microsoft/onnxruntime).

| File | Purpose | License |
| :--- | :--- | :--- |
| `libonnxruntime.so` | ONNX inference engine | MIT |
| `libsherpa-onnx-jni.so` | Sherpa-ONNX JNI (used by the app) | Apache-2.0 |

ABIs shipped: `arm64-v8a`, `armeabi-v7a`, `x86_64` (and `x86` if present).
`libsherpa-onnx-c-api.so` and `libsherpa-onnx-cxx-api.so` are **not** bundled;
the JNI path only needs the two libraries above.

### How to obtain them yourself

1. Download a Sherpa-ONNX Android release from
   [k2-fsa/sherpa-onnx releases](https://github.com/k2-fsa/sherpa-onnx/releases).
2. Copy `libonnxruntime.so` and `libsherpa-onnx-jni.so` into
   `app/src/main/jniLibs/<abi>/`.
3. Rebuild with `./gradlew assembleDebug`.

## Speech models (`app/src/main/assets/sherpa-onnx-vi`)

Weights: [hynt/Zipformer-30M-RNNT-6000h](https://huggingface.co/hynt/Zipformer-30M-RNNT-6000h)
([CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode)).

Sherpa-ONNX package (no LICENSE file on the card; files copied from `hynt`):
[csukuangfj/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09](https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09).

| File | Role |
| :--- | :--- |
| `encoder.int8.onnx` / `decoder.onnx` / `joiner.int8.onnx` | Zipformer transducer |
| `tokens.txt` / `bpe.model` | Vocabulary / SentencePiece |
| `silero_vad.onnx` | Silero VAD (MIT) |

Replace by downloading the Hugging Face repo and copying those files into
`app/src/main/assets/sherpa-onnx-vi/`.

## GGUF NLU models (Git LFS)

Qwen3 0.6B NLU weights (GGUF `Q4_K_M`) are stored in `models/` via Git LFS
and loaded at runtime from the device Download folder (not from the APK).

Repo path: [`models/qwen3-nlu-Q4_K_M.gguf`](../models/qwen3-nlu-Q4_K_M.gguf)

After `git lfs pull`, copy onto the device (`adb push` as in the [README](../README.md)).

- Engine: llama.cpp via `io.github.ljcamargo:llamacpp-kotlin:0.4.0` (MIT)
- Base model family: [Qwen3](https://github.com/QwenLM/Qwen3) (Apache-2.0)
- App source that consumes this model: [ViDroidCall-Studio](https://github.com/tuanhdevvn/ViDroidCall-Studio)

## Kotlin API copy

`app/src/main/java/com/k2fsa/sherpa/onnx/` is the upstream Sherpa-ONNX Android
API (Xiaomi / k2-fsa, Apache-2.0). This is not ViDroidCall code: keep Xiaomi
copyright, do not re-license as the team's work. SPDX and the Apache 2.0
notice were added as comments only so each file states the license; no API
or JNI behavior was changed.
