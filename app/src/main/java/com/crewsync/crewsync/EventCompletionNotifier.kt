package com.crewsync.crewsync

import android.content.Context
import com.crewsync.crewsync.db.TaskDao

object EventCompletionNotifier {
    private const val PREFS = "crewsync_prefs"
    private const val PREFIX = "event_completion_notified_"

    private fun key(eventId: Int): String = PREFIX + eventId

    suspend fun checkAndNotifyIfComplete(context: Context, dao: TaskDao, eventId: Int) {
        if (eventId <= 0) return

        val total = dao.countTotalNow(eventId)
        val done = dao.countDoneNow(eventId)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alreadyNotified = prefs.getBoolean(key(eventId), false)

        if (total > 0 && done == total && !alreadyNotified) {
            val event = EventRepository.getEventById(context, eventId)

            val eventTitle = event?.title ?: "Event #$eventId"

            val assignedUsers =
                if (event?.assignedUserIds?.isNotEmpty() == true) {
                    event.assignedUserIds
                } else {
                    UserStore.getUsers(context).filter { it != "manager" }
                }

            val usersToNotify = (assignedUsers + "manager").distinct()

            NotificationStore.addForUsers(
                context = context,
                userIds = usersToNotify,
                actorUserId = "CrewSync",
                message = "Event completed: $eventTitle",
                targetType = "event",
                eventId = eventId,
                taskId = 0
            )

            prefs.edit().putBoolean(key(eventId), true).apply()
        }

        // If someone unchecks a task later
        if (total == 0 || done < total) {
            prefs.edit().putBoolean(key(eventId), false).apply()
        }
    }
}
