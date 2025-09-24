package com.example.ed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.ed.models.*
import com.example.ed.adapters.ModuleAdapter
import com.example.ed.adapters.LearningObjectiveAdapter
import com.example.ed.adapters.PrerequisiteAdapter
import java.util.*

class EnhancedCourseCreationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    // UI Components - Basic Info Tab
    private lateinit var etCourseTitle: TextInputEditText
    private lateinit var etCourseSubtitle: TextInputEditText
    private lateinit var etShortDescription: TextInputEditText
    private lateinit var etLongDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var spinnerLanguage: Spinner
    private lateinit var etTags: TextInputEditText
    
    // UI Components - Pricing Tab
    private lateinit var switchIsFree: Switch
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDiscountPrice: TextInputEditText
    private lateinit var etDiscountPercentage: TextInputEditText
    
    // UI Components - Content Tab
    private lateinit var rvModules: RecyclerView
    private lateinit var moduleAdapter: ModuleAdapter
    private lateinit var btnAddModule: Button
    
    // UI Components - Objectives Tab
    private lateinit var rvLearningObjectives: RecyclerView
    private lateinit var learningObjectiveAdapter: LearningObjectiveAdapter
    private lateinit var btnAddObjective: Button
    private lateinit var rvPrerequisites: RecyclerView
    private lateinit var prerequisiteAdapter: PrerequisiteAdapter
    private lateinit var btnAddPrerequisite: Button
    
    // UI Components - Settings Tab
    private lateinit var switchAllowEnrollment: Switch
    private lateinit var switchAllowDiscussions: Switch
    private lateinit var switchAllowDownloads: Switch
    private lateinit var switchCertificateEnabled: Switch
    private lateinit var switchDripContent: Switch
    private lateinit var etMaxStudents: TextInputEditText
    
    // UI Components - Media Tab
    private lateinit var ivCourseThumbnail: ImageView
    private lateinit var btnSelectThumbnail: Button
    private lateinit var btnSelectPreviewVideo: Button
    private lateinit var tvPreviewVideoStatus: TextView
    
    // UI Components - Navigation
    private lateinit var tabLayout: TabLayout
    private lateinit var btnSaveDraft: Button
    private lateinit var btnPublishCourse: Button
    private lateinit var btnPreview: Button
    
    // Data
    private var courseModules = mutableListOf<CourseModule>()
    private var learningObjectives = mutableListOf<String>()
    private var prerequisites = mutableListOf<String>()
    private var selectedThumbnailUri: Uri? = null
    private var selectedPreviewVideoUri: Uri? = null
    private var uploadedThumbnailUrl: String = ""
    private var uploadedPreviewVideoUrl: String = ""
    private var currentCourse: EnhancedCourse? = null
    private var isEditMode = false

    companion object {
        private const val REQUEST_THUMBNAIL_IMAGE = 1001
        private const val REQUEST_PREVIEW_VIDEO = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enhanced_course_creation)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Check if editing existing course
        val courseId = intent.getStringExtra("courseId")
        if (!courseId.isNullOrEmpty()) {
            isEditMode = true
            loadCourseForEditing(courseId)
        }

        // Initialize UI components
        initializeViews()
        
        // Set up adapters
        setupAdapters()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set up tab navigation
        setupTabNavigation()
        
        // Load initial data
        loadInitialData()
    }

    private fun initializeViews() {
        // Basic Info Tab
        etCourseTitle = findViewById(R.id.et_course_title)
        etCourseSubtitle = findViewById(R.id.et_course_subtitle)
        etShortDescription = findViewById(R.id.et_short_description)
        etLongDescription = findViewById(R.id.et_long_description)
        spinnerCategory = findViewById(R.id.spinner_category)
        spinnerDifficulty = findViewById(R.id.spinner_difficulty)
        spinnerLanguage = findViewById(R.id.spinner_language)
        etTags = findViewById(R.id.et_tags)
        
        // Pricing Tab
        switchIsFree = findViewById(R.id.switch_is_free)
        etPrice = findViewById(R.id.et_price)
        etDiscountPrice = findViewById(R.id.et_discount_price)
        etDiscountPercentage = findViewById(R.id.et_discount_percentage)
        
        // Content Tab
        rvModules = findViewById(R.id.rv_modules)
        btnAddModule = findViewById(R.id.btn_add_module)
        
        // Objectives Tab
        rvLearningObjectives = findViewById(R.id.rv_learning_objectives)
        btnAddObjective = findViewById(R.id.btn_add_objective)
        rvPrerequisites = findViewById(R.id.rv_prerequisites)
        btnAddPrerequisite = findViewById(R.id.btn_add_prerequisite)
        
        // Settings Tab
        switchAllowEnrollment = findViewById(R.id.switch_allow_enrollment)
        switchAllowDiscussions = findViewById(R.id.switch_allow_discussions)
        switchAllowDownloads = findViewById(R.id.switch_allow_downloads)
        switchCertificateEnabled = findViewById(R.id.switch_certificate_enabled)
        switchDripContent = findViewById(R.id.switch_drip_content)
        etMaxStudents = findViewById(R.id.et_max_students)
        
        // Media Tab
        ivCourseThumbnail = findViewById(R.id.iv_course_thumbnail)
        btnSelectThumbnail = findViewById(R.id.btn_select_thumbnail)
        btnSelectPreviewVideo = findViewById(R.id.btn_select_preview_video)
        tvPreviewVideoStatus = findViewById(R.id.tv_preview_video_status)
        
        // Navigation
        tabLayout = findViewById(R.id.tab_layout)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnPublishCourse = findViewById(R.id.btn_publish_course)
        btnPreview = findViewById(R.id.btn_preview)
    }

    private fun setupAdapters() {
        // Modules adapter
        moduleAdapter = ModuleAdapter(courseModules) { module, action ->
            handleModuleAction(module, action)
        }
        rvModules.layoutManager = LinearLayoutManager(this)
        rvModules.adapter = moduleAdapter
        
        // Learning objectives adapter
        learningObjectiveAdapter = LearningObjectiveAdapter(learningObjectives) { objective, position ->
            handleObjectiveAction(objective, position)
        }
        rvLearningObjectives.layoutManager = LinearLayoutManager(this)
        rvLearningObjectives.adapter = learningObjectiveAdapter
        
        // Prerequisites adapter
        prerequisiteAdapter = PrerequisiteAdapter(prerequisites) { prerequisite, position ->
            handlePrerequisiteAction(prerequisite, position)
        }
        rvPrerequisites.layoutManager = LinearLayoutManager(this)
        rvPrerequisites.adapter = prerequisiteAdapter
    }

    private fun setupClickListeners() {
        btnAddModule.setOnClickListener {
            showAddModuleDialog()
        }
        
        btnAddObjective.setOnClickListener {
            showAddObjectiveDialog()
        }
        
        btnAddPrerequisite.setOnClickListener {
            showAddPrerequisiteDialog()
        }
        
        btnSelectThumbnail.setOnClickListener {
            selectThumbnailImage()
        }
        
        btnSelectPreviewVideo.setOnClickListener {
            selectPreviewVideo()
        }
        
        btnSaveDraft.setOnClickListener {
            saveCourse(isDraft = true)
        }
        
        btnPublishCourse.setOnClickListener {
            publishCourse()
        }
        
        btnPreview.setOnClickListener {
            previewCourse()
        }
        
        // Pricing switch listener
        switchIsFree.setOnCheckedChangeListener { _, isChecked ->
            togglePricingFields(!isChecked)
        }
    }

    private fun setupTabNavigation() {
        tabLayout.addTab(tabLayout.newTab().setText("Basic Info"))
        tabLayout.addTab(tabLayout.newTab().setText("Content"))
        tabLayout.addTab(tabLayout.newTab().setText("Objectives"))
        tabLayout.addTab(tabLayout.newTab().setText("Pricing"))
        tabLayout.addTab(tabLayout.newTab().setText("Settings"))
        tabLayout.addTab(tabLayout.newTab().setText("Media"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { showTabContent(it.position) }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTabContent(position: Int) {
        // Hide all tab content views
        hideAllTabContent()
        
        // Show selected tab content
        when (position) {
            0 -> findViewById<View>(R.id.tab_basic_info).visibility = View.VISIBLE
            1 -> findViewById<View>(R.id.tab_content).visibility = View.VISIBLE
            2 -> findViewById<View>(R.id.tab_objectives).visibility = View.VISIBLE
            3 -> findViewById<View>(R.id.tab_pricing).visibility = View.VISIBLE
            4 -> findViewById<View>(R.id.tab_settings).visibility = View.VISIBLE
            5 -> findViewById<View>(R.id.tab_media).visibility = View.VISIBLE
        }
    }

    private fun hideAllTabContent() {
        findViewById<View>(R.id.tab_basic_info).visibility = View.GONE
        findViewById<View>(R.id.tab_content).visibility = View.GONE
        findViewById<View>(R.id.tab_objectives).visibility = View.GONE
        findViewById<View>(R.id.tab_pricing).visibility = View.GONE
        findViewById<View>(R.id.tab_settings).visibility = View.GONE
        findViewById<View>(R.id.tab_media).visibility = View.GONE
    }

    private fun loadInitialData() {
        // Load categories, difficulties, languages for spinners
        setupSpinners()
        
        // Set default values
        setDefaultValues()
    }

    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf("Mathematics", "Science", "English", "History", "Programming", "Art")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
        
        // Difficulty spinner
        val difficulties = arrayOf("Beginner", "Intermediate", "Advanced", "Expert")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter
        
        // Language spinner
        val languages = arrayOf("English", "Spanish", "French", "German", "Chinese", "Japanese")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter
    }

    private fun setDefaultValues() {
        switchIsFree.isChecked = true
        switchAllowEnrollment.isChecked = true
        switchAllowDiscussions.isChecked = true
        switchAllowDownloads.isChecked = true
        switchCertificateEnabled.isChecked = false
        switchDripContent.isChecked = false
        
        togglePricingFields(false)
    }

    private fun togglePricingFields(enabled: Boolean) {
        etPrice.isEnabled = enabled
        etDiscountPrice.isEnabled = enabled
        etDiscountPercentage.isEnabled = enabled
    }

    private fun showAddModuleDialog() {
        // Implementation for adding new module
        val intent = Intent(this, ModuleCreationActivity::class.java)
        startActivityForResult(intent, 1003)
    }

    private fun showAddObjectiveDialog() {
        // Implementation for adding learning objective
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "Enter learning objective"
        
        builder.setTitle("Add Learning Objective")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val objective = input.text.toString().trim()
                if (objective.isNotEmpty()) {
                    learningObjectives.add(objective)
                    learningObjectiveAdapter.notifyItemInserted(learningObjectives.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddPrerequisiteDialog() {
        // Implementation for adding prerequisite
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "Enter prerequisite"
        
        builder.setTitle("Add Prerequisite")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val prerequisite = input.text.toString().trim()
                if (prerequisite.isNotEmpty()) {
                    prerequisites.add(prerequisite)
                    prerequisiteAdapter.notifyItemInserted(prerequisites.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun selectThumbnailImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_THUMBNAIL_IMAGE)
    }

    private fun selectPreviewVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_PREVIEW_VIDEO)
    }

    private fun handleModuleAction(module: CourseModule, action: String) {
        when (action) {
            "edit" -> editModule(module)
            "delete" -> deleteModule(module)
            "reorder" -> reorderModule(module)
        }
    }

    private fun handleObjectiveAction(objective: String, position: Int) {
        learningObjectives.removeAt(position)
        learningObjectiveAdapter.notifyItemRemoved(position)
    }

    private fun handlePrerequisiteAction(prerequisite: String, position: Int) {
        prerequisites.removeAt(position)
        prerequisiteAdapter.notifyItemRemoved(position)
    }

    private fun editModule(module: CourseModule) {
        val intent = Intent(this, ModuleCreationActivity::class.java)
        intent.putExtra("moduleId", module.id)
        startActivityForResult(intent, 1004)
    }

    private fun deleteModule(module: CourseModule) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Module")
            .setMessage("Are you sure you want to delete this module? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                courseModules.remove(module)
                moduleAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reorderModule(module: CourseModule) {
        // Implementation for reordering modules
        Toast.makeText(this, "Module reordering feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun saveCourse(isDraft: Boolean) {
        if (!validateCourseData()) {
            return
        }
        
        val course = buildCourseObject(isDraft)
        
        // Show progress
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage("Saving course...")
        progressDialog.show()
        
        // Upload media files first if any
        uploadMediaFiles { thumbnailUrl, previewVideoUrl ->
            course.copy(
                thumbnailUrl = thumbnailUrl,
                previewVideoUrl = previewVideoUrl
            )
            
            // Save to Firestore
            saveCourseToFirestore(course) { success ->
                progressDialog.dismiss()
                if (success) {
                    Toast.makeText(this, "Course saved successfully!", Toast.LENGTH_SHORT).show()
                    if (!isDraft) {
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Failed to save course", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun publishCourse() {
        if (!validateCourseForPublishing()) {
            return
        }
        
        saveCourse(isDraft = false)
    }

    private fun previewCourse() {
        val course = buildCourseObject(isDraft = true)
        val intent = Intent(this, CoursePreviewActivity::class.java)
        // Pass course data to preview activity
        startActivity(intent)
    }

    private fun validateCourseData(): Boolean {
        if (etCourseTitle.text.toString().trim().isEmpty()) {
            etCourseTitle.error = "Course title is required"
            tabLayout.getTabAt(0)?.select()
            return false
        }
        
        if (etShortDescription.text.toString().trim().isEmpty()) {
            etShortDescription.error = "Short description is required"
            tabLayout.getTabAt(0)?.select()
            return false
        }
        
        return true
    }

    private fun validateCourseForPublishing(): Boolean {
        if (!validateCourseData()) {
            return false
        }
        
        if (courseModules.isEmpty()) {
            Toast.makeText(this, "Course must have at least one module", Toast.LENGTH_SHORT).show()
            tabLayout.getTabAt(1)?.select()
            return false
        }
        
        if (learningObjectives.isEmpty()) {
            Toast.makeText(this, "Course must have learning objectives", Toast.LENGTH_SHORT).show()
            tabLayout.getTabAt(2)?.select()
            return false
        }
        
        if (uploadedThumbnailUrl.isEmpty() && selectedThumbnailUri == null) {
            Toast.makeText(this, "Course thumbnail is required for publishing", Toast.LENGTH_SHORT).show()
            tabLayout.getTabAt(5)?.select()
            return false
        }
        
        return true
    }

    private fun buildCourseObject(isDraft: Boolean): EnhancedCourse {
        val currentUser = auth.currentUser!!
        
        return EnhancedCourse(
            id = currentCourse?.id ?: UUID.randomUUID().toString(),
            title = etCourseTitle.text.toString().trim(),
            subtitle = etCourseSubtitle.text.toString().trim(),
            description = etShortDescription.text.toString().trim(),
            longDescription = etLongDescription.text.toString().trim(),
            instructor = TeacherProfile(
                id = currentUser.uid,
                name = currentUser.displayName ?: "Teacher"
            ),
            category = CourseCategory(
                name = spinnerCategory.selectedItem.toString()
            ),
            difficulty = CourseDifficulty.valueOf(
                spinnerDifficulty.selectedItem.toString().uppercase()
            ),
            language = spinnerLanguage.selectedItem.toString().lowercase(),
            tags = etTags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() },
            learningObjectives = learningObjectives.toList(),
            prerequisites = prerequisites.toList(),
            courseStructure = CourseStructure(
                modules = courseModules.toList()
            ),
            pricing = CoursePricing(
                isFree = switchIsFree.isChecked,
                price = if (switchIsFree.isChecked) 0.0 else etPrice.text.toString().toDoubleOrNull() ?: 0.0,
                discountPrice = etDiscountPrice.text.toString().toDoubleOrNull() ?: 0.0
            ),
            settings = CourseSettings(
                isPublished = !isDraft,
                allowEnrollment = switchAllowEnrollment.isChecked,
                allowDiscussions = switchAllowDiscussions.isChecked,
                allowDownloads = switchAllowDownloads.isChecked,
                certificateEnabled = switchCertificateEnabled.isChecked,
                dripContent = switchDripContent.isChecked,
                maxStudents = etMaxStudents.text.toString().toIntOrNull() ?: 0
            ),
            status = if (isDraft) CourseStatus.DRAFT else CourseStatus.PUBLISHED,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun uploadMediaFiles(callback: (String, String) -> Unit) {
        var thumbnailUrl = uploadedThumbnailUrl
        var previewVideoUrl = uploadedPreviewVideoUrl
        var uploadCount = 0
        val totalUploads = listOfNotNull(selectedThumbnailUri, selectedPreviewVideoUri).size
        
        if (totalUploads == 0) {
            callback(thumbnailUrl, previewVideoUrl)
            return
        }
        
        selectedThumbnailUri?.let { uri ->
            // Upload thumbnail implementation
            uploadCount++
            if (uploadCount == totalUploads) {
                callback(thumbnailUrl, previewVideoUrl)
            }
        }
        
        selectedPreviewVideoUri?.let { uri ->
            // Upload preview video implementation
            uploadCount++
            if (uploadCount == totalUploads) {
                callback(thumbnailUrl, previewVideoUrl)
            }
        }
    }

    private fun saveCourseToFirestore(course: EnhancedCourse, callback: (Boolean) -> Unit) {
        db.collection("courses")
            .document(course.id)
            .set(course)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun loadCourseForEditing(courseId: String) {
        db.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Parse and populate course data
                    populateCourseData(document.toObject(EnhancedCourse::class.java)!!)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load course: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun populateCourseData(course: EnhancedCourse) {
        currentCourse = course
        
        // Populate basic info
        etCourseTitle.setText(course.title)
        etCourseSubtitle.setText(course.subtitle)
        etShortDescription.setText(course.description)
        etLongDescription.setText(course.longDescription)
        etTags.setText(course.tags.joinToString(", "))
        
        // Populate other fields...
        learningObjectives.clear()
        learningObjectives.addAll(course.learningObjectives)
        learningObjectiveAdapter.notifyDataSetChanged()
        
        prerequisites.clear()
        prerequisites.addAll(course.prerequisites)
        prerequisiteAdapter.notifyDataSetChanged()
        
        courseModules.clear()
        courseModules.addAll(course.courseStructure.modules)
        moduleAdapter.notifyDataSetChanged()
        
        uploadedThumbnailUrl = course.thumbnailUrl
        uploadedPreviewVideoUrl = course.previewVideoUrl
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_THUMBNAIL_IMAGE -> {
                    selectedThumbnailUri = data.data
                    ivCourseThumbnail.setImageURI(selectedThumbnailUri)
                }
                REQUEST_PREVIEW_VIDEO -> {
                    selectedPreviewVideoUri = data.data
                    tvPreviewVideoStatus.text = "Preview video selected"
                }
                1003 -> {
                    // Module added
                    val moduleData = data.getSerializableExtra("module") as? CourseModule
                    moduleData?.let {
                        courseModules.add(it)
                        moduleAdapter.notifyItemInserted(courseModules.size - 1)
                    }
                }
                1004 -> {
                    // Module edited
                    val moduleData = data.getSerializableExtra("module") as? CourseModule
                    moduleData?.let { updatedModule ->
                        val index = courseModules.indexOfFirst { it.id == updatedModule.id }
                        if (index != -1) {
                            courseModules[index] = updatedModule
                            moduleAdapter.notifyItemChanged(index)
                        }
                    }
                }
            }
        }
    }
}