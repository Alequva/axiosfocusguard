# AxiosFocusGuard 🛡️🐱

AxiosFocusGuard is a premium, AI-powered productivity companion for Android. It combines a robust Pomodoro timer with strict application blocking, distraction categorization, and a personalized mascot system to help users achieve deep work.

---

## 🚀 Project Overview
The app follows the **Pomodoro Technique** (Focus/Break cycles) but adds a layer of accountability. While a focus session is active, the app monitors for "distraction impulses"—attempts to open blocked apps like Instagram, TikTok, or YouTube. These attempts are logged, categorized, and analyzed by an AI coach to provide actionable feedback.

### Key Features
- **Strict App Blocking**: Uses Accessibility Services to forcefully redirect users away from distractors.
- **Mascot System**: An "Alien Cat" mascot that reacts to your focus state (ZEN, YAY, THINKING, etc.).
- **AI Insights**: Integration with Google Gemini to analyze distraction patterns and offer coaching.
- **Focus Scoring**: A proprietary scoring logic based on raw attempts, "burst" impulses, and timing patterns.
- **Custom Presets**: Support for classic Pomodoro, deep work sessions, or custom user-defined intervals.

---

## 🏗️ Architecture
The project follows modern Android development practices using **Clean Architecture** principles and **MVVM** pattern.

### Package Structure
- **`com.axios.focusguard`**: Root package containing the Application class and Main Activity.
- **`data`**: Room database, DAOs, Repositories (App, AI, Presets), and the core `FocusManager`.
- **`service`**: Background services including `FocusAccessibilityService` (blocking) and `FocusForegroundService` (lifecycle management).
- **`ui`**: Jetpack Compose screens, ViewModels, and the Theme system.
- **`util`**: Utility classes for permissions and constants.

### Key Design Decisions
1. **Accessibility Service**: Chosen for app blocking as it provides the most reliable way to detect window state changes and perform global "Home" actions.
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
3. **Monitoring**: `FocusAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED`. 
   - If a blocked app is detected, it logs a `SessionEvent` and executes `GLOBAL_ACTION_HOME`.
4. **Scoring**: At session end, `ResultsViewModel` and `AnalysisViewModel` query the `session_events` table.
   - **Score Calculation**: Starts at 100. Deducts points for raw attempts (-2), distraction "bursts" (-8), and "early session" lapses (-15).
5. **AI Analysis**: `AiRepository` groups events into temporal bursts and sends a prompt to Gemini Flash, which returns a "slightly sarcastic" coaching summary.

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
- **Permissions**: The app requires **Usage Access** and **Accessibility Service** to be enabled manually via the Settings screen.

### Dependencies
- **UI**: Jetpack Compose, Material3, Navigation Compose.
- **Logic**: Hilt (DI), Room (Persistence), DataStore (Preferences).
- **AI**: `google.generativeai` (Gemini SDK).

---

## 📂 Project Structure Tree
```text
app/src/main/java/com/axios/focusguard/
├── data/
│   ├── AiRepository.kt         # Gemini integration
│   ├── AppRepository.kt        # Room & App Info bridge
│   ├── FocusManager.kt         # Core Timer & Session logic
│   ├── SessionEvent.kt         # Distraction log model
│   └── FocusSession.kt         # Historical session model
├── service/
│   ├── FocusAccessibilityService.kt # App Blocking logic
│   └── FocusForegroundService.kt    # Persistence & Notification
├── ui/
│   ├── analysis/               # AI Insights & Scoring
│   ├── results/                # Post-session summary
│   ├── settings/               # App selection & Permissions
│   └── timer/                  # Main Pomodoro UI
└── util/
    └── PermissionManager.kt    # System intent helpers
```
