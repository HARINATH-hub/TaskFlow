package com.example.taskflow.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.taskflow.model.Category
import com.example.taskflow.model.Priority

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "taskflow.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_TASKS = "tasks"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_PRIORITY TEXT NOT NULL,
                $COLUMN_DUE_DATE INTEGER,
                $COLUMN_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        // Seed initial friendly onboarding tasks
        seedInitialTasks(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    private fun seedInitialTasks(db: SQLiteDatabase) {
        val initialTasks = listOf(
            TaskEntity(
                title = "Welcome to TaskFlow! 🎉",
                description = "Swipe or tap to explore your new modern task manager.",
                category = Category.WORK,
                priority = Priority.HIGH,
                isCompleted = false
            ),
            TaskEntity(
                title = "Review project deliverables 📊",
                description = "Check sprint goals and organize upcoming backlog items.",
                category = Category.WORK,
                priority = Priority.MEDIUM,
                isCompleted = false
            ),
            TaskEntity(
                title = "Grocery shopping 🛒",
                description = "Milk, fresh fruits, vegetables, and whole wheat bread.",
                category = Category.SHOPPING,
                priority = Priority.LOW,
                isCompleted = false
            ),
            TaskEntity(
                title = "Morning workout & 30 min run 🏃‍♂️",
                description = "Completed stretching and 5km jog around the park.",
                category = Category.HEALTH,
                priority = Priority.MEDIUM,
                isCompleted = true
            )
        )

        for (task in initialTasks) {
            val values = ContentValues().apply {
                put(COLUMN_TITLE, task.title)
                put(COLUMN_DESCRIPTION, task.description)
                put(COLUMN_CATEGORY, task.category.name)
                put(COLUMN_PRIORITY, task.priority.name)
                put(COLUMN_DUE_DATE, task.dueDate)
                put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
                put(COLUMN_CREATED_AT, task.createdAt)
            }
            db.insert(TABLE_TASKS, null, values)
        }
    }

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

                do {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val desc = it.getString(descIndex) ?: ""
                    val catStr = it.getString(categoryIndex) ?: Category.WORK.name
                    val prioStr = it.getString(priorityIndex) ?: Priority.MEDIUM.name
                    val dueDate = if (it.isNull(dueDateIndex)) null else it.getLong(dueDateIndex)
                    val isCompleted = it.getInt(isCompletedIndex) == 1
                    val createdAt = it.getLong(createdAtIndex)

                    taskList.add(
                        TaskEntity(
                            id = id,
                            title = title,
                            description = desc,
                            category = Category.fromString(catStr),
                            priority = Priority.fromString(prioStr),
                            dueDate = dueDate,
                            isCompleted = isCompleted,
                            createdAt = createdAt
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
            put(COLUMN_CATEGORY, task.category.name)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_DUE_DATE, task.dueDate)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COLUMN_CREATED_AT, task.createdAt)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    fun updateTask(task: TaskEntity): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_CATEGORY, task.category.name)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_DUE_DATE, task.dueDate)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
        }
        return db.update(TABLE_TASKS, values, "$COLUMN_ID = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_TASKS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun toggleTaskCompletion(id: Long, isCompleted: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_COMPLETED, if (isCompleted) 1 else 0)
        }
        return db.update(TABLE_TASKS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
}
