package com.crewsync.crewsync

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object EventStore {
    private const val PREFS = "crewsync_prefs"
    private const val KEY_EVENTS = "custom_events"
    private const val KEY_NEXT_ID = "custom_event_next_id"

    private val dateFormat = SimpleDateFormat("EEE MMM d, yyyy h:mm a", Locale.US)

    fun getDefaultEvents(): List<Event> = listOf(
        demoEvent(1, "Rehearsal - Setlist Run", 2026, Calendar.APRIL, 6, 19, 0, "Rehearsal", "Garage Studio", "Practice intros + transitions"),
        demoEvent(2, "Gig @ Calakas Raza", 2026, Calendar.APRIL, 11, 21, 0, "Performance", "Downtown", "Arrive early, soundcheck 8:15"),
        demoEvent(3, "Team Meeting", 2026, Calendar.APRIL, 8, 17, 30, "Meeting", "Coffee shop", null),
        demoEvent(4, "Personal: Replace Strings", 2026, Calendar.APRIL, 9, 18, 0, "Personal", null, "NYXL set + tune stability check"),
        demoEvent(5, "Photo/Promo Content", 2026, Calendar.APRIL, 12, 14, 0, "Content", "Home", "Record 3 clips for TikTok")
    )

    private fun demoEvent(
        id: Int,
        title: String,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        category: String,
        location: String?,
        notesPreview: String?
    ): Event {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return Event(
            id = id,
            title = title,
            dateTime = dateFormat.format(cal.time),
            category = category,
            location = location,
            notesPreview = notesPreview,
            dateTimeMillis = cal.timeInMillis,
            assignedUserIds = emptyList() // empty means default/demo event is visible to everyone
        )
    }

    fun getEvents(context: Context): List<Event> {
        return getDefaultEvents() + getCustomEvents(context)
    }

    fun getCategories(context: Context): List<String> {
        return getEvents(context)
            .map { it.category }
            .distinct()
            .sorted()
    }

    fun addEvent(
        context: Context,
        title: String,
        category: String,
        dateTimeMillis: Long,
        location: String?,
        notesPreview: String?,
        assignedUserIds: List<String>
    ): Event {
        val finalAssigned = assignedUserIds.distinct().filter { it.isNotBlank() }

        require(finalAssigned.isNotEmpty()) {
            "Event must be assigned to at least one user"
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val nextId = prefs.getInt(KEY_NEXT_ID, 1000)
        val event = Event(
            id = nextId,
            title = title,
            dateTime = dateFormat.format(Date(dateTimeMillis)),
            category = category,
            location = location,
            notesPreview = notesPreview,
            dateTimeMillis = dateTimeMillis,
            assignedUserIds = finalAssigned
        )

        val current = getCustomEvents(context).toMutableList()
        current.add(event)

        prefs.edit()
            .putString(KEY_EVENTS, encodeEvents(current))
            .putInt(KEY_NEXT_ID, nextId + 1)
            .apply()

        val actor = UserPrefs.getCurrentUserId(context)

        NotificationStore.addForUsers(
            context = context,
            userIds = finalAssigned,
            actorUserId = actor,
            message = "$actor created event ${event.title}",
            targetType = "event",
            eventId = event.id
        )

        return event
    }


    fun updateCategoryForCustomEvents(context: Context, oldCategory: String, newCategory: String) {
        val updated = getCustomEvents(context).map { event ->
            if (event.category.equals(oldCategory, ignoreCase = true)) {
                event.copy(category = newCategory)
            } else {
                event
            }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EVENTS, encodeEvents(updated))
            .apply()
    }

    private fun getCustomEvents(context: Context): List<Event> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_EVENTS, "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { decodeEvent(it) }
    }

    private fun encodeEvents(events: List<Event>): String {
        return events.joinToString("\n") { e ->
            listOf(
                e.id.toString(),
                enc(e.title),
                enc(e.dateTime),
                enc(e.category),
                enc(e.location ?: ""),
                enc(e.notesPreview ?: ""),
                (e.dateTimeMillis ?: 0L).toString(),
                encodeUsers(e.assignedUserIds)
            ).joinToString("|")
        }
    }

    private fun decodeEvent(line: String): Event? {
        val parts = line.split("|")
        if (parts.size < 7) return null

        return try {
            val millis = parts[6].toLong()
            val assigned = if (parts.size >= 8) decodeUsers(parts[7]) else emptyList()

            Event(
                id = parts[0].toInt(),
                title = dec(parts[1]),
                dateTime = dec(parts[2]),
                category = dec(parts[3]),
                location = dec(parts[4]).ifBlank { null },
                notesPreview = dec(parts[5]).ifBlank { null },
                dateTimeMillis = millis.takeIf { it > 0L },
                assignedUserIds = assigned
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeUsers(users: List<String>): String {
        return users.joinToString(",") { enc(it) }
    }

    private fun decodeUsers(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").map { dec(it) }.filter { it.isNotBlank() }
    }

    private fun enc(value: String): String = Uri.encode(value)
    private fun dec(value: String): String = Uri.decode(value)
}
