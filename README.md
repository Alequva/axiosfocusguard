# AxiosFocusGuard 🛡️🐱

AxiosFocusGuard is a premium, AI-powered productivity companion for Android. It combines a robust Pomodoro timer with strict application blocking, distraction categorization, and a personalized mascot system to help users achieve deep work.

---

## 🚀 Project Overview
The app follows the **Pomodoro Technique** (Focus/Break cycles) but adds a layer of accountability. While a focus session is active, the app monitors for "distraction impulses"—attempts to open blocked apps like Instagram, TikTok, or YouTube. These attempts are logged, categorized, and analyzed by an AI coach to provide actionable feedback.

### Key Features
- **Strict App Blocking**: Uses a high-frequency polling mechanism via **UsageStats** and a **Foreground Service** to forcefully redirect users away from distractors.
- **Mascot System**: An "Alien Cat" mascot that reacts to your focus state (ZEN, YAY, THINKING, etc.).
- **AI Accountability Coach**: Integration with Google Gemini to analyze distraction patterns. The coach avoids generic advice, focusing instead on cognitive and behavioral tactics.
- **Focus Scoring**: A proprietary scoring logic based on raw attempts, "burst" impulses, and timing patterns.
- **Custom Presets**: Support for classic Pomodoro, deep work sessions, or custom user-defined intervals.

---

## 🏗️ Architecture
The project follows modern Android development practices using **Clean Architecture** principles and **MVVM** pattern.

### Package Structure
- **`com.axios.focusguard`**: Root package containing the Application class and Main Activity.
- **`data`**: Room database, DAOs, Repositories (App, AI, Presets), and the core `FocusManager`.
- **`service`**: `FocusForegroundService` handles both lifecycle management (via a persistent notification) and the active app-blocking polling loop.
- **`ui`**: Jetpack Compose screens, ViewModels, and the Theme system.
- **`util`**: Utility classes for permissions and constants.

### Key Design Decisions
1. **UsageStats Monitoring**: Switched from Accessibility Service to `UsageStatsManager` polling for better performance and reliability across different Android versions. It detects the top-most app every 200ms during active sessions.
2. **Hilt for DI**: Dependency injection is used project-wide to decouple the data layer from the UI.
3. **StateFlow & Compose**: Unidirectional data flow (UDF) is enforced, with `FocusManager` serving as the single source of truth for the timer state.

---

## 📱 Screen Inventory

| Screen | Purpose | Key Composables |
| :--- | :--- | :--- |
| **Timer** | Main focus hub with countdown and preset selection. | `TimerScreen`, `ActionButton`, `MascotImage` |
| **Results** | Post-session summary with distraction list and timeline. | `ResultsScreen`, `AppViolationSummary` |
| **Analysis** | AI-driven insights and detailed focus score breakdown. | `AnalysisScreen`, `CategorySegmentedBar`, `TimingBreakdownGrid` |
| **Settings** | Permission management and app blocking configuration. | `SettingsScreen`, `AppItem`, `PermissionSection` |

---

## 🔄 Data Flow: The Session Lifecycle

1. **Initialization**: `FocusManager` loads the active preset from `DataStore` and initializes `TimerUiState`.
2. **Starting**: User hits "Play". `FocusManager` starts `FocusForegroundService` (to prevent process death) and begins the countdown.
3. **Monitoring**: `FocusForegroundService` runs a high-priority loop that queries `UsageStatsManager.queryEvents()`.
   - If the current top package matches a blocked app, it logs a `SessionEvent` and triggers a forceful redirect back to the `MainActivity`.
4. **Scoring**: At session end, `ResultsViewModel` and `AnalysisViewModel` query the `session_events` table.
   - **Score Calculation**: Starts at 100. Deducts points for raw attempts (-2), distraction "bursts" (-8), and "early session" lapses (-15).
5. **AI Coaching**: `AiRepository` groups events into temporal bursts. A sophisticated prompt guides Gemini to provide behavioral recommendations (e.g., urge surfing, boredom tolerance) while explicitly banning generic advice like "uninstall apps."

---

## 🎨 Theme System
The app uses a custom design system defined in `Theme.kt`, focused on "Dark Mode" aesthetics and "Calm Green" tones.

- **Primary Color**: `#6BBF6B` (Calm Green)
- **Background**: `#101410` (Deep Dark Green)
- **Token Set**: Defined as `CalmGreenColors` in `Theme.kt`. 
- **Typography**: Uses the `Inter` font family (customized via `Type.kt`) for a premium, modern feel.

---

## 🐱 Mascot System (`MascotImage.kt`)
The mascot is an integral part of the UX, mapped via the `MascotPose` enum:
- **`ZEN`**: Active during focus (floating/meditating).
- **`YAY`**: Success state (perfect focus).
- **`THINKING`**: Displayed during AI analysis.
- **`CLOCK`**: Neutral focus state or session finish with distractions.
- **`SHIELD`**: Displayed in the permissions section.

---

## 🛠️ Build & Run

### Prerequisites
- **Gemini API Key**: Required for the Analysis screen. Add `GEMINI_API_KEY=your_key` to `local.properties`.
- **Permissions**: The app requires **Usage Access** (to monitor foreground apps) and **Overlay Permission** (to ensure the blocker can redirect effectively).

### Dependencies
- **UI**: Jetpack Compose, Material3, Navigation Compose.
- **Logic**: Hilt (DI), Room (Persistence), DataStore (Preferences).
- **AI**: `google.generativeai` (Gemini SDK).

---

## 📂 Project Structure Tree
```text
app/src/main/java/com/axios/focusguard/
├── data/
│   ├── AiRepository.kt         # Gemini integration & coaching logic
│   ├── AppRepository.kt        # Room & App Info bridge
│   ├── FocusManager.kt         # Core Timer & Session logic
│   ├── SessionEvent.kt         # Distraction log model
│   └── FocusSession.kt         # Historical session model
├── service/
│   └── FocusForegroundService.kt    # Polling logic & Notification
├── ui/
│   ├── analysis/               # AI Insights & Scoring
│   ├── results/                # Post-session summary
│   ├── settings/               # App selection & Permissions
│   └── timer/                  # Main Pomodoro UI
└── util/
    └── PermissionManager.kt    # System intent helpers
```
