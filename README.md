# AxiosFocusGuard 🛡️
### *Reclaim your focus, guard your time.*

AxiosFocusGuard is a premium Android productivity application designed to help users achieve deep work and maintain focus. By combining scientifically-backed timer presets with robust distraction blocking and AI-driven insights, it provides a comprehensive environment for high-performance work.

---

## 🚀 Key Features

- **🎯 Intelligent Timer Presets**: 
    - **Easy Task**: 25 min focus / 5 min break.
    - **Medium Task**: 40 min focus / 15 min break for deeper concentration.
    - **Hard Task**: 120 min focus / 30 min break to enter and sustain the "Flow" state.
    - **Productivity Classics**: Support for Pomodoro and the 52/17 technique.
- **🛑 Deep Focus Guard**: Real-time monitoring and blocking of distracting applications using Android Accessibility Services.
- **🤖 AI Insights**: Integration with **Google Gemini AI** to analyze session performance and provide personalized productivity coaching.
- **🎨 Premium UI/UX**: A modern, minimalist aesthetic featuring a custom circular timer, smooth animations, and a focus-oriented "Greenish-Dark" theme.
- **📊 Session Analytics**: Track your focus history and rounds completed per task level.

---

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (100%)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **State Management**: Kotlin Coroutines & Flows
- **Local Storage**: Jetpack DataStore (Preferences & Presets)
- **AI Integration**: Google Generative AI (Gemini Flash)

---

## 📦 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- Android SDK 26 (Android 8.0) or higher.
- A **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/).

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AxiosFocusGuard.git
   ```
2. Open the project in Android Studio.
3. Open your `local.properties` file and add your API key:
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
4. Build and deploy to your device.

---

## 🛡 Permissions & Security

AxiosFocusGuard takes your privacy and system security seriously. To function effectively, the app requires:
- **Accessibility Service**: Only used to detect the package name of the foreground app to trigger blocking. No personal data is collected or transmitted.
- **Display Over Other Apps**: Used to show the focus guard screen when a distracted app is opened.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Developed as part of the MP Course Project.*
