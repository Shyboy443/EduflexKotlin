package com.example.ed.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ed.models.*
import com.example.ed.activities.QuizCreationActivity
import com.example.ed.activities.AddContentItemActivity
import com.example.ed.adapters.ContentItemAdapter
import com.example.ed.adapters.ActivitiesAdapter
import com.example.ed.R
import java.util.*

class WeeklyContentFragment : Fragment() {

    // UI Components
    private lateinit var spinnerCourse: Spinner
    private lateinit var etWeekNumber: TextInputEditText
    private lateinit var etWeekTitle: TextInputEditText
    private lateinit var etWeekDescription: TextInputEditText
    private lateinit var etLearningObjectives: TextInputEditText
    private lateinit var rvContentItems: RecyclerView
    private lateinit var btnAddContent: MaterialButton
    private lateinit var btnAddQuiz: MaterialButton
    private lateinit var btnAddActivity: MaterialButton
    private lateinit var btnSaveDraft: MaterialButton
    private lateinit var btnPublish: MaterialButton

    // Quiz Section
    private lateinit var cardQuizSection: MaterialCardView
    private lateinit var tvQuizTitle: TextView
    private lateinit var btnEditQuiz: MaterialButton
    private lateinit var btnRemoveQuiz: MaterialButton

    // Activities Section
    private lateinit var rvActivities: RecyclerView
    private lateinit var btnAddAssignment: MaterialButton

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Data
    private var coursesList = mutableListOf<EnhancedCourse>()
    private var contentItems = mutableListOf<ContentItem>()
    private var activities = mutableListOf<WeeklyAssignment>()
    private var currentQuiz: Quiz? = null
    private var currentWeeklyContent: WeeklyContent? = null
    private var isEditMode = false

    // Adapters
    private lateinit var contentAdapter: ContentItemAdapter
    private lateinit var activitiesAdapter: ActivitiesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_weekly_content, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if editing existing content
        val weeklyContentId = arguments?.getString("weekly_content_id")
        isEditMode = weeklyContentId != null

        initializeViews(view)
        setupRecyclerViews()
        setupClickListeners()
        loadTeacherCourses()

        if (isEditMode) {
            loadWeeklyContentForEditing(weeklyContentId!!)
        }

