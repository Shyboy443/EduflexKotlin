package com.example.ed.models

// UI-specific models for display purposes
data class LessonProgressUI(
    val id: String,
    val lessonId: String,
    val title: String,
    val completionPercentage: Int,
    val completedAt: Long?, // timestamp in milliseconds, null if not completed
    val isCompleted: Boolean
)

data class DiscussionUI(
    val id: String,
    val title: String,
    val author: String,
    val content: String,
    val createdAt: Long,
    val replies: List<DiscussionReplyUI> = emptyList()
)

data class DiscussionReplyUI(
    val id: String,
    val discussionId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Long
)