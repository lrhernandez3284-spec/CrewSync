package com.crewsync.crewsync.models

data class Event(
    val id: Int,
    val title: String,
    val dateTime: String,
    val category: String,
    val location: String?,
    val notesPreview: String?,
    val dateTimeMillis: Long? = null,
    val assignedUserIds: List<String> = emptyList()
)
