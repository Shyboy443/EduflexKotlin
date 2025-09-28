package com.example.ed.models

import java.util.*

data class LessonContent(
    val id: String = "",
    val courseId: String = "",
    val sectionId: String = "",
    val title: String = "",
    val description: String = "",
    val type: LessonContentType = LessonContentType.TEXT,
    val content: String = "", // Text content, video URL, or file path
    val duration: Int = 0, // Duration in minutes
    val order: Int = 0,
    val isRequired: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "", // Teacher ID
    val aiGenerated: Boolean = false,
    val aiPrompt: String = "", // Original AI prompt used to generate content
    val resources: List<LessonResource> = emptyList(),
    val quiz: Quiz? = null, // Associated quiz
    val prerequisites: List<String> = emptyList(), // Lesson IDs that must be completed first
    val learningObjectives: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

data class LessonResource(
    val id: String = "",
    val title: String = "",
    val type: ResourceType = ResourceType.DOCUMENT,
    val url: String = "", // Local path or external URL
    val description: String = "",
    val fileSize: Long = 0,
    val mimeType: String = ""
)

enum class LessonContentType {
    TEXT,
    VIDEO,
    AUDIO,
    DOCUMENT,
    PRESENTATION,
    INTERACTIVE,
    QUIZ,
    ASSIGNMENT
}

enum class ResourceType {
    DOCUMENT,
    IMAGE,
    VIDEO,
    AUDIO,
    LINK,
    FILE
}

// Quiz class moved to Quiz.kt to avoid duplication

// QuizQuestion class moved to Quiz.kt to avoid duplication

// QuestionType enum moved to Quiz.kt to avoid duplication

// StudentProgress class moved to Student.kt to avoid duplication

// QuizAttempt class moved to Quiz.kt to avoid duplication

enum class ProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    LOCKED // Prerequisites not met
}

data class CourseProgress(
    val studentId: String = "",
    val courseId: String = "",
    val overallProgress: Int = 0, // Overall completion percentage
    val lessonsCompleted: Int = 0,
    val totalLessons: Int = 0,
    val quizzesCompleted: Int = 0,
    val totalQuizzes: Int = 0,
    val averageQuizScore: Int = 0,
    val totalTimeSpent: Long = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val enrolledAt: Long = 0,
    val certificateEarned: Boolean = false,
    val certificateEarnedAt: Long = 0
)

data class AIContentRequest(
    val id: String = "",
    val type: AIContentType = AIContentType.LESSON,
    val prompt: String = "",
    val subject: String = "",
    val difficulty: String = "Intermediate",
    val duration: Int = 30, // Target duration in minutes
    val learningObjectives: List<String> = emptyList(),
    val additionalContext: String = "",
    val requestedBy: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: AIRequestStatus = AIRequestStatus.PENDING,
    val generatedContent: String = "",
    val completedAt: Long = 0,
    val errorMessage: String = ""
)

enum class AIContentType {
    LESSON,
    QUIZ,
    ASSIGNMENT,
    EXPLANATION
}

enum class AIRequestStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
