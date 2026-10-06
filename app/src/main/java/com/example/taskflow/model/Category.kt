package com.example.taskflow.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Category(val displayName: String, val color: Color) {
    WORK("Work", Color(0xFF1976D2)),
    PERSONAL("Personal", Color(0xFF7B1FA2)),
    SHOPPING("Shopping", Color(0xFFF57C00)),
    HEALTH("Health", Color(0xFF388E3C)),
    STUDY("Study", Color(0xFF0097A7)),
    OTHER("Other", Color(0xFF616161));

    fun icon(): ImageVector = when (this) {
        WORK -> Icons.Default.Work
        PERSONAL -> Icons.Default.Person
        SHOPPING -> Icons.Default.ShoppingCart
        HEALTH -> Icons.Default.Favorite
        STUDY -> Icons.Default.Book
        OTHER -> Icons.AutoMirrored.Filled.List
    }

    companion object {
        fun fromString(value: String): Category {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
