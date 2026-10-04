package com.example.taskflow.ui

import com.example.taskflow.data.local.AppThemeMode
import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.model.Achievement
import com.example.taskflow.model.Priority
import com.example.taskflow.model.ProductivityRating
import com.example.taskflow.model.RewardCalculator
import com.example.taskflow.model.TaskCategory
import com.example.taskflow.model.UserLevel

enum class TaskFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

data class TaskUiState(
    val rawTasks: List<TaskEntity> = emptyList(),
    val categories: List<TaskCategory> = TaskCategory.DEFAULT_CATEGORIES,
    val currentFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isAddEditSheetOpen: Boolean = false,
    val editingTask: TaskEntity? = null,
    val isProfileOpen: Boolean = false,
    val isManageCategoriesOpen: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val userName: String = "Productivity Pro",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val snackbarMessage: String? = null,
    val snackbarActionLabel: String? = null
) {
    val totalCount: Int get() = rawTasks.size
    val completedCount: Int get() = rawTasks.count { it.isCompleted }
    val pendingCount: Int get() = totalCount - completedCount

    val completionProgress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val highPriorityCompletedCount: Int
        get() = rawTasks.count { it.isCompleted && it.priority == Priority.HIGH }

    val productivityRating: ProductivityRating
        get() = RewardCalculator.calculateProductivityRating(totalCount, completedCount, streakDays)

    val userLevel: UserLevel
        get() = RewardCalculator.calculateLevel(totalXp)

    val achievements: List<Achievement>
        get() = RewardCalculator.computeAchievements(
            completedCount = completedCount,
            streakDays = streakDays,
            highPriorityCompletedCount = highPriorityCompletedCount
        )

    val filteredTasks: List<TaskEntity>
        get() {
            return rawTasks.filter { task ->
                val matchesFilter = when (currentFilter) {
                    TaskFilter.ALL -> true
                    TaskFilter.PENDING -> !task.isCompleted
                    TaskFilter.COMPLETED -> task.isCompleted
                }

                val matchesCategory = selectedCategory == null ||
                        task.category.equals(selectedCategory, ignoreCase = true)

                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    task.title.contains(searchQuery, ignoreCase = true) ||
                            task.description.contains(searchQuery, ignoreCase = true)
                }

                matchesFilter && matchesCategory && matchesSearch
            }
        }
}
