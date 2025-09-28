package com.example.ed.services

import android.content.Context
import android.util.Log
import com.example.ed.models.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class LessonContentService private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: LessonContentService? = null
        
        fun getInstance(): LessonContentService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LessonContentService().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "LessonContentService"
        private const val COLLECTION_LESSONS = "lesson_content"
        private const val COLLECTION_PROGRESS = "student_progress"
        private const val COLLECTION_QUIZ_ATTEMPTS = "quiz_attempts"
        private const val COLLECTION_COURSE_PROGRESS = "course_progress"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val aiService = AIContentService()
    
    // MARK: - Lesson Content Management
    
    suspend fun createLessonContent(lessonContent: LessonContent): Result<String> {
        return try {
            val docRef = firestore.collection(COLLECTION_LESSONS).document()
            val contentWithId = lessonContent.copy(id = docRef.id)
            
            docRef.set(contentWithId).await()
            Log.d(TAG, "Lesson content created successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating lesson content", e)
            Result.failure(e)
        }
    }
    
    suspend fun createAILessonContent(request: AIContentRequest): Result<LessonContent> {
        return try {
            Log.d(TAG, "Creating AI-generated lesson content")
            
            // Create a basic lesson content structure
            val lessonContent = LessonContent(
                courseId = request.additionalContext,
                title = request.prompt,
                description = "AI-generated lesson content",
                type = LessonContentType.TEXT,
                content = "Content will be generated...", // Placeholder
                duration = request.duration,
                aiGenerated = true,
                aiPrompt = request.prompt,
                learningObjectives = request.learningObjectives,
                createdBy = request.requestedBy
            )
            
            // Save to Firestore
            val docRef = firestore.collection(COLLECTION_LESSONS).document()
            val finalContent = lessonContent.copy(id = docRef.id)
            
            docRef.set(finalContent).await()
            Log.d(TAG, "AI lesson content created successfully: ${docRef.id}")
            Result.success(finalContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating AI lesson content", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateLessonContent(lessonContent: LessonContent): Result<Unit> {
        return try {
            val updatedContent = lessonContent.copy(updatedAt = System.currentTimeMillis())
            firestore.collection(COLLECTION_LESSONS)
                .document(lessonContent.id)
                .set(updatedContent)
                .await()
            
            Log.d(TAG, "Lesson content updated successfully: ${lessonContent.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lesson content", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLessonContent(lessonId: String): LessonContent? {
        return try {
            val doc = firestore.collection(COLLECTION_LESSONS)
                .document(lessonId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(LessonContent::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting lesson content", e)
            null
        }
    }
    
    fun getLessonsForCourse(courseId: String): Flow<List<LessonContent>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_LESSONS)
            .whereEqualTo("courseId", courseId)
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to lessons", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val lessons = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(LessonContent::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing lesson document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(lessons)
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Student Progress Management
    
    suspend fun startLesson(studentId: String, lessonId: String, courseId: String): Result<Unit> {
        return try {
            val progressId = "${studentId}_${lessonId}"
            val progress = StudentProgress(
                studentId = studentId,
                courseId = courseId,
                lastAccessedAt = System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_PROGRESS)
                .document(progressId)
                .set(progress)
                .await()
            
            Log.d(TAG, "Lesson started: $lessonId for student: $studentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting lesson", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateLessonProgress(
        studentId: String,
        lessonId: String,
        completionPercentage: Int,
        timeSpent: Long
    ): Result<Unit> {
        return try {
            val progressId = "${studentId}_${lessonId}"
            val updates = mapOf(
                "completionPercentage" to completionPercentage,
                "timeSpent" to FieldValue.increment(timeSpent),
                "lastAccessedAt" to System.currentTimeMillis(),
                "status" to if (completionPercentage >= 100) ProgressStatus.COMPLETED else ProgressStatus.IN_PROGRESS,
                "completedAt" to if (completionPercentage >= 100) System.currentTimeMillis() else 0L
            )
            
            firestore.collection(COLLECTION_PROGRESS)
                .document(progressId)
                .update(updates)
                .await()
            
            // Update overall course progress
            if (completionPercentage >= 100) {
                updateCourseProgress(studentId, lessonId)
            }
            
            Log.d(TAG, "Lesson progress updated: $lessonId for student: $studentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lesson progress", e)
            Result.failure(e)
        }
    }
    
    suspend fun getStudentProgress(studentId: String, lessonId: String): StudentProgress? {
        return try {
            val progressId = "${studentId}_${lessonId}"
            val doc = firestore.collection(COLLECTION_PROGRESS)
                .document(progressId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(StudentProgress::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student progress", e)
            null
        }
    }
    
    fun getStudentProgressForCourse(studentId: String, courseId: String): Flow<List<StudentProgress>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_PROGRESS)
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to student progress", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val progressList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(StudentProgress::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing progress document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(progressList)
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Quiz Management
    
    suspend fun submitQuizAttempt(attempt: QuizAttempt, quiz: Quiz): Result<QuizAttempt> {
        return try {
            // For now, just save the attempt without auto-grading
            // TODO: Implement grading logic or integrate with AIQuizService
            val finalAttempt = attempt.copy(
                id = if (attempt.id.isEmpty()) UUID.randomUUID().toString() else attempt.id,
                endTime = System.currentTimeMillis(),
                isCompleted = true
            )
            
            // Save to Firestore
            val docRef = firestore.collection(COLLECTION_QUIZ_ATTEMPTS).document(finalAttempt.id)
            docRef.set(finalAttempt).await()
            
            // Update student progress with quiz result
            updateStudentProgressWithQuiz(finalAttempt)
            
            Log.d(TAG, "Quiz attempt submitted: ${finalAttempt.id}")
            Result.success(finalAttempt)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting quiz attempt", e)
            Result.failure(e)
        }
    }
    
    suspend fun getQuizAttempts(studentId: String, quizId: String): List<QuizAttempt> {
        return try {
            val snapshot = firestore.collection(COLLECTION_QUIZ_ATTEMPTS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("quizId", quizId)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(QuizAttempt::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing quiz attempt: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quiz attempts", e)
            emptyList()
        }
    }
    
    // MARK: - Course Progress Analytics
    
    suspend fun getCourseProgress(studentId: String, courseId: String): CourseProgress? {
        return try {
            val progressId = "${studentId}_${courseId}"
            val doc = firestore.collection(COLLECTION_COURSE_PROGRESS)
                .document(progressId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(CourseProgress::class.java)
            } else {
                // Calculate and create new course progress
                calculateCourseProgress(studentId, courseId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting course progress", e)
            null
        }
    }
    
    fun getTeacherProgressOverview(teacherId: String, courseId: String): Flow<List<CourseProgress>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_COURSE_PROGRESS)
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to course progress", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val progressList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(CourseProgress::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing course progress: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(progressList)
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Private Helper Methods
    
    private suspend fun updateStudentProgressWithQuiz(attempt: QuizAttempt) {
        try {
            // Find the lesson that contains this quiz
            val lessons = firestore.collection(COLLECTION_LESSONS)
                .whereEqualTo("quiz.id", attempt.quizId)
                .get()
                .await()
            
            if (lessons.documents.isNotEmpty()) {
                val lesson = lessons.documents.first()
                val lessonId = lesson.id
                val progressId = "${attempt.studentId}_${lessonId}"
                
                // Update progress with quiz attempt
                firestore.collection(COLLECTION_PROGRESS)
                    .document(progressId)
                    .update(
                        "quizAttempts", FieldValue.arrayUnion(attempt),
                        "lastAccessedAt", System.currentTimeMillis()
                    )
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating student progress with quiz", e)
        }
    }
    
    private suspend fun updateCourseProgress(studentId: String, completedLessonId: String) {
        try {
            // Get lesson to find course ID
            val lessonDoc = firestore.collection(COLLECTION_LESSONS)
                .document(completedLessonId)
                .get()
                .await()
            
            if (lessonDoc.exists()) {
                val courseId = lessonDoc.getString("courseId") ?: return
                calculateAndUpdateCourseProgress(studentId, courseId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating course progress", e)
        }
    }
    
    private suspend fun calculateCourseProgress(studentId: String, courseId: String): CourseProgress? {
        return try {
            // Get all lessons for the course
            val lessonsSnapshot = firestore.collection(COLLECTION_LESSONS)
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            
            val totalLessons = lessonsSnapshot.documents.size
            if (totalLessons == 0) return null
            
            // Get student progress for all lessons in the course
            val progressSnapshot = firestore.collection(COLLECTION_PROGRESS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            
            val completedLessons = progressSnapshot.documents.count { doc ->
                val status = doc.getString("status")
                status == ProgressStatus.COMPLETED.name
            }
            
            val totalTimeSpent = progressSnapshot.documents.sumOf { doc ->
                doc.getLong("timeSpent") ?: 0L
            }
            
            // Calculate quiz statistics
            val quizAttempts = mutableListOf<QuizAttempt>()
            progressSnapshot.documents.forEach { doc ->
                val attempts = doc.get("quizAttempts") as? List<Map<String, Any>>
                attempts?.forEach { attemptMap ->
                    // Convert map to QuizAttempt object
                    // This is a simplified version - you might need more robust conversion
                }
            }
            
            val overallProgress = if (totalLessons > 0) {
                (completedLessons * 100) / totalLessons
            } else 0
            
            val courseProgress = CourseProgress(
                studentId = studentId,
                courseId = courseId,
                overallProgress = overallProgress,
                lessonsCompleted = completedLessons,
                totalLessons = totalLessons,
                totalTimeSpent = totalTimeSpent,
                lastAccessedAt = System.currentTimeMillis()
            )
            
            // Save the calculated progress
            val progressId = "${studentId}_${courseId}"
            firestore.collection(COLLECTION_COURSE_PROGRESS)
                .document(progressId)
                .set(courseProgress)
                .await()
            
            courseProgress
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating course progress", e)
            null
        }
    }
    
    private suspend fun calculateAndUpdateCourseProgress(studentId: String, courseId: String) {
        calculateCourseProgress(studentId, courseId)
    }
    
    // MARK: - Utility Methods
    
    suspend fun deleteLessonContent(lessonId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_LESSONS)
                .document(lessonId)
                .delete()
                .await()
            
            Log.d(TAG, "Lesson content deleted: $lessonId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting lesson content", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchLessons(courseId: String, query: String): List<LessonContent> {
        return try {
            val snapshot = firestore.collection(COLLECTION_LESSONS)
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            
            val allLessons = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(LessonContent::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            
            // Filter lessons based on search query
            allLessons.filter { lesson ->
                lesson.title.contains(query, ignoreCase = true) ||
                lesson.description.contains(query, ignoreCase = true) ||
                lesson.content.contains(query, ignoreCase = true) ||
                lesson.tags.any { it.contains(query, ignoreCase = true) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching lessons", e)
            emptyList()
        }
    }
}
