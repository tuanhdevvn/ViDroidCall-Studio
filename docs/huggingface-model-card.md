---
license: apache-2.0
base_model: Qwen/Qwen3-0.6B
tags:
  - gguf
  - nlu
  - vietnamese
  - android
---

# ViDroidCall NLU — Qwen3 0.6B (run-006, Q4_K_M)

On-device Vietnamese NLU for **[ViDroidCall Studio](https://github.com/tuanhdevvn/ViDroidCall-Studio)** (Apache-2.0).

Fine-tune of [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B), exported as GGUF `Q4_K_M` (~397 MB).

- App source: https://github.com/tuanhdevvn/ViDroidCall-Studio
- Issues: https://github.com/tuanhdevvn/ViDroidCall-Studio/issues
- Load guide: https://github.com/tuanhdevvn/ViDroidCall-Studio/blob/main/docs/02-Huong_dan_tai_va_nap_model_GGUF.md

## File

`qwen3-nlu-run-006-Q4_K_M.gguf` — demo / contest release.

## Install on Android

```bash
adb push qwen3-nlu-run-006-Q4_K_M.gguf /sdcard/Download/
```

The app scans `.gguf` in the device Download folder. Fast-Path still works without this file.

## License

Apache-2.0. Base model: Qwen3 (Alibaba).
