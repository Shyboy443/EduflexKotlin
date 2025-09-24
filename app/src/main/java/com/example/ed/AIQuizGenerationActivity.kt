package com.example.ed

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.QuestionPreviewAdapter
import com.example.ed.models.QuestionType
import com.example.ed.services.AIQuizService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AIQuizGenerationActivity : AppCompatActivity() {

    // UI Components
    private lateinit var topicInput: TextInputEditText
    private lateinit var topicLayout: TextInputLayout
    private lateinit var courseContextInput: TextInputEditText
    private lateinit var difficultySpinner: Spinner
    private lateinit var questionCountSlider: Slider
    private lateinit var questionCountText: TextView
    private lateinit var questionTypesChipGroup: ChipGroup
    private lateinit var generateButton: MaterialButton
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var previewCard: MaterialCardView
    private lateinit var questionsRecyclerView: RecyclerView
    private lateinit var saveQuizButton: MaterialButton
    private lateinit var regenerateButton: MaterialButton
    private lateinit var loadingLayout: LinearLayout
    private lateinit var resultsLayout: LinearLayout

    // Services and Adapters
    private lateinit var aiQuizService: AIQuizService
    private lateinit var questionPreviewAdapter: QuestionPreviewAdapter

    // Data
    private var generatedQuiz: com.example.ed.models.Quiz? = null
    private val selectedQuestionTypes = mutableSetOf<QuestionType>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_quiz_generation)

        initializeViews()
        setupServices()
        setupUI()
        setupListeners()
    }

    private fun initializeViews() {
        topicInput = findViewById(R.id.topicInput)
        topicLayout = findViewById(R.id.topicLayout)
        courseContextInput = findViewById(R.id.courseContextInput)
        difficultySpinner = findViewById(R.id.difficultySpinner)
        questionCountSlider = findViewById(R.id.questionCountSlider)
        questionCountText = findViewById(R.id.questionCountText)
        questionTypesChipGroup = findViewById(R.id.questionTypesChipGroup)
        generateButton = findViewById(R.id.generateButton)
        progressIndicator = findViewById(R.id.progressIndicator)
        previewCard = findViewById(R.id.previewCard)
        questionsRecyclerView = findViewById(R.id.questionsRecyclerView)
        saveQuizButton = findViewById(R.id.saveQuizButton)
        regenerateButton = findViewById(R.id.regenerateButton)
        loadingLayout = findViewById(R.id.loadingLayout)
        resultsLayout = findViewById(R.id.resultsLayout)
    }

    private fun setupServices() {
        aiQuizService = AIQuizService()
        questionPreviewAdapter = com.example.ed.adapters.QuestionPreviewAdapter { question, _ ->
            // Handle question edit/improve
            showQuestionEditDialog(question)
        }
    }

    private fun setupUI() {
        setupToolbar()
        setupDifficultySpinner()
        setupQuestionCountSlider()
        setupQuestionTypesChips()
        setupRecyclerView()
        hideResults()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "AI Quiz Generator"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupDifficultySpinner() {
        val difficulties = arrayOf("Easy", "Medium", "Hard")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = adapter
        difficultySpinner.setSelection(1) // Default to Medium
    }

    private fun setupQuestionCountSlider() {
        questionCountSlider.apply {
            valueFrom = 5f
            valueTo = 50f
            value = 10f
            stepSize = 1f
        }
        updateQuestionCountText(10)
    }

    private fun setupQuestionTypesChips() {
        val questionTypes = listOf(
            "Multiple Choice" to QuestionType.MULTIPLE_CHOICE,
            "True/False" to QuestionType.TRUE_FALSE,
            "Short Answer" to QuestionType.SHORT_ANSWER,
            "Essay" to QuestionType.ESSAY,
            "Fill in Blank" to QuestionType.FILL_IN_BLANK
        )

        questionTypes.forEach { (name, type) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = type == QuestionType.MULTIPLE_CHOICE // Default selection
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedQuestionTypes.add(type)
                    } else {
                        selectedQuestionTypes.remove(type)
                    }
                    validateForm()
                }
            }
            questionTypesChipGroup.addView(chip)
        }

        // Initialize with default selection
        selectedQuestionTypes.add(QuestionType.MULTIPLE_CHOICE)
    }

    private fun setupRecyclerView() {
        questionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AIQuizGenerationActivity)
            adapter = questionPreviewAdapter
        }
    }

    private fun setupListeners() {
        generateButton.setOnClickListener {
            generateQuiz()
        }

        saveQuizButton.setOnClickListener {
            saveGeneratedQuiz()
        }

        regenerateButton.setOnClickListener {
            regenerateQuiz()
        }

        questionCountSlider.addOnChangeListener { _, value, _ ->
            updateQuestionCountText(value.toInt())
        }

        // Form validation listeners
        topicInput.setOnFocusChangeListener { _, _ -> validateForm() }
    }

    private fun updateQuestionCountText(count: Int) {
        questionCountText.text = "$count questions"
    }

    private fun validateForm(): Boolean {
        val topic = topicInput.text?.toString()?.trim()
        val hasQuestionTypes = selectedQuestionTypes.isNotEmpty()

        val isValid = !topic.isNullOrEmpty() && hasQuestionTypes

        generateButton.isEnabled = isValid

        // Update UI feedback
        if (topic.isNullOrEmpty()) {
            topicLayout.error = "Please enter a topic"
        } else {
            topicLayout.error = null
        }

        return isValid
    }

    private fun generateQuiz() {
        if (!validateForm()) return

        val topic = topicInput.text.toString().trim()
        val difficulty = difficultySpinner.selectedItem.toString().lowercase()
        val questionCount = questionCountSlider.value.toInt()
        val courseContext = courseContextInput.text?.toString()?.trim()

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = aiQuizService.generateQuiz(
                    topic = topic,
                    difficulty = difficulty,
                    questionCount = questionCount,
                    questionTypes = selectedQuestionTypes.toList(),
                    courseContext = courseContext
                )

                result.fold(
                    onSuccess = { quiz ->
                        generatedQuiz = quiz
                        displayGeneratedQuiz(quiz)
                        showResults()
                    },
                    onFailure = { error ->
                        showError("Failed to generate quiz: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("An error occurred: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun regenerateQuiz() {
        generateQuiz()
    }

    private fun displayGeneratedQuiz(quiz: com.example.ed.models.Quiz) {
        questionPreviewAdapter.updateQuestions(quiz.questions)
        
        // Update quiz info
        supportActionBar?.subtitle = "${quiz.questions.size} questions generated"
    }

    private fun saveGeneratedQuiz() {
        generatedQuiz?.let { quiz ->
            // TODO: Implement save to Firebase/database
            showSuccess("Quiz saved successfully!")
            finish()
        }
    }

    private fun showQuestionEditDialog(question: com.example.ed.models.Question) {
        // TODO: Implement question editing dialog
        Toast.makeText(this, "Question editing coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingLayout.visibility = View.VISIBLE
            progressIndicator.visibility = View.VISIBLE
            generateButton.isEnabled = false
        } else {
            loadingLayout.visibility = View.GONE
            progressIndicator.visibility = View.GONE
            generateButton.isEnabled = true
        }
    }

    private fun showResults() {
        resultsLayout.visibility = View.VISIBLE
        previewCard.visibility = View.VISIBLE
    }

    private fun hideResults() {
        resultsLayout.visibility = View.GONE
        previewCard.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}