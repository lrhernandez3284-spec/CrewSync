package com.crewsync.crewsync

import android.content.Context

object EventRepository {
    fun getEvents(): List<Event> = EventStore.getDefaultEvents()

    fun getEvents(context: Context): List<Event> = EventStore.getEvents(context)

    fun getCategories(context: Context): List<String> = EventStore.getCategories(context)

    fun getEventById(context: Context, eventId: Int): Event? {
        return getEvents(context).firstOrNull { it.id == eventId }
    }
}
