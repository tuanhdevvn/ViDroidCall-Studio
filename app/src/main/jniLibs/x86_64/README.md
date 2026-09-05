# Sherpa-ONNX JNI libraries

This app uses the JNI build only. Each ABI directory contains unmodified
upstream binaries:

- `libonnxruntime.so`
- `libsherpa-onnx-jni.so`

`libsherpa-onnx-c-api.so` and `libsherpa-onnx-cxx-api.so` are not shipped.
See `docs/THIRD_PARTY_BINARIES.md` for sources and replacement steps.
