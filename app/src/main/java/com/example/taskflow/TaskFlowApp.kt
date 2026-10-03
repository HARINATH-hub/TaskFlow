package com.example.taskflow

import android.app.Application
import com.example.taskflow.data.repository.TaskRepository

class TaskFlowApp : Application() {
    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TaskRepository(this)
    }
}
