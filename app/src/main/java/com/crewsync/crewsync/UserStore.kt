package com.crewsync.crewsync

import android.content.Context

object UserStore {
    private const val PREFS_NAME = "crewsync_prefs"
    private const val KEY_USERS = "users_set"
    private const val KEY_DISPLAY_PREFIX = "display_name_"

    private val defaultUsers = setOf("manager", "luis", "mike", "guest")

    fun getUsers(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_USERS, null) ?: defaultUsers

        val normalized = (set + "manager")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        if (prefs.getStringSet(KEY_USERS, null) == null) {
            prefs.edit().putStringSet(KEY_USERS, normalized).apply()
        }

        return normalized.toList().sorted()
    }

    fun getDisplayName(context: Context, userId: String): String {
        val id = userId.trim().lowercase()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISPLAY_PREFIX + id, null)
            ?.takeIf { it.isNotBlank() }
            ?: id
    }

    fun setDisplayName(context: Context, userId: String, displayName: String) {
        val id = userId.trim().lowercase()
        val cleaned = displayName.trim().ifBlank { id }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DISPLAY_PREFIX + id, cleaned)
            .apply()
    }

    fun getUserLabel(context: Context, userId: String): String {
        val display = getDisplayName(context, userId)
        return if (display == userId) userId else "$display ($userId)"
    }

    fun addUser(context: Context, userId: String, displayName: String? = null): Boolean {
        val id = userId.trim().lowercase()
        if (id.isBlank()) return false
        if (!id.matches(Regex("^[a-z0-9_]{3,20}$"))) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_USERS, defaultUsers) ?: defaultUsers).toMutableSet()
        val added = current.add(id)

        current.add("manager")
        prefs.edit().putStringSet(KEY_USERS, current).apply()

        if (added) {
            setDisplayName(context, id, displayName?.trim().takeUnless { it.isNullOrBlank() } ?: id)
        }

        return added
    }

    fun removeUser(context: Context, userId: String): Boolean {
        val id = userId.trim().lowercase()
        if (id == "manager") return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_USERS, defaultUsers) ?: defaultUsers).toMutableSet()
        val removed = current.remove(id)

        current.add("manager")
        prefs.edit()
            .putStringSet(KEY_USERS, current)
            .remove(KEY_DISPLAY_PREFIX + id)
            .apply()

        return removed
    }
}
