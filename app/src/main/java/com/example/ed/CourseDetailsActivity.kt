package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.adapters.CourseSectionAdapter
import com.example.ed.databinding.ActivityCourseDetailsBinding
import com.example.ed.models.Course
import com.example.ed.models.CourseSection
import com.example.ed.models.Lesson
import com.example.ed.models.LessonType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Build
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color

class CourseDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var courseSectionAdapter: CourseSectionAdapter
    
    private var currentCourse: Course? = null
    private var userRole: String = "Student" // Default to Student
    private var isBookmarked: Boolean = false
    private var isSectionsLoading: Boolean = false
    private var isEnrolled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get course ID from intent
        val courseId = intent.getStringExtra("COURSE_ID") ?: ""
        
        setupUI()
        loadUserRole()
        loadCourseDetails(courseId)
        setupClickListeners()
    }

    private fun setupUI() {
        // Setup RecyclerView
        courseSectionAdapter = CourseSectionAdapter(mutableListOf()) { sectionId, lessonId ->
            onLessonClick(sectionId, lessonId)
        }
        
        binding.rvCourseContent.apply {
            layoutManager = LinearLayoutManager(this@CourseDetailsActivity)
            adapter = courseSectionAdapter
        }

        // Setup bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, StudentDashboardFragmentActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_courses -> {
                    // Already in courses section
                    true
                }
                R.id.nav_live -> {
                    Toast.makeText(this, "Live sessions coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Set courses as selected
        binding.bottomNavigation.selectedItemId = R.id.nav_courses
    }

    private fun loadUserRole() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userRole = document.getString("role") ?: "Student"
                        updateUIForUserRole()
                    }
                }
                .addOnFailureListener {
                    // Default to Student role
                    userRole = "Student"
                    updateUIForUserRole()
                }
        }
    }

    private fun updateUIForUserRole() {
        // Teachers can see additional options or different UI
        if (userRole == "Teacher") {
            // Show teacher-specific UI elements
            binding.btnStartLesson.text = "Manage Course"
            binding.btnTakeQuiz.text = "Edit Content"
            
            // Teachers can access all course content regardless of progress
            // This is where you'd add teacher-specific functionality like:
            // - Edit course content
            // - View student progress
            // - Manage assignments
            Toast.makeText(this, "Teacher mode: Full course access enabled", Toast.LENGTH_SHORT).show()
        } else {
            // Student mode - normal functionality
            binding.btnStartLesson.text = "Start Lesson"
            binding.btnTakeQuiz.text = "Take Quiz"
        }
    }

    private fun loadCourseDetails(courseId: String) {
        if (courseId.isEmpty()) {
            // Load sample course for demonstration
            loadSampleCourse()
            return
        }

        // Load actual course from Firebase
        firestore.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Parse actual course from Firestore
                    try {
                        val courseTitle = document.getString("title") ?: "Course Title"
                        val courseInstructor = document.getString("instructor") ?: document.getString("teacherName") ?: "Instructor"
                        val courseDescription = document.getString("description") ?: "Course description"
                        
                        android.util.Log.d("CourseDetails", "Loading course: $courseTitle by $courseInstructor")
                        android.util.Log.d("CourseDetails", "Course ID: ${document.id}")
                        android.util.Log.d("CourseDetails", "Document data: ${document.data}")
                        
                        // Handle duration field which might be stored as different types
                        val courseDuration = try {
                            document.getString("duration") ?: "4 weeks"
                        } catch (e: Exception) {
                            // If duration is stored as a number, convert it
                            try {
                                val durationNum = document.getLong("duration") ?: 4
                                "${durationNum} weeks"
                            } catch (e2: Exception) {
                                "4 weeks"
                            }
                        }
                        
                        // Handle createdAt field safely
                        val courseCreatedAt = try {
                            document.getLong("createdAt") ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            try {
                                // Try as Timestamp
                                document.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                            } catch (e2: Exception) {
                                System.currentTimeMillis()
                            }
                        }
                        
                        // Handle updatedAt field safely
                        val courseUpdatedAt = try {
                            document.getLong("updatedAt") ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            try {
                                // Try as Timestamp
                                document.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            } catch (e2: Exception) {
                                System.currentTimeMillis()
                            }
                        }
                        
                        // Handle numeric fields safely
                        val courseEnrolledStudents = try {
                            document.getLong("enrolledStudents")?.toInt() ?: 0
                        } catch (e: Exception) {
                            0
                        }
                        
                        val courseRating = try {
                            document.getDouble("rating")?.toFloat() ?: 0.0f
                        } catch (e: Exception) {
                            0.0f
                        }
                        
                        val coursePrice = try {
                            document.getDouble("price") ?: 0.0
                        } catch (e: Exception) {
                            0.0
                        }
                        
                        val courseTotalLessons = try {
                            document.getLong("totalLessons")?.toInt() ?: 0
                        } catch (e: Exception) {
                            0
                        }
                        
                        currentCourse = Course(
                            id = document.id,
                            title = courseTitle,
                            instructor = courseInstructor,
                            description = courseDescription,
                            category = document.getString("category") ?: "",
                            difficulty = document.getString("difficulty") ?: "Beginner",
                            duration = courseDuration,
                            thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                            isPublished = document.getBoolean("isPublished") ?: true,
                            createdAt = courseCreatedAt,
                            updatedAt = courseUpdatedAt,
                            enrolledStudents = courseEnrolledStudents,
                            rating = courseRating,
                            teacherId = document.getString("teacherId") ?: "",
                            price = coursePrice,
                            totalLessons = courseTotalLessons,
                            isFree = document.getBoolean("isFree") ?: true,
                            progress = 0, // Will be calculated based on user progress
                            completedLessons = 0, // Will be calculated based on user progress
                            isBookmarked = false, // Will be loaded separately
                            courseContent = emptyList() // Will load real weekly content next
                        )
                        
                        android.util.Log.d("CourseDetails", "Successfully created course object: ${currentCourse?.title}")
                        // Fetch real weekly content from Firestore and update UI when done
                        fetchWeeklySections(courseId) { sections ->
                            try {
                                val existing = currentCourse
                                if (existing != null) {
                                    currentCourse = Course(
                                        id = existing.id,
                                        title = existing.title,
                                        instructor = existing.instructor,
                                        description = existing.description,
                                        category = existing.category,
                                        difficulty = existing.difficulty,
                                        duration = existing.duration,
                                        thumbnailUrl = existing.thumbnailUrl,
                                        isPublished = existing.isPublished,
                                        createdAt = existing.createdAt,
                                        updatedAt = existing.updatedAt,
                                        enrolledStudents = existing.enrolledStudents,
                                        rating = existing.rating,
                                        teacherId = existing.teacherId,
                                        price = existing.price,
                                        totalLessons = sections.sumOf { it.lessons.size },
                                        isFree = existing.isFree,
                                        courseContent = sections,
                                        progress = existing.progress,
                                        completedLessons = existing.completedLessons,
                                        isBookmarked = existing.isBookmarked
                                    )
                                }
                                // Check enrollment status before allowing access
                                checkEnrollmentStatus(courseId) {
                                    updateUI()
                                    loadUserProgress(courseId)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CourseDetails", "Error applying weekly sections", e)
                                checkEnrollmentStatus(courseId) {
                                    updateUI()
                                    loadUserProgress(courseId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CourseDetails", "Error parsing course data", e)
                        Toast.makeText(this, "Error loading course details: ${e.message}", Toast.LENGTH_SHORT).show()
                        loadSampleCourse()
                    }
                } else {
                    Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show()
                    loadSampleCourse()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading course: ${e.message}", Toast.LENGTH_SHORT).show()
                loadSampleCourse()
            }
    }

    private fun loadSampleCourse() {
        // Create sample course data
        val sampleLessons1 = listOf(
            Lesson(
                id = "lesson1",
                title = "Basic Algebraic Operations",
                duration = "15 min",
                type = LessonType.VIDEO,
                isCompleted = true
            ),
            Lesson(
                id = "lesson2",
                title = "Solving Linear Equations",
                duration = "20 min",
                type = LessonType.VIDEO,
                isCompleted = false
            ),
            Lesson(
                id = "lesson3",
                title = "Practice Problems",
                duration = "30 min",
                type = LessonType.PRACTICE,
                isCompleted = false
            )
        )

        val sampleLessons2 = listOf(
            Lesson(
                id = "lesson4",
                title = "Quadratic Equations",
                duration = "25 min",
                type = LessonType.VIDEO,
                isCompleted = false
            ),
            Lesson(
                id = "lesson5",
                title = "Graphing Functions",
                duration = "18 min",
                type = LessonType.VIDEO,
                isCompleted = false
            ),
            Lesson(
                id = "lesson6",
                title = "Chapter Quiz",
                duration = "15 min",
                type = LessonType.QUIZ,
                isCompleted = false
            )
        )

        val courseSections = listOf(
            CourseSection(
                id = "section1",
                title = "Introduction to Algebra",
                lessons = sampleLessons1,
                isExpanded = true
            ),
            CourseSection(
                id = "section2",
                title = "Advanced Topics",
                lessons = sampleLessons2,
                isExpanded = false
            )
        )

        currentCourse = Course(
            id = "course1",
            title = "Advanced Mathematics",
            instructor = "Dr. Nimal Perera",
            description = "Master advanced mathematical concepts with practical examples and interactive exercises designed for Sri Lankan O/L and A/L students.",
            progress = 65,
            totalLessons = 6,
            completedLessons = 1,
            courseContent = courseSections
        )

        updateUI()
    }

    /**
     * Fetch weekly content from Firestore and map to CourseSection/Lesson for the student view.
     * Uses collection "weekly_content" for course content.
     */
    private fun fetchWeeklySections(courseId: String, onDone: (List<CourseSection>) -> Unit) {
        isSectionsLoading = true
        val userId = auth.currentUser?.uid
        firestore.collection("weekly_content")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("isPublished", true)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val docs = if (snapshot.isEmpty) {
                        // Fallback: try without isPublished filter (maybe teacher left drafts)
                        null
                    } else snapshot.documents

                    if (docs == null) {
                        firestore.collection("weekly_content")
                            .whereEqualTo("courseId", courseId)
                            .get()
                            .addOnSuccessListener { snap2 ->
                                handleWeeklyDocs(snap2.documents, userId, onDone)
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("CourseDetails", "Error fetching fallback weekly sections", e)
                                isSectionsLoading = false
                                onDone(emptyList())
                            }
                        return@addOnSuccessListener
                    }

                    val rawSections = docs.mapNotNull { doc ->
                        try {
                            val weekNumber = (doc.getLong("weekNumber") ?: 0L).toInt()
                            val weekTitle = doc.getString("title") ?: "Week $weekNumber"
                            val contentItems = (doc.get("contentItems") as? List<*>) ?: emptyList<Any>()
                            val progressMap = doc.get("studentProgress") as? Map<*, *>
                            val myProgress = if (userId != null) progressMap?.get(userId) as? Map<*, *> else null
                            val completedIds: Set<String> = (myProgress?.get("contentItemsCompleted") as? List<*>)
                                ?.mapNotNull { it as? String }
                                ?.toSet() ?: emptySet()

                            // Map content items to lessons
                            val lessons: List<Lesson> = contentItems.mapNotNull { anyItem ->
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val item = anyItem as Map<String, Any?>
                                    val id = (item["id"] as? String) ?: doc.id + "_" + (item["order"] as? Long ?: 0L)
                                    val title = (item["title"] as? String) ?: (item["type"] as? String ?: "Content")
                                    val durationMin = ((item["duration"] as? Long) ?: (item["duration"] as? Int)?.toLong() ?: 0L).toInt()
                                    val order = ((item["order"] as? Long) ?: 0L).toInt()
                                    val typeStr = (item["type"] as? String) ?: "TEXT"
                                    val lessonType = when (typeStr.uppercase(Locale.getDefault())) {
                                        "VIDEO" -> LessonType.VIDEO
                                        "DOCUMENT", "TEXT", "PRESENTATION", "AUDIO" -> LessonType.READING
                                        "INTERACTIVE", "SIMULATION", "CASE_STUDY" -> LessonType.PRACTICE
                                        else -> LessonType.READING
                                    }
                                    Lesson(
                                        id = id,
                                        title = title,
                                        duration = if (durationMin > 0) "${durationMin} min" else "",
                                        type = lessonType,
                                        isCompleted = completedIds.contains(id),
                                        order = order
                                    )
                                } catch (_: Exception) { null }
                            }.sortedBy { it.order }

                            // Optionally append a quiz as a lesson if present
                            val hasQuiz = doc.get("quiz") != null
                            val fullLessons = if (hasQuiz) {
                                lessons + Lesson(
                                    id = doc.id + "_quiz",
                                    title = "Weekly Quiz",
                                    duration = "",
                                    type = LessonType.QUIZ,
                                    isCompleted = completedIds.contains(doc.id + "_quiz"),
                                    order = (lessons.maxOfOrNull { it.order } ?: 0) + 1
                                )
                            } else lessons

                            CourseSection(
                                id = doc.id,
                                title = "Week $weekNumber: $weekTitle",
                                lessons = fullLessons,
                                isExpanded = weekNumber == 1 // expand first week by default
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("CourseDetails", "Error mapping weekly content doc ${doc.id}", e)
                            null
                        }
                    }
                    // Sort weeks client-side to avoid composite index requirement
                    val sections = rawSections.sortedBy { sec ->
                        // Extract weekNumber from title prefix "Week X:"
                        val match = Regex("^Week\\s+(\\d+)").find(sec.title)
                        match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                    }

                    // Compute overall progress from completed lessons
                    val totalLessons = sections.sumOf { it.lessons.size }
                    val completedLessons = sections.sumOf { it.lessons.count { l -> l.isCompleted } }
                    val progressPercent = if (totalLessons > 0) (completedLessons * 100 / totalLessons) else 0

                    // Update currentCourse fields if available
                    currentCourse?.let { existing ->
                        currentCourse = Course(
                            id = existing.id,
                            title = existing.title,
                            instructor = existing.instructor,
                            description = existing.description,
                            category = existing.category,
                            difficulty = existing.difficulty,
                            duration = existing.duration,
                            thumbnailUrl = existing.thumbnailUrl,
                            isPublished = existing.isPublished,
                            createdAt = existing.createdAt,
                            updatedAt = existing.updatedAt,
                            enrolledStudents = existing.enrolledStudents,
                            rating = existing.rating,
                            teacherId = existing.teacherId,
                            price = existing.price,
                            totalLessons = totalLessons,
                            isFree = existing.isFree,
                            courseContent = sections,
                            progress = progressPercent,
                            completedLessons = completedLessons,
                            isBookmarked = existing.isBookmarked
                        )
                    }

                    isSectionsLoading = false
                    onDone(sections)
                } catch (e: Exception) {
                    android.util.Log.e("CourseDetails", "Failed to parse weekly sections", e)
                    isSectionsLoading = false
                    onDone(emptyList())
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CourseDetails", "Error fetching weekly sections", e)
                isSectionsLoading = false
                onDone(emptyList())
            }
    }

    private fun handleWeeklyDocs(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        userId: String?,
        onDone: (List<CourseSection>) -> Unit
    ) {
        try {
            val rawSections = documents.mapNotNull { doc ->
                try {
                    val weekNumber = (doc.getLong("weekNumber") ?: 0L).toInt()
                    val weekTitle = doc.getString("title") ?: "Week $weekNumber"
                    val contentItems = (doc.get("contentItems") as? List<*>) ?: emptyList<Any>()
                    val progressMap = doc.get("studentProgress") as? Map<*, *>
                    val myProgress = if (userId != null) progressMap?.get(userId) as? Map<*, *> else null
                    val completedIds: Set<String> = (myProgress?.get("contentItemsCompleted") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.toSet() ?: emptySet()

                    val lessons: List<Lesson> = contentItems.mapNotNull { anyItem ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val item = anyItem as Map<String, Any?>
                            val id = (item["id"] as? String) ?: doc.id + "_" + (item["order"] as? Long ?: 0L)
                            val title = (item["title"] as? String) ?: (item["type"] as? String ?: "Content")
                            val durationMin = ((item["duration"] as? Long) ?: (item["duration"] as? Int)?.toLong() ?: 0L).toInt()
                            val order = ((item["order"] as? Long) ?: 0L).toInt()
                            val typeStr = (item["type"] as? String) ?: "TEXT"
                            val lessonType = when (typeStr.uppercase(Locale.getDefault())) {
                                "VIDEO" -> LessonType.VIDEO
                                "DOCUMENT", "TEXT", "PRESENTATION", "AUDIO" -> LessonType.READING
                                "INTERACTIVE", "SIMULATION", "CASE_STUDY" -> LessonType.PRACTICE
                                else -> LessonType.READING
                            }
                            Lesson(
                                id = id,
                                title = title,
                                duration = if (durationMin > 0) "${durationMin} min" else "",
                                type = lessonType,
                                isCompleted = completedIds.contains(id),
                                order = order
                            )
                        } catch (_: Exception) { null }
                    }.sortedBy { it.order }

                    val hasQuiz = doc.get("quiz") != null
                    val fullLessons = if (hasQuiz) {
                        lessons + Lesson(
                            id = doc.id + "_quiz",
                            title = "Weekly Quiz",
                            duration = "",
                            type = LessonType.QUIZ,
                            isCompleted = completedIds.contains(doc.id + "_quiz"),
                            order = (lessons.maxOfOrNull { it.order } ?: 0) + 1
                        )
                    } else lessons

                    CourseSection(
                        id = doc.id,
                        title = "Week $weekNumber: $weekTitle",
                        lessons = fullLessons,
                        isExpanded = weekNumber == 1
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CourseDetails", "Error mapping weekly content doc ${doc.id}", e)
                    null
                }
            }

            val sections = rawSections.sortedBy { sec ->
                val match = Regex("^Week\\s+(\\d+)").find(sec.title)
                match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
            }

            val totalLessons = sections.sumOf { it.lessons.size }
            val completedLessons = sections.sumOf { it.lessons.count { l -> l.isCompleted } }
            val progressPercent = if (totalLessons > 0) (completedLessons * 100 / totalLessons) else 0

            currentCourse?.let { existing ->
                currentCourse = Course(
                    id = existing.id,
                    title = existing.title,
                    instructor = existing.instructor,
                    description = existing.description,
                    category = existing.category,
                    difficulty = existing.difficulty,
                    duration = existing.duration,
                    thumbnailUrl = existing.thumbnailUrl,
                    isPublished = existing.isPublished,
                    createdAt = existing.createdAt,
                    updatedAt = existing.updatedAt,
                    enrolledStudents = existing.enrolledStudents,
                    rating = existing.rating,
                    teacherId = existing.teacherId,
                    price = existing.price,
                    totalLessons = totalLessons,
                    isFree = existing.isFree,
                    courseContent = sections,
                    progress = progressPercent,
                    completedLessons = completedLessons,
                    isBookmarked = existing.isBookmarked
                )
            }

            isSectionsLoading = false
            onDone(sections)
        } catch (e: Exception) {
            android.util.Log.e("CourseDetails", "handleWeeklyDocs parse error", e)
            isSectionsLoading = false
            onDone(emptyList())
        }
    }

    private fun loadUserProgress(courseId: String) {
        val currentUser = auth.currentUser ?: return
        
        // Load user's progress for this specific course
        firestore.collection("enrollments")
            .whereEqualTo("studentId", currentUser.uid)
            .whereEqualTo("courseId", courseId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val enrollment = documents.documents[0]
                    val progress = enrollment.getDouble("progress")?.toInt() ?: 0
                    val completedLessons = enrollment.getLong("completedLessons")?.toInt() ?: 0
                    
                    android.util.Log.d("CourseDetails", "Found enrollment with progress: $progress%, completed: $completedLessons")
                    
                    // Update course with user-specific progress by creating a new Course object
                    currentCourse?.let { course ->
                        currentCourse = Course(
                            id = course.id,
                            title = course.title,
                            instructor = course.instructor,
                            description = course.description,
                            category = course.category,
                            difficulty = course.difficulty,
                            duration = course.duration,
                            thumbnailUrl = course.thumbnailUrl,
                            isPublished = course.isPublished,
                            createdAt = course.createdAt,
                            updatedAt = course.updatedAt,
                            enrolledStudents = course.enrolledStudents,
                            rating = course.rating,
                            teacherId = course.teacherId,
                            price = course.price,
                            totalLessons = course.totalLessons,
                            isFree = course.isFree,
                            courseContent = course.courseContent,
                            progress = progress, // Updated progress
                            completedLessons = completedLessons, // Updated completed lessons
                            isBookmarked = course.isBookmarked
                        )
                        updateUI()
                    }
                } else {
                    android.util.Log.d("CourseDetails", "No enrollment found for course: $courseId")
                }
                // Also load bookmark status
                loadBookmarkStatus(courseId)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CourseDetails", "Error loading user progress", e)
                loadBookmarkStatus(courseId)
            }
    }

    private fun loadBookmarkStatus(courseId: String) {
        val currentUser = auth.currentUser ?: return
        
        firestore.collection("users")
            .document(currentUser.uid)
            .collection("bookmarks")
            .document(courseId)
            .get()
            .addOnSuccessListener { document ->
                isBookmarked = document.getBoolean("isBookmarked") ?: false
                updateBookmarkUI()
            }
    }

    private fun updateUI() {
        currentCourse?.let { course ->
            android.util.Log.d("CourseDetails", "Updating UI with course: ${course.title}")
            android.util.Log.d("CourseDetails", "Course instructor: ${course.instructor}")
            android.util.Log.d("CourseDetails", "Course description: ${course.description}")
            android.util.Log.d("CourseDetails", "Enrollment status: $isEnrolled")
            
            binding.tvCourseTitle.text = course.title
            binding.tvInstructor.text = "By ${course.instructor}"
            binding.tvCourseDescription.text = course.description
            
            // Check enrollment status for paid courses
            val isPaidCourse = !course.isFree && course.price > 0.0
            val canAccessContent = isEnrolled || userRole == "Teacher" || !isPaidCourse
            
            if (canAccessContent) {
                // User has access - show normal course content
                binding.progressBar.progress = course.progress
                binding.tvProgressPercentage.text = "${course.progress}%"
                
                // Show certificate download when course is 100% complete
                val showCert = course.progress >= 100
                val certButton = findViewById<android.widget.Button>(R.id.btn_download_certificate)
                certButton?.visibility = if (showCert) View.VISIBLE else View.GONE
                if (showCert) {
                    certButton?.setOnClickListener {
                        generateCertificatePdf(course)
                    }
                }
                
                // Update course content
                courseSectionAdapter.updateSections(course.courseContent)
                
                // Show enrollment status for paid courses
                if (isPaidCourse && isEnrolled) {
                    Toast.makeText(this, "✅ You are enrolled in this course", Toast.LENGTH_SHORT).show()
                }
            } else {
                // User doesn't have access - show enrollment required message
                binding.progressBar.progress = 0
                binding.tvProgressPercentage.text = "Enrollment Required"
                
                // Hide certificate button
                val certButton = findViewById<android.widget.Button>(R.id.btn_download_certificate)
                certButton?.visibility = View.GONE
                
                // Show empty course content with enrollment message
                courseSectionAdapter.updateSections(emptyList())
                
                // Show enrollment required message
                Toast.makeText(this, "⚠️ Enrollment required to access this paid course", Toast.LENGTH_LONG).show()
                
                // Optionally redirect to enrollment activity
                binding.btnStartLesson.text = "Enroll Now - $${course.price}"
                binding.btnStartLesson.setOnClickListener {
                    val intent = Intent(this, CourseEnrollmentActivity::class.java)
                    intent.putExtra("COURSE_ID", course.id)
                    intent.putExtra("COURSE_TITLE", course.title)
                    intent.putExtra("COURSE_PRICE", course.price)
                    startActivity(intent)
                }
            }
            
            // Update bookmark status
            updateBookmarkUI()
            
            android.util.Log.d("CourseDetails", "UI update completed")
        } ?: run {
            android.util.Log.e("CourseDetails", "currentCourse is null, cannot update UI")
        }
    }

    private fun generateCertificatePdf(course: Course) {
        try {
            val user = auth.currentUser
            val studentName = user?.displayName ?: "Student"
            val fileName = "${course.title}_Certificate_${System.currentTimeMillis()}.pdf"

            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (approx)
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }

            // Title
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 28f
            canvas.drawText("Certificate of Completion", (pageInfo.pageWidth / 2).toFloat(), 120f, paint)

            // Subtitle
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            canvas.drawText("This is to certify that", (pageInfo.pageWidth / 2).toFloat(), 170f, paint)

            // Student Name
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            canvas.drawText(studentName, (pageInfo.pageWidth / 2).toFloat(), 210f, paint)

            // Course line
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            canvas.drawText("has successfully completed the course", (pageInfo.pageWidth / 2).toFloat(), 250f, paint)

            // Course Title
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            canvas.drawText(course.title, (pageInfo.pageWidth / 2).toFloat(), 290f, paint)

            // Date
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            canvas.drawText("Date: $date", (pageInfo.pageWidth / 2).toFloat(), 340f, paint)

            pdf.finishPage(page)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/")
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdf.writeTo(out)
                    }
                    Toast.makeText(this, "Certificate saved to Downloads", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to save certificate", Toast.LENGTH_LONG).show()
                }
            } else {
                // Legacy: save to external storage root (permission may be required on old devices)
                val path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(path, fileName)
                path.mkdirs()
                java.io.FileOutputStream(file).use { out -> pdf.writeTo(out) }
                Toast.makeText(this, "Certificate saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }

            pdf.close()
        } catch (e: Exception) {
            android.util.Log.e("CourseDetails", "Error generating certificate", e)
            Toast.makeText(this, "Failed to generate certificate", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateBookmarkUI() {
        val bookmarkIcon = if (isBookmarked) {
            R.drawable.ic_bookmark_filled
        } else {
            R.drawable.ic_bookmark_border
        }
        binding.btnBookmark.setImageResource(bookmarkIcon)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBookmark.setOnClickListener {
            toggleBookmark()
        }

        binding.btnStartLesson.setOnClickListener {
            if (userRole == "Teacher") {
                // Teacher functionality - manage course
                Toast.makeText(this, "Course management coming soon", Toast.LENGTH_SHORT).show()
            } else {
                // Student functionality - start weekly content
                startWeeklyContent()
            }
        }

        binding.btnTakeQuiz.setOnClickListener {
            if (userRole == "Teacher") {
                // Teacher functionality - edit content
                Toast.makeText(this, "Content editing coming soon", Toast.LENGTH_SHORT).show()
            } else {
                // Student functionality - take weekly quiz if available
                if (isSectionsLoading) {
                    Toast.makeText(this, "Loading course content...", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val course = currentCourse
                if (course == null || course.courseContent.isEmpty()) {
                    Toast.makeText(this, "No content available yet", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Find first section with a quiz lesson
                val quizEntry = course.courseContent.firstNotNullOfOrNull { section ->
                    val quizLesson = section.lessons.firstOrNull { it.type == LessonType.QUIZ }
                    if (quizLesson != null) Pair(section.id, quizLesson.id) else null
                }
                if (quizEntry != null) {
                    startLesson(quizEntry.first, quizEntry.second)
                } else {
                    Toast.makeText(this, "No quiz available for this course", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleBookmark() {
        isBookmarked = !isBookmarked
        updateBookmarkUI()
        
        val message = if (isBookmarked) {
            "Course bookmarked"
        } else {
            "Bookmark removed"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        // In a real app, you'd save this to Firebase
        saveBookmarkStatus()
    }

    private fun saveBookmarkStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentCourse != null) {
            val bookmarkData = mapOf(
                "courseId" to currentCourse!!.id,
                "isBookmarked" to isBookmarked,
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(currentUser.uid)
                .collection("bookmarks")
                .document(currentCourse!!.id)
                .set(bookmarkData)
        }
    }

    private fun findNextIncompleteLesson(course: Course): Pair<String, String>? {
        for (section in course.courseContent) {
            for (lesson in section.lessons) {
                if (!lesson.isCompleted) {
                    return Pair(section.id, lesson.id)
                }
            }
        }
        return null
    }

    private fun startWeeklyContent() {
        // Check if user is a student
        if (userRole == "Student") {
            // For now, redirect to the fragment-based dashboard
            val intent = Intent(this, StudentDashboardFragmentActivity::class.java)
            intent.putExtra("OPEN_WEEKLY_CONTENT", true)
            intent.putExtra("COURSE_ID", currentCourse?.id)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Weekly content is for students only", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onLessonClick(sectionId: String, lessonId: String) {
        // Check if user has access (teachers can access all, students need to follow order)
        if (userRole == "Teacher" || canAccessLesson(sectionId, lessonId)) {
            startLesson(sectionId, lessonId)
        } else {
            Toast.makeText(this, "Complete previous lessons first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canAccessLesson(sectionId: String, lessonId: String): Boolean {
        currentCourse?.let { course ->
            // First check enrollment status for paid courses
            val isPaidCourse = !course.isFree && course.price > 0.0
            if (isPaidCourse && !isEnrolled && userRole != "Teacher") {
                return false
            }
            
            // Students can only access lessons in order
            var canAccess = true
            
            for (section in course.courseContent) {
                for (lesson in section.lessons) {
                    if (lesson.id == lessonId) {
                        return canAccess
                    }
                    if (!lesson.isCompleted) {
                        canAccess = false
                    }
                }
            }
        }
        return false
    }

    private fun startLesson(sectionId: String, lessonId: String) {
        // TODO: Implement lesson viewing functionality
        Toast.makeText(this, "Lesson viewing feature is not available", Toast.LENGTH_SHORT).show()
    }

    private fun checkEnrollmentStatus(courseId: String, callback: () -> Unit) {
        try {
            val currentUser = auth.currentUser ?: return callback()
            
            firestore.collection("enrollments")
                .whereEqualTo("studentId", currentUser.uid)
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener { documents ->
                    isEnrolled = !documents.isEmpty
                    callback()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CourseDetails", "Error checking enrollment status", e)
                    isEnrolled = false
                    callback()
                }
        } catch (e: Exception) {
            android.util.Log.e("CourseDetails", "Exception checking enrollment", e)
            isEnrolled = false
            callback()
        }
    }

    companion object {
        const val EXTRA_COURSE_ID = "COURSE_ID"
    }
}