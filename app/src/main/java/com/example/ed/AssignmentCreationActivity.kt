package com.example.ed

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.ResourceAdapter
import com.example.ed.models.WeeklyAssignment
import com.example.ed.models.WeeklyResource
import com.example.ed.models.AssignmentType
import com.example.ed.models.SubmissionFormat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

class AssignmentCreationActivity : AppCompatActivity() {

    // UI Components
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etInstructions: TextInputEditText
    private lateinit var etMaxPoints: TextInputEditText
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerSubmissionFormat: Spinner
    private lateinit var btnSelectDueDate: MaterialButton
    private lateinit var btnSelectDueTime: MaterialButton
    private lateinit var tvSelectedDueDate: TextView
    private lateinit var rvResources: RecyclerView
    private lateinit var btnAddResource: MaterialButton
    private lateinit var btnSaveDraft: MaterialButton
    private lateinit var btnCreateAssignment: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    // Checkboxes for submission format
    private lateinit var cbTextSubmission: CheckBox
    private lateinit var cbFileUpload: CheckBox
    private lateinit var cbVideoSubmission: CheckBox
    private lateinit var cbLinkSubmission: CheckBox

    // Data
    private val resources = mutableListOf<WeeklyResource>()
    private lateinit var resourceAdapter: ResourceAdapter
    private var selectedDueDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_creation)

        initializeViews()
        setupSpinners()
        setupRecyclerView()
        setupClickListeners()
        setupToolbar()
    }

    private fun initializeViews() {
        etTitle = findViewById(R.id.et_assignment_title)
        etDescription = findViewById(R.id.et_assignment_description)
        etInstructions = findViewById(R.id.et_instructions)
        etMaxPoints = findViewById(R.id.et_max_points)
        spinnerType = findViewById(R.id.spinner_assignment_type)
        btnSelectDueDate = findViewById(R.id.btn_select_due_date)
        btnSelectDueTime = findViewById(R.id.btn_select_due_time)
        tvSelectedDueDate = findViewById(R.id.tv_selected_due_date)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnCreateAssignment = findViewById(R.id.btn_create_assignment)
        toolbar = findViewById(R.id.toolbar)

        // Checkboxes
        cbTextSubmission = findViewById(R.id.cb_text_submission)
        cbFileUpload = findViewById(R.id.cb_file_upload)
        cbVideoSubmission = findViewById(R.id.cb_video_submission)
        cbLinkSubmission = findViewById(R.id.cb_link_submission)

        // Note: RecyclerView for resources might need to be added to layout if not present
        // rvResources = findViewById(R.id.rv_assignment_resources)

        // Set default due date to next week
        selectedDueDate.add(Calendar.WEEK_OF_YEAR, 1)
        updateDateTimeDisplay()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSpinners() {
        // Assignment Type Spinner
        val assignmentTypes = arrayOf("ESSAY", "PROJECT", "PRESENTATION", "RESEARCH", "PRACTICAL", "OTHER")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, assignmentTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
    }

    private fun setupRecyclerView() {
        // Only setup if RecyclerView exists in layout
        val rvResources: RecyclerView? = findViewById(R.id.rv_assignment_resources)
        rvResources?.let { rv ->
            resourceAdapter = ResourceAdapter(resources) { resource ->
                showResourceOptionsDialog(resource)
            }
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = resourceAdapter
        }
    }

    private fun setupClickListeners() {
        btnSelectDueDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectDueTime.setOnClickListener {
            showTimePicker()
        }

        btnSaveDraft.setOnClickListener {
            saveDraft()
        }

        btnCreateAssignment.setOnClickListener {
            createAssignment()
        }

        // Only setup if add resource button exists
        val addResourceBtn: MaterialButton? = findViewById(R.id.btn_add_resource)
        addResourceBtn?.setOnClickListener {
            showAddResourceDialog()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDueDate.set(Calendar.YEAR, year)
                selectedDueDate.set(Calendar.MONTH, month)
                selectedDueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeDisplay()
            },
            selectedDueDate.get(Calendar.YEAR),
            selectedDueDate.get(Calendar.MONTH),
            selectedDueDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDueDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDueDate.set(Calendar.MINUTE, minute)
                updateDateTimeDisplay()
            },
            selectedDueDate.get(Calendar.HOUR_OF_DAY),
            selectedDueDate.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateDateTimeDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val formattedDateTime = "${dateFormat.format(selectedDueDate.time)} at ${timeFormat.format(selectedDueDate.time)}"
        tvSelectedDueDate.text = formattedDateTime
    }

    private fun showAddResourceDialog() {
        Toast.makeText(this, "Add Resource dialog - To be implemented", Toast.LENGTH_SHORT).show()
    }

    private fun showResourceOptionsDialog(resource: WeeklyResource) {
        Toast.makeText(this, "Resource options - To be implemented", Toast.LENGTH_SHORT).show()
    }

    private fun getSelectedSubmissionFormats(): List<SubmissionFormat> {
        val formats = mutableListOf<SubmissionFormat>()

        if (cbTextSubmission.isChecked) formats.add(SubmissionFormat.TEXT)
        if (cbFileUpload.isChecked) formats.add(SubmissionFormat.DOCUMENT)
        if (cbVideoSubmission.isChecked) formats.add(SubmissionFormat.VIDEO)
        if (cbLinkSubmission.isChecked) formats.add(SubmissionFormat.LINK)

        return formats
    }

    private fun saveDraft() {
        Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show()
    }

    private fun createAssignment() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val instructions = etInstructions.text.toString().trim()
        val maxPointsStr = etMaxPoints.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            etTitle.error = "Title is required"
            etTitle.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Description is required"
            etDescription.requestFocus()
            return
        }

        if (maxPointsStr.isEmpty()) {
            etMaxPoints.error = "Max points is required"
            etMaxPoints.requestFocus()
            return
        }

        val maxPoints = maxPointsStr.toIntOrNull()
        if (maxPoints == null || maxPoints <= 0) {
            etMaxPoints.error = "Please enter a valid number of points"
            etMaxPoints.requestFocus()
            return
        }

        val selectedFormats = getSelectedSubmissionFormats()
        if (selectedFormats.isEmpty()) {
            Toast.makeText(this, "Please select at least one submission format", Toast.LENGTH_LONG).show()
            return
        }

        // Create assignment object
        val assignment = WeeklyAssignment(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            instructions = instructions,
            type = AssignmentType.valueOf(spinnerType.selectedItem.toString()),
            submissionFormat = selectedFormats.first(), // Use first selected format as primary
            maxPoints = maxPoints,
            dueDate = selectedDueDate.timeInMillis,
            allowLateSubmission = false, // Default value since switch is not in current layout
            latePenaltyPercentage = 0,
            resources = resources.map { it.toString() },
            rubric = emptyList(),
            isGroupAssignment = false,
            estimatedDuration = 60,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Return result
        val resultIntent = Intent().apply {
            putExtra("assignment_id", assignment.id)
            putExtra("assignment_title", assignment.title)
            putExtra("assignment_description", assignment.description)
            putExtra("assignment_instructions", assignment.instructions)
            putExtra("assignment_max_points", assignment.maxPoints)
            putExtra("assignment_due_date", assignment.dueDate)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}