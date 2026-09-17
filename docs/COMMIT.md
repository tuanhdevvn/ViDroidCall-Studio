# Chuẩn commit — ViDroidCall Studio

Mọi commit trên `main` và PR phải đọc được trên GitHub, map được sang [issue](https://github.com/tuanhdevvn/ViDroidCall-Studio/issues), và không lẫn file máy local.

Định dạng: [Conventional Commits](https://www.conventionalcommits.org/).

---

## Cú pháp

```text
<type>(<scope>): <subject>

<body>
```

| Phần | Bắt buộc | Quy tắc |
| :--- | :---: | :--- |
| `type` | Có | Một trong bảng dưới |
| `scope` | Không | Module ngắn: `home`, `overlay`, `stt`, `nlu`, `settings`, `ci` |
| `subject` | Có | Tiếng Việt **không dấu** (`đ` → `d`), ≤ 72 ký tự, không chấm cuối |
| `body` | Không | Tiếng Việt có dấu; giải thích **vì sao**, không liệt kê diff |

Subject dùng thì hiện tại, kiểu mệnh lệnh: *sua*, *them*, *an*, *gop* — không *da sua*, không *fixing*.

Đúng:

```text
ux(home): noi bat nut Home/mic, icon to hon, bo vien bar

Nút Home bị chìm trong menu bar; icon lớn hơn và bỏ viền giúp người lớn tuổi nhận ra nhanh hơn.
```

Sai:

```text
Update HomeScreen.kt
fix bug
feat: Nổi bật nút Home...
feat(home): Highlight the home FAB.
```

---

## Type

`type` trong commit **không** phải tên label GitHub. Map giống title issue `[loại]`.

| Type | Khi nào | Label PR / issue |
| :--- | :--- | :--- |
| `feat` | Tính năng mới | enhancement |
| `fix` | Sửa hành vi sai (crash, logic, regression) | bug |
| `ux` | Chỉ giao diện / a11y, không đổi hành vi NLU/STT | enhancement |
| `nlu` | Fast-Path, GGUF, schema JSON | bug hoặc enhancement |
| `stt` | Sherpa-ONNX, VAD, ITN | bug hoặc enhancement |
| `docs` | README, BUILD, CONTRIBUTING, giấy phép | documentation |
| `ci` | GitHub Actions, Gradle test job | bug nếu pipeline gãy |
| `chore` | Version, ignore, hook, phụ thuộc không đổi hành vi | — |
| `perf` | Nhanh hơn, cùng hành vi | enhancement |
| `refactor` | Đổi cấu trúc, cùng hành vi | — |
| `test` | Chỉ unit test | — |

Một commit = một việc. Không gộp `feat` + `docs` + `chore` trừ khi tài liệu là phần không tách được của cùng thay đổi.

---

## Scope gợi ý

| Scope | Thư mục / module |
| :--- | :--- |
| `home` | Màn trợ lý, menu bar, pager tab |
| `overlay` | Trợ lý nổi |
| `settings` | Cài đặt, theme, cỡ chữ |
| `history` | Lịch sử câu lệnh |
| `stt` | Sherpa, VAD, formatter |
| `nlu` | Fast-Path, Llama.cpp / GGUF |
| `kws` | Wake word |
| `ci` | `.github/workflows` |
| `license` | `OPEN_SOURCE_LICENSES.md`, NOTICE |

Bỏ `scope` nếu thay đổi trải nhiều module và không có điểm nhấn.

---

## Body và tham chiếu issue

- Viết **tại sao** thay đổi, không copy `git diff`.
- Gắn issue: `(#67)` ở cuối subject, hoặc `Closes #67` / `Fixes #67` ở body khi commit **đóng** issue.
- Không dán SĐT, danh bạ, nội dung SMS thật.
- Không bịa SHA, số đo, log.

```text
nlu(settings): luu mau sai JSON sau cau GGUF (#67)

Fast-Path không lưu. Thẻ JSON trên màn trợ lý + danh sách / xóa / share trong Cài đặt để R&D sửa rule.

Closes #67
```

---

## Nhánh

Tên nhánh tiếng Việt không dấu, có số issue:

| Prefix | Việc |
| :--- | :--- |
| `feature/<issue>-<tom-tat>` | Tính năng / UX / NLU mới |
| `bugfix/<issue>-<tom-tat>` | Sửa lỗi |

Ví dụ: `feature/67-luu-mau-sai-json-gguf`, `bugfix/41-stt-khong-chuyen-giai-doan-3`.

CI chạy trên push của `feature/**` và `bugfix/**`. PR nhắm `main`.

---

## Trước khi `git commit`

1. Chỉ stage file của **đúng việc này**.
2. Chạy `./gradlew testDebugUnitTest` nếu đụng Kotlin / native / CI.
3. Subject khớp type; body giải thích lý do nếu không hiển nhiên.

Không commit:

* `local.properties`, `.idea/`, khóa máy, `.env`
* `.gguf` ngoài `models/` (Git LFS); không đổi tên `qwen3-nlu-Q4_K_M.gguf`
* `.bin` linh tinh, `.so` / ONNX chưa ghi nguồn — xem [THIRD_PARTY_BINARIES.md](THIRD_PARTY_BINARIES.md)
* Trailer Cursor: `Co-authored-by: Cursor`, `Made-with: Cursor` (hook `.githooks/commit-msg` gỡ nếu đã cài)

---

## Merge PR

* GitHub **Squash and merge** cho PR một việc: một commit trên `main`, subject theo chuẩn này (sửa lại title GitHub nếu bot điền tên nhánh).
* Giữ merge commit chỉ khi PR thật sự gồm nhiều commit độc lập cần lịch sử.

Sau khi merge vào `main`, cập nhật [CHANGELOG.md](../CHANGELOG.md) mục `[Unreleased]` nếu thay đổi người dùng nhìn thấy (Added / Changed / Fixed / Docs).
