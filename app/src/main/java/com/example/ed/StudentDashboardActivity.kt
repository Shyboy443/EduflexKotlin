package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ed.databinding.ActivityStudentDashboardBinding
import com.example.ed.models.Course
import com.example.ed.adapters.CourseAdapter
import com.example.ed.services.DatabaseService
import com.example.ed.utils.NetworkUtils
import com.example.ed.utils.FirebaseConnectionChecker
import com.example.ed.utils.FirebaseDataSeeder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class StudentDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStudentDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var databaseService: DatabaseService
    
    // Coroutine jobs for lifecycle management
    private var analyticsJob: Job? = null
    private var enrolledCoursesJob: Job? = null
    private var popularCoursesJob: Job? = null
    
    // Student dashboard metrics
    private var enrolledCourses = 0
    private var completedCourses = 0
    private var averageGrade = 0.0
    private var studyStreak = 0
    private var pendingAssignments = 0
    
    // Course adapters
    private lateinit var continueCoursesAdapter: CourseAdapter
    private lateinit var popularCoursesAdapter: CourseAdapter
    private val continueCourses = mutableListOf<Course>()
    private val popularCourses = mutableListOf<Course>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        databaseService = DatabaseService.getInstance(this)

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupRecyclerViews()
        loadUserProfile()
        loadStudentAnalytics()
        loadEnrolledCourses()
        loadPopularCourses()
        checkForLiveLectures()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set up any initial UI configurations
        binding.progressBar.visibility = View.VISIBLE
        
        // Load analytics data
        loadAnalyticsData()
    }

    private fun setupRecyclerViews() {
        // Setup Continue Learning RecyclerView
        continueCoursesAdapter = CourseAdapter(
            courses = continueCourses,
            enrolledCourses = emptyList(), // No need to pass enrolled courses for filtering in enrolled view
            onCourseClick = { course ->
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            },
            onEditClick = null, // Hide edit for students
            onMenuClick = { course, view ->
                // Show enrolled course options (unenroll, progress, etc.)
            },
            showAsEnrolled = true, // Show as enrolled courses with View button
            isTeacherView = false // Student view
        )
        
        binding.rvContinueCourses.apply {
            layoutManager = LinearLayoutManager(this@StudentDashboardActivity)
            adapter = continueCoursesAdapter
        }
        
        // Setup Popular Courses RecyclerView
        popularCoursesAdapter = CourseAdapter(
            courses = popularCourses,
            enrolledCourses = continueCourses, // Pass enrolled courses for filtering
            onCourseClick = { course ->
                // Show course details with enrollment option
                showCourseEnrollmentDialog(course)
            },
            onEditClick = null, // Hide edit for students
            onMenuClick = { course, view ->
                // Show course options (enroll, preview, etc.)
                showCourseOptionsMenu(course, view)
            },
            showAsEnrolled = false, // Show as available courses with Enroll button
            isTeacherView = false // Student view
        )
        
        binding.rvPopularCourses.apply {
            layoutManager = LinearLayoutManager(this@StudentDashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularCoursesAdapter
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return
        
        try {
            // Load user profile image with better error handling
            val uri = currentUser.photoUrl
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_person) // Use a simpler placeholder
                    .error(R.drawable.ic_person) // Fallback if loading fails
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            } else {
                // Avoid calling Glide with a null model
                binding.ivProfilePicture.setImageResource(R.drawable.ic_person)
            }
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Error loading profile image", e)
            // Set a default drawable directly if Glide fails
            binding.ivProfilePicture.setImageResource(R.drawable.ic_person)
        }
        
        // Set welcome message
        val displayName = currentUser.displayName ?: "Student"
        binding.tvWelcomeMessage.text = "Welcome back, $displayName!"
    }

    private fun loadStudentAnalytics() {
        val currentUser = auth.currentUser ?: return
        
        // Use Firestore with offline persistence - let Firestore handle offline/online automatically
        firestore.collection("students")
            .document(currentUser.uid)
            .get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Load existing analytics from cache
                    enrolledCourses = document.getLong("enrolledCourses")?.toInt() ?: 0
                    completedCourses = document.getLong("completedCourses")?.toInt() ?: 0
                    averageGrade = document.getDouble("averageGrade") ?: 0.0
                    studyStreak = document.getLong("studyStreak")?.toInt() ?: 0
                    pendingAssignments = document.getLong("pendingAssignments")?.toInt() ?: 0
                    
                    updateAnalyticsUI(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
                    Log.d("StudentDashboard", "Analytics loaded from cache successfully")
                    
                    // Try to get fresh data from server in the background
                    loadAnalyticsFromServer(currentUser.uid)
                } else {
                    // No cached data, try to load from server or create initial data
                    loadAnalyticsFromServer(currentUser.uid)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("StudentDashboard", "Cache miss, trying server", exception)
                // If cache fails, try server
                loadAnalyticsFromServer(currentUser.uid)
            }
    }
    
    private fun loadAnalyticsFromServer(userId: String) {
        firestore.collection("students")
            .document(userId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Load existing analytics from server
                    enrolledCourses = document.getLong("enrolledCourses")?.toInt() ?: 0
                    completedCourses = document.getLong("completedCourses")?.toInt() ?: 0
                    averageGrade = document.getDouble("averageGrade") ?: 0.0
                    studyStreak = document.getLong("studyStreak")?.toInt() ?: 0
                    pendingAssignments = document.getLong("pendingAssignments")?.toInt() ?: 0
                    
                    // Cache the analytics data locally
                    cacheAnalyticsData(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
                    
                    updateAnalyticsUI(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
                    Log.d("StudentDashboard", "Analytics loaded from server successfully")
                } else {
                    // Create initial analytics document for new student
                    createInitialAnalytics(userId)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("StudentDashboard", "Server fetch failed, using fallback", exception)
                handleAnalyticsError(exception)
            }
    }
    
    private fun createInitialAnalytics(userId: String) {
        val initialAnalytics = hashMapOf(
            "enrolledCourses" to 0,
            "completedCourses" to 0,
            "averageGrade" to 0.0,
            "studyStreak" to 0,
            "pendingAssignments" to 0,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        firestore.collection("students")
            .document(userId)
            .set(initialAnalytics)
            .addOnSuccessListener {
                Log.d("StudentDashboard", "Initial analytics created successfully")
                cacheAnalyticsData(0, 0, 0.0, 0, 0)
                updateAnalyticsUI(0, 0, 0.0, 0, 0)
            }
            .addOnFailureListener { exception ->
                Log.e("StudentDashboard", "Error creating initial analytics", exception)
                // Fallback to cached data or defaults
                loadCachedAnalytics()
            }
    }
    
    private fun loadCachedAnalytics() {
        val prefs = getSharedPreferences("student_analytics", MODE_PRIVATE)
        val lastCached = prefs.getLong("lastCached", 0)
        
        // Check if we have cached data
        if (lastCached > 0) {
            enrolledCourses = prefs.getInt("enrolledCourses", 0)
            completedCourses = prefs.getInt("completedCourses", 0)
            averageGrade = prefs.getFloat("averageGrade", 0.0f).toDouble()
            studyStreak = prefs.getInt("studyStreak", 0)
            pendingAssignments = prefs.getInt("pendingAssignments", 0)
            
            updateAnalyticsUI(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
            Log.d("StudentDashboard", "Analytics loaded from cache")
            
            // Show offline indicator if data is older than 1 hour
            if ((System.currentTimeMillis() - lastCached) > 60 * 60 * 1000) {
                showOfflineMessage()
            }
        } else {
            // No cached data available, show defaults
            Log.d("StudentDashboard", "No cached analytics data available, showing defaults")
            updateAnalyticsUI(0, 0, 0.0, 0, 0)
            showOfflineMessage()
        }
    }
    
    private fun cacheAnalyticsData(enrolled: Int, completed: Int, grade: Double, streak: Int, pending: Int) {
        val prefs = getSharedPreferences("student_analytics", MODE_PRIVATE)
        prefs.edit().apply {
            putInt("enrolledCourses", enrolled)
            putInt("completedCourses", completed)
            putFloat("averageGrade", grade.toFloat())
            putInt("studyStreak", streak)
            putInt("pendingAssignments", pending)
            putLong("lastCached", System.currentTimeMillis())
            apply()
        }
    }
    
    private fun handleAnalyticsError(exception: Exception) {
        val errorMessage = exception.message?.lowercase() ?: ""
        
        // Check if it's an offline error
        if (errorMessage.contains("offline") || errorMessage.contains("unavailable") || 
            errorMessage.contains("network") || errorMessage.contains("connection")) {
            Log.w("StudentDashboard", "Offline error detected, loading cached data", exception)
            loadCachedAnalytics()
        } else {
            Log.e("StudentDashboard", "Analytics error: $errorMessage", exception)
            // Try to load cached data as fallback
            loadCachedAnalytics()
        }
    }
    
    private fun showOfflineMessage() {
        // Show a subtle offline indicator to the user
        Toast.makeText(this, "Showing cached data - you're currently offline", Toast.LENGTH_SHORT).show()
        Log.i("StudentDashboard", "Displaying cached analytics data (offline mode)")
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.w("StudentDashboard", "Error message shown: $message")
    }

    private fun loadEnrolledCourses() {
        val currentUser = auth.currentUser ?: return
        
        enrolledCoursesJob?.cancel()
        
        binding.progressBar.visibility = View.VISIBLE
        binding.cardEmptyStateContinue.visibility = View.GONE
        
        enrolledCoursesJob = lifecycleScope.launch {
            try {
                if (!isActive) return@launch
                
                databaseService.getEnrolledCoursesRealTime(currentUser.uid)
                    .collect { courses ->
                        if (!isActive) return@collect
                        
                        Log.d("StudentDashboard", "Received ${courses.size} enrolled courses")
                        
                        if (courses.isEmpty()) {
                            runOnUiThread {
                                binding.cardEmptyStateContinue.visibility = View.VISIBLE
                                binding.progressBar.visibility = View.GONE
                                continueCourses.clear()
                                continueCoursesAdapter.notifyDataSetChanged()
                            }
                            return@collect
                        }

                        val publishedCourses = courses.filter { it.isPublished }
                        val sortedCourses = publishedCourses.sortedByDescending { it.updatedAt }
                        
                        runOnUiThread {
                            try {
                                continueCourses.clear()
                                continueCourses.addAll(sortedCourses.take(5))
                                continueCoursesAdapter.notifyDataSetChanged()
                                
                                enrolledCourses = publishedCourses.size
                                
                                updateAnalyticsUI(
                                    enrolledCourses,
                                    completedCourses,
                                    averageGrade,
                                    studyStreak,
                                    pendingAssignments
                                )
                                
                                if (publishedCourses.isEmpty()) {
                                    binding.cardEmptyStateContinue.visibility = View.VISIBLE
                                } else {
                                    binding.cardEmptyStateContinue.visibility = View.GONE
                                }
                                
                                Log.d("StudentDashboard", "Successfully loaded ${publishedCourses.size} courses")
                            } catch (e: Exception) {
                                Log.e("StudentDashboard", "Error updating UI with courses", e)
                                showErrorMessage("Error updating course list")
                            } finally {
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                    }
            } catch (e: CancellationException) {
                Log.d("StudentDashboard", "Enrolled courses loading cancelled")
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading enrolled courses", e)
                runOnUiThread {
                    try {
                        binding.progressBar.visibility = View.GONE
                        binding.cardEmptyStateContinue.visibility = View.VISIBLE
                        
                        
                        Log.e("StudentDashboard", "Failed to load enrolled courses: ${e.message}", e)
                    } catch (uiError: Exception) {
                        Log.e("StudentDashboard", "Error in error handling", uiError)
                    }
                }
            }
        }
    }

    private fun loadPopularCourses() {
        popularCoursesJob?.cancel()
        
        binding.progressBarPopular.visibility = View.VISIBLE
        binding.cardEmptyStatePopular.visibility = View.GONE
        
        popularCoursesJob = lifecycleScope.launch {
            try {
                if (!isActive) return@launch
                
                databaseService.getCoursesRealTime()
                    .collect { enhancedCourses ->
                        if (!isActive) return@collect
                        
                        Log.d("StudentDashboard", "Received ${enhancedCourses.size} courses for popular section")

                        if (enhancedCourses.isEmpty()) {
                            Log.d("StudentDashboard", "No popular courses to process.")
                            runOnUiThread {
                                binding.progressBarPopular.visibility = View.GONE
                                binding.cardEmptyStatePopular.visibility = View.VISIBLE
                            }
                            return@collect
                        }
                        
                        val courses = withContext(Dispatchers.Default) {
                            enhancedCourses
                                .filter { 
                                    // More lenient filter - show courses that are published OR don't have the field set
                                    it.settings.isPublished || 
                                    (it.title.isNotEmpty() && it.instructor.name.isNotEmpty()) // Basic validation instead
                                }
                                .sortedByDescending { it.enrollmentInfo.totalEnrolled }
                                .take(6)
                                .map { enhancedCourse ->
                                    Course(
                                        id = enhancedCourse.id,
                                        title = enhancedCourse.title,
                                        instructor = enhancedCourse.instructor.name,
                                        description = enhancedCourse.description,
                                        category = enhancedCourse.category.name,
                                        difficulty = enhancedCourse.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                                        duration = "${enhancedCourse.courseStructure.totalDuration / 3600000}h",
                                        thumbnailUrl = enhancedCourse.thumbnailUrl,
                                        isPublished = enhancedCourse.settings.isPublished,
                                        createdAt = enhancedCourse.createdAt,
                                        updatedAt = enhancedCourse.updatedAt,
                                        enrolledStudents = enhancedCourse.enrollmentInfo.totalEnrolled,
                                        rating = enhancedCourse.enrollmentInfo.averageRating,
                                        teacherId = enhancedCourse.instructor.id,
                                        price = enhancedCourse.pricing.price,
                                        totalLessons = enhancedCourse.courseStructure.totalLessons,
                                        isFree = enhancedCourse.pricing.isFree
                                    )
                                }
                        }
                        
                        Log.d("StudentDashboard", "Processed ${courses.size} popular courses on background thread.")
                        Log.d("StudentDashboard", "Courses to display: ${courses.joinToString { it.title }}")

                        runOnUiThread {
                            try {
                                popularCourses.clear()
                                popularCourses.addAll(courses)
                                popularCoursesAdapter.notifyDataSetChanged()
                                
                                if (courses.isEmpty()) {
                                    binding.cardEmptyStatePopular.visibility = View.VISIBLE
                                } else {
                                    binding.cardEmptyStatePopular.visibility = View.GONE
                                }
                                
                                Log.d("StudentDashboard", "Successfully loaded ${courses.size} popular courses")
                            } catch (e: Exception) {
                                Log.e("StudentDashboard", "Error updating popular courses UI", e)
                                showErrorMessage("Error updating popular courses")
                            } finally {
                                binding.progressBarPopular.visibility = View.GONE
                            }
                        }
                    }
            } catch (e: CancellationException) {
                Log.d("StudentDashboard", "Popular courses loading cancelled")
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading popular courses", e)
                runOnUiThread {
                    binding.progressBarPopular.visibility = View.GONE
                    binding.cardEmptyStatePopular.visibility = View.VISIBLE


                    Toast.makeText(
                        this@StudentDashboardActivity,
                        "Error loading popular courses",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateAnalyticsUI(
        enrolledCount: Int,
        completedCount: Int,
        avgGrade: Double,
        streak: Int,
        pendingCount: Int
    ) {
        // Update analytics UI with provided values
        binding.tvEnrolledCourses.text = enrolledCount.toString()
        binding.tvCompletedCourses.text = completedCount.toString()
        binding.tvStudyStreak.text = streak.toString()
    }

    private fun setupClickListeners() {
        try {
            // Analytics cards click listeners
            binding.tvEnrolledCourses.setOnClickListener {
                startActivity(Intent(this, MyEnrolledCoursesActivity::class.java))
            }
            
            binding.tvCompletedCourses.setOnClickListener {
                startActivity(Intent(this, MyEnrolledCoursesActivity::class.java))
            }
            
            binding.tvStudyStreak.setOnClickListener {
                Toast.makeText(this, "Study Streak Details - Coming Soon", Toast.LENGTH_SHORT).show()
            }
            
            // Quick Action buttons
            binding.btnBrowseCourses.setOnClickListener {
                startActivity(Intent(this, CourseCatalogActivity::class.java))
            }
            
            binding.btnMyProgress.setOnClickListener {
                startActivity(Intent(this, MyEnrolledCoursesActivity::class.java))
            }
            
            // Search section
            binding.searchSection.setOnClickListener {
                startActivity(Intent(this, CourseCatalogActivity::class.java))
            }
            
            // Browse All Courses (from empty state)
            binding.btnBrowseAllCourses.setOnClickListener {
                startActivity(Intent(this, CourseCatalogActivity::class.java))
            }
            
            // View All buttons
            binding.tvViewAllContinue.setOnClickListener {
                startActivity(Intent(this, MyEnrolledCoursesActivity::class.java))
            }
            
            binding.tvViewAllPopular.setOnClickListener {
                startActivity(Intent(this, CourseCatalogActivity::class.java))
            }
            
            // Live lecture alert click listeners
            binding.liveLectureAlert.setOnClickListener {
                Toast.makeText(this, "Live lectures feature is not available", Toast.LENGTH_SHORT).show()
            }
            
            binding.btnJoinLiveNow.setOnClickListener {
                Toast.makeText(this, "Live lectures feature is not available", Toast.LENGTH_SHORT).show()
            }
        
            // Bottom navigation
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home
                        true
                    }
                    R.id.nav_live -> {
                        // Navigate to Live Lectures
                        Toast.makeText(this, "Live lectures feature is not available", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.nav_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            
            // Firebase connection test button (for debugging)
            binding.ivProfilePicture.setOnLongClickListener {
                testFirebaseConnection()
                true
            }
        
        // Set up profile picture tap handling for both single tap (profile menu) and double tap (data seeding)
        var lastClickTime = 0L
        var clickCount = 0
        binding.ivProfilePicture.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastClickTime < 500) { // Within 500ms of last click
                clickCount++
                if (clickCount == 2) { // Double tap detected
                    showDataSeedingDialog()
                    clickCount = 0 // Reset counter
                    return@setOnClickListener
                }
            } else {
                clickCount = 1 // First click or timeout, reset counter
            }
            
            lastClickTime = currentTime
            
            // Handle single tap with delay to allow for double tap detection
            binding.ivProfilePicture.postDelayed({
                if (clickCount == 1) { // Only single tap occurred
                    showProfileMenu()
                    clickCount = 0
                }
            }, 300) // 300ms delay to detect double tap
        }
        
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Error setting up click listeners", e)
        }
    }
    
    private fun loadAnalyticsData() {
        val currentUser = auth.currentUser ?: return
        
        // Cancel previous job if running
        analyticsJob?.cancel()
        
        analyticsJob = lifecycleScope.launch {
            try {
                if (!isActive) return@launch
                
                databaseService.getEnrolledCoursesRealTime(currentUser.uid)
                    .collect { enrolledCoursesList ->
                        if (!isActive) return@collect

                        // Fetch enrollments to compute completion status and average grade accurately
                        firestore.collection("enrollments")
                            .whereEqualTo("studentId", currentUser.uid)
                            .get()
                            .addOnSuccessListener { snap ->
                                val enrollments = snap.documents
                                val enrolledIds = enrollments.mapNotNull { it.getString("courseId") }
                                val completedCount = enrollments.count { (it.getString("status") ?: "").equals("completed", ignoreCase = true)
                                        || (it.getLong("progress") ?: 0L) >= 100L }
                                val avgGrade = enrollments.mapNotNull { it.getDouble("grade") }.takeIf { it.isNotEmpty() }?.average() ?: 0.0

                                // Filter continueCourses to only courses truly enrolled by the user
                                val filteredCourses = enrolledCoursesList.filter { enrolledIds.contains(it.id) }

                                runOnUiThread {
                                    try {
                                        continueCourses.clear()
                                        continueCourses.addAll(filteredCourses.take(5))
                                        continueCoursesAdapter.notifyDataSetChanged()
                                        
                                        updateAnalyticsUI(
                                            enrolledIds.size,
                                            completedCount,
                                            avgGrade,
                                            7,
                                            0
                                        )
                                    } catch (e: Exception) {
                                        Log.e("StudentDashboard", "Error updating analytics UI", e)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("StudentDashboard", "Failed to load enrollments for analytics", e)
                            }
                    }
            } catch (e: CancellationException) {
                Log.d("StudentDashboard", "Analytics data loading cancelled")
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading analytics data", e)
                if (isActive) {
                    // Show default values on error
                    updateAnalyticsUI(0, 0, 0.0, 0, 0)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh analytics data when returning to dashboard
        loadStudentAnalytics()
        loadEnrolledCourses()
        loadPopularCourses()
    }

    override fun onPause() {
        super.onPause()
        // Data loading jobs are not cancelled here to allow them to complete
        // even if the activity is briefly paused.
    }

    override fun onStop() {
        super.onStop()
        // Cancel ongoing coroutines when the activity is no longer visible
        analyticsJob?.cancel()
        enrolledCoursesJob?.cancel()
        popularCoursesJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure all jobs are cancelled
        analyticsJob?.cancel()
        enrolledCoursesJob?.cancel()
        popularCoursesJob?.cancel()
    }

    override fun onBackPressed() {
        // Prevent going back to login/signup
        finishAffinity()
    }

    private fun showProfileMenu() {
        val options = arrayOf("My Profile", "Settings", "Logout")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profile")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, StudentProfileActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                    2 -> {
                        showLogoutConfirmation()
                    }
                }
            }
            .show()
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        
        // Clear any cached data
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        
        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@StudentDashboardActivity, "Testing Firebase connection...", Toast.LENGTH_SHORT).show()
                
                val result = FirebaseConnectionChecker.checkFirebaseConnection(this@StudentDashboardActivity)
                
                val message = buildString {
                    appendLine("Firebase Connection Test Results:")
                    appendLine("Status: ${if (result.isConnected) "✓ Connected" else "✗ Disconnected"}")
                    appendLine()
                    result.details.forEach { detail ->
                        appendLine(detail)
                    }
                }
                
                Log.d("StudentDashboard", message)
                
                // Show a summary toast
                val summary = if (result.isConnected) {
                    "Firebase connection OK - Check logs for details"
                } else {
                    "Firebase connection issues detected - Check logs"
                }
                
                Toast.makeText(this@StudentDashboardActivity, summary, Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Firebase connection test failed", e)
                Toast.makeText(this@StudentDashboardActivity, "Connection test failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showDataSeedingDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Firebase Data Seeding")
        builder.setMessage("This will populate the Firebase database with sample data for development and testing purposes.\n\nWarning: This operation will add multiple records to your database.")
        
        builder.setPositiveButton("Seed Database") { _, _ ->
            seedFirebaseDatabase()
        }
        
        builder.setNegativeButton("Clear Sample Data") { _, _ ->
            clearSampleData()
        }
        
        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    private fun seedFirebaseDatabase() {
        lifecycleScope.launch {
            try {
                // Add Firebase diagnostic information
                val diagnosticInfo = getFirebaseDiagnostic()
                Log.d("StudentDashboard", "Firebase Diagnostic:\n$diagnosticInfo")
                
                Toast.makeText(this@StudentDashboardActivity, "Seeding database... This may take a moment.", Toast.LENGTH_LONG).show()
                
                val result = FirebaseDataSeeder.seedDatabase(this@StudentDashboardActivity)
                
                val message = buildString {
                    appendLine("Database Seeding Results:")
                    appendLine("Success: ${if (result.success) "✓ Yes" else "✗ No"}")
                    appendLine("Total Records: ${result.totalRecords}")
                    appendLine()
                    appendLine("Firebase Diagnostic:")
                    appendLine(diagnosticInfo)
                    appendLine()
                    result.details.forEach { detail ->
                        appendLine(detail)
                    }
                }
                
                Log.d("StudentDashboard", message)
                
                val summary = if (result.success) {
                    "✅ Database seeded successfully! ${result.totalRecords} records added."
                } else {
                    "❌ Database seeding failed. Check logs for details."
                }
                
                Toast.makeText(this@StudentDashboardActivity, summary, Toast.LENGTH_LONG).show()
                
                // Refresh the dashboard data
                if (result.success) {
                    loadStudentAnalytics()
                    loadEnrolledCourses()
                    loadPopularCourses()
                }
                
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Database seeding failed", e)
                Toast.makeText(this@StudentDashboardActivity, "Seeding failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun clearSampleData() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@StudentDashboardActivity, "Clearing sample data...", Toast.LENGTH_SHORT).show()
                
                val result = FirebaseDataSeeder.clearSampleData()
                
                val message = buildString {
                    appendLine("Sample Data Clearing Results:")
                    appendLine("Success: ${if (result.success) "✓ Yes" else "✗ No"}")
                    appendLine("Records Deleted: ${result.totalRecords}")
                    appendLine()
                    result.details.forEach { detail ->
                        appendLine(detail)
                    }
                }
                
                Log.d("StudentDashboard", message)
                
                val summary = if (result.success) {
                    "✅ Sample data cleared successfully! ${result.totalRecords} records deleted."
                } else {
                    "❌ Data clearing failed. Check logs for details."
                }
                
                Toast.makeText(this@StudentDashboardActivity, summary, Toast.LENGTH_LONG).show()
                
                // Refresh the dashboard data
                if (result.success) {
                    loadStudentAnalytics()
                    loadEnrolledCourses()
                    loadPopularCourses()
                }
                
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Sample data clearing failed", e)
                Toast.makeText(this@StudentDashboardActivity, "Clearing failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun getFirebaseDiagnostic(): String {
        return buildString {
            try {
                // Firebase App Information
                val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
                appendLine("🔥 Firebase App: ${firebaseApp.name}")
                appendLine("📱 Project ID: ${firebaseApp.options.projectId}")
                appendLine("🔑 Application ID: ${firebaseApp.options.applicationId}")
                
                // Authentication Status
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    appendLine("👤 User: Authenticated")
                    appendLine("   - UID: ${currentUser.uid}")
                    appendLine("   - Email: ${currentUser.email ?: "N/A"}")
                    appendLine("   - Display Name: ${currentUser.displayName ?: "N/A"}")
                    appendLine("   - Email Verified: ${currentUser.isEmailVerified}")
                    appendLine("   - Anonymous: ${currentUser.isAnonymous}")
                } else {
                    appendLine("👤 User: Not authenticated")
                }
                
                // Network Status
                val isNetworkAvailable = NetworkUtils.isNetworkAvailable(this@StudentDashboardActivity)
                appendLine("🌐 Network: ${if (isNetworkAvailable) "Available" else "Unavailable"}")
                
                // Firestore Settings
                appendLine("🗄️ Firestore Settings:")
                appendLine("   - Offline Persistence: Enabled")
                appendLine("   - Host: ${firestore.firestoreSettings.host}")
                appendLine("   - SSL Enabled: ${firestore.firestoreSettings.isSslEnabled}")
                appendLine("   - Persistence Enabled: ${firestore.firestoreSettings.isPersistenceEnabled}")
                
                // Google Services Configuration
                val googleServicesFile = "google-services.json"
                val hasGoogleServices = try {
                    assets.open(googleServicesFile)
                    true
                } catch (e: Exception) {
                    false
                }
                appendLine("📄 Google Services: ${if (hasGoogleServices) "Found" else "Missing"}")
                
                // Test Firestore Connection
                appendLine("🔗 Firestore Connection: Testing...")
                
            } catch (e: Exception) {
                appendLine("❌ Error generating diagnostic: ${e.message}")
            }
        }
    }
    
    // MARK: - Live Lecture Functionality
    
    private fun checkForLiveLectures() {
        val currentUser = auth.currentUser ?: return
        
        lifecycleScope.launch {
            try {
                // Get user's enrolled courses using Firestore directly
                val enrollments = firestore.collection("enrollments")
                    .whereEqualTo("studentId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                val courseIds = enrollments.documents.mapNotNull { it.getString("courseId") }
                
                if (courseIds.isEmpty()) {
                    return@launch
                }
                
                // Check for live lectures
                val currentTime = System.currentTimeMillis()
                val lectures = firestore.collection("live_lectures")
                    .whereIn("courseId", courseIds)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                    .toObjects(com.example.ed.models.LiveLecture::class.java)
                
                // Find currently live lectures
                val liveLectures = lectures.filter { lecture ->
                    val scheduledTimeMillis = lecture.scheduledTime?.toDate()?.time ?: 0L
                    val endTimeMillis = lecture.endTime?.toDate()?.time ?: 0L
                    val isLive = currentTime >= scheduledTimeMillis && 
                                currentTime <= endTimeMillis && 
                                lecture.isLive
                    isLive
                }
                
                // Find upcoming lectures (within next 30 minutes)
                val upcomingLectures = lectures.filter { lecture ->
                    val scheduledTimeMillis = lecture.scheduledTime?.toDate()?.time ?: 0L
                    val timeUntilStart = scheduledTimeMillis - currentTime
                    timeUntilStart > 0 && timeUntilStart <= 30 * 60 * 1000 // 30 minutes
                }
                
                runOnUiThread {
                    if (liveLectures.isNotEmpty()) {
                        // Show live lecture alert
                        val liveLecture = liveLectures.first()
                        showLiveLectureAlert(liveLecture, isLive = true)
                    } else if (upcomingLectures.isNotEmpty()) {
                        // Show upcoming lecture alert
                        val upcomingLecture = upcomingLectures.first()
                        showLiveLectureAlert(upcomingLecture, isLive = false)
                    } else {
                        // Hide live lecture alert
                        binding.liveLectureAlert.visibility = View.GONE
                    }
                }
                
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error checking for live lectures", e)
            }
        }
    }
    
    private fun showLiveLectureAlert(lecture: com.example.ed.models.LiveLecture, isLive: Boolean) {
        binding.apply {
            liveLectureAlert.visibility = View.VISIBLE
            
            if (isLive) {
                tvLiveLectureInfo.text = "${lecture.title} - ${lecture.courseName}"
                btnJoinLiveNow.text = "JOIN NOW"
                btnJoinLiveNow.setOnClickListener {
                    joinLiveLecture(lecture)
                }
            } else {
                val scheduledTimeMillis = lecture.scheduledTime?.toDate()?.time ?: 0L
                val timeUntilStartMillis = scheduledTimeMillis - System.currentTimeMillis()
                val minutes = timeUntilStartMillis / (60 * 1000)
                tvLiveLectureInfo.text = "${lecture.title} - Starts in ${minutes}m"
                btnJoinLiveNow.text = "VIEW"
                btnJoinLiveNow.setOnClickListener {
                    Toast.makeText(this@StudentDashboardActivity, "Live lectures feature is not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun joinLiveLecture(lecture: com.example.ed.models.LiveLecture) {
        if (lecture.meetingLink.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(lecture.meetingLink))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open meeting link", Toast.LENGTH_SHORT).show()
                // Fallback message since live lectures activity is no longer available
                Toast.makeText(this, "Live lectures feature is not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            // No direct link available and live lectures activity is no longer available
            Toast.makeText(this, "Live lectures feature is not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    // MARK: - Course Enrollment Functionality
    
    private fun showCourseEnrollmentDialog(course: Course) {
        try {
            // Check if already enrolled
            checkEnrollmentStatus(course) { isEnrolled ->
                if (isEnrolled) {
                    // Already enrolled - show course details
                    val intent = Intent(this, CourseDetailsActivity::class.java)
                    intent.putExtra("COURSE_ID", course.id)
                    startActivity(intent)
                } else {
                    // Show enrollment dialog
                    showEnrollmentConfirmationDialog(course)
                }
            }
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Error showing enrollment dialog", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEnrollmentConfirmationDialog(course: Course) {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Enroll in Course")
            .setMessage("📚 ${course.title}\n\n" +
                    "👨‍🏫 Instructor: ${course.instructor}\n" +
                    "📖 ${course.description}\n\n" +
                    "💰 ${if (course.isFree) "Free Course" else "Price: $${course.price}"}\n\n" +
                    "Would you like to enroll in this course?")
            .setPositiveButton("Enroll Now") { _, _ ->
                enrollInCourse(course)
            }
            .setNeutralButton("Preview") { _, _ ->
                // Show course details without enrolling
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun enrollInCourse(course: Course) {
        try {
            val currentUser = auth.currentUser ?: return
            
            Toast.makeText(this, "Enrolling in ${course.title}...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                try {
                    val enrollmentData = hashMapOf(
                        "studentId" to currentUser.uid,
                        "courseId" to course.id,
                        "courseName" to course.title,
                        "instructor" to course.instructor,
                        "enrolledAt" to System.currentTimeMillis(),
                        "progress" to 0,
                        "completedLessons" to 0,
                        "totalLessons" to course.totalLessons,
                        "isActive" to true,
                        "lastAccessedAt" to System.currentTimeMillis()
                    )
                    
                    firestore.collection("enrollments")
                        .add(enrollmentData)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(this@StudentDashboardActivity, 
                                "✅ Successfully enrolled in ${course.title}!", Toast.LENGTH_LONG).show()
                            
                            Log.d("StudentDashboard", "Enrolled in course: ${course.title}")
                            
                            // Refresh the dashboard to show updated data
                            loadEnrolledCourses()
                            loadStudentAnalytics()
                            
                            // Navigate to course details
                            val intent = Intent(this@StudentDashboardActivity, CourseDetailsActivity::class.java)
                            intent.putExtra("COURSE_ID", course.id)
                            startActivity(intent)
                        }
                        .addOnFailureListener { e ->
                            Log.e("StudentDashboard", "Error enrolling in course", e)
                            Toast.makeText(this@StudentDashboardActivity, 
                                "❌ Failed to enroll: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } catch (e: Exception) {
                    Log.e("StudentDashboard", "Exception during enrollment", e)
                    Toast.makeText(this@StudentDashboardActivity, 
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Exception in enrollInCourse", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkEnrollmentStatus(course: Course, callback: (Boolean) -> Unit) {
        try {
            val currentUser = auth.currentUser ?: return callback(false)
            
            firestore.collection("enrollments")
                .whereEqualTo("studentId", currentUser.uid)
                .whereEqualTo("courseId", course.id)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener { documents ->
                    callback(!documents.isEmpty)
                }
                .addOnFailureListener { e ->
                    Log.e("StudentDashboard", "Error checking enrollment status", e)
                    callback(false)
                }
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Exception checking enrollment", e)
            callback(false)
        }
    }
    
    private fun showCourseOptionsMenu(course: Course, view: View) {
        try {
            val options = arrayOf(
                "📖 View Details",
                "📝 Enroll in Course",
                "👀 Preview Content",
                "📤 Share Course"
            )
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(course.title)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // View Details
                            val intent = Intent(this, CourseDetailsActivity::class.java)
                            intent.putExtra("COURSE_ID", course.id)
                            startActivity(intent)
                        }
                        1 -> {
                            // Enroll in Course
                            showCourseEnrollmentDialog(course)
                        }
                        2 -> {
                            // Preview Content
                            Toast.makeText(this, "Preview feature coming soon", Toast.LENGTH_SHORT).show()
                        }
                        3 -> {
                            // Share Course
                            shareCourse(course)
                        }
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Error showing course options", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareCourse(course: Course) {
        try {
            val shareText = "Check out this course: ${course.title}\n" +
                    "Instructor: ${course.instructor}\n" +
                    "${course.description}\n\n" +
                    "Join me on EduFlex!"
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Course"))
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Error sharing course", e)
            Toast.makeText(this, "Error sharing course", Toast.LENGTH_SHORT).show()
        }
    }
    
}