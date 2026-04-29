package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ReminderScheduler {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    fun schedule(context: Context, taskId: Int, taskTitle: String, dueDateMillis: Long) {
        val reminderAt = dueDateMillis - ONE_DAY_MS
        val triggerAt = if (reminderAt < System.currentTimeMillis()) {
            // If it's already within a day, trigger soon (or you could skip)
            System.currentTimeMillis() + 5_000L
        } else reminderAt

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, taskTitle)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun cancel(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pending)
    }
}

