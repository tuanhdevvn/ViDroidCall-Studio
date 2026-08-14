# 🎙️ Emma ViDroidCall - Trợ Lý Giọng Nói Thông Minh

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![DataStore](https://img.shields.io/badge/Storage-Jetpack%20DataStore-orange.svg)](https://developer.android.com/topic/libraries/architecture/datastore)
[![MinSDK](https://img.shields.io/badge/Min%20SDK-26-brightgreen.svg)](https://android.com)

**Emma ViDroidCall** là ứng dụng trợ lý giọng nói ảo tiếng Việt hiện đại dành cho hệ điều hành Android, được xây dựng hoàn toàn bằng **Jetpack Compose** và **Material 3**. Ứng dụng cung cấp khả năng nhận diện giọng nói, thực thi các tác vụ rảnh tay (gọi điện, mở ứng dụng, đặt báo thức, nhắn tin) cùng giao diện người dùng mượt mà, hỗ trợ chuyển đổi chủ đề Sáng / Tối (Light & Dark theme) tức thì.

---

## ✨ Tính Năng Nổi Bật

### 1. 🎤 Trợ Lý Ảo Giọng Nói (Emma Assistant)
- Nhận diện giọng nói tiếng Việt thời gian thực thông qua `rememberSpeechToText`.
- Hiệu ứng hoạt họa sóng âm thanh (pulse / breathing wave) và vòng hào quang phản hồi theo trạng thái lắng nghe.
- Mascot Emma chuyển động lơ lửng kèm bong bóng hội thoại tương tác sinh động với hiệu ứng gõ chữ (typing effect).
- Danh sách gợi ý câu lệnh nhanh (Gọi điện, Nhắn tin, Báo thức, Phát nhạc, Chỉ đường).

### 2. 📜 Lịch Sử Câu Lệnh (Command History)
- Quản lý và theo dõi nhật ký các câu lệnh đã thực hiện (Cuộc gọi, Tin nhắn, Ứng dụng, Hệ thống).
- Nút bấm chạy lại lệnh nhanh chỉ với 1 chạm.

### 3. 🎨 Cài Đặt Giao Diện & Theme Động (DataStore Backed)
- Hỗ trợ 3 chế độ chủ đề:
  - ☀️ **Giao diện Sáng (Trắng)**: Tông màu sáng thanh lịch, rõ nét.
  - 🌙 **Giao diện Tối (Đen)**: Tông màu tối dịu mắt, tiết kiệm pin cho màn hình OLED/AMOLED.
  - 📱 **Theo hệ thống**: Tự động đồng bộ theo cài đặt chế độ của thiết bị.
- Lưu trữ cấu hình bằng **Jetpack DataStore Preferences** và tự động đồng bộ tức thì trên toàn bộ ứng dụng.

### 4. 🧭 Thanh Điều Hướng Tùy Biến (Custom Bottom Navigation Bar)
- Thanh menu phía dưới với thiết kế đường cong lõm độc đáo (Custom Cutout Notch Shape).
- Nút Micro FAB trung tâm nhô cao với hiệu ứng chạm co giãn (`bounceClick`) và hào quang phát sáng.

### 5. 🚀 Luồng Giới Thiệu (Onboarding Flow)
- Hướng dẫn người dùng các tính năng cốt lõi với `HorizontalPager` mượt mà.
- Lưu trạng thái đã hoàn thành onboarding vào DataStore để chỉ hiển thị ở lần đầu mở app.

---

## 🛠️ Công Nghệ & Thư Viện Sử Dụng

- **Ngôn ngữ**: [Kotlin](https://kotlinlang.org/)
- **Giao diện (UI)**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material Design 3](https://m3.material.io/)
- **Quản lý trạng thái & Phản ứng (State & Reactivity)**: Kotlin Coroutines & StateFlow / Flow
- **Điều hướng (Navigation)**: [Jetpack Navigation Compose](https://developer.android.com/guide/navigation/navigation-compose)
- **Lưu trữ cục bộ (Local Storage)**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Nhận diện giọng nói (Speech Recognition)**: Android SpeechRecognizer API
- **Hiệu ứng đồ họa (Animations)**: Compose Animation API (Springs, Transitions, Infinite Transitions, GraphicsLayers)

---

## 📁 Cấu Trúc Dự Án (Project Structure)

```text
com.example.emma_vidroidcall/
│
├── MainActivity.kt                      # Activity chính, quan sát Theme từ DataStore
│
├── data/
│   └── local/
│       ├── OnboardingPreferences.kt     # Lưu trạng thái onboarding vào DataStore
│       └── ThemePreferences.kt          # Lưu cấu hình Theme (Light/Dark/System)
│
├── feature/
│   ├── assistant/
│   │   └── AssistantScreen.kt           # Tab Hỏi đáp & Giao diện Trợ lý ảo Emma
│   ├── history/
│   │   ├── HistoryScreen.kt             # Tab Lịch sử câu lệnh
│   │   └── model/
│   │       └── CommandHistoryItem.kt    # Model dữ liệu câu lệnh lịch sử
│   ├── home/
│   │   └── HomeScreen.kt                # Màn hình chính điều phối các tab
│   ├── onboarding/
│   │   └── OnbroadingScreen.kt          # Màn hình Onboarding giới thiệu ứng dụng
│   ├── settings/
│   │   ├── SettingsScreen.kt            # Tab Cài đặt tổng quan
│   │   └── ThemeSelectionScreen.kt      # Trang chọn Theme Sáng / Tối
│   └── speech/
│       └── SpeechToTextManager.kt       # Xử lý nhận diện giọng nói
│
├── navigation/
│   ├── AppNavHost.kt                    # Điều hướng Onboarding <-> Home
│   ├── AppRoot.kt                       # Gốc ứng dụng kiểm tra trạng thái khởi chạy
│   └── AppRoute.kt                      # Định nghĩa các Route điều hướng
│
└── ui/
    ├── component/
    │   └── CustomBottomMenuBar.kt       # Custom Bottom Bar với rãnh lõm và nút Mic FAB
    └── theme/
        ├── Color.kt                     # Định nghĩa bảng màu thương hiệu & giao diện
        ├── Shape.kt                     # Định nghĩa bo góc hình học
        ├── Theme.kt                     # Định nghĩa ColorScheme (Light & Dark Theme)
        └── Type.kt                      # Cấu hình Typography
```

---

## 🚀 Cài Đặt & Chạy Ứng Dụng (Getting Started)

### Yêu cầu môi trường
- **Android Studio**: Koala / Ladybug hoặc phiên bản mới hơn
- **JDK**: Java 11 hoặc Java 17
- **Min SDK**: API Level 26 (Android 8.0)
- **Target SDK**: API Level 36
- **Compile SDK**: API Level 37.1

### Các bước biên dịch
1. Clone dự án về máy tính:
   ```bash
   git clone <repo-url>
   cd EmmaViDroidCall
   ```
2. Mở dự án bằng **Android Studio**.
3. Sync Gradle và biên dịch ứng dụng:
   ```bash
   ./gradlew assembleDebug
   ```
4. Cài đặt và chạy trên thiết bị Android hoặc Emulator có hỗ trợ Micro/Google Speech Services.

---

## 📄 Bản Quyền (License)

Dự án được phát triển và quản lý bởi tác giả.
