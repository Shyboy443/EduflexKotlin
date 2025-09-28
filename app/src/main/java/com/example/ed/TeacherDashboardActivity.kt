package com.example.ed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ed.ui.teacher.DashboardFragment
import com.example.ed.ui.teacher.CoursesFragment
import com.example.ed.ui.teacher.WeeklyContentFragment
import com.example.ed.ui.teacher.StudentsFragment
import com.example.ed.ui.teacher.TeacherSettingsFragment
import com.example.ed.utils.NetworkUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // UI Components
    private lateinit var ivNotification: ImageView
    private lateinit var tvProfileInitial: TextView
    private lateinit var tvActiveStudents: TextView
    private lateinit var tvCourseCompletion: TextView
    private lateinit var tvPendingAssignments: TextView
    private lateinit var tvUpcomingClasses: TextView
    private lateinit var tvViewAllTools: TextView
    private lateinit var tvViewAllCourses: TextView
    // private lateinit var btnGenerateQuiz: LinearLayout // removed legacy AI quiz entry point
    private lateinit var btnGradeAssignments: LinearLayout
    private lateinit var btnCreateCourse: LinearLayout
    private lateinit var btnManageCourses: LinearLayout
    private lateinit var btnManageContent: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme before setting content view
        ThemeManager.applyCurrentTheme(this)
        
        setContentView(R.layout.activity_teacher_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Load dashboard data
        loadDashboardData()
        
        // Set profile initial
        setProfileInitial()
    }

    private fun initializeViews() {
        ivNotification = findViewById(R.id.iv_notification)
        tvProfileInitial = findViewById(R.id.tv_profile_initial)
        tvActiveStudents = findViewById(R.id.tv_active_students)
        tvCourseCompletion = findViewById(R.id.tv_course_completion)
        tvPendingAssignments = findViewById(R.id.tv_pending_assignments)
        tvUpcomingClasses = findViewById(R.id.tv_upcoming_classes)
        tvViewAllTools = findViewById(R.id.tv_view_all_tools)
        tvViewAllCourses = findViewById(R.id.tv_view_all_courses)
        // btnGenerateQuiz = findViewById(R.id.btn_generate_quiz) // removed
        btnGradeAssignments = findViewById(R.id.btn_grade_assignments)
        btnCreateCourse = findViewById(R.id.btn_create_course)
        btnManageCourses = findViewById(R.id.btn_manage_courses)
        btnManageContent = findViewById(R.id.btn_manage_content)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        ivNotification.setOnClickListener {
            // TODO: Navigate to notifications
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show()
        }

        tvProfileInitial.setOnClickListener {
            // TODO: Navigate to profile
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
        }

        tvViewAllTools.setOnClickListener {
            // TODO: Navigate to all AI tools
            Toast.makeText(this, "View all tools clicked", Toast.LENGTH_SHORT).show()
        }

        // Removed legacy AI quiz generator navigation

        btnGradeAssignments.setOnClickListener {
            // TODO: Navigate to assignment grading
            Toast.makeText(this, "Grade Assignments clicked", Toast.LENGTH_SHORT).show()
        }

        tvViewAllCourses.setOnClickListener {
            val intent = Intent(this, CourseListActivity::class.java)
            startActivity(intent)
        }

        btnCreateCourse.setOnClickListener {
            val intent = Intent(this, CourseCreationActivity::class.java)
            startActivity(intent)
        }

        btnManageCourses.setOnClickListener {
            val intent = Intent(this, CourseListActivity::class.java)
            startActivity(intent)
        }

        btnManageContent.setOnClickListener {
            val intent = Intent(this, WeeklyContentActivity::class.java)
            startActivity(intent)
        }
        
        // Add live lectures button click listener
        findViewById<LinearLayout>(R.id.btn_live_lectures)?.setOnClickListener {
            Toast.makeText(this, "Live lectures management feature is not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDashboardData() {
        // Check network connectivity before loading data
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.w("TeacherDashboard", "No network connection, loading cached data")
            loadCachedDashboardData()
            showOfflineMessage()
            return
        }
        
        // Load statistics from Firebase
        loadActiveStudentsCount()
        loadCourseCompletionRate()
        loadPendingAssignmentsCount()
        loadUpcomingClassesCount()
    }

    private fun loadActiveStudentsCount() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("enrollments")
            .whereEqualTo("teacherId", currentUser.uid)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                val activeStudents = documents.size()
                tvActiveStudents.text = activeStudents.toString()
                cacheActiveStudentsCount(activeStudents)
            }
            .addOnFailureListener { exception ->
                Log.e("TeacherDashboard", "Error loading active students", exception)
                handleDashboardError("active_students", exception)
            }
    }

    private fun loadCourseCompletionRate() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("courses")
            .whereEqualTo("instructorId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvCourseCompletion.text = "0%"
                    cacheCourseCompletionRate(0)
                    return@addOnSuccessListener
                }
                
                var totalCourses = 0
                var completedCourses = 0
                
                for (document in documents) {
                    totalCourses++
                    val isCompleted = document.getBoolean("isCompleted") ?: false
                    if (isCompleted) completedCourses++
                }
                
                val completionRate = if (totalCourses > 0) {
                    (completedCourses * 100) / totalCourses
                } else 0
                
                tvCourseCompletion.text = "$completionRate%"
                cacheCourseCompletionRate(completionRate)
            }
            .addOnFailureListener { exception ->
                Log.e("TeacherDashboard", "Error loading course completion rate", exception)
                handleDashboardError("course_completion", exception)
            }
    }

    private fun loadPendingAssignmentsCount() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("assignments")
            .whereEqualTo("teacherId", currentUser.uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                val pendingCount = documents.size()
                tvPendingAssignments.text = pendingCount.toString()
                cachePendingAssignmentsCount(pendingCount)
            }
            .addOnFailureListener { exception ->
                Log.e("TeacherDashboard", "Error loading pending assignments", exception)
                handleDashboardError("pending_assignments", exception)
            }
    }

    private fun loadUpcomingClassesCount() {
        val currentUser = auth.currentUser ?: return
        
        val currentTime = System.currentTimeMillis()
        val nextWeek = currentTime + (7 * 24 * 60 * 60 * 1000) // 7 days from now
        
        db.collection("classes")
            .whereEqualTo("teacherId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                // Filter upcoming classes in code to avoid index requirement
                val upcomingClasses = documents.documents.filter { doc ->
                    val scheduledTime = doc.getLong("scheduledTime") ?: 0L
                    scheduledTime > currentTime && scheduledTime < nextWeek
                }
                val upcomingCount = upcomingClasses.size
                tvUpcomingClasses.text = upcomingCount.toString()
                cacheUpcomingClassesCount(upcomingCount)
            }
            .addOnFailureListener { exception ->
                Log.e("TeacherDashboard", "Error loading upcoming classes", exception)
                handleDashboardError("upcoming_classes", exception)
            }
    }

    private fun setProfileInitial() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Get user's name from Firestore and set initial
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("fullName") ?: "Teacher"
                        val initial = if (name.isNotEmpty()) name.first().uppercaseChar().toString() else "T"
                        tvProfileInitial.text = initial
                    } else {
                        tvProfileInitial.text = "T"
                    }
                }
                .addOnFailureListener {
                    tvProfileInitial.text = "T"
                }
        } else {
            tvProfileInitial.text = "T"
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    switchToFragment(DashboardFragment())
                    true
                }
                R.id.nav_courses -> {
                    switchToFragment(CoursesFragment())
                    true
                }
                R.id.nav_weekly_content -> {
                    switchToFragment(WeeklyContentFragment())
                    true
                }
                R.id.nav_students -> {
                    switchToFragment(StudentsFragment())
                    true
                }
                R.id.nav_settings -> {
                    switchToFragment(TeacherSettingsFragment())
                    true
                }
                else -> false
            }
        }
        
        // Set initial fragment
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            switchToFragment(DashboardFragment())
            bottomNavigation.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun switchToFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun switchToCoursesFragment() {
        switchToFragment(CoursesFragment())
        bottomNavigation.selectedItemId = R.id.nav_courses
    }

    override fun onBackPressed() {
        // Handle back press - maybe show confirmation dialog
        super.onBackPressed()
        finishAffinity() // Close the app completely
    }

    private fun showProfileMenu() {
        val options = arrayOf("Settings", "Logout")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profile")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Navigate to teacher settings
                        val intent = Intent(this, TeacherSettingsActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
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

    // Helper methods for caching and offline handling
    private fun loadCachedDashboardData() {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        val cacheTimestamp = sharedPrefs.getLong("cache_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        val cacheValidityPeriod = 24 * 60 * 60 * 1000L // 24 hours
        
        if (currentTime - cacheTimestamp < cacheValidityPeriod) {
            // Load cached data
            val activeStudents = sharedPrefs.getInt("active_students", 0)
            val courseCompletion = sharedPrefs.getInt("course_completion", 0)
            val pendingAssignments = sharedPrefs.getInt("pending_assignments", 0)
            val upcomingClasses = sharedPrefs.getInt("upcoming_classes", 0)
            
            tvActiveStudents.text = activeStudents.toString()
            tvCourseCompletion.text = "$courseCompletion%"
            tvPendingAssignments.text = pendingAssignments.toString()
            tvUpcomingClasses.text = upcomingClasses.toString()
            
            Log.i("TeacherDashboard", "Loaded cached dashboard data")
        } else {
            // Cache is expired, show default values
            tvActiveStudents.text = "0"
            tvCourseCompletion.text = "0%"
            tvPendingAssignments.text = "0"
            tvUpcomingClasses.text = "0"
            Log.w("TeacherDashboard", "Cache expired, showing default values")
        }
    }
    
    private fun cacheActiveStudentsCount(count: Int) {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("active_students", count)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun cacheCourseCompletionRate(rate: Int) {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("course_completion", rate)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun cachePendingAssignmentsCount(count: Int) {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("pending_assignments", count)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun cacheUpcomingClassesCount(count: Int) {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("upcoming_classes", count)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun handleDashboardError(dataType: String, exception: Exception) {
        when (exception) {
            is FirebaseFirestoreException -> {
                if (exception.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.w("TeacherDashboard", "Firestore unavailable for $dataType, loading cached data")
                    loadCachedDashboardData()
                    showOfflineMessage()
                } else {
                    Log.e("TeacherDashboard", "Firestore error for $dataType: ${exception.message}")
                    showErrorMessage("Error loading $dataType data")
                }
            }
            else -> {
                Log.e("TeacherDashboard", "Unexpected error for $dataType", exception)
                showErrorMessage("Error loading $dataType data")
            }
        }
    }
    
    private fun showOfflineMessage() {
        Toast.makeText(this, "You're offline. Showing cached data.", Toast.LENGTH_LONG).show()
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}