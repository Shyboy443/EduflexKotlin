package com.example.ed.models

data class StudentEnrollment(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val enrolledAt: Long = 0L,
    val progress: Int = 0,
    val isActive: Boolean = true
)