        return view
    }

    private fun initializeViews(view: View) {
        spinnerCourse = view.findViewById(R.id.spinner_course)
        etWeekNumber = view.findViewById(R.id.et_week_number)
        etWeekTitle = view.findViewById(R.id.et_week_title)
        etWeekDescription = view.findViewById(R.id.et_week_description)
        etLearningObjectives = view.findViewById(R.id.et_learning_objectives)
        rvContentItems = view.findViewById(R.id.rv_content_items)
        btnAddContent = view.findViewById(R.id.btn_add_content)
        btnAddQuiz = view.findViewById(R.id.btn_add_quiz)
        btnAddActivity = view.findViewById(R.id.btn_add_activity)
        btnSaveDraft = view.findViewById(R.id.btn_save_draft)
        btnPublish = view.findViewById(R.id.btn_publish)

        // Quiz section
        cardQuizSection = view.findViewById(R.id.card_quiz_section)
        tvQuizTitle = view.findViewById(R.id.tv_quiz_title)
        btnEditQuiz = view.findViewById(R.id.btn_edit_quiz)
        btnRemoveQuiz = view.findViewById(R.id.btn_remove_quiz)

        // Activities section
        rvActivities = view.findViewById(R.id.rv_activities)
        btnAddAssignment = view.findViewById(R.id.btn_add_assignment)
    }

    private fun setupRecyclerViews() {
        // Content Items RecyclerView
        contentAdapter = ContentItemAdapter(
            contentItems,
            onEditClick = { contentItem -> editContentItem(contentItem) },
            onDeleteClick = { contentItem -> deleteContentItem(contentItem) },
            onMoveUpClick = { contentItem -> moveContentItemUp(contentItem) },
            onMoveDownClick = { contentItem -> moveContentItemDown(contentItem) }
        )
        rvContentItems.layoutManager = LinearLayoutManager(requireContext())
        rvContentItems.adapter = contentAdapter

        // Activities RecyclerView
        activitiesAdapter = ActivitiesAdapter(activities) { position ->
            removeActivity(position)
        }
        rvActivities.layoutManager = LinearLayoutManager(requireContext())
        rvActivities.adapter = activitiesAdapter
    }

    private fun setupClickListeners() {
        btnAddContent.setOnClickListener {
            showAddContentDialog()
        }

        btnAddQuiz.setOnClickListener {
            if (currentQuiz == null) {
                createNewQuiz()
            } else {
                editQuiz()
            }
        }

        btnAddActivity.setOnClickListener {
            showAddActivityDialog()
        }

        btnAddAssignment.setOnClickListener {
            showAddActivityDialog()
        }

        btnEditQuiz.setOnClickListener {
            editQuiz()
        }

        btnRemoveQuiz.setOnClickListener {
            removeQuiz()
        }

        btnSaveDraft.setOnClickListener {
            saveWeeklyContent(false)
        }

        btnPublish.setOnClickListener {
            saveWeeklyContent(true)
        }
    }

    private fun loadTeacherCourses() {
        val currentUser = auth.currentUser ?: return

        db.collection("courses")
            .whereEqualTo("instructor.id", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                coursesList.clear()
                for (document in documents) {
                    val course = document.toObject(EnhancedCourse::class.java)
                    coursesList.add(course)
                }
                setupCourseSpinner()
            }
            .addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load courses: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCourseSpinner() {
        val courseNames = coursesList.map { it.title }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courseNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourse.adapter = adapter
    }

    private fun showAddContentDialog() {
        val intent = Intent(requireContext(), AddContentItemActivity::class.java)
        intent.putExtra("order", contentItems.size)
        startActivityForResult(intent, REQUEST_ADD_CONTENT)
    }

    private fun showAddActivityDialog() {
        // TODO: Implement AssignmentCreationActivity
        Toast.makeText(requireContext(), "Add Assignment - Coming Soon", Toast.LENGTH_SHORT).show()
        // val selectedCourse = getSelectedCourse() ?: return
        // val intent = Intent(requireContext(), AssignmentCreationActivity::class.java)
        // intent.putExtra("course_id", selectedCourse.id)
        // intent.putExtra("week_number", etWeekNumber.text.toString().toIntOrNull() ?: 1)
        // startActivityForResult(intent, REQUEST_ADD_ACTIVITY)
    }

    private fun createNewQuiz() {
        val selectedCourse = getSelectedCourse() ?: return

        val intent = Intent(requireContext(), QuizCreationActivity::class.java)
        intent.putExtra("course_id", selectedCourse.id)
        intent.putExtra("week_number", etWeekNumber.text.toString().toIntOrNull() ?: 1)
        startActivityForResult(intent, REQUEST_CREATE_QUIZ)
    }

    private fun editQuiz() {
        currentQuiz?.let { quiz ->
            val intent = Intent(requireContext(), QuizCreationActivity::class.java)
            intent.putExtra("quiz_id", quiz.id)
            startActivityForResult(intent, REQUEST_EDIT_QUIZ)
        }
    }

    private fun removeQuiz() {
        currentQuiz = null
        updateQuizSection()
    }

    private fun updateQuizSection() {
        if (currentQuiz != null) {
            cardQuizSection.visibility = View.VISIBLE
            tvQuizTitle.text = currentQuiz!!.title
            btnAddQuiz.text = "Edit Quiz"
        } else {
            cardQuizSection.visibility = View.GONE
            btnAddQuiz.text = "Add Quiz"
        }
    }

    private fun removeContentItem(position: Int) {
        contentItems.removeAt(position)
        contentAdapter.notifyItemRemoved(position)
    }

    private fun removeActivity(position: Int) {
        activities.removeAt(position)
        activitiesAdapter.notifyItemRemoved(position)
    }

    private fun getSelectedCourse(): EnhancedCourse? {
        val selectedPosition = spinnerCourse.selectedItemPosition
        return if (selectedPosition >= 0 && selectedPosition < coursesList.size) {
            coursesList[selectedPosition]
        } else null
    }

    private fun saveWeeklyContent(publish: Boolean) {
        if (!validateInput()) return

        val selectedCourse = getSelectedCourse() ?: return
        val currentUser = auth.currentUser ?: return

        val weeklyContent = WeeklyContent(
            id = currentWeeklyContent?.id ?: UUID.randomUUID().toString(),
            courseId = selectedCourse.id,
            weekNumber = etWeekNumber.text.toString().toInt(),
            title = etWeekTitle.text.toString().trim(),
            description = etWeekDescription.text.toString().trim(),
            learningObjectives = etLearningObjectives.text.toString()
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() },
            contentItems = contentItems,
            assignments = activities,
            quiz = currentQuiz,
            isPublished = publish,
            createdBy = currentUser.uid,
            updatedAt = System.currentTimeMillis()
        )

        val progressDialog = android.app.ProgressDialog(requireContext())
        progressDialog.setMessage("Saving weekly content...")
        progressDialog.show()

        db.collection("weekly_content")
            .document(weeklyContent.id)
            .set(weeklyContent)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                progressDialog.dismiss()
                Toast.makeText(requireContext(),
                    if (publish) "Weekly content published successfully!"
                    else "Weekly content saved as draft!",
                    Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(): Boolean {
        if (getSelectedCourse() == null) {
            Toast.makeText(requireContext(), "Please select a course", Toast.LENGTH_SHORT).show()
            return false
        }

        if (etWeekNumber.text.toString().trim().isEmpty()) {
            etWeekNumber.error = "Week number is required"
            return false
        }

        if (etWeekTitle.text.toString().trim().isEmpty()) {
            etWeekTitle.error = "Week title is required"
            return false
        }

        if (etWeekDescription.text.toString().trim().isEmpty()) {
            etWeekDescription.error = "Week description is required"
            return false
        }

        return true
    }

    private fun loadWeeklyContentForEditing(weeklyContentId: String) {
        db.collection("weekly_content")
            .document(weeklyContentId)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                if (document.exists()) {
                    currentWeeklyContent = document.toObject(WeeklyContent::class.java)
                    populateFields()
                }
            }
            .addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load content: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateFields() {
        currentWeeklyContent?.let { content ->
            etWeekNumber.setText(content.weekNumber.toString())
            etWeekTitle.setText(content.title)
            etWeekDescription.setText(content.description)
            etLearningObjectives.setText(content.learningObjectives.joinToString("\n"))

            contentItems.clear()
            contentItems.addAll(content.contentItems)
            contentAdapter.notifyDataSetChanged()

            activities.clear()
            activities.addAll(content.assignments)
            activitiesAdapter.notifyDataSetChanged()

            currentQuiz = content.quiz
            updateQuizSection()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_ADD_CONTENT -> {
                    // Handle content item result
                    val contentItem = data.getParcelableExtra<ContentItem>("content_item")
                    contentItem?.let {
                        contentItems.add(it)
                        contentAdapter.notifyItemInserted(contentItems.size - 1)
                        Toast.makeText(requireContext(), "Content item added successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_ADD_ACTIVITY -> {
                    // Reconstruct WeeklyAssignment from individual extras
                    val assignmentId = data.getStringExtra("assignment_id") ?: ""
                    val assignmentTitle = data.getStringExtra("assignment_title") ?: ""
                    val assignmentDescription = data.getStringExtra("assignment_description") ?: ""
                    val assignmentInstructions = data.getStringExtra("assignment_instructions") ?: ""
                    val assignmentMaxPoints = data.getIntExtra("assignment_max_points", 100)
                    val assignmentDueDate = data.getLongExtra("assignment_due_date", 0)

                    if (assignmentTitle.isNotEmpty()) {
                        val assignment = WeeklyAssignment(
                            id = assignmentId,
                            title = assignmentTitle,
                            description = assignmentDescription,
                            instructions = assignmentInstructions,
                            maxPoints = assignmentMaxPoints,
                            dueDate = assignmentDueDate
                        )
                        activities.add(assignment)
                        activitiesAdapter.notifyItemInserted(activities.size - 1)
                        Toast.makeText(requireContext(), "Assignment added successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_EDIT_CONTENT -> {
                    // Handle edited content item
                    val contentItem = data.getParcelableExtra<ContentItem>("content_item")
                    contentItem?.let { editedItem ->
                        val index = contentItems.indexOfFirst { it.id == editedItem.id }
                        if (index != -1) {
                            contentItems[index] = editedItem
                            contentAdapter.notifyItemChanged(index)
                            Toast.makeText(requireContext(), "Content item updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                REQUEST_CREATE_QUIZ, REQUEST_EDIT_QUIZ -> {
                    val quizId = data.getStringExtra("quiz_id")
                    if (quizId != null) {
                        loadQuizById(quizId)
                    }
                }
            }
        }
    }

    private fun loadQuizById(quizId: String) {
        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                if (document.exists()) {
                    currentQuiz = document.toObject(Quiz::class.java)
                    updateQuizSection()
                }
            }
    }

    private fun editContentItem(contentItem: ContentItem) {
        val intent = Intent(requireContext(), AddContentItemActivity::class.java)
        intent.putExtra("content_item", contentItem as android.os.Parcelable)
        intent.putExtra("order", contentItems.indexOf(contentItem))
        startActivityForResult(intent, REQUEST_EDIT_CONTENT)
    }

    private fun deleteContentItem(contentItem: ContentItem) {
        val position = contentItems.indexOf(contentItem)
        if (position != -1) {
            contentItems.removeAt(position)
            contentAdapter.notifyItemRemoved(position)
            Toast.makeText(requireContext(), "Content item removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveContentItemUp(contentItem: ContentItem) {
        val position = contentItems.indexOf(contentItem)
        if (position > 0) {
            contentItems.removeAt(position)
            contentItems.add(position - 1, contentItem)
            contentAdapter.notifyItemMoved(position, position - 1)
        }
    }

    private fun moveContentItemDown(contentItem: ContentItem) {
        val position = contentItems.indexOf(contentItem)
        if (position < contentItems.size - 1) {
            contentItems.removeAt(position)
            contentItems.add(position + 1, contentItem)
            contentAdapter.notifyItemMoved(position, position + 1)
        }
    }

    companion object {
        private const val REQUEST_ADD_CONTENT = 1001
        private const val REQUEST_EDIT_CONTENT = 1005
        private const val REQUEST_ADD_ACTIVITY = 1002
        private const val REQUEST_CREATE_QUIZ = 1003
        private const val REQUEST_EDIT_QUIZ = 1004
    }
}
