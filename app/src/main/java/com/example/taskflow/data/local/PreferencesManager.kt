package com.example.taskflow.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.taskflow.model.UserProfile
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

    // Reactive user profile state
    private val _userNameFlow = MutableStateFlow(getUserName())
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _userEmailFlow = MutableStateFlow(getUserEmail())
    val userEmailFlow: StateFlow<String?> = _userEmailFlow.asStateFlow()

    private val _userPhoneFlow = MutableStateFlow(getUserPhone())
    val userPhoneFlow: StateFlow<String?> = _userPhoneFlow.asStateFlow()

    private val _authProviderFlow = MutableStateFlow(getAuthProvider())
    val authProviderFlow: StateFlow<String> = _authProviderFlow.asStateFlow()

    // Reactive profile photo
    private val _profilePhotoFlow = MutableStateFlow(getProfilePhoto())
    val profilePhotoFlow: StateFlow<String?> = _profilePhotoFlow.asStateFlow()

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

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun setUserEmail(email: String?) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
        _userEmailFlow.value = email
    }

    fun getUserPhone(): String? {
        return prefs.getString(KEY_USER_PHONE, null)
    }

    fun setUserPhone(phone: String?) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply()
        _userPhoneFlow.value = phone
    }

    fun getAuthProvider(): String {
        return prefs.getString(KEY_AUTH_PROVIDER, "local") ?: "local"
    }

    fun setAuthProvider(provider: String) {
        prefs.edit().putString(KEY_AUTH_PROVIDER, provider).apply()
        _authProviderFlow.value = provider
    }

    fun getXp(): Int {
        return prefs.getInt(KEY_TOTAL_XP, 0)
    }

    fun setXp(xp: Int) {
        prefs.edit().putInt(KEY_TOTAL_XP, xp).apply()
        _xpFlow.value = xp
    }

    fun getStreakDays(): Int {
        return prefs.getInt(KEY_STREAK_DAYS, 0)
    }

    fun setStreakDays(streak: Int) {
        prefs.edit().putInt(KEY_STREAK_DAYS, streak).apply()
        _streakFlow.value = streak
    }

    fun getLastCompletionDate(): String {
        return prefs.getString(KEY_LAST_COMPLETION_DATE, "") ?: ""
    }

    fun setLastCompletionDate(date: String) {
        prefs.edit().putString(KEY_LAST_COMPLETION_DATE, date).apply()
    }

    fun isMigratedForUid(uid: String): Boolean {
        return prefs.getBoolean("migrated_for_$uid", false)
    }

    fun setMigratedForUid(uid: String) {
        prefs.edit().putBoolean("migrated_for_$uid", true).apply()
    }

    fun getProfilePhoto(): String? {
        return prefs.getString(KEY_PROFILE_PHOTO, null)
    }

    fun setProfilePhoto(path: String?) {
        prefs.edit().putString(KEY_PROFILE_PHOTO, path).apply()
        _profilePhotoFlow.value = path
    }

    fun getProfilePhotoForUid(uid: String): String? {
        return prefs.getString("key_profile_photo_$uid", null)
    }

    fun setProfilePhotoForUid(uid: String, path: String?) {
        prefs.edit().putString("key_profile_photo_$uid", path).apply()
        setProfilePhoto(path)
    }

    /**
     * Synchronizes in-memory and persistent state from a loaded cloud UserProfile.
     */
    fun syncFromUserProfile(profile: UserProfile) {
        setUserName(profile.displayName)
        setUserEmail(profile.email)
        setUserPhone(profile.phoneNumber)
        setAuthProvider(profile.authProvider)
        setXp(profile.totalXp)
        setStreakDays(profile.streakDays)
        setLastCompletionDate(profile.lastCompletionDate)
        val uid = profile.uid
        val localPhoto = if (uid.isNotBlank()) getProfilePhotoForUid(uid) else null
        if (localPhoto != null) {
            setProfilePhoto(localPhoto)
        } else if (!profile.photoUrl.isNullOrBlank()) {
            setProfilePhoto(profile.photoUrl)
        }
        try {
            setThemeMode(AppThemeMode.valueOf(profile.themeMode))
        } catch (_: Exception) {
            setThemeMode(AppThemeMode.SYSTEM)
        }
    }

    /**
     * Clears user session on logout.
     */
    fun clearUserSession() {
        prefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .remove(KEY_AUTH_PROVIDER)
            .remove(KEY_PROFILE_PHOTO)
            .remove(KEY_TOTAL_XP)
            .remove(KEY_STREAK_DAYS)
            .remove(KEY_LAST_COMPLETION_DATE)
            .apply()

        _userNameFlow.value = "Productivity Pro"
        _userEmailFlow.value = null
        _userPhoneFlow.value = null
        _authProviderFlow.value = "local"
        _profilePhotoFlow.value = null
        _xpFlow.value = 0
        _streakFlow.value = 0
    }

    /**
     * Records a task completion, grants XP, and updates the daily streak.
     * Returns Pair(updatedStreakDays, streakIncrementedToday)
     */
    fun recordTaskCompletion(xpGain: Int): Pair<Int, Boolean> {
        val currentXp = getXp()
        val newXp = currentXp + xpGain
        setXp(newXp)

        val todayStr = dateFormat.format(Date())
        val lastDateStr = getLastCompletionDate()
        var streak = getStreakDays()
        var streakIncremented = false

        if (lastDateStr == todayStr) {
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
        setXp(newXp)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_PHONE = "key_user_phone"
        private const val KEY_AUTH_PROVIDER = "key_auth_provider"
        private const val KEY_PROFILE_PHOTO = "key_profile_photo"
        private const val KEY_TOTAL_XP = "key_total_xp"
        private const val KEY_STREAK_DAYS = "key_streak_days"
        private const val KEY_LAST_COMPLETION_DATE = "key_last_completion_date"
    }
}
