package com.example.ed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.models.Course
import com.example.ed.adapters.CourseAdapter
import com.example.ed.utils.NetworkUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyEnrolledCoursesActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvEnrolledCourses: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var tvCoursesCount: TextView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var coursesAdapter: CourseAdapter
    private val enrolledCourses = mutableListOf<Course>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_enrolled_courses)
        
        initializeViews()
        initializeFirebase()
        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        loadEnrolledCourses()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        rvEnrolledCourses = findViewById(R.id.rv_enrolled_courses)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        progressBar = findViewById(R.id.progress_bar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        tvCoursesCount = findViewById(R.id.tv_courses_count)
    }
    
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Courses"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        coursesAdapter = CourseAdapter(
            courses = enrolledCourses,
            enrolledCourses = emptyList(), // No need to check enrollment for enrolled courses
            onCourseClick = { course -> 
                // Navigate to course details or continue learning
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("courseId", course.id)
                startActivity(intent)
            },
            showAsEnrolled = true, // Show "View" button for enrolled courses
            isTeacherView = false // Student view
        )
        
        rvEnrolledCourses.layoutManager = GridLayoutManager(this, 2)
        rvEnrolledCourses.adapter = coursesAdapter
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_courses
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, StudentDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_courses -> {
                    // Already on this screen
                    true
                }
                R.id.nav_catalog -> {
                    startActivity(Intent(this, CourseCatalogActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_payments -> {
                    startActivity(Intent(this, PaymentHistoryActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadEnrolledCourses() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState("Please log in to view your courses")
            return
        }
        
        progressBar.visibility = View.VISIBLE
        
        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d("MyEnrolledCourses", "No network available, loading cached courses")
            loadCachedCourses()
            return
        }
        
        // First, get all payment records for this user
        firestore.collection("payments")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { paymentDocuments ->
                // Sort payment documents by timestamp (newest first) then extract course IDs
                val courseIds = paymentDocuments.documents
                    .sortedByDescending { it.getLong("timestamp") ?: 0L }
                    .mapNotNull { it.getString("courseId") }
                    .distinct()
                
                if (courseIds.isEmpty()) {
                    progressBar.visibility = View.GONE
                    showEmptyState("You haven't enrolled in any courses yet")
                    return@addOnSuccessListener
                }
                
                // Then get course details for each enrolled course
                loadCourseDetails(courseIds)
            }
            .addOnFailureListener { exception ->
                Log.e("MyEnrolledCourses", "Error loading payment records", exception)
                handleCoursesError(exception)
            }
    }
    
    private fun loadCourseDetails(courseIds: List<String>) {
        val loadedCourses = mutableListOf<Course>()
        var loadedCount = 0
        
        courseIds.forEach { courseId ->
            firestore.collection("courses")
                .document(courseId)
                .get()
                .addOnSuccessListener { document ->
                    document.toObject(Course::class.java)?.let { course: Course ->
                        loadedCourses.add(course.copy(id = document.id))
                    }
                    
                    loadedCount++
                    if (loadedCount == courseIds.size) {
                        // All courses loaded
                        progressBar.visibility = View.GONE
                        updateCoursesList(loadedCourses)
                        // Cache the loaded courses
                        cacheEnrolledCourses(loadedCourses)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MyEnrolledCourses", "Error loading course $courseId", exception)
                    loadedCount++
                    if (loadedCount == courseIds.size) {
                        progressBar.visibility = View.GONE
                        if (loadedCourses.isEmpty()) {
                            handleCoursesError(exception)
                        } else {
                            updateCoursesList(loadedCourses)
                            cacheEnrolledCourses(loadedCourses)
                        }
                    }
                }
        }
    }
    
    private fun updateCoursesList(courses: List<Course>) {
        enrolledCourses.clear()
        enrolledCourses.addAll(courses)
        coursesAdapter.notifyDataSetChanged()
        
        if (courses.isEmpty()) {
            showEmptyState("No courses found")
        } else {
            tvEmptyState.visibility = View.GONE
            rvEnrolledCourses.visibility = View.VISIBLE
            tvCoursesCount.text = "${courses.size} enrolled courses"
        }
    }
    
    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        rvEnrolledCourses.visibility = View.GONE
        tvCoursesCount.text = "0 courses"
    }
    
    private fun showCourseOptions(course: Course, view: View) {
        // TODO: Implement course options menu
        // Options could include: View Certificate, Rate Course, Remove from Favorites, etc.
    }
    
    // Helper methods for offline handling
    private fun cacheEnrolledCourses(courses: List<Course>) {
        try {
            val sharedPrefs = getSharedPreferences("enrolled_courses_cache", Context.MODE_PRIVATE)
            val gson = Gson()
            val coursesJson = gson.toJson(courses)
            val currentTime = System.currentTimeMillis()
            
            sharedPrefs.edit()
                .putString("courses_data", coursesJson)
                .putLong("cache_timestamp", currentTime)
                .apply()
            
            Log.d("MyEnrolledCourses", "Cached ${courses.size} enrolled courses")
        } catch (e: Exception) {
            Log.e("MyEnrolledCourses", "Error caching enrolled courses", e)
        }
    }
    
    private fun loadCachedCourses() {
        try {
            val sharedPrefs = getSharedPreferences("enrolled_courses_cache", Context.MODE_PRIVATE)
            val coursesJson = sharedPrefs.getString("courses_data", null)
            val cacheTimestamp = sharedPrefs.getLong("cache_timestamp", 0)
            
            // Check if cache is valid (24 hours)
            val currentTime = System.currentTimeMillis()
            val cacheAge = currentTime - cacheTimestamp
            val maxCacheAge = 24 * 60 * 60 * 1000L // 24 hours
            
            if (coursesJson != null && cacheAge < maxCacheAge) {
                val gson = Gson()
                val type = object : TypeToken<List<Course>>() {}.type
                val cachedCourses: List<Course> = gson.fromJson(coursesJson, type)
                
                progressBar.visibility = View.GONE
                updateCoursesList(cachedCourses)
                showOfflineMessage()
                Log.d("MyEnrolledCourses", "Loaded ${cachedCourses.size} courses from cache")
            } else {
                progressBar.visibility = View.GONE
                showEmptyState("No internet connection and no cached courses available")
            }
        } catch (e: Exception) {
            Log.e("MyEnrolledCourses", "Error loading cached courses", e)
            progressBar.visibility = View.GONE
            showEmptyState("Error loading cached courses")
        }
    }
    
    private fun handleCoursesError(exception: Exception) {
        progressBar.visibility = View.GONE
        
        when (exception) {
            is FirebaseFirestoreException -> {
                Log.e("MyEnrolledCourses", "Firestore error: ${exception.code}", exception)
                // Try to load cached data as fallback
                loadCachedCourses()
            }
            else -> {
                Log.e("MyEnrolledCourses", "Unexpected error loading courses", exception)
                showErrorMessage("Error loading courses: ${exception.message}")
            }
        }
    }
    
    private fun showOfflineMessage() {
        // You could show a snackbar or toast here to inform user they're viewing cached data
        Log.d("MyEnrolledCourses", "Showing offline cached courses")
    }
    
    private fun showErrorMessage(message: String) {
        showEmptyState(message)
    }
}