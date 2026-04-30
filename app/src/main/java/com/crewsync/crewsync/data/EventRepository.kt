package com.crewsync.crewsync.data

import android.content.Context
import com.crewsync.crewsync.models.Event

object EventRepository {
    fun getEvents(): List<Event> = EventStore.getDefaultEvents()

    fun getEvents(context: Context): List<Event> {
        val currentUser = UserPrefs.getCurrentUserId(context)

        return EventStore.getEvents(context).filter { event ->
            currentUser == "manager" ||
                    event.assignedUserIds.isEmpty() ||
                    event.assignedUserIds.contains(currentUser)
        }
    }

    fun getCategories(context: Context): List<String> = EventStore.getCategories(context)

    fun getEventById(context: Context, eventId: Int): Event? {
        return EventStore.getEvents(context).firstOrNull { it.id == eventId }
    }
}
