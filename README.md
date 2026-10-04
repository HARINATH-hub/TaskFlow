# 📱 TaskFlow - Modern Android Productivity & Task Manager

A modern, offline-first Todo and Productivity Management Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Unidirectional Data Flow (MVVM)**.

---

## ✨ Features

### 1. 🔄 Reliable Multi-Step Undo & Redo
- Full action history stack supporting **Create**, **Edit**, **Delete**, **Complete**, and **Uncomplete** task operations.
- Dedicated **Undo** and **Redo** action buttons in the top app bar with dynamic enabled/disabled visual feedback.
- Interactive SnackBar toast with immediate "Undo" quick action on task completion, deletion, or edits.
- Reversible XP points adjustment during undo/redo actions to prevent score inflation.
- Safe app lifecycle and restart handling without task data corruption.

### 2. 🎨 Adaptive App Logo & Icon
- Custom, modern vector-designed adaptive application launcher icon.
- Productivity emblem featuring a rounded checklist card, speed flow accents, and a dynamic emerald checkmark badge.
- Fully compatible with circle, squircle, and rounded-square adaptive launcher masks across modern Android versions.

### 3. 🌗 Persistent Theme Settings
- Switch between **Light Theme**, **Dark Theme**, and **System Default**.
- Persists instantly to device local storage (`SharedPreferences`).
- Consistent styling across all UI elements: backgrounds, typography, cards, buttons, dialogs, inputs, and bottom sheets.

### 4. 👤 Local Profile & Display Settings
- Offline user profile with custom name editing and personalized avatar badge.
- Quick productivity statistics grid: Total Tasks, Completed Tasks, Completion Percentage, and Active Days Streak.
- Zero online account or cloud backend required—100% privacy-friendly and local to the device.

### 5. ⭐ Meaningful Productivity Rating System
- Calculates an objective rating from 1.0 to 5.0 stars (e.g., `★★★★☆ 4.4 / 5.0`).
- Grounded in real metrics:
  - **Completion Rate** (up to 3.0 points)
  - **Task Volume Bonus** (up to 1.0 point based on tasks completed)
  - **Consistency / Daily Streak** (up to 1.0 point based on consecutive days)
- Transparent explanation breakdown provided directly on the profile card.

### 6. 🏷️ Category Management with Safe Deletion
- View, create, customize, and edit custom categories.
- Customizable palette with 9 vibrant colors and 10 productivity icons.
- Built-in default categories (*Work*, *Personal*, *Shopping*, *Health*, *Study*, *Other*) are protected against accidental removal.
- **Safe Task Reassignment**: If a custom category with active tasks is deleted, a confirmation dialog appears and existing tasks are safely reassigned to *Other*—preventing any task data loss.

### 7. 🏆 Task Completion Rewards & Gamification
- **XP / Points Engine**: Earn +20 XP per task (+10 XP bonus for High-priority tasks).
- **Player Levels**: Level 1 (Novice Planner) to Level 6 (Zen Grandmaster) with dynamic XP progress bars.
- **Milestone Achievements**:
  - *First Step* (1 task)
  - *High Five* (5 tasks)
  - *Quarter Century* (25 tasks)
  - *Half Century* (50 tasks)
  - *Century Club* (100 tasks)
  - *Streak Starter* (3-day streak)
  - *Unstoppable* (7-day streak)
  - *Priority Master* (5 high-priority tasks)
- **Daily Streak Tracker**: Tracks consecutive days of completed tasks with motivational fire badges (🔥).

### 8. 🔍 Search, Filtering & Core Task Management
- Real-time instant search across titles and descriptions.
- Quick filter chips (*All*, *Pending*, *Completed*).
- Dynamic horizontal category filter pills.
- Priority levels: *Low* (Green), *Medium* (Amber), *High* (Red).
- Add & Edit bottom sheets with title validation and input fields.
- Offline-first SQLite database running asynchronously with Kotlin Coroutines and reactive `StateFlow`.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.2+
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Reactive State:** Kotlin Coroutines `StateFlow` & `Flow`
- **Database:** Local SQLite with `TaskDatabaseHelper` (Version 2 with migrations)
- **Local Settings:** `SharedPreferences` via `PreferencesManager`
- **Build System:** Gradle (Kotlin DSL `.kts`) with Version Catalog (`libs.versions.toml`)

---

## 📂 Project Structure

