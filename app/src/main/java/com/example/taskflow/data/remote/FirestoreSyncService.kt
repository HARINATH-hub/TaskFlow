package com.example.taskflow.data.remote

import com.example.taskflow.data.local.TaskEntity
import com.example.taskflow.model.TaskCategory
import com.example.taskflow.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // ----------------------------------------------------
    // User Profile
    // ----------------------------------------------------

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(profile.uid)
                .set(profile.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            if (doc.exists() && doc.data != null) {
                Result.success(UserProfile.fromMap(doc.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Tasks Operations in Firestore
    // ----------------------------------------------------

    suspend fun upsertTask(uid: String, task: TaskEntity): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document(task.id.toString())
                .set(task.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(uid: String, taskId: Long): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllTasks(uid: String): Result<List<TaskEntity>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .await()

            val tasks = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = doc.id.toLongOrNull() ?: (data["id"] as? Number)?.toLong() ?: return@mapNotNull null
                TaskEntity.fromFirestoreMap(id, data)
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToTasks(
        uid: String,
        onTasksUpdated: (List<TaskEntity>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val id = doc.id.toLongOrNull() ?: (data["id"] as? Number)?.toLong() ?: return@mapNotNull null
                        TaskEntity.fromFirestoreMap(id, data)
                    }
                    onTasksUpdated(list)
                }
            }
    }

    // ----------------------------------------------------
    // Categories Operations in Firestore
    // ----------------------------------------------------

    suspend fun upsertCategory(uid: String, category: TaskCategory): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("categories")
                .document(category.id.toString())
                .set(category.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(uid: String, categoryId: Long): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("categories")
                .document(categoryId.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCustomCategories(uid: String): Result<List<TaskCategory>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("categories")
                .get()
                .await()

            val categories = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = doc.id.toLongOrNull() ?: (data["id"] as? Number)?.toLong() ?: return@mapNotNull null
                TaskCategory.fromFirestoreMap(id, data)
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToCategories(
        uid: String,
        onCategoriesUpdated: (List<TaskCategory>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val id = doc.id.toLongOrNull() ?: (data["id"] as? Number)?.toLong() ?: return@mapNotNull null
                        TaskCategory.fromFirestoreMap(id, data)
                    }
                    onCategoriesUpdated(list)
                }
            }
    }

    // ----------------------------------------------------
    // Safe Local Data Migration
    // ----------------------------------------------------

    /**
     * Safely migrates local tasks and custom categories to Firestore in batches.
     * Preserves exact IDs to prevent duplicates.
     */
    suspend fun migrateDataToCloud(
        uid: String,
        profile: UserProfile,
        localTasks: List<TaskEntity>,
        localCategories: List<TaskCategory>
    ): Result<Int> {
        return try {
            // 1. Upload User Profile
            saveUserProfile(profile)

            // 2. Upload Tasks (using Firestore WriteBatch)
            if (localTasks.isNotEmpty()) {
                val batch = firestore.batch()
                val tasksRef = firestore.collection("users").document(uid).collection("tasks")
                for (task in localTasks) {
                    val docRef = tasksRef.document(task.id.toString())
                    batch.set(docRef, task.toFirestoreMap(), SetOptions.merge())
                }
                batch.commit().await()
            }

            // 3. Upload Custom Categories (excluding defaults)
            val customCategories = localCategories.filter { !it.isDefault }
            if (customCategories.isNotEmpty()) {
                val catBatch = firestore.batch()
                val catsRef = firestore.collection("users").document(uid).collection("categories")
                for (cat in customCategories) {
                    val docRef = catsRef.document(cat.id.toString())
                    catBatch.set(docRef, cat.toFirestoreMap(), SetOptions.merge())
                }
                catBatch.commit().await()
            }

            Result.success(localTasks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
