package com.crewsync.crewsync

import android.content.Context
import android.net.Uri

object NotificationStore {
    private const val PREFS = "crewsync_prefs"
    private const val KEY_NOTIFICATIONS = "notification_log"

    fun addNotification(
        context: Context,
        userId: String,
        actorUserId: String,
        message: String,
        targetType: String,
        eventId: Int,
        taskId: Int = 0
    ) {
        val current = getAllNotifications(context).toMutableList()

        val item = NotificationItem(
            id = System.currentTimeMillis(),
            userId = userId,
            actorUserId = actorUserId,
            message = message,
            targetType = targetType,
            eventId = eventId,
            taskId = taskId,
            createdAtMillis = System.currentTimeMillis()
        )

        current.add(0, item)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTIFICATIONS, encode(current.take(100)))
            .apply()
    }

    fun addForUsers(
        context: Context,
        userIds: List<String>,
        actorUserId: String,
        message: String,
        targetType: String,
        eventId: Int,
        taskId: Int = 0
    ) {
        userIds.distinct().forEach { userId ->
            addNotification(
                context = context,
                userId = userId,
                actorUserId = actorUserId,
                message = message,
                targetType = targetType,
                eventId = eventId,
                taskId = taskId
            )
        }
    }

    fun getNotificationsForUser(context: Context, userId: String): List<NotificationItem> {
        val all = getAllNotifications(context)

        return if (userId == "manager") {
            all
        } else {
            all.filter { it.userId == userId }
        }
    }

    private fun getAllNotifications(context: Context): List<NotificationItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NOTIFICATIONS, "") ?: ""

        if (raw.isBlank()) return emptyList()

        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { decodeLine(it) }
            .sortedByDescending { it.createdAtMillis }
    }

    private fun encode(items: List<NotificationItem>): String {
        return items.joinToString("\n") { item ->
            listOf(
                item.id.toString(),
                enc(item.userId),
                enc(item.actorUserId),
                enc(item.message),
                enc(item.targetType),
                item.eventId.toString(),
                item.taskId.toString(),
                item.createdAtMillis.toString()
            ).joinToString("|")
        }
    }

    private fun decodeLine(line: String): NotificationItem? {
        val parts = line.split("|")
        if (parts.size < 8) return null

        return try {
            NotificationItem(
                id = parts[0].toLong(),
                userId = dec(parts[1]),
                actorUserId = dec(parts[2]),
                message = dec(parts[3]),
                targetType = dec(parts[4]),
                eventId = parts[5].toInt(),
                taskId = parts[6].toInt(),
                createdAtMillis = parts[7].toLong()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun enc(value: String): String = Uri.encode(value)
    private fun dec(value: String): String = Uri.decode(value)
}
