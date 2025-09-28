package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.CourseAdapter
import com.example.ed.models.Course
import com.example.ed.models.CourseStatus
import com.example.ed.services.DatabaseService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class CourseCatalogActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvCourses: RecyclerView
    private lateinit var fabFilter: FloatingActionButton

    private lateinit var courseAdapter: CourseAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var allCourses = mutableListOf<Course>()
    private var filteredCourses = mutableListOf<Course>()
    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_catalog)

        initializeComponents()
        setupRecyclerView()
        setupClickListeners()
        loadCourses()
    }

    private fun initializeComponents() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        toolbar = findViewById(R.id.toolbar)
        chipGroupCategories = findViewById(R.id.chip_group_categories)
        rvCourses = findViewById(R.id.rv_courses)
        fabFilter = findViewById(R.id.fab_filter)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Course Catalog"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupCategoryChips()
    }

    private fun setupCategoryChips() {
        val categories = listOf("All", "Programming", "Mathematics", "Science", "Languages", "Business")

        categories.forEach { category ->
            val chip = Chip(this)
            chip.text = category
            chip.isCheckable = true
            chip.isChecked = category == "All"
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = category
                    filterCoursesByCategory(category)
                    // Uncheck other chips
                    for (i in 0 until chipGroupCategories.childCount) {
                        val otherChip = chipGroupCategories.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
            }
            chipGroupCategories.addView(chip)
        }
    }

    private fun setupRecyclerView() {
        courseAdapter = CourseAdapter(
            courses = filteredCourses,
            enrolledCourses = emptyList(), // Empty list for catalog view
            onCourseClick = { course ->
                // Navigate to course details
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("COURSE_ID", course.id)
                startActivity(intent)
            },
            onEditClick = null, // No edit functionality for students
            onMenuClick = null,  // No menu functionality for students
            showAsEnrolled = false, // Show as available courses with Enroll button
            isTeacherView = false
        )

        rvCourses.layoutManager = GridLayoutManager(this, 2)
        rvCourses.adapter = courseAdapter
    }

    private fun setupClickListeners() {
        fabFilter.setOnClickListener {
            // TODO: Show advanced filter dialog
            Toast.makeText(this, "Advanced filters coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCourses() {
        // Use DatabaseService for consistency with other parts of the app
        val databaseService = DatabaseService.getInstance(this)

        lifecycleScope.launch {
            try {
                databaseService.getCoursesRealTime()
                    .collect { enhancedCourses ->
                        allCourses.clear()

                        // Convert EnhancedCourse to Course for compatibility
                        val courses = enhancedCourses.map { enhancedCourse ->
                            Course(
                                id = enhancedCourse.id,
                                title = enhancedCourse.title,
                                description = enhancedCourse.description,
                                category = enhancedCourse.category.name,
                                difficulty = enhancedCourse.difficulty.name,
                                instructor = enhancedCourse.instructor.name,
                                teacherId = enhancedCourse.instructor.id,
                                thumbnailUrl = enhancedCourse.thumbnailUrl,
                                isPublished = enhancedCourse.status == CourseStatus.PUBLISHED,
                                createdAt = enhancedCourse.createdAt,
                                updatedAt = enhancedCourse.updatedAt,
                                enrolledStudents = enhancedCourse.enrollmentInfo.totalEnrolled,
                                rating = enhancedCourse.enrollmentInfo.averageRating,
                                price = enhancedCourse.pricing.price,
                                totalLessons = enhancedCourse.courseStructure.totalLessons,
                                isFree = enhancedCourse.pricing.isFree
                            )
                        }

                        allCourses.addAll(courses)
                        filterCoursesByCategory(selectedCategory)

                        // If no courses found, show message to seed database
                        if (allCourses.isEmpty()) {
                            Toast.makeText(this@CourseCatalogActivity, "No courses available. Please seed the database from the student dashboard.", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this@CourseCatalogActivity, "Failed to load courses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterCoursesByCategory(category: String) {
        filteredCourses.clear()
        if (category == "All") {
            filteredCourses.addAll(allCourses)
        } else {
            filteredCourses.addAll(allCourses.filter { it.category == category })
        }
        courseAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Navigate back to student dashboard
                startActivity(Intent(this, StudentDashboardActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        // Navigate to student dashboard
        startActivity(Intent(this, StudentDashboardActivity::class.java))
        finish()
    }
}