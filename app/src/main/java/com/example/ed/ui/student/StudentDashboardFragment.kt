package com.example.ed.ui.student

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ed.CourseDetailsActivity
import com.example.ed.R
import com.example.ed.StudentDashboardFragmentActivity
import com.example.ed.adapters.CourseAdapter
import com.example.ed.databinding.FragmentStudentDashboardBinding
import com.example.ed.models.Course
import com.example.ed.services.DatabaseService
import com.example.ed.services.PointsRewardsService
import com.example.ed.ui.student.StudentWeeklyContentFragment
import com.example.ed.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StudentDashboardFragment : Fragment() {

    private var _binding: FragmentStudentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var databaseService: DatabaseService
    private lateinit var pointsRewardsService: PointsRewardsService
    
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        databaseService = DatabaseService.getInstance(requireContext())
        pointsRewardsService = PointsRewardsService.getInstance(requireContext())

        setupUI()
        setupRecyclerViews()
        loadUserProfile()
        loadStudentAnalytics()
        loadEnrolledCourses()
        loadPopularCourses()
        loadUserPoints()
        setupClickListeners()
        
        // Debug: Check if we need to create test data
        checkAndCreateTestData()
        
        // Add manual refresh capability
        setupRefreshListeners()
    }

    private fun setupUI() {
        binding.progressBar.visibility = View.VISIBLE
        loadAnalyticsData()
    }

    private fun setupRecyclerViews() {
        // Setup Continue Learning RecyclerView
        continueCoursesAdapter = CourseAdapter(
            courses = continueCourses,
            enrolledCourses = emptyList(), // No need to pass enrolled courses for filtering in enrolled view
            onCourseClick = { course ->
                // Navigate to weekly content fragment
                val fragment = StudentWeeklyContentFragment.newInstance(course.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onEditClick = null, // Hide edit for students
            onMenuClick = { course, view ->
                showCourseOptionsMenu(course, view)
            },
            showAsEnrolled = true, // Show as enrolled courses with View button
            isTeacherView = false // Student view
        )
        binding.rvContinueCourses.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvContinueCourses.adapter = continueCoursesAdapter

        // Setup Popular Courses RecyclerView
        popularCoursesAdapter = CourseAdapter(
            courses = popularCourses,
            enrolledCourses = emptyList(), // No need to pass since we filter in loadPopularCourses
            onCourseClick = { course ->
                showCourseEnrollmentDialog(course)
            },
            onEditClick = null, // Hide edit for students
            onMenuClick = { course, view ->
                showCourseOptionsMenu(course, view)
            },
            showAsEnrolled = false, // Show as available courses with Enroll button
            isTeacherView = false // Student view
        )
        binding.rvPopularCourses.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPopularCourses.adapter = popularCoursesAdapter
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.tvUserName.text = currentUser.displayName ?: "Student"
            
            // Load profile picture
            if (currentUser.photoUrl != null) {
                Glide.with(this)
                    .load(currentUser.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivProfilePicture)
            } else {
                binding.ivProfilePicture.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun loadStudentAnalytics() {
        if (!isAdded) return
        
        analyticsJob?.cancel()
        analyticsJob = lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    loadCachedAnalytics()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }
                
                // Calculate real analytics from enrollments
                calculateRealAnalytics(currentUser.uid)
            } catch (e: Exception) {
                if (isAdded) {
                    handleAnalyticsError(e)
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun calculateRealAnalytics(userId: String) {
        firestore.collection("enrollments")
            .whereEqualTo("studentId", userId)
            .get()
            .addOnSuccessListener { enrollmentSnapshot ->
                if (!isAdded) return@addOnSuccessListener
                
                val enrollments = enrollmentSnapshot.documents
                val enrolledCount = enrollments.size
                val completedCount = enrollments.count { doc ->
                    (doc.getDouble("progress") ?: 0.0) >= 100.0
                }
                
                // Calculate average grade from completed courses
                val grades = enrollments.mapNotNull { doc ->
                    doc.getDouble("finalGrade")
                }.filter { it > 0 }
                val avgGrade = if (grades.isNotEmpty()) grades.average() else 0.0
                
                // Calculate study streak (simplified - days since last activity)
                val lastActivity = enrollments.mapNotNull { doc ->
                    doc.getLong("lastAccessedAt")
                }.maxOrNull() ?: 0L
                
                val daysSinceLastActivity = if (lastActivity > 0) {
                    ((System.currentTimeMillis() - lastActivity) / (1000 * 60 * 60 * 24)).toInt()
                } else 0
                
                val studyStreak = if (daysSinceLastActivity <= 1) 7 else 0 // Simplified streak calculation
                
                // Get pending assignments count
                firestore.collection("assignments")
                    .whereArrayContains("enrolledStudents", userId)
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener { assignmentSnapshot ->
                        if (!isAdded) return@addOnSuccessListener
                        
                        val pendingCount = assignmentSnapshot.size()
                        
                        // Update UI with real data
                        updateAnalyticsUI(enrolledCount, completedCount, avgGrade, studyStreak, pendingCount)
                        
                        // Cache the data
                        cacheAnalyticsData(enrolledCount, completedCount, avgGrade, studyStreak, pendingCount)
                        
                        // Hide progress bar
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) {
                            Log.e("StudentDashboard", "Error loading assignments", e)
                            // Still update with partial data
                            updateAnalyticsUI(enrolledCount, completedCount, avgGrade, studyStreak, 0)
                            binding.progressBar.visibility = View.GONE
                        }
                    }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Log.e("StudentDashboard", "Error loading enrollments", e)
                    handleAnalyticsError(e)
                    binding.progressBar.visibility = View.GONE
                }
            }
    }

    private fun loadAnalyticsFromServer(userId: String) {
        firestore.collection("student_analytics")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                
                if (document.exists()) {
                    enrolledCourses = document.getLong("enrolledCourses")?.toInt() ?: 0
                    completedCourses = document.getLong("completedCourses")?.toInt() ?: 0
                    averageGrade = document.getDouble("averageGrade") ?: 0.0
                    studyStreak = document.getLong("studyStreak")?.toInt() ?: 0
                    pendingAssignments = document.getLong("pendingAssignments")?.toInt() ?: 0
                    
                    updateAnalyticsUI(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
                    cacheAnalyticsData(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
                } else {
                    createInitialAnalytics(userId)
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded) {
                    handleAnalyticsError(exception)
                }
            }
    }

    private fun createInitialAnalytics(userId: String) {
        val initialData = hashMapOf(
            "enrolledCourses" to 0,
            "completedCourses" to 0,
            "averageGrade" to 0.0,
            "studyStreak" to 0,
            "pendingAssignments" to 0,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        firestore.collection("student_analytics")
            .document(userId)
            .set(initialData)
            .addOnSuccessListener {
                if (isAdded) {
                    updateAnalyticsUI(0, 0, 0.0, 0, 0)
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded) {
                    handleAnalyticsError(exception)
                }
            }
    }

    private fun loadCachedAnalytics() {
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("student_analytics_cache", android.content.Context.MODE_PRIVATE)
        val cacheTimestamp = sharedPrefs.getLong("cache_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        val cacheValidityPeriod = 24 * 60 * 60 * 1000L // 24 hours
        
        if (currentTime - cacheTimestamp < cacheValidityPeriod) {
            enrolledCourses = sharedPrefs.getInt("enrolled_courses", 0)
            completedCourses = sharedPrefs.getInt("completed_courses", 0)
            averageGrade = sharedPrefs.getFloat("average_grade", 0.0f).toDouble()
            studyStreak = sharedPrefs.getInt("study_streak", 0)
            pendingAssignments = sharedPrefs.getInt("pending_assignments", 0)
            
            updateAnalyticsUI(enrolledCourses, completedCourses, averageGrade, studyStreak, pendingAssignments)
        } else {
            updateAnalyticsUI(0, 0, 0.0, 0, 0)
        }
    }

    private fun cacheAnalyticsData(enrolled: Int, completed: Int, grade: Double, streak: Int, pending: Int) {
        if (!isAdded) return
        val sharedPrefs = requireContext().getSharedPreferences("student_analytics_cache", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("enrolled_courses", enrolled)
            .putInt("completed_courses", completed)
            .putFloat("average_grade", grade.toFloat())
            .putInt("study_streak", streak)
            .putInt("pending_assignments", pending)
            .putLong("cache_timestamp", System.currentTimeMillis())
            .apply()
    }

    private fun handleAnalyticsError(exception: Exception) {
        Log.e("StudentDashboard", "Error loading analytics", exception)
        loadCachedAnalytics()
        
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Failed to load latest data. Showing cached information.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No internet connection. Showing cached data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadEnrolledCourses() {
        enrolledCoursesJob?.cancel()

        binding.progressBarContinue.visibility = View.VISIBLE
        binding.cardEmptyStateContinue.visibility = View.GONE

        enrolledCoursesJob = lifecycleScope.launch {
            try {
                if (!isActive) return@launch

                val currentUser = auth.currentUser ?: return@launch

                // Get enrolled courses for the current user
                firestore.collection("enrollments")
                    .whereEqualTo("studentId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .addOnSuccessListener { enrollmentDocs ->
                        if (!isActive || !isAdded) return@addOnSuccessListener

                        if (enrollmentDocs.isEmpty) {
                            binding.progressBarContinue.visibility = View.GONE
                            binding.cardEmptyStateContinue.visibility = View.VISIBLE
                            continueCourses.clear()
                            continueCoursesAdapter.notifyDataSetChanged()
                            Log.d("StudentDashboard", "No enrollments found for user")
                            return@addOnSuccessListener
                        }

                        val courseIds = enrollmentDocs.documents.mapNotNull { it.getString("courseId") }
                        
                        Log.d("StudentDashboard", "Found ${courseIds.size} enrolled course IDs: $courseIds")

                        if (courseIds.isEmpty()) {
                            binding.progressBarContinue.visibility = View.GONE
                            binding.cardEmptyStateContinue.visibility = View.VISIBLE
                            Log.d("StudentDashboard", "No course IDs found in enrollments")
                            return@addOnSuccessListener
                        }

                        // Fetch course details using document references
                        val courseTasks = courseIds.take(10).map { courseId ->
                            firestore.collection("courses").document(courseId).get()
                        }

                        Tasks.whenAllSuccess<DocumentSnapshot>(courseTasks)
                            .addOnSuccessListener { courseDocs ->
                                if (!isActive || !isAdded) return@addOnSuccessListener

                                val courses = courseDocs.mapNotNull { doc ->
                                    try {
                                        if (doc.exists()) {
                                            Course(
                                                id = doc.id,
                                                title = doc.getString("title") ?: "",
                                                instructor = try {
                                                    doc.getString("instructor") ?: doc.getString("teacherName") ?: "Unknown Instructor"
                                                } catch (e: Exception) {
                                                    Log.w("StudentDashboard", "Error getting instructor for course ${doc.id}: ${e.message}")
                                                    "Unknown Instructor"
                                                },
                                                description = doc.getString("description") ?: "",
                                                category = doc.getString("category") ?: "",
                                                difficulty = doc.getString("difficulty") ?: "",
                                                duration = doc.getString("estimatedDuration") ?: "",
                                                thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                                isPublished = doc.getBoolean("isPublished") ?: true,
                                                createdAt = doc.getLong("createdAt") ?: 0L,
                                                updatedAt = doc.getLong("updatedAt") ?: 0L,
                                                enrolledStudents = doc.getLong("enrolledStudents")?.toInt() ?: 0,
                                                rating = doc.getDouble("rating")?.toFloat() ?: 0.0f,
                                                teacherId = doc.getString("teacherId") ?: "",
                                                price = doc.getDouble("price") ?: 0.0,
                                                isFree = doc.getBoolean("isFree") ?: true
                                            )
                                        } else {
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Log.e("StudentDashboard", "Error parsing enrolled course", e)
                                        null
                                    }
                                }

                                Log.d("StudentDashboard", "Successfully loaded ${courses.size} enrolled courses")
                                courses.forEach { course ->
                                    Log.d("StudentDashboard", "Enrolled course: ${course.title} (${course.id})")
                                }

                                continueCourses.clear()
                                continueCourses.addAll(courses)
                                continueCoursesAdapter.notifyDataSetChanged()

                                binding.progressBarContinue.visibility = View.GONE
                                binding.rvContinueCourses.visibility = View.VISIBLE // Always make RecyclerView visible
                                
                                if (courses.isEmpty()) {
                                    binding.cardEmptyStateContinue.visibility = View.VISIBLE
                                    binding.rvContinueCourses.visibility = View.GONE
                                    Log.d("StudentDashboard", "No enrolled courses to display - showing empty state")
                                } else {
                                    binding.cardEmptyStateContinue.visibility = View.GONE
                                    binding.rvContinueCourses.visibility = View.VISIBLE
                                    Log.d("StudentDashboard", "Showing ${courses.size} enrolled courses in RecyclerView")
                                }
                            }
                            .addOnFailureListener { e ->
                                if (isAdded) {
                                    Log.e("StudentDashboard", "Error loading courses", e)
                                    binding.progressBarContinue.visibility = View.GONE
                                    binding.cardEmptyStateContinue.visibility = View.VISIBLE
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) {
                            Log.e("StudentDashboard", "Error loading enrollments", e)
                            binding.progressBarContinue.visibility = View.GONE
                            binding.cardEmptyStateContinue.visibility = View.VISIBLE
                        }
                    }
            } catch (e: Exception) {
                if (isAdded) {
                    when (e) {
                        is kotlinx.coroutines.CancellationException -> {
                            Log.d("StudentDashboard", "Enrolled courses loading cancelled")
                        }
                        else -> {
                            Log.e("StudentDashboard", "Error in loadEnrolledCourses", e)
                            binding.progressBarContinue.visibility = View.GONE
                            binding.cardEmptyStateContinue.visibility = View.VISIBLE
                        }
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
                
                val currentUser = auth.currentUser ?: return@launch
                
                // First, get enrolled course IDs to filter them out
                val enrolledCourseIds = mutableSetOf<String>()
                try {
                    val enrollmentSnapshot = firestore.collection("enrollments")
                        .whereEqualTo("studentId", currentUser.uid)
                        .whereEqualTo("isActive", true)
                        .get()
                        .await()
                    
                    enrolledCourseIds.addAll(
                        enrollmentSnapshot.documents.mapNotNull { it.getString("courseId") }
                    )
                    
                    Log.d("StudentDashboard", "Found ${enrolledCourseIds.size} enrolled courses to filter out")
                } catch (e: Exception) {
                    Log.w("StudentDashboard", "Error getting enrolled courses for filtering: ${e.message}")
                }
                
                // Load all published courses
                val coursesSnapshot = firestore.collection("courses")
                    .whereEqualTo("isPublished", true)
                    .get()
                    .await()
                
                Log.d("StudentDashboard", "Received ${coursesSnapshot.documents.size} published courses from Firestore")
                
                // Debug: Log course details
                coursesSnapshot.documents.forEach { doc ->
                    Log.d("StudentDashboard", "Course: ${doc.id} - ${doc.getString("title")} - Published: ${doc.getBoolean("isPublished")}")
                }

                if (coursesSnapshot.documents.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.progressBarPopular.visibility = View.GONE
                        binding.cardEmptyStatePopular.visibility = View.VISIBLE
                    }
                    return@launch
                }
                
                val courses = withContext(Dispatchers.Default) {
                    coursesSnapshot.documents
                        .mapNotNull { document ->
                            try {
                                Course(
                                    id = document.id,
                                    title = document.getString("title") ?: "",
                                    instructor = document.getString("instructor") ?: document.getString("teacherName") ?: "Unknown Instructor",
                                    description = document.getString("description") ?: "",
                                    category = document.getString("category") ?: "",
                                    difficulty = document.getString("difficulty") ?: "Beginner",
                                    duration = document.getString("duration") ?: document.getString("estimatedDuration") ?: "1 hour",
                                    thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                                    isPublished = document.getBoolean("isPublished") ?: false,
                                    createdAt = document.getLong("createdAt") ?: 0,
                                    updatedAt = document.getLong("updatedAt") ?: 0,
                                    enrolledStudents = document.getLong("enrolledStudents")?.toInt() ?: 0,
                                    rating = document.getDouble("rating")?.toFloat() ?: 4.0f,
                                    teacherId = document.getString("teacherId") ?: "",
                                    price = document.getDouble("price") ?: 0.0,
                                    totalLessons = document.getLong("totalLessons")?.toInt() ?: 1,
                                    isFree = document.getBoolean("isFree") ?: true,
                                    progress = 0,
                                    completedLessons = 0,
                                    isBookmarked = false,
                                    courseContent = emptyList(),
                                    originalPrice = document.getDouble("originalPrice") ?: (document.getDouble("price") ?: 0.0),
                                    deadline = document.getLong("deadline"),
                                    hasDeadline = document.getBoolean("hasDeadline") ?: false
                                )
                            } catch (e: Exception) {
                                Log.e("StudentDashboard", "Error parsing course document: ${document.id}", e)
                                null
                            }
                        }
                        .filter { course ->
                            // FILTER OUT enrolled courses - only show courses NOT enrolled in
                            !enrolledCourseIds.contains(course.id)
                        }
                        .sortedByDescending { it.enrolledStudents }
                        .take(10) // Show top 10 non-enrolled courses
                }
                
                withContext(Dispatchers.Main) {
                    popularCourses.clear()
                    popularCourses.addAll(courses)
                    popularCoursesAdapter.notifyDataSetChanged()
                    
                    binding.progressBarPopular.visibility = View.GONE
                    binding.rvPopularCourses.visibility = View.VISIBLE // Always make RecyclerView visible
                    
                    if (courses.isEmpty()) {
                        binding.cardEmptyStatePopular.visibility = View.VISIBLE
                        binding.rvPopularCourses.visibility = View.GONE
                        Log.d("StudentDashboard", "No non-enrolled courses available - showing empty state")
                    } else {
                        binding.cardEmptyStatePopular.visibility = View.GONE
                        binding.rvPopularCourses.visibility = View.VISIBLE
                        Log.d("StudentDashboard", "Loaded ${courses.size} non-enrolled popular courses in RecyclerView")
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    when (e) {
                        is kotlinx.coroutines.CancellationException -> {
                            Log.d("StudentDashboard", "Popular courses loading cancelled")
                        }
                        else -> {
                            Log.e("StudentDashboard", "Error loading popular courses", e)
                            binding.progressBarPopular.visibility = View.GONE
                            binding.cardEmptyStatePopular.visibility = View.VISIBLE
                        }
                    }
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
        if (!isAdded) return
        
        binding.tvEnrolledCourses.text = enrolledCount.toString()
        binding.tvCompletedCourses.text = completedCount.toString()
        binding.tvAverageGrade.text = String.format("%.1f%%", avgGrade)
        binding.tvStudyStreak.text = "$streak days"
        binding.tvPendingAssignments.text = pendingCount.toString()
    }

    private fun setupClickListeners() {
        // Profile picture click for menu
        binding.ivProfilePicture.setOnClickListener {
            showProfileMenu()
        }
        
        // View all courses click
        binding.tvViewAllContinue.setOnClickListener {
            // Navigate to courses fragment
            val activity = requireActivity() as? StudentDashboardFragmentActivity
            activity?.switchToCoursesFragment()
        }
        
        binding.tvViewAllPopular.setOnClickListener {
            // Navigate to courses fragment
            val activity = requireActivity() as? StudentDashboardFragmentActivity
            activity?.switchToCoursesFragment()
        }
        
        // Play Games button click
        binding.btnPlayGames.setOnClickListener {
            // Navigate to gamification fragment
            val fragment = StudentGamificationFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showProfileMenu() {
        // TODO: Implement profile menu
        Toast.makeText(requireContext(), "Profile menu coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showCourseEnrollmentDialog(course: Course) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Enroll in Course")
            .setMessage("Do you want to enroll in '${course.title}'?\n\nInstructor: ${course.instructor}\nDifficulty: ${course.difficulty}")
            .setPositiveButton("Enroll") { _, _ -> 
                enrollInCourse(course)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCourseOptionsMenu(course: Course, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.available_course_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_enroll -> {
                    showCourseEnrollmentDialog(course)
                    true
                }
                R.id.action_preview -> {
                    // Navigate to course details for preview
                    val fragment = StudentWeeklyContentFragment.newInstance(course.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
    private fun enrollInCourse(course: Course) {
        val currentUser = auth.currentUser ?: return
        val enrollmentData = hashMapOf(
            "studentId" to currentUser.uid,
            "courseId" to course.id,
            "enrolledAt" to System.currentTimeMillis(),
            "isActive" to true,
            "progress" to 0,
            "lastAccessedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("enrollments")
            .add(enrollmentData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Successfully enrolled in ${course.title}!", Toast.LENGTH_SHORT).show()
                // Refresh both sections to show updated enrollment status
                loadEnrolledCourses()
                loadPopularCourses()
                loadStudentAnalytics() // Update statistics
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to enroll: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAnalyticsData() {
        // Load user profile and analytics data
        loadUserProfile()
        loadStudentAnalytics()
        loadEnrolledCourses()
        loadPopularCourses()
        
        // Hide progress bar after a reasonable timeout
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000) // 3 second timeout
            if (isAdded && _binding != null) {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadUserPoints() {
        lifecycleScope.launch {
            try {
                val userPoints = pointsRewardsService.getUserPoints()
                val totalPoints = userPoints?.totalPoints ?: 0L
                
                withContext(Dispatchers.Main) {
                    binding.tvTotalPoints.text = "${totalPoints} Points"
                    Log.d("StudentDashboard", "Updated points display: $totalPoints")
                }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading user points", e)
                withContext(Dispatchers.Main) {
                    binding.tvTotalPoints.text = "0 Points"
                }
            }
        }
    }
    
    private fun setupRefreshListeners() {
        // Add click listeners to refresh sections manually
        binding.tvViewAllContinue?.setOnClickListener {
            Log.d("StudentDashboard", "Manual refresh of Continue Learning section")
            loadEnrolledCourses()
            loadUserPoints() // Refresh points as well
        }
        
        binding.tvViewAllPopular?.setOnClickListener {
            Log.d("StudentDashboard", "Manual refresh of Popular Courses section")
            loadPopularCourses()
            loadUserPoints() // Refresh points as well
        }
        
        // Long press on profile picture to create fresh test data (for debugging)
        binding.ivProfilePicture.setOnLongClickListener {
            Log.d("StudentDashboard", "Creating fresh test data...")
            createTestCourses()
            true
        }
    }
    
    // Public method to refresh points from external sources (like games)
    fun refreshPoints() {
        loadUserPoints()
    }
    
    // Public method to refresh all dashboard data
    fun refreshDashboard() {
        loadUserProfile()
        loadStudentAnalytics()
        loadEnrolledCourses()
        loadPopularCourses()
        loadUserPoints()
    }
    
    private fun checkAndCreateTestData() {
        lifecycleScope.launch {
            try {
                // Check if there are any published courses
                val coursesSnapshot = firestore.collection("courses")
                    .whereEqualTo("isPublished", true)
                    .limit(1)
                    .get()
                    .await()
                
                if (coursesSnapshot.documents.isEmpty()) {
                    Log.d("StudentDashboard", "No published courses found. Creating test data...")
                    createTestCourses()
                } else {
                    Log.d("StudentDashboard", "Found existing published courses")
                    // Check if user has any enrollments
                    checkAndCreateTestEnrollment()
                }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error checking for test data", e)
            }
        }
    }
    
    private fun checkAndCreateTestEnrollment() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Check if user has any enrollments
                val enrollmentsSnapshot = firestore.collection("enrollments")
                    .whereEqualTo("studentId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .limit(1)
                    .get()
                    .await()
                
                if (enrollmentsSnapshot.documents.isEmpty()) {
                    Log.d("StudentDashboard", "No enrollments found. Creating test enrollment...")
                    
                    // Get the first available published course
                    val coursesSnapshot = firestore.collection("courses")
                        .whereEqualTo("isPublished", true)
                        .whereEqualTo("isFree", true)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (coursesSnapshot.documents.isNotEmpty()) {
                        val courseId = coursesSnapshot.documents.first().id
                        createTestEnrollment(courseId)
                        
                        // Refresh enrolled courses after creating enrollment
                        kotlinx.coroutines.delay(1000)
                        loadEnrolledCourses()
                    }
                } else {
                    Log.d("StudentDashboard", "Found existing enrollments")
                }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error checking for test enrollment", e)
            }
        }
    }
    
    private fun createTestCourses() {
        val testCourses = listOf(
            hashMapOf(
                "title" to "Introduction to Programming",
                "instructor" to "Dr. Sarah Johnson",
                "description" to "Learn the fundamentals of programming with hands-on exercises",
                "category" to "Programming",
                "difficulty" to "Beginner",
                "duration" to "6 weeks",
                "isPublished" to true,
                "isFree" to true,
                "price" to 0.0,
                "rating" to 4.5,
                "enrolledStudents" to 150,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "teacherId" to "test_teacher_1"
            ),
            hashMapOf(
                "title" to "Advanced Mathematics",
                "instructor" to "Prof. Michael Chen",
                "description" to "Master advanced mathematical concepts and problem-solving",
                "category" to "Mathematics",
                "difficulty" to "Advanced",
                "duration" to "8 weeks",
                "isPublished" to true,
                "isFree" to false,
                "price" to 99.99,
                "rating" to 4.8,
                "enrolledStudents" to 89,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "teacherId" to "test_teacher_2"
            ),
            hashMapOf(
                "title" to "Digital Marketing Essentials",
                "instructor" to "Emma Rodriguez",
                "description" to "Learn modern digital marketing strategies and tools",
                "category" to "Business",
                "difficulty" to "Intermediate",
                "duration" to "4 weeks",
                "isPublished" to true,
                "isFree" to true,
                "price" to 0.0,
                "rating" to 4.3,
                "enrolledStudents" to 203,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "teacherId" to "test_teacher_3"
            ),
            hashMapOf(
                "title" to "Web Development Bootcamp",
                "instructor" to "Alex Thompson",
                "description" to "Complete web development course from beginner to advanced",
                "category" to "Programming",
                "difficulty" to "Intermediate",
                "duration" to "12 weeks",
                "isPublished" to true,
                "isFree" to false,
                "price" to 149.99,
                "rating" to 4.7,
                "enrolledStudents" to 320,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "teacherId" to "test_teacher_4"
            ),
            hashMapOf(
                "title" to "Data Science Fundamentals",
                "instructor" to "Dr. Lisa Wang",
                "description" to "Introduction to data science, statistics, and machine learning",
                "category" to "Data Science",
                "difficulty" to "Intermediate",
                "duration" to "10 weeks",
                "isPublished" to true,
                "isFree" to true,
                "price" to 0.0,
                "rating" to 4.6,
                "enrolledStudents" to 275,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "teacherId" to "test_teacher_5"
            )
        )
        
        val createdCourseIds = mutableListOf<String>()
        
        testCourses.forEachIndexed { index, courseData ->
            firestore.collection("courses")
                .add(courseData)
                .addOnSuccessListener { documentReference ->
                    Log.d("StudentDashboard", "Created test course: ${documentReference.id}")
                    createdCourseIds.add(documentReference.id)
                    
                    // Create test enrollments for first 2 courses only (leave others for Popular Courses)
                    if (index <= 1) {
                        createTestEnrollment(documentReference.id)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("StudentDashboard", "Error creating test course", e)
                }
        }
        
        // Refresh the UI after creating test data
        lifecycleScope.launch {
            kotlinx.coroutines.delay(4000) // Wait for courses and enrollments to be created
            refreshDashboard() // Refresh all sections
        }
    }
    
    private fun createTestEnrollment(courseId: String) {
        val currentUser = auth.currentUser ?: return
        
        val enrollmentData = hashMapOf(
            "studentId" to currentUser.uid,
            "courseId" to courseId,
            "enrolledAt" to System.currentTimeMillis(),
            "isActive" to true,
            "progress" to 25, // 25% progress
            "completedLessons" to 2,
            "totalLessons" to 8,
            "lastAccessedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("enrollments")
            .add(enrollmentData)
            .addOnSuccessListener { documentReference ->
                Log.d("StudentDashboard", "Created test enrollment: ${documentReference.id} for course: $courseId")
            }
            .addOnFailureListener { e ->
                Log.e("StudentDashboard", "Error creating test enrollment", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        analyticsJob?.cancel()
        enrolledCoursesJob?.cancel()
        popularCoursesJob?.cancel()
        _binding = null
    }
}