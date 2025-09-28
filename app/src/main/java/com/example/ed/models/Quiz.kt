package com.example.ed.models

import java.util.*

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val courseId: String = "",
    val teacherId: String = "",
    val questions: List<Question> = emptyList(),
    val timeLimit: Int = 0, // in minutes
    val maxAttempts: Int = 1,
    val passingScore: Int = 70, // percentage
    val isPublished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val aiGenerated: Boolean = false,
    val totalPoints: Int = 0
)

data class Question(
    val id: String = "",
    val quizId: String = "",
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val question: String = "",
    val options: List<String> = emptyList(), // For multiple choice, matching
    val correctAnswers: List<String> = emptyList(), // Can be multiple for matching/multiple select
    val explanation: String = "",
    val points: Int = 1,
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val topic: String = "",
    val order: Int = 0,
    val aiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Alias for compatibility with lesson system
typealias QuizQuestion = Question

data class QuizAttempt(
    val id: String = "",
    val quizId: String = "",
    val studentId: String = "",
    val answers: Map<String, List<String>> = emptyMap(), // questionId -> selected answers
    val score: Int = 0,
    val totalPoints: Int = 0,
    val percentage: Float = 0f,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val timeSpent: Long = 0, // in milliseconds
    val attemptNumber: Int = 1,
    val isCompleted: Boolean = false,
    val feedback: String = "",
    val gradedBy: String = "", // teacherId for manual grading
    val gradedAt: Long = 0
)

data class QuizResult(
    val attemptId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val score: Int = 0,
    val percentage: Float = 0f,
    val timeSpent: Long = 0,
    val completedAt: Long = 0,
    val questionResults: List<QuestionResult> = emptyList()
)

data class QuestionResult(
    val questionId: String = "",
    val question: String = "",
    val studentAnswers: List<String> = emptyList(),
    val correctAnswers: List<String> = emptyList(),
    val isCorrect: Boolean = false,
    val pointsEarned: Int = 0,
    val pointsPossible: Int = 0
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER,
    ESSAY,
    MATCHING,
    FILL_IN_BLANK,
    MULTIPLE_SELECT
}

enum class QuizDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class QuestionDifficulty {
    EASY,
    MEDIUM,
    HARD
}