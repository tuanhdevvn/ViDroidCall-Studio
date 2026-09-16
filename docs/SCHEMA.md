# Schema dự án ViDroidCall Studio

Mô tả cấu trúc repo, JSON NLU, hành động native, lưu trữ local và điều hướng.
Mã nguồn: `com.example.ViDroidCall_Studio`.

Build: [BUILD.md](BUILD.md). Binary bên thứ ba: [THIRD_PARTY_BINARIES.md](THIRD_PARTY_BINARIES.md). GGUF: [models/README.md](../models/README.md). Trợ lý nổi: §8.

---

## 1. Cây repo

```
ViDroidCall-Studio/
├── app/                    # Module Android
│   └── src/main/
│       ├── java/com/example/ViDroidCall_Studio/
│       ├── java/com/k2fsa/sherpa/onnx/   # API Sherpa-ONNX (upstream)
│       ├── assets/fast_path_rules.json
│       ├── assets/sherpa-onnx-vi/        # Zipformer + Silero VAD
│       └── jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}/
├── models/                 # GGUF Git LFS (không nằm trong APK)
├── docs/
├── gradle/
└── .github/workflows/
```

Package Kotlin của nhóm:

| Thư mục | Vai trò |
| :--- | :--- |
| `data/model` | `NluResult`, enum intent / status / risk, parser JSON |
| `data/nlu` | Fast-Path, Llama.cpp, `NluEngineManager`, dispatcher |
| `data/local` | DataStore (theme, font, onboarding, Trợ lý nổi) |
| `data/local/history` | SQLite lịch sử lệnh (10 câu STT mới nhất) |
| `data/local/habit` | SQLite lệnh hay dùng (`habit_actions` + events; không lấy từ history) |
| `data/local/feedback` | JSONL phản hồi NLU (nếu bật) |
| `domain/model` | `NativeAction` (thao tác Android) |
| `feature/assistant` | Màn Home, helper khóa màn hình |
| `feature/overlay` | Hộp thoại nổi, wake word, foreground service |
| `feature/*` | `home`, `history`, `settings`, `onboarding`, `speech` |
| `navigation` | `AppRoute` |
| `ui/component`, `ui/theme` | UI dùng chung, dialog quyền overlay |
| `util` | Contact, app, `TroLyNoiPermissions`, quyền lưu trữ |

---

## 2. Luồng dữ liệu

```
Vào lệnh:
  Home mic (chỉ khi NLU Ready)
  hoặc Trợ lý nổi: “Trợ lý ơi” / thông báo / trợ lý hệ thống
        ↓
STT (Sherpa, assets) → text
        ↓
Fast-Path (fast_path_rules.json + regex) ──khớp──► NluResult (isFastPath=true)
        ↓ không khớp
LLM GGUF (ChatML, Qwen3) ──────────────────────► NluResult (isFastPath=false)
        ↓
NativeAction.fromNluResult
        ↓
Home: xác nhận nếu requiresConfirmation
Overlay: xác nhận mọi thao tác native (ẩn số điện thoại)
        ↓
NluActionDispatcher → Intent Android
```

Micro **trên tab Home** chỉ chạy khi `NluModelState.Ready` (đã nạp `.gguf` trên máy). File GGUF chuẩn: `qwen3-nlu-run-Q4_K_M.gguf` trong thư mục Download.

Wake word Trợ lý nổi dùng Sherpa (không cần GGUF). Câu sau wake word: Fast-Path không gọi LLM; không khớp Fast-Path thì **cần** GGUF Ready.

**GGUF không** đọc từ thư mục clone. **STT ONNX** nằm trong APK (`assets` + `jniLibs`).

---

## 3. JSON NLU (hợp đồng Fast-Path và LLM)

Cả hai nhánh trả cùng hình dạng. Parser chấp nhận `slots` như bí danh của `arguments`.

```json
{
  "intent": "open_app",
  "arguments": {},
  "risk_level": "low",
  "status": "success",
  "requires_confirmation": false
}
```

| Trường | Kiểu | Giá trị |
| :--- | :--- | :--- |
| `intent` | string | Xem bảng intent |
| `arguments` | object | Slot theo intent |
| `risk_level` | string | `low` \| `medium` \| `high` |
| `status` | string | `success` \| `needs_clarification` \| `invalid` \| `unsupported` |
| `requires_confirmation` | boolean | `call_contact` / `send_sms` luôn buộc `true` khi map sang `NativeAction` |

Định nghĩa: `data/model/NluModels.kt`. Prompt ChatML: `data/nlu/NluConstants.kt`.

Runtime thêm (không bắt LLM in ra): `rawJson`, `argumentsJson`, `isParsedSuccessfully`, `errorMessage`, `isFastPath`, `slots`, `executionId`.

### Intent

| `intent` | Enum | Arguments |
| :--- | :--- | :--- |
| `set_alarm` | SET_ALARM | `hour` (int), `minute` (int), tùy chọn `label` / `message` |
| `set_timer` | SET_TIMER | `duration` (int), `unit`: `hours` \| `minutes` \| `seconds`, tùy chọn `label` |
| `open_app` | OPEN_APP | `app_name` (string) |
| `open_map` | OPEN_MAP | `destination` hoặc `query` (string) |
| `call_contact` | CALL_CONTACT | `contact` (string), tùy chọn `phone_number` |
| `send_sms` | SEND_SMS | `contact`, tùy chọn `phone_number`, `message` |
| `search_video` | SEARCH_VIDEO | `query` |
| `play_music` | PLAY_MUSIC | tùy chọn `song_name`, `artist`, `genre` |
| `search_web` | SEARCH_WEB | `query` (trống → UX làm rõ) |
| `clarify` | CLARIFY | thường `missing`: mảng string; `status` = `needs_clarification` |
| `greeting` | GREETING | `{}` |
| `goodbye` | GOODBYE | `{}` |
| `unsupported` | UNSUPPORTED | thường `{}` |

Intent lạ khi parse → `unsupported`.

Sau LLM: nếu mô hình trả `call_contact` nhưng câu kiểu “X là ai / là gì / nghĩa là gì” thì engine đổi thành `search_web`.

---

## 4. NativeAction

`domain/model/NativeAction.kt` — sealed class, mỗi phần tử có `actionId`, `intentName`, `requiresConfirmation`.

| Lớp | Intent | Trường chính |
| :--- | :--- | :--- |
| `OpenApp` | `open_app` | `appName` |
| `CallContact` | `call_contact` | `contact`, `phoneNumber` (xác nhận) |
| `SendSms` | `send_sms` | `contact`, `phoneNumber`, `message` (xác nhận) |
| `OpenMap` | `open_map` | `destination` |
| `SetAlarm` | `set_alarm` | `hour`, `minute`, `label` |
| `SetTimer` | `set_timer` | `durationSeconds`, `displayDuration`, `unitText`, `label` |
| `SearchVideo` | `search_video` | `query` |
| `PlayMusic` | `play_music` | `songName`, `artist`, `genre`, `musicQuery` |
| `SearchWeb` | `search_web` | `query` |
| `Informational` | `greeting` / `goodbye` / `clarify` | `message`, `speechText` |
| `Unsupported` | `unsupported` | `message` |

---

## 5. Trạng thái mô hình NLU

`NluEngineManager` — `sealed interface NluModelState`:

| Trạng thái | Ý nghĩa |
| :--- | :--- |
| `Uninitialized` | Chưa quét |
| `Loading` | Đang nạp GGUF |
| `Ready(modelPath)` | Micro được phép ghi âm |
| `ModelNotFound` | Không có `.gguf` trong thư mục quét |
| `Error(message)` | Lỗi nạp |

Thư mục quét (ưu tiên tên `qwen3-nlu-run-Q4_K_M.gguf`, rồi bất kỳ `*.gguf`): filesDir ngoài / Download app / filesDir / Download công khai / `/sdcard/Download`.

---

## 6. Lưu trữ local

Không dùng Room.

### SQLite — `vidroidcall_history.db`

Bảng `command_history`, tối đa **10** dòng mới nhất.

| Cột | Kiểu |
| :--- | :--- |
| `id` | INTEGER PK AUTOINCREMENT |
| `command_text` | TEXT NOT NULL |
| `category` | TEXT NOT NULL (mặc định `"Hệ thống"`) |
| `status` | TEXT NOT NULL (mặc định `"Thành công"`) |
| `time_formatted` | TEXT NOT NULL |
| `timestamp` | INTEGER NOT NULL |

UI: `CommandHistoryItem(id, commandText, time, status, category, timestamp)`.

### SQLite — `vidroidcall_habit.db`

Bảng `habit_actions` / `habit_events` / `habit_meta`. **Không** nới `command_history` để tính tần suất.

