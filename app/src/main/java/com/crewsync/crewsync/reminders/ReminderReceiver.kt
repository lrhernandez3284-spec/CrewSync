package com.crewsync.crewsync.reminders

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.crewsync.crewsync.data.NotificationStore
import com.crewsync.crewsync.data.UserStore

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val targetType = intent.getStringExtra(EXTRA_TARGET_TYPE) ?: "task"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "CrewSync Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Reminder"
        val eventId = intent.getIntExtra(EXTRA_EVENT_ID, 0)
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        val users = intent.getStringArrayListExtra(EXTRA_USER_IDS) ?: arrayListOf()

        Log.d(
            "CREWSYNC_REMINDER",
            "FIRED targetType=$targetType title=$title body=$body eventId=$eventId taskId=$taskId users=$users"
        )

        val finalUsers =
            if (users.isNotEmpty()) users
            else UserStore.getUsers(context).filter { it != "manager" }.toCollection(arrayListOf())

        NotificationStore.addForUsers(
            context = context,
            userIds = finalUsers,
            actorUserId = "CrewSync",
            message = when (targetType) {
                ReminderScheduler.TARGET_TYPE_EVENT -> "Reminder: event tomorrow - $body"
                else -> "Reminder: task due tomorrow - $body"
            },
            targetType = targetType,
            eventId = eventId,
            taskId = taskId
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CrewSync Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notificationId = when (targetType) {
            ReminderScheduler.TARGET_TYPE_EVENT -> 100_000 + eventId
            else -> 200_000 + taskId
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "crewsync_reminders"

        const val EXTRA_TARGET_TYPE = "target_type"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_USER_IDS = "user_ids"

        const val EXTRA_TASK_ID_OLD = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }
}
