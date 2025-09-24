package com.example.ed

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.example.ed.models.CourseModule

class ModuleCreationActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etModuleTitle: TextInputEditText
    private lateinit var etModuleDescription: TextInputEditText
    private lateinit var etLessonsCount: TextInputEditText
    private lateinit var etDuration: TextInputEditText
    private lateinit var btnSaveModule: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private lateinit var auth: FirebaseAuth
    
    private var moduleId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_creation)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Check if editing existing module
        moduleId = intent.getStringExtra("moduleId")
        isEditMode = moduleId != null

        initializeViews()
        setupToolbar()
        setupClickListeners()
        
        if (isEditMode) {
            loadModuleData()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        etModuleTitle = findViewById(R.id.et_module_title)
        etModuleDescription = findViewById(R.id.et_module_description)
        etLessonsCount = findViewById(R.id.et_lessons_count)
        etDuration = findViewById(R.id.et_duration)
        btnSaveModule = findViewById(R.id.btn_save_module)
        btnCancel = findViewById(R.id.btn_cancel)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Module" else "Create Module"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        btnSaveModule.setOnClickListener {
            saveModule()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadModuleData() {
        // In a real implementation, you would load the module data from Firebase
        // For now, this is a placeholder
        Toast.makeText(this, "Loading module data...", Toast.LENGTH_SHORT).show()
    }

    private fun saveModule() {
        val title = etModuleTitle.text.toString().trim()
        val description = etModuleDescription.text.toString().trim()
        val lessonsCountStr = etLessonsCount.text.toString().trim()
        val durationStr = etDuration.text.toString().trim()

        if (title.isEmpty()) {
            etModuleTitle.error = "Module title is required"
            return
        }

        if (description.isEmpty()) {
            etModuleDescription.error = "Module description is required"
            return
        }

        val lessonsCount = lessonsCountStr.toIntOrNull() ?: 0
        val duration = durationStr.toIntOrNull() ?: 0

        // Create CourseModule object
        val module = CourseModule(
            id = moduleId ?: generateModuleId(),
            title = title,
            description = description,
            estimatedDuration = duration.toLong() * 60 * 1000 // Convert minutes to milliseconds
        )

        // Return result to parent activity
        val resultIntent = Intent()
        resultIntent.putExtra("module", module)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun generateModuleId(): String {
        return "module_${System.currentTimeMillis()}"
    }
}