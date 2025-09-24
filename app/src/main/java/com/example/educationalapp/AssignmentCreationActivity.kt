package com.example.educationalapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ed.R
import com.example.ed.utils.SecurityUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AssignmentCreationActivity : AppCompatActivity() {

    private lateinit var etAssignmentTitle: TextInputEditText
    private lateinit var etAssignmentDescription: TextInputEditText
    private lateinit var spinnerAssignmentType: Spinner
    private lateinit var etInstructions: TextInputEditText
    private lateinit var etMaxPoints: TextInputEditText
    private lateinit var switchAutoGrading: SwitchMaterial
    private lateinit var btnSelectDueDate: MaterialButton
    private lateinit var btnSelectDueTime: MaterialButton
    private lateinit var tvSelectedDueDate: TextView
    private lateinit var cbTextSubmission: CheckBox
    private lateinit var cbFileUpload: CheckBox
    private lateinit var cbVideoSubmission: CheckBox
    private lateinit var cbLinkSubmission: CheckBox
    private lateinit var btnSaveDraft: MaterialButton
    private lateinit var btnCreateAssignment: MaterialButton

    private var selectedDueDate: Calendar? = null
    private lateinit var courseId: String
    private lateinit var moduleId: String
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_creation)

        // Security check - ensure only teachers can access
        lifecycleScope.launch {
            if (!SecurityUtils.canAccessTeacherFeatures()) {
                SecurityUtils.logSecurityEvent("UNAUTHORIZED_ASSIGNMENT_ACCESS", "User attempted to access assignment creation without teacher permissions")
                Toast.makeText(this@AssignmentCreationActivity, "Access denied. Teacher permissions required.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
        }

        // Get course and module IDs from intent
        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        moduleId = intent.getStringExtra("MODULE_ID") ?: ""

        if (courseId.isEmpty()) {
            Toast.makeText(this, "Invalid course ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        setupSpinner()
        setupClickListeners()
    }

    private fun initializeViews() {
        etAssignmentTitle = findViewById(R.id.et_assignment_title)
        etAssignmentDescription = findViewById(R.id.et_assignment_description)
        spinnerAssignmentType = findViewById(R.id.spinner_assignment_type)
        etInstructions = findViewById(R.id.et_instructions)
        etMaxPoints = findViewById(R.id.et_max_points)
        switchAutoGrading = findViewById(R.id.switch_auto_grading)
        btnSelectDueDate = findViewById(R.id.btn_select_due_date)
        btnSelectDueTime = findViewById(R.id.btn_select_due_time)
        tvSelectedDueDate = findViewById(R.id.tv_selected_due_date)
        cbTextSubmission = findViewById(R.id.cb_text_submission)
        cbFileUpload = findViewById(R.id.cb_file_upload)
        cbVideoSubmission = findViewById(R.id.cb_video_submission)
        cbLinkSubmission = findViewById(R.id.cb_link_submission)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnCreateAssignment = findViewById(R.id.btn_create_assignment)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Assignment"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupSpinner() {
        val assignmentTypes = arrayOf(
            "Essay",
            "Multiple Choice Quiz",
            "Programming Assignment",
            "Research Project",
            "Presentation",
            "Lab Report",
            "Case Study",
            "Discussion Post",
            "Peer Review",
            "Portfolio"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, assignmentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAssignmentType.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSelectDueDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectDueTime.setOnClickListener {
            showTimePicker()
        }

        btnSaveDraft.setOnClickListener {
            lifecycleScope.launch {
                if (SecurityUtils.isOperationAllowed("SAVE_ASSIGNMENT_DRAFT")) {
                    saveAssignment(isDraft = true)
                } else {
                    Toast.makeText(this@AssignmentCreationActivity, "Rate limit exceeded. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCreateAssignment.setOnClickListener {
            lifecycleScope.launch {
                if (SecurityUtils.isOperationAllowed("CREATE_ASSIGNMENT")) {
                    if (validateAssignmentData()) {
                        saveAssignment(isDraft = false)
                    }
                } else {
                    Toast.makeText(this@AssignmentCreationActivity, "Rate limit exceeded. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                if (selectedDueDate == null) {
                    selectedDueDate = Calendar.getInstance()
                }
                selectedDueDate?.set(Calendar.YEAR, selectedYear)
                selectedDueDate?.set(Calendar.MONTH, selectedMonth)
                selectedDueDate?.set(Calendar.DAY_OF_MONTH, selectedDay)
                updateDueDateDisplay()
            },
            year, month, day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        if (selectedDueDate == null) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedDueDate?.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedDueDate?.set(Calendar.MINUTE, selectedMinute)
                selectedDueDate?.set(Calendar.SECOND, 0)
                updateDueDateDisplay()
            },
            hour, minute, true
        )

        timePickerDialog.show()
    }

    private fun updateDueDateDisplay() {
        selectedDueDate?.let { date ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            tvSelectedDueDate.text = "Due: ${dateFormat.format(date.time)}"
        }
    }

    private fun validateAssignmentData(): Boolean {
        val title = etAssignmentTitle.text.toString().trim()
        val description = etAssignmentDescription.text.toString().trim()
        val instructions = etInstructions.text.toString().trim()
        val maxPointsText = etMaxPoints.text.toString().trim()

        if (title.isEmpty()) {
            etAssignmentTitle.error = "Assignment title is required"
            etAssignmentTitle.requestFocus()
            return false
        }

        if (description.isEmpty()) {
            etAssignmentDescription.error = "Assignment description is required"
            etAssignmentDescription.requestFocus()
            return false
        }

        if (instructions.isEmpty()) {
            etInstructions.error = "Instructions are required"
            etInstructions.requestFocus()
            return false
        }

        if (maxPointsText.isEmpty()) {
            etMaxPoints.error = "Maximum points is required"
            etMaxPoints.requestFocus()
            return false
        }

        val maxPoints = maxPointsText.toIntOrNull()
        if (maxPoints == null || maxPoints <= 0) {
            etMaxPoints.error = "Please enter a valid positive number"
            etMaxPoints.requestFocus()
            return false
        }

        if (selectedDueDate == null) {
            Toast.makeText(this, "Please select a due date", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!cbTextSubmission.isChecked && !cbFileUpload.isChecked && 
            !cbVideoSubmission.isChecked && !cbLinkSubmission.isChecked) {
            Toast.makeText(this, "Please select at least one submission format", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private suspend fun saveAssignment(isDraft: Boolean) {
        // Re-verify teacher permissions before saving
        if (!SecurityUtils.canAccessTeacherFeatures()) {
            SecurityUtils.logSecurityEvent("UNAUTHORIZED_ASSIGNMENT_SAVE", "User attempted to save assignment without teacher permissions")
            Toast.makeText(this, "Access denied. Teacher permissions required.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val assignmentId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            // Sanitize input data
            val title = SecurityUtils.sanitizeInput(etAssignmentTitle.text.toString().trim())
            val description = SecurityUtils.sanitizeInput(etAssignmentDescription.text.toString().trim())
            val instructions = SecurityUtils.sanitizeInput(etInstructions.text.toString().trim())
            val maxPoints = etMaxPoints.text.toString().trim().toIntOrNull() ?: 100

            // Get submission formats
            val submissionFormats = mutableListOf<String>()
            if (cbTextSubmission.isChecked) submissionFormats.add("TEXT")
            if (cbFileUpload.isChecked) submissionFormats.add("FILE")
            if (cbVideoSubmission.isChecked) submissionFormats.add("VIDEO")
            if (cbLinkSubmission.isChecked) submissionFormats.add("LINK")

            val assignment = hashMapOf(
                "id" to assignmentId,
                "moduleId" to moduleId,
                "courseId" to courseId,
                "title" to title,
                "description" to description,
                "instructions" to instructions,
                "type" to spinnerAssignmentType.selectedItem.toString(),
                "maxPoints" to maxPoints,
                "dueDate" to (selectedDueDate?.timeInMillis ?: 0L),
                "submissionFormat" to submissionFormats,
                "autoGrading" to switchAutoGrading.isChecked,
                "isDraft" to isDraft,
                "createdAt" to currentTime,
                "updatedAt" to currentTime,
                "createdBy" to SecurityUtils.getCurrentUserRole(),
                "rubric" to emptyList<String>(),
                "resources" to emptyList<String>(),
                "peerReview" to false
            )

            // Save to Firestore
            firestore.collection("courses")
                .document(courseId)
                .collection("assignments")
                .document(assignmentId)
                .set(assignment)
                .addOnSuccessListener {
                    SecurityUtils.logSecurityEvent(
                        "ASSIGNMENT_CREATED",
                        "Assignment created successfully - ID: $assignmentId, Title: $title, IsDraft: $isDraft"
                    )
                    
                    val message = if (isDraft) "Assignment saved as draft" else "Assignment created successfully"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    
                    if (!isDraft) {
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    SecurityUtils.logSecurityEvent(
                        "ASSIGNMENT_CREATION_FAILED",
                        "Failed to create assignment - Error: ${exception.message}"
                    )
                    Toast.makeText(this, "Failed to save assignment: ${exception.message}", Toast.LENGTH_LONG).show()
                }

        } catch (e: Exception) {
            SecurityUtils.logSecurityEvent(
                "ASSIGNMENT_CREATION_ERROR",
                "Error during assignment creation - Error: ${e.message}"
            )
            Toast.makeText(this, "Error creating assignment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        // Check if there's unsaved data
        val hasUnsavedData = etAssignmentTitle.text?.isNotEmpty() == true ||
                           etAssignmentDescription.text?.isNotEmpty() == true ||
                           etInstructions.text?.isNotEmpty() == true

        if (hasUnsavedData) {
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
}