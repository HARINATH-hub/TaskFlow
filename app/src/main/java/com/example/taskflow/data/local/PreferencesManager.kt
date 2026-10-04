package com.example.taskflow.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("taskflow_preferences", Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Reactive theme state
    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()

    // Reactive user name
    private val _userNameFlow = MutableStateFlow(getUserName())
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    // Reactive total XP
    private val _xpFlow = MutableStateFlow(getXp())
    val xpFlow: StateFlow<Int> = _xpFlow.asStateFlow()

    // Reactive streak days
    private val _streakFlow = MutableStateFlow(getStreakDays())
    val streakFlow: StateFlow<Int> = _streakFlow.asStateFlow()

    fun getThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Productivity Pro") ?: "Productivity Pro"
    }

    fun setUserName(name: String) {
        val trimmed = name.trim().ifEmpty { "Productivity Pro" }
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
        _userNameFlow.value = trimmed
    }

    fun getXp(): Int {
        return prefs.getInt(KEY_TOTAL_XP, 0)
    }

    fun getStreakDays(): Int {
        return prefs.getInt(KEY_STREAK_DAYS, 0)
    }

    fun getLastCompletionDate(): String {
        return prefs.getString(KEY_LAST_COMPLETION_DATE, "") ?: ""
    }

    /**
     * Records a task completion, grants XP, and updates the daily streak.
     * Returns Pair(updatedStreakDays, streakIncrementedToday)
     */
    fun recordTaskCompletion(xpGain: Int): Pair<Int, Boolean> {
        val currentXp = getXp()
        val newXp = currentXp + xpGain
        prefs.edit().putInt(KEY_TOTAL_XP, newXp).apply()
        _xpFlow.value = newXp

        val todayStr = dateFormat.format(Date())
        val lastDateStr = getLastCompletionDate()
        var streak = getStreakDays()
        var streakIncremented = false

        if (lastDateStr == todayStr) {
            // Already active today, streak stays current
            streakIncremented = false
        } else if (lastDateStr.isEmpty()) {
            streak = 1
            streakIncremented = true
        } else {
            try {
                val lastDate = dateFormat.parse(lastDateStr)
                val cal = Calendar.getInstance()
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = dateFormat.format(cal.time)

                if (lastDateStr == yesterdayStr) {
                    streak += 1
                    streakIncremented = true
                } else {
                    // Missed one or more days, reset to 1
                    streak = 1
                    streakIncremented = true
                }
            } catch (_: Exception) {
                streak = 1
                streakIncremented = true
            }
        }

        prefs.edit()
            .putInt(KEY_STREAK_DAYS, streak)
            .putString(KEY_LAST_COMPLETION_DATE, todayStr)
            .apply()
        _streakFlow.value = streak

        return Pair(streak, streakIncremented)
    }

    /**
     * Deducts XP safely when an undo operation reverts a task completion.
     */
    fun deductXp(xpLoss: Int) {
        val currentXp = getXp()
        val newXp = (currentXp - xpLoss).coerceAtLeast(0)
        prefs.edit().putInt(KEY_TOTAL_XP, newXp).apply()
        _xpFlow.value = newXp
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_TOTAL_XP = "key_total_xp"
        private const val KEY_STREAK_DAYS = "key_streak_days"
        private const val KEY_LAST_COMPLETION_DATE = "key_last_completion_date"
    }
}
