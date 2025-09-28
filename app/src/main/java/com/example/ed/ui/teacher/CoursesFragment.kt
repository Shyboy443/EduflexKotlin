package com.example.ed.ui.teacher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.adapters.CourseAdapter
import com.example.ed.CourseModulesAdapter
import com.example.ed.SimpleListAdapter
import com.example.ed.TeacherCourseUploadActivity
import com.example.ed.models.Course
import com.example.ed.services.ImageUploadService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class CoursesFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Main UI Components
    private lateinit var rvCourses: RecyclerView
    private lateinit var fabAddCourse: FloatingActionButton
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnCreateFirstCourse: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Course Creation UI Components (for bottom sheet/dialog)
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
    private lateinit var tvPricePreview: TextView

    // Data
    private val courses = mutableListOf<Course>()
    private val learningObjectives = mutableListOf<String>()
    private val prerequisites = mutableListOf<String>()
    private val courseModules = mutableListOf<CourseModule>()
    private val courseTags = mutableListOf<String>()
    private var selectedThumbnailUri: Uri? = null
    private var uploadedThumbnailUrl: String? = null

    // Adapters
    private lateinit var coursesAdapter: CourseAdapter
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_teacher_courses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        
        // Ensure FAB is always visible
        fabAddCourse.visibility = View.VISIBLE
        fabAddCourse.show()
        
        loadCourses()
    }

    private fun initializeViews(view: View) {
        rvCourses = view.findViewById(R.id.rv_courses)
        fabAddCourse = view.findViewById(R.id.fab_add_course)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        btnCreateFirstCourse = view.findViewById(R.id.btn_create_first_course)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun setupRecyclerView() {
        coursesAdapter = CourseAdapter(
            courses = courses,
            enrolledCourses = emptyList(), // Teachers don't need enrollment filtering
            onCourseClick = { course -> viewCourse(course) },
            onEditClick = { course -> editCourse(course) },
            onMenuClick = { course, view -> showCourseMenu(course, view) },
            showAsEnrolled = false, // Not showing as enrolled courses
            isTeacherView = true // Show teacher management buttons (Edit/Menu)
        )
        rvCourses.layoutManager = LinearLayoutManager(requireContext())
        rvCourses.adapter = coursesAdapter
    }

    private fun showCourseMenu(course: Course, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.course_menu, popup.menu)
        
        // Update publish/unpublish menu item based on current status
        val publishItem = popup.menu.findItem(R.id.action_publish_unpublish)
        if (course.isPublished) {
            publishItem.title = "Unpublish Course"
        } else {
            publishItem.title = "Publish Course"
        }
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    editCourse(course)
                    true
                }
                R.id.action_publish_unpublish -> {
                    toggleCoursePublishStatus(course)
                    true
                }
                R.id.action_duplicate -> {
                    duplicateCourse(course)
                    true
                }
                R.id.action_delete -> {
                    deleteCourse(course)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupClickListeners() {
        fabAddCourse.setOnClickListener {
            showCourseCreationDialog()
        }
        
        btnCreateFirstCourse.setOnClickListener {
            showCourseCreationDialog()
        }
    }

    private fun loadCourses() {
        progressBar.visibility = View.VISIBLE
        val currentUser = auth.currentUser ?: return
        
        // Use real-time listener for immediate updates
        db.collection("courses")
            .whereEqualTo("teacherId", currentUser.uid)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    android.util.Log.e("CoursesFragment", "Error listening to courses: ${error.message}", error)
                    Toast.makeText(requireContext(), "Failed to load courses: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    updateEmptyState()
                    return@addSnapshotListener
                }
                
                if (documents != null) {
                courses.clear()
                android.util.Log.d("CoursesFragment", "Found ${documents.size()} courses for teacher ${currentUser.uid}")
                for (document in documents) {
                    try {
                        android.util.Log.d("CoursesFragment", "Processing course document: ${document.id}")
                        android.util.Log.d("CoursesFragment", "Document data: ${document.data}")
                        val course = Course(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            description = document.getString("longDescription") ?: document.getString("description") ?: "",
                            category = document.getString("category") ?: "",
                            difficulty = document.getString("difficulty") ?: "",
                            instructor = document.getString("teacherName") ?: document.getString("instructor") ?: "",
                            teacherId = document.getString("teacherId") ?: "",
                            thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                            isPublished = document.getBoolean("isPublished") ?: false,
                            createdAt = document.getLong("createdAt") ?: 0L,
                            updatedAt = document.getLong("updatedAt") ?: 0L,
                            enrolledStudents = document.getLong("enrolledStudents")?.toInt() ?: document.getLong("totalEnrolled")?.toInt() ?: 0,
                            rating = document.getDouble("rating")?.toFloat() ?: document.getDouble("averageRating")?.toFloat() ?: 0.0f,
                            isFree = document.getBoolean("isFree") ?: true,
                            price = document.getDouble("price") ?: 0.0,
                            duration = document.getString("estimatedDuration") ?: document.getString("duration") ?: ""
                        )
                        courses.add(course)
                        android.util.Log.d("CoursesFragment", "Successfully added course: ${course.title}")
                    } catch (e: Exception) {
                        android.util.Log.e("CoursesFragment", "Error parsing course document ${document.id}: ${e.message}", e)
                        // Skip malformed documents
                        continue
                    }
                }
                
                courses.sortByDescending { it.createdAt }
                coursesAdapter.notifyDataSetChanged()
                updateEmptyState()
                progressBar.visibility = View.GONE
                }
            }
    }

    private fun updateEmptyState() {
        if (courses.isEmpty()) {
            rvCourses.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            fabAddCourse.visibility = View.VISIBLE // Always show FAB
        } else {
            rvCourses.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            fabAddCourse.visibility = View.VISIBLE // Always show FAB
        }
    }

    private fun showCourseCreationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_course_creation, null)
        
        // Initialize course creation UI components
        initializeCourseCreationViews(dialogView)
        setupCourseCreationSpinners()
        setupCourseCreationRecyclerViews()
        setupCourseCreationClickListeners()
        setupCourseCreationTextWatchers()
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New Course")
            .setView(dialogView)
            .setPositiveButton("Save Draft", null)
            .setNeutralButton("Publish", null)
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Set button click listeners after showing dialog to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateCourseData()) {
                saveCourse(isDraft = true)
                dialog.dismiss()
            }
        }
        
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            if (validateCourseData()) {
                saveCourse(isDraft = false)
                dialog.dismiss()
            }
        }
    }

    private fun editCourse(course: Course) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_course_creation, null)
        
        // Initialize course creation UI components
        initializeCourseCreationViews(dialogView)
        setupCourseCreationSpinners()
        setupCourseCreationRecyclerViews()
        setupCourseCreationClickListeners()
        setupCourseCreationTextWatchers()
        
        // Pre-populate the form with existing course data
        populateCourseEditForm(course)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Course: ${course.title}")
            .setView(dialogView)
            .setPositiveButton("Update Course", null)
            .setNeutralButton(if (course.isPublished) "Unpublish" else "Publish", null)
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Set button click listeners after showing dialog to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateCourseData()) {
                updateCourse(course.id, course.isPublished)
                dialog.dismiss()
            }
        }
        
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            if (validateCourseData()) {
                updateCourse(course.id, !course.isPublished)
                dialog.dismiss()
            }
        }
    }

    private fun deleteCourse(course: Course) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete '${course.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("courses").document(course.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Course deleted successfully", Toast.LENGTH_SHORT).show()
                        loadCourses()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to delete course: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun viewCourse(course: Course) {
        val courseDetailsFragment = CourseDetailsFragment.newInstance(course.id)
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, courseDetailsFragment)
            .addToBackStack("course_details")
            .commit()
    }
    
    // Method to be called from CourseDetailsFragment to edit course
    fun editCourseFromDetails(course: Course) {
        editCourse(course)
    }
    
    private fun toggleCoursePublishStatus(course: Course) {
        val newStatus = !course.isPublished
        val statusText = if (newStatus) "publish" else "unpublish"
        
        AlertDialog.Builder(requireContext())
            .setTitle("${statusText.capitalize()} Course")
            .setMessage("Are you sure you want to $statusText '${course.title}'?")
            .setPositiveButton(statusText.capitalize()) { _, _ ->
                progressBar.visibility = View.VISIBLE
                
                val updateData = hashMapOf(
                    "isPublished" to newStatus,
                    "status" to if (newStatus) "PUBLISHED" else "DRAFT",
                    "updatedAt" to System.currentTimeMillis()
                )
                
                db.collection("courses").document(course.id)
                    .update(updateData as Map<String, Any>)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        val message = if (newStatus) "Course published successfully" else "Course unpublished successfully"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to $statusText course: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun duplicateCourse(course: Course) {
        AlertDialog.Builder(requireContext())
            .setTitle("Duplicate Course")
            .setMessage("Create a copy of '${course.title}'?")
            .setPositiveButton("Duplicate") { _, _ ->
                progressBar.visibility = View.VISIBLE
                
                // Load full course data from Firestore
                db.collection("courses").document(course.id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val originalData = document.data ?: emptyMap()
                            val currentTime = System.currentTimeMillis()
                            
                            // Create new course data with modifications
                            val duplicatedData = originalData.toMutableMap().apply {
                                put("title", "${originalData["title"]} (Copy)")
                                put("isPublished", false)
                                put("status", "DRAFT")
                                put("enrolledStudents", 0)
                                put("totalEnrolled", 0)
                                put("rating", 0.0)
                                put("averageRating", 0.0f)
                                put("createdAt", currentTime)
                                put("updatedAt", currentTime)
                                put("teacherId", auth.currentUser?.uid)
                                put("teacherName", auth.currentUser?.displayName)
                                put("instructor", auth.currentUser?.displayName)
                            }
                            
                            // Create the duplicate course
                            db.collection("courses")
                                .add(duplicatedData)
                                .addOnSuccessListener { documentReference ->
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(requireContext(), "Course duplicated successfully", Toast.LENGTH_SHORT).show()
                                    android.util.Log.d("CoursesFragment", "Course duplicated with ID: ${documentReference.id}")
                                }
                                .addOnFailureListener { e ->
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(requireContext(), "Failed to duplicate course: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Course not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to load course data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Course creation methods (adapted from TeacherCourseUploadActivity)
    private fun initializeCourseCreationViews(view: View) {
        etCourseTitle = view.findViewById(R.id.et_course_title)
        etCourseDescription = view.findViewById(R.id.et_course_description)
        etShortDescription = view.findViewById(R.id.et_short_description)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        spinnerDifficulty = view.findViewById(R.id.spinner_difficulty)
        spinnerLanguage = view.findViewById(R.id.spinner_language)
        etPrice = view.findViewById(R.id.et_price)
        etOriginalPrice = view.findViewById(R.id.et_original_price)
        switchFreePrice = view.findViewById(R.id.switch_free_price)
        etDuration = view.findViewById(R.id.et_duration)
        chipGroupTags = view.findViewById(R.id.chip_group_tags)
        etNewTag = view.findViewById(R.id.et_new_tag)
        btnAddTag = view.findViewById(R.id.btn_add_tag)
        ivCourseThumbnail = view.findViewById(R.id.iv_course_thumbnail)
        btnSelectThumbnail = view.findViewById(R.id.btn_select_thumbnail)
        rvLearningObjectives = view.findViewById(R.id.rv_learning_objectives)
        etNewObjective = view.findViewById(R.id.et_new_objective)
        btnAddObjective = view.findViewById(R.id.btn_add_objective)
        rvPrerequisites = view.findViewById(R.id.rv_prerequisites)
        etNewPrerequisite = view.findViewById(R.id.et_new_prerequisite)
        btnAddPrerequisite = view.findViewById(R.id.btn_add_prerequisite)
        rvCourseModules = view.findViewById(R.id.rv_course_modules)
        btnAddModule = view.findViewById(R.id.btn_add_module)
        tvPricePreview = view.findViewById(R.id.tv_price_preview)
    }

    private fun setupCourseCreationSpinners() {
        // Category Spinner
        val categories = arrayOf("Programming", "Design", "Business", "Marketing", "Photography", "Music", "Health & Fitness", "Language", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Difficulty Spinner
        val difficulties = arrayOf("Beginner", "Intermediate", "Advanced")
        val difficultyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter

        // Language Spinner
        val languages = arrayOf("English", "Spanish", "French", "German", "Chinese", "Japanese", "Other")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter
    }

    private fun setupCourseCreationRecyclerViews() {
        // Learning Objectives
        objectivesAdapter = SimpleListAdapter(learningObjectives) { position ->
            learningObjectives.removeAt(position)
            objectivesAdapter.notifyItemRemoved(position)
        }
        rvLearningObjectives.layoutManager = LinearLayoutManager(requireContext())
        rvLearningObjectives.adapter = objectivesAdapter

        // Prerequisites
        prerequisitesAdapter = SimpleListAdapter(prerequisites) { position ->
            prerequisites.removeAt(position)
            prerequisitesAdapter.notifyItemRemoved(position)
        }
        rvPrerequisites.layoutManager = LinearLayoutManager(requireContext())
        rvPrerequisites.adapter = prerequisitesAdapter

        // Course Modules - Convert to TeacherCourseUploadActivity.CourseModule for adapter
        val adapterModules = courseModules.map { module ->
            TeacherCourseUploadActivity.CourseModule(
                id = module.id,
                title = module.title,
                description = module.description,
                lessons = module.lessons,
                order = module.order
            )
        }.toMutableList()
        
        modulesAdapter = CourseModulesAdapter(
            modules = adapterModules,
            onEditClick = { position ->
                showEditModuleDialog(position)
            },
            onDeleteClick = { position ->
                courseModules.removeAt(position)
                adapterModules.removeAt(position)
                modulesAdapter.notifyItemRemoved(position)
            }
        )
        rvCourseModules.layoutManager = LinearLayoutManager(requireContext())
        rvCourseModules.adapter = modulesAdapter
    }

    private fun setupCourseCreationClickListeners() {
        btnAddTag.setOnClickListener {
            val tag = etNewTag.text.toString().trim()
            if (tag.isNotEmpty() && !courseTags.contains(tag)) {
                courseTags.add(tag)
                addTagChip(tag)
                etNewTag.text?.clear()
            }
        }

        btnSelectThumbnail.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
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
    }

    private fun setupCourseCreationTextWatchers() {
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
        val chip = Chip(requireContext())
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
                tvPricePreview.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
            originalPrice > price && price > 0 -> {
                val discount = ((originalPrice - price) / originalPrice * 100).toInt()
                tvPricePreview.text = "${formatter.format(price)} (${discount}% off)"
                tvPricePreview.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            price > 0 -> {
                tvPricePreview.text = formatter.format(price)
                tvPricePreview.setTextColor(resources.getColor(android.R.color.black, null))
            }
            else -> {
                tvPricePreview.text = "FREE"
                tvPricePreview.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }
    }

    private fun showEditModuleDialog(position: Int) {
        val module = courseModules[position]
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_module, null)
        
        val etModuleTitle = dialogView.findViewById<TextInputEditText>(R.id.et_module_title)
        val etModuleDescription = dialogView.findViewById<TextInputEditText>(R.id.et_module_description)
        
        etModuleTitle.setText(module.title)
        etModuleDescription.setText(module.description)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Module")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                module.title = etModuleTitle.text.toString().trim()
                module.description = etModuleDescription.text.toString().trim()
                modulesAdapter.notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCourse(isDraft: Boolean) {
        if (!validateCourseData()) {
            return
        }
        
        progressBar.visibility = View.VISIBLE
        
        // Upload thumbnail if selected
        if (selectedThumbnailUri != null) {
            uploadThumbnail { thumbnailUrl ->
                uploadedThumbnailUrl = thumbnailUrl
                createCourseDocument(auth.currentUser!!.uid, isDraft)
            }
        } else {
            createCourseDocument(auth.currentUser!!.uid, isDraft)
        }
    }

    private fun validateCourseData(): Boolean {
        if (etCourseTitle.text.toString().trim().isEmpty()) {
            etCourseTitle.error = "Course title is required"
            return false
        }
        
        if (etCourseDescription.text.toString().trim().isEmpty()) {
            etCourseDescription.error = "Course description is required"
            return false
        }
        
        if (learningObjectives.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one learning objective", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    private fun uploadThumbnail(onComplete: (String?) -> Unit) {
        selectedThumbnailUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    val imageUploadService = ImageUploadService.getInstance()
                    val result = imageUploadService.uploadImage(
                        context = requireContext(),
                        imageUri = uri,
                        uploaderId = auth.currentUser?.uid ?: "",
                        folder = "course_thumbnails"
                    )
                    onComplete(result.imageUrl)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to upload thumbnail: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            }
        } ?: onComplete(null)
    }

    private fun updateCourse(courseId: String, isPublished: Boolean) {
        val currentTime = System.currentTimeMillis()
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        
        val courseData = hashMapOf(
            // Basic course information
            "title" to etCourseTitle.text.toString().trim(),
            "subtitle" to "", // Can be added later
            "description" to etShortDescription.text.toString().trim(),
            "longDescription" to etCourseDescription.text.toString().trim(),
            "category" to spinnerCategory.selectedItem.toString(),
            "difficulty" to spinnerDifficulty.selectedItem.toString().uppercase(),
            "language" to spinnerLanguage.selectedItem.toString().lowercase(),
            "thumbnailUrl" to (uploadedThumbnailUrl ?: ""),
            "previewVideoUrl" to "", // Can be added later
            "tags" to courseTags,
            "learningObjectives" to learningObjectives,
            "prerequisites" to prerequisites,
            
            // Instructor information (keep existing)
            "teacherId" to auth.currentUser?.uid,
            "teacherName" to auth.currentUser?.displayName,
            "instructor" to auth.currentUser?.displayName,
            
            // Pricing information
            "isFree" to switchFreePrice.isChecked,
            "price" to price,
            "originalPrice" to (etOriginalPrice.text.toString().toDoubleOrNull() ?: price),
            
            // Course settings and status
            "isPublished" to isPublished,
            "allowEnrollment" to true,
            "status" to if (isPublished) "PUBLISHED" else "DRAFT",
            
            // Course structure
            "modules" to courseModules.map { module ->
                hashMapOf(
                    "id" to module.id,
                    "title" to module.title,
                    "description" to module.description,
                    "lessons" to module.lessons,
                    "order" to module.order
                )
            },
            "totalLessons" to courseModules.sumOf { it.lessons.size },
            "estimatedDuration" to etDuration.text.toString().trim(),
            
            // Timestamps
            "updatedAt" to currentTime
        )

        android.util.Log.d("CoursesFragment", "Updating course data: $courseData")
        
        progressBar.visibility = View.VISIBLE
        
        db.collection("courses")
            .document(courseId)
            .update(courseData)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                val message = if (isPublished) "Course updated and published successfully" else "Course updated and saved as draft"
                android.util.Log.d("CoursesFragment", "Course updated successfully: $courseId")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                
                // Clear form data
                clearCourseCreationForm()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                android.util.Log.e("CoursesFragment", "Failed to update course: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to update course: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createCourseDocument(teacherId: String, isDraft: Boolean) {
        val currentTime = System.currentTimeMillis()
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        
        val courseData = hashMapOf(
            // Basic course information
            "title" to etCourseTitle.text.toString().trim(),
            "subtitle" to "", // Can be added later
            "description" to etShortDescription.text.toString().trim(),
            "longDescription" to etCourseDescription.text.toString().trim(),
            "category" to spinnerCategory.selectedItem.toString(),
            "difficulty" to spinnerDifficulty.selectedItem.toString().uppercase(),
            "language" to spinnerLanguage.selectedItem.toString().lowercase(),
            "thumbnailUrl" to (uploadedThumbnailUrl ?: ""),
            "previewVideoUrl" to "", // Can be added later
            "tags" to courseTags,
            "learningObjectives" to learningObjectives,
            "prerequisites" to prerequisites,
            
            // Instructor information
            "teacherId" to teacherId,
            "teacherName" to auth.currentUser?.displayName,
            "instructor" to auth.currentUser?.displayName, // For backward compatibility
            
            // Pricing information
            "isFree" to switchFreePrice.isChecked,
            "price" to price,
            "originalPrice" to (etOriginalPrice.text.toString().toDoubleOrNull() ?: price),
            
            // Course settings and status
            "isPublished" to !isDraft,
            "allowEnrollment" to true,
            "status" to if (isDraft) "DRAFT" else "PUBLISHED",
            
            // Course structure
            "modules" to courseModules.map { module ->
                hashMapOf(
                    "id" to module.id,
                    "title" to module.title,
                    "description" to module.description,
                    "lessons" to module.lessons,
                    "order" to module.order
                )
            },
            "totalLessons" to courseModules.sumOf { it.lessons.size },
            "estimatedDuration" to etDuration.text.toString().trim(),
            
            // Enrollment and rating information
            "enrolledStudents" to 0,
            "totalEnrolled" to 0, // For EnhancedCourse compatibility
            "rating" to 0.0,
            "averageRating" to 0.0f, // For EnhancedCourse compatibility
            "completedLessons" to 0,
            
            // Timestamps
            "createdAt" to currentTime,
            "updatedAt" to currentTime
        )

        android.util.Log.d("CoursesFragment", "Saving course data: $courseData")
        
        db.collection("courses")
            .add(courseData)
            .addOnSuccessListener { documentReference ->
                progressBar.visibility = View.GONE
                val message = if (isDraft) "Course saved as draft" else "Course published successfully"
                android.util.Log.d("CoursesFragment", "Course saved successfully with ID: ${documentReference.id}")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                
                // Clear form data
                clearCourseCreationForm()
                
                // Reload courses
                loadCourses()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to save course: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateCourseEditForm(course: Course) {
        // Load course data from Firestore to get complete information
        db.collection("courses").document(course.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Basic information
                    etCourseTitle.setText(document.getString("title") ?: course.title)
                    etCourseDescription.setText(document.getString("longDescription") ?: course.description)
                    etShortDescription.setText(document.getString("description") ?: "")
                    etDuration.setText(document.getString("estimatedDuration") ?: course.duration)
                    
                    // Pricing
                    val isFree = document.getBoolean("isFree") ?: course.isFree
                    switchFreePrice.isChecked = isFree
                    if (!isFree) {
                        etPrice.setText(document.getDouble("price")?.toString() ?: course.price.toString())
                        etOriginalPrice.setText(document.getDouble("originalPrice")?.toString() ?: course.price.toString())
                    }
                    
                    // Spinners
                    val category = document.getString("category") ?: course.category
                    val difficulty = document.getString("difficulty") ?: course.difficulty
                    val language = document.getString("language") ?: "english"
                    
                    setSpinnerSelection(spinnerCategory, category)
                    setSpinnerSelection(spinnerDifficulty, difficulty)
                    setSpinnerSelection(spinnerLanguage, language)
                    
                    // Tags
                    val tags = document.get("tags") as? List<String> ?: emptyList()
                    courseTags.clear()
                    chipGroupTags.removeAllViews()
                    tags.forEach { tag ->
                        courseTags.add(tag)
                        addTagChip(tag)
                    }
                    
                    // Learning objectives
                    val objectives = document.get("learningObjectives") as? List<String> ?: emptyList()
                    learningObjectives.clear()
                    learningObjectives.addAll(objectives)
                    objectivesAdapter.notifyDataSetChanged()
                    
                    // Prerequisites
                    val prereqs = document.get("prerequisites") as? List<String> ?: emptyList()
                    prerequisites.clear()
                    prerequisites.addAll(prereqs)
                    prerequisitesAdapter.notifyDataSetChanged()
                    
                    // Modules
                    val modules = document.get("modules") as? List<Map<String, Any>> ?: emptyList()
                    courseModules.clear()
                    modules.forEach { moduleData ->
                        val module = CourseModule(
                            id = moduleData["id"] as? String ?: UUID.randomUUID().toString(),
                            title = moduleData["title"] as? String ?: "",
                            description = moduleData["description"] as? String ?: "",
                            lessons = (moduleData["lessons"] as? List<String>)?.toMutableList() ?: mutableListOf(),
                            order = (moduleData["order"] as? Number)?.toInt() ?: 0
                        )
                        courseModules.add(module)
                    }
                    modulesAdapter.notifyDataSetChanged()
                    
                    updatePricePreview()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load course details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(i)
                break
            }
        }
    }

    private fun clearCourseCreationForm() {
        etCourseTitle.text?.clear()
        etCourseDescription.text?.clear()
        etShortDescription.text?.clear()
        etPrice.text?.clear()
        etOriginalPrice.text?.clear()
        etDuration.text?.clear()
        etNewTag.text?.clear()
        etNewObjective.text?.clear()
        etNewPrerequisite.text?.clear()
        
        learningObjectives.clear()
        prerequisites.clear()
        courseModules.clear()
        courseTags.clear()
        
        chipGroupTags.removeAllViews()
        selectedThumbnailUri = null
        uploadedThumbnailUrl = null
        
        switchFreePrice.isChecked = true
        spinnerCategory.setSelection(0)
        spinnerDifficulty.setSelection(0)
        spinnerLanguage.setSelection(0)
    }

    data class CourseModule(
        val id: String,
        var title: String,
        var description: String,
        val lessons: MutableList<String>,
        val order: Int
    )
}


