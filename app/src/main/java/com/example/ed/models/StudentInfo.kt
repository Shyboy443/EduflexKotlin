package com.example.ed.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StudentInfo(
    val studentId: String = "",
    val fullName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val enrolledCourses: List<EnrollmentInfo> = emptyList(),
    val totalEnrolledCourses: Int = 0,
    val averageProgress: Double = 0.0,
    val lastActiveTimestamp: Long = 0L,
    val isActive: Boolean = true,
    val joinedTimestamp: Long = 0L,
    val totalPointsEarned: Int = 0,
    val completedAssignments: Int = 0,
    val totalAssignments: Int = 0
) : Parcelable

@Parcelize
data class EnrollmentInfo(
    val courseId: String = "",
    val courseName: String = "",
    val enrollmentDate: Long = 0L,
    val progress: Double = 0.0,
    val isCompleted: Boolean = false,
    val lastAccessedDate: Long = 0L,
    val pointsEarned: Int = 0,
    val assignmentsCompleted: Int = 0,
    val totalAssignments: Int = 0
) : Parcelable

@Parcelize
data class StudentProgress(
    val studentId: String = "",
    val courseId: String = "",
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val completedQuizzes: Int = 0,
    val totalQuizzes: Int = 0,
    val averageQuizScore: Double = 0.0,
    val timeSpentHours: Double = 0.0,
    val streakDays: Int = 0,
    val lastActivityDate: Long = 0L,
    val achievements: List<String> = emptyList()
) : Parcelable

enum class StudentStatus {
    ACTIVE,
    INACTIVE,
    COMPLETED,
    DROPPED_OUT
}
