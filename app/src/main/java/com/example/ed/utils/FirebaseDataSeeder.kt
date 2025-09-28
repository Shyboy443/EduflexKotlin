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
    
    // Course image URLs mapped by category
    private val courseImageUrls = mapOf(
        "Mathematics" to listOf(
            "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1596495578065-6e0763fa1178?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1518133910546-b6c2fb7d79e3?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1453733190371-0a9bedd82893?w=400&h=300&fit=crop"
        ),
        "Science" to listOf(
            "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1554475901-4538ddfbccc2?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1628595351029-c2bf17511435?w=400&h=300&fit=crop"
        ),
        "English" to listOf(
            "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=400&h=300&fit=crop"
        ),
        "History" to listOf(
            "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1520637836862-4d197d17c93a?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1604580864964-0462f5d5b1a8?w=400&h=300&fit=crop"
        ),
        "Geography" to listOf(
            "https://images.unsplash.com/photo-1446776653964-20c1d3a81b06?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1519452575417-564c1401ecc0?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1564053489984-317bbd824340?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1502780402662-acc01917949e?w=400&h=300&fit=crop"
        ),
        "Computer Science" to listOf(
            "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1517077304055-6e89abbf09b0?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1484417894907-623942c8ee29?w=400&h=300&fit=crop"
        ),
        "Physics" to listOf(
            "https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1582719471384-894fbb16e074?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1614935151651-0bea6508db6b?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=400&h=300&fit=crop"
        ),
        "Chemistry" to listOf(
            "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1554475901-4538ddfbccc2?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1628595351029-c2bf17511435?w=400&h=300&fit=crop"
        ),
        "Biology" to listOf(
            "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=400&h=300&fit=crop"
        ),
        "Art" to listOf(
            "https://images.unsplash.com/photo-1541961017774-22349e4a1262?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?w=400&h=300&fit=crop"
        )
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
            titles.forEachIndexed { index, title ->
                val courseId = "course_${System.currentTimeMillis()}_${count}"
                val teacherId = "teacher_${System.currentTimeMillis()}_${Random.nextInt(0, teacherNames.size)}"
                
                // Get appropriate image URL for this category
                val categoryImages = courseImageUrls[category] ?: emptyList()
                val thumbnailUrl = if (categoryImages.isNotEmpty()) {
                    categoryImages[index % categoryImages.size]
                } else {
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop"
                }
                
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
                    "thumbnailUrl" to thumbnailUrl,
                    "videoUrl" to "https://www.youtube.com/watch?v=dQw4w9WgXcQ", // Sample video URL
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
            Log.d(TAG, "Starting clearSampleData operation...")
            Log.d(TAG, "Firebase project: ${firestore.app.options.projectId}")
            Log.d(TAG, "Firestore app name: ${firestore.app.name}")
            
            val collections = listOf("users", "courses", "enrollments", "assignments", "analytics", "materials")
            val results = mutableListOf<String>()
            var totalDeleted = 0
            
            collections.forEach { collectionName ->
                Log.d(TAG, "Processing collection: $collectionName")
                val documents = firestore.collection(collectionName).get().await()
                Log.d(TAG, "Found ${documents.size()} documents in $collectionName")
                
                var batch = firestore.batch()
                var batchCount = 0
                
                documents.documents.forEach { doc ->
                    val docId = doc.id
                    var shouldDelete = false
                    var reason = ""
                    
                    // Check if document appears to be sample data based on ID patterns or content
                    if (docId.contains("teacher_") || docId.contains("student_") || 
                        docId.contains("course_") || docId.contains("enrollment_") ||
                        docId.contains("assignment_") || docId.contains("analytics_") ||
                        docId.contains("material_")) {
                        shouldDelete = true
                        reason = "ID pattern match"
                    } else {
                        // For courses collection, also check if instructor name matches sample data
                        if (collectionName == "courses") {
                            val instructor = doc.getString("instructor") ?: doc.getString("teacherName")
                            Log.d(TAG, "Course $docId has instructor: '$instructor'")
                            if (!instructor.isNullOrEmpty() && teacherNames.contains(instructor)) {
                                shouldDelete = true
                                reason = "Sample instructor name: $instructor"
                            }
                        }
                        // For users collection, check if name matches sample data
                        else if (collectionName == "users") {
                            val fullName = doc.getString("fullName") ?: doc.getString("name")
                            Log.d(TAG, "User $docId has name: '$fullName'")
                            if (!fullName.isNullOrEmpty() && 
                                (teacherNames.contains(fullName) || studentNames.contains(fullName))) {
                                shouldDelete = true
                                reason = "Sample user name: $fullName"
                            }
                        }
                    }
                    
                    if (shouldDelete) {
                        Log.d(TAG, "Marking for deletion: $docId ($reason)")
                        batch.delete(doc.reference)
                        batchCount++
                        totalDeleted++
                        
                        // Firestore batch limit is 500 operations
                        if (batchCount >= 450) {
                            Log.d(TAG, "Committing batch of $batchCount deletions")
                            batch.commit().await()
                            batch = firestore.batch()
                            batchCount = 0
                        }
                    }
                }
                
                if (batchCount > 0) {
                    Log.d(TAG, "Committing final batch of $batchCount deletions for $collectionName")
                    batch.commit().await()
                }
                
                results.add("✅ Cleared $batchCount documents from $collectionName")
                Log.d(TAG, "Completed $collectionName: deleted $batchCount documents")
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
    
    /**
     * Clears ALL data from specified collections (use with extreme caution!)
     * This is for complete database reset during development
     */
    suspend fun clearAllData(): SeedResult {
        return try {
            Log.d(TAG, "Starting clearAllData operation...")
            Log.d(TAG, "Firebase project: ${firestore.app.options.projectId}")
            Log.d(TAG, "Firestore app name: ${firestore.app.name}")
            
            val collections = listOf("courses", "enrollments", "assignments", "analytics", "materials")
            val results = mutableListOf<String>()
            var totalDeleted = 0
            
            collections.forEach { collectionName ->
                Log.d(TAG, "Processing collection: $collectionName")
                val documents = firestore.collection(collectionName).get().await()
                Log.d(TAG, "Found ${documents.size()} documents in $collectionName")
                
                var batch = firestore.batch()
                var batchCount = 0
                
                documents.documents.forEach { doc ->
                    Log.d(TAG, "Deleting document: ${doc.id}")
                    batch.delete(doc.reference)
                    batchCount++
                    totalDeleted++
                    
                    // Firestore batch limit is 500 operations
                    if (batchCount >= 450) {
                        Log.d(TAG, "Committing batch of $batchCount deletions")
                        batch.commit().await()
                        batch = firestore.batch()
                        batchCount = 0
                    }
                }
                
                if (batchCount > 0) {
                    Log.d(TAG, "Committing final batch of $batchCount deletions for $collectionName")
                    batch.commit().await()
                }
                
                results.add("✅ Cleared ALL $batchCount documents from $collectionName")
                Log.d(TAG, "Completed $collectionName: deleted $batchCount documents")
            }
            
            SeedResult(
                success = true,
                totalRecords = totalDeleted,
                details = results,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all data", e)
            SeedResult(
                success = false,
                totalRecords = 0,
                details = listOf("❌ Clear all operation failed: ${e.message}"),
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Debug function to check database connectivity and show current data
     */
    suspend fun debugDatabaseStatus(): SeedResult {
        return try {
            Log.d(TAG, "=== DATABASE DEBUG STATUS ===")
            Log.d(TAG, "Firebase project: ${firestore.app.options.projectId}")
            Log.d(TAG, "Firestore app name: ${firestore.app.name}")
            
            val collections = listOf("users", "courses", "enrollments", "assignments", "analytics", "materials")
            val results = mutableListOf<String>()
            var totalDocuments = 0
            
            collections.forEach { collectionName ->
                try {
                    val documents = firestore.collection(collectionName).get().await()
                    val count = documents.size()
                    totalDocuments += count
                    
                    Log.d(TAG, "Collection '$collectionName': $count documents")
                    results.add("📊 $collectionName: $count documents")
                    
                    // Show first few document IDs and some sample data
                    documents.documents.take(3).forEach { doc ->
                        val docId = doc.id
                        when (collectionName) {
                            "courses" -> {
                                val title = doc.getString("title") ?: "No title"
                                val instructor = doc.getString("instructor") ?: doc.getString("teacherName") ?: "No instructor"
                                Log.d(TAG, "  Course $docId: '$title' by '$instructor'")
                                results.add("    📚 $docId: '$title' by '$instructor'")
                            }
                            "users" -> {
                                val name = doc.getString("fullName") ?: doc.getString("name") ?: "No name"
                                val role = doc.getString("role") ?: "No role"
                                Log.d(TAG, "  User $docId: '$name' ($role)")
                                results.add("    👤 $docId: '$name' ($role)")
                            }
                            else -> {
                                Log.d(TAG, "  Document $docId")
                                results.add("    📄 $docId")
                            }
                        }
                    }
                    
                    if (documents.size() > 3) {
                        results.add("    ... and ${documents.size() - 3} more")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing collection $collectionName", e)
                    results.add("❌ Error accessing $collectionName: ${e.message}")
                }
            }
            
            Log.d(TAG, "=== END DATABASE DEBUG ===")
            
            SeedResult(
                success = true,
                totalRecords = totalDocuments,
                details = results,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Database debug failed", e)
            SeedResult(
                success = false,
                totalRecords = 0,
                details = listOf("❌ Database debug failed: ${e.message}"),
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