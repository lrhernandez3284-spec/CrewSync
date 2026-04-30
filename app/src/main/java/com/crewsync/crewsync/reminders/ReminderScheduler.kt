package com.crewsync.crewsync.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderScheduler {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    const val TARGET_TYPE_TASK = "task"
    const val TARGET_TYPE_EVENT = "event"

    private val debugFormat = SimpleDateFormat("MMM d, h:mm:ss a", Locale.US)

    fun scheduleTaskReminder(
        context: Context,
        taskId: Int,
        taskTitle: String,
        dueDateMillis: Long,
        eventId: Int,
        assignedUserIds: List<String>
    ) {
        scheduleReminder(
            context = context,
            requestCode = stableRequestCode(TARGET_TYPE_TASK, taskId),
            targetType = TARGET_TYPE_TASK,
            title = "Task due tomorrow",
            body = taskTitle,
            scheduledMillis = dueDateMillis,
            eventId = eventId,
            taskId = taskId,
            userIds = assignedUserIds
        )
    }

    fun scheduleEventReminder(
        context: Context,
        eventId: Int,
        eventTitle: String,
        eventMillis: Long,
        assignedUserIds: List<String>
    ) {
        scheduleReminder(
            context = context,
            requestCode = stableRequestCode(TARGET_TYPE_EVENT, eventId),
            targetType = TARGET_TYPE_EVENT,
            title = "Event tomorrow",
            body = eventTitle,
            scheduledMillis = eventMillis,
            eventId = eventId,
            taskId = 0,
            userIds = assignedUserIds
        )
    }

    fun schedule(context: Context, taskId: Int, taskTitle: String, dueDateMillis: Long) {
        scheduleTaskReminder(
            context = context,
            taskId = taskId,
            taskTitle = taskTitle,
            dueDateMillis = dueDateMillis,
            eventId = 0,
            assignedUserIds = emptyList()
        )
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        cancelByRequestCode(context, stableRequestCode(TARGET_TYPE_TASK, taskId))
    }

    fun cancelEventReminder(context: Context, eventId: Int) {
        cancelByRequestCode(context, stableRequestCode(TARGET_TYPE_EVENT, eventId))
    }

    fun cancel(context: Context, taskId: Int) {
        cancelTaskReminder(context, taskId)
    }

    private fun scheduleReminder(
        context: Context,
        requestCode: Int,
        targetType: String,
        title: String,
        body: String,
        scheduledMillis: Long,
        eventId: Int,
        taskId: Int,
        userIds: List<String>
    ) {
        val reminderAt = scheduledMillis - ONE_DAY_MS

        val triggerAt = if (reminderAt < System.currentTimeMillis()) {
            // For demo/testing: if already within 24 hours, trigger soon.
            System.currentTimeMillis() + 5_000L
        } else {
            reminderAt
        }

        Log.d(
            "CREWSYNC_REMINDER",
            "Scheduling $targetType reminder. body=$body scheduled=${debugFormat.format(Date(scheduledMillis))} trigger=${debugFormat.format(Date(triggerAt))} users=$userIds"
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TARGET_TYPE, targetType)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putStringArrayListExtra(ReminderReceiver.EXTRA_USER_IDS, ArrayList(userIds.distinct()))
        }

        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancelByRequestCode(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pending)
    }

    private fun stableRequestCode(type: String, id: Int): Int {
        return if (type == TARGET_TYPE_EVENT) {
            100_000 + id
        } else {
            200_000 + id
        }
    }
}
