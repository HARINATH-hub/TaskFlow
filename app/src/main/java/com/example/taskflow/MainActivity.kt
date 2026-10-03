package com.example.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.taskflow.ui.TaskScreen
import com.example.taskflow.ui.TaskViewModel
import com.example.taskflow.ui.theme.TaskFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TaskFlowApp
        val viewModel = ViewModelProvider(
            this,
            TaskViewModel.Factory(app.repository)
        )[TaskViewModel::class.java]

        setContent {
            TaskFlowTheme {
                TaskScreen(viewModel = viewModel)
            }
        }
    }
}
