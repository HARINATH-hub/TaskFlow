package com.example.taskflow.model

import com.example.taskflow.data.local.TaskEntity

sealed interface TaskAction {
    val description: String

    data class AddTask(val task: TaskEntity) : TaskAction {
        override val description: String = "Create \"${task.title}\""
    }

    data class DeleteTask(val task: TaskEntity) : TaskAction {
        override val description: String = "Delete \"${task.title}\""
    }

    data class EditTask(val oldTask: TaskEntity, val newTask: TaskEntity) : TaskAction {
        override val description: String = "Edit \"${newTask.title}\""
    }

    data class ToggleCompletion(val taskBeforeToggle: TaskEntity, val xpAwarded: Int = 0) : TaskAction {
        override val description: String = if (!taskBeforeToggle.isCompleted) {
            "Complete \"${taskBeforeToggle.title}\""
        } else {
            "Uncomplete \"${taskBeforeToggle.title}\""
        }
    }
}
