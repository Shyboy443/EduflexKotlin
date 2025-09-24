package com.example.ed.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.random.Random

/**
 * Firebase Firestore Data Seeder
 * Generates and inserts dynamic sample data for the education app
 * 
 * Database Structure:
 * - users/: User profiles (students, teachers, admins)
 * - courses/: Course information and content
 * - enrollments/: Student course enrollments
 * - analytics/: Student performance analytics
 * - assignments/: Course assignments and submissions
 * - materials/: Course materials and resources
 */
object FirebaseDataSeeder {
    
    private const val TAG = "FirebaseDataSeeder"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Sample data generators
    private val courseCategories = listOf(
        "Mathematics", "Science", "English", "History", "Geography", 
        "Computer Science", "Physics", "Chemistry", "Biology", "Art"
    )
    
    private val courseTitles = mapOf(
        "Mathematics" to listOf("Algebra Fundamentals", "Calculus I", "Statistics", "Geometry", "Trigonometry"),
        "Science" to listOf("General Science", "Environmental Science", "Earth Science", "Space Science"),
        "English" to listOf("English Literature", "Creative Writing", "Grammar Essentials", "Public Speaking"),
        "History" to listOf("World History", "Ancient Civilizations", "Modern History", "Sri Lankan History"),
        "Geography" to listOf("Physical Geography", "Human Geography", "World Geography", "Climate Studies"),
        "Computer Science" to listOf("Programming Basics", "Web Development", "Data Structures", "Mobile Apps"),
        "Physics" to listOf("Classical Mechanics", "Thermodynamics", "Electromagnetism", "Quantum Physics"),
        "Chemistry" to listOf("Organic Chemistry", "Inorganic Chemistry", "Physical Chemistry", "Biochemistry"),
        "Biology" to listOf("Cell Biology", "Genetics", "Ecology", "Human Anatomy", "Microbiology"),
        "Art" to listOf("Drawing Fundamentals", "Digital Art", "Art History", "Sculpture", "Photography")
    )
    
    private val teacherNames = listOf(
        "Dr. Sarah Johnson", "Prof. Michael Chen", "Ms. Emily Rodriguez", "Dr. David Kumar",
        "Prof. Lisa Thompson", "Mr. James Wilson", "Dr. Maria Garcia", "Prof. Robert Lee",
        "Ms. Jennifer Brown", "Dr. Ahmed Hassan", "Prof. Anna Kowalski", "Mr. Daniel Kim"
    )
    
    private val studentNames = listOf(
        "Alex Thompson", "Priya Patel", "Marcus Johnson", "Sophia Chen", "Ethan Williams",
        "Isabella Garcia", "Noah Brown", "Ava Davis", "Liam Miller", "Emma Wilson",
        "Oliver Martinez", "Charlotte Anderson", "William Taylor", "Amelia Thomas", "James Jackson",
        "Harper White", "Benjamin Harris", "Evelyn Martin", "Lucas Thompson", "Abigail Garcia"
    )
    
