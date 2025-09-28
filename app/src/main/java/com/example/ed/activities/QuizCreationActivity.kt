package com.example.ed.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.adapters.QuestionAdapter
import com.example.ed.models.Question
import com.example.ed.models.QuestionDifficulty
import com.example.ed.models.QuestionType
import com.example.ed.models.Quiz
import com.example.ed.models.QuizDifficulty
import com.example.ed.services.AIQuizService
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class QuizCreationActivity : AppCompatActivity() {

    private lateinit var etQuizTitle: TextInputEditText
    private lateinit var etQuizDescription: TextInputEditText
    private lateinit var etTimeLimit: TextInputEditText
    private lateinit var etPassingScore: TextInputEditText
    private lateinit var etMaxAttempts: TextInputEditText
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var rvQuestions: RecyclerView
    private lateinit var btnAddQuestion: MaterialButton
    private lateinit var btnGenerateWithAI: MaterialButton
    private lateinit var btnSaveQuiz: MaterialButton
    private lateinit var btnPublishQuiz: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvNoQuestions: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var questionAdapter: QuestionAdapter
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // AI Service
    private lateinit var aiQuizService: AIQuizService
    
    // Data
    private val questions = mutableListOf<Question>()
    private var courseId: String = ""
    private var weekNumber: Int = 0
    private var isEditMode = false
    private var editingQuizId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_creation)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Initialize AI Service
        aiQuizService = AIQuizService()
        
        // Get data from intent
        courseId = intent.getStringExtra("course_id") ?: ""
        weekNumber = intent.getIntExtra("week_number", 0)
        editingQuizId = intent.getStringExtra("quiz_id")
        isEditMode = editingQuizId != null

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupDifficultySpinner()
        setupTags()
        updateQuestionsVisibility()
        updateTotalPoints()
        
        if (isEditMode) {
            loadQuizForEditing(editingQuizId!!)
        }
    }

    private fun initializeViews() {
        etQuizTitle = findViewById(R.id.etQuizTitle)
        etQuizDescription = findViewById(R.id.etQuizDescription)
        etTimeLimit = findViewById(R.id.etTimeLimit)
        etPassingScore = findViewById(R.id.etPassingScore)
        etMaxAttempts = findViewById(R.id.etMaxAttempts)
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty)
        chipGroupTags = findViewById(R.id.chipGroupTags)
        rvQuestions = findViewById(R.id.rvQuestions)
        btnAddQuestion = findViewById(R.id.btnAddQuestion)
        btnGenerateWithAI = findViewById(R.id.btnGenerateWithAI)
        btnSaveQuiz = findViewById(R.id.btnSaveQuiz)
        btnPublishQuiz = findViewById(R.id.btnPublishQuiz)
        btnBack = findViewById(R.id.btnBack)
        tvNoQuestions = findViewById(R.id.tvNoQuestions)
        tvTotalPoints = findViewById(R.id.tvTotalPoints)
        
        // Set title based on mode
        title = if (isEditMode) "Edit Quiz" else "Create Quiz"
    }

    private fun setupRecyclerView() {
        questionAdapter = QuestionAdapter(
            questions = questions,
            onEditQuestion = { position -> editQuestion(position) },
            onDeleteQuestion = { position -> deleteQuestion(position) }
        )
        rvQuestions.layoutManager = LinearLayoutManager(this)
        rvQuestions.adapter = questionAdapter
        rvQuestions.isNestedScrollingEnabled = false
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnAddQuestion.setOnClickListener { showAddQuestionDialog() }
        btnGenerateWithAI.setOnClickListener { showAIGenerationDialog() }
        btnSaveQuiz.setOnClickListener { saveQuiz(false) }
        btnPublishQuiz.setOnClickListener { saveQuiz(true) }
    }

    private fun setupDifficultySpinner() {
        val difficulties = QuizDifficulty.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = adapter
    }

    private fun showAddQuestionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        val etQuestionText = dialogView.findViewById<TextInputEditText>(R.id.etQuestionText)
        val spinnerQuestionType = dialogView.findViewById<Spinner>(R.id.spinnerQuestionType)
        val etOption1 = dialogView.findViewById<TextInputEditText>(R.id.etOption1)
        val etOption2 = dialogView.findViewById<TextInputEditText>(R.id.etOption2)
        val etOption3 = dialogView.findViewById<TextInputEditText>(R.id.etOption3)
        val etOption4 = dialogView.findViewById<TextInputEditText>(R.id.etOption4)
        val etCorrectAnswer = dialogView.findViewById<TextInputEditText>(R.id.etCorrectAnswer)
        val etExplanation = dialogView.findViewById<TextInputEditText>(R.id.etExplanation)  // 修正：findViewById 而非 findById
        val etPoints = dialogView.findViewById<TextInputEditText>(R.id.etPoints)

        val questionTypes = QuestionType.values().map { it.name }
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questionTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuestionType.adapter = typeAdapter

        spinnerQuestionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = QuestionType.values().getOrNull(position) ?: QuestionType.MULTIPLE_CHOICE
                val isMultipleChoice = selectedType == QuestionType.MULTIPLE_CHOICE || selectedType == QuestionType.MULTIPLE_SELECT
                etOption1.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
                etOption2.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
                etOption3.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
                etOption4.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Question")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                addQuestionFromDialog(
                    etQuestionText, spinnerQuestionType, etOption1, etOption2,
                    etOption3, etOption4, etCorrectAnswer, etExplanation, etPoints
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addQuestionFromDialog(
        etQuestionText: TextInputEditText,
        spinnerQuestionType: Spinner,
        etOption1: TextInputEditText,
        etOption2: TextInputEditText,
        etOption3: TextInputEditText,
        etOption4: TextInputEditText,
        etCorrectAnswer: TextInputEditText,
        etExplanation: TextInputEditText,
        etPoints: TextInputEditText
    ) {
        val questionText = etQuestionText.text.toString().trim()
        val questionType = try {
            QuestionType.valueOf(spinnerQuestionType.selectedItem.toString())
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Invalid question type selected", Toast.LENGTH_SHORT).show()
            return
        }
        val options = listOf(
            etOption1.text.toString().trim(),
            etOption2.text.toString().trim(),
            etOption3.text.toString().trim(),
            etOption4.text.toString().trim()
        ).filter { it.isNotEmpty() }
        val correctAnswer = etCorrectAnswer.text.toString().trim()
        val explanation = etExplanation.text.toString().trim()
        val points = etPoints.text.toString().toIntOrNull() ?: 1

        if (questionText.isEmpty()) {
            Toast.makeText(this, "Please enter question text", Toast.LENGTH_SHORT).show()
            return
        }
        if (correctAnswer.isEmpty() && questionType != QuestionType.ESSAY) {
            Toast.makeText(this, "Please enter correct answer", Toast.LENGTH_SHORT).show()
            return
        }
        if ((questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.MULTIPLE_SELECT) && options.size < 2) {
            Toast.makeText(this, "Multiple choice questions need at least 2 options", Toast.LENGTH_SHORT).show()
            return
        }
        if ((questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.MULTIPLE_SELECT) && correctAnswer.isNotEmpty() && !options.contains(correctAnswer)) {
            Toast.makeText(this, "Correct answer must be one of the options", Toast.LENGTH_SHORT).show()
            return
        }

        val question = Question(
            id = UUID.randomUUID().toString(),
            quizId = "", // Will be set when quiz is saved
            type = questionType,
            question = questionText,
            options = options,
            correctAnswers = if (correctAnswer.isNotEmpty()) listOf(correctAnswer) else emptyList(),
            explanation = explanation,
            points = points,
            difficulty = QuestionDifficulty.MEDIUM,
            topic = "",
            order = questions.size,
            aiGenerated = false,
            createdAt = System.currentTimeMillis()
        )
        questions.add(question)
        questionAdapter.notifyItemInserted(questions.size - 1)
        updateQuestionsVisibility()
    }

    private fun editQuestion(position: Int) {
        if (position >= questions.size) return
        val question = questions[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        val etQuestionText = dialogView.findViewById<TextInputEditText>(R.id.etQuestionText)
        val spinnerQuestionType = dialogView.findViewById<Spinner>(R.id.spinnerQuestionType)
        val etOption1 = dialogView.findViewById<TextInputEditText>(R.id.etOption1)
        val etOption2 = dialogView.findViewById<TextInputEditText>(R.id.etOption2)
        val etOption3 = dialogView.findViewById<TextInputEditText>(R.id.etOption3)
        val etOption4 = dialogView.findViewById<TextInputEditText>(R.id.etOption4)
        val etCorrectAnswer = dialogView.findViewById<TextInputEditText>(R.id.etCorrectAnswer)
        val etExplanation = dialogView.findViewById<TextInputEditText>(R.id.etExplanation)
        val etPoints = dialogView.findViewById<TextInputEditText>(R.id.etPoints)

        etQuestionText.setText(question.question)
        etCorrectAnswer.setText(question.correctAnswers.firstOrNull() ?: "")
        etExplanation.setText(question.explanation)
        etPoints.setText(question.points.toString())
        question.options.getOrNull(0)?.let { etOption1.setText(it) }
        question.options.getOrNull(1)?.let { etOption2.setText(it) }
        question.options.getOrNull(2)?.let { etOption3.setText(it) }
        question.options.getOrNull(3)?.let { etOption4.setText(it) }

        val questionTypes = QuestionType.values().map { it.name }
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questionTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuestionType.adapter = typeAdapter
        val typeIndex = questionTypes.indexOf(question.type.name)
        if (typeIndex >= 0) {
            spinnerQuestionType.setSelection(typeIndex)
        }

        spinnerQuestionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = QuestionType.values().getOrNull(position) ?: QuestionType.MULTIPLE_CHOICE
                val isMultipleChoice = selectedType == QuestionType.MULTIPLE_CHOICE || selectedType == QuestionType.MULTIPLE_SELECT
                etOption1.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
                etOption2.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
                etOption3.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
                etOption4.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Question")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                updateQuestionFromDialog(
                    position, question, etQuestionText, spinnerQuestionType,
                    etOption1, etOption2, etOption3, etOption4,
                    etCorrectAnswer, etExplanation, etPoints
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateQuestionFromDialog(
        position: Int,
        originalQuestion: Question,
        etQuestionText: TextInputEditText,
        spinnerQuestionType: Spinner,
        etOption1: TextInputEditText,
        etOption2: TextInputEditText,
        etOption3: TextInputEditText,
        etOption4: TextInputEditText,
        etCorrectAnswer: TextInputEditText,
        etExplanation: TextInputEditText,
        etPoints: TextInputEditText
    ) {
        val questionText = etQuestionText.text.toString().trim()
        val questionType = try {
            QuestionType.valueOf(spinnerQuestionType.selectedItem.toString())
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Invalid question type selected", Toast.LENGTH_SHORT).show()
            return
        }
        val options = listOf(
            etOption1.text.toString().trim(),
            etOption2.text.toString().trim(),
            etOption3.text.toString().trim(),
            etOption4.text.toString().trim()
        ).filter { it.isNotEmpty() }
        val correctAnswer = etCorrectAnswer.text.toString().trim()
        val explanation = etExplanation.text.toString().trim()
        val points = etPoints.text.toString().toIntOrNull() ?: 1

        if (questionText.isEmpty()) {
            Toast.makeText(this, "Please enter question text", Toast.LENGTH_SHORT).show()
            return
        }
        if (correctAnswer.isEmpty() && questionType != QuestionType.ESSAY) {
            Toast.makeText(this, "Please enter correct answer", Toast.LENGTH_SHORT).show()
            return
        }
        if ((questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.MULTIPLE_SELECT) && options.size < 2) {
            Toast.makeText(this, "Multiple choice questions need at least 2 options", Toast.LENGTH_SHORT).show()
            return
        }
        if ((questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.MULTIPLE_SELECT) && correctAnswer.isNotEmpty() && !options.contains(correctAnswer)) {
            Toast.makeText(this, "Correct answer must be one of the options", Toast.LENGTH_SHORT).show()
            return
        }

        questions[position] = originalQuestion.copy(
            question = questionText,
            type = questionType,
            options = options,
            correctAnswers = if (correctAnswer.isNotEmpty()) listOf(correctAnswer) else emptyList(),
            explanation = explanation,
            points = points,
            order = position,
            aiGenerated = false,
            createdAt = System.currentTimeMillis()
        )
        questionAdapter.notifyItemChanged(position)
        updateQuestionsVisibility()
    }

    private fun deleteQuestion(position: Int) {
        if (position >= questions.size) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Question")
            .setMessage("Are you sure you want to delete this question?")
            .setPositiveButton("Delete") { _, _ ->
                questions.removeAt(position)
                // Update order for remaining questions
                questions.forEachIndexed { index, q ->
                    questions[index] = q.copy(order = index)
                }
                questionAdapter.notifyItemRemoved(position)
                if (position < questions.size) {
                    questionAdapter.notifyItemRangeChanged(position, questions.size - position)
                }
                updateQuestionsVisibility()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupTags() {
        val defaultTags = listOf("Assessment", "Practice", "Exam", "Review", "Homework")
        defaultTags.forEach { tag ->
            val chip = Chip(this)
            chip.text = tag
            chip.isCheckable = true
            chipGroupTags.addView(chip)
        }
    }
    
    private fun showAIGenerationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_quiz_generation, null)
        val etTopic = dialogView.findViewById<TextInputEditText>(R.id.et_topic)
        val etNumberOfQuestions = dialogView.findViewById<TextInputEditText>(R.id.et_question_count)
        val spinnerQuestionType = dialogView.findViewById<Spinner>(R.id.spinner_question_type)
        val spinnerDifficulty = dialogView.findViewById<Spinner>(R.id.spinner_difficulty)
        
        // Setup spinners
        val questionTypes = arrayOf("Mixed", "Multiple Choice", "True/False", "Short Answer")
        spinnerQuestionType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questionTypes)
        
        val difficulties = QuestionDifficulty.values().map { it.name }
        spinnerDifficulty.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Generate Quiz with AI")
            .setView(dialogView)
            .setPositiveButton("Generate") { _, _ ->
                generateQuestionsWithAI(
                    etTopic.text.toString(),
                    etNumberOfQuestions.text.toString().toIntOrNull() ?: 5,
                    spinnerQuestionType.selectedItem.toString(),
                    QuestionDifficulty.valueOf(spinnerDifficulty.selectedItem.toString())
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun generateQuestionsWithAI(topic: String, numberOfQuestions: Int, questionType: String, difficulty: QuestionDifficulty) {
        if (topic.isEmpty()) {
            Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
            return
        }
        
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Generating questions with AI...")
        progressDialog.show()
        
        aiQuizService.generateQuestions(
            topic = topic,
            numberOfQuestions = numberOfQuestions,
            questionType = questionType,
            difficulty = difficulty,
            courseContext = etQuizTitle.text.toString()
        ) { generatedQuestions ->
            runOnUiThread {
                progressDialog.dismiss()
                if (generatedQuestions != null && generatedQuestions.isNotEmpty()) {
                    questions.addAll(generatedQuestions)
                    questionAdapter.notifyDataSetChanged()
                    updateQuestionsVisibility()
                    updateTotalPoints()
                    Toast.makeText(this, "Generated ${generatedQuestions.size} questions successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to generate questions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadQuizForEditing(quizId: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading quiz...")
        progressDialog.show()
        
        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { document ->
                progressDialog.dismiss()
                if (document.exists()) {
                    val quiz = document.toObject(Quiz::class.java)
                    quiz?.let { populateFields(it) }
                }
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to load quiz: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    
    private fun populateFields(quiz: Quiz) {
        etQuizTitle.setText(quiz.title)
        etQuizDescription.setText(quiz.description)
        etTimeLimit.setText(quiz.timeLimit.toString())
        etPassingScore.setText(quiz.passingScore.toString())
        etMaxAttempts.setText(quiz.maxAttempts.toString())
        
        // Set difficulty
        val difficultyIndex = QuizDifficulty.values().indexOf(quiz.difficulty)
        if (difficultyIndex >= 0) {
            spinnerDifficulty.setSelection(difficultyIndex)
        }
        
        // Set tags
        quiz.tags.forEach { tag ->
            for (i in 0 until chipGroupTags.childCount) {
                val chip = chipGroupTags.getChildAt(i) as? Chip
                if (chip?.text == tag) {
                    chip.isChecked = true
                }
            }
        }
        
        // Load questions
        questions.clear()
        questions.addAll(quiz.questions)
        questionAdapter.notifyDataSetChanged()
        updateQuestionsVisibility()
        updateTotalPoints()
    }
    
    private fun updateTotalPoints() {
        val totalPoints = questions.sumOf { it.points }
        tvTotalPoints.text = "Total Points: $totalPoints"
    }
    
    private fun saveQuiz(publish: Boolean) {
        val title = etQuizTitle.text.toString().trim()
        val description = etQuizDescription.text.toString().trim()
        val timeLimitStr = etTimeLimit.text.toString().trim()
        val passingScoreStr = etPassingScore.text.toString().trim()

        if (title.isEmpty()) {
            etQuizTitle.error = "Quiz title is required"
            etQuizTitle.requestFocus()
            return
        }
        if (questions.isEmpty()) {
            Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show()
            return
        }

        val timeLimit = if (timeLimitStr.isNotEmpty()) {
            val parsed = timeLimitStr.toIntOrNull()
            if (parsed == null || parsed < 0) {
                etTimeLimit.error = "Time limit must be a non-negative number"
                etTimeLimit.requestFocus()
                return
            }
            parsed
        } else 0

        val passingScore = if (passingScoreStr.isNotEmpty()) {
            val parsed = passingScoreStr.toIntOrNull()
            if (parsed == null || parsed < 0 || parsed > 100) {
                etPassingScore.error = "Passing score must be between 0 and 100"
                etPassingScore.requestFocus()
                return
            }
            parsed
        } else 70

        val difficulty = try {
            QuizDifficulty.valueOf(spinnerDifficulty.selectedItem.toString())
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Invalid difficulty level", Toast.LENGTH_SHORT).show()
            return
        }

        val quizId = UUID.randomUUID().toString()
        val updatedQuestions = questions.map { it.copy(quizId = quizId) }

        val maxAttempts = etMaxAttempts.text.toString().toIntOrNull() ?: 3
        
        // Get selected tags
        val selectedTags = mutableListOf<String>()
        for (i in 0 until chipGroupTags.childCount) {
            val chip = chipGroupTags.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedTags.add(chip.text.toString())
            }
        }
        
        val quiz = Quiz(
            id = editingQuizId ?: quizId,
            title = title,
            description = description,
            courseId = courseId,
            teacherId = auth.currentUser?.uid ?: "",
            questions = updatedQuestions,
            timeLimit = timeLimit,
            maxAttempts = maxAttempts,
            passingScore = passingScore,
            isPublished = publish,
            createdAt = if (isEditMode) System.currentTimeMillis() else System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            difficulty = difficulty,
            category = "Week $weekNumber",
            tags = selectedTags,
            aiGenerated = questions.any { it.aiGenerated },
            totalPoints = questions.sumOf { it.points }
        )
        
        // Save to Firestore
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(if (publish) "Publishing quiz..." else "Saving quiz...")
        progressDialog.show()
        
        db.collection("quizzes")
            .document(quiz.id)
            .set(quiz)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, 
                    if (publish) "Quiz published successfully!" else "Quiz saved as draft!",
                    Toast.LENGTH_SHORT).show()
                
                val resultIntent = Intent().apply {
                    putExtra("quiz_id", quiz.id)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to save quiz: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun updateQuestionsVisibility() {
        tvNoQuestions.visibility = if (questions.isEmpty()) View.VISIBLE else View.GONE
        rvQuestions.visibility = if (questions.isEmpty()) View.GONE else View.VISIBLE
    }
}