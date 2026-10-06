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

### 9. 🔐 Firebase Authentication (Google & Phone OTP)
- **Google Sign-In**: "Continue with Google" one-tap sign-in directly authenticating with Firebase Auth.
- **Phone SMS OTP Authentication**: "Continue with Phone" flow with:
  - Phone number input with country code validation
  - SMS 6-digit verification code delivery
  - Resend countdown timer (60 seconds)
  - Clear error feedback for invalid numbers, invalid/expired OTPs, and network issues
  - OTP codes are never exposed in application logs
- **Session Continuity**: Automatic login bypass if already authenticated.
- **Clean Logout**: Dedicated "Sign Out" button in Profile with confirmation dialog, safely clearing local active session cache without touching remote cloud data.

### 10. ☁️ Real-time Cloud Firestore Synchronization & Safe Migration
- **Per-User Cloud Isolation**: Each user's data is stored under `/users/{uid}`, with tasks under `/users/{uid}/tasks` and custom categories under `/users/{uid}/categories`.
- **Real-Time Snapshot Sync**: Changes made on one device are immediately reflected in real-time.
- **Safe First-Login Migration**:
  - Automatically checks if local data has been migrated for the authenticated account (`migrated_for_{uid}`).
  - Non-destructively uploads all pre-existing SQLite tasks, custom categories, streak, and XP to Firestore in an atomic batch.
  - Zero duplicates and zero data loss.
- **Multi-Account Switching**: When switching accounts, local database cache is safely swapped with the incoming user's cloud data.
- **Offline Resilient**: Local SQLite database acts as a responsive local cache, keeping the app fast even without internet.

---

## 🔒 Cloud Firestore Security Rules

The application includes `firestore.rules` enforcing strict per-user authorization:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /tasks/{taskId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /categories/{categoryId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    match /{document=**} {
      allow read, write: false;
    }
  }
}
```

---

## ⚙️ Firebase Console Configuration Guide

To connect your own live Firebase project:

1. **Create Firebase Project**:
   - Go to [Firebase Console](https://console.firebase.google.com/) and create a project (e.g., `taskflow-prod`).
2. **Add Android App**:
   - Package name: `com.example.taskflow`
   - Retrieve your debug SHA-1 signing certificate fingerprint:
     ```cmd
     keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
     ```
   - Paste the SHA-1 into your Android app settings in the Firebase Console.
3. **Enable Authentication Providers**:
   - In Firebase Console > **Authentication** > **Sign-in method**:
     - Enable **Google**
     - Enable **Phone** (Optional: Add test phone numbers like `+1 650-555-3434` with code `123456` for free emulator testing)
4. **Create Firestore Database**:
   - Go to **Cloud Firestore** > **Create database** > Select region and start in **Production mode**.
   - Copy the rules from `firestore.rules` in this project into the **Rules** tab and click **Publish**.
5. **Download `google-services.json`**:
   - Download the file from Firebase Console and place it into `app/google-services.json`.

---

## 🚀 How to Run and Test the App

### Option A: Open with Android Studio (Recommended)
1. Open **Android Studio**.
2. Select **File > Open...** and choose:
   ```
   C:\Users\hihar\.gemini\antigravity\scratch\TaskFlow
   ```
3. Click **OK**. Android Studio will sync Gradle automatically.
4. Select an emulator or connected physical Android device.
5. Click the green **Run (▶)** button (or press `Shift + F10`).

### Option B: Build via Command Line
Run the Gradle wrapper inside the project folder:
```cmd
cd C:\Users\hihar\.gemini\antigravity\scratch\TaskFlow
gradlew.bat assembleDebug
```
The output debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testing the New Features on Your Device

1. **Test Authentication**:
   - Open TaskFlow -> You will see the new **AuthScreen** with TaskFlow branding and value cards.
   - Tap **Continue with Phone** -> Enter your phone number (or Firebase test phone number) -> Enter the 6-digit OTP code -> You are instantly authenticated and brought into TaskFlow!
   - Tap **Continue with Google** -> Choose your Google Account -> Seamlessly authenticate into TaskFlow.

2. **Test First-Login Data Migration**:
   - Any tasks, categories, or XP previously in the app are automatically uploaded to your Cloud Firestore account.
   - Verify on Firebase Console that `/users/{uid}/tasks` and `/users/{uid}` documents are populated.

3. **Test Real-Time Cloud Sync & Offline Support**:
   - Add, edit, complete, or delete a task.
   - Notice the status badge in **Profile & Settings** showing `Cloud Synced ☁️`.
   - The changes are immediately written to Cloud Firestore and cached locally in SQLite.

4. **Test Account Switching & Multi-User Isolation**:
   - Go to **Profile & Settings** -> Observe your email/phone and provider badge.
   - Tap **Sign Out** -> Confirm in the dialog.
   - The local session is cleared and the login screen appears.
   - Log in with a different user -> Only that second user's data is shown!

5. **Test Undo & Redo**:
   - Complete a task -> Tap **Undo** -> The task reverts to pending state and XP adjusts in both SQLite and Firestore.
   - Tap **Redo** -> The task completes again.

6. **Test Theme & Custom Categories**:
   - Switch themes in **Profile & Settings** -> Theme preference syncs to the cloud and persists across device reinstalls.
   - Create custom categories -> User-created categories are synchronized with your account in Firestore.
