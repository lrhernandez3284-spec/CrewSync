package com.crewsync.crewsync

data class NotificationItem(
    val id: Long,
    val userId: String,
    val actorUserId: String,
    val message: String,
    val targetType: String, // "event" or "task"
    val eventId: Int,
    val taskId: Int = 0,
    val createdAtMillis: Long
)
