package com.example.data

import android.content.Context
import com.example.R
import com.example.model.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class TaskRepository(
    private val db: FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTasks(): Flow<List<TaskItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        val query = db.collection("users").document(uid).collection("tasks")

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TaskItem::class.java).sortedWith(
                        compareBy<TaskItem> { it.isCompleted }
                            .thenBy { if (it.reminderTimestamp > 0) it.reminderTimestamp else Long.MAX_VALUE }
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(task: TaskItem): Result<String> {
        return try {
            val uid = requireUserId()
            val collectionPath = "users/$uid/tasks"
            val tasksRef = db.collection("users").document(uid).collection("tasks")
            val newDocId = if (task.id.isNotBlank()) task.id else UUID.randomUUID().toString()

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to task.title.trim(),
                "category" to task.category,
                "priority" to task.priority,
                "reminderTimestamp" to task.reminderTimestamp,
                "isCompleted" to task.isCompleted,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (task.description.isNotBlank()) {
                payload["description"] = task.description.trim()
            }

            tasksRef.document(newDocId).set(payload).await()
            Result.success(newDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/tasks")
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(task.id.isNotBlank()) { "Task ID must not be blank" }
            val docPath = "users/$uid/tasks/${task.id}"
            val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)

            val updates = mutableMapOf<String, Any>(
                "title" to task.title.trim(),
                "category" to task.category,
                "priority" to task.priority,
                "reminderTimestamp" to task.reminderTimestamp,
                "isCompleted" to task.isCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (task.description.isNotBlank()) {
                updates["description"] = task.description.trim()
            }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/tasks/${task.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleTaskComplete(task: TaskItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(task.id.isNotBlank()) { "Task ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
            docRef.update(
                mapOf(
                    "isCompleted" to !task.isCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/tasks/${task.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank()) { "Task ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/tasks/$taskId")
            Result.failure(e)
        }
    }
}
