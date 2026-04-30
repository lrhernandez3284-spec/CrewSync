package com.crewsync.crewsync.data

import android.content.Context

object UserPrefs {
    private const val PREFS_NAME = "crewsync_prefs"
    private const val KEY_USER = "current_user"

    fun getCurrentUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER, "luis") ?: "luis"
    }

    fun setCurrentUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER, userId).apply()
    }
}

