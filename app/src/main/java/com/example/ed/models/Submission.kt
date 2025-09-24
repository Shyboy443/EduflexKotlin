package com.example.ed.models

import java.util.*

enum class SubmissionStatus {
    SUBMITTED,
    GRADED,
    NEEDS_REVIEW,
    LATE,
    PENDING
}

data class Submission(
    val id: String = "",
    val assignmentId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val submittedAt: Date = Date(),
    val status: SubmissionStatus = SubmissionStatus.SUBMITTED,
    val grade: Double? = null,
    val feedback: String = "",
    val content: String = "",
    val attachments: List<String> = emptyList(),
    val gradedAt: Date? = null,
    val gradedBy: String? = null
)

data class StudentSubmission(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val assignmentId: String = "",
    val submittedAt: Date = Date(),
    val content: String = "",
    val attachments: List<String> = emptyList(),
    var status: SubmissionStatus = SubmissionStatus.SUBMITTED,
    var grade: Double? = null,
    var feedback: String? = null,
    var gradedAt: Date? = null,
    var gradedBy: String? = null
)