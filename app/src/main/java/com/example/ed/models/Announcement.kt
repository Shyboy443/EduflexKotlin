package com.example.ed.models

import java.util.*

data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val courseId: String = "",
    val createdAt: Date = Date(),
    val isImportant: Boolean = false,
    val authorId: String = "",
    val authorName: String = "",
    val attachments: List<String> = emptyList(),
    val isPublished: Boolean = true,
    val targetAudience: String = "all" // "all", "students", "teachers"
)