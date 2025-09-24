package com.example.ed

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.ed.R
import com.example.ed.databinding.ActivityCourseCreationBinding
import com.example.ed.utils.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CourseCreationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseCreationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var courseId: String? = null
    private var isEditMode = false
    
    // Deadline variables
    private var selectedDeadline: Calendar? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    // Activity result launchers
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivCourseImage.setImageURI(uri)
                binding.ivCourseImage.visibility = android.view.View.VISIBLE
                binding.llImagePlaceholder.visibility = android.view.View.GONE
            }
        }
    }

    private fun loadCourseData() {
        if (courseId == null) return
        
        db.collection("courses")
            .document(courseId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        // Populate form fields with existing data
                        binding.etCourseTitle.setText(document.getString("title") ?: "")
                        binding.etCourseDescription.setText(document.getString("description") ?: "")
                        binding.etCourseCategory.setText(document.getString("category") ?: "", false)
                        binding.etCourseLevel.setText(document.getString("level") ?: "Beginner", false)
                        binding.etCoursePrice.setText(document.getDouble("price")?.toString() ?: "0")
                        
                        // Load course image if exists
                        val imageUrl = document.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            selectedImageUri = Uri.parse(imageUrl)
                            Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(binding.ivCourseImage)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error loading course data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading course: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied to access gallery", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase first
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        // Security check: Verify user has teacher permissions
        lifecycleScope.launch {
            Log.d("CourseCreationActivity", "Starting security check for teacher permissions")
            Log.d("CourseCreationActivity", "Current user: ${auth.currentUser?.uid}")
            Log.d("CourseCreationActivity", "Current user email: ${auth.currentUser?.email}")
            
            if (!SecurityUtils.canAccessTeacherFeatures()) {
                Log.w("CourseCreationActivity", "Access denied - teacher permissions check failed")
                SecurityUtils.logSecurityEvent(
                    "unauthorized_course_creation_attempt",
                    auth.currentUser?.uid,
                    mapOf("activity" to "CourseCreationActivity")
                )
                Toast.makeText(this@CourseCreationActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            } else {
                Log.d("CourseCreationActivity", "Security check passed - user has teacher permissions")
            }
        }
        
        try {
            binding = ActivityCourseCreationBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Check if we're editing an existing course
            courseId = intent.getStringExtra("courseId")
            isEditMode = courseId != null

            setupUI()
            setupClickListeners()
            
            if (isEditMode) {
                loadCourseData()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing course creation: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        try {
            // Setup category dropdown
            val categories = arrayOf(
                "Programming", "Design", "Business", "Marketing", 
                "Data Science", "Mathematics", "Science", "Language",
                "Art", "Music", "Health", "Fitness", "Other"
            )
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            binding.etCourseCategory.setAdapter(categoryAdapter)

            // Setup level dropdown
            val levels = arrayOf("Beginner", "Intermediate", "Advanced", "Expert")
            val levelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, levels)
            binding.etCourseLevel.setAdapter(levelAdapter)

            // Set default values
            binding.etCourseLevel.setText("Beginner", false)
            
            // Update UI based on mode
            if (isEditMode) {
                supportActionBar?.title = "Edit Course"
                binding.btnCreateCourse.text = "Update Course"
            } else {
                supportActionBar?.title = "Create Course"
                binding.btnCreateCourse.text = "Create Course"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up UI: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.cardImageUpload.setOnClickListener {
            checkPermissionAndOpenPicker()
        }
        
        // Also add click listener to the image placeholder for better UX
        binding.llImagePlaceholder.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        binding.btnSaveDraft.setOnClickListener {
            saveCourse(isDraft = true)
        }

        binding.btnCreateCourse.setOnClickListener {
            saveCourse(isDraft = false)
        }
        
        // Deadline functionality
        binding.switchDeadline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.llDeadlinePicker.visibility = View.VISIBLE
                binding.tvSelectedDeadline.visibility = View.VISIBLE
            } else {
                binding.llDeadlinePicker.visibility = View.GONE
                binding.tvSelectedDeadline.visibility = View.GONE
                selectedDeadline = null
                binding.tvSelectedDeadline.text = "No deadline set"
            }
        }
        
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun checkPermissionAndOpenPicker() {
        try {
            when {
                // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                    when {
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            openImagePicker()
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    }
                }
                // For older versions, use READ_EXTERNAL_STORAGE
                else -> {
                    when {
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            openImagePicker()
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error accessing gallery: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImagePicker() {
        try {
            // Create chooser for camera and gallery
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryIntent.type = "image/*"
            
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            
            val chooserIntent = Intent.createChooser(galleryIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            
            imagePickerLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening image picker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCourse(isDraft: Boolean) {
        try {
            // Rate limiting check
            if (!SecurityUtils.isOperationAllowed("course_creation", 10000)) {
                Toast.makeText(this, "Please wait before creating another course", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!validateInputs()) return

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to create a course", Toast.LENGTH_SHORT).show()
                return
            }

            // Disable buttons during save
            binding.btnSaveDraft.isEnabled = false
            binding.btnCreateCourse.isEnabled = false

            // Security check: Verify teacher permissions again before course creation
            lifecycleScope.launch {
                if (!SecurityUtils.canAccessTeacherFeatures()) {
                    SecurityUtils.logSecurityEvent(
                        "unauthorized_course_creation_attempt",
                        currentUser.uid,
                        mapOf("isDraft" to isDraft)
                    )
                    binding.btnSaveDraft.isEnabled = true
                    binding.btnCreateCourse.isEnabled = true
                    Toast.makeText(this@CourseCreationActivity, "Access denied: Teacher permissions required", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Get teacher name from user profile first
                db.collection("users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val teacherName = if (document.exists()) {
                            document.getString("fullName") ?: currentUser.displayName ?: "Teacher"
                        } else {
                            currentUser.displayName ?: "Teacher"
                        }
                        
                        val courseData = getCourseData(currentUser.uid, isDraft, teacherName)
                        
                        // Sanitize input data
                        val sanitizedCourseData = courseData.mapValues { (key, value) ->
                            if (value is String) SecurityUtils.sanitizeInput(value) else value
                        }

                        if (selectedImageUri != null) {
                            uploadImageAndSaveCourse(sanitizedCourseData, isDraft)
                        } else {
                            saveCourseToFirestore(sanitizedCourseData, isDraft)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Fallback to display name or "Teacher" if profile fetch fails
                        val teacherName = currentUser.displayName ?: "Teacher"
                        val courseData = getCourseData(currentUser.uid, isDraft, teacherName)
                        
                        // Sanitize input data
                        val sanitizedCourseData = courseData.mapValues { (key, value) ->
                            if (value is String) SecurityUtils.sanitizeInput(value) else value
                        }

                        if (selectedImageUri != null) {
                            uploadImageAndSaveCourse(sanitizedCourseData, isDraft)
                        } else {
                            saveCourseToFirestore(sanitizedCourseData, isDraft)
                        }
                    }
            }
        } catch (e: Exception) {
            binding.btnSaveDraft.isEnabled = true
            binding.btnCreateCourse.isEnabled = true
            Toast.makeText(this, "Error saving course: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateInputs(): Boolean {
        val title = binding.etCourseTitle.text?.toString()?.trim()
        val description = binding.etCourseDescription.text?.toString()?.trim()
        val category = binding.etCourseCategory.text?.toString()?.trim()
        val duration = binding.etCourseDuration.text?.toString()?.trim()
        val level = binding.etCourseLevel.text?.toString()?.trim()

        when {
            title.isNullOrEmpty() -> {
                binding.tilCourseTitle.error = "Course title is required"
                binding.etCourseTitle.requestFocus()
                return false
            }
            description.isNullOrEmpty() -> {
                binding.tilCourseDescription.error = "Course description is required"
                binding.etCourseDescription.requestFocus()
                return false
            }
            category.isNullOrEmpty() -> {
                binding.tilCourseCategory.error = "Course category is required"
                binding.etCourseCategory.requestFocus()
                return false
            }
            duration.isNullOrEmpty() -> {
                binding.tilCourseDuration.error = "Course duration is required"
                binding.etCourseDuration.requestFocus()
                return false
            }
            level.isNullOrEmpty() -> {
                binding.tilCourseLevel.error = "Course level is required"
                binding.etCourseLevel.requestFocus()
                return false
            }
            else -> {
                // Clear any existing errors
                binding.tilCourseTitle.error = null
                binding.tilCourseDescription.error = null
                binding.tilCourseCategory.error = null
                binding.tilCourseDuration.error = null
                binding.tilCourseLevel.error = null
                return true
            }
        }
    }

    private fun getCourseData(teacherId: String, isDraft: Boolean, teacherName: String): Map<String, Any> {
        val courseData = hashMapOf(
            "title" to binding.etCourseTitle.text.toString().trim(),
            "description" to binding.etCourseDescription.text.toString().trim(),
            "category" to binding.etCourseCategory.text.toString().trim(),
            "duration" to binding.etCourseDuration.text.toString().trim(),
            "difficulty" to binding.etCourseLevel.text.toString().trim(),
            "instructor" to teacherName,
            "teacherId" to teacherId,
            "thumbnailUrl" to (uploadedImageUrl ?: ""),
            "isPublished" to !isDraft,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
            "enrolledStudents" to 0,
            "rating" to 0.0f,
            "progress" to 0,
            "totalLessons" to 0,
            "completedLessons" to 0,
            "isBookmarked" to false,
            "courseContent" to emptyList<Map<String, Any>>(),
            "hasDeadline" to binding.switchDeadline.isChecked
        )
        
        // Add deadline if set
        selectedDeadline?.let { deadline ->
            courseData["deadline"] = deadline.timeInMillis
        }
        
        return courseData
    }

    private fun uploadImageAndSaveCourse(courseData: Map<String, Any>, isDraft: Boolean) {
        try {
            val imageRef = storage.reference
                .child("course_images")
                .child("${UUID.randomUUID()}.jpg")

            selectedImageUri?.let { uri ->
                imageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        imageRef.downloadUrl.addOnSuccessListener { downloadUrl: Uri ->
                            uploadedImageUrl = downloadUrl.toString()
                            val updatedCourseData = courseData.toMutableMap()
                            updatedCourseData["thumbnailUrl"] = uploadedImageUrl!!
                            saveCourseToFirestore(updatedCourseData, isDraft)
                        }.addOnFailureListener { e: Exception ->
                            Toast.makeText(this, "Error getting image URL: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            // Save without image
                            saveCourseToFirestore(courseData, isDraft)
                        }
                    }
                    .addOnFailureListener { e: Exception ->
                        Toast.makeText(this, "Error uploading image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        // Save without image
                        saveCourseToFirestore(courseData, isDraft)
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error during image upload: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            saveCourseToFirestore(courseData, isDraft)
        }
    }

    private fun saveCourseToFirestore(courseData: Map<String, Any>, isDraft: Boolean) {
        try {
            val currentUser = auth.currentUser ?: return
            
            if (isEditMode && courseId != null) {
                // Update existing course
                db.collection("courses")
                    .document(courseId!!)
                    .update(courseData)
                    .addOnSuccessListener {
                        // Log successful course update
                        SecurityUtils.logSecurityEvent(
                            "course_updated",
                            currentUser.uid,
                            mapOf(
                                "courseId" to courseId!!,
                                "isDraft" to isDraft,
                                "title" to (courseData["title"] as? String ?: "")
                            )
                        )
                        
                        val message = if (isDraft) "Course updated as draft" else "Course updated successfully"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        
                        // Return to previous screen
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Log failed course update
                        SecurityUtils.logSecurityEvent(
                            "course_update_failed",
                            currentUser.uid,
                            mapOf("courseId" to courseId!!, "error" to e.message.orEmpty())
                        )
                        
                        binding.btnSaveDraft.isEnabled = true
                        binding.btnCreateCourse.isEnabled = true
                        Toast.makeText(this, "Error updating course: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                // Create new course
                db.collection("courses")
                    .add(courseData)
                    .addOnSuccessListener { documentReference ->
                        // Log successful course creation
                        SecurityUtils.logSecurityEvent(
                            "course_created",
                            currentUser.uid,
                            mapOf(
                                "courseId" to documentReference.id,
                                "isDraft" to isDraft,
                                "title" to (courseData["title"] as? String ?: "")
                            )
                        )
                        
                        val message = if (isDraft) "Course saved as draft" else "Course created successfully"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        
                        // Return to previous screen
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Log failed course creation
                        SecurityUtils.logSecurityEvent(
                            "course_creation_failed",
                            currentUser.uid,
                            mapOf("error" to e.message.orEmpty())
                        )
                        
                        binding.btnSaveDraft.isEnabled = true
                        binding.btnCreateCourse.isEnabled = true
                        Toast.makeText(this, "Error saving course: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
        } catch (e: Exception) {
            binding.btnSaveDraft.isEnabled = true
            binding.btnCreateCourse.isEnabled = true
            Toast.makeText(this, "Error during course save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        // Check if there are unsaved changes
        val hasChanges = binding.etCourseTitle.text?.isNotEmpty() == true ||
                        binding.etCourseDescription.text?.isNotEmpty() == true ||
                        selectedImageUri != null

        if (hasChanges) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setPositiveButton("Leave") { _, _ -> super.onBackPressed() }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showDatePicker() {
        val calendar = selectedDeadline ?: Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            if (selectedDeadline == null) {
                selectedDeadline = Calendar.getInstance()
            }
            selectedDeadline?.set(Calendar.YEAR, selectedYear)
            selectedDeadline?.set(Calendar.MONTH, selectedMonth)
            selectedDeadline?.set(Calendar.DAY_OF_MONTH, selectedDay)
            
            updateDeadlineDisplay()
        }, year, month, day).apply {
            // Set minimum date to today
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }
    
    private fun showTimePicker() {
        if (selectedDeadline == null) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val calendar = selectedDeadline ?: Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            selectedDeadline?.set(Calendar.HOUR_OF_DAY, selectedHour)
            selectedDeadline?.set(Calendar.MINUTE, selectedMinute)
            selectedDeadline?.set(Calendar.SECOND, 0)
            
            updateDeadlineDisplay()
        }, hour, minute, false).show()
    }
    
    private fun updateDeadlineDisplay() {
        selectedDeadline?.let { deadline ->
            val formattedDeadline = fullDateFormat.format(deadline.time)
            binding.tvSelectedDeadline.text = "Deadline: $formattedDeadline"
            binding.tvSelectedDeadline.visibility = View.VISIBLE
        }
    }
}