package com.example.taskflow.data.repository

import android.content.Context
import com.example.taskflow.data.local.TaskDatabaseHelper
import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.model.TaskCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TaskRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dbHelper = TaskDatabaseHelper(context.applicationContext)

    private val _tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasksFlow: StateFlow<List<TaskEntity>> = _tasksFlow.asStateFlow()

    private val _categoriesFlow = MutableStateFlow<List<TaskCategory>>(TaskCategory.DEFAULT_CATEGORIES)
    val categoriesFlow: StateFlow<List<TaskCategory>> = _categoriesFlow.asStateFlow()

    suspend fun refresh() {
        withContext(ioDispatcher) {
            val list = dbHelper.getAllTasks()
            _tasksFlow.value = list
            val cats = dbHelper.getAllCategories()
            _categoriesFlow.value = cats
        }
    }

    suspend fun refreshCategories() {
        withContext(ioDispatcher) {
            val cats = dbHelper.getAllCategories()
            _categoriesFlow.value = cats
        }
    }

    suspend fun insertTask(task: TaskEntity): Long {
        return withContext(ioDispatcher) {
            val id = dbHelper.insertTask(task)
            refresh()
            id
        }
    }

    suspend fun restoreTask(task: TaskEntity): Long {
        return withContext(ioDispatcher) {
            val id = dbHelper.restoreTask(task)
            refresh()
            id
        }
    }

    suspend fun updateTask(task: TaskEntity) {
        withContext(ioDispatcher) {
            dbHelper.updateTask(task)
            refresh()
        }
    }

    suspend fun deleteTask(id: Long) {
        withContext(ioDispatcher) {
            dbHelper.deleteTask(id)
            refresh()
        }
    }

    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long? = null) {
        withContext(ioDispatcher) {
            dbHelper.toggleTaskCompletion(id, isCompleted, completedAt)
            refresh()
        }
    }

    // ----------------------------------------------------
    // Category management
    // ----------------------------------------------------

    suspend fun insertCategory(category: TaskCategory): Long {
        return withContext(ioDispatcher) {
            val id = dbHelper.insertCategory(category)
            refreshCategories()
            id
        }
    }

    suspend fun updateCategory(category: TaskCategory, oldName: String) {
        withContext(ioDispatcher) {
            dbHelper.updateCategory(category, oldName)
            refresh() // also refreshes tasks since category name might have changed
        }
    }

    suspend fun deleteCategory(id: Long, categoryName: String): Int {
        return withContext(ioDispatcher) {
            val rows = dbHelper.deleteCategory(id, categoryName, fallbackCategoryName = "OTHER")
            refresh() // refreshes both categories and reassigned tasks
            rows
        }
    }

    suspend fun getTaskCountForCategory(categoryName: String): Int {
        return withContext(ioDispatcher) {
            dbHelper.getTaskCountForCategory(categoryName)
        }
    }

    suspend fun replaceTasks(tasks: List<TaskEntity>) {
        withContext(ioDispatcher) {
            dbHelper.replaceTasks(tasks)
            refresh()
        }
    }

    suspend fun replaceCustomCategories(categories: List<TaskCategory>) {
        withContext(ioDispatcher) {
            dbHelper.replaceCustomCategories(categories)
            refreshCategories()
        }
    }

    suspend fun clearLocalData() {
        withContext(ioDispatcher) {
            dbHelper.clearAllTasks()
            refresh()
        }
    }

    suspend fun getAllLocalTasksRaw(): List<TaskEntity> {
        return withContext(ioDispatcher) {
            dbHelper.getAllTasks()
        }
    }

    suspend fun getAllLocalCategoriesRaw(): List<TaskCategory> {
        return withContext(ioDispatcher) {
            dbHelper.getAllCategories()
        }
    }
}