```
TaskFlow/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/taskflow/
│   │   │   ├── MainActivity.kt               # Entry point with dynamic theme handling
│   │   │   ├── TaskFlowApp.kt                # Application class & provider
│   │   │   ├── model/
│   │   │   │   ├── TaskCategory.kt           # Dynamic & default categories
│   │   │   │   ├── Priority.kt               # Priority enum
│   │   │   │   ├── TaskAction.kt             # Undo / Redo command patterns
│   │   │   │   └── RewardModels.kt           # XP, levels, milestones & rating logic
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── TaskEntity.kt         # Task data model
│   │   │   │   │   ├── TaskDatabaseHelper.kt # SQLite storage (tasks & categories)
│   │   │   │   │   └── PreferencesManager.kt # SharedPreferences (Theme, Profile, XP)
│   │   │   │   └── repository/
│   │   │   │       └── TaskRepository.kt     # Coroutine StateFlow repository
│   │   │   └── ui/
│   │   │       ├── TaskScreen.kt             # Main Scaffold with top bar & fab
│   │   │       ├── TaskViewModel.kt          # ViewModel orchestrating state & actions
│   │   │       ├── TaskUiState.kt            # Unidirectional UI state model
│   │   │       ├── theme/                    # Material 3 colors, typography, theme
│   │   │       ├── screens/
│   │   │       │   └── ProfileScreen.kt      # Profile, rating, rewards & theme settings
│   │   │       └── components/
│   │   │           ├── StatsCard.kt          # Dashboard card
│   │   │           ├── FilterBar.kt          # Filter & dynamic category chips
│   │   │           ├── TaskItem.kt           # Individual task card
│   │   │           ├── AddEditTaskSheet.kt   # Modal bottom sheet for tasks
│   │   │           ├── ManageCategoriesSheet.kt # Category management & safe delete
│   │   │           └── EmptyState.kt         # Empty state illustration
│   │   └── res/                              # Adaptive icons, drawables, strings, colors
│   └── build.gradle.kts                      # App module build configuration
├── gradle/
│   ├── libs.versions.toml                    # Version catalog
│   └── wrapper/                              # Gradle wrapper
├── build.gradle.kts                          # Root build configuration
├── settings.gradle.kts                       # Settings and repositories
└── README.md
```

---

## 🚀 How to Run and Test the App

### Option A: Open with Android Studio (Recommended)
1. Open **Android Studio**.
2. Select **File > Open...** (or click **Open** on the Welcome screen).
3. Navigate to:
   ```
   C:\Users\hihar\.gemini\antigravity\scratch\TaskFlow
   ```
4. Click **OK**. Android Studio will sync Gradle automatically.
5. Select an emulator or connected physical Android device.
6. Click the green **Run (▶)** button (or press `Shift + F10`).

### Option B: Build via Command Line
Run the Gradle wrapper inside the project folder:
```cmd
cd C:\Users\hihar\.gemini\antigravity\scratch\TaskFlow
gradlew.bat assembleDebug
```
The output APK is ready at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testing the New Features on Your Device

1. **Test Undo & Redo**:
   - Complete a task by tapping the checkbox -> Notice the `+XP` snackbar and the Undo icon in the top app bar lighting up.
   - Tap **Undo** (either in the top bar or on the snackbar) -> The task immediately returns to pending state and XP is reverted.
   - Tap **Redo** in the top bar -> The task is completed again and XP is restored.
   - Delete a task -> Tap **Undo** -> The task is restored with its exact original ID and details.

2. **Test App Icon**:
   - Return to your phone's home screen or app drawer.
   - Observe the new **TaskFlow** icon featuring the modern checklist card with the emerald checkmark badge.

3. **Test Profile, Ratings & Gamification**:
   - Tap the **Profile Avatar** in the top-right corner of the Home screen.
   - Tap the user name or avatar to edit your display name.
   - Check your **Productivity Rating** (e.g. `★★★★☆ 4.4 / 5.0`) and read the breakdown explaining how your completion rate, completed tasks, and streak days produce the rating.
   - Scroll down to review your **Level** (with XP progress bar) and the **Milestones & Achievements** list.

4. **Test Category Management**:
   - On the Profile screen, tap **Manage Categories**.
   - Tap **Add Custom** -> Enter a name (e.g., "Fitness"), pick a color and icon, and tap **Add**.
   - Return to the Home screen -> Notice your new category pill appears in the horizontal filter list!
   - Tap `+` to create a task -> Your new category is available in the category selector.
   - Return to Manage Categories and tap Delete on your custom category -> If tasks use it, observe the safe confirmation dialog reassigning them to "Other" without deleting any task.

5. **Test Theme Settings**:
   - On the Profile screen, find **Theme Settings**.
   - Switch between **Light Theme**, **Dark Theme**, and **System Default**.
   - The entire app UI updates instantly and preserves your selection across app restarts.
