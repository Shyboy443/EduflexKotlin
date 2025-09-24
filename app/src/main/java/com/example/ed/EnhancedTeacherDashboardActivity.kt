package com.example.ed

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.RecentActivityAdapter
import com.example.ed.models.*
import com.example.ed.utils.SecurityUtils
import com.example.ed.utils.NetworkUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

class EnhancedTeacherDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // UI Components - Header
    private lateinit var ivNotification: ImageView
    private lateinit var tvProfileInitial: TextView
    private lateinit var tvWelcomeMessage: TextView
    
    // UI Components - Quick Stats Cards
    private lateinit var cardTotalStudents: CardView
    private lateinit var cardActiveStudents: CardView
    private lateinit var cardCourseCompletion: CardView
    private lateinit var cardMonthlyRevenue: CardView
    
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvActiveStudents: TextView
    private lateinit var tvCourseCompletion: TextView
    private lateinit var tvMonthlyRevenue: TextView
    
    // UI Components - Charts and Analytics
    private lateinit var lineChartEngagement: LineChart
    private lateinit var pieChartCourseDistribution: PieChart
    
    // UI Components - Quick Actions
    private lateinit var btnCreateCourse: LinearLayout
    private lateinit var btnGenerateQuiz: LinearLayout
    private lateinit var btnViewAnalytics: LinearLayout
    private lateinit var btnManageStudents: LinearLayout
    private lateinit var btnGradeAssignments: LinearLayout
    private lateinit var btnScheduleClass: LinearLayout
    private lateinit var btnUploadMaterials: LinearLayout
    private lateinit var btnCreateAssignment: LinearLayout
    
    // UI Components - Recent Activity
    private lateinit var rvRecentActivity: RecyclerView
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    
    // UI Components - Notifications Panel
    private lateinit var llNotificationsPanel: LinearLayout
    private lateinit var tvPendingAssignments: TextView
    private lateinit var tvUpcomingDeadlines: TextView
    private lateinit var tvNewMessages: TextView
    
    // UI Components - Performance Insights
    private lateinit var tvTopPerformingCourse: TextView
    private lateinit var tvAverageQuizScore: TextView
    private lateinit var tvStudentEngagementRate: TextView
    
    // Bottom Navigation
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Floating Action Button for Quick Course Creation
    private lateinit var fabQuickAction: FloatingActionButton
    
    // Data
    private var dashboardMetrics = DashboardMetrics()
    private var teacherAnalytics = TeacherAnalytics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enhanced_teacher_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Security check: Verify teacher permissions
        lifecycleScope.launch {
            if (!SecurityUtils.canAccessTeacherFeatures()) {
                SecurityUtils.logSecurityEvent(
                    "unauthorized_teacher_dashboard_access",
                    auth.currentUser?.uid,
                    mapOf("activity" to "EnhancedTeacherDashboardActivity")
                )
                Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            
            // Initialize views and setup UI after security check passes
            initializeViews()
            setupClickListeners()
            setupBottomNavigation()
            setupCharts()
            setupRecentActivityRecyclerView()
            setupNotifications()
            setProfileInformation()
            loadDashboardData()
        }
    }

    private fun initializeViews() {
        // Header components
        ivNotification = findViewById(R.id.iv_notification)
        tvProfileInitial = findViewById(R.id.tv_profile_initial)
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message)
        
        // Stats cards
        cardTotalStudents = findViewById(R.id.card_total_students)
        cardActiveStudents = findViewById(R.id.card_active_students)
        cardCourseCompletion = findViewById(R.id.card_course_completion)
        cardMonthlyRevenue = findViewById(R.id.card_monthly_revenue)
        
        tvTotalStudents = findViewById(R.id.tv_total_students)
        tvActiveStudents = findViewById(R.id.tv_active_students)
        tvCourseCompletion = findViewById(R.id.tv_course_completion)
        tvMonthlyRevenue = findViewById(R.id.tv_monthly_revenue)
        
        // Charts
        lineChartEngagement = findViewById(R.id.line_chart_engagement)
        pieChartCourseDistribution = findViewById(R.id.pie_chart_course_distribution)
        
        // Quick actions
        btnCreateCourse = findViewById(R.id.btn_create_course)
        btnGenerateQuiz = findViewById(R.id.btn_generate_quiz)
        btnViewAnalytics = findViewById(R.id.btn_view_analytics)
        btnManageStudents = findViewById(R.id.btn_manage_students)
        btnGradeAssignments = findViewById(R.id.btn_grade_assignments)
        btnScheduleClass = findViewById(R.id.btn_schedule_class)
        btnUploadMaterials = findViewById(R.id.btn_upload_materials)
        btnCreateAssignment = findViewById(R.id.btn_create_assignment)
        
        // Recent activity
        rvRecentActivity = findViewById(R.id.rv_recent_activity)
        setupRecentActivityRecyclerView()
        
        // Notifications panel
        llNotificationsPanel = findViewById(R.id.ll_notifications_panel)
        tvPendingAssignments = findViewById(R.id.tv_pending_assignments)
        tvUpcomingDeadlines = findViewById(R.id.tv_upcoming_deadlines)
        tvNewMessages = findViewById(R.id.tv_new_messages)
        
        // Performance insights
        tvTopPerformingCourse = findViewById(R.id.tv_top_performing_course)
        tvAverageQuizScore = findViewById(R.id.tv_average_quiz_score)
        tvStudentEngagementRate = findViewById(R.id.tv_student_engagement_rate)
        
        // Bottom navigation and FAB
        bottomNavigation = findViewById(R.id.bottom_navigation)
        fabQuickAction = findViewById(R.id.fab_quick_action)
    }

    private fun setupClickListeners() {
        // Quick action buttons
        btnCreateCourse.setOnClickListener {
            lifecycleScope.launch {
                // Security check and rate limiting
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    SecurityUtils.logSecurityEvent(
                        "unauthorized_course_creation_attempt",
                        auth.currentUser?.uid,
                        mapOf("source" to "teacher_dashboard")
                    )
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (!SecurityUtils.isOperationAllowed("course_creation_navigation", 2000)) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Please wait before creating another course", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                SecurityUtils.logSecurityEvent(
                    "course_creation_navigation",
                    auth.currentUser?.uid,
                    mapOf("source" to "teacher_dashboard")
                )
                
                startActivity(Intent(this@EnhancedTeacherDashboardActivity, CourseCreationActivity::class.java))
            }
        }
        
        btnGenerateQuiz.setOnClickListener {
            lifecycleScope.launch {
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    SecurityUtils.logSecurityEvent(
                        "unauthorized_quiz_generation_attempt",
                        auth.currentUser?.uid,
                        mapOf("source" to "teacher_dashboard")
                    )
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                startActivity(Intent(this@EnhancedTeacherDashboardActivity, AIQuizGenerationActivity::class.java))
            }
        }

        btnViewAnalytics.setOnClickListener {
            lifecycleScope.launch {
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // TODO: Create AnalyticsActivity
                Toast.makeText(this@EnhancedTeacherDashboardActivity, "Analytics coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        btnManageStudents.setOnClickListener {
            lifecycleScope.launch {
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // TODO: Create StudentManagementActivity
                Toast.makeText(this@EnhancedTeacherDashboardActivity, "Student Management coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        btnGradeAssignments.setOnClickListener {
            lifecycleScope.launch {
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // TODO: Create GradingActivity
                Toast.makeText(this@EnhancedTeacherDashboardActivity, "Grading coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        btnScheduleClass.setOnClickListener {
            lifecycleScope.launch {
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // TODO: Create ClassSchedulingActivity
                Toast.makeText(this@EnhancedTeacherDashboardActivity, "Class Scheduling coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        btnUploadMaterials.setOnClickListener {
            lifecycleScope.launch {
                // Security check and rate limiting
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    SecurityUtils.logSecurityEvent(
                        "unauthorized_material_upload_attempt",
                        auth.currentUser?.uid,
                        mapOf("source" to "teacher_dashboard")
                    )
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (!SecurityUtils.isOperationAllowed("material_upload_navigation", 2000)) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Please wait before uploading materials", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                SecurityUtils.logSecurityEvent(
                    "material_upload_navigation",
                    auth.currentUser?.uid,
                    mapOf("source" to "teacher_dashboard")
                )
                
                startActivity(Intent(this@EnhancedTeacherDashboardActivity, com.example.educationalapp.MaterialUploadActivity::class.java))
            }
        }

        btnCreateAssignment.setOnClickListener {
            lifecycleScope.launch {
                // Security check and rate limiting
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    SecurityUtils.logSecurityEvent(
                        "unauthorized_assignment_creation_attempt",
                        auth.currentUser?.uid,
                        mapOf("source" to "teacher_dashboard")
                    )
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (!SecurityUtils.isOperationAllowed("assignment_creation_navigation", 2000)) {
                    Toast.makeText(this@EnhancedTeacherDashboardActivity, "Please wait before creating assignments", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                SecurityUtils.logSecurityEvent(
                    "assignment_creation_navigation",
                    auth.currentUser?.uid,
                    mapOf("source" to "teacher_dashboard")
                )
                
                startActivity(Intent(this@EnhancedTeacherDashboardActivity, com.example.educationalapp.AssignmentCreationActivity::class.java))
            }
        }
        
        // Stats cards click listeners for detailed views
        cardTotalStudents.setOnClickListener {
            // TODO: Create StudentManagementActivity
            Toast.makeText(this, "Student Management coming soon", Toast.LENGTH_SHORT).show()
        }

        cardActiveStudents.setOnClickListener {
            // TODO: Create StudentEngagementActivity
            Toast.makeText(this, "Student Engagement coming soon", Toast.LENGTH_SHORT).show()
        }

        cardCourseCompletion.setOnClickListener {
            // TODO: Create CourseAnalyticsActivity
            Toast.makeText(this, "Course Analytics coming soon", Toast.LENGTH_SHORT).show()
        }

        cardMonthlyRevenue.setOnClickListener {
            // TODO: Create RevenueAnalyticsActivity
            Toast.makeText(this, "Revenue Analytics coming soon", Toast.LENGTH_SHORT).show()
        }

        // Notification icon
        ivNotification.setOnClickListener {
            // TODO: Create NotificationsActivity
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        // FAB for quick course creation
        fabQuickAction.setOnClickListener {
            // TODO: Create QuickCourseCreationActivity
            Toast.makeText(this, "Quick Course Creation coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.nav_courses -> {
                    startActivity(Intent(this, CourseCreationActivity::class.java))
                    true
                }
                R.id.nav_students -> {
                    // TODO: Create StudentManagementActivity
                    Toast.makeText(this, "Student Management coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_analytics -> {
                    // TODO: Create AnalyticsActivity
                    Toast.makeText(this, "Analytics coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    showProfileMenu()
                    true
                }
                else -> false
            }
        }
        
        // Set dashboard as selected
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    private fun setupCharts() {
        // Setup engagement line chart
        lineChartEngagement.description.isEnabled = false
        lineChartEngagement.setTouchEnabled(true)
        lineChartEngagement.isDragEnabled = true
        lineChartEngagement.setScaleEnabled(true)
        lineChartEngagement.setPinchZoom(true)
        
        // Setup course distribution pie chart
        pieChartCourseDistribution.description.isEnabled = false
        pieChartCourseDistribution.setUsePercentValues(true)
        pieChartCourseDistribution.setEntryLabelTextSize(12f)
        pieChartCourseDistribution.setEntryLabelColor(android.graphics.Color.BLACK)
    }

    private fun setupRecentActivityRecyclerView() {
        recentActivityAdapter = RecentActivityAdapter(emptyList()) { activity ->
            // Handle activity item click
            handleActivityClick(activity)
        }
        rvRecentActivity.layoutManager = LinearLayoutManager(this)
        rvRecentActivity.adapter = recentActivityAdapter
    }

    private fun loadDashboardData() {
        // Load dashboard metrics from Firebase
        loadDashboardMetrics()
        loadTeacherAnalytics()
        loadRecentActivity()
        updateCharts()
    }

    private fun loadDashboardMetrics() {
        val currentUser = auth.currentUser ?: return
        
        // Check network connectivity before making Firestore request
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.w("TeacherDashboard", "No network connection - loading cached dashboard data")
            loadCachedDashboardMetrics()
            return
        }
        
        db.collection("teacherAnalytics")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Parse dashboard metrics from Firestore
                    parseDashboardMetrics(document)
                    cacheDashboardMetrics()
                    updateDashboardUI()
                } else {
                    // Create initial analytics document
                    createInitialAnalytics()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TeacherDashboard", "Error loading dashboard metrics", exception)
                handleDashboardError(exception)
            }
    }

    private fun updateDashboardUI() {
        // Update stats cards
        tvTotalStudents.text = dashboardMetrics.totalStudents.toString()
        tvActiveStudents.text = dashboardMetrics.activeStudents.toString()
        tvCourseCompletion.text = "${dashboardMetrics.courseCompletionRate.toInt()}%"
        tvMonthlyRevenue.text = "$${String.format("%.2f", dashboardMetrics.totalRevenue)}"
        
        // Update performance insights
        tvTopPerformingCourse.text = dashboardMetrics.topPerformingCourse
        tvAverageQuizScore.text = "${dashboardMetrics.averageQuizScore.toInt()}%"
        tvStudentEngagementRate.text = "${(dashboardMetrics.activeStudents.toFloat() / 
            dashboardMetrics.totalStudents * 100).toInt()}%"
        
        // Update notifications - Load from Firebase instead of placeholders
        tvPendingAssignments.text = dashboardMetrics.pendingAssignments.toString()
        loadUpcomingDeadlines()
        loadNewMessages()
    }

    private fun loadUpcomingDeadlines() {
        val currentUser = auth.currentUser ?: return
        val currentTime = System.currentTimeMillis()
        val nextWeek = currentTime + (7 * 24 * 60 * 60 * 1000)
        
        db.collection("assignments")
            .whereEqualTo("teacherId", currentUser.uid)
            .whereGreaterThan("dueDate", currentTime)
            .whereLessThan("dueDate", nextWeek)
            .get()
            .addOnSuccessListener { documents ->
                tvUpcomingDeadlines.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvUpcomingDeadlines.text = "0"
            }
    }

    private fun loadNewMessages() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("messages")
            .whereEqualTo("recipientId", currentUser.uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                tvNewMessages.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvNewMessages.text = "0"
            }
    }

    private fun updateCharts() {
        updateEngagementChart()
        updateCourseDistributionChart()
    }

    private fun updateEngagementChart() {
        val entries = ArrayList<Entry>()
        // Add sample data - in real implementation, use actual analytics data
        for (i in 0..6) {
            entries.add(Entry(i.toFloat(), (Math.random() * 100).toFloat()))
        }
        
        val dataSet = LineDataSet(entries, "Student Engagement")
        dataSet.color = resources.getColor(R.color.primary_color, null)
        dataSet.setCircleColor(resources.getColor(R.color.primary_color, null))
        
        val lineData = LineData(dataSet)
        lineChartEngagement.data = lineData
        lineChartEngagement.invalidate()
    }

    private fun updateCourseDistributionChart() {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(40f, "Mathematics"))
        entries.add(PieEntry(30f, "Science"))
        entries.add(PieEntry(20f, "English"))
        entries.add(PieEntry(10f, "History"))
        
        val dataSet = PieDataSet(entries, "Course Distribution")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        val pieData = PieData(dataSet)
        pieChartCourseDistribution.data = pieData
        pieChartCourseDistribution.invalidate()
    }

    private fun loadTeacherAnalytics() {
        // Implementation for loading comprehensive teacher analytics
    }

    private fun loadRecentActivity() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("recentActivities")
            .whereEqualTo("teacherId", currentUser.uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val activities = mutableListOf<RecentActivity>()
                for (document in documents) {
                    val activity = document.toObject(RecentActivity::class.java)
                    activities.add(activity)
                }
                
                if (activities.isEmpty()) {
                    // Show empty state or placeholder message
                    activities.add(
                        RecentActivity(
                            type = ActivityType.LESSON_COMPLETED,
                            description = "No recent activity",
                            studentName = "",
                            courseName = "",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                
                recentActivityAdapter.updateActivities(activities)
            }
            .addOnFailureListener {
                // Show empty state on failure
                val emptyActivity = listOf(
                    RecentActivity(
                        type = ActivityType.LESSON_COMPLETED,
                        description = "Unable to load recent activity",
                        studentName = "",
                        courseName = "",
                        timestamp = System.currentTimeMillis()
                    )
                )
                recentActivityAdapter.updateActivities(emptyActivity)
            }
    }

    private fun handleActivityClick(activity: RecentActivity) {
        // Handle clicking on recent activity items
        when (activity.type) {
            ActivityType.LESSON_COMPLETED -> {
                // Navigate to lesson details or student progress
            }
            ActivityType.QUIZ_SUBMITTED -> {
                // Navigate to quiz results or grading
            }
            else -> {
                // Handle other activity types
            }
        }
    }

    private fun setupNotifications() {
        // Setup real-time notifications for important events
    }

    private fun setProfileInformation() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Teacher"
                        tvWelcomeMessage.text = "Welcome back, $name!"
                        tvProfileInitial.text = name.first().toString().uppercase()
                    }
                }
        }
    }

    private fun createInitialAnalytics() {
        // Create initial analytics document for new teachers
        val currentUser = auth.currentUser ?: return
        
        val initialAnalytics = hashMapOf(
            "teacherId" to currentUser.uid,
            "totalStudents" to 0,
            "activeCourses" to 0,
            "totalRevenue" to 0.0,
            "createdAt" to System.currentTimeMillis()
        )
        
        db.collection("teacherAnalytics")
            .document(currentUser.uid)
            .set(initialAnalytics)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to dashboard
        loadDashboardData()
    }

    private fun showProfileMenu() {
        val options = arrayOf("Profile Settings", "Logout")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profile")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Profile Settings - TODO: Navigate to profile settings
                        Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show()
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

    private fun parseDashboardMetrics(document: DocumentSnapshot) {
        dashboardMetrics = DashboardMetrics(
            totalStudents = document.getLong("totalStudents")?.toInt() ?: 0,
            activeStudents = document.getLong("activeStudents")?.toInt() ?: 0,
            courseCompletionRate = document.getDouble("courseCompletionRate")?.toFloat() ?: 0f,
            pendingAssignments = document.getLong("pendingAssignments")?.toInt() ?: 0,
            upcomingClasses = document.getLong("upcomingClasses")?.toInt() ?: 0,
            averageQuizScore = document.getDouble("averageQuizScore")?.toFloat() ?: 0f,
            totalRevenue = document.getDouble("totalRevenue") ?: 0.0,
            monthlyGrowth = document.getDouble("monthlyGrowth")?.toFloat() ?: 0f,
            topPerformingCourse = document.getString("topPerformingCourse") ?: "No courses yet"
        )
    }

    private fun cacheDashboardMetrics() {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt("totalStudents", dashboardMetrics.totalStudents)
            putInt("activeStudents", dashboardMetrics.activeStudents)
            putFloat("courseCompletionRate", dashboardMetrics.courseCompletionRate)
            putInt("pendingAssignments", dashboardMetrics.pendingAssignments)
            putInt("upcomingClasses", dashboardMetrics.upcomingClasses)
            putFloat("averageQuizScore", dashboardMetrics.averageQuizScore)
            putString("totalRevenue", dashboardMetrics.totalRevenue.toString())
            putFloat("monthlyGrowth", dashboardMetrics.monthlyGrowth)
            putString("topPerformingCourse", dashboardMetrics.topPerformingCourse)
            putLong("lastCached", System.currentTimeMillis())
            apply()
        }
    }

    private fun loadCachedDashboardMetrics() {
        val sharedPrefs = getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        val lastCached = sharedPrefs.getLong("lastCached", 0)
        
        // Use cached data if it's less than 24 hours old
        if (System.currentTimeMillis() - lastCached < 24 * 60 * 60 * 1000) {
            dashboardMetrics = DashboardMetrics(
                totalStudents = sharedPrefs.getInt("totalStudents", 0),
                activeStudents = sharedPrefs.getInt("activeStudents", 0),
                courseCompletionRate = sharedPrefs.getFloat("courseCompletionRate", 0f),
                pendingAssignments = sharedPrefs.getInt("pendingAssignments", 0),
                upcomingClasses = sharedPrefs.getInt("upcomingClasses", 0),
                averageQuizScore = sharedPrefs.getFloat("averageQuizScore", 0f),
                totalRevenue = sharedPrefs.getString("totalRevenue", "0.0")?.toDoubleOrNull() ?: 0.0,
                monthlyGrowth = sharedPrefs.getFloat("monthlyGrowth", 0f),
                topPerformingCourse = sharedPrefs.getString("topPerformingCourse", "No courses yet") ?: "No courses yet"
            )
            updateDashboardUI()
            showOfflineMessage()
        } else {
            // Cached data is too old, show default values
            dashboardMetrics = DashboardMetrics()
            updateDashboardUI()
            showErrorMessage("Unable to load dashboard data. Please check your internet connection.")
        }
    }

    private fun handleDashboardError(exception: Exception) {
        when (exception) {
            is FirebaseFirestoreException -> {
                if (exception.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.w("TeacherDashboard", "Firestore unavailable - loading cached data")
                    loadCachedDashboardMetrics()
                } else {
                    showErrorMessage("Failed to load dashboard data: ${exception.message}")
                }
            }
            else -> {
                Log.e("TeacherDashboard", "Unexpected error loading dashboard", exception)
                loadCachedDashboardMetrics()
            }
        }
    }

    private fun showOfflineMessage() {
        Toast.makeText(this, "Showing cached data - you're currently offline", Toast.LENGTH_LONG).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}