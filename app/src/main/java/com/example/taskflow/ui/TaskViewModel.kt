package com.example.taskflow.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.local.AppThemeMode
import com.example.taskflow.data.local.PreferencesManager
import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.data.remote.AuthLogger
import com.example.taskflow.data.remote.AuthManager
import com.example.taskflow.data.remote.AuthState
import com.example.taskflow.data.remote.FirestoreSyncService
import com.example.taskflow.data.repository.TaskRepository
import com.example.taskflow.model.Priority
import com.example.taskflow.model.TaskAction
import com.example.taskflow.model.TaskCategory
import com.example.taskflow.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager = AuthManager(),
    private val firestoreService: FirestoreSyncService = FirestoreSyncService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<TaskAction>()
    private val redoStack = mutableListOf<TaskAction>()

    private var tasksListenerRegistration: ListenerRegistration? = null
    private var categoriesListenerRegistration: ListenerRegistration? = null

    init {
        // Collect repository tasks
        viewModelScope.launch {
            repository.tasksFlow.collect { tasks ->
                _uiState.update { it.copy(rawTasks = tasks) }
            }
        }
        // Collect repository categories
        viewModelScope.launch {
            repository.categoriesFlow.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        // Collect preferences: Theme
        viewModelScope.launch {
            preferencesManager.themeModeFlow.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }
        // Collect preferences: User Name
        viewModelScope.launch {
            preferencesManager.userNameFlow.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        // Collect preferences: Email
        viewModelScope.launch {
            preferencesManager.userEmailFlow.collect { email ->
                _uiState.update { it.copy(userEmail = email) }
            }
        }
        // Collect preferences: Phone
        viewModelScope.launch {
            preferencesManager.userPhoneFlow.collect { phone ->
                _uiState.update { it.copy(userPhone = phone) }
            }
        }
        // Collect preferences: Auth Provider
        viewModelScope.launch {
            preferencesManager.authProviderFlow.collect { provider ->
                _uiState.update { it.copy(authProvider = provider) }
            }
        }
        // Collect preferences: Profile Photo
        viewModelScope.launch {
            preferencesManager.profilePhotoFlow.collect { photo ->
                _uiState.update { it.copy(profilePhotoPath = photo) }
            }
        }
        // Collect preferences: XP
        viewModelScope.launch {
            preferencesManager.xpFlow.collect { xp ->
                _uiState.update { it.copy(totalXp = xp) }
            }
        }
        // Collect preferences: Streak
        viewModelScope.launch {
            preferencesManager.streakFlow.collect { streak ->
                _uiState.update { it.copy(streakDays = streak) }
            }
        }

        // Listen to Firebase Auth state
        viewModelScope.launch {
            authManager.authStateFlow.collect { authState ->
                _uiState.update { it.copy(authState = authState) }
                when (authState) {
                    is AuthState.Authenticated -> {
                        handleUserAuthenticated(authState.user)
                    }
                    is AuthState.Unauthenticated -> {
                        detachFirestoreListeners()
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            repository.refresh()
        }
    }

    // ----------------------------------------------------
    // Authentication & Cloud Sync Flow
    // ----------------------------------------------------

    private fun handleUserAuthenticated(user: FirebaseUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            AuthLogger.stage("AuthSync", "handleUserAuthenticated started", "UID: ${user.uid}")

            preferencesManager.setUserEmail(user.email)
            preferencesManager.setUserPhone(user.phoneNumber)
            val provider = user.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "phone"
            preferencesManager.setAuthProvider(provider)

            if (!user.displayName.isNullOrBlank()) {
                preferencesManager.setUserName(user.displayName!!)
            }

            try {
                // Check if user has an existing Firestore cloud profile with safe timeout
                AuthLogger.stage("AuthSync", "Fetching remote profile from Cloud Firestore")
                val profileResult = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestoreService.getUserProfile(user.uid)
                } ?: Result.failure(Exception("Cloud sync timeout - continuing in local cache mode"))

                val existingProfile = profileResult.getOrNull()

                if (existingProfile != null) {
                    AuthLogger.success("AuthSync", "Found existing cloud profile, syncing data to local SQLite")
                    preferencesManager.syncFromUserProfile(existingProfile)

                    val cloudTasksResult = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                        firestoreService.fetchAllTasks(user.uid)
                    }
                    val cloudCategoriesResult = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                        firestoreService.fetchCustomCategories(user.uid)
                    }

                    val cloudTasks = cloudTasksResult?.getOrNull() ?: emptyList()
                    val cloudCats = cloudCategoriesResult?.getOrNull() ?: emptyList()

                    // Replace local cache with authenticated user's cloud data for strict user isolation
                    repository.replaceTasks(cloudTasks)
                    repository.replaceCustomCategories(cloudCats)
                    AuthLogger.success("AuthSync", "Loaded ${cloudTasks.size} tasks and ${cloudCats.size} custom categories from cloud")
                } else {
                    // First time sign-in for this user on cloud: Safe Local Migration
                    AuthLogger.stage("AuthSync", "First cloud login detected - performing non-destructive local data migration")
                    val localTasks = repository.getAllLocalTasksRaw()
                    val localCategories = repository.getAllLocalCategoriesRaw()

                    val newProfile = UserProfile(
                        uid = user.uid,
                        displayName = user.displayName ?: preferencesManager.getUserName(),
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        photoUrl = preferencesManager.getProfilePhoto() ?: user.photoUrl?.toString(),
                        createdAt = System.currentTimeMillis(),
                        totalXp = preferencesManager.getXp(),
                        streakDays = preferencesManager.getStreakDays(),
                        lastCompletionDate = preferencesManager.getLastCompletionDate(),
                        themeMode = preferencesManager.getThemeMode().name,
                        authProvider = provider
                    )

                    // Safe migration: Upload local data to Firestore
                    kotlinx.coroutines.withTimeoutOrNull(6000L) {
                        firestoreService.migrateDataToCloud(
                            uid = user.uid,
                            profile = newProfile,
                            localTasks = localTasks,
                            localCategories = localCategories
                        )
                    }
                    preferencesManager.setMigratedForUid(user.uid)

                    if (localTasks.isNotEmpty()) {
                        showSnackbar("Synchronized ${localTasks.size} local tasks to your account ☁️", null)
                    }
                    AuthLogger.success("AuthSync", "Local migration completed for ${localTasks.size} tasks")
                }

                // Attach real-time Firestore listeners for ongoing sync
                attachFirestoreListeners(user.uid)
                AuthLogger.success("AuthSync", "Real-time sync listeners attached", "UID: ${user.uid}")
            } catch (e: Exception) {
                AuthLogger.error("AuthSync", "Non-fatal exception during cloud sync - offline cache active", e.localizedMessage ?: "", e)
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    private fun attachFirestoreListeners(uid: String) {
        detachFirestoreListeners()

        tasksListenerRegistration = firestoreService.listenToTasks(
            uid = uid,
            onTasksUpdated = { remoteTasks ->
                viewModelScope.launch {
                    repository.replaceTasks(remoteTasks)
                }
            }
        )

        categoriesListenerRegistration = firestoreService.listenToCategories(
            uid = uid,
            onCategoriesUpdated = { remoteCategories ->
                viewModelScope.launch {
                    repository.replaceCustomCategories(remoteCategories)
                }
            }
        )
    }

    private fun detachFirestoreListeners() {
        tasksListenerRegistration?.remove()
        tasksListenerRegistration = null
        categoriesListenerRegistration?.remove()
        categoriesListenerRegistration = null
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleAuthLoading = true) }
            val result = authManager.signInWithGoogleIdToken(idToken)
            result.onSuccess { user ->
                _uiState.update { it.copy(isGoogleAuthLoading = false) }
                showSnackbar("Signed in as ${user.email ?: user.displayName ?: "Google user"} 🎉", null)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGoogleAuthLoading = false,
                        authState = AuthState.Error(err.localizedMessage ?: "Google sign-in failed")
                    )
                }
                showSnackbar(err.localizedMessage ?: "Google sign-in failed.", null)
            }
        }
    }

    fun setGoogleAuthLoading(loading: Boolean) {
        _uiState.update { it.copy(isGoogleAuthLoading = loading) }
    }

    fun sendPhoneOtp(phoneNumber: String, activity: Activity) {
        val isResend = _uiState.value.isPhoneOtpSent
        _uiState.update { it.copy(isPhoneAuthLoading = true, phoneAuthError = null) }
        authManager.sendPhoneVerificationCode(
            phoneNumber = phoneNumber,
            activity = activity,
            isResend = isResend,
            onCodeSent = { verificationId, _ ->
                _uiState.update {
                    it.copy(
                        isPhoneAuthLoading = false,
                        isPhoneOtpSent = true,
                        phoneAuthVerificationId = verificationId,
                        phoneAuthError = null
                    )
                }
                showSnackbar("Verification code sent via SMS 📲", null)
            },
            onVerificationCompleted = { user ->
                _uiState.update {
                    it.copy(
                        isPhoneAuthLoading = false,
                        isPhoneOtpSent = false,
                        phoneAuthVerificationId = null,
                        phoneAuthError = null
                    )
                }
                showSnackbar("Phone verification successful! Welcome ${user.phoneNumber ?: ""}", null)
            },
            onError = { errorMsg ->
                _uiState.update {
                    it.copy(
                        isPhoneAuthLoading = false,
                        phoneAuthError = errorMsg
                    )
                }
            }
        )
    }

    fun verifyPhoneOtp(otpCode: String) {
        val verificationId = _uiState.value.phoneAuthVerificationId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPhoneAuthLoading = true, phoneAuthError = null) }
            val result = authManager.verifyPhoneOtp(verificationId, otpCode)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isPhoneAuthLoading = false,
                        isPhoneOtpSent = false,
                        phoneAuthVerificationId = null,
                        phoneAuthError = null
                    )
                }
                showSnackbar("Signed in with phone ${user.phoneNumber ?: ""}", null)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isPhoneAuthLoading = false,
                        phoneAuthError = err.localizedMessage ?: "Incorrect verification code."
                    )
                }
            }
        }
    }

    fun resetPhoneAuthState() {
        _uiState.update {
            it.copy(
                isPhoneAuthLoading = false,
                isPhoneOtpSent = false,
                phoneAuthVerificationId = null,
                phoneAuthError = null
            )
        }
    }

    fun signOut() {
        detachFirestoreListeners()
        authManager.signOut()
        preferencesManager.clearUserSession()
        viewModelScope.launch {
            repository.clearLocalData()
        }
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoAvailability()
        _uiState.update {
            it.copy(
                authState = AuthState.Unauthenticated,
                isProfileOpen = false,
                rawTasks = emptyList()
            )
        }
        showSnackbar("You have been signed out.", null)
    }

    // ----------------------------------------------------
    // Filtering and Search
    // ----------------------------------------------------

    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }

    fun setCategory(categoryName: String?) {
        _uiState.update { current ->
            val newCategory = if (current.selectedCategory == categoryName) null else categoryName
            current.copy(selectedCategory = newCategory)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(open: Boolean) {
        _uiState.update {
            it.copy(
                isSearchOpen = open,
                searchQuery = if (!open) "" else it.searchQuery
            )
        }
    }

    // ----------------------------------------------------
    // Task CRUD, Undo/Redo & Cloud Sync
    // ----------------------------------------------------

    private fun pushAction(action: TaskAction) {
        undoStack.add(action)
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    private fun updateUndoRedoAvailability() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val uid = _uiState.value.currentUid
            if (!task.isCompleted) {
                // Completing task -> calculate XP reward
                val baseExp = 20
                val bonusExp = when (task.priority) {
                    Priority.HIGH -> 10
                    Priority.MEDIUM -> 5
                    Priority.LOW -> 0
                }
                val totalExp = baseExp + bonusExp
                val (streak, incremented) = preferencesManager.recordTaskCompletion(totalExp)
                val completedTime = System.currentTimeMillis()

                repository.toggleTaskCompletion(task.id, true, completedTime)
                val updatedTask = task.copy(isCompleted = true, completedAt = completedTime)

                pushAction(TaskAction.ToggleCompletion(task, xpAwarded = totalExp))

                val msg = if (incremented && streak > 1) {
                    "+$totalExp XP! $streak-Day Streak active! 🔥"
                } else {
                    "+$totalExp XP! Task completed 🎉"
                }
                showSnackbar(msg, "Undo")

                // Sync to Firestore asynchronously in background
                if (uid != null) {
                    launch {
                        firestoreService.upsertTask(uid, updatedTask)
                        syncProfileStatsToCloud(uid)
                    }
                }
            } else {
                // Marking uncompleted
                repository.toggleTaskCompletion(task.id, false, null)
                val updatedTask = task.copy(isCompleted = false, completedAt = null)

                pushAction(TaskAction.ToggleCompletion(task, xpAwarded = 0))
                showSnackbar("Task marked pending", "Undo")

                // Sync to Firestore asynchronously in background
                if (uid != null) {
                    launch {
                        firestoreService.upsertTask(uid, updatedTask)
                    }
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            val uid = _uiState.value.currentUid
            repository.deleteTask(task.id)

            pushAction(TaskAction.DeleteTask(task))
            showSnackbar("Task \"${task.title}\" deleted", "Undo")

            // Sync to Firestore asynchronously in background
            if (uid != null) {
                launch {
                    firestoreService.deleteTask(uid, task.id)
                }
            }
        }
    }

    fun saveTask(
        title: String,
        description: String,
        category: String,
        priority: Priority,
        dueDate: Long?
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            showSnackbar("Task title cannot be empty", null)
            return
        }

        val editing = _uiState.value.editingTask
        val uid = _uiState.value.currentUid

        viewModelScope.launch {
            try {
                if (editing != null) {
                    val updated = editing.copy(
                        title = trimmedTitle,
                        description = description.trim(),
                        category = category,
                        priority = priority,
                        dueDate = dueDate
                    )
                    repository.updateTask(updated)
                    pushAction(TaskAction.EditTask(oldTask = editing, newTask = updated))
                    showSnackbar("Task updated", "Undo")
                    dismissAddEditSheet()

                    // Background cloud sync
                    if (uid != null) {
                        launch {
                            firestoreService.upsertTask(uid, updated)
                        }
                    }
                } else {
                    val newTask = TaskEntity(
                        title = trimmedTitle,
                        description = description.trim(),
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        isCompleted = false
                    )
                    val generatedId = repository.insertTask(newTask)
                    if (generatedId > 0) {
                        val insertedWithId = repository.getAllLocalTasksRaw().find { it.id == generatedId }
                            ?: newTask.copy(id = generatedId)

                        pushAction(TaskAction.AddTask(insertedWithId))
                        val taskNumStr = if (insertedWithId.taskNumber > 0) "#${insertedWithId.taskNumber}" else "#${insertedWithId.id}"
                        showSnackbar("Task $taskNumStr created", "Undo")
                        dismissAddEditSheet()

                        // Background cloud sync
                        if (uid != null) {
                            launch {
                                firestoreService.upsertTask(uid, insertedWithId)
                            }
                        }
                    } else {
                        showSnackbar("Failed to save task to local database. Please try again.", null)
                    }
                }
            } catch (e: Exception) {
                showSnackbar("Error saving task: ${e.localizedMessage ?: "Unknown error"}", null)
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(action)
        updateUndoRedoAvailability()

        val uid = _uiState.value.currentUid
        viewModelScope.launch {
            try {
                when (action) {
                    is TaskAction.AddTask -> {
                        repository.deleteTask(action.task.id)
                        if (uid != null) launch { firestoreService.deleteTask(uid, action.task.id) }
                    }
                    is TaskAction.DeleteTask -> {
                        repository.restoreTask(action.task)
                        if (uid != null) launch { firestoreService.upsertTask(uid, action.task) }
                    }
                    is TaskAction.EditTask -> {
                        repository.updateTask(action.oldTask)
                        if (uid != null) launch { firestoreService.upsertTask(uid, action.oldTask) }
                    }
                    is TaskAction.ToggleCompletion -> {
                        repository.toggleTaskCompletion(
                            action.taskBeforeToggle.id,
                            action.taskBeforeToggle.isCompleted,
                            action.taskBeforeToggle.completedAt
                        )
                        if (uid != null) {
                            launch { firestoreService.upsertTask(uid, action.taskBeforeToggle) }
                        }
                        if (action.xpAwarded > 0) {
                            preferencesManager.deductXp(action.xpAwarded)
                            if (uid != null) launch { syncProfileStatsToCloud(uid) }
                        }
                    }
                    is TaskAction.AddCategory -> {
                        repository.deleteCategory(action.category.id, action.category.name)
                        if (uid != null) launch { firestoreService.deleteCategory(uid, action.category.id) }
                    }
                    is TaskAction.DeleteCategory -> {
                        repository.insertCategory(action.category)
                        if (uid != null) launch { firestoreService.upsertCategory(uid, action.category) }
                    }
                    is TaskAction.EditCategory -> {
                        repository.updateCategory(action.oldCategory, action.newCategory.name)
                        if (uid != null) launch { firestoreService.upsertCategory(uid, action.oldCategory) }
                    }
                }
                showSnackbar("Undone: ${action.description}", null)
            } catch (e: Exception) {
                // Rollback stack upon failure
                redoStack.remove(action)
                undoStack.add(action)
                updateUndoRedoAvailability()
                showSnackbar("Undo failed: ${e.localizedMessage ?: "Unknown error"}", null)
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(action)
        updateUndoRedoAvailability()

        val uid = _uiState.value.currentUid
        viewModelScope.launch {
            try {
                when (action) {
                    is TaskAction.AddTask -> {
                        repository.restoreTask(action.task)
                        if (uid != null) launch { firestoreService.upsertTask(uid, action.task) }
                    }
                    is TaskAction.DeleteTask -> {
                        repository.deleteTask(action.task.id)
                        if (uid != null) launch { firestoreService.deleteTask(uid, action.task.id) }
                    }
                    is TaskAction.EditTask -> {
                        repository.updateTask(action.newTask)
                        if (uid != null) launch { firestoreService.upsertTask(uid, action.newTask) }
                    }
                    is TaskAction.ToggleCompletion -> {
                        val wasOriginallyCompleted = action.taskBeforeToggle.isCompleted
                        val completionTime = System.currentTimeMillis()
                        repository.toggleTaskCompletion(
                            action.taskBeforeToggle.id,
                            !wasOriginallyCompleted,
                            completionTime
                        )
                        val redoneTask = action.taskBeforeToggle.copy(
                            isCompleted = !wasOriginallyCompleted,
                            completedAt = if (!wasOriginallyCompleted) completionTime else null
                        )
                        if (uid != null) {
                            launch { firestoreService.upsertTask(uid, redoneTask) }
                        }
                        if (action.xpAwarded > 0) {
                            preferencesManager.recordTaskCompletion(action.xpAwarded)
                            if (uid != null) launch { syncProfileStatsToCloud(uid) }
                        }
                    }
                    is TaskAction.AddCategory -> {
                        repository.insertCategory(action.category)
                        if (uid != null) launch { firestoreService.upsertCategory(uid, action.category) }
                    }
                    is TaskAction.DeleteCategory -> {
                        repository.deleteCategory(action.category.id, action.category.name)
                        if (uid != null) launch { firestoreService.deleteCategory(uid, action.category.id) }
                    }
                    is TaskAction.EditCategory -> {
                        repository.updateCategory(action.newCategory, action.oldCategory.name)
                        if (uid != null) launch { firestoreService.upsertCategory(uid, action.newCategory) }
                    }
                }
                showSnackbar("Redone: ${action.description}", null)
            } catch (e: Exception) {
                // Rollback stack upon failure
                undoStack.remove(action)
                redoStack.add(action)
                updateUndoRedoAvailability()
                showSnackbar("Redo failed: ${e.localizedMessage ?: "Unknown error"}", null)
            }
        }
    }

    private suspend fun syncProfileStatsToCloud(uid: String) {
        val profile = UserProfile(
            uid = uid,
            displayName = preferencesManager.getUserName(),
            email = preferencesManager.getUserEmail(),
            phoneNumber = preferencesManager.getUserPhone(),
            photoUrl = preferencesManager.getProfilePhoto(),
            totalXp = preferencesManager.getXp(),
            streakDays = preferencesManager.getStreakDays(),
            lastCompletionDate = preferencesManager.getLastCompletionDate(),
            themeMode = preferencesManager.getThemeMode().name,
            authProvider = preferencesManager.getAuthProvider()
        )
        firestoreService.saveUserProfile(profile)
    }

    // ----------------------------------------------------
    // Category Management & Cloud Sync
    // ----------------------------------------------------

    fun addCustomCategory(displayName: String, colorValue: Long, iconName: String) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return
        val uniqueName = trimmed.uppercase().replace("\\s+".toRegex(), "_")
        val uid = _uiState.value.currentUid
        viewModelScope.launch {
            val newCat = TaskCategory(
                name = uniqueName,
                displayName = trimmed,
                colorValue = colorValue,
                iconName = iconName,
                isDefault = false
            )
            val id = repository.insertCategory(newCat)
            val insertedCat = newCat.copy(id = id)
            pushAction(TaskAction.AddCategory(insertedCat))
            if (uid != null) {
                launch { firestoreService.upsertCategory(uid, insertedCat) }
            }
            showSnackbar("Category \"$trimmed\" added", "Undo")
        }
    }

    fun updateCustomCategory(category: TaskCategory, newDisplayName: String, newColorValue: Long, newIconName: String) {
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) return
        val newUniqueName = trimmed.uppercase().replace("\\s+".toRegex(), "_")
        val uid = _uiState.value.currentUid
        viewModelScope.launch {
            val updated = category.copy(
                name = newUniqueName,
                displayName = trimmed,
                colorValue = newColorValue,
                iconName = newIconName
            )
            repository.updateCategory(updated, oldName = category.name)
            pushAction(TaskAction.EditCategory(oldCategory = category, newCategory = updated))
            if (uid != null) {
                launch { firestoreService.upsertCategory(uid, updated) }
            }
            showSnackbar("Category updated", "Undo")
        }
    }

    fun deleteCustomCategory(category: TaskCategory, onComplete: () -> Unit = {}) {
        val uid = _uiState.value.currentUid
        viewModelScope.launch {
            val count = repository.getTaskCountForCategory(category.name)
            repository.deleteCategory(category.id, category.name)
            pushAction(TaskAction.DeleteCategory(category))
            if (uid != null) {
                launch { firestoreService.deleteCategory(uid, category.id) }
            }
            onComplete()
            val msg = if (count > 0) {
                "Category deleted. $count ${if (count == 1) "task" else "tasks"} reassigned to Other."
            } else {
                "Category \"${category.displayName}\" deleted."
            }
            showSnackbar(msg, "Undo")
        }
    }

    fun checkCategoryUsage(categoryName: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.getTaskCountForCategory(categoryName)
            onResult(count)
        }
    }

    // ----------------------------------------------------
    // Profile & Settings
    // ----------------------------------------------------

    fun setThemeMode(mode: AppThemeMode) {
        preferencesManager.setThemeMode(mode)
        val uid = _uiState.value.currentUid
        if (uid != null) {
            viewModelScope.launch { syncProfileStatsToCloud(uid) }
        }
    }

    fun setUserName(name: String) {
        preferencesManager.setUserName(name)
        val uid = _uiState.value.currentUid
        if (uid != null) {
            viewModelScope.launch { syncProfileStatsToCloud(uid) }
        }
        showSnackbar("Profile updated", null)
    }

    fun updateProfilePhoto(photoPath: String?) {
        val uid = _uiState.value.currentUid
        preferencesManager.setProfilePhoto(photoPath)
        if (uid != null) {
            preferencesManager.setProfilePhotoForUid(uid, photoPath)
            viewModelScope.launch { syncProfileStatsToCloud(uid) }
        }
        _uiState.update { it.copy(profilePhotoPath = photoPath) }
        showSnackbar(if (photoPath != null) "Profile photo updated 📸" else "Profile photo removed", null)
    }

    fun openProfile() {
        _uiState.update { it.copy(isProfileOpen = true) }
    }

    fun closeProfile() {
        _uiState.update { it.copy(isProfileOpen = false) }
    }

    fun openManageCategories() {
        _uiState.update { it.copy(isManageCategoriesOpen = true) }
    }

    fun closeManageCategories() {
        _uiState.update { it.copy(isManageCategoriesOpen = false) }
    }

    // ----------------------------------------------------
    // Sheet and Dialog Controls
    // ----------------------------------------------------

    fun openAddTask() {
        _uiState.update { it.copy(isAddEditSheetOpen = true, editingTask = null) }
    }

    fun openEditTask(task: TaskEntity) {
        _uiState.update { it.copy(isAddEditSheetOpen = true, editingTask = task) }
    }

    fun dismissAddEditSheet() {
        _uiState.update { it.copy(isAddEditSheetOpen = false, editingTask = null) }
    }

    private fun showSnackbar(message: String, actionLabel: String?) {
        _uiState.update {
            it.copy(snackbarMessage = message, snackbarActionLabel = actionLabel)
        }
    }

    fun clearSnackbar() {
        _uiState.update {
            it.copy(snackbarMessage = null, snackbarActionLabel = null)
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val preferencesManager: PreferencesManager,
        private val authManager: AuthManager,
        private val firestoreService: FirestoreSyncService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(repository, preferencesManager, authManager, firestoreService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
