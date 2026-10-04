package com.example.taskflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.local.AppThemeMode
import com.example.taskflow.data.local.PreferencesManager
import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.data.repository.TaskRepository
import com.example.taskflow.model.Priority
import com.example.taskflow.model.TaskAction
import com.example.taskflow.model.TaskCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<TaskAction>()
    private val redoStack = mutableListOf<TaskAction>()

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

        viewModelScope.launch {
            repository.refresh()
        }
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
    // Task CRUD & Undo/Redo
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
                pushAction(TaskAction.ToggleCompletion(task, xpAwarded = totalExp))

                val msg = if (incremented && streak > 1) {
                    "+$totalExp XP! $streak-Day Streak active! 🔥"
                } else {
                    "+$totalExp XP! Task completed 🎉"
                }
                showSnackbar(msg, "Undo")
            } else {
                // Marking uncompleted
                repository.toggleTaskCompletion(task.id, false, null)
                pushAction(TaskAction.ToggleCompletion(task, xpAwarded = 0))
                showSnackbar("Task marked pending", "Undo")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            pushAction(TaskAction.DeleteTask(task))
            showSnackbar("Task \"${task.title}\" deleted", "Undo")
        }
    }

    fun saveTask(
        title: String,
        description: String,
        category: String,
        priority: Priority,
        dueDate: Long?
    ) {
        val editing = _uiState.value.editingTask
        viewModelScope.launch {
            if (editing != null) {
                val updated = editing.copy(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    dueDate = dueDate
                )
                repository.updateTask(updated)
                pushAction(TaskAction.EditTask(oldTask = editing, newTask = updated))
                showSnackbar("Task updated", "Undo")
            } else {
                val newTask = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    dueDate = dueDate,
                    isCompleted = false
                )
                val generatedId = repository.insertTask(newTask)
                val insertedWithId = newTask.copy(id = generatedId)
                pushAction(TaskAction.AddTask(insertedWithId))
                showSnackbar("Task created", "Undo")
            }
            dismissAddEditSheet()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.lastIndex)
        viewModelScope.launch {
            when (action) {
                is TaskAction.AddTask -> {
                    repository.deleteTask(action.task.id)
                }
                is TaskAction.DeleteTask -> {
                    repository.restoreTask(action.task)
                }
                is TaskAction.EditTask -> {
                    repository.updateTask(action.oldTask)
                }
                is TaskAction.ToggleCompletion -> {
                    repository.toggleTaskCompletion(
                        action.taskBeforeToggle.id,
                        action.taskBeforeToggle.isCompleted,
                        action.taskBeforeToggle.completedAt
                    )
                    if (action.xpAwarded > 0) {
                        preferencesManager.deductXp(action.xpAwarded)
                    }
                }
            }
            redoStack.add(action)
            updateUndoRedoAvailability()
            showSnackbar("Undone: ${action.description}", null)
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeAt(redoStack.lastIndex)
        viewModelScope.launch {
            when (action) {
                is TaskAction.AddTask -> {
                    repository.restoreTask(action.task)
                }
                is TaskAction.DeleteTask -> {
                    repository.deleteTask(action.task.id)
                }
                is TaskAction.EditTask -> {
                    repository.updateTask(action.newTask)
                }
                is TaskAction.ToggleCompletion -> {
                    val wasOriginallyCompleted = action.taskBeforeToggle.isCompleted
                    repository.toggleTaskCompletion(
                        action.taskBeforeToggle.id,
                        !wasOriginallyCompleted,
                        System.currentTimeMillis()
                    )
                    if (action.xpAwarded > 0) {
                        preferencesManager.recordTaskCompletion(action.xpAwarded)
                    }
                }
            }
            undoStack.add(action)
            updateUndoRedoAvailability()
            showSnackbar("Redone: ${action.description}", null)
        }
    }

    // ----------------------------------------------------
    // Category Management
    // ----------------------------------------------------

    fun addCustomCategory(displayName: String, colorValue: Long, iconName: String) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return
        val uniqueName = trimmed.uppercase().replace("\\s+".toRegex(), "_")
        viewModelScope.launch {
            val newCat = TaskCategory(
                name = uniqueName,
                displayName = trimmed,
                colorValue = colorValue,
                iconName = iconName,
                isDefault = false
            )
            repository.insertCategory(newCat)
            showSnackbar("Category \"$trimmed\" added", null)
        }
    }

    fun updateCustomCategory(category: TaskCategory, newDisplayName: String, newColorValue: Long, newIconName: String) {
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) return
        val newUniqueName = trimmed.uppercase().replace("\\s+".toRegex(), "_")
        viewModelScope.launch {
            val updated = category.copy(
                name = newUniqueName,
                displayName = trimmed,
                colorValue = newColorValue,
                iconName = newIconName
            )
            repository.updateCategory(updated, oldName = category.name)
            showSnackbar("Category updated", null)
        }
    }

    fun deleteCustomCategory(category: TaskCategory, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.getTaskCountForCategory(category.name)
            repository.deleteCategory(category.id, category.name)
            onComplete()
            val msg = if (count > 0) {
                "Category deleted. $count ${if (count == 1) "task" else "tasks"} reassigned to Other."
            } else {
                "Category \"${category.displayName}\" deleted."
            }
            showSnackbar(msg, null)
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
    }

    fun setUserName(name: String) {
        preferencesManager.setUserName(name)
        showSnackbar("Profile updated", null)
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
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(repository, preferencesManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
