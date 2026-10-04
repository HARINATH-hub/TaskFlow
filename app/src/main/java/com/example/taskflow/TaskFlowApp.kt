package com.example.taskflow

import android.app.Application
import com.example.taskflow.data.local.PreferencesManager
import com.example.taskflow.data.repository.TaskRepository

class TaskFlowApp : Application() {
    lateinit var repository: TaskRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TaskRepository(this)
        preferencesManager = PreferencesManager(this)
    }
}
