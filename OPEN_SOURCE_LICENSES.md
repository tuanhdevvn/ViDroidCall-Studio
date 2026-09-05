# 📜 Giấy Phép Mã Nguồn Mở (Open Source Licenses)

Mã nguồn **ViDroidCall Studio** được cấp phép theo **Apache License 2.0** (OSI-approved). Xem [LICENSE](LICENSE) và [NOTICE](NOTICE).

Dự án còn sử dụng các thư viện, mô hình trí tuệ nhân tạo (AI) và công cụ mã nguồn mở của bên thứ ba (Third-Party Open Source Software). Chúng tôi xin chân thành cảm ơn cộng đồng các nhà phát triển và các tổ chức đã đóng góp vào các dự án mã nguồn mở này.

Dưới đây là danh sách đầy đủ các thành phần mã nguồn mở được tích hợp trong dự án:

---

## 📊 Bảng Tổng Hợp Thư Viện & Mô Hình AI

| STT | Tên Thành Phần / Thư Viện | Tác Giả / Tổ Chức | Mục Đích Sử Dụng | Loại Giấy Phép | Liên Kết Mã Nguồn |
|:---:|:---|:---|:---|:---:|:---|
| 1 | **Sherpa-ONNX** | Next-gen Kaldi / k2-fsa Team | Nhận diện giọng nói (Speech-to-Text) & VAD On-Device 100% Offline | **Apache-2.0** | [GitHub Repo](https://github.com/k2-fsa/sherpa-onnx) |
| 2 | **ONNX Runtime** | Microsoft Corporation | Engine tăng tốc suy luận mô hình nơ-ron cục bộ trên thiết bị | **MIT License** | [GitHub Repo](https://github.com/microsoft/onnxruntime) |
| 3 | **Silero VAD** | Silero Team | Mô hình phát hiện điểm ngắt / khoảng lặng giọng nói chính xác cao | **MIT License** | [GitHub Repo](https://github.com/snakers4/silero-vad) |
| 4 | **Zipformer Vietnamese Model (30M Int8)** | Fangjun Kuang / k2-fsa | Mô hình mạng nơ-ron nhận dạng tiếng Việt nén Int8 tối ưu di động | **Apache-2.0** | [HuggingFace Repo](https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09) |
| 5 | **Android Jetpack & Jetpack Compose** | Google LLC | Bộ công cụ phát triển giao diện hiện đại & Quản lý vòng đời ứng dụng | **Apache-2.0** | [Android Open Source](https://android.googlesource.com) |
| 6 | **Kotlin & Kotlinx Coroutines** | JetBrains s.r.o. | Ngôn ngữ lập trình chính & Quản lý luồng bất đồng bộ (Asynchronous) | **Apache-2.0** | [GitHub Repo](https://github.com/JetBrains/kotlin) |
| 7 | **Material Components for Android 3** | Google LLC | Hệ thống thiết kế giao diện chuẩn Material You (M3) | **Apache-2.0** | [GitHub Repo](https://github.com/material-components/material-components-android) |
| 8 | **JSON in Java (org.json)** | JSON.org | Phân tích và xử lý cấu trúc dữ liệu JSON NLU | **Apache-2.0 / JSON** | [GitHub Repo](https://github.com/stleary/JSON-java) |

---

## 📑 Chi Tiết Các Giấy Phép Mã Nguồn Mở

### 1. Apache License, Version 2.0
Áp dụng cho: **Sherpa-ONNX**, **Zipformer Vietnamese Model**, **Android Jetpack / Compose**, **Kotlin / Coroutines**, **Material Design Components**.

```text
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

---

### 2. The MIT License (MIT)
Áp dụng cho: **ONNX Runtime (Microsoft)**, **Silero VAD (Silero Team)**.

```text
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🔒 Tuyên Bố Miễn Trừ Trách Nhiệm
Mọi bản quyền phần mềm, nhãn hiệu thương mại và tài nguyên mã nguồn mở thuộc về các tác giả và tổ chức sở hữu tương ứng. Dự án **ViDroidCall Studio** tuân thủ đầy đủ các điều khoản phân phối và quyền sử dụng được quy định trong các giấy phép trên.
