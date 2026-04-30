package com.crewsync.crewsync

import android.content.Context

object UserStore {
    private const val PREFS_NAME = "crewsync_prefs"
    private const val KEY_USERS = "users_set"

    private val defaultUsers = setOf("manager", "luis", "mike", "guest")

    fun getUsers(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_USERS, null) ?: defaultUsers

        // ensure manager always exists and normalize
        val normalized = (set + "manager")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        // initialize store if missing
        if (prefs.getStringSet(KEY_USERS, null) == null) {
            prefs.edit().putStringSet(KEY_USERS, normalized).apply()
        }

        return normalized.toList().sorted()
    }

    fun addUser(context: Context, userId: String): Boolean {
        val id = userId.trim().lowercase()
        if (id.isBlank()) return false
        if (!id.matches(Regex("^[a-z0-9_]{3,20}$"))) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_USERS, defaultUsers) ?: defaultUsers).toMutableSet()
        val added = current.add(id)

        // ensure manager always exists
        current.add("manager")
        prefs.edit().putStringSet(KEY_USERS, current).apply()
        return added
    }

    fun removeUser(context: Context, userId: String): Boolean {
        val id = userId.trim().lowercase()
        if (id == "manager") return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_USERS, defaultUsers) ?: defaultUsers).toMutableSet()
        val removed = current.remove(id)

        current.add("manager")
        prefs.edit().putStringSet(KEY_USERS, current).apply()
        return removed
    }
}
