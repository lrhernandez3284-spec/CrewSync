package com.crewsync.crewsync.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_assignments",
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["userId"]),
        Index(value = ["taskId", "userId"], unique = true)
    ]
)
data class TaskAssignment(
    @PrimaryKey(autoGenerate = true) val assignmentId: Int = 0,
    val taskId: Int,
    val userId: String
)
