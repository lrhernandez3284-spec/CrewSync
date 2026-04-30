package com.crewsync.crewsync

data class Event(
    val id: Int,
    val title: String,
    val dateTime: String,
    val category: String,
    val location: String?,
    val notesPreview: String?,
    val dateTimeMillis: Long? = null
)
