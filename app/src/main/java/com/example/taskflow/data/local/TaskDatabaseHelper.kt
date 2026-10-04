package com.example.taskflow.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.taskflow.model.Priority
import com.example.taskflow.model.TaskCategory

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "taskflow.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_TASKS = "tasks"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_COMPLETED_AT = "completed_at"

        const val TABLE_CATEGORIES = "categories"
        const val COLUMN_CAT_ID = "id"
        const val COLUMN_CAT_NAME = "name"
        const val COLUMN_CAT_DISPLAY_NAME = "display_name"
        const val COLUMN_CAT_COLOR = "color_value"
        const val COLUMN_CAT_ICON = "icon_name"
        const val COLUMN_CAT_IS_DEFAULT = "is_default"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create tasks table
        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_PRIORITY TEXT NOT NULL,
                $COLUMN_DUE_DATE INTEGER,
                $COLUMN_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_COMPLETED_AT INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTasksTable)

        // Create categories table
        createCategoriesTable(db)
        seedDefaultCategories(db)

        // Seed initial friendly onboarding tasks
        seedInitialTasks(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Safe upgrade from version 1 to 2
            try {
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_COMPLETED_AT INTEGER DEFAULT 0")
            } catch (_: Exception) {
                // Column might already exist in fresh testing
            }
            createCategoriesTable(db)
            seedDefaultCategories(db)
        }
    }

    private fun createCategoriesTable(db: SQLiteDatabase) {
        val createCategoriesTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                $COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CAT_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_CAT_DISPLAY_NAME TEXT NOT NULL,
                $COLUMN_CAT_COLOR INTEGER NOT NULL,
                $COLUMN_CAT_ICON TEXT NOT NULL,
                $COLUMN_CAT_IS_DEFAULT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createCategoriesTable)
    }

    private fun seedDefaultCategories(db: SQLiteDatabase) {
        for (cat in TaskCategory.DEFAULT_CATEGORIES) {
            val values = ContentValues().apply {
                put(COLUMN_CAT_NAME, cat.name)
                put(COLUMN_CAT_DISPLAY_NAME, cat.displayName)
                put(COLUMN_CAT_COLOR, cat.colorValue)
                put(COLUMN_CAT_ICON, cat.iconName)
                put(COLUMN_CAT_IS_DEFAULT, if (cat.isDefault) 1 else 0)
            }
            db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun seedInitialTasks(db: SQLiteDatabase) {
        val initialTasks = listOf(
            TaskEntity(
                title = "Welcome to TaskFlow! 🎉",
                description = "Swipe or tap to explore your new modern task manager.",
                category = "WORK",
                priority = Priority.HIGH,
                isCompleted = false
            ),
            TaskEntity(
                title = "Review project deliverables 📊",
                description = "Check sprint goals and organize upcoming backlog items.",
                category = "WORK",
                priority = Priority.MEDIUM,
                isCompleted = false
            ),
            TaskEntity(
                title = "Grocery shopping 🛒",
                description = "Milk, fresh fruits, vegetables, and whole wheat bread.",
                category = "SHOPPING",
                priority = Priority.LOW,
                isCompleted = false
            ),
            TaskEntity(
                title = "Morning workout & 30 min run 🏃‍♂️",
                description = "Completed stretching and 5km jog around the park.",
                category = "HEALTH",
                priority = Priority.MEDIUM,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
        )

        for (task in initialTasks) {
            val values = ContentValues().apply {
                put(COLUMN_TITLE, task.title)
                put(COLUMN_DESCRIPTION, task.description)
                put(COLUMN_CATEGORY, task.category)
                put(COLUMN_PRIORITY, task.priority.name)
                put(COLUMN_DUE_DATE, task.dueDate)
                put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
                put(COLUMN_CREATED_AT, task.createdAt)
                put(COLUMN_COMPLETED_AT, task.completedAt ?: 0)
            }
            db.insert(TABLE_TASKS, null, values)
        }
    }

    // ----------------------------------------------------
    // Tasks Operations
    // ----------------------------------------------------

    fun getAllTasks(): List<TaskEntity> {
        val taskList = mutableListOf<TaskEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_IS_COMPLETED ASC, $COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_ID)
                val titleIndex = it.getColumnIndexOrThrow(COLUMN_TITLE)
                val descIndex = it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)
                val categoryIndex = it.getColumnIndexOrThrow(COLUMN_CATEGORY)
                val priorityIndex = it.getColumnIndexOrThrow(COLUMN_PRIORITY)
                val dueDateIndex = it.getColumnIndexOrThrow(COLUMN_DUE_DATE)
                val isCompletedIndex = it.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)
                val createdAtIndex = it.getColumnIndexOrThrow(COLUMN_CREATED_AT)
                val completedAtIndex = it.getColumnIndex(COLUMN_COMPLETED_AT)

                do {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val desc = it.getString(descIndex) ?: ""
                    val catStr = it.getString(categoryIndex) ?: "WORK"
                    val prioStr = it.getString(priorityIndex) ?: Priority.MEDIUM.name
                    val dueDate = if (it.isNull(dueDateIndex)) null else it.getLong(dueDateIndex)
                    val isCompleted = it.getInt(isCompletedIndex) == 1
                    val createdAt = it.getLong(createdAtIndex)
                    val completedAt = if (completedAtIndex != -1 && !it.isNull(completedAtIndex)) {
                        val v = it.getLong(completedAtIndex)
                        if (v > 0) v else null
                    } else null

                    taskList.add(
                        TaskEntity(
                            id = id,
                            title = title,
                            description = desc,
                            category = catStr,
                            priority = Priority.fromString(prioStr),
                            dueDate = dueDate,
                            isCompleted = isCompleted,
                            createdAt = createdAt,
                            completedAt = completedAt
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return taskList
    }

    fun insertTask(task: TaskEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_CATEGORY, task.category)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_DUE_DATE, task.dueDate)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COLUMN_CREATED_AT, task.createdAt)
            put(COLUMN_COMPLETED_AT, task.completedAt ?: 0)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    /**
     * Inserts or restores a task preserving its original ID (crucial for Undo & Redo)
     */
    fun restoreTask(task: TaskEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, task.id)
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_CATEGORY, task.category)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_DUE_DATE, task.dueDate)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COLUMN_CREATED_AT, task.createdAt)
            put(COLUMN_COMPLETED_AT, task.completedAt ?: 0)
        }
        return db.insertWithOnConflict(TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateTask(task: TaskEntity): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_CATEGORY, task.category)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_DUE_DATE, task.dueDate)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COLUMN_COMPLETED_AT, task.completedAt ?: 0)
        }
        return db.update(TABLE_TASKS, values, "$COLUMN_ID = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_TASKS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun toggleTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_COMPLETED, if (isCompleted) 1 else 0)
            put(COLUMN_COMPLETED_AT, if (isCompleted) (completedAt ?: System.currentTimeMillis()) else 0)
        }
        return db.update(TABLE_TASKS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // ----------------------------------------------------
    // Categories Operations
    // ----------------------------------------------------

    fun getAllCategories(): List<TaskCategory> {
        val categories = mutableListOf<TaskCategory>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CATEGORIES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CAT_IS_DEFAULT DESC, $COLUMN_CAT_DISPLAY_NAME ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_CAT_ID)
                val nameIndex = it.getColumnIndexOrThrow(COLUMN_CAT_NAME)
                val dispIndex = it.getColumnIndexOrThrow(COLUMN_CAT_DISPLAY_NAME)
                val colorIndex = it.getColumnIndexOrThrow(COLUMN_CAT_COLOR)
                val iconIndex = it.getColumnIndexOrThrow(COLUMN_CAT_ICON)
                val isDefaultIndex = it.getColumnIndexOrThrow(COLUMN_CAT_IS_DEFAULT)

                do {
                    categories.add(
                        TaskCategory(
                            id = it.getLong(idIndex),
                            name = it.getString(nameIndex),
                            displayName = it.getString(dispIndex),
                            colorValue = it.getLong(colorIndex),
                            iconName = it.getString(iconIndex),
                            isDefault = it.getInt(isDefaultIndex) == 1
                        )
                    )
                } while (it.moveToNext())
            }
        }

        // If table was empty for any reason, return default list
        if (categories.isEmpty()) {
            return TaskCategory.DEFAULT_CATEGORIES
        }
        return categories
    }

    fun insertCategory(category: TaskCategory): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CAT_NAME, category.name)
            put(COLUMN_CAT_DISPLAY_NAME, category.displayName)
            put(COLUMN_CAT_COLOR, category.colorValue)
            put(COLUMN_CAT_ICON, category.iconName)
            put(COLUMN_CAT_IS_DEFAULT, if (category.isDefault) 1 else 0)
        }
        return db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateCategory(category: TaskCategory, oldName: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CAT_NAME, category.name)
            put(COLUMN_CAT_DISPLAY_NAME, category.displayName)
            put(COLUMN_CAT_COLOR, category.colorValue)
            put(COLUMN_CAT_ICON, category.iconName)
        }
        val rows = db.update(TABLE_CATEGORIES, values, "$COLUMN_CAT_ID = ?", arrayOf(category.id.toString()))

        // If category name changed, update tasks using the old name
        if (oldName != category.name) {
            val taskValues = ContentValues().apply {
                put(COLUMN_CATEGORY, category.name)
            }
            db.update(TABLE_TASKS, taskValues, "$COLUMN_CATEGORY = ?", arrayOf(oldName))
        }
        return rows
    }

    fun getTaskCountForCategory(categoryName: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TASKS WHERE $COLUMN_CATEGORY = ?",
            arrayOf(categoryName)
        )
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count
    }

    /**
     * Safely deletes a category by reassigning existing tasks to a fallback category (default: OTHER)
     */
    fun deleteCategory(id: Long, categoryName: String, fallbackCategoryName: String = "OTHER"): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Reassign any existing tasks with this category
            val taskValues = ContentValues().apply {
                put(COLUMN_CATEGORY, fallbackCategoryName)
            }
            db.update(TABLE_TASKS, taskValues, "$COLUMN_CATEGORY = ?", arrayOf(categoryName))

            // Delete the category row
            val deleted = db.delete(TABLE_CATEGORIES, "$COLUMN_CAT_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }
}
