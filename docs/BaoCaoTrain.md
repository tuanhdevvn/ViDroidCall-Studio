# Báo cáo fine-tune NLU tiếng Việt

**Người thực hiện:** Nguyễn Tuấn Anh  
**Thời gian:** 09/2026  
**Mục đích:** Tự fine-tune mô hình NLU cho app trợ lý giọng nói (STT → JSON), không dùng model NLU có sẵn.

---

## 1. Việc đã làm

Luồng trên máy:

```text
Giọng nói (app) → STT → văn bản tiếng Việt → NLU (model tự train) → JSON → validator → thực thi
```

| Hạng mục | Chi tiết |
|----------|----------|
| Base | Qwen3-0.6B (mô hình gốc, chưa phải NLU) |
| Cách train | LoRA, rank 16, Apple Silicon (MLX) |
| Việc model làm | 11 intent tiếng Việt → **một JSON** (không chat, không markdown) |
| Dữ liệu | Tự sinh + kiểm tra schema (báo thức, gọi điện, SMS, bản đồ, nhạc, web, YouTube, hỏi lại, từ chối…) |
| Đầu ra app | GGUF Q4_K_M ~378 MB |

JSON mẫu:

```json
{"intent":"set_alarm","arguments":{"hour":6,"minute":0},"risk_level":"low","status":"success","requires_confirmation":false}
```

Cách chứng minh đã train: có checkpoint LoRA từng vòng, báo cáo eval, và file GGUF xuất từ adapter đó — không phải tải model NLU hoàn chỉnh từ ngoài.

---

## 2. Cách chọn bản (không liệt kê hết vòng train)

Train nhiều vòng: mỗi vòng **đánh giá** rồi mới giữ hoặc bỏ. Báo cáo này chỉ giữ **3 bản đại diện**.

| Cột | Ý nghĩa | Ngưỡng tối thiểu |
|-----|---------|------------------|
| Intent | Đúng loại lệnh | ≥ 90% |
| Slot | Đúng giờ / tên / địa điểm… | ≥ 85% |
| JSON | Output parse được | ≥ 98% |
| Hallu | Bịa slot | ≤ 5% |
| Unsup | Từ chối lệnh ngoài phạm vi | ≥ 90% |
| ASR Δ | Lệch khi câu không dấu / STT bẩn | ≤ 8% |
| Smoke | 14 câu lệnh thật | 14/14 |
| hard-v3 | Bộ câu khó (già, cụt, nhiễu) | càng cao càng tốt |

---

## 3. Ba bản đại diện

| | **run-003** Pilot | **run-014** Giao hàng lần 1 | **run-017** Chọn cho sản phẩm |
|--|-------------------|-----------------------------|-------------------------------|
| Vai trò | Chứng minh pipeline train chạy | Bản ổn định đầu tiên | Bản đang dùng |
| Intent | 100%* | 98.2% | 97.9% |
| Slot F1 | 96.3%* | **98.0%** | 97.2% |
| JSON | 100% | 100% | 100% |
| Hallu | 0%* | 0.7% | 0.8% |
| Unsup | 100%* | **96.8%** | 92.5% |
| ASR Δ | — | 3.2% | **0.4%** |
| Smoke | 10/10 | **14/14** | **14/14** |
| hard-v3 | — | 93.5% | **96.4%** |

\*run-003 test nhỏ (pilot). run-014/017 chấm trên bộ test lớn hơn (~1 600 mẫu).

---

## 4. Lý do chọn run-017 cho sản phẩm (so với run-014)

Cả hai đều **đạt ngưỡng MVP**, smoke **14/14**, JSON **100%**. Không chọn 017 vì “số vòng lớn hơn”, mà vì app chạy **lời nói thật qua STT**, không phải câu gõ sạch.

### 4.1. Việc app cần nhất: chịu STT bẩn

Người dùng nói không dấu, nuốt chữ, STT sai nhẹ. Đo bằng **ASR Δ** (độ tụt intent so với câu sạch; càng thấp càng tốt, tối đa cho phép 8%).

| | run-014 | run-017 |
|--|---------|---------|
| ASR Δ | 3.2% | **0.4%** |
| Intent câu sạch | 98.3% | 98.0% |
| Intent câu kiểu STT | 95.1% | **97.5%** |

014 vẫn dùng được, nhưng khi STT bẩn dễ lệch loại lệnh hơn. 017 gần như **không tụt** trên nhóm này — đúng môi trường máy thật.

### 4.2. Câu khó (già, câu cụt, bẫy chỉ đường / nhạc / web)

Bộ **hard-v3** (138 câu): 014 **93.5%** (129/138), 017 **96.4%** (133/138).

Điểm lệch có ý nghĩa trên app:

| Nhóm lệnh khó | run-014 | run-017 | Hệ quả nếu chọn 014 |
|---------------|---------|---------|---------------------|
| Chỉ đường / map | 8/10 | **10/10** | Dễ mở map sai chỗ |
| Nhạc | 10/10 | **10/10** | hòa |
| Web / hỏi kiến thức | 19/20 | **20/20** | 017 chắc hơn |
| Hẹn giờ / timer | 8/10 | 9/10 | 017 ít nhầm báo thức hơn |

### 4.3. 014 hơn ở đâu — và vì sao vẫn không chọn làm bản chính

| 014 hơn | Mức | Xử lý trên sản phẩm |
|---------|-----|---------------------|
| Slot F1 | 98.0% vs 97.2% | Chênh nhỏ; cả hai đều trên ngưỡng 85% |
| Intent tổng | 98.2% vs 97.9% | Không đáng kể |
| Từ chối lệnh ngoài phạm vi | **96.8% vs 92.5%** | 017 dễ “cho qua” lệnh lạ hơn một chút → **validator app** chặn (đặt vé, bật đèn, dịch thuật, chuyển tiền… không thực thi) |

Đổi 0.4 điểm intent/slot lấy **STT ổn + chỉ đường đúng** là hợp lý cho trợ lý giọng nói. Phần 014 mạnh (OOD) bù bằng rule phía app, không cần đổi cả model.

### 4.4. Không lấy vòng train sau 017

Một số vòng sau (019, 022…) điểm hard-v3 cao hơn trên giấy, nhưng **gãy Slot F1**, **gãy bật nhạc**, hoặc **ASR Δ > 8%**. Đó là thí nghiệm, không phải bản ship. run-014 giữ làm **rollback** nếu 017 trên máy thật cho qua quá nhiều lệnh ngoài phạm vi.

---

## 5. File giao cho app

| File | Dùng |
|------|------|
| `outputs/gguf/run-017/qwen3-nlu-run-017-Q4_K_M.gguf` | **Bản chính** (378 MB) |
| `outputs/gguf/run-014/qwen3-nlu-run-014-Q4_K_M.gguf` | Rollback |

App phải gắn **system prompt NLU** (cố định) và **lớp validator** (câu cụt thì hỏi lại; 113/114/115 = khẩn; gọi/SMS phải xác nhận). File GGUF chỉ chứa trọng số.

---

## 6. Kết luận

Đã tự fine-tune Qwen3-0.6B thành NLU tiếng Việt (LoRA → GGUF).

**Bản đưa vào sử dụng: run-017** — cùng đạt MVP như 014, nhưng phù hợp máy thật hơn: STT nhiễu gần như không lệch, bộ câu khó/chỉ đường cao hơn. **run-014** giữ để rollback. Phần 014 mạnh hơn (từ chối OOD) xử lý bằng validator trên app, không đổi model.
