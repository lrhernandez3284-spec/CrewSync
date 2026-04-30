package com.crewsync.crewsync

import android.content.Context

object CategoryStore {
    private const val PREFS = "crewsync_prefs"
    private const val KEY_CATEGORIES = "saved_categories"

    private val defaultCategories = setOf(
        "Rehearsal",
        "Performance",
        "Meeting",
        "Personal",
        "Content",
        "General"
    )

    fun getCategories(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_CATEGORIES, null) ?: emptySet()

        val eventCategories = EventStore.getDefaultEvents()
            .map { it.category }
            .toSet()

        return (defaultCategories + saved + eventCategories)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun addCategory(context: Context, category: String) {
        val cleaned = category.trim()
        if (cleaned.isBlank()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_CATEGORIES, emptySet()) ?: emptySet()).toMutableSet()

        current.add(cleaned)

        prefs.edit()
            .putStringSet(KEY_CATEGORIES, current)
            .apply()
    }
}
