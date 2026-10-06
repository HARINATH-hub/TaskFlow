package com.example.taskflow

import android.app.Application
import com.example.taskflow.data.local.PreferencesManager
import com.example.taskflow.data.remote.AuthManager
import com.example.taskflow.data.remote.FirestoreSyncService
import com.example.taskflow.data.repository.TaskRepository

class TaskFlowApp : Application() {
    lateinit var repository: TaskRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var firestoreService: FirestoreSyncService
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TaskRepository(this)
        preferencesManager = PreferencesManager(this)
        authManager = AuthManager()
        firestoreService = FirestoreSyncService()
    }
}
