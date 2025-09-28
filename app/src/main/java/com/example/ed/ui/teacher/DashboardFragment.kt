package com.example.ed.ui.teacher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ed.R
import com.example.ed.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class DashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // UI Components
    private lateinit var tvActiveStudents: TextView
    private lateinit var tvCourseCompletion: TextView
    private lateinit var tvPendingAssignments: TextView
    private lateinit var tvUpcomingClasses: TextView
    private lateinit var tvViewAllTools: TextView
    private lateinit var tvViewAllCourses: TextView
    private lateinit var btnGradeAssignments: LinearLayout
    private lateinit var btnCreateCourse: LinearLayout
    private lateinit var btnManageCourses: LinearLayout
    private lateinit var btnManageContent: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("DashboardFragment", "onCreateView called")
        
        val view = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        try {
            // Initialize UI components
            initializeViews(view)
            
            // Set up click listeners
            setupClickListeners()
            
            // Load dashboard data
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error in onCreateView: ${e.message}", e)
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DashboardFragment", "onViewCreated called")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("DashboardFragment", "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("DashboardFragment", "onPause called")
    }

    private fun initializeViews(view: View) {
        try {
            tvActiveStudents = view.findViewById(R.id.tv_active_students)
            tvCourseCompletion = view.findViewById(R.id.tv_course_completion)
            tvPendingAssignments = view.findViewById(R.id.tv_pending_assignments)
            tvUpcomingClasses = view.findViewById(R.id.tv_upcoming_classes)
            tvViewAllTools = view.findViewById(R.id.tv_view_all_tools)
            tvViewAllCourses = view.findViewById(R.id.tv_view_all_courses)
            btnGradeAssignments = view.findViewById(R.id.btn_grade_assignments)
            btnCreateCourse = view.findViewById(R.id.btn_create_course)
            btnManageCourses = view.findViewById(R.id.btn_manage_courses)
            btnManageContent = view.findViewById(R.id.btn_manage_content)
            
            Log.d("DashboardFragment", "All views initialized successfully")
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error initializing views: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        tvViewAllTools.setOnClickListener {
            // TODO: Navigate to all AI tools
            Toast.makeText(context, "View all tools clicked", Toast.LENGTH_SHORT).show()
        }

        btnGradeAssignments.setOnClickListener {
            // TODO: Navigate to assignment grading
            Toast.makeText(context, "Grade Assignments clicked", Toast.LENGTH_SHORT).show()
        }

        tvViewAllCourses.setOnClickListener {
            // Navigate to courses fragment
            (activity as? com.example.ed.TeacherDashboardActivity)?.switchToCoursesFragment()
        }

        btnCreateCourse.setOnClickListener {
            // Navigate to courses fragment
            (activity as? com.example.ed.TeacherDashboardActivity)?.switchToCoursesFragment()
        }

        btnManageCourses.setOnClickListener {
            // Navigate to courses fragment
            (activity as? com.example.ed.TeacherDashboardActivity)?.switchToCoursesFragment()
        }

        btnManageContent.setOnClickListener {
            val intent = Intent(context, com.example.ed.WeeklyContentActivity::class.java)
            startActivity(intent)
        }
        
    }

    private fun loadDashboardData() {
        // Check network connectivity before loading data
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
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
                if (!isAdded) return@addOnSuccessListener
                val activeStudents = documents.size()
                tvActiveStudents.text = activeStudents.toString()
                cacheActiveStudentsCount(activeStudents)
            }
            .addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
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
                if (!isAdded) return@addOnSuccessListener
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
                if (!isAdded) return@addOnFailureListener
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
                if (!isAdded) return@addOnSuccessListener
                val pendingCount = documents.size()
                tvPendingAssignments.text = pendingCount.toString()
                cachePendingAssignmentsCount(pendingCount)
            }
            .addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
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
                if (!isAdded) return@addOnSuccessListener
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
                if (!isAdded) return@addOnFailureListener
                Log.e("TeacherDashboard", "Error loading upcoming classes", exception)
                handleDashboardError("upcoming_classes", exception)
            }
    }

    // Helper methods for caching and offline handling
    private fun loadCachedDashboardData() {
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
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
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("active_students", count)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun cacheCourseCompletionRate(rate: Int) {
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("course_completion", rate)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun cachePendingAssignmentsCount(count: Int) {
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("pending_assignments", count)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun cacheUpcomingClassesCount(count: Int) {
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("teacher_dashboard_cache", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("upcoming_classes", count)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun handleDashboardError(dataType: String, exception: Exception) {
        if (!isAdded) return
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
        if (!isAdded) return
        Toast.makeText(context, "You're offline. Showing cached data.", Toast.LENGTH_LONG).show()
    }
    
    private fun showErrorMessage(message: String) {
        if (!isAdded) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}


