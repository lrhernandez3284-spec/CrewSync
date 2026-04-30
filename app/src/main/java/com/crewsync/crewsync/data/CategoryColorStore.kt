package com.crewsync.crewsync.data

import android.content.Context
import android.graphics.Color
import java.util.Locale

object CategoryColorStore {
    private const val PREFS = "crewsync_prefs"
    private const val PREFIX = "cat_color_"

    private fun key(category: String): String =
        PREFIX + category.trim().lowercase(Locale.US)

    fun getColor(context: Context, category: String): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = key(category)

        // If we already have a stored color, return it
        val stored = prefs.getInt(k, Int.MIN_VALUE)
        if (stored != Int.MIN_VALUE) return stored

        // Otherwise generate one, store it, and return it
        val generated = generateColor(category)
        prefs.edit().putInt(k, generated).apply()
        return generated
    }

    fun setColor(context: Context, category: String, colorInt: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(key(category), colorInt).apply()
    }

    fun removeColor(context: Context, category: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(key(category)).apply()
    }

    // Deterministic “unique-ish” color per category string
    private fun generateColor(category: String): Int {
        val seed = category.trim().lowercase(Locale.US).hashCode()

        // Hue based on hash, keep saturation/value high for readability
        val hue = ((seed % 360) + 360) % 360
        val hsv = floatArrayOf(hue.toFloat(), 0.60f, 0.90f)
        return Color.HSVToColor(hsv)
    }
}
