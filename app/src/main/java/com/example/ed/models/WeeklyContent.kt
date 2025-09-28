package com.example.ed.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

data class WeeklyContent(
    val id: String = "",
    val courseId: String = "",
    val weekNumber: Int = 0,
    val title: String = "",
    val description: String = "",
    val learningObjectives: List<String> = emptyList(),
    val contentItems: List<ContentItem> = emptyList(),
    val assignments: List<WeeklyAssignment> = emptyList(),
    val resources: List<WeeklyResource> = emptyList(),
    val quiz: Quiz? = null,
    val releaseDate: Long = 0,
    val dueDate: Long = 0,
    val isPublished: Boolean = false,
    val isAIEnhanced: Boolean = false,
    val aiEnhancements: AIEnhancements = AIEnhancements(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val studentProgress: Map<String, WeeklyProgress> = emptyMap(),
    val estimatedDuration: Int = 0, // in minutes
    val difficulty: CourseDifficulty = CourseDifficulty.INTERMEDIATE,
    val tags: List<String> = emptyList(),
    val prerequisites: List<String> = emptyList() // Previous week IDs
)

@Parcelize
data class ContentItem(
    val id: String = "",
    val type: ContentType = ContentType.TEXT,
    val title: String = "",
    val content: String = "",
    val mediaUrl: String = "",
    val duration: Int = 0, // in minutes
    val order: Int = 0,
    val isRequired: Boolean = true,
    val aiGenerated: Boolean = false,
    val aiPrompt: String = "",
    val interactiveElements: List<InteractiveElement> = emptyList(),
    val attachments: List<ContentAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

data class WeeklyAssignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val type: AssignmentType = AssignmentType.ESSAY,
    val submissionFormat: SubmissionFormat = SubmissionFormat.PDF,
    val maxPoints: Int = 100,
    val dueDate: Long = 0,
    val allowLateSubmission: Boolean = false,
    val latePenaltyPercentage: Int = 0,
    val resources: List<String> = emptyList(),
    val rubric: List<String> = emptyList(), // Simplified for now
    val isGroupAssignment: Boolean = false,
    val estimatedDuration: Int = 60, // in minutes
    val aiGeneratedFeedback: Boolean = false,
    val autoGrading: Boolean = false,
    val peerReview: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class WeeklyResource(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ResourceType = ResourceType.DOCUMENT,
    val url: String = "",
    val fileSize: Long = 0,
    val isDownloadable: Boolean = true,
    val category: ResourceCategory = ResourceCategory.READING,
    val aiSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Parcelize
data class ContentAttachment(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val type: String = "",
    val size: Long = 0,
    val uploadedAt: Long = System.currentTimeMillis()
) : Parcelable

data class WeeklyProgress(
    val studentId: String = "",
    val weekId: String = "",
    val contentItemsCompleted: List<String> = emptyList(),
    val assignmentsSubmitted: List<String> = emptyList(),
    val quizScore: Int = 0,
    val totalTimeSpent: Long = 0,
    val lastAccessedAt: Long = 0,
    val completionPercentage: Int = 0,
    val status: WeekStatus = WeekStatus.NOT_STARTED,
    val notes: String = "",
    val feedback: String = ""
)

data class AIEnhancements(
    val summaryGenerated: Boolean = false,
    val keyPointsExtracted: Boolean = false,
    val questionsGenerated: Boolean = false,
    val additionalResourcesSuggested: Boolean = false,
    val difficultyAnalyzed: Boolean = false,
    val learningPathOptimized: Boolean = false,
    val summary: String = "",
    val keyPoints: List<String> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
    val additionalResources: List<AIResource> = emptyList(),
    val difficultyScore: Float = 0f,
    val improvementSuggestions: List<String> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class AIResource(
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val type: ResourceType = ResourceType.LINK,
    val relevanceScore: Float = 0f,
    val source: String = ""
)

enum class ContentType {
    TEXT,
    VIDEO,
    AUDIO,
    PRESENTATION,
    DOCUMENT,
    INTERACTIVE,
    LIVE_SESSION,
    DISCUSSION,
    CASE_STUDY,
    SIMULATION
}

enum class ResourceCategory {
    READING,
    REFERENCE,
    SUPPLEMENTARY,
    TOOL,
    TEMPLATE,
    EXAMPLE,
    EXERCISE
}

enum class WeekStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE,
    LOCKED
}

// Course Content Template for AI Generation
data class ContentTemplate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val subject: String = "",
    val difficulty: CourseDifficulty = CourseDifficulty.INTERMEDIATE,
    val weeklyStructure: List<WeekTemplate> = emptyList(),
    val aiPrompts: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    val isPublic: Boolean = false,
    val usageCount: Int = 0,
    val rating: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

data class WeekTemplate(
    val weekNumber: Int = 0,
    val title: String = "",
    val objectives: List<String> = emptyList(),
    val contentStructure: List<ContentStructure> = emptyList(),
    val assessmentTypes: List<AssignmentType> = emptyList(),
    val estimatedDuration: Int = 0
)

data class ContentStructure(
    val type: ContentType = ContentType.TEXT,
    val title: String = "",
    val duration: Int = 0,
    val isRequired: Boolean = true,
    val aiPrompt: String = ""
)
