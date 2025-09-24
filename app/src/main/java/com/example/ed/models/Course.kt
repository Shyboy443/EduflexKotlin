package com.example.ed.models

data class Course(
    val id: String = "",
    val title: String = "",
    val instructor: String = "",
    val description: String = "",
    val progress: Int = 0, // Progress percentage (0-100)
    val totalLessons: Int = 0,
    val completedLessons: Int = 0,
    val duration: String = "", // e.g., "2h 30min"
    val difficulty: String = "", // Beginner, Intermediate, Advanced
    val category: String = "",
    val thumbnailUrl: String = "",
    val isBookmarked: Boolean = false,
    val rating: Float = 0.0f,
    val enrolledStudents: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val courseContent: List<CourseSection> = emptyList(),
    val isPublished: Boolean = false,
    val teacherId: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val isFree: Boolean = false,
    val deadline: Long? = null, // Course deadline timestamp
    val hasDeadline: Boolean = false // Whether the course has a deadline
)

data class CourseSection(
    val id: String = "",
    val title: String = "",
    val lessons: List<Lesson> = emptyList(),
    val isExpanded: Boolean = false
)

data class Lesson(
    val id: String = "",
    val title: String = "",
    val duration: String = "", // e.g., "15 min"
    val type: LessonType = LessonType.VIDEO,
    val isCompleted: Boolean = false,
    val videoUrl: String = "",
    val description: String = "",
    val order: Int = 0
)

enum class LessonType {
    VIDEO,
    READING,
    QUIZ,
    PRACTICE
}