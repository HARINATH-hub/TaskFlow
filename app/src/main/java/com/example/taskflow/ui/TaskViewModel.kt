package com.example.taskflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.data.repository.TaskRepository
import com.example.taskflow.model.Category
import com.example.taskflow.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.tasksFlow.collect { tasks ->
                _uiState.update { it.copy(rawTasks = tasks) }
            }
        }
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }

    fun setCategory(category: Category?) {
        _uiState.update { current ->
            val newCategory = if (current.selectedCategory == category) null else category
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

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task.id, !task.isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
        }
    }

    fun openAddTask() {
        _uiState.update { it.copy(isAddEditSheetOpen = true, editingTask = null) }
    }

    fun openEditTask(task: TaskEntity) {
        _uiState.update { it.copy(isAddEditSheetOpen = true, editingTask = task) }
    }

    fun dismissAddEditSheet() {
        _uiState.update { it.copy(isAddEditSheetOpen = false, editingTask = null) }
    }

    fun saveTask(
        title: String,
        description: String,
        category: Category,
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
            } else {
                val newTask = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    dueDate = dueDate,
                    isCompleted = false
                )
                repository.insertTask(newTask)
            }
            dismissAddEditSheet()
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
