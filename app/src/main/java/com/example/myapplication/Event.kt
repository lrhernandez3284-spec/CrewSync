package com.example.myapplication

data class Event(
    val title: String,
    val dateTime: String,    // keep as String for now
    val category: String,
    val location: String?,
    val notesPreview: String?
)