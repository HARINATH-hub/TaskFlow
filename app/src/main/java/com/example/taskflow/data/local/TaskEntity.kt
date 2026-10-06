package com.example.taskflow.data.local

import com.example.taskflow.model.Priority

data class TaskEntity(
    val id: Long = 0,
    val taskNumber: Int = 0,
    val title: String,
    val description: String = "",
    val category: String = "WORK",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "taskNumber" to taskNumber,
            "title" to title,
            "description" to description,
            "category" to category,
            "priority" to priority.name,
            "dueDate" to dueDate,
            "isCompleted" to isCompleted,
            "createdAt" to createdAt,
            "completedAt" to completedAt
        )
    }

    companion object {
        fun fromFirestoreMap(id: Long, data: Map<String, Any?>): TaskEntity {
            return TaskEntity(
                id = id,
                taskNumber = (data["taskNumber"] as? Number)?.toInt() ?: id.toInt(),
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                category = data["category"] as? String ?: "WORK",
                priority = Priority.fromString(data["priority"] as? String ?: "MEDIUM"),
                dueDate = (data["dueDate"] as? Number)?.toLong(),
                isCompleted = data["isCompleted"] as? Boolean ?: false,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completedAt = (data["completedAt"] as? Number)?.toLong()
            )
        }
    }
}
