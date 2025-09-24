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

class CourseDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var courseSectionAdapter: CourseSectionAdapter
    
    private var currentCourse: Course? = null
    private var userRole: String = "Student" // Default to Student
    private var isBookmarked: Boolean = false

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
                    startActivity(Intent(this, StudentDashboardActivity::class.java))
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

        // In a real app, you'd load from Firebase
        firestore.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Parse course from Firestore
                    // For now, load sample data
                    loadSampleCourse()
                } else {
                    loadSampleCourse()
                }
            }
            .addOnFailureListener {
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

    private fun updateUI() {
        currentCourse?.let { course ->
            binding.tvCourseTitle.text = course.title
            binding.tvInstructor.text = "By ${course.instructor}"
            binding.tvCourseDescription.text = course.description
            binding.progressBar.progress = course.progress
            binding.tvProgressPercentage.text = "${course.progress}%"
            
            // Update bookmark status
            updateBookmarkUI()
            
            // Update course content
            courseSectionAdapter.updateSections(course.courseContent)
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
                // Student functionality - start lesson
                // Find the next incomplete lesson
                currentCourse?.let { course ->
                    val nextLesson = findNextIncompleteLesson(course)
                    if (nextLesson != null) {
                        startLesson(nextLesson.first, nextLesson.second)
                    } else {
                        Toast.makeText(this, "All lessons completed!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnTakeQuiz.setOnClickListener {
            if (userRole == "Teacher") {
                // Teacher functionality - edit content
                Toast.makeText(this, "Content editing coming soon", Toast.LENGTH_SHORT).show()
            } else {
                // Student functionality - take quiz
                Toast.makeText(this, "Quiz feature coming soon", Toast.LENGTH_SHORT).show()
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

    private fun onLessonClick(sectionId: String, lessonId: String) {
        // Check if user has access (teachers can access all, students need to follow order)
        if (userRole == "Teacher" || canAccessLesson(sectionId, lessonId)) {
            startLesson(sectionId, lessonId)
        } else {
            Toast.makeText(this, "Complete previous lessons first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canAccessLesson(sectionId: String, lessonId: String): Boolean {
        // Students can only access lessons in order
        currentCourse?.let { course ->
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
        // In a real app, you'd navigate to a lesson activity
        Toast.makeText(this, "Starting lesson: $lessonId", Toast.LENGTH_SHORT).show()
        
        // For demonstration, mark lesson as completed after a delay
        // In a real app, this would happen when the lesson is actually completed
    }

    companion object {
        const val EXTRA_COURSE_ID = "COURSE_ID"
    }
}