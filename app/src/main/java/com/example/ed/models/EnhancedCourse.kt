package com.example.ed.models

import java.util.*

data class EnhancedCourse(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val longDescription: String = "",
    val instructor: TeacherProfile = TeacherProfile(),
    val category: CourseCategory = CourseCategory(),
    val subcategory: String = "",
    val difficulty: CourseDifficulty = CourseDifficulty.BEGINNER,
    val language: String = "en",
    val duration: CourseDuration = CourseDuration(),
    val pricing: CoursePricing = CoursePricing(),
    val thumbnailUrl: String = "",
    val previewVideoUrl: String = "",
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val learningObjectives: List<String> = emptyList(),
    val prerequisites: List<String> = emptyList(),
    val targetAudience: List<String> = emptyList(),
    val courseStructure: CourseStructure = CourseStructure(),
    val assessments: List<Assessment> = emptyList(),
    val resources: List<CourseResource> = emptyList(),
    val settings: CourseSettings = CourseSettings(),
    val analytics: CourseAnalytics = CourseAnalytics(),
    val reviews: List<CourseReview> = emptyList(),
    val enrollmentInfo: EnrollmentInfo = EnrollmentInfo(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val publishedAt: Long = 0,
    val status: CourseStatus = CourseStatus.DRAFT,
    val version: String = "1.0"
)

data class CourseStructure(
    val modules: List<CourseModule> = emptyList(),
    val totalLessons: Int = 0,
    val totalDuration: Long = 0, // in milliseconds
    val totalQuizzes: Int = 0,
    val totalAssignments: Int = 0,
    val completionCriteria: CompletionCriteria = CompletionCriteria()
)

data class CourseModule(
    val id: String = "",
    val courseId: String = "",
    val title: String = "",
    val description: String = "",
    val order: Int = 0,
    val isLocked: Boolean = false,
    val unlockConditions: List<UnlockCondition> = emptyList(),
    val lessons: List<EnhancedLesson> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val resources: List<ModuleResource> = emptyList(),
    val estimatedDuration: Long = 0, // in milliseconds
    val learningObjectives: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : java.io.Serializable

data class EnhancedLesson(
    val id: String = "",
    val moduleId: String = "",
    val title: String = "",
    val description: String = "",
    val content: LessonContent = LessonContent(),
    val type: LessonType = LessonType.VIDEO,
    val order: Int = 0,
    val duration: Long = 0, // in milliseconds
    val isPreview: Boolean = false,
    val isMandatory: Boolean = true,
    val prerequisites: List<String> = emptyList(), // lesson IDs
    val learningObjectives: List<String> = emptyList(),
    val resources: List<LessonResource> = emptyList(),
    val interactions: List<LessonInteraction> = emptyList(),
    val notes: List<LessonNote> = emptyList(),
    val transcripts: List<Transcript> = emptyList(),
    val captions: List<Caption> = emptyList(),
    val accessibility: AccessibilityFeatures = AccessibilityFeatures(),
    val analytics: LessonAnalytics = LessonAnalytics(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class LessonContent(
    val videoUrl: String = "",
    val audioUrl: String = "",
    val textContent: String = "",
    val htmlContent: String = "",
    val pdfUrl: String = "",
    val presentationUrl: String = "",
    val interactiveElements: List<InteractiveElement> = emptyList(),
    val downloadableFiles: List<DownloadableFile> = emptyList()
)

data class InteractiveElement(
    val id: String = "",
    val type: InteractionType = InteractionType.QUIZ,
    val position: Long = 0, // position in video/content (milliseconds)
    val content: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val feedback: String = "",
    val points: Int = 0
)

data class LessonResource(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ResourceType = ResourceType.DOCUMENT,
    val url: String = "",
    val fileSize: Long = 0,
    val downloadable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Assignment(
    val id: String = "",
    val moduleId: String = "",
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val type: AssignmentType = AssignmentType.ESSAY,
    val maxPoints: Int = 100,
    val dueDate: Long = 0,
    val submissionFormat: List<SubmissionFormat> = emptyList(),
    val rubric: AssignmentRubric = AssignmentRubric(),
    val resources: List<AssignmentResource> = emptyList(),
    val peerReview: PeerReviewSettings = PeerReviewSettings(),
    val autoGrading: AutoGradingSettings = AutoGradingSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class CourseCategory(
    val id: String = "",
    val name: String = "",
    val parentId: String = "",
    val iconUrl: String = "",
    val color: String = ""
)

data class TeacherProfile(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val credentials: List<String> = emptyList(),
    val experience: String = "",
    val rating: Float = 0f,
    val totalStudents: Int = 0,
    val totalCourses: Int = 0
)

data class CourseDuration(
    val totalHours: Int = 0,
    val totalMinutes: Int = 0,
    val weeksToComplete: Int = 0,
    val hoursPerWeek: Int = 0
)

data class CoursePricing(
    val isFree: Boolean = true,
    val price: Double = 0.0,
    val currency: String = "USD",
    val discountPrice: Double = 0.0,
    val discountPercentage: Int = 0,
    val discountValidUntil: Long = 0
)

data class CourseSettings(
    val isPublished: Boolean = false,
    val allowEnrollment: Boolean = true,
    val maxStudents: Int = 0, // 0 = unlimited
    val enrollmentStartDate: Long = 0,
    val enrollmentEndDate: Long = 0,
    val courseStartDate: Long = 0,
    val courseEndDate: Long = 0,
    val allowDiscussions: Boolean = true,
    val allowDownloads: Boolean = true,
    val certificateEnabled: Boolean = false,
    val dripContent: Boolean = false, // release content gradually
    val moderationRequired: Boolean = false
)

data class EnrollmentInfo(
    val totalEnrolled: Int = 0,
    val activeStudents: Int = 0,
    val completedStudents: Int = 0,
    val averageRating: Float = 0f,
    val totalReviews: Int = 0,
    val completionRate: Float = 0f
)

data class CompletionCriteria(
    val minimumLessonsCompleted: Int = 0,
    val minimumQuizzesPassed: Int = 0,
    val minimumAssignmentsSubmitted: Int = 0,
    val minimumOverallScore: Float = 70f,
    val requireAllMandatoryContent: Boolean = true
)

data class UnlockCondition(
    val type: UnlockType = UnlockType.LESSON_COMPLETION,
    val targetId: String = "", // lesson/quiz/assignment ID
    val minimumScore: Float = 0f
)

enum class CourseStatus {
    DRAFT,
    UNDER_REVIEW,
    PUBLISHED,
    ARCHIVED,
    SUSPENDED
}

enum class CourseDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

enum class InteractionType {
    QUIZ,
    POLL,
    DISCUSSION,
    BOOKMARK,
    NOTE,
    HIGHLIGHT
}

enum class ResourceType {
    DOCUMENT,
    VIDEO,
    AUDIO,
    IMAGE,
    PRESENTATION,
    SPREADSHEET,
    ARCHIVE,
    LINK
}

enum class AssignmentType {
    ESSAY,
    MULTIPLE_CHOICE,
    FILE_UPLOAD,
    PEER_REVIEW,
    PROJECT,
    PRESENTATION,
    CODE_SUBMISSION
}

enum class SubmissionFormat {
    TEXT,
    FILE_UPLOAD,
    VIDEO,
    AUDIO,
    LINK,
    CODE
}

enum class UnlockType {
    LESSON_COMPLETION,
    QUIZ_PASS,
    ASSIGNMENT_SUBMISSION,
    TIME_BASED,
    MANUAL
}

// Additional supporting data classes
data class AssignmentRubric(
    val criteria: List<RubricCriterion> = emptyList(),
    val totalPoints: Int = 0
)

data class RubricCriterion(
    val name: String = "",
    val description: String = "",
    val maxPoints: Int = 0,
    val levels: List<RubricLevel> = emptyList()
)

data class RubricLevel(
    val name: String = "",
    val description: String = "",
    val points: Int = 0
)

data class PeerReviewSettings(
    val enabled: Boolean = false,
    val reviewsRequired: Int = 3,
    val reviewDeadline: Long = 0,
    val anonymousReviews: Boolean = true
)

data class AutoGradingSettings(
    val enabled: Boolean = false,
    val gradingCriteria: List<GradingCriterion> = emptyList()
)

data class GradingCriterion(
    val type: String = "",
    val weight: Float = 0f,
    val parameters: Map<String, Any> = emptyMap()
)

data class ModuleResource(
    val id: String = "",
    val title: String = "",
    val type: ResourceType = ResourceType.DOCUMENT,
    val url: String = "",
    val description: String = ""
)

data class AssignmentResource(
    val id: String = "",
    val title: String = "",
    val type: ResourceType = ResourceType.DOCUMENT,
    val url: String = "",
    val description: String = ""
)

data class LessonNote(
    val id: String = "",
    val content: String = "",
    val position: Long = 0, // position in video/content
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Transcript(
    val language: String = "",
    val content: String = "",
    val timestamps: List<TimestampedText> = emptyList()
)

data class Caption(
    val language: String = "",
    val url: String = "",
    val format: String = "srt"
)

data class TimestampedText(
    val startTime: Long = 0,
    val endTime: Long = 0,
    val text: String = ""
)

data class AccessibilityFeatures(
    val hasTranscripts: Boolean = false,
    val hasCaptions: Boolean = false,
    val hasAudioDescription: Boolean = false,
    val isScreenReaderFriendly: Boolean = false,
    val hasHighContrast: Boolean = false
)

data class DownloadableFile(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val fileSize: Long = 0,
    val format: String = ""
)

data class CourseResource(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ResourceType = ResourceType.DOCUMENT,
    val url: String = "",
    val category: String = "",
    val isDownloadable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class CourseReview(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rating: Int = 0, // 1-5 stars
    val comment: String = "",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val helpfulVotes: Int = 0
)

data class Assessment(
    val id: String = "",
    val title: String = "",
    val type: AssessmentType = AssessmentType.QUIZ,
    val moduleId: String = "",
    val weight: Float = 0f, // percentage of final grade
    val passingScore: Float = 70f,
    val maxAttempts: Int = 1,
    val timeLimit: Int = 0, // in minutes
    val createdAt: Long = System.currentTimeMillis()
)

enum class AssessmentType {
    QUIZ,
    ASSIGNMENT,
    PROJECT,
    EXAM,
    PEER_REVIEW
}

data class LessonInteraction(
    val id: String = "",
    val type: InteractionType = InteractionType.QUIZ,
    val position: Long = 0,
    val data: Map<String, Any> = emptyMap()
)