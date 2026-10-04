package com.example.taskflow.data.local

import com.example.taskflow.model.Priority

data class TaskEntity(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "WORK",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
