package com.example.ed.models

import java.util.*

enum class NotificationType {
    ASSIGNMENT_DUE,
    GRADE_POSTED,
    DISCUSSION_REPLY,
    COURSE_UPDATE,
    SYSTEM,
    REMINDER
}

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val data: Map<String, String> = emptyMap(),
    var isRead: Boolean = false,
    val createdAt: Date = Date(),
    val scheduledAt: Date? = null
)