import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.WriteBatch
import java.io.ByteArrayInputStream
import kotlin.random.Random

/**
 * Standalone Firebase Data Seeder
 * Seeds Firebase Firestore with sample educational data
 */
object DatabaseSeeder {
    
    private lateinit var firestore: Firestore
    
    // Sample data
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
    
    fun initializeFirestore() {
        try {
            // Create service account JSON from google-services.json data
            val serviceAccountJson = """
            {
              "type": "service_account",
              "project_id": "eduflex-f62b5",
              "private_key_id": "dummy",
              "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKB\nxIuOiQ4SiYPi6z02MnVdIiXvVoYvt/Q7O2lWisAkycyMKNdgxllB73t+2d3dtqBz\nP-VqHbs2wqJGGSr+/Naon/VqLNSHzdKg6ztHnvQRi6D9kqmHnFTXdF6di1i7Ar\nOmMA2sHOLP43As3nWqohjFWx7PkC3I0kG38w0DFTGoiubVaFoweQQGNiXL7MrT\nDdEfxh0oojavtAMdpLcjenXdMx1ta1aaqFaO+MFgM9ioMQoXvxnYrrbMpg71lR\nqnHuHf/zSUdemMXryMhQqWhWJbYu6LgNJSatxaNLQOFVvRJDbvx3/7QIDAQAB\nAoIBABagpxpM1aoLWfvDKHcwTCDjxDsaARs2QbsQQ4xwEeYHZoANMFbfAvK4\nTpuFBFCy54k7jTKnAs/doX9dBPXrddo/vTEaSlejmLHyWRwHinS2RAEKNixa\nxLIpRHIHI7XWRjNstEa5mmDCwU2JnViGypwUII9N1s9c/OeQHUe9pLrAXyB+\nKpYoJ5GmXDqIjMj2e1ZvpsgnHgxcV5qvpexLaW+SQdWLAMpZZnqiQiKCQkBN\nBFRqh17s1+0ba2LQlvKjpMpAHMhVuFh4uuiWiVhKu0WeOuPiLbFxQ4aLSOhI\nXh73y8CQdPLQpLmyOLIf7wjEeWC3F1g6R4EQC+2YgwHqmxHHm+g=\n-----END PRIVATE KEY-----\n",
              "client_email": "firebase-adminsdk-dummy@eduflex-f62b5.iam.gserviceaccount.com",
              "client_id": "dummy",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
              "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-dummy%40eduflex-f62b5.iam.gserviceaccount.com"
            }
            """.trimIndent()
            
            val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(serviceAccountJson.toByteArray()))
            
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("eduflex-f62b5")
                .build()
            
            FirebaseApp.initializeApp(options)
            firestore = FirestoreClient.getFirestore()
            println("✅ Firestore initialized successfully")
        } catch (e: Exception) {
            println("❌ Failed to initialize Firestore: ${e.message}")
            // Try alternative initialization using API key
            initializeWithApiKey()
        }
    }
    
    private fun initializeWithApiKey() {
        try {
            println("🔄 Trying alternative initialization...")
            val options = FirebaseOptions.builder()
                .setProjectId("eduflex-f62b5")
                .build()
            
            FirebaseApp.initializeApp(options)
            firestore = FirestoreClient.getFirestore()
            println("✅ Firestore initialized with API key")
        } catch (e: Exception) {
            println("❌ Alternative initialization failed: ${e.message}")
            throw e
        }
    }
    
    fun seedDatabase() {
        try {
            println("🌱 Starting database seeding...")
            
            // Test connectivity
            testConnectivity()
            
            var totalRecords = 0
            
            // Create users
            println("👥 Creating users...")
            totalRecords += createUsers()
            
            // Create courses
            println("📚 Creating courses...")
            totalRecords += createCourses()
            
            // Create enrollments
            println("📝 Creating enrollments...")
            totalRecords += createEnrollments()
            
            // Create assignments
            println("📋 Creating assignments...")
            totalRecords += createAssignments()
            
            // Create analytics
            println("📊 Creating analytics...")
            totalRecords += createAnalytics()
            
            // Create materials
            println("📄 Creating materials...")
            totalRecords += createMaterials()
            
            println("✅ Database seeding completed! Total records: $totalRecords")
            
        } catch (e: Exception) {
            println("❌ Database seeding failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun testConnectivity() {
        try {
            val testDoc = firestore.collection("connection_test").document("test")
            testDoc.set(mapOf("test" to true, "timestamp" to System.currentTimeMillis())).get()
            println("✅ Firestore connectivity test successful")
            
            // Clean up
            testDoc.delete().get()
        } catch (e: Exception) {
            throw Exception("Firestore connectivity test failed: ${e.message}")
        }
    }
    
    private fun createUsers(): Int {
        val batch = firestore.batch()
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
                "grade" to Random.nextInt(9, 13),
                "dateOfBirth" to (System.currentTimeMillis() - Random.nextLong(15 * 365 * 24 * 60 * 60 * 1000L, 18 * 365 * 24 * 60 * 60 * 1000L)),
                "parentEmail" to "parent.${name.lowercase().replace(" ", ".")}@email.com"
            )
            
            batch.set(firestore.collection("users").document(studentId), student)
            count++
        }
        
        batch.commit().get()
        println("✅ Created $count users (${teacherNames.size} teachers, ${studentNames.size} students)")
        return count
    }
    
    private fun createCourses(): Int {
        val batch = firestore.batch()
        var count = 0
        
        courseCategories.forEach { category ->
            val titles = courseTitles[category] ?: listOf("$category Course")
            titles.forEach { title ->
                val courseId = "course_${category.lowercase().replace(" ", "_")}_${title.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}"
                
                val course = mapOf(
                    "id" to courseId,
                    "title" to title,
                    "category" to category,
                    "description" to "Comprehensive $title course covering fundamental concepts and practical applications.",
                    "instructor" to teacherNames.random(),
                    "duration" to "${Random.nextInt(8, 16)} weeks",
                    "difficulty" to listOf("Beginner", "Intermediate", "Advanced").random(),
                    "maxStudents" to Random.nextInt(20, 50),
                    "currentEnrollment" to Random.nextInt(5, 30),
                    "price" to Random.nextDouble(50.0, 500.0),
                    "rating" to Random.nextDouble(3.5, 5.0),
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "startDate" to (System.currentTimeMillis() + Random.nextLong(0, 30 * 24 * 60 * 60 * 1000)),
                    "endDate" to (System.currentTimeMillis() + Random.nextLong(60 * 24 * 60 * 60 * 1000L, 120 * 24 * 60 * 60 * 1000L))
                )
                
                batch.set(firestore.collection("courses").document(courseId), course)
                count++
            }
        }
        
        batch.commit().get()
        println("✅ Created $count courses")
        return count
    }
    
    private fun createEnrollments(): Int {
        val batch = firestore.batch()
        var count = 0
        
        // Get all students and courses
        val students = firestore.collection("users").whereEqualTo("role", "Student").get().get().documents
        val courses = firestore.collection("courses").get().get().documents
        
        students.forEach { student ->
            val studentId = student.id
            val enrollmentCount = Random.nextInt(2, 6) // Each student enrolls in 2-5 courses
            val selectedCourses = courses.shuffled().take(enrollmentCount)
            
            selectedCourses.forEach { course ->
                val courseId = course.id
                val enrollmentId = "enrollment_${studentId}_${courseId}"
                
                val enrollment = mapOf(
                    "id" to enrollmentId,
                    "studentId" to studentId,
                    "courseId" to courseId,
                    "enrollmentDate" to (System.currentTimeMillis() - Random.nextLong(0, 60 * 24 * 60 * 60 * 1000)),
                    "status" to listOf("Active", "Completed", "In Progress").random(),
                    "progress" to Random.nextDouble(0.0, 100.0),
                    "grade" to if (Random.nextBoolean()) Random.nextDouble(60.0, 95.0) else null,
                    "lastAccessed" to (System.currentTimeMillis() - Random.nextLong(0, 7 * 24 * 60 * 60 * 1000))
                )
                
                batch.set(firestore.collection("enrollments").document(enrollmentId), enrollment)
                count++
            }
        }
        
        batch.commit().get()
        println("✅ Created $count enrollments")
        return count
    }
    
    private fun createAssignments(): Int {
        val batch = firestore.batch()
        var count = 0
        
        val courses = firestore.collection("courses").get().get().documents
        
        courses.forEach { course ->
            val courseId = course.id
            val courseTitle = course.getString("title") ?: "Course"
            val assignmentCount = Random.nextInt(3, 8)
            
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
                    "instructions" to "Follow the guidelines provided in class and submit your completed work.",
                    "submissionFormat" to listOf("PDF", "Word Document", "Online Form", "Video").random()
                )
                
                batch.set(firestore.collection("assignments").document(assignmentId), assignment)
                count++
            }
        }
        
        batch.commit().get()
        println("✅ Created $count assignments")
        return count
    }
    
    private fun createAnalytics(): Int {
        val batch = firestore.batch()
        var count = 0
        
        val students = firestore.collection("users").whereEqualTo("role", "Student").get().get().documents
        
        students.forEach { student ->
            val studentId = student.id
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
                )
            )
            
            batch.set(firestore.collection("analytics").document(analyticsId), analytics)
            count++
        }
        
        batch.commit().get()
        println("✅ Created $count analytics records")
        return count
    }
    
    private fun createMaterials(): Int {
        val batch = firestore.batch()
        var count = 0
        
        val courses = firestore.collection("courses").get().get().documents
        
        courses.forEach { course ->
            val courseId = course.id
            val materialCount = Random.nextInt(3, 8)
            
            repeat(materialCount) { index ->
                val materialId = "material_${courseId}_${index}"
                val materialTypes = listOf("video", "document", "quiz", "assignment")
                val type = materialTypes.random()
                
                val material = mapOf(
                    "id" to materialId,
                    "courseId" to courseId,
                    "title" to "Course Material ${index + 1}",
                    "type" to type,
                    "url" to "https://example.com/materials/$materialId",
                    "duration" to if (type == "video") Random.nextInt(300, 3600) else null,
                    "uploadedAt" to System.currentTimeMillis(),
                    "isActive" to true,
                    "description" to "Educational material for course content.",
                    "fileSize" to Random.nextLong(1024 * 1024, 100 * 1024 * 1024) // 1MB to 100MB
                )
                
                batch.set(firestore.collection("materials").document(materialId), material)
                count++
            }
        }
        
        batch.commit().get()
        println("✅ Created $count materials")
        return count
    }
}

fun main() {
    try {
        DatabaseSeeder.initializeFirestore()
        DatabaseSeeder.seedDatabase()
        println("\n🎉 Database seeding completed successfully!")
    } catch (e: Exception) {
        println("\n💥 Database seeding failed: ${e.message}")
        e.printStackTrace()
    }
}