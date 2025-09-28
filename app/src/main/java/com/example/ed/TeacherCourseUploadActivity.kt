package com.example.ed

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.services.ImageUploadService
import kotlinx.coroutines.launch
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.NumberFormat
import java.util.*

class TeacherCourseUploadActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etCourseTitle: TextInputEditText
    private lateinit var etCourseDescription: TextInputEditText
    private lateinit var etShortDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var spinnerLanguage: Spinner
    private lateinit var etPrice: TextInputEditText
    private lateinit var etOriginalPrice: TextInputEditText
    private lateinit var switchFreePrice: Switch
    private lateinit var etDuration: TextInputEditText
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var etNewTag: TextInputEditText
    private lateinit var btnAddTag: MaterialButton
    private lateinit var ivCourseThumbnail: ImageView
    private lateinit var btnSelectThumbnail: MaterialButton
    private lateinit var rvLearningObjectives: RecyclerView
    private lateinit var etNewObjective: TextInputEditText
    private lateinit var btnAddObjective: MaterialButton
    private lateinit var rvPrerequisites: RecyclerView
    private lateinit var etNewPrerequisite: TextInputEditText
    private lateinit var btnAddPrerequisite: MaterialButton
    private lateinit var rvCourseModules: RecyclerView
    private lateinit var btnAddModule: MaterialButton
    private lateinit var btnSaveDraft: MaterialButton
    private lateinit var btnPublishCourse: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPricePreview: TextView

    // Data
    private val learningObjectives = mutableListOf<String>()
    private val prerequisites = mutableListOf<String>()
    private val courseModules = mutableListOf<CourseModule>()
    private val courseTags = mutableListOf<String>()
    private var selectedThumbnailUri: Uri? = null
    private var uploadedThumbnailUrl: String? = null

    // Adapters
    private lateinit var objectivesAdapter: SimpleListAdapter
    private lateinit var prerequisitesAdapter: SimpleListAdapter
    private lateinit var modulesAdapter: CourseModulesAdapter

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedThumbnailUri = result.data?.data
            selectedThumbnailUri?.let { uri ->
                ivCourseThumbnail.setImageURI(uri)
                btnSelectThumbnail.text = "Change Thumbnail"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme
        ThemeManager.applyCurrentTheme(this)
        
        setContentView(R.layout.activity_teacher_course_upload)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        initializeViews()
        setupSpinners()
        setupRecyclerViews()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        etCourseTitle = findViewById(R.id.et_course_title)
        etCourseDescription = findViewById(R.id.et_course_description)
        etShortDescription = findViewById(R.id.et_short_description)
        spinnerCategory = findViewById(R.id.spinner_category)
        spinnerDifficulty = findViewById(R.id.spinner_difficulty)
        spinnerLanguage = findViewById(R.id.spinner_language)
        etPrice = findViewById(R.id.et_price)
        etOriginalPrice = findViewById(R.id.et_original_price)
        switchFreePrice = findViewById(R.id.switch_free_price)
        etDuration = findViewById(R.id.et_duration)
        chipGroupTags = findViewById(R.id.chip_group_tags)
        etNewTag = findViewById(R.id.et_new_tag)
        btnAddTag = findViewById(R.id.btn_add_tag)
        ivCourseThumbnail = findViewById(R.id.iv_course_thumbnail)
        btnSelectThumbnail = findViewById(R.id.btn_select_thumbnail)
        rvLearningObjectives = findViewById(R.id.rv_learning_objectives)
        etNewObjective = findViewById(R.id.et_new_objective)
        btnAddObjective = findViewById(R.id.btn_add_objective)
        rvPrerequisites = findViewById(R.id.rv_prerequisites)
        etNewPrerequisite = findViewById(R.id.et_new_prerequisite)
        btnAddPrerequisite = findViewById(R.id.btn_add_prerequisite)
        rvCourseModules = findViewById(R.id.rv_course_modules)
        btnAddModule = findViewById(R.id.btn_add_module)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnPublishCourse = findViewById(R.id.btn_publish_course)
        progressBar = findViewById(R.id.progress_bar)
        tvPricePreview = findViewById(R.id.tv_price_preview)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create New Course"
    }

    private fun setupSpinners() {
        // Category Spinner
        val categories = arrayOf(
            "Select Category", "Programming", "Design", "Business", "Marketing",
            "Photography", "Music", "Health & Fitness", "Language", "Academic"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Difficulty Spinner
        val difficulties = arrayOf("Select Level", "Beginner", "Intermediate", "Advanced", "Expert")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter

        // Language Spinner
        val languages = arrayOf("Select Language", "English", "Sinhala", "Tamil", "Hindi", "Spanish", "French")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter
    }

    private fun setupRecyclerViews() {
        // Learning Objectives
        objectivesAdapter = SimpleListAdapter(learningObjectives) { position ->
            learningObjectives.removeAt(position)
            objectivesAdapter.notifyItemRemoved(position)
        }
        rvLearningObjectives.layoutManager = LinearLayoutManager(this)
        rvLearningObjectives.adapter = objectivesAdapter

        // Prerequisites
        prerequisitesAdapter = SimpleListAdapter(prerequisites) { position ->
            prerequisites.removeAt(position)
            prerequisitesAdapter.notifyItemRemoved(position)
        }
        rvPrerequisites.layoutManager = LinearLayoutManager(this)
        rvPrerequisites.adapter = prerequisitesAdapter

        // Course Modules
        modulesAdapter = CourseModulesAdapter(
            modules = courseModules,
            onDeleteClick = { position ->
                courseModules.removeAt(position)
                modulesAdapter.notifyItemRemoved(position)
                modulesAdapter.notifyItemRangeChanged(position, courseModules.size)
            },
            onEditClick = { position ->
                // TODO: Open module edit dialog
                showEditModuleDialog(position)
            }
        )
        rvCourseModules.layoutManager = LinearLayoutManager(this)
        rvCourseModules.adapter = modulesAdapter
    }

    private fun setupClickListeners() {
        toolbar.setNavigationOnClickListener { finish() }

        btnSelectThumbnail.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        btnAddTag.setOnClickListener {
            val tag = etNewTag.text.toString().trim()
            if (tag.isNotEmpty() && !courseTags.contains(tag)) {
                addTagChip(tag)
                courseTags.add(tag)
                etNewTag.text?.clear()
            }
        }

        btnAddObjective.setOnClickListener {
            val objective = etNewObjective.text.toString().trim()
            if (objective.isNotEmpty()) {
                learningObjectives.add(objective)
                objectivesAdapter.notifyItemInserted(learningObjectives.size - 1)
                etNewObjective.text?.clear()
            }
        }

        btnAddPrerequisite.setOnClickListener {
            val prerequisite = etNewPrerequisite.text.toString().trim()
            if (prerequisite.isNotEmpty()) {
                prerequisites.add(prerequisite)
                prerequisitesAdapter.notifyItemInserted(prerequisites.size - 1)
                etNewPrerequisite.text?.clear()
            }
        }

        btnAddModule.setOnClickListener {
            val module = CourseModule(
                id = UUID.randomUUID().toString(),
                title = "New Module ${courseModules.size + 1}",
                description = "",
                lessons = mutableListOf(),
                order = courseModules.size
            )
            courseModules.add(module)
            modulesAdapter.notifyItemInserted(courseModules.size - 1)
        }

        switchFreePrice.setOnCheckedChangeListener { _, isChecked ->
            etPrice.isEnabled = !isChecked
            etOriginalPrice.isEnabled = !isChecked
            if (isChecked) {
                etPrice.setText("0")
                etOriginalPrice.setText("0")
            }
            updatePricePreview()
        }

        btnSaveDraft.setOnClickListener {
            saveCourse(isDraft = true)
        }

        btnPublishCourse.setOnClickListener {
            if (validateCourseData()) {
                saveCourse(isDraft = false)
            }
        }
    }

    private fun setupTextWatchers() {
        val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePricePreview()
            }
        }

        etPrice.addTextChangedListener(priceWatcher)
        etOriginalPrice.addTextChangedListener(priceWatcher)
    }

    private fun addTagChip(tag: String) {
        val chip = Chip(this)
        chip.text = tag
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroupTags.removeView(chip)
            courseTags.remove(tag)
        }
        chipGroupTags.addView(chip)
    }

    private fun updatePricePreview() {
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val originalPrice = etOriginalPrice.text.toString().toDoubleOrNull() ?: 0.0

        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        
        when {
            switchFreePrice.isChecked -> {
                tvPricePreview.text = "FREE"
            }
            originalPrice > price && price > 0 -> {
                val discount = ((originalPrice - price) / originalPrice * 100).toInt()
                tvPricePreview.text = "${formatter.format(price)} (${discount}% OFF from ${formatter.format(originalPrice)})"
            }
            price > 0 -> {
                tvPricePreview.text = formatter.format(price)
            }
            else -> {
                tvPricePreview.text = "Set your price"
            }
        }
    }

    private fun validateCourseData(): Boolean {
        var isValid = true

        if (etCourseTitle.text.toString().trim().isEmpty()) {
            etCourseTitle.error = "Course title is required"
            isValid = false
        }

        if (etCourseDescription.text.toString().trim().isEmpty()) {
            etCourseDescription.error = "Course description is required"
            isValid = false
        }

        if (spinnerCategory.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (spinnerDifficulty.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select difficulty level", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (learningObjectives.isEmpty()) {
            Toast.makeText(this, "Please add at least one learning objective", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (courseModules.isEmpty()) {
            Toast.makeText(this, "Please add at least one course module", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun showEditModuleDialog(position: Int) {
        val module = courseModules[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_module, null)
        
        val etTitle = dialogView.findViewById<EditText>(R.id.et_module_title)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_module_description)
        
        etTitle.setText(module.title)
        etDescription.setText(module.description)
        
        AlertDialog.Builder(this)
            .setTitle("Edit Module")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog: DialogInterface, which: Int ->
                module.title = etTitle.text.toString()
                module.description = etDescription.text.toString()
                modulesAdapter.notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCourse(isDraft: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSaveDraft.isEnabled = false
        btnPublishCourse.isEnabled = false

        // Upload thumbnail first if selected
        if (selectedThumbnailUri != null) {
            uploadThumbnail { thumbnailUrl ->
                uploadedThumbnailUrl = thumbnailUrl
                createCourseDocument(currentUser.uid, isDraft)
            }
        } else {
            createCourseDocument(currentUser.uid, isDraft)
        }
    }

    private fun uploadThumbnail(onComplete: (String?) -> Unit) {
        selectedThumbnailUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    val imageUploadService = ImageUploadService.getInstance()
                    val result = imageUploadService.uploadImage(
                        context = this@TeacherCourseUploadActivity,
                        imageUri = uri,
                        uploaderId = auth.currentUser?.uid ?: "unknown"
                    )
                    
                    if (result.success && result.imageUrl != null) {
                        Toast.makeText(this@TeacherCourseUploadActivity, "✅ Thumbnail saved locally!", Toast.LENGTH_SHORT).show()
                        onComplete(result.imageUrl)
                    } else {
                        Toast.makeText(this@TeacherCourseUploadActivity, "⚠️ Failed to save thumbnail: ${result.error}", Toast.LENGTH_SHORT).show()
                        onComplete(null)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@TeacherCourseUploadActivity, "❌ Error saving thumbnail: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            }
        } ?: onComplete(null)
    }

    private fun createCourseDocument(teacherId: String, isDraft: Boolean) {
        val courseData = hashMapOf(
            "title" to etCourseTitle.text.toString().trim(),
            "description" to etCourseDescription.text.toString().trim(),
            "shortDescription" to etShortDescription.text.toString().trim(),
            "category" to spinnerCategory.selectedItem.toString(),
            "difficulty" to spinnerDifficulty.selectedItem.toString(),
            "language" to spinnerLanguage.selectedItem.toString(),
            "price" to (etPrice.text.toString().toDoubleOrNull() ?: 0.0),
            "originalPrice" to (etOriginalPrice.text.toString().toDoubleOrNull() ?: 0.0),
            "isFree" to switchFreePrice.isChecked,
            "duration" to etDuration.text.toString().trim(),
            "tags" to courseTags,
            "thumbnailUrl" to (uploadedThumbnailUrl ?: ""),
            "learningObjectives" to learningObjectives,
            "prerequisites" to prerequisites,
            "modules" to courseModules.map { module ->
                hashMapOf(
                    "id" to module.id,
                    "title" to module.title,
                    "description" to module.description,
                    "lessons" to module.lessons,
                    "order" to module.order
                )
            },
            "teacherId" to teacherId,
            "isPublished" to !isDraft,
            "isDraft" to isDraft,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
            "enrolledStudents" to 0,
            "rating" to 0.0f,
            "totalLessons" to courseModules.sumOf { it.lessons.size },
            "completedLessons" to 0
        )

        db.collection("courses")
            .add(courseData)
            .addOnSuccessListener { documentReference ->
                progressBar.visibility = View.GONE
                val message = if (isDraft) "Course saved as draft" else "Course published successfully"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSaveDraft.isEnabled = true
                btnPublishCourse.isEnabled = true
                Toast.makeText(this, "Failed to save course: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class CourseModule(
        val id: String,
        var title: String,
        var description: String,
        val lessons: MutableList<String>,
        val order: Int
    )
}