package com.example.ed.services

import android.content.Context
import android.util.Log
import com.example.ed.models.*
import com.example.ed.utils.DataValidator
import com.example.ed.utils.ErrorHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class DatabaseService private constructor(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cacheService = CacheService.getInstance(context)
    
    companion object {
        @Volatile
        private var INSTANCE: DatabaseService? = null
        private const val TAG = "DatabaseService"
        
        fun getInstance(context: Context): DatabaseService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // MARK: - Course Operations
    
    fun getCoursesRealTime(): Flow<List<EnhancedCourse>> = callbackFlow {
        val listener = firestore.collection("courses")
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to courses", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val courses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        EnhancedCourse(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            subtitle = doc.getString("subtitle") ?: "",
                            description = doc.getString("description") ?: "",
                            longDescription = doc.getString("longDescription") ?: "",
                            instructor = TeacherProfile(
                                id = doc.getString("teacherId") ?: "",
                                name = doc.getString("instructor") ?: ""
                            ),
                            category = CourseCategory(
                                name = doc.getString("category") ?: ""
                            ),
                            difficulty = try {
                                CourseDifficulty.valueOf(doc.getString("difficulty")?.uppercase() ?: "BEGINNER")
                            } catch (e: Exception) {
                                CourseDifficulty.BEGINNER
                            },
                            language = doc.getString("language") ?: "en",
                            thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                            previewVideoUrl = doc.getString("previewVideoUrl") ?: "",
                            tags = doc.get("tags") as? List<String> ?: emptyList(),
                            learningObjectives = doc.get("learningObjectives") as? List<String> ?: emptyList(),
                            prerequisites = doc.get("prerequisites") as? List<String> ?: emptyList(),
                            pricing = CoursePricing(
                                isFree = doc.getBoolean("isFree") ?: false,
                                price = doc.getDouble("price") ?: 0.0
                            ),
                            settings = CourseSettings(
                                isPublished = doc.getBoolean("isPublished") ?: false,
                                allowEnrollment = doc.getBoolean("allowEnrollment") ?: true
                            ),
                            status = try {
                                CourseStatus.valueOf(doc.getString("status")?.uppercase() ?: "DRAFT")
                            } catch (e: Exception) {
                                CourseStatus.DRAFT
                            },
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse course: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(courses)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getCoursesByInstructorRealTime(instructorId: String): Flow<List<Course>> = callbackFlow {
        val listener = firestore.collection("courses")
            .whereEqualTo("teacherId", instructorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to instructor courses", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val courses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                            Course(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                instructor = doc.getString("instructor") ?: "",
                                description = doc.getString("description") ?: "",
                                category = doc.getString("category") ?: "",
                                difficulty = doc.getString("difficulty") ?: "Beginner",
                                duration = doc.getString("duration") ?: "",
                                thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                isPublished = doc.getBoolean("isPublished") ?: false,
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                updatedAt = doc.getLong("updatedAt") ?: 0L,
                                enrolledStudents = doc.getLong("enrolledStudents")?.toInt() ?: 0,
                                rating = doc.getDouble("rating")?.toFloat() ?: 0.0f,
                                teacherId = doc.getString("teacherId") ?: "",
                                price = doc.getDouble("price") ?: 0.0
                            )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing instructor course document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(courses)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getCourseById(courseId: String): Course? {
        return try {
            val document = firestore.collection("courses").document(courseId).get().await()
            if (document.exists()) {
                Course(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    instructor = document.getString("instructor") ?: "",
                    description = document.getString("description") ?: "",
                    category = document.getString("category") ?: "",
                    difficulty = document.getString("difficulty") ?: "Beginner",
                    duration = document.getString("duration") ?: "",
                    thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                    isPublished = document.getBoolean("isPublished") ?: false,
                    createdAt = document.getLong("createdAt") ?: 0L,
                    updatedAt = document.getLong("updatedAt") ?: 0L,
                    enrolledStudents = document.getLong("enrolledStudents")?.toInt() ?: 0,
                    rating = document.getDouble("rating")?.toFloat() ?: 0.0f,
                    teacherId = document.getString("teacherId") ?: "",
                    price = document.getDouble("price") ?: 0.0
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting course by ID: $courseId", e)
            null
        }
    }
    
    suspend fun getCourseDetails(courseId: String): EnhancedCourse? {
        return try {
            // Check cache first
            val cachedCourse = cacheService.getCachedCourseDetails(courseId)
            if (cachedCourse != null) {
                return cachedCourse
            }

            val document = firestore.collection("courses").document(courseId).get().await()
            if (document.exists()) {
                val course = EnhancedCourse(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    subtitle = document.getString("subtitle") ?: "",
                    description = document.getString("description") ?: "",
                    longDescription = document.getString("longDescription") ?: "",
                    instructor = TeacherProfile(
                        id = document.getString("teacherId") ?: "",
                        name = document.getString("instructor") ?: ""
                    ),
                    category = CourseCategory(
                        name = document.getString("category") ?: ""
                    ),
                    difficulty = try {
                        CourseDifficulty.valueOf(document.getString("difficulty")?.uppercase() ?: "BEGINNER")
                    } catch (e: Exception) {
                        CourseDifficulty.BEGINNER
                    },
                    language = document.getString("language") ?: "en",
                    thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                    previewVideoUrl = document.getString("previewVideoUrl") ?: "",
                    tags = document.get("tags") as? List<String> ?: emptyList(),
                    learningObjectives = document.get("learningObjectives") as? List<String> ?: emptyList(),
                    prerequisites = document.get("prerequisites") as? List<String> ?: emptyList(),
                    pricing = CoursePricing(
                        isFree = document.getBoolean("isFree") ?: false,
                        price = document.getDouble("price") ?: 0.0
                    ),
                    settings = CourseSettings(
                        isPublished = document.getBoolean("isPublished") ?: false,
                        allowEnrollment = document.getBoolean("allowEnrollment") ?: true
                    ),
                    status = try {
                        CourseStatus.valueOf(document.getString("status")?.uppercase() ?: "DRAFT")
                    } catch (e: Exception) {
                        CourseStatus.DRAFT
                    },
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                
                // Cache the result
                cacheService.cacheCourseDetails(courseId, course)
                course
            } else {
                null
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(context, e)
            null
        }
    }
    
    // MARK: - Assignment Operations
    
    fun getAssignmentsRealTime(instructorId: String): Flow<List<Assignment>> = callbackFlow {
        val listener = firestore.collection("assignments")
            .whereEqualTo("instructorId", instructorId)
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to assignments", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val assignments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Assignment(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            dueDate = doc.getLong("dueDate") ?: 0L,
                            moduleId = doc.getString("moduleId") ?: "",
                            instructions = doc.getString("instructions") ?: "",
                            maxPoints = doc.getLong("maxPoints")?.toInt() ?: 100,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing assignment document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(assignments)
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Announcement Operations
    
    fun getAnnouncementsRealTime(courseIds: List<String>): Flow<List<Announcement>> = callbackFlow {
        if (courseIds.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = firestore.collection("announcements")
            .whereIn("courseId", courseIds)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to announcements", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val announcements = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Announcement(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            courseId = doc.getString("courseId") ?: "",
                            authorId = doc.getString("authorId") ?: "",
                            authorName = doc.getString("authorName") ?: "",
                            isImportant = doc.getBoolean("isImportant") ?: false,
                            attachments = doc.get("attachments") as? List<String> ?: emptyList(),
                            isPublished = doc.getBoolean("isPublished") ?: true,
                            targetAudience = doc.getString("targetAudience") ?: "all",
                            createdAt = doc.getDate("createdAt") ?: Date()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse announcement: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(announcements)
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Enrollment Operations
    
    fun getEnrolledCoursesRealTime(userId: String): Flow<List<Course>> = callbackFlow {
        val listener = firestore.collection("enrollments")
            .whereEqualTo("studentId", userId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to enrollments", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val courseIds = snapshot?.documents?.mapNotNull { 
                    it.getString("courseId") 
                } ?: emptyList()
                
                if (courseIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Get course details for enrolled courses
                firestore.collection("courses")
                    .whereIn(FieldPath.documentId(), courseIds)
                    .get()
                    .addOnSuccessListener { courseSnapshot ->
                        val courses = courseSnapshot.documents.mapNotNull { doc ->
                            try {
                                Course(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    instructor = doc.getString("instructor") ?: "",
                                    description = doc.getString("description") ?: "",
                                    category = doc.getString("category") ?: "",
                                    difficulty = doc.getString("difficulty") ?: "Beginner",
                                    duration = doc.getString("duration") ?: "",
                                    thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                    isPublished = doc.getBoolean("isPublished") ?: false,
                                    createdAt = doc.getLong("createdAt") ?: 0L,
                                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                                    enrolledStudents = doc.getLong("enrolledStudents")?.toInt() ?: 0,
                                    rating = doc.getDouble("rating")?.toFloat() ?: 0.0f,
                                    teacherId = doc.getString("teacherId") ?: "",
                                    price = doc.getDouble("price") ?: 0.0
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing enrolled course document: ${doc.id}", e)
                                null
                            }
                        }
                        trySend(courses)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting enrolled courses", e)
                        trySend(emptyList())
                    }
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Statistics Operations
    
    suspend fun getInstructorStats(instructorId: String): InstructorStats {
        return try {
            val coursesSnapshot = firestore.collection("courses")
                .whereEqualTo("instructorId", instructorId)
                .get().await()
            
            val enrollmentsSnapshot = firestore.collection("enrollments")
                .whereEqualTo("instructorId", instructorId)
                .get().await()
            
            val assignmentsSnapshot = firestore.collection("assignments")
                .whereEqualTo("instructorId", instructorId)
                .whereEqualTo("status", "pending")
                .get().await()
            
            val gradesSnapshot = firestore.collection("grades")
                .whereEqualTo("instructorId", instructorId)
                .get().await()
            
            val totalCourses = coursesSnapshot.size()
            val totalStudents = enrollmentsSnapshot.documents
                .mapNotNull { it.getString("studentId") }
                .distinct().size
            val pendingAssignments = assignmentsSnapshot.size()
            
            val grades = gradesSnapshot.documents.mapNotNull { 
                it.getDouble("score") 
            }
            val averageGrade = if (grades.isNotEmpty()) grades.average() else 0.0
            
            InstructorStats(
                totalCourses = totalCourses,
                totalStudents = totalStudents,
                pendingAssignments = pendingAssignments,
                averageGrade = averageGrade
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting instructor stats", e)
            InstructorStats(0, 0, 0, 0.0)
        }
    }
    
    suspend fun getStudentStats(studentId: String): StudentStats {
        return try {
            val enrollmentsSnapshot = firestore.collection("enrollments")
                .whereEqualTo("studentId", studentId)
                .get().await()
            
            val completedCoursesSnapshot = firestore.collection("enrollments")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "completed")
                .get().await()
            
            val gradesSnapshot = firestore.collection("grades")
                .whereEqualTo("studentId", studentId)
                .get().await()
            
            val assignmentsSnapshot = firestore.collection("assignments")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "pending")
                .get().await()
            
            val enrolledCourses = enrollmentsSnapshot.size()
            val completedCourses = completedCoursesSnapshot.size()
            val pendingAssignments = assignmentsSnapshot.size()
            
            val grades = gradesSnapshot.documents.mapNotNull { 
                it.getDouble("score") 
            }
            val averageGrade = if (grades.isNotEmpty()) grades.average() else 0.0
            
            StudentStats(
                enrolledCourses = enrolledCourses,
                completedCourses = completedCourses,
                averageGrade = averageGrade,
                pendingAssignments = pendingAssignments
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student stats", e)
            StudentStats(0, 0, 0.0, 0)
        }
    }
    
    // MARK: - Payment Operations
    
    fun getPaymentHistoryRealTime(userId: String): Flow<List<PaymentRecord>> = callbackFlow {
        val listener = firestore.collection("payments")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to payment history", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val payments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        PaymentRecord(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            courseId = doc.getString("courseId") ?: "",
                            courseName = doc.getString("courseName") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            currency = doc.getString("currency") ?: "USD",
                            status = doc.getString("status") ?: "pending",
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            transactionId = doc.getString("transactionId") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            receiptUrl = doc.getString("receiptUrl") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing payment record document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(payments)
            }
        
        awaitClose { listener.remove() }
    }
}

// Data classes for statistics
data class InstructorStats(
    val totalCourses: Int,
    val totalStudents: Int,
    val pendingAssignments: Int,
    val averageGrade: Double
)

data class StudentStats(
    val enrolledCourses: Int,
    val completedCourses: Int,
    val averageGrade: Double,
    val pendingAssignments: Int
)