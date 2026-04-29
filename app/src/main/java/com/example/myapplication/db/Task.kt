package com.example.myapplication.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Int = 0,
    val eventId: Int,
    val title: String,
    val category: String,
    val dueDateMillis: Long,
    val isDone: Boolean = false,
    val createdByUserId: String
)
