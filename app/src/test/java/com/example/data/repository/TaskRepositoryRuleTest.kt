package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.TaskRepository
import com.example.model.TaskItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId(): Unit = runBlocking {
        val uid = signInTestUser("user1@test.com")
        delay(300)
        val repository = TaskRepository(firestore, auth)

        val task = TaskItem(
            id = "task_" + UUID.randomUUID().toString().replace("-", ""),
            userId = uid,
            title = "Test Task Title",
            description = "Some description",
            category = "Work",
            priority = "HIGH",
            reminderTimestamp = 0L,
            isCompleted = false
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(task)
        }
        assertTrue("Expected createTask to succeed: ${result.exceptionOrNull()}", result.isSuccess)
        val taskId = result.getOrThrow()
        assertTrue(taskId.isNotEmpty())
    }

    @Test
    fun observeTasks_authenticatedOwner_emitsRealtimeUpdates(): Unit = runBlocking {
        val uid = signInTestUser("user2@test.com")
        delay(300)
        val repository = TaskRepository(firestore, auth)

        val task = TaskItem(
            id = "task_" + UUID.randomUUID().toString().replace("-", ""),
            userId = uid,
            title = "Realtime Observation Task",
            category = "Personal",
            priority = "MEDIUM",
            reminderTimestamp = 0L,
            isCompleted = false
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(task) }
        val taskId = createResult.getOrThrow()

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks().first { list -> list.any { it.id == taskId } }
        }
        assertNotNull(emittedTasks.find { it.id == taskId })
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithException(): Unit = runBlocking {
        auth.signOut()
        val repository = TaskRepository(firestore, auth)

        var failed = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks().first()
            }
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Unauthenticated caller must fail", failed)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
