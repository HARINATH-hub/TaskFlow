package com.example.taskflow.data.repository

import android.content.Context
import com.example.taskflow.data.local.TaskDatabaseHelper
import com.example.taskflow.data.local.TaskEntity
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

    suspend fun refresh() {
        withContext(ioDispatcher) {
            val list = dbHelper.getAllTasks()
            _tasksFlow.value = list
        }
    }

    suspend fun insertTask(task: TaskEntity): Long {
        return withContext(ioDispatcher) {
            val id = dbHelper.insertTask(task)
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

    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        withContext(ioDispatcher) {
            dbHelper.toggleTaskCompletion(id, isCompleted)
            refresh()
        }
    }
}
