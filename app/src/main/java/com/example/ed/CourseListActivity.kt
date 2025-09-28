package com.example.ed

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.R
import com.example.ed.adapters.CourseAdapter
import com.example.ed.databinding.ActivityCourseListBinding
import com.example.ed.models.Course
import com.example.ed.utils.SecurityUtils
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class CourseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseListBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var courseAdapter: CourseAdapter
    
    private var allCourses = mutableListOf<Course>()
    private var filteredCourses = mutableListOf<Course>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityCourseListBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

            setupUI()
            setupClickListeners()
            setupRecyclerView()
            setupTabLayout()
            setupSearch()
            loadCourses()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing course list: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        // Check if user is authenticated
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please log in to view courses", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnAddCourse.setOnClickListener {
            navigateToCreateCourse()
        }

        binding.fabAddCourse.setOnClickListener {
            navigateToCreateCourse()
        }

        binding.btnCreateFirstCourse.setOnClickListener {
            navigateToCreateCourse()
        }

        binding.btnFilter.setOnClickListener {
            // TODO: Implement filter dialog
            Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        courseAdapter = CourseAdapter(
            courses = filteredCourses,
            enrolledCourses = emptyList(), // Empty list for teacher's course management view
            onCourseClick = { course ->
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            },
            onEditClick = { course ->
                editCourse(course)
            },
            onMenuClick = { course, view ->
                showCourseMenu(course, view)
            },
            showAsEnrolled = false, // Show as available courses with Enroll button
            isTeacherView = true // Teacher view for course management
        )

        // Use GridLayoutManager for better course display
        binding.rvCourses.layoutManager = GridLayoutManager(this, 2)
        binding.rvCourses.adapter = courseAdapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentFilter = "All"
                    1 -> currentFilter = "Published"
                    2 -> currentFilter = "Drafts"
                }
                filterCourses()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCourses()
            }
        })
    }

    private fun loadCourses() {
        val currentUser = auth.currentUser ?: return

        try {
            db.collection("courses")
                .whereEqualTo("instructor.id", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading courses: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        allCourses.clear()
                        for (document in snapshot.documents) {
                            try {
                                // Map EnhancedCourse structure to Course model
                                val instructorMap = document.get("instructor") as? Map<String, Any>
                                val categoryMap = document.get("category") as? Map<String, Any>
                                val pricingMap = document.get("pricing") as? Map<String, Any>
                                val settingsMap = document.get("settings") as? Map<String, Any>
                                
                                val course = Course(
                                    id = document.id,
                                    title = document.getString("title") ?: "",
                                    description = document.getString("description") ?: "",
                                    category = categoryMap?.get("name") as? String ?: "",
                                    duration = "Self-paced", // EnhancedCourse doesn't have simple duration string
                                    difficulty = document.getString("difficulty")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Beginner",
                                    instructor = instructorMap?.get("name") as? String ?: "Teacher",
                                    teacherId = instructorMap?.get("id") as? String ?: "",
                                    thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                                    isPublished = settingsMap?.get("isPublished") as? Boolean ?: (document.getString("status") == "PUBLISHED"),
                                    createdAt = document.getLong("createdAt") ?: 0L,
                                    updatedAt = document.getLong("updatedAt") ?: 0L,
                                    enrolledStudents = 0, // Will be calculated separately
                                    rating = 0.0f, // Will be calculated from reviews
                                    isFree = pricingMap?.get("isFree") as? Boolean ?: true,
                                    price = pricingMap?.get("price") as? Double ?: 0.0
                                )
                                allCourses.add(course)
                            } catch (ex: Exception) {
                                // Skip malformed documents
                                continue
                            }
                        }
                        
                        // Sort courses by createdAt in descending order (newest first)
                        allCourses.sortByDescending { it.createdAt }
                        
                        filterCourses()
                        updateEmptyState()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up course listener: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun filterCourses() {
        val searchQuery = binding.etSearch.text?.toString()?.lowercase()?.trim() ?: ""
        
        filteredCourses.clear()
        
        for (course in allCourses) {
            // Apply status filter
            val matchesStatus = when (currentFilter) {
                "All" -> true
                "Published" -> course.isPublished
                "Drafts" -> !course.isPublished
                else -> true
            }
            
            // Apply search filter
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                course.title.lowercase().contains(searchQuery) ||
                course.description.lowercase().contains(searchQuery) ||
                course.category.lowercase().contains(searchQuery)
            }
            
            if (matchesStatus && matchesSearch) {
                filteredCourses.add(course)
            }
        }
        
        courseAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (filteredCourses.isEmpty()) {
            binding.rvCourses.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvCourses.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun navigateToCreateCourse() {
        val intent = Intent(this, CourseCreationActivity::class.java)
        createCourseLauncher.launch(intent)
    }

    private val createCourseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Reload courses when returning from create/edit
            loadCourses()
        }
    }

    private fun editCourse(course: Course) {
        val intent = Intent(this, CourseCreationActivity::class.java)
        intent.putExtra("courseId", course.id)
        createCourseLauncher.launch(intent)
    }

    private fun showCourseMenu(course: Course, view: View) {
        val popupMenu = PopupMenu(view.context, view)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.course_menu, popupMenu.menu)  // Use your custom menu XML

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_enroll -> {
                    // Enroll course logic
                    enrollInCourse(course)
                    true
                }
                R.id.action_unenroll -> {
                    // Unenroll course logic
                    unenrollFromCourse(course)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun showDeleteConfirmation(course: Course) {
        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete \"${course.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCourse(course)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCourse(course: Course) {
        db.collection("courses")
            .document(course.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Course deleted successfully", Toast.LENGTH_SHORT).show()
                loadCourses() // Reload the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting course: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun duplicateCourse(course: Course) {
        val duplicatedCourse = mapOf(
            "title" to "${course.title} (Copy)",
            "description" to course.description,
            "category" to course.category,
            "difficulty" to course.difficulty,
            "thumbnailUrl" to course.thumbnailUrl,
            "teacherId" to auth.currentUser?.uid,
            "teacherName" to auth.currentUser?.displayName,
            "isPublished" to false, // Duplicated courses start as drafts
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("courses")
            .add(duplicatedCourse)
            .addOnSuccessListener {
                Toast.makeText(this, "Course duplicated successfully", Toast.LENGTH_SHORT).show()
                loadCourses() // Reload the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error duplicating course: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun toggleCoursePublishStatus(course: Course) {
        // Security check: Verify teacher permissions and course ownership
        lifecycleScope.launch {
            if (!SecurityUtils.canAccessTeacherFeatures()) {
                SecurityUtils.logSecurityEvent(
                    "unauthorized_course_publish_attempt",
                    auth.currentUser?.uid,
                    mapOf("courseId" to course.id)
                )
                Toast.makeText(this@CourseListActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // Verify course ownership
            if (!SecurityUtils.validateCourseOwnership(course.id)) {
                SecurityUtils.logSecurityEvent(
                    "unauthorized_course_access_attempt",
                    auth.currentUser?.uid,
                    mapOf("courseId" to course.id, "action" to "publish_toggle")
                )
                Toast.makeText(this@CourseListActivity, "Access denied: You can only modify your own courses", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // Rate limiting check
            if (!SecurityUtils.isOperationAllowed("course_publish_toggle", 3000)) {
                Toast.makeText(this@CourseListActivity, "Please wait before toggling course status again", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val newStatus = !course.isPublished
            val statusText = if (newStatus) "published" else "unpublished"
            
            db.collection("courses")
                .document(course.id)
                .update("isPublished", newStatus)
                .addOnSuccessListener {
                    // Log successful status change
                    SecurityUtils.logSecurityEvent(
                        "course_publish_status_changed",
                        auth.currentUser?.uid,
                        mapOf(
                            "courseId" to course.id,
                            "newStatus" to newStatus,
                            "title" to course.title
                        )
                    )
                    
                    Toast.makeText(this@CourseListActivity, "Course $statusText successfully", Toast.LENGTH_SHORT).show()
                    loadCourses() // Reload the course list
                }
                .addOnFailureListener { e ->
                    // Log failed status change
                    SecurityUtils.logSecurityEvent(
                        "course_publish_status_change_failed",
                        auth.currentUser?.uid,
                        mapOf("courseId" to course.id, "error" to e.message.orEmpty())
                    )
                    
                    Toast.makeText(this@CourseListActivity, "Error updating course status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun enrollInCourse(course: Course) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to enroll in courses", Toast.LENGTH_SHORT).show()
            return
        }

        // Create enrollment document
        val enrollmentData = mapOf(
            "studentId" to currentUser.uid,
            "courseId" to course.id,
            "enrolledAt" to System.currentTimeMillis(),
            "isActive" to true,
            "progress" to 0,
            "completedLessons" to 0,
            "lastAccessedAt" to System.currentTimeMillis(),
            "status" to "active"
        )

        db.collection("enrollments")
            .add(enrollmentData)
            .addOnSuccessListener {
                // Update course enrolled students count
                db.collection("courses").document(course.id)
                    .update("enrolledStudents", course.enrolledStudents + 1)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Successfully enrolled in ${course.title}", Toast.LENGTH_SHORT).show()
                        loadCourses() // Reload to update UI
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to enroll: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unenrollFromCourse(course: Course) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage enrollments", Toast.LENGTH_SHORT).show()
            return
        }

        // Find and delete enrollment
        db.collection("enrollments")
            .whereEqualTo("studentId", currentUser.uid)
            .whereEqualTo("courseId", course.id)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val enrollmentDoc = documents.documents[0]
                    enrollmentDoc.reference.delete()
                        .addOnSuccessListener {
                            // Update course enrolled students count
                            db.collection("courses").document(course.id)
                                .update("enrolledStudents", maxOf(0, course.enrolledStudents - 1))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Successfully unenrolled from ${course.title}", Toast.LENGTH_SHORT).show()
                                    loadCourses() // Reload to update UI
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to unenroll: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "You are not enrolled in this course", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to check enrollment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}