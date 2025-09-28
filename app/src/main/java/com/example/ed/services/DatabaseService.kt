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
        Log.d(TAG, "Starting getCoursesRealTime() - Setting up Firestore listener")
        
        val listener = firestore.collection("courses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to courses", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "Firestore snapshot received - Document count: ${snapshot?.documents?.size ?: 0}")
                
                val courses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "Processing course document: ${doc.id}")
                        Log.d(TAG, "Document data: ${doc.data}")
                        
                        val course = EnhancedCourse(
                            id = doc.id,
                            title = try {
                                doc.getString("title") ?: "Untitled Course"
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting title for course ${doc.id}: ${e.message}")
                                "Untitled Course"
                            },
                            subtitle = try {
                                doc.getString("subtitle") ?: ""
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting subtitle for course ${doc.id}: ${e.message}")
                                ""
                            },
                            description = try {
                                doc.getString("description") ?: "No description available"
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting description for course ${doc.id}: ${e.message}")
                                "No description available"
                            },
                            longDescription = try {
                                doc.getString("longDescription") ?: doc.getString("description") ?: "No description available"
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting longDescription for course ${doc.id}: ${e.message}")
                                "No description available"
                            },
                            instructor = TeacherProfile(
                                id = try {
                                    doc.getString("teacherId") ?: ""
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error getting teacherId for course ${doc.id}: ${e.message}")
                                    ""
                                },
                                name = try {
                                    doc.getString("instructor") ?: doc.getString("teacherName") ?: doc.getString("instructor") ?: "Unknown Instructor"
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error getting instructor name for course ${doc.id}: ${e.message}")
                                    doc.getString("teacherName") ?: "Unknown Instructor"
                                }
                            ),
                            category = CourseCategory(
                                name = try {
                                    doc.getString("category") ?: "General"
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error getting category for course ${doc.id}: ${e.message}")
                                    "General"
                                }
                            ),
                            difficulty = try {
                                CourseDifficulty.valueOf(doc.getString("difficulty")?.uppercase() ?: "BEGINNER")
                            } catch (e: Exception) {
                                CourseDifficulty.BEGINNER
                            },
                            language = try {
                                doc.getString("language") ?: "en"
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting language for course ${doc.id}: ${e.message}")
                                "en"
                            },
                            thumbnailUrl = try {
                                doc.getString("thumbnailUrl") ?: ""
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting thumbnailUrl for course ${doc.id}: ${e.message}")
                                ""
                            },
                            previewVideoUrl = try {
                                doc.getString("previewVideoUrl") ?: ""
                            } catch (e: Exception) {
                                Log.w(TAG, "Error getting previewVideoUrl for course ${doc.id}: ${e.message}")
                                ""
                            },
                            tags = doc.get("tags") as? List<String> ?: emptyList(),
                            learningObjectives = doc.get("learningObjectives") as? List<String> ?: emptyList(),
                            prerequisites = doc.get("prerequisites") as? List<String> ?: emptyList(),
                            pricing = CoursePricing(
                                isFree = doc.getBoolean("isFree")
                                    ?: ((doc.getDouble("price") ?: 0.0) <= 0.0),
                                price = doc.getDouble("price") ?: 0.0
                            ),
                            settings = CourseSettings(
                                isPublished = doc.getBoolean("isPublished") ?: true, // Default to published for sample data
                                allowEnrollment = doc.getBoolean("allowEnrollment") ?: true
                            ),
                            status = try {
                                CourseStatus.valueOf(doc.getString("status")?.uppercase() ?: "PUBLISHED")
                            } catch (e: Exception) {
                                CourseStatus.PUBLISHED // Default to published for sample data
                            },
                            createdAt = try {
                                doc.getTimestamp("createdAt")?.toDate()?.time ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                doc.getLong("createdAt") ?: System.currentTimeMillis()
                            },
                            updatedAt = try {
                                doc.getTimestamp("updatedAt")?.toDate()?.time ?: doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            }
                        )
                        
                        Log.d(TAG, "Successfully parsed course: ${course.title}")
                        course
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse course: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                // Sort courses by createdAt in descending order (newest first)
                val sortedCourses = courses.sortedByDescending { it.createdAt }
                
                Log.d(TAG, "Parsed ${sortedCourses.size} courses successfully")
                trySend(sortedCourses)
                Log.d(TAG, "Fetched ${courses.size} courses")
            }
        
        awaitClose { 
            Log.d(TAG, "Closing getCoursesRealTime() listener")
            listener.remove() 
        }
    }
    
    fun getCoursesByInstructorRealTime(instructorId: String): Flow<List<Course>> = callbackFlow {
        val listener = firestore.collection("courses")
            .whereEqualTo("teacherId", instructorId)
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
                                createdAt = try {
                                    val timestamp = doc.getTimestamp("createdAt")
                                    timestamp?.toDate()?.time ?: doc.getLong("createdAt") ?: 0L
                                } catch (e: Exception) {
                                    doc.getLong("createdAt") ?: 0L
                                },
                                updatedAt = try {
                                    val timestamp = doc.getTimestamp("updatedAt")
                                    timestamp?.toDate()?.time ?: doc.getLong("updatedAt") ?: 0L
                                } catch (e: Exception) {
                                    doc.getLong("updatedAt") ?: 0L
                                },
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
                
                // Sort courses by createdAt in descending order (newest first)
                val sortedCourses = courses.sortedByDescending { it.createdAt }
                
                trySend(sortedCourses)
            }
        
        awaitClose { listener.remove() }
    }

    // MARK: - Teacher Enrollments
    suspend fun getTeacherEnrollments(teacherId: String): List<StudentEnrollment> = withContext(Dispatchers.IO) {
        try {
            // Get teacher's courses
            val coursesSnapshot = firestore.collection("courses")
                .whereEqualTo("teacherId", teacherId)
                .get().await()

            val courseIdToName = coursesSnapshot.documents.associate { doc ->
                doc.id to (doc.getString("title") ?: "Untitled Course")
            }

            if (courseIdToName.isEmpty()) return@withContext emptyList()

            val courseIds = courseIdToName.keys.toList()
            val chunks = courseIds.chunked(10)
            val enrollmentsRaw = mutableListOf<Map<String, Any?>>()

            // Fetch enrollments per chunk
            for (chunk in chunks) {
                val snapshot = firestore.collection("enrollments")
                    .whereIn("courseId", chunk)
                    .get().await()
                enrollmentsRaw.addAll(snapshot.documents.map { it.data ?: emptyMap() })
            }

            if (enrollmentsRaw.isEmpty()) return@withContext emptyList()

            // Collect unique student IDs
            val studentIds = enrollmentsRaw.mapNotNull { it["studentId"] as? String }.distinct()

            // Fetch student profiles in chunks
            val studentInfo = mutableMapOf<String, Pair<String, String>>() // id -> (name,email)
            val studentChunks = studentIds.chunked(10)
            for (chunk in studentChunks) {
                val snapshot = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()
                snapshot.documents.forEach { doc ->
                    val name = doc.getString("fullName")
                        ?: doc.getString("displayName")
                        ?: doc.getString("name")
                        ?: "Student"
                    val email = doc.getString("email") ?: ""
                    studentInfo[doc.id] = name to email
                }
            }

            // Build StudentEnrollment list
            val result = enrollmentsRaw.mapNotNull { e ->
                try {
                    val studentId = e["studentId"] as? String ?: return@mapNotNull null
                    val courseId = e["courseId"] as? String ?: return@mapNotNull null
                    val enrolledAt = (e["enrolledAt"] as? Number)?.toLong() ?: 0L
                    val progress = (e["progress"] as? Number)?.toInt() ?: 0
                    val isActive = (e["isActive"] as? Boolean) ?: true
                    val (name, email) = studentInfo[studentId] ?: ("Student" to "")
                    StudentEnrollment(
                        id = UUID.randomUUID().toString(),
                        studentId = studentId,
                        studentName = name,
                        studentEmail = email,
                        courseId = courseId,
                        courseName = courseIdToName[courseId] ?: "",
                        enrolledAt = enrolledAt,
                        progress = progress,
                        isActive = isActive
                    )
                } catch (_: Exception) { null }
            }

            // Sort by enrolledAt desc
            result.sortedByDescending { it.enrolledAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching teacher enrollments", e)
            emptyList()
        }
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
                    createdAt = try {
                        val timestamp = document.getTimestamp("createdAt")
                        timestamp?.toDate()?.time ?: document.getLong("createdAt") ?: 0L
                    } catch (e: Exception) {
                        document.getLong("createdAt") ?: 0L
                    },
                    updatedAt = try {
                        val timestamp = document.getTimestamp("updatedAt")
                        timestamp?.toDate()?.time ?: document.getLong("updatedAt") ?: 0L
                    } catch (e: Exception) {
                        document.getLong("updatedAt") ?: 0L
                    },
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
                    createdAt = try {
                        val timestamp = document.getTimestamp("createdAt")
                        timestamp?.toDate()?.time ?: document.getLong("createdAt") ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        document.getLong("createdAt") ?: System.currentTimeMillis()
                    },
                    updatedAt = try {
                        val timestamp = document.getTimestamp("updatedAt")
                        timestamp?.toDate()?.time ?: document.getLong("updatedAt") ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        document.getLong("updatedAt") ?: System.currentTimeMillis()
                    }
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
                
                // Sort assignments by dueDate in ascending order (earliest first)
                val sortedAssignments = assignments.sortedBy { it.dueDate }
                
                trySend(sortedAssignments)
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
                
                // Sort announcements by createdAt in descending order (newest first)
                val sortedAnnouncements = announcements.sortedByDescending { it.createdAt }
                
                trySend(sortedAnnouncements)
            }
        
        awaitClose { listener.remove() }
    }
    
    // MARK: - Enrollment Operations
    
    fun getEnrolledCoursesRealTime(userId: String): Flow<List<Course>> = callbackFlow {
        Log.d(TAG, "Starting getEnrolledCoursesRealTime for user: $userId")
        
        val listener = firestore.collection("enrollments")
            .whereEqualTo("studentId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errorMsg = "Error listening to enrollments: ${error.message}"
                    Log.e(TAG, errorMsg, error)
                    try {
                        trySend(emptyList())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending empty list to flow", e)
                    }
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "No enrollments found for user: $userId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Get all course IDs from enrollments (including inactive ones for now)
                val enrollments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val status = doc.getString("status")?.lowercase()
                        val courseId = doc.getString("courseId")
                        if (!courseId.isNullOrBlank()) {
                            Triple(doc.id, courseId, status)
                        } else {
                            Log.w(TAG, "Enrollment document ${doc.id} has null or blank courseId")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing enrollment document: ${doc.id}", e)
                        null
                    }
                }
                
                if (enrollments.isEmpty()) {
                    Log.d(TAG, "No valid course IDs found in enrollments")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "Found ${enrollments.size} enrollments, fetching course details...")
                
                // Get all course IDs (unique) and filter out any nulls
                val courseIds = enrollments.map { it.second }.distinct().filterNotNull().filter { it.isNotBlank() }
                
                if (courseIds.isEmpty()) {
                    Log.d(TAG, "No valid course IDs after filtering")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "Valid course IDs: $courseIds")
                
                // Split into chunks to handle Firestore's limit of 10 items in whereIn
                val chunks = courseIds.chunked(10)
                val allCourses = mutableListOf<Course>()
                val completedChunks = mutableListOf<Boolean>()
                
                chunks.forEachIndexed { index, chunk ->
                    // Double-check chunk has no null or empty values
                    val validChunk = chunk.filter { !it.isNullOrBlank() }
                    if (validChunk.isEmpty()) {
                        Log.w(TAG, "Chunk $index is empty after filtering, skipping")
                        completedChunks.add(true)
                        return@forEachIndexed
                    }
                    
                    firestore.collection("courses")
                        .whereIn(FieldPath.documentId(), validChunk)
                        .get()
                        .addOnSuccessListener { courseSnapshot ->
                            Log.d(TAG, "Fetched ${courseSnapshot.size()} courses for chunk $index")
                            
                            val courses = courseSnapshot.documents.mapNotNull { doc ->
                                try {
                                    Course(
                                        id = doc.id,
                                        title = doc.getString("title") ?: "Untitled Course",
                                        instructor = doc.getString("instructor") ?: "Unknown Instructor",
                                        description = doc.getString("description") ?: "No description available",
                                        category = doc.getString("category") ?: "Uncategorized",
                                        difficulty = doc.getString("difficulty") ?: "Beginner",
                                        duration = doc.getString("duration") ?: "0h",
                                        thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                        isPublished = doc.getBoolean("isPublished") ?: false,
                                        createdAt = try {
                                            val timestamp = doc.getTimestamp("createdAt")
                                            timestamp?.toDate()?.time ?: doc.getLong("createdAt") ?: 0L
                                        } catch (e: Exception) {
                                            doc.getLong("createdAt") ?: 0L
                                        },
                                        updatedAt = try {
                                            val timestamp = doc.getTimestamp("updatedAt")
                                            timestamp?.toDate()?.time ?: doc.getLong("updatedAt") ?: 0L
                                        } catch (e: Exception) {
                                            doc.getLong("updatedAt") ?: 0L
                                        },
                                        enrolledStudents = doc.getLong("enrolledStudents")?.toInt() ?: 0,
                                        rating = doc.getDouble("rating")?.toFloat() ?: 0.0f,
                                        teacherId = doc.getString("teacherId") ?: "",
                                        price = doc.getDouble("price") ?: 0.0
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing course document: ${doc.id}", e)
                                    null
                                }
                            }
                            
                            allCourses.addAll(courses)
                            completedChunks.add(true)
                            
                            // If all chunks are processed, send the combined results
                            if (completedChunks.size == chunks.size) {
                                Log.d(TAG, "All chunks processed, sending ${allCourses.size} courses to flow")
                                trySend(allCourses)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching courses for chunk $index", e)
                            completedChunks.add(false)
                            
                            // If all chunks are processed (even with failures), send what we have
                            if (completedChunks.size == chunks.size && allCourses.isNotEmpty()) {
                                Log.w(TAG, "Some chunks failed, but sending ${allCourses.size} successfully loaded courses")
                                trySend(allCourses)
                            } else if (completedChunks.size == chunks.size) {
                                Log.e(TAG, "All chunks failed to load courses")
                                trySend(emptyList())
                            }
                        }
                }
            }
        
        awaitClose { 
            Log.d(TAG, "Removing enrollments listener for user: $userId")
            listener.remove() 
        }
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
                
                // Sort payments by timestamp in descending order (newest first)
                val sortedPayments = payments.sortedByDescending { it.timestamp }
                
                trySend(sortedPayments)
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