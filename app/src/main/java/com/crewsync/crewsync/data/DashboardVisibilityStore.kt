package com.crewsync.crewsync.data

import android.content.Context

object DashboardVisibilityStore {
    private const val PREFS = "crewsync_prefs"
    private const val HIDDEN_PREFIX = "hidden_dashboard_events_"
    private const val FILTER_PREFIX = "dashboard_filter_"

    const val FILTER_UPCOMING = "Upcoming events"
    const val FILTER_ALL = "All events"
    const val FILTER_PAST = "Past events"
    const val FILTER_COMPLETED = "Completed events"

    val filters = listOf(
        FILTER_UPCOMING,
        FILTER_ALL,
        FILTER_PAST,
        FILTER_COMPLETED
    )

    private fun hiddenKey(userId: String): String = HIDDEN_PREFIX + userId
    private fun filterKey(userId: String): String = FILTER_PREFIX + userId

    fun getFilter(context: Context, userId: String): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(filterKey(userId), FILTER_UPCOMING) ?: FILTER_UPCOMING
    }

    fun setFilter(context: Context, userId: String, filter: String) {
        val cleaned = if (filters.contains(filter)) filter else FILTER_UPCOMING
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(filterKey(userId), cleaned)
            .apply()
    }

    fun isHidden(context: Context, userId: String, eventId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(hiddenKey(userId), emptySet()) ?: emptySet()
        return set.contains(eventId.toString())
    }

    fun hideEventForUser(context: Context, userId: String, eventId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(hiddenKey(userId), emptySet()) ?: emptySet()).toMutableSet()
        current.add(eventId.toString())
        prefs.edit().putStringSet(hiddenKey(userId), current).apply()
    }

    fun unhideEventForUser(context: Context, userId: String, eventId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(hiddenKey(userId), emptySet()) ?: emptySet()).toMutableSet()
        current.remove(eventId.toString())
        prefs.edit().putStringSet(hiddenKey(userId), current).apply()
    }

    fun hideEventForUsers(context: Context, userIds: List<String>, eventId: Int) {
        userIds.distinct().forEach { hideEventForUser(context, it, eventId) }
    }
}
