package com.crewsync.crewsync

import android.content.Context

object CategoryStore {
    private const val PREFS = "crewsync_prefs"
    private const val KEY_CATEGORIES = "saved_categories"

    private val defaultCategories = setOf(
        "General",
        "Rehearsal",
        "Performance",
        "Meeting",
        "Personal",
        "Content"
    )

    fun getCategories(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_CATEGORIES, null)

        if (stored == null) {
            prefs.edit().putStringSet(KEY_CATEGORIES, defaultCategories).apply()
            return defaultCategories.sorted()
        }

        return stored
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun addCategory(context: Context, category: String): Boolean {
        val cleaned = category.trim()
        if (cleaned.isBlank()) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = getCategories(context).toMutableSet()

        val alreadyExists = current.any { it.equals(cleaned, ignoreCase = true) }
        if (alreadyExists) return false

        current.add(cleaned)

        prefs.edit()
            .putStringSet(KEY_CATEGORIES, current)
            .apply()

        return true
    }

    fun renameCategory(context: Context, oldCategory: String, newCategory: String): Boolean {
        val oldClean = oldCategory.trim()
        val newClean = newCategory.trim()

        if (oldClean.isBlank() || newClean.isBlank()) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = getCategories(context).toMutableSet()

        if (!current.any { it.equals(oldClean, ignoreCase = true) }) return false

        val duplicate = current.any {
            it.equals(newClean, ignoreCase = true) && !it.equals(oldClean, ignoreCase = true)
        }
        if (duplicate) return false

        val updated = current
            .filterNot { it.equals(oldClean, ignoreCase = true) }
            .toMutableSet()

        updated.add(newClean)

        prefs.edit()
            .putStringSet(KEY_CATEGORIES, updated)
            .apply()

        return true
    }

    fun deleteCategory(context: Context, category: String): Boolean {
        val cleaned = category.trim()
        if (cleaned.isBlank()) return false
        if (cleaned.equals("General", ignoreCase = true)) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = getCategories(context).toMutableSet()

        val removed = current.removeAll { it.equals(cleaned, ignoreCase = true) }

        if (!current.any { it.equals("General", ignoreCase = true) }) {
            current.add("General")
        }

        prefs.edit()
            .putStringSet(KEY_CATEGORIES, current)
            .apply()

        return removed
    }
}