    /**
     * Seeds the database with comprehensive sample data
     */
    suspend fun seedDatabase(context: Context): SeedResult {
        return try {
            Log.d(TAG, "Starting database seeding process...")
            
            // Check authentication first
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated - cannot seed database")
                return SeedResult(
                    success = false,
                    totalRecords = 0,
                    details = listOf("❌ User not authenticated. Please sign in first."),
                    timestamp = System.currentTimeMillis()
                )
            }
            
            Log.d(TAG, "User authenticated: ${currentUser.email}")
            Log.d(TAG, "Firebase project: ${firestore.app.options.projectId}")
            
            // Test Firestore connectivity first
            try {
                Log.d(TAG, "Testing Firestore connectivity...")
                val testDoc = firestore.collection("connection_test").document("test")
                testDoc.set(mapOf("test" to true, "timestamp" to System.currentTimeMillis())).await()
                Log.d(TAG, "✅ Firestore write test successful")
                
                // Clean up test document
                testDoc.delete().await()
                Log.d(TAG, "✅ Firestore delete test successful")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firestore connectivity test failed", e)
                return SeedResult(
                    success = false,
                    totalRecords = 0,
                    details = listOf("❌ Firestore connectivity failed: ${e.message}"),
                    timestamp = System.currentTimeMillis()
                )
            }
            
            val results = mutableListOf<String>()
            var totalRecords = 0
            
            Log.d(TAG, "Starting data creation process...")
            
            // 1. Create sample users (teachers and students)
            Log.d(TAG, "Creating sample users...")
            val userResult = createSampleUsers()
            results.addAll(userResult.details)
            totalRecords += userResult.count
            Log.d(TAG, "Users created: ${userResult.count}")
            
            // 2. Create sample courses
            Log.d(TAG, "Creating sample courses...")
            val courseResult = createSampleCourses()
            results.addAll(courseResult.details)
            totalRecords += courseResult.count
            Log.d(TAG, "Courses created: ${courseResult.count}")
            
            // 3. Create sample enrollments
            Log.d(TAG, "Creating sample enrollments...")
            val enrollmentResult = createSampleEnrollments()
            results.addAll(enrollmentResult.details)
            totalRecords += enrollmentResult.count
            Log.d(TAG, "Enrollments created: ${enrollmentResult.count}")
            
            // 4. Create sample assignments
            Log.d(TAG, "Creating sample assignments...")
            val assignmentResult = createSampleAssignments()
            results.addAll(assignmentResult.details)
            totalRecords += assignmentResult.count
            Log.d(TAG, "Assignments created: ${assignmentResult.count}")
            
            // 5. Create sample analytics
            Log.d(TAG, "Creating sample analytics...")
            val analyticsResult = createSampleAnalytics()
            results.addAll(analyticsResult.details)
            totalRecords += analyticsResult.count
            Log.d(TAG, "Analytics created: ${analyticsResult.count}")
            
            // 6. Create sample materials
            Log.d(TAG, "Creating sample materials...")
            val materialResult = createSampleMaterials()
            results.addAll(materialResult.details)
            totalRecords += materialResult.count
            Log.d(TAG, "Materials created: ${materialResult.count}")
            
            Log.d(TAG, "Database seeding completed successfully. Total records: $totalRecords")
            
            SeedResult(
                success = true,
                totalRecords = totalRecords,
                details = results,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Database seeding failed", e)
            SeedResult(
                success = false,
                totalRecords = 0,
                details = listOf("❌ Seeding failed: ${e.message}"),
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Creates sample users (teachers and students)
     */
    private suspend fun createSampleUsers(): SeedOperationResult {
        val batch = firestore.batch()
        val details = mutableListOf<String>()
        var count = 0
        
        // Create teachers
        teacherNames.forEachIndexed { index, name ->
            val teacherId = "teacher_${System.currentTimeMillis()}_$index"
            val email = name.lowercase().replace(" ", ".").replace("dr.", "").replace("prof.", "").replace("ms.", "").replace("mr.", "") + "@school.edu"
            
            val teacher = mapOf(
                "id" to teacherId,
                "fullName" to name,
                "email" to email,
                "role" to "Teacher",
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "lastLoginAt" to (System.currentTimeMillis() - Random.nextLong(0, 7 * 24 * 60 * 60 * 1000)),
                "profilePicture" to "",
                "bio" to "Experienced educator passionate about teaching and student success.",
                "specialization" to courseCategories.random(),
                "yearsOfExperience" to Random.nextInt(1, 20)
            )
            
            batch.set(firestore.collection("users").document(teacherId), teacher)
            count++
        }
        
        // Create students
        studentNames.forEachIndexed { index, name ->
            val studentId = "student_${System.currentTimeMillis()}_$index"
            val email = name.lowercase().replace(" ", ".") + "@student.edu"
            
            val student = mapOf(
                "id" to studentId,
                "fullName" to name,
                "email" to email,
                "role" to "Student",
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "lastLoginAt" to (System.currentTimeMillis() - Random.nextLong(0, 3 * 24 * 60 * 60 * 1000)),
                "profilePicture" to "",
                "grade" to Random.nextInt(9, 13), // Grades 9-12
                "dateOfBirth" to (System.currentTimeMillis() - Random.nextLong(15 * 365 * 24 * 60 * 60 * 1000L, 18 * 365 * 24 * 60 * 60 * 1000L)),
                "parentEmail" to "parent.${name.lowercase().replace(" ", ".")}@email.com"
            )
            
            batch.set(firestore.collection("users").document(studentId), student)
            count++
        }
        
        batch.commit().await()
        details.add("✅ Created $count users (${teacherNames.size} teachers, ${studentNames.size} students)")
        
        return SeedOperationResult(count, details)
    }
    
    /**
     * Creates sample courses
     */
    private suspend fun createSampleCourses(): SeedOperationResult {
        val batch = firestore.batch()
        val details = mutableListOf<String>()
        var count = 0
        
        courseTitles.forEach { (category, titles) ->
            titles.forEach { title ->
                val courseId = "course_${System.currentTimeMillis()}_${count}"
                val teacherId = "teacher_${System.currentTimeMillis()}_${Random.nextInt(0, teacherNames.size)}"
                
                val course = mapOf(
                    "id" to courseId,
                    "title" to title,
                    "category" to category,
                    "description" to "Comprehensive $title course covering fundamental concepts and practical applications.",
                    "teacherId" to teacherId,
                    "teacherName" to teacherNames.random(),
                    "duration" to "${Random.nextInt(4, 16)} weeks",
                    "difficulty" to listOf("Beginner", "Intermediate", "Advanced").random(),
                    "price" to Random.nextDouble(29.99, 199.99),
                    "rating" to Random.nextDouble(3.5, 5.0),
                    "enrolledStudents" to Random.nextInt(10, 150),
                    "maxStudents" to Random.nextInt(50, 200),
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "startDate" to (System.currentTimeMillis() + Random.nextLong(0, 30 * 24 * 60 * 60 * 1000)),
                    "endDate" to (System.currentTimeMillis() + Random.nextLong(60 * 24 * 60 * 60 * 1000L, 120 * 24 * 60 * 60 * 1000L)),
                    "prerequisites" to if (Random.nextBoolean()) listOf("Basic ${category.lowercase()} knowledge") else emptyList<String>(),
                    "learningObjectives" to listOf(
                        "Understand core concepts of $title",
                        "Apply theoretical knowledge to practical problems",
                        "Develop critical thinking skills in $category"
                    ),
                    "thumbnailUrl" to "",
                    "videoUrl" to "",
                    "syllabus" to mapOf(
                        "week1" to "Introduction and Fundamentals",
                        "week2" to "Core Concepts",
                        "week3" to "Practical Applications",
                        "week4" to "Advanced Topics"
                    )
                )
                
                batch.set(firestore.collection("courses").document(courseId), course)
                count++
            }
        }
        
        batch.commit().await()
        details.add("✅ Created $count courses across ${courseCategories.size} categories")
        
        return SeedOperationResult(count, details)
    }
    
    /**
     * Creates sample enrollments
     */
    private suspend fun createSampleEnrollments(): SeedOperationResult {
        val batch = firestore.batch()
        val details = mutableListOf<String>()
        var count = 0
        
        // Get all courses and students for enrollment creation
        val courses = firestore.collection("courses").get().await()
        val students = firestore.collection("users").whereEqualTo("role", "Student").get().await()
        
        students.documents.forEach { studentDoc ->
            val studentId = studentDoc.id
            val enrollmentCount = Random.nextInt(2, 6) // Each student enrolls in 2-5 courses
            val selectedCourses = courses.documents.shuffled().take(enrollmentCount)
            
            selectedCourses.forEach { courseDoc ->
                val courseId = courseDoc.id
                val enrollmentId = "enrollment_${studentId}_${courseId}"
                
                val enrollment = mapOf(
                    "id" to enrollmentId,
                    "studentId" to studentId,
                    "courseId" to courseId,
                    "enrolledAt" to (System.currentTimeMillis() - Random.nextLong(0, 60 * 24 * 60 * 60 * 1000)),
                    "status" to listOf("Active", "Completed", "Paused").random(),
                    "progress" to Random.nextDouble(0.0, 100.0),
                    "lastAccessedAt" to (System.currentTimeMillis() - Random.nextLong(0, 7 * 24 * 60 * 60 * 1000)),
                    "completedLessons" to Random.nextInt(0, 20),
                    "totalLessons" to Random.nextInt(15, 25),
                    "grade" to if (Random.nextBoolean()) Random.nextDouble(60.0, 100.0) else null,
                    "certificateIssued" to Random.nextBoolean()
                )
                
                batch.set(firestore.collection("enrollments").document(enrollmentId), enrollment)
                count++
            }
        }
        
        batch.commit().await()
        details.add("✅ Created $count enrollments")
        
        return SeedOperationResult(count, details)
    }
    
    /**
     * Creates sample assignments
     */
    private suspend fun createSampleAssignments(): SeedOperationResult {
        val batch = firestore.batch()
        val details = mutableListOf<String>()
        var count = 0
        
        val courses = firestore.collection("courses").get().await()
        
        courses.documents.forEach { courseDoc ->
            val courseId = courseDoc.id
            val courseTitle = courseDoc.getString("title") ?: "Course"
            val assignmentCount = Random.nextInt(3, 8) // 3-7 assignments per course
            
            repeat(assignmentCount) { index ->
                val assignmentId = "assignment_${courseId}_${index}"
                
                val assignment = mapOf(
                    "id" to assignmentId,
                    "courseId" to courseId,
                    "title" to "$courseTitle Assignment ${index + 1}",
                    "description" to "Complete the assigned tasks and submit your work by the due date.",
                    "type" to listOf("Quiz", "Essay", "Project", "Lab Report", "Presentation").random(),
                    "maxPoints" to Random.nextInt(50, 200),
                    "dueDate" to (System.currentTimeMillis() + Random.nextLong(1 * 24 * 60 * 60 * 1000, 30 * 24 * 60 * 60 * 1000)),
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "instructions" to "Follow the guidelines provided in class and submit your best work.",
                    "attachments" to emptyList<String>(),
                    "submissionFormat" to listOf("PDF", "Word Document", "Online Form", "Video").random()
                )
                
                batch.set(firestore.collection("assignments").document(assignmentId), assignment)
                count++
            }
        }
        
        batch.commit().await()
        details.add("✅ Created $count assignments")
        
        return SeedOperationResult(count, details)
    }
    
    /**
     * Creates sample analytics data
     */
    private suspend fun createSampleAnalytics(): SeedOperationResult {
        val batch = firestore.batch()
        val details = mutableListOf<String>()
        var count = 0
        
        val students = firestore.collection("users").whereEqualTo("role", "Student").get().await()
        
        students.documents.forEach { studentDoc ->
            val studentId = studentDoc.id
            val analyticsId = "analytics_$studentId"
            
            val analytics = mapOf(
                "id" to analyticsId,
                "studentId" to studentId,
                "totalCoursesEnrolled" to Random.nextInt(2, 10),
                "completedCourses" to Random.nextInt(0, 5),
                "averageGrade" to Random.nextDouble(65.0, 95.0),
                "studyStreak" to Random.nextInt(0, 30),
                "totalStudyHours" to Random.nextDouble(10.0, 200.0),
                "pendingAssignments" to Random.nextInt(0, 8),
                "completedAssignments" to Random.nextInt(5, 25),
                "lastUpdated" to System.currentTimeMillis(),
                "monthlyProgress" to mapOf(
                    "january" to Random.nextDouble(0.0, 100.0),
                    "february" to Random.nextDouble(0.0, 100.0),
                    "march" to Random.nextDouble(0.0, 100.0),
                    "april" to Random.nextDouble(0.0, 100.0),
                    "may" to Random.nextDouble(0.0, 100.0),
                    "june" to Random.nextDouble(0.0, 100.0)
                ),
                "subjectPerformance" to courseCategories.associate { category ->
                    category.lowercase() to Random.nextDouble(60.0, 100.0)
                },
                "achievements" to listOf(
                    "First Assignment Completed",
                    "Perfect Attendance",
                    "High Performer",
                    "Quick Learner"
                ).shuffled().take(Random.nextInt(1, 4))
            )
            
            batch.set(firestore.collection("analytics").document(analyticsId), analytics)
            count++
        }
        
        batch.commit().await()
        details.add("✅ Created $count analytics records")
        
        return SeedOperationResult(count, details)
    }
    
    /**
     * Creates sample course materials
     */
    private suspend fun createSampleMaterials(): SeedOperationResult {
        val batch = firestore.batch()
        val details = mutableListOf<String>()
        var count = 0
        
        val courses = firestore.collection("courses").get().await()
        
        courses.documents.forEach { courseDoc ->
            val courseId = courseDoc.id
            val courseTitle = courseDoc.getString("title") ?: "Course"
            val materialCount = Random.nextInt(5, 12) // 5-11 materials per course
            
            repeat(materialCount) { index ->
                val materialId = "material_${courseId}_${index}"
                val materialTypes = listOf("PDF", "Video", "Audio", "Presentation", "Document")
                val materialType = materialTypes.random()
                
                val material = mapOf(
                    "id" to materialId,
                    "courseId" to courseId,
                    "title" to "$courseTitle - Lesson ${index + 1}",
                    "description" to "Educational material for $courseTitle covering important concepts.",
                    "type" to materialType,
                    "fileUrl" to "",
                    "fileName" to "lesson_${index + 1}.${materialType.lowercase()}",
                    "fileSize" to Random.nextLong(1024 * 1024, 50 * 1024 * 1024), // 1MB to 50MB
                    "uploadedAt" to System.currentTimeMillis(),
                    "isPublic" to true,
                    "downloadCount" to Random.nextInt(0, 100),
                    "tags" to listOf(courseDoc.getString("category") ?: "General", "Lesson", "Study Material"),
                    "order" to index + 1,
                    "duration" to if (materialType == "Video" || materialType == "Audio") Random.nextInt(300, 3600) else null // 5-60 minutes
                )
                
                batch.set(firestore.collection("materials").document(materialId), material)
                count++
            }
        }
        
        batch.commit().await()
        details.add("✅ Created $count course materials")
        
        return SeedOperationResult(count, details)
    }
    
    /**
     * Clears all sample data from the database
     */
    suspend fun clearSampleData(): SeedResult {
        return try {
            val collections = listOf("users", "courses", "enrollments", "assignments", "analytics", "materials")
            val results = mutableListOf<String>()
            var totalDeleted = 0
            
            collections.forEach { collectionName ->
                val documents = firestore.collection(collectionName).get().await()
                var batch = firestore.batch()
                var batchCount = 0
                
                documents.documents.forEach { doc ->
                    // Only delete documents that appear to be sample data
                    val docId = doc.id
                    if (docId.contains("teacher_") || docId.contains("student_") || 
                        docId.contains("course_") || docId.contains("enrollment_") ||
                        docId.contains("assignment_") || docId.contains("analytics_") ||
                        docId.contains("material_")) {
                        
                        batch.delete(doc.reference)
                        batchCount++
                        totalDeleted++
                        
                        // Firestore batch limit is 500 operations
                        if (batchCount >= 450) {
                            batch.commit().await()
                            batch = firestore.batch()
                            batchCount = 0
                        }
                    }
                }
                
                if (batchCount > 0) {
                    batch.commit().await()
                }
                
                results.add("✅ Cleared $batchCount documents from $collectionName")
            }
            
            SeedResult(
                success = true,
                totalRecords = totalDeleted,
                details = results,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear sample data", e)
            SeedResult(
                success = false,
                totalRecords = 0,
                details = listOf("❌ Clear operation failed: ${e.message}"),
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    // Data classes for results
    data class SeedResult(
        val success: Boolean,
        val totalRecords: Int,
        val details: List<String>,
        val timestamp: Long
    )
    
    private data class SeedOperationResult(
        val count: Int,
        val details: List<String>
    )
}