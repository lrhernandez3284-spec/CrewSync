package com.crewsync.crewsync.models

data class NotificationItem(
    val id: Long,
    val userId: String,
    val actorUserId: String,
    val message: String,
    val targetType: String,
    val eventId: Int,
    val taskId: Int = 0,
    val createdAtMillis: Long
)
