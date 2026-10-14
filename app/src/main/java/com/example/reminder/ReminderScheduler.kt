package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.TaskItem

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    const val ACTION_SHOW_REMINDER = "com.example.reminder.ACTION_SHOW_REMINDER"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    const val EXTRA_TASK_CATEGORY = "extra_task_category"
    const val EXTRA_TASK_PRIORITY = "extra_task_priority"

    fun schedule(context: Context, task: TaskItem) {
        if (task.id.isBlank() || task.reminderTimestamp <= System.currentTimeMillis() || task.isCompleted) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(EXTRA_TASK_CATEGORY, task.category)
            putExtra(EXTRA_TASK_PRIORITY, task.priority)
        }

        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.reminderTimestamp,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.reminderTimestamp,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.reminderTimestamp,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for task '${task.title}' at ${task.reminderTimestamp}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to standard alarm: ${e.message}")
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    task.reminderTimestamp,
                    pendingIntent
                )
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Failed to schedule fallback alarm", fallbackException)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    fun cancel(context: Context, taskId: String) {
        if (taskId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
        }
        val requestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled reminder for task ID: $taskId")
        }
    }
}
