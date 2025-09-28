package com.example.ed

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.ed.models.*
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
    private lateinit var ivThumbnailPreview: ImageView
    private lateinit var btnSelectThumbnail: Button
    
    // UI Components - Pricing Tab
    private lateinit var switchIsFree: Switch
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDiscountPrice: TextInputEditText
    private lateinit var etDiscountPercentage: TextInputEditText
    

    
    // UI Components - Navigation
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var btnSaveDraft: Button
    private lateinit var btnPublishCourse: Button
    private lateinit var btnPreview: Button
    
    // Data
    private var currentCourse: EnhancedCourse? = null
    private var isEditMode = false
    private var selectedThumbnailUri: Uri? = null
    private var uploadedThumbnailUrl: String? = null
    
    // Activity Result Launcher for image selection
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedThumbnailUri = it
            displayThumbnailPreview(it)
        }
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
        
        // Set up toolbar
        setupToolbar()
        
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
        ivThumbnailPreview = findViewById(R.id.iv_thumbnail_preview)
        btnSelectThumbnail = findViewById(R.id.btn_select_thumbnail)
        
        // Pricing Tab
        switchIsFree = findViewById(R.id.switch_is_free)
        etPrice = findViewById(R.id.et_price)
        etDiscountPrice = findViewById(R.id.et_discount_price)
        etDiscountPercentage = findViewById(R.id.et_discount_percentage)
        

        
        // Navigation
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tab_layout)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnPublishCourse = findViewById(R.id.btn_publish_course)
        btnPreview = findViewById(R.id.btn_preview)
    }

    private fun setupClickListeners() {
        
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
        
        // Thumbnail selection listener
        btnSelectThumbnail.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTabNavigation() {
        tabLayout.addTab(tabLayout.newTab().setText("Basic Info"))
        tabLayout.addTab(tabLayout.newTab().setText("Pricing"))
        
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
            1 -> findViewById<View>(R.id.tab_pricing).visibility = View.VISIBLE
        }
    }

    private fun hideAllTabContent() {
        findViewById<View>(R.id.tab_basic_info).visibility = View.GONE
        findViewById<View>(R.id.tab_pricing).visibility = View.GONE
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
        
        togglePricingFields(false)
    }

    private fun togglePricingFields(enabled: Boolean) {
        etPrice.isEnabled = enabled
        etDiscountPrice.isEnabled = enabled
        etDiscountPercentage.isEnabled = enabled
    }



    private fun saveCourse(isDraft: Boolean) {
        if (!validateCourseData()) {
            return
        }
        
        // Show progress
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage("Saving course...")
        progressDialog.show()
        
        // Upload thumbnail first, then save course
        uploadThumbnailToStorage { thumbnailUrl ->
            val course = buildCourseObject(isDraft, thumbnailUrl)
            
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
        

        
        return true
    }

    private fun buildCourseObject(isDraft: Boolean, thumbnailUrl: String? = null): EnhancedCourse {
        val currentUser = auth.currentUser!!
        
        return EnhancedCourse(
            id = currentCourse?.id ?: UUID.randomUUID().toString(),
            title = etCourseTitle.text.toString().trim(),
            subtitle = etCourseSubtitle.text.toString().trim(),
            description = etShortDescription.text.toString().trim(),
            longDescription = etLongDescription.text.toString().trim(),
            thumbnailUrl = thumbnailUrl ?: uploadedThumbnailUrl ?: "",
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
            learningObjectives = emptyList(),
            prerequisites = emptyList(),
            courseStructure = CourseStructure(
                modules = emptyList()
            ),
            pricing = CoursePricing(
                isFree = switchIsFree.isChecked,
                price = if (switchIsFree.isChecked) 0.0 else etPrice.text.toString().toDoubleOrNull() ?: 0.0,
                discountPrice = etDiscountPrice.text.toString().toDoubleOrNull() ?: 0.0
            ),
            settings = CourseSettings(
                isPublished = !isDraft,
                allowEnrollment = true,
                allowDiscussions = true,
                allowDownloads = true,
                certificateEnabled = false,
                dripContent = false,
                maxStudents = 0
            ),
            status = if (isDraft) CourseStatus.DRAFT else CourseStatus.PUBLISHED,
            updatedAt = System.currentTimeMillis()
        )
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
        
        // Populate pricing
        switchIsFree.isChecked = course.pricing.isFree
        etPrice.setText(course.pricing.price.toString())
        etDiscountPrice.setText(course.pricing.discountPrice.toString())
        
        // Load thumbnail if exists
        course.thumbnailUrl?.let { url ->
            uploadedThumbnailUrl = url
            loadThumbnailFromUrl(url)
        }
    }

    private fun displayThumbnailPreview(uri: Uri) {
        ivThumbnailPreview.setImageURI(uri)
        ivThumbnailPreview.visibility = View.VISIBLE
        btnSelectThumbnail.text = "Change Thumbnail"
    }

    private fun loadThumbnailFromUrl(url: String) {
        // For now, we'll just show the ImageView and update button text
        // In a real app, you'd use an image loading library like Glide or Picasso
        ivThumbnailPreview.visibility = View.VISIBLE
        btnSelectThumbnail.text = "Change Thumbnail"
    }

    private fun uploadThumbnailToStorage(callback: (String?) -> Unit) {
        selectedThumbnailUri?.let { uri ->
            val thumbnailRef = storage.reference
                .child("course_thumbnails")
                .child("${UUID.randomUUID()}.jpg")

            thumbnailRef.putFile(uri)
                .addOnSuccessListener {
                    thumbnailRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        uploadedThumbnailUrl = downloadUrl.toString()
                        callback(downloadUrl.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to upload thumbnail: ${exception.message}", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
        } ?: callback(uploadedThumbnailUrl)
    }


}