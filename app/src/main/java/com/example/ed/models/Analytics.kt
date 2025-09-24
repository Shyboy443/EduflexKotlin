package com.example.ed.models

data class TeacherAnalytics(
    val teacherId: String = "",
    val totalStudents: Int = 0,
    val activeCourses: Int = 0,
    val totalCourses: Int = 0,
    val averageRating: Float = 0f,
    val totalRevenue: Double = 0.0,
    val monthlyStats: List<MonthlyStats> = emptyList(),
    val coursePerformance: List<CourseAnalytics> = emptyList(),
    val studentEngagement: StudentEngagementStats = StudentEngagementStats(),
    val quizAnalytics: QuizAnalytics = QuizAnalytics(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class CourseAnalytics(
    val courseId: String = "",
    val courseName: String = "",
    val enrolledStudents: Int = 0,
    val completionRate: Float = 0f,
    val averageScore: Float = 0f,
    val averageTimeSpent: Long = 0, // in milliseconds
    val dropoutRate: Float = 0f,
    val rating: Float = 0f,
    val totalRevenue: Double = 0.0,
    val lessonAnalytics: List<LessonAnalytics> = emptyList(),
    val weeklyProgress: List<WeeklyProgress> = emptyList(),
    val studentFeedback: List<StudentFeedback> = emptyList()
)

data class LessonAnalytics(
    val lessonId: String = "",
    val lessonName: String = "",
    val completionRate: Float = 0f,
    val averageTimeSpent: Long = 0,
    val dropoffRate: Float = 0f, // percentage of students who don't complete
    val replayRate: Float = 0f, // percentage who replay the lesson
    val engagementScore: Float = 0f // calculated based on various factors
)

data class QuizAnalytics(
    val totalQuizzes: Int = 0,
    val averageScore: Float = 0f,
    val completionRate: Float = 0f,
    val averageAttempts: Float = 0f,
    val questionAnalytics: List<QuestionAnalytics> = emptyList(),
    val difficultyDistribution: Map<QuizDifficulty, Int> = emptyMap()
)

data class QuestionAnalytics(
    val questionId: String = "",
    val question: String = "",
    val correctAnswerRate: Float = 0f,
    val averageTimeSpent: Long = 0,
    val commonWrongAnswers: List<String> = emptyList(),
    val difficultyRating: Float = 0f // calculated based on student performance
)

data class StudentEngagementStats(
    val dailyActiveUsers: Int = 0,
    val weeklyActiveUsers: Int = 0,
    val monthlyActiveUsers: Int = 0,
    val averageSessionDuration: Long = 0, // in milliseconds
    val averageSessionsPerWeek: Float = 0f,
    val retentionRate: Map<String, Float> = emptyMap(), // "7day", "30day", etc.
    val engagementTrends: List<EngagementTrend> = emptyList()
)

data class EngagementTrend(
    val date: String = "", // YYYY-MM-DD format
    val activeUsers: Int = 0,
    val totalSessions: Int = 0,
    val averageSessionDuration: Long = 0,
    val completedLessons: Int = 0,
    val quizzesCompleted: Int = 0
)

data class MonthlyStats(
    val month: String = "", // YYYY-MM format
    val newStudents: Int = 0,
    val activeStudents: Int = 0,
    val coursesCompleted: Int = 0,
    val revenue: Double = 0.0,
    val averageRating: Float = 0f,
    val totalStudyHours: Long = 0 // in hours
)

data class WeeklyProgress(
    val weekStartDate: String = "", // YYYY-MM-DD format
    val newEnrollments: Int = 0,
    val lessonsCompleted: Int = 0,
    val quizzesCompleted: Int = 0,
    val averageScore: Float = 0f,
    val studyTime: Long = 0 // in milliseconds
)

data class StudentFeedback(
    val studentId: String = "",
    val studentName: String = "",
    val rating: Int = 0, // 1-5 stars
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true
)

data class DashboardMetrics(
    val totalStudents: Int = 0,
    val activeStudents: Int = 0,
    val courseCompletionRate: Float = 0f,
    val pendingAssignments: Int = 0,
    val upcomingClasses: Int = 0,
    val averageQuizScore: Float = 0f,
    val totalRevenue: Double = 0.0,
    val monthlyGrowth: Float = 0f, // percentage
    val topPerformingCourse: String = "",
    val recentActivity: List<RecentActivity> = emptyList()
)

data class RecentActivity(
    val id: String = "",
    val type: ActivityType = ActivityType.LESSON_COMPLETED,
    val description: String = "",
    val studentName: String = "",
    val courseName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActivityType {
    LESSON_COMPLETED,
    QUIZ_SUBMITTED,
    COURSE_ENROLLED,
    DISCUSSION_POSTED,
    ASSIGNMENT_SUBMITTED,
    COURSE_COMPLETED
}