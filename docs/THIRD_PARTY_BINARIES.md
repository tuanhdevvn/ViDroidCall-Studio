# Third-party binaries bundled in this repository

These files are **unmodified** upstream artifacts. ViDroidCall Studio does not
patch their source or weights. They are included so `./gradlew assembleDebug`
produces a runnable APK without a separate native build.

Maven/Gradle already supplies Jetpack, Kotlin, and `llamacpp-kotlin`.
GGUF NLU weights are **not** stored in git (hundreds of MB to 1.6 GB).

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

Upstream package:
[csukuangfj/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09](https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09)
(Apache-2.0).

| File | Role |
| :--- | :--- |
| `encoder.int8.onnx` / `decoder.onnx` / `joiner.int8.onnx` | Zipformer transducer |
| `tokens.txt` / `bpe.model` | Vocabulary / SentencePiece |
| `silero_vad.onnx` | Silero VAD (MIT) |

Replace by downloading the Hugging Face repo and copying those files into
`app/src/main/assets/sherpa-onnx-vi/`.

## GGUF NLU models (not in this repo)

Qwen3 0.6B NLU weights (GGUF `Q4_K_M`) are loaded at runtime from the device
Download folder. Official weights:

[tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6](https://huggingface.co/tuanhdev/vidroidcall-qwen3-0.6B-nlu-gguf-v6)

File: `qwen3-nlu-run-006-Q4_K_M.gguf`. Install steps:
[02-Huong_dan_tai_va_nap_model_GGUF.md](02-Huong_dan_tai_va_nap_model_GGUF.md).

- Engine: llama.cpp via `io.github.ljcamargo:llamacpp-kotlin:0.4.0` (MIT)
- Base model family: [Qwen3](https://github.com/QwenLM/Qwen3) (Apache-2.0)
- App source that consumes this model: [ViDroidCall-Studio](https://github.com/tuanhdevvn/ViDroidCall-Studio)

## Kotlin API copy

`app/src/main/java/com/k2fsa/sherpa/onnx/` is the upstream Sherpa-ONNX Android
API (Xiaomi / k2-fsa, Apache-2.0). Copyright headers in those files are left
intact.