Ghi khi user đã thực thi (`NativeAction` đủ điều kiện: không `greeting` / `goodbye` / `clarify` / `unsupported`). Unique `intent|slot`. Nhãn kiểu `Gọi Mai`, JSON đủ `executeNativeAction`.

Top 5: `hits ≥ 2` trong 14 ngày; loại `last_used` > 30 ngày; snapshot 1 lần/ngày (`habit_meta`). Dòng không event 60 ngày thì xóa. UI: khối trên tab Lịch sử.

### DataStore Preferences

| Store | Key | Kiểu | Giá trị |
| :--- | :--- | :--- | :--- |
| `theme_preferences` | `theme_mode` | string | `light` \| `dark` \| `system` (mặc định `light`) |
| `font_size_preferences` | `font_scale` | float | mặc định `1.0`, khoảng `0.85`–`1.35` |
| `onboarding_preferences` | `onboarding_completed` | boolean | mặc định `false` |
| `tro_ly_noi_preferences` | `tro_ly_noi_enabled` | boolean | mặc định `false`; UI chỉ persist `true` khi đủ mic, thông báo (API 33+), `SYSTEM_ALERT_WINDOW`. Wake word **đồng bộ** với công tắc này (không còn công tắc riêng). |

### JSONL (tùy chọn)

`getExternalFilesDir(null)/nlu_feedback_log.jsonl`

```json
{ "stt_text": "...", "model_output": {}, "saved_at": 0 }
```

---

## 7. Điều hướng UI

| Route (`AppRoute`) | Màn |
| :--- | :--- |
| `onboarding` | Onboarding (nếu chưa hoàn thành DataStore) |
| `home` | Tab host |

Tab trong Home (`NavTab`, không phải NavController):

| Tab | Title | Màn |
| :--- | :--- | :--- |
| `HISTORY` | Lịch sử | `HistoryScreen` |
| `ASSISTANT` | Hỏi đáp | `AssistantScreen` (mặc định) |
| `SETTINGS` | Cài đặt | `SettingsScreen` |

Trợ lý nổi **không** phải `AppRoute`. `TroLyNoiOverlayManager` gắn `ComposeView` vào `WindowManager` (`TYPE_APPLICATION_OVERLAY`).

---

## 8. Trợ lý nổi

| Thành phần | Vai trò |
| :--- | :--- |
| `TroLyNoiPreferences` | Công tắc master; mặc định tắt |
| `TroLyNoiPermissions` | Thứ tự quyền: mic → thông báo (API 33+) → `SYSTEM_ALERT_WINDOW` |
| `TroLyNoiForegroundService` | `foregroundServiceType=microphone`; thông báo đang chờ; pre-warm STT |
| `TroLyNoiWakeWordManager` | Lắng nghe từ khóa khi màn hình sáng, đã mở khóa, overlay không đang hiện |
| `TroLyNoiOverlayManager` | STT + Fast-Path / GGUF + hộp xác nhận trên overlay |
| `TroLyNoiSheet` | UI hộp thoại; không hiện huy hiệu Fast-Path / GGUF |

Cách mở overlay: wake word **「Trợ lý ơi」** / **「Trợ lý」** (biến thể dấu `trợ lí`), hoặc **nút nhanh trên thông báo** (`ACTION_SHOW_OVERLAY`). Không còn trợ lý mặc định hệ thống (`VoiceInteraction` / `ACTION_ASSIST`).

`AssistantOverlayState` (UI): `LISTENING`, `STT`, `FAST_PATH`, `ANALYZING` / `GGUF_LOADING`, `CONFIRM_CALL`, `MAP_CONFIRM`, `CONFIRM_ACTION`, `CONVERSATIONAL_REPLY`. Overlay xác nhận **mọi** `NativeAction` native; phản hồi hội thoại dùng nút「Nói tiếp」. Tóm tắt gọi/SMS trên overlay dùng `getActionSummary()` (ẩn số).

Wake word **tạm dừng** khi: overlay đang mở, màn hình tắt, hoặc keyguard khóa.

---

## 9. Artifact trên thiết bị / APK

| Thành phần | Vị trí |
| :--- | :--- |
| Zipformer + VAD | `assets/sherpa-onnx-vi/*.onnx`, `tokens.txt`, `bpe.model` |
| `libsherpa-onnx-jni.so`, `libonnxruntime.so` | `jniLibs/<abi>/` |
| Fast-Path rules | `assets/fast_path_rules.json` |
| GGUF NLU | Download trên máy; bản nguồn: `models/qwen3-nlu-run-Q4_K_M.gguf` |
