package com.example.taskflow.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TaskCategory(
    val id: Long = 0,
    val name: String,
    val displayName: String,
    val colorValue: Long,
    val iconName: String,
    val isDefault: Boolean = false
) {
    val color: Color get() = Color(colorValue)

    fun icon(): ImageVector = when (iconName.lowercase()) {
        "work" -> Icons.Default.Work
        "person" -> Icons.Default.Person
        "shopping", "shopping_cart" -> Icons.Default.ShoppingCart
        "health", "favorite" -> Icons.Default.Favorite
        "study", "book", "school" -> Icons.Default.Book
        "star" -> Icons.Default.Star
        "flag" -> Icons.Default.Flag
        "fitness" -> Icons.Default.FitnessCenter
        "code" -> Icons.Default.Code
        "home" -> Icons.Default.Home
        "lightbulb" -> Icons.Default.Lightbulb
        "music" -> Icons.Default.MusicNote
        else -> Icons.AutoMirrored.Filled.List
    }

    companion object {
        val WORK = TaskCategory(1, "WORK", "Work", 0xFF1976D2, "work", true)
        val PERSONAL = TaskCategory(2, "PERSONAL", "Personal", 0xFF7B1FA2, "person", true)
        val SHOPPING = TaskCategory(3, "SHOPPING", "Shopping", 0xFFF57C00, "shopping", true)
        val HEALTH = TaskCategory(4, "HEALTH", "Health", 0xFF388E3C, "health", true)
        val STUDY = TaskCategory(5, "STUDY", "Study", 0xFF0097A7, "study", true)
        val OTHER = TaskCategory(6, "OTHER", "Other", 0xFF616161, "list", true)

        val DEFAULT_CATEGORIES = listOf(WORK, PERSONAL, SHOPPING, HEALTH, STUDY, OTHER)

        val AVAILABLE_ICONS = listOf(
            "star" to "Star",
            "flag" to "Flag",
            "fitness" to "Fitness",
            "code" to "Code",
            "home" to "Home",
            "lightbulb" to "Idea",
            "music" to "Music",
            "work" to "Work",
            "shopping" to "Shopping",
            "list" to "List"
        )

        val PRESET_COLORS = listOf(
            0xFFEF4444, // Red
            0xFFF97316, // Orange
            0xFFF59E0B, // Amber
            0xFF10B981, // Emerald
            0xFF06B6D4, // Cyan
            0xFF3B82F6, // Blue
            0xFF6366F1, // Indigo
            0xFF8B5CF6, // Violet
            0xFFEC4899  // Pink
        )

        fun findByName(categories: List<TaskCategory>, name: String): TaskCategory {
            return categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: DEFAULT_CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: OTHER
        }
    }
}
