package com.crewsync.crewsync

import android.content.Context
import java.security.MessageDigest

object PinPrefs {
    private const val PREFS_NAME = "crewsync_prefs"
    private const val KEY_PIN_HASH_PREFIX = "pin_hash_"

    private fun keyForUser(userId: String): String = KEY_PIN_HASH_PREFIX + userId

    fun hasPin(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(keyForUser(userId))
    }

    fun savePin(context: Context, userId: String, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(keyForUser(userId), sha256(pin)).apply()
    }

    fun verifyPin(context: Context, userId: String, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(keyForUser(userId), null) ?: return false
        return stored == sha256(pin)
    }

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun resetPin(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(keyForUser(userId)).apply()
    }
}

