package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.remote.FirestoreRepository
import com.example.model.FirestoreTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedAccess_throwsIllegalStateException() = runBlocking {
        auth.signOut()
        val repo = FirestoreRepository(firestore)
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repo.fetchAllTasks()
            }
        }
    }

    @Test
    fun authenticatedUser_canSaveAndFetchTask() = runBlocking {
        val testEmail = "testuser1_${System.currentTimeMillis()}@example.com"
        val uid = signInTestUser(testEmail)
        val repo = FirestoreRepository(firestore)

        val taskId = "test_task_${System.currentTimeMillis()}"
        val task = FirestoreTask(
            id = taskId,
            userId = uid,
            title = "Meeting with Doctor",
            description = "Bring prescriptions",
            category = "health",
            priority = "high",
            dueDateEpochMillis = System.currentTimeMillis() + 3600000,
            hasReminder = true,
            isCompleted = false
        )

        repo.saveTask(task)
        val tasks = repo.fetchAllTasks()
        assertNotNull(tasks)
        val saved = tasks.find { it.id == taskId }
        assertNotNull("Saved task should be present", saved)
        assertEquals("Meeting with Doctor", saved?.title)
        assertEquals("high", saved?.priority)
    }

    @Test
    fun crossUserAccess_isPreventedByRules() = runBlocking {
        val user1Email = "user1_${System.currentTimeMillis()}@example.com"
        val uid1 = signInTestUser(user1Email)
        val repo1 = FirestoreRepository(firestore)

        val taskId = "task_user1_${System.currentTimeMillis()}"
        val task1 = FirestoreTask(
            id = taskId,
            userId = uid1,
            title = "Private Task",
            category = "personal",
            priority = "medium",
            dueDateEpochMillis = System.currentTimeMillis() + 7200000,
            hasReminder = false,
            isCompleted = false
        )
        repo1.saveTask(task1)

        // Switch to user 2
        val user2Email = "user2_${System.currentTimeMillis()}@example.com"
        val uid2 = signInTestUser(user2Email)
        val repo2 = FirestoreRepository(firestore)

        val user2Tasks = repo2.fetchAllTasks()
        assertTrue("User 2 should not see User 1 tasks", user2Tasks.none { it.id == taskId })
    }
}
