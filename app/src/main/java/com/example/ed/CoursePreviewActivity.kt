package com.example.ed

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.ed.models.Course
import com.example.ed.models.CourseSection

class CoursePreviewActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCourseTitle: TextView
    private lateinit var tvCourseDescription: TextView
    private lateinit var tvCourseDuration: TextView
    private lateinit var tvCoursePrice: TextView
    private lateinit var tvCourseLevel: TextView
    private lateinit var tvCourseCategory: TextView
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var rvModules: RecyclerView
    private lateinit var rvObjectives: RecyclerView
    private lateinit var btnPublishCourse: MaterialButton
    private lateinit var btnEditCourse: MaterialButton

    private var course: Course? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_preview)

        // Get course data from intent
        course = intent.getStringExtra("course_title")?.let { title ->
            // Create a simple Course object from basic data
            Course(
                title = title,
                description = intent.getStringExtra("course_description") ?: "",
                duration = intent.getStringExtra("course_duration") ?: "",
                difficulty = intent.getStringExtra("course_difficulty") ?: "",
                category = intent.getStringExtra("course_category") ?: ""
            )
        }

        initializeViews()
        setupToolbar()
        setupClickListeners()
        displayCourseData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tvCourseTitle = findViewById(R.id.tv_course_title)
        tvCourseDescription = findViewById(R.id.tv_course_description)
        tvCourseDuration = findViewById(R.id.tv_course_duration)
        tvCoursePrice = findViewById(R.id.tv_course_price)
        tvCourseLevel = findViewById(R.id.tv_course_level)
        tvCourseCategory = findViewById(R.id.tv_course_category)
        chipGroupTags = findViewById(R.id.chip_group_tags)
        rvModules = findViewById(R.id.rv_modules)
        rvObjectives = findViewById(R.id.rv_objectives)
        btnPublishCourse = findViewById(R.id.btn_publish_course)
        btnEditCourse = findViewById(R.id.btn_edit_course)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Course Preview"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        btnPublishCourse.setOnClickListener {
            publishCourse()
        }

        btnEditCourse.setOnClickListener {
            finish() // Return to edit mode
        }
    }

    private fun displayCourseData() {
        course?.let { course ->
            tvCourseTitle.text = course.title
            tvCourseDescription.text = course.description
            tvCourseDuration.text = course.duration
            tvCoursePrice.text = "Free" // Course model doesn't have price field
            tvCourseLevel.text = course.difficulty
            tvCourseCategory.text = course.category

            // Display tags - Course model doesn't have tags, so skip this
            chipGroupTags.removeAllViews()

            // Setup modules RecyclerView - using courseContent instead of modules
            rvModules.layoutManager = LinearLayoutManager(this)
            rvModules.adapter = ModulePreviewAdapter(course.courseContent)

            // Setup objectives RecyclerView - Course model doesn't have learningObjectives
            rvObjectives.layoutManager = LinearLayoutManager(this)
            rvObjectives.adapter = ObjectivePreviewAdapter(emptyList())
        }
    }

    private fun publishCourse() {
        // In a real implementation, you would publish the course to Firebase
        // For now, just finish the activity
        setResult(RESULT_OK)
        finish()
    }

    // Simple adapter for module preview
    private class ModulePreviewAdapter(private val sections: List<CourseSection>) : 
        RecyclerView.Adapter<ModulePreviewAdapter.ViewHolder>() {

        class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvModuleTitle: TextView = view.findViewById(R.id.tv_module_title)
            val tvModuleDescription: TextView = view.findViewById(R.id.tv_module_description)
            val tvModuleDuration: TextView = view.findViewById(R.id.tv_module_duration)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_module_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val section = sections[position]
            holder.tvModuleTitle.text = section.title
            holder.tvModuleDescription.text = "Section with ${section.lessons.size} lessons"
            holder.tvModuleDuration.text = "${section.lessons.size} lessons"
        }

        override fun getItemCount() = sections.size
    }

    // Simple adapter for objective preview
    private class ObjectivePreviewAdapter(private val objectives: List<String>) : 
        RecyclerView.Adapter<ObjectivePreviewAdapter.ViewHolder>() {

        class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvObjective: TextView = view.findViewById(R.id.tv_objective)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_objective_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvObjective.text = "• ${objectives[position]}"
        }

        override fun getItemCount() = objectives.size
    }
}