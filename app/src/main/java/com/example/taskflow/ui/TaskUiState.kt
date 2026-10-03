package com.example.taskflow.ui

import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.model.Category

enum class TaskFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

data class TaskUiState(
    val rawTasks: List<TaskEntity> = emptyList(),
    val currentFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategory: Category? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isAddEditSheetOpen: Boolean = false,
    val editingTask: TaskEntity? = null
) {
    val totalCount: Int get() = rawTasks.size
    val completedCount: Int get() = rawTasks.count { it.isCompleted }
    val pendingCount: Int get() = totalCount - completedCount
    val completionProgress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val filteredTasks: List<TaskEntity>
        get() {
            return rawTasks.filter { task ->
                val matchesFilter = when (currentFilter) {
                    TaskFilter.ALL -> true
                    TaskFilter.PENDING -> !task.isCompleted
                    TaskFilter.COMPLETED -> task.isCompleted
                }

                val matchesCategory = selectedCategory == null || task.category == selectedCategory

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
