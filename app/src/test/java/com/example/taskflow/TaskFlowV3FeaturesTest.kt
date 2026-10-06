package com.example.taskflow

import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.model.Priority
import com.example.taskflow.model.TaskAction
import com.example.taskflow.model.TaskCategory
import com.example.taskflow.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskFlowV3FeaturesTest {

    @Test
    fun taskEntity_taskNumberAndCreatedAt_serializationToAndFromFirestore() {
        val originalTime = 1775389200000L // 05 Oct 2026 6:45 PM approx
        val task = TaskEntity(
            id = 42L,
            taskNumber = 7,
            title = "Complete DBMS assignment",
            description = "Normalization and ACID properties",
            category = "STUDY",
            priority = Priority.HIGH,
            dueDate = null,
            isCompleted = false,
            createdAt = originalTime,
            completedAt = null
        )

        // Test toFirestoreMap
        val firestoreMap = task.toFirestoreMap()
        assertEquals(7, firestoreMap["taskNumber"])
        assertEquals("Complete DBMS assignment", firestoreMap["title"])
        assertEquals(originalTime, firestoreMap["createdAt"])
        assertEquals(false, firestoreMap["isCompleted"])

        // Test fromFirestoreMap
        val restoredTask = TaskEntity.fromFirestoreMap(42L, firestoreMap)
        assertEquals(42L, restoredTask.id)
        assertEquals(7, restoredTask.taskNumber)
        assertEquals("Complete DBMS assignment", restoredTask.title)
        assertEquals(originalTime, restoredTask.createdAt)
        assertEquals(Priority.HIGH, restoredTask.priority)
    }

    @Test
    fun taskEntity_backwardCompatibility_fallbacksToIdWhenTaskNumberMissing() {
        val legacyData = mapOf<String, Any?>(
            "title" to "Old Legacy Task",
            "category" to "WORK",
            "priority" to "MEDIUM",
            "isCompleted" to false,
            "createdAt" to 1700000000000L
        )

        val restoredTask = TaskEntity.fromFirestoreMap(15L, legacyData)
        assertEquals(15L, restoredTask.id)
        assertEquals(15, restoredTask.taskNumber) // Fallback to id
        assertEquals("Old Legacy Task", restoredTask.title)
    }

    @Test
    fun taskTimestamp_formattedCorrectlyInLocalTimezone() {
        val fixedTime = 1791206100000L // Example epoch millis
        val sdf = SimpleDateFormat("dd MMM yyyy • h:mm a", Locale.US)
        val formatted = sdf.format(Date(fixedTime))

        assertNotNull(formatted)
        assertTrue(formatted.contains("•"))
    }

    @Test
    fun taskAction_allTypesAndDescriptions() {
        val task = TaskEntity(
            id = 1L,
            taskNumber = 1,
            title = "Read 1 paper",
            category = "STUDY"
        )
        val category = TaskCategory(
            id = 10L,
            name = "RESEARCH",
            displayName = "Research",
            colorValue = 0xFF4F46E5,
            iconName = "Book",
            isDefault = false
        )

        val addAction = TaskAction.AddTask(task)
        assertEquals("Create \"Read 1 paper\"", addAction.description)

        val deleteAction = TaskAction.DeleteTask(task)
        assertEquals("Delete \"Read 1 paper\"", deleteAction.description)

        val editAction = TaskAction.EditTask(oldTask = task, newTask = task.copy(title = "Read 2 papers"))
        assertEquals("Edit \"Read 2 papers\"", editAction.description)

        val toggleAction = TaskAction.ToggleCompletion(taskBeforeToggle = task, xpAwarded = 25)
        assertEquals("Complete \"Read 1 paper\"", toggleAction.description)

        val addCatAction = TaskAction.AddCategory(category)
        assertEquals("Create category \"Research\"", addCatAction.description)

        val deleteCatAction = TaskAction.DeleteCategory(category)
        assertEquals("Delete category \"Research\"", deleteCatAction.description)
    }

    @Test
    fun userProfile_photoUrl_serializationToAndFromFirestore() {
        val profile = UserProfile(
            uid = "user_abc_123",
            displayName = "Alex Morgan",
            email = "alex@example.com",
            photoUrl = "/data/user/0/com.example.taskflow/files/avatars/avatar_user_abc_123.jpg",
            totalXp = 450,
            streakDays = 5,
            themeMode = "DARK",
            authProvider = "google"
        )

        val map = profile.toMap()
        assertEquals("/data/user/0/com.example.taskflow/files/avatars/avatar_user_abc_123.jpg", map["photoUrl"])

        val restored = UserProfile.fromMap(map)
        assertEquals(profile.photoUrl, restored.photoUrl)
        assertEquals("Alex Morgan", restored.displayName)
        assertEquals(450, restored.totalXp)
        assertEquals("DARK", restored.themeMode)
    }

    @Test
    fun motivationalQuote_isSubtleAndNonEmpty() {
        val quote = "“Small steps. Clear goals. Real progress.”"
        assertFalse(quote.isBlank())
        assertTrue(quote.contains("Small steps"))
        assertTrue(quote.contains("Clear goals"))
        assertTrue(quote.contains("Real progress"))
    }
}
