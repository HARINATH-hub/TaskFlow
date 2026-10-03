# 📱 TaskFlow - Modern Android Task Manager

A modern, offline-first Todo and Task Management Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Unidirectional Data Flow (MVVM)**.

---

## ✨ Features

- **📊 Dynamic Progress Dashboard**:
    - Live progress card tracking daily completion rate with smooth animations.
    - Quick count of pending vs completed tasks.
- **🏷️ Smart Categorization & Priorities**:
    - Categories: *Work*, *Personal*, *Shopping*, *Health*, *Study*, and *Other* with custom color badges and icons.
    - Priority levels: *Low* (Green), *Medium* (Amber), and *High* (Red).
- **⚡ Instant Search & Filtering**:
    - Fast search by title or description.
    - Filter chips to switch between *All*, *Pending*, and *Completed* tasks.
    - Category selector chips with horizontal scrolling.
- **📝 Full CRUD Task Management**:
    - Add tasks with title, description, category, and priority.
    - Edit existing tasks seamlessly via the bottom sheet.
    - Delete tasks or toggle completion status with strike-through animations.
- **💾 Offline-First Local Persistence**:
    - Built-in SQLite database with asynchronous Coroutine flows.
    - Auto-seeded with friendly welcome tasks on first launch.
- **🎨 Material 3 & Dynamic Color**:
    - Supports Android 12+ dynamic color theming.
    - Fully tuned for both Light and Dark modes.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.2+
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Reactive State:** Kotlin Coroutines `StateFlow` & `Flow`
- **Database:** Local SQLite with `SQLiteOpenHelper`
- **Build System:** Gradle (Kotlin DSL `.kts`) with Version Catalog (`libs.versions.toml`)

---

## 📂 Project Structure

```
TaskFlow/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/taskflow/
│   │   │   ├── MainActivity.kt               # Main activity & entry point
│   │   │   ├── TaskFlowApp.kt                # Application class & repository provider
│   │   │   ├── model/
│   │   │   │   ├── Category.kt               # Category enum with icons & colors
│   │   │   │   └── Priority.kt               # Priority enum with colors
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── TaskEntity.kt         # Data model
│   │   │   │   │   └── TaskDatabaseHelper.kt # SQLite persistence helper
│   │   │   │   └── repository/
│   │   │   │       └── TaskRepository.kt     # Repository with Coroutine flows
│   │   │   └── ui/
│   │   │       ├── TaskScreen.kt             # Main Compose screen & scaffold
│   │   │       ├── TaskViewModel.kt          # ViewModel & event handling
│   │   │       ├── TaskUiState.kt            # UI State & filter logic
│   │   │       ├── theme/                    # Material 3 colors, typography, theme
│   │   │       └── components/
│   │   │           ├── StatsCard.kt          # Progress indicator & task statistics
│   │   │           ├── FilterBar.kt          # Filter chips & category selector
│   │   │           ├── TaskItem.kt           # Individual task card
│   │   │           ├── AddEditTaskSheet.kt   # Modal bottom sheet for add/edit
│   │   │           └── EmptyState.kt         # Empty state illustration
│   │   └── res/                              # Drawables, mipmaps, strings, colors
│   └── build.gradle.kts                      # App module build configuration
├── gradle/
│   ├── libs.versions.toml                    # Version catalog
│   └── wrapper/                              # Gradle wrapper
├── build.gradle.kts                          # Root build configuration
├── settings.gradle.kts                       # Settings and repositories
└── README.md
```

---

## 🚀 How to Run the App

### Option A: Open with Android Studio (Recommended)
1. Open **Android Studio**.
2. Select **File > Open...** (or click **Open** on the Welcome screen).
3. Navigate to:
   ```
   C:\Users\hihar\.gemini\antigravity\scratch\TaskFlow
   ```
4. Click **OK**. Android Studio will automatically sync Gradle and index the project.
5. Select an emulator or connected physical Android device from the device dropdown.
6. Click the green **Run (▶)** button (or press `Shift + F10`).

### Option B: Build via Command Line
Run the Gradle wrapper inside the project folder:
```cmd
cd C:\Users\hihar\.gemini\antigravity\scratch\TaskFlow
gradlew.bat assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
