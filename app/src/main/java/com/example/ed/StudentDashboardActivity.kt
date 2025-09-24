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

class StudentDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStudentDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var databaseService: DatabaseService
    
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
            onCourseClick = { course ->
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            },
            onEditClick = { course -> 
                // Students can't edit courses
            },
            onMenuClick = { course, view ->
                // Show course options
            }
        )
        
        binding.rvContinueCourses.apply {
            layoutManager = LinearLayoutManager(this@StudentDashboardActivity)
            adapter = continueCoursesAdapter
        }
        
        // Setup Popular Courses RecyclerView
        popularCoursesAdapter = CourseAdapter(
            courses = popularCourses,
            onCourseClick = { course ->
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            },
            onEditClick = { course -> 
                // Students can't edit courses
            },
            onMenuClick = { course, view ->
                // Show course options
            }
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
            Glide.with(this)
                .load(currentUser.photoUrl)
                .placeholder(R.drawable.ic_person) // Use a simpler placeholder
                .error(R.drawable.ic_person) // Fallback if loading fails
                .circleCrop()
                .into(binding.ivProfilePicture)
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
        
        lifecycleScope.launch {
            try {
                databaseService.getEnrolledCoursesRealTime(currentUser.uid)
                    .collect { courses ->
                        continueCourses.clear()
                        continueCourses.addAll(courses.take(5)) // Show only 5 most recent
                        continueCoursesAdapter.notifyDataSetChanged()
                        
                        // Update enrolled courses count
                        enrolledCourses = courses.size
                        // Analytics will be updated by loadAnalyticsData()
                        
                        // Hide loading indicator
                        binding.progressBar.visibility = View.GONE
                        
                        // Show/hide empty state
                        if (courses.isEmpty()) {
                            binding.tvEmptyStateContinue.visibility = View.VISIBLE
                            binding.tvEmptyStateContinue.text = "No enrolled courses yet. Browse our course catalog to get started!"
                        } else {
                            binding.tvEmptyStateContinue.visibility = View.GONE
                        }
                    }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading enrolled courses", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@StudentDashboardActivity, "Error loading courses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPopularCourses() {
        lifecycleScope.launch {
            try {
                databaseService.getCoursesRealTime()
                    .collect { enhancedCourses ->
                        // Convert EnhancedCourse to Course and filter published courses
                        val courses = enhancedCourses
                            .filter { it.settings.isPublished }
                            .sortedByDescending { it.enrollmentInfo.totalEnrolled }
                            .take(6) // Show top 6 popular courses
                            .map { enhancedCourse ->
                                Course(
                                    id = enhancedCourse.id,
                                    title = enhancedCourse.title,
                                    instructor = enhancedCourse.instructor.name,
                                    description = enhancedCourse.description,
                                    category = enhancedCourse.category.name,
                                    difficulty = enhancedCourse.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                                    duration = "${enhancedCourse.courseStructure.totalDuration / 3600000}h", // Convert ms to hours
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
                        
                        popularCourses.clear()
                        popularCourses.addAll(courses)
                        popularCoursesAdapter.notifyDataSetChanged()
                        
                        // Show/hide empty state for popular courses
                        if (courses.isEmpty()) {
                            binding.tvEmptyStatePopular.visibility = View.VISIBLE
                            binding.tvEmptyStatePopular.text = "No courses available at the moment."
                        } else {
                            binding.tvEmptyStatePopular.visibility = View.GONE
                        }
                    }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading popular courses", e)
                Toast.makeText(this@StudentDashboardActivity, "Error loading popular courses", Toast.LENGTH_SHORT).show()
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
        binding.tvAverageGrade.text = String.format("%.1f", avgGrade)
        binding.tvStudyStreak.text = streak.toString()
        binding.tvPendingAssignments.text = pendingCount.toString()
    }

    private fun setupClickListeners() {
        // Analytics cards click listeners
        binding.tvEnrolledCourses.setOnClickListener {
            startActivity(Intent(this, MyEnrolledCoursesActivity::class.java))
        }
        
        binding.tvCompletedCourses.setOnClickListener {
            // Navigate to completed courses
            val intent = Intent(this, MyEnrolledCoursesActivity::class.java)
            intent.putExtra("filter", "completed")
            startActivity(intent)
        }
        
        binding.tvAverageGrade.setOnClickListener {
            // Navigate to grades/performance
            startActivity(Intent(this, GradingSystemActivity::class.java))
        }
        
        binding.tvStudyStreak.setOnClickListener {
            // Navigate to study streak details
            startActivity(Intent(this, StudentEngagementActivity::class.java))
        }
        
        // Focus Timer click
        binding.btnFocusTimer.setOnClickListener {
            Toast.makeText(this, "Focus Timer feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Downloads click
        binding.btnDownloads.setOnClickListener {
            Toast.makeText(this, "Downloads feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Search functionality
        binding.searchSection.setOnClickListener {
            startActivity(Intent(this, CourseListActivity::class.java))
        }
        
        // View all buttons
        binding.tvViewAllContinue.setOnClickListener {
            startActivity(Intent(this, MyEnrolledCoursesActivity::class.java))
        }
        
        binding.tvViewAllPopular.setOnClickListener {
            startActivity(Intent(this, CourseCatalogActivity::class.java))
        }
        
        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_courses -> {
                    startActivity(Intent(this, CourseListActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
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
    }
    
    private fun loadAnalyticsData() {
        val currentUser = auth.currentUser ?: return
        
        lifecycleScope.launch {
            try {
                databaseService.getEnrolledCoursesRealTime(currentUser.uid)
                    .collect { enrolledCourses ->
                        // Calculate completed courses (assuming we have completion data)
                        val completedCourses = enrolledCourses.filter { course ->
                            // For now, we'll use a simple heuristic - courses with high rating might be completed
                            course.rating >= 4.0
                        }
                        
                        // Calculate average grade from course ratings
                        val totalGrades = enrolledCourses.mapNotNull { it.rating }.sum()
                        val averageGrade = if (enrolledCourses.isNotEmpty()) totalGrades.toDouble() / enrolledCourses.size else 0.0
                        
                        // Calculate study streak (mock data for now)
                        val studyStreak = 7
                        
                        // Calculate pending assignments (mock data for now)
                        val pendingAssignments = 3
                        
                        updateAnalyticsUI(
                            enrolledCourses.size,
                            completedCourses.size,
                            averageGrade,
                            studyStreak,
                            pendingAssignments
                        )
                    }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading analytics data", e)
                // Show default values on error
                updateAnalyticsUI(0, 0, 0.0, 0, 0)
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
}