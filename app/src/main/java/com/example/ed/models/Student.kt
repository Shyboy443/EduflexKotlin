package com.example.ed.models

data class Student(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val enrolledCourses: List<String> = emptyList(), // course IDs
    val completedCourses: List<String> = emptyList(),
    val totalStudyTime: Long = 0, // in milliseconds
    val averageScore: Float = 0f,
    val level: Int = 1,
    val experiencePoints: Int = 0,
    val badges: List<Badge> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val preferences: StudentPreferences = StudentPreferences()
)

data class StudentProgress(
    val studentId: String = "",
    val courseId: String = "",
    val lessonProgress: Map<String, LessonProgress> = emptyMap(), // lessonId -> progress
    val quizScores: Map<String, Float> = emptyMap(), // quizId -> score percentage
    val totalTimeSpent: Long = 0, // in milliseconds
    val completionPercentage: Float = 0f,
    val lastAccessedAt: Long = 0,
    val currentLessonId: String = "",
    val streakDays: Int = 0,
    val totalSessions: Int = 0
)

data class LessonProgress(
    val lessonId: String = "",
    val isCompleted: Boolean = false,
    val timeSpent: Long = 0, // in milliseconds
    val lastPosition: Long = 0, // for video lessons - position in milliseconds
    val completedAt: Long = 0,
    val notes: String = "",
    val bookmarks: List<Bookmark> = emptyList()
)

data class Bookmark(
    val id: String = "",
    val lessonId: String = "",
    val position: Long = 0, // position in video/content
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val earnedAt: Long = 0,
    val category: BadgeCategory = BadgeCategory.ACHIEVEMENT
)

data class StudentPreferences(
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val studyReminders: Boolean = true,
    val preferredStudyTime: String = "evening", // morning, afternoon, evening
    val language: String = "en",
    val theme: String = "light" // light, dark, auto
)

data class Discussion(
    val id: String = "",
    val courseId: String = "",
    val lessonId: String = "",
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "student",
    val replies: List<DiscussionReply> = emptyList(),
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val isResolved: Boolean = false,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DiscussionReply(
    val id: String = "",
    val discussionId: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "student",
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val isAcceptedAnswer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BadgeCategory {
    ACHIEVEMENT,
    COMPLETION,
    STREAK,
    PERFORMANCE,
    PARTICIPATION
}

enum class Permission {
    VIEW_COURSES,
    MANAGE_COURSES,
    VIEW_STUDENT_PROGRESS,
    MANAGE_USERS,
    VIEW_ANALYTICS,
    MANAGE_SYSTEM,
    GRADE_ASSIGNMENTS,
    SUBMIT_ASSIGNMENTS,
    VIEW_GRADES
}