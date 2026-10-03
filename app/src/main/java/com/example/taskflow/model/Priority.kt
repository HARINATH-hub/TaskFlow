package com.example.taskflow.model

import androidx.compose.ui.graphics.Color

enum class Priority(val title: String, val color: Color) {
    LOW("Low", Color(0xFF4CAF50)),
    MEDIUM("Medium", Color(0xFFFFA000)),
    HIGH("High", Color(0xFFE53935));

    companion object {
        fun fromString(value: String): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
