package com.example.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.R
import com.example.model.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.reminder.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.example.reminder.ACTION_SNOOZE_TASK"
        private const val TAG = "NotificationAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.hashCode())

        when (intent.action) {
            ACTION_COMPLETE_TASK -> {
                val currentUid = Firebase.auth.currentUser?.uid
                if (currentUid != null) {
                    try {
                        val dbId = context.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        db.collection("users").document(currentUid).collection("tasks").document(taskId)
                            .update(
                                mapOf(
                                    "isCompleted" to true,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            .addOnSuccessListener {
                                Log.d(TAG, "Task $taskId marked as complete from notification")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to mark task as complete", e)
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating task from notification", e)
                    }
                }
            }
            ACTION_SNOOZE_TASK -> {
                val taskTitle = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val taskCategory = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_CATEGORY) ?: "General"
                val taskPriority = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_PRIORITY) ?: "MEDIUM"

                val snoozedTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes later
                val snoozedTask = TaskItem(
                    id = taskId,
                    title = taskTitle,
                    category = taskCategory,
                    priority = taskPriority,
                    reminderTimestamp = snoozedTime
                )
                ReminderScheduler.schedule(context, snoozedTask)
                Log.d(TAG, "Snoozed task $taskId for 10 minutes")
            }
        }
    }
}
