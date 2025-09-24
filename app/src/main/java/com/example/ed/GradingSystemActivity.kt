package com.example.ed

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.SubmissionAdapter
import com.example.ed.models.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class GradingSystemActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var assignmentSpinner: Spinner
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var submissionsRecyclerView: RecyclerView
    private lateinit var gradingProgressCard: MaterialCardView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var progressText: TextView
    private lateinit var aiGradingButton: MaterialButton
    private lateinit var exportGradesButton: MaterialButton
    private lateinit var gradingStatsLayout: LinearLayout
    
    private lateinit var submissionAdapter: SubmissionAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var currentAssignmentId: String? = null
    private var submissions = mutableListOf<StudentSubmission>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grading_system)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupSpinner()
        setupFilters()
        setupButtons()
        loadAssignments()
        loadGradingStats()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        assignmentSpinner = findViewById(R.id.assignmentSpinner)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        submissionsRecyclerView = findViewById(R.id.submissionsRecyclerView)
        gradingProgressCard = findViewById(R.id.gradingProgressCard)
        progressIndicator = findViewById(R.id.progressIndicator)
        progressText = findViewById(R.id.progressText)
        aiGradingButton = findViewById(R.id.aiGradingButton)
        exportGradesButton = findViewById(R.id.exportGradesButton)
        gradingStatsLayout = findViewById(R.id.gradingStatsLayout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        submissionAdapter = SubmissionAdapter(
            onSubmissionClick = { submission -> openSubmissionDetails(submission) },
            onQuickGrade = { submission, grade -> quickGradeSubmission(submission, grade) }
        )
        
        submissionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GradingSystemActivity)
            adapter = submissionAdapter
        }
    }

    private fun setupSpinner() {
        // Mock assignment data
        val assignments = listOf(
            "Select Assignment",
            "Quiz 1: Android Basics",
            "Assignment 2: RecyclerView Implementation",
            "Project: Weather App",
            "Final Exam"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, assignments)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assignmentSpinner.adapter = adapter
        
        assignmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    currentAssignmentId = "assignment_$position"
                    loadSubmissions(currentAssignmentId!!)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFilters() {
        val filters = listOf("All", "Ungraded", "Graded", "Needs Review", "Late Submissions")
        
        filters.forEach { filter ->
            val chip = Chip(this).apply {
                text = filter
                isCheckable = true
                if (filter == "All") isChecked = true
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        // Uncheck other chips
                        for (i in 0 until filterChipGroup.childCount) {
                            val otherChip = filterChipGroup.getChildAt(i) as Chip
                            if (otherChip != this) {
                                otherChip.isChecked = false
                            }
                        }
                        applyFilter(filter)
                    }
                }
            }
            filterChipGroup.addView(chip)
        }
    }

    private fun setupButtons() {
        aiGradingButton.setOnClickListener {
            showAIGradingDialog()
        }
        
        exportGradesButton.setOnClickListener {
            exportGrades()
        }
    }

    private fun loadAssignments() {
        // Mock data - in real app, load from Firebase
        // This would typically fetch assignments from Firestore
    }

    private fun loadSubmissions(assignmentId: String) {
        // Mock submission data
        submissions.clear()
        
        val mockSubmissions = listOf(
            StudentSubmission(
                id = "sub1",
                studentId = "student1",
                studentName = "Alice Johnson",
                assignmentId = assignmentId,
                submittedAt = Date(),
                content = "My solution to the RecyclerView implementation...",
                attachments = listOf("solution.zip", "screenshots.png"),
                status = SubmissionStatus.SUBMITTED,
                grade = null,
                feedback = null,
                gradedAt = null,
                gradedBy = null
            ),
            StudentSubmission(
                id = "sub2",
                studentId = "student2",
                studentName = "Bob Smith",
                assignmentId = assignmentId,
                submittedAt = Date(System.currentTimeMillis() - 86400000), // 1 day ago
                content = "Here's my approach to the weather app project...",
                attachments = listOf("weather_app.apk", "source_code.zip"),
                status = SubmissionStatus.GRADED,
                grade = 85.0,
                feedback = "Good implementation, but could improve error handling.",
                gradedAt = Date(),
                gradedBy = "teacher_id"
            ),
            StudentSubmission(
                id = "sub3",
                studentId = "student3",
                studentName = "Carol Davis",
                assignmentId = assignmentId,
                submittedAt = Date(System.currentTimeMillis() - 3600000), // 1 hour ago
                content = "My quiz answers are attached...",
                attachments = listOf("quiz_answers.pdf"),
                status = SubmissionStatus.NEEDS_REVIEW,
                grade = 78.0,
                feedback = "Please review the flagged answers.",
                gradedAt = Date(),
                gradedBy = "ai_grader"
            )
        )
        
        submissions.addAll(mockSubmissions)
        submissionAdapter.updateSubmissions(submissions)
        updateGradingProgress()
    }

    private fun applyFilter(filter: String) {
        val filteredSubmissions = when (filter) {
            "Ungraded" -> submissions.filter { it.status == SubmissionStatus.SUBMITTED }
            "Graded" -> submissions.filter { it.status == SubmissionStatus.GRADED }
            "Needs Review" -> submissions.filter { it.status == SubmissionStatus.NEEDS_REVIEW }
            "Late Submissions" -> submissions.filter { 
                // Mock logic for late submissions
                it.submittedAt.time > System.currentTimeMillis() - 86400000 * 2
            }
            else -> submissions
        }
        
        submissionAdapter.updateSubmissions(filteredSubmissions)
    }

    private fun updateGradingProgress() {
        val totalSubmissions = submissions.size
        val gradedSubmissions = submissions.count { it.status == SubmissionStatus.GRADED }
        
        if (totalSubmissions > 0) {
            val progress = (gradedSubmissions * 100) / totalSubmissions
            progressIndicator.progress = progress
            progressText.text = "$gradedSubmissions of $totalSubmissions graded ($progress%)"
            gradingProgressCard.visibility = View.VISIBLE
        } else {
            gradingProgressCard.visibility = View.GONE
        }
    }

    private fun openSubmissionDetails(submission: StudentSubmission) {
        // Create detailed grading dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_grade_submission, null)
        
        val studentNameText = dialogView.findViewById<TextView>(R.id.studentName)
        val submissionContentText = dialogView.findViewById<TextView>(R.id.submissionContent)
        val gradeInput = dialogView.findViewById<TextInputEditText>(R.id.gradeInput)
        val feedbackInput = dialogView.findViewById<TextInputEditText>(R.id.feedbackInput)
        val aiSuggestButton = dialogView.findViewById<MaterialButton>(R.id.aiSuggestButton)
        
        studentNameText.text = submission.studentName
        submissionContentText.text = submission.content
        gradeInput.setText(submission.grade?.toString() ?: "")
        feedbackInput.setText(submission.feedback ?: "")
        
        aiSuggestButton.setOnClickListener {
            generateAIFeedback(submission, feedbackInput)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Grade Submission")
            .setView(dialogView)
            .setPositiveButton("Save Grade") { _, _ ->
                val grade = gradeInput.text.toString().toDoubleOrNull()
                val feedback = feedbackInput.text.toString()
                saveGrade(submission, grade, feedback)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun quickGradeSubmission(submission: StudentSubmission, grade: Double) {
        saveGrade(submission, grade, "Quick grade applied")
    }

    private fun saveGrade(submission: StudentSubmission, grade: Double?, feedback: String) {
        // Update submission
        submission.grade = grade
        submission.feedback = feedback
        submission.status = SubmissionStatus.GRADED
        submission.gradedAt = Date()
        submission.gradedBy = "current_teacher_id"
        
        // Update in Firebase (mock)
        // firestore.collection("submissions").document(submission.id).update(...)
        
        // Refresh UI
        submissionAdapter.notifyDataSetChanged()
        updateGradingProgress()
        
        Toast.makeText(this, "Grade saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun generateAIFeedback(submission: StudentSubmission, feedbackInput: TextInputEditText) {
        // Mock AI feedback generation
        val aiFeedback = when ((1..3).random()) {
            1 -> "Good work! Your implementation shows understanding of the concepts. Consider adding more error handling for edge cases."
            2 -> "Well structured code. The logic is clear and follows best practices. Minor suggestion: add more comments for complex sections."
            else -> "Creative approach to the problem. The solution works but could be optimized for better performance. Great effort overall!"
        }
        
        feedbackInput.setText(aiFeedback)
        Toast.makeText(this, "AI feedback generated", Toast.LENGTH_SHORT).show()
    }

    private fun showAIGradingDialog() {
        val options = arrayOf(
            "Grade all ungraded submissions",
            "Generate feedback for graded submissions",
            "Review flagged submissions",
            "Batch grade by criteria"
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle("AI Grading Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> performAIGrading()
                    1 -> generateBatchFeedback()
                    2 -> reviewFlaggedSubmissions()
                    3 -> showBatchGradingCriteria()
                }
            }
            .show()
    }

    private fun performAIGrading() {
        val ungradedSubmissions = submissions.filter { it.status == SubmissionStatus.SUBMITTED }
        
        ungradedSubmissions.forEach { submission ->
            // Mock AI grading
            val aiGrade = (70..95).random().toDouble()
            val needsReview = aiGrade < 75 || (1..10).random() <= 2 // 20% chance of review
            
            submission.grade = aiGrade
            submission.status = if (needsReview) SubmissionStatus.NEEDS_REVIEW else SubmissionStatus.GRADED
            submission.gradedAt = Date()
            submission.gradedBy = "ai_grader"
            submission.feedback = "AI-generated feedback based on rubric criteria."
        }
        
        submissionAdapter.notifyDataSetChanged()
        updateGradingProgress()
        
        Toast.makeText(this, "AI grading completed for ${ungradedSubmissions.size} submissions", Toast.LENGTH_LONG).show()
    }

    private fun generateBatchFeedback() {
        Toast.makeText(this, "Generating batch feedback...", Toast.LENGTH_SHORT).show()
    }

    private fun reviewFlaggedSubmissions() {
        val flaggedSubmissions = submissions.filter { it.status == SubmissionStatus.NEEDS_REVIEW }
        if (flaggedSubmissions.isEmpty()) {
            Toast.makeText(this, "No submissions need review", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "${flaggedSubmissions.size} submissions need review", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBatchGradingCriteria() {
        Toast.makeText(this, "Batch grading criteria dialog would open here", Toast.LENGTH_SHORT).show()
    }

    private fun exportGrades() {
        // Mock export functionality
        Toast.makeText(this, "Grades exported to CSV file", Toast.LENGTH_SHORT).show()
    }

    private fun loadGradingStats() {
        // Mock stats data
        val statsData = mapOf(
            "Total Assignments" to "12",
            "Avg Grade" to "82.5",
            "Completion Rate" to "94%",
            "Pending Reviews" to "3"
        )
        
        statsData.forEach { (label, value) ->
            val statView = layoutInflater.inflate(R.layout.item_grading_stat, gradingStatsLayout, false)
            statView.findViewById<TextView>(R.id.statLabel).text = label
            statView.findViewById<TextView>(R.id.statValue).text = value
            gradingStatsLayout.addView(statView)
        }
    }
}