package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.CourseAdapter
import com.example.ed.models.Course
import com.example.ed.ui.LoadingStateView
import com.example.ed.utils.PerformanceOptimizer
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StudentCoursesActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    
    private var showPopular = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_courses)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if we should show popular courses
        showPopular = intent.getBooleanExtra("SHOW_POPULAR", false)

        initializeViews()
        setupToolbar()
        setupTabs()
        setupRecyclerView()
        loadCourses()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tab_layout)
        recyclerView = findViewById(R.id.recycler_view_courses)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (showPopular) "Popular Courses" else "My Courses"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupTabs() {
        if (!showPopular) {
            tabLayout.addTab(tabLayout.newTab().setText("Enrolled"))
            tabLayout.addTab(tabLayout.newTab().setText("Completed"))
            tabLayout.addTab(tabLayout.newTab().setText("Wishlist"))
        } else {
            tabLayout.addTab(tabLayout.newTab().setText("All"))
            tabLayout.addTab(tabLayout.newTab().setText("Mathematics"))
            tabLayout.addTab(tabLayout.newTab().setText("Science"))
            tabLayout.addTab(tabLayout.newTab().setText("Languages"))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadCourses()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        courseAdapter = CourseAdapter(
            courses = mutableListOf(),
            enrolledCourses = emptyList(), // No enrolled courses filtering needed here
            onCourseClick = { course ->
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            },
            onEditClick = null, // No edit functionality for students
            onMenuClick = null,  // No menu functionality for students
            showAsEnrolled = false, // Show as available courses with Enroll button
            isTeacherView = false
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@StudentCoursesActivity)
            adapter = courseAdapter
            
            // Apply performance optimizations
            optimizeRecyclerView(this, "course")
        }
    }

    private fun loadCourses() {
        val currentUser = auth.currentUser ?: return
        val selectedTab = tabLayout.selectedTabPosition

        if (showPopular) {
            loadPopularCourses(selectedTab)
        } else {
            loadStudentCourses(selectedTab)
        }
    }

    private fun loadStudentCourses(tabIndex: Int) {
        val currentUser = auth.currentUser ?: return
        
        when (tabIndex) {
            0 -> loadEnrolledCourses(currentUser.uid) // Enrolled
            1 -> loadCompletedCourses(currentUser.uid) // Completed
            2 -> loadWishlistCourses(currentUser.uid) // Wishlist
            else -> loadEnrolledCourses(currentUser.uid)
        }
    }
    
    private fun loadEnrolledCourses(userId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("enrolledCourses")
            .get()
            .addOnSuccessListener { documents ->
                val courseIds = documents.map { it.getString("courseId") ?: "" }.filter { it.isNotEmpty() }
                if (courseIds.isNotEmpty()) {
                    loadCoursesByIds(courseIds)
                } else {
                    courseAdapter.updateCourses(emptyList())
                    Toast.makeText(this, "No enrolled courses found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Implement retry mechanism for network failures
                val retryHandler = com.example.ed.utils.RetryHandler()
                retryHandler.retryCallback(
                    maxRetries = 3,
                    shouldRetry = com.example.ed.utils.RetryHandler.RetryConditions.temporaryErrors,
                    onRetry = { attempt, error ->
                        android.util.Log.w("StudentCoursesActivity", "Retrying enrolled courses load, attempt $attempt: ${error.message}")
                    },
                    onSuccess = { documents: com.google.firebase.firestore.QuerySnapshot ->
                        val courseIds = documents.map { it.getString("courseId") ?: "" }.filter { it.isNotEmpty() }
                        if (courseIds.isNotEmpty()) {
                            loadCoursesByIds(courseIds)
                        } else {
                            courseAdapter.updateCourses(emptyList())
                            Toast.makeText(this, "No enrolled courses found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("StudentCoursesActivity", "Failed to load enrolled courses after retries: ${error.message}")
                        Toast.makeText(this, "Failed to load enrolled courses: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                ) { onSuccess, onFailure ->
                    firestore.collection("users").document(userId).collection("enrolledCourses")
                        .get()
                        .addOnSuccessListener(onSuccess)
                        .addOnFailureListener(onFailure)
                }
            }
    }
    
    private fun loadCompletedCourses(userId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("completedCourses")
            .get()
            .addOnSuccessListener { documents ->
                val courseIds = documents.map { it.getString("courseId") ?: "" }.filter { it.isNotEmpty() }
                if (courseIds.isNotEmpty()) {
                    loadCoursesByIds(courseIds)
                } else {
                    courseAdapter.updateCourses(emptyList())
                    Toast.makeText(this, "No completed courses found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load completed courses: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadWishlistCourses(userId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("wishlist")
            .get()
            .addOnSuccessListener { documents ->
                val courseIds = documents.map { it.getString("courseId") ?: "" }.filter { it.isNotEmpty() }
                if (courseIds.isNotEmpty()) {
                    loadCoursesByIds(courseIds)
                } else {
                    courseAdapter.updateCourses(emptyList())
                    Toast.makeText(this, "No wishlist courses found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load wishlist courses: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadCoursesByIds(courseIds: List<String>) {
        if (courseIds.isEmpty()) {
            courseAdapter.updateCourses(emptyList())
            return
        }
        
        // Batch queries for better performance (Firestore 'in' query limit is 10)
        val batches = courseIds.chunked(10)
        val allCourses = mutableListOf<Course>()
        var completedBatches = 0
        
        batches.forEach { batch ->
            firestore.collection("courses")
                .whereIn("id", batch)
                .get()
                .addOnSuccessListener { documents ->
                    val courses = documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Course::class.java).copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("StudentCoursesActivity", "Error parsing course: ${e.message}")
                            null
                        }
                    }
                    allCourses.addAll(courses)
                    completedBatches++
                    
                    // Update UI when all batches are complete
                    if (completedBatches == batches.size) {
                        courseAdapter.updateCourses(allCourses)
                        if (allCourses.isEmpty()) {
                            Toast.makeText(this, "No courses found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("StudentCoursesActivity", "Failed to load course batch: ${exception.message}")
                    completedBatches++
                    
                    // Still update UI even if some batches fail
                    if (completedBatches == batches.size) {
                        courseAdapter.updateCourses(allCourses)
                        if (allCourses.isEmpty()) {
                            Toast.makeText(this, "Failed to load course details: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private fun loadPopularCourses(categoryIndex: Int) {
        val category = when (categoryIndex) {
            0 -> "All"
            1 -> "Mathematics"
            2 -> "Science"
            3 -> "Languages"
            else -> "All"
        }

        val query = if (category == "All") {
            firestore.collection("courses")
                .whereEqualTo("isPublished", true)
                .limit(20)
        } else {
            firestore.collection("courses")
                .whereEqualTo("isPublished", true)
                .whereEqualTo("category", category)
                .limit(20)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val courses = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Course::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                // Sort courses by enrolledStudents in descending order (most popular first)
                val sortedCourses = courses.sortedByDescending { it.enrolledStudents }
                
                courseAdapter.updateCourses(sortedCourses)
                if (sortedCourses.isEmpty()) {
                    Toast.makeText(this, "No courses found in $category", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load popular courses: ${exception.message}", Toast.LENGTH_SHORT).show()
                // Load sample courses as fallback
                loadSampleCourses()
            }
    }
    
    private fun loadSampleCourses() {
        val sampleCourses = listOf(
            Course(
                id = "sample1",
                title = "Introduction to Android Development",
                instructor = "John Doe",
                description = "Learn the basics of Android app development",
                category = "Technology",
                difficulty = "Beginner",
                rating = 4.5f,
                enrolledStudents = 1250,
                duration = "8 weeks",
                price = 99.99,
                isFree = false,
                thumbnailUrl = "",
                isPublished = true
            ),
            Course(
                id = "sample2",
                title = "Advanced Mathematics",
                instructor = "Jane Smith",
                description = "Master advanced mathematical concepts",
                category = "Mathematics",
                difficulty = "Advanced",
                rating = 4.8f,
                enrolledStudents = 890,
                duration = "12 weeks",
                price = 149.99,
                isFree = false,
                thumbnailUrl = "",
                isPublished = true
            ),
            Course(
                id = "sample3",
                title = "English Literature",
                instructor = "Mike Johnson",
                description = "Explore classic and modern literature",
                category = "Languages",
                difficulty = "Intermediate",
                rating = 4.3f,
                enrolledStudents = 650,
                duration = "10 weeks",
                price = 79.99,
                isFree = false,
                thumbnailUrl = "",
                isPublished = true
            )
        )
        courseAdapter.updateCourses(sampleCourses)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}