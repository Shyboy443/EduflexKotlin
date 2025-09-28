package com.example.ed

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.example.ed.models.*
import com.example.ed.activities.QuizCreationActivity
import com.example.ed.adapters.ContentItemAdapter
import com.example.ed.adapters.ActivitiesAdapter
import com.example.ed.services.AIQuizService
import com.example.ed.services.GeminiAIService
import kotlinx.coroutines.*
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class WeeklyContentActivity : AppCompatActivity() {

    // UI Components
    private lateinit var toolbar: MaterialToolbar
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
    private lateinit var tvQuizDetails: TextView
    private lateinit var rvQuizQuestions: RecyclerView
    private lateinit var btnEditQuiz: MaterialButton
    private lateinit var btnRemoveQuiz: MaterialButton
    private lateinit var btnPreviewQuiz: MaterialButton
    private lateinit var btnGenerateAIQuiz: MaterialButton
    
    // Activities Section
    private lateinit var rvActivities: RecyclerView
    private lateinit var btnAddAssignment: MaterialButton

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // AI Services
    private lateinit var aiQuizService: AIQuizService
    private lateinit var geminiService: GeminiAIService

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
    private lateinit var quizQuestionsAdapter: QuizQuestionsPreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_content)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Enable Firestore offline persistence and better error handling
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e("WeeklyContentActivity", "Firestore settings error: ${e.message}")
        }

        // Initialize AI Services
        aiQuizService = AIQuizService()
        geminiService = GeminiAIService()

        // Check if editing existing content
        val weeklyContentId = intent.getStringExtra("weekly_content_id")
        isEditMode = weeklyContentId != null

        initializeViews()
        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        loadTeacherCourses()

        if (isEditMode) {
            loadWeeklyContentForEditing(weeklyContentId!!)
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        spinnerCourse = findViewById(R.id.spinner_course)
        etWeekNumber = findViewById(R.id.et_week_number)
        etWeekTitle = findViewById(R.id.et_week_title)
        etWeekDescription = findViewById(R.id.et_week_description)
        etLearningObjectives = findViewById(R.id.et_learning_objectives)
        rvContentItems = findViewById(R.id.rv_content_items)
        btnAddContent = findViewById(R.id.btn_add_content)
        btnAddQuiz = findViewById(R.id.btn_add_quiz)
        btnAddActivity = findViewById(R.id.btn_add_activity)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnPublish = findViewById(R.id.btn_publish)
        
        // Quiz section
        cardQuizSection = findViewById(R.id.card_quiz_section)
        tvQuizTitle = findViewById(R.id.tv_quiz_title)
        tvQuizDetails = findViewById(R.id.tv_quiz_details)
        rvQuizQuestions = findViewById(R.id.rv_quiz_questions)
        btnEditQuiz = findViewById(R.id.btn_edit_quiz)
        btnRemoveQuiz = findViewById(R.id.btn_remove_quiz)
        btnPreviewQuiz = findViewById(R.id.btn_preview_quiz)
        btnGenerateAIQuiz = findViewById(R.id.btn_generate_ai_quiz)
        
        // Activities section
        rvActivities = findViewById(R.id.rv_activities)
        btnAddAssignment = findViewById(R.id.btn_add_assignment)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Weekly Content" else "Add Weekly Content"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
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
        rvContentItems.layoutManager = LinearLayoutManager(this)
        rvContentItems.adapter = contentAdapter

        // Activities RecyclerView
        activitiesAdapter = ActivitiesAdapter(activities) { position ->
            removeActivity(position)
        }
        rvActivities.layoutManager = LinearLayoutManager(this)
        rvActivities.adapter = activitiesAdapter

        // Quiz Questions RecyclerView
        quizQuestionsAdapter = QuizQuestionsPreviewAdapter(emptyList())
        rvQuizQuestions.layoutManager = LinearLayoutManager(this)
        rvQuizQuestions.adapter = quizQuestionsAdapter
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

        btnGenerateAIQuiz.setOnClickListener {
            showAIQuizDialog()
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
        
        // Try both instructor.id and teacherId fields to catch all courses
        db.collection("courses")
            .whereEqualTo("teacherId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                coursesList.clear()
                for (document in documents) {
                    try {
                        val course = document.toObject(EnhancedCourse::class.java).copy(id = document.id)
                        coursesList.add(course)
                    } catch (e: Exception) {
                        // If EnhancedCourse fails, try simple parsing
                        val id = document.id
                        val title = document.getString("title") ?: "Untitled Course"
                        val description = document.getString("description") ?: ""
                        coursesList.add(EnhancedCourse(
                            id = id,
                            title = title,
                            description = description
                        ))
                    }
                }
                
                // If no courses found with teacherId, try instructor.id
                if (coursesList.isEmpty()) {
                    db.collection("courses")
                        .whereEqualTo("instructor.id", currentUser.uid)
                        .get()
                        .addOnSuccessListener { docs ->
                            for (document in docs) {
                                try {
                                    val course = document.toObject(EnhancedCourse::class.java).copy(id = document.id)
                                    coursesList.add(course)
                                } catch (e: Exception) {
                                    val id = document.id
                                    val title = document.getString("title") ?: "Untitled Course"
                                    val description = document.getString("description") ?: ""
                                    coursesList.add(EnhancedCourse(
                                        id = id,
                                        title = title,
                                        description = description
                                    ))
                                }
                            }
                            setupCourseSpinner()
                        }
                } else {
                    setupCourseSpinner()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load courses: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCourseSpinner() {
        val courseNames = coursesList.map { it.title }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courseNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourse.adapter = adapter
    }

    private fun showAddContentDialog() {
        val intent = Intent(this, AddContentItemActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_CONTENT)
    }

    private fun showAddActivityDialog() {
        val selectedCourse = getSelectedCourse() ?: return
        
        val intent = Intent(this, AssignmentCreationActivity::class.java)
        intent.putExtra("course_id", selectedCourse.id)
        intent.putExtra("week_number", etWeekNumber.text.toString().toIntOrNull() ?: 1)
        startActivityForResult(intent, REQUEST_ADD_ACTIVITY)
    }

    private fun createNewQuiz() {
        val selectedCourse = getSelectedCourse() ?: return
        
        val intent = Intent(this, QuizCreationActivity::class.java)
        intent.putExtra("course_id", selectedCourse.id)
        intent.putExtra("week_number", etWeekNumber.text.toString().toIntOrNull() ?: 1)
        startActivityForResult(intent, REQUEST_CREATE_QUIZ)
    }

    private fun editQuiz() {
        currentQuiz?.let { quiz ->
            val intent = Intent(this, QuizCreationActivity::class.java)
            intent.putExtra("quiz_id", quiz.id)
            startActivityForResult(intent, REQUEST_EDIT_QUIZ)
        }
    }

    private fun removeQuiz() {
        currentQuiz = null
        updateQuizSection()
    }

    private fun showAIQuizDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_quiz_generation, null)
        val etTopic = dialogView.findViewById<EditText>(R.id.et_topic)
        val spinnerDifficulty = dialogView.findViewById<Spinner>(R.id.spinner_difficulty)
        val etQuestionCount = dialogView.findViewById<EditText>(R.id.et_question_count)
        val spinnerQuestionType = dialogView.findViewById<Spinner>(R.id.spinner_question_type)
        val etContext = dialogView.findViewById<EditText>(R.id.et_context)
        
        // Pre-fill with week content
        etTopic.setText(etWeekTitle.text.toString())
        etContext.setText(etWeekDescription.text.toString())
        
        // Setup difficulty spinner
        val difficulties = arrayOf("Easy", "Medium", "Hard")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter
        spinnerDifficulty.setSelection(1) // Default to Medium
        
        // Setup question type spinner
        val questionTypes = arrayOf("Multiple Choice", "True/False", "Short Answer", "Mixed")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questionTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuestionType.adapter = typeAdapter
        spinnerQuestionType.setSelection(3) // Default to Mixed
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Generate AI Quiz")
            .setView(dialogView)
            .setPositiveButton("Generate") { _, _ ->
                val topic = etTopic.text.toString()
                val difficulty = spinnerDifficulty.selectedItem.toString()
                val questionCount = etQuestionCount.text.toString().toIntOrNull() ?: 5
                val questionType = spinnerQuestionType.selectedItem.toString()
                val context = etContext.text.toString()
                
                if (topic.isNotEmpty()) {
                    generateAIQuiz(topic, difficulty, questionCount, questionType, context)
                } else {
                    Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateAIQuiz(topic: String, difficulty: String, questionCount: Int, questionType: String, context: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Generating AI quiz...")
        progressDialog.setCancelable(true)
        progressDialog.setOnCancelListener {
            Toast.makeText(this, "Quiz generation cancelled", Toast.LENGTH_SHORT).show()
        }
        progressDialog.show()
        
        val selectedCourse = getSelectedCourse()
        val currentUser = auth.currentUser
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val quizContent = withTimeout(90000) { // 90 second timeout
                    withContext(Dispatchers.IO) {
                        geminiService.generateQuizQuestions(
                            topic = topic,
                            numberOfQuestions = questionCount,
                            questionType = questionType,
                            difficulty = difficulty.uppercase(),
                            courseContext = context
                        )
                    }
                }
                
                if (quizContent != null) {
                    Log.d("WeeklyContentActivity", "AI content received, parsing questions...")
                    // Parse and create quiz
                    val questions = parseAIGeneratedQuestions(quizContent)
                    Log.d("WeeklyContentActivity", "Parsed ${questions.size} questions")
                    
                    if (questions.isNotEmpty()) {
                        val quiz = Quiz(
                            id = UUID.randomUUID().toString(),
                            title = "AI Quiz: $topic",
                            description = "AI-generated quiz on $topic with $difficulty difficulty",
                            courseId = selectedCourse?.id ?: "",
                            teacherId = currentUser?.uid ?: "",
                            questions = questions,
                            timeLimit = questionCount * 2, // 2 minutes per question
                            maxAttempts = 3,
                            passingScore = 70,
                            isPublished = false,
                            difficulty = when(difficulty.lowercase()) {
                                "easy" -> QuizDifficulty.EASY
                                "hard" -> QuizDifficulty.HARD
                                else -> QuizDifficulty.MEDIUM
                            },
                            aiGenerated = true,
                            totalPoints = questions.sumOf { it.points }
                        )
                        
                        // Store quiz locally as draft (don't save to Firestore until published)
                        Log.d("WeeklyContentActivity", "Quiz generated as draft: ${quiz.id}")
                        currentQuiz = quiz
                        updateQuizSection()
                        progressDialog.dismiss()
                        Toast.makeText(this@WeeklyContentActivity, "AI Quiz generated successfully! (Draft mode)", Toast.LENGTH_SHORT).show()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this@WeeklyContentActivity, "Failed to parse AI-generated questions", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Try local fallback generation
                    Log.w("WeeklyContentActivity", "AI generation failed, trying local fallback")
                    
                    // Check if it's a network issue
                    val isNetworkError = quizContent == null
                    if (isNetworkError) {
                        Toast.makeText(this@WeeklyContentActivity, 
                            "No internet connection. Generating quiz locally...", 
                            Toast.LENGTH_LONG).show()
                    }
                    
                    val localQuestions = generateLocalFallbackQuiz(topic, questionCount, difficulty)
                    
                    if (localQuestions.isNotEmpty()) {
                        val quiz = Quiz(
                            id = UUID.randomUUID().toString(),
                            title = "Quiz: $topic",
                            description = "Locally generated quiz on $topic",
                            courseId = selectedCourse?.id ?: "",
                            teacherId = currentUser?.uid ?: "",
                            questions = localQuestions,
                            timeLimit = questionCount * 2,
                            maxAttempts = 3,
                            passingScore = 70,
                            isPublished = false,
                            difficulty = when(difficulty.lowercase()) {
                                "easy" -> QuizDifficulty.EASY
                                "hard" -> QuizDifficulty.HARD
                                else -> QuizDifficulty.MEDIUM
                            },
                            aiGenerated = false, // Mark as locally generated
                            totalPoints = localQuestions.sumOf { it.points }
                        )
                        
                        // Store quiz locally as draft
                        currentQuiz = quiz
                        updateQuizSection()
                        progressDialog.dismiss()
                        Toast.makeText(this@WeeklyContentActivity, "Basic quiz generated locally (AI unavailable) - Draft mode", Toast.LENGTH_LONG).show()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this@WeeklyContentActivity, "Failed to generate quiz. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                progressDialog.dismiss()
                Log.w("WeeklyContentActivity", "AI generation timed out, trying local fallback")
                val localQuestions = generateLocalFallbackQuiz(topic, questionCount, difficulty)
                
                if (localQuestions.isNotEmpty()) {
                    val quiz = Quiz(
                        id = UUID.randomUUID().toString(),
                        title = "Quiz: $topic",
                        description = "Locally generated quiz on $topic (AI timed out)",
                        courseId = selectedCourse?.id ?: "",
                        teacherId = currentUser?.uid ?: "",
                        questions = localQuestions,
                        timeLimit = questionCount * 2,
                        maxAttempts = 3,
                        passingScore = 70,
                        isPublished = false,
                        difficulty = when(difficulty.lowercase()) {
                            "easy" -> QuizDifficulty.EASY
                            "hard" -> QuizDifficulty.HARD
                            else -> QuizDifficulty.MEDIUM
                        },
                        aiGenerated = false,
                        totalPoints = localQuestions.sumOf { it.points }
                    )
                    
                    // Store quiz locally as draft
                    currentQuiz = quiz
                    updateQuizSection()
                    Toast.makeText(this@WeeklyContentActivity, "Basic quiz generated (AI timed out) - Draft mode", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@WeeklyContentActivity, "AI generation timed out. Please try again.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e("WeeklyContentActivity", "Error generating quiz: ${e.message}", e)
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timed out. Please check your internet connection and try again."
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection."
                    else -> "Error generating quiz: ${e.message}"
                }
                Toast.makeText(this@WeeklyContentActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cleanJsonFromMarkdown(content: String): String {
        var cleaned = content.trim()
        
        // Remove markdown code block markers
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }
        
        // Try to extract JSON object or array from the content
        val jsonStart = cleaned.indexOf('{')
        val arrayStart = cleaned.indexOf('[')
        
        // Determine if it's an object or array and extract accordingly
        when {
            jsonStart != -1 && (arrayStart == -1 || jsonStart < arrayStart) -> {
                // It's a JSON object, find the matching closing brace
                val jsonEnd = findMatchingBrace(cleaned, jsonStart)
                if (jsonEnd != -1) {
                    cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
                }
            }
            arrayStart != -1 -> {
                // It's a JSON array, find the matching closing bracket
                val arrayEnd = findMatchingBracket(cleaned, arrayStart)
                if (arrayEnd != -1) {
                    cleaned = cleaned.substring(arrayStart, arrayEnd + 1)
                }
            }
        }
        
        // If the content is a JSON object with a "questions" property, extract the array
        if (cleaned.startsWith("{")) {
            try {
                val jsonObject = org.json.JSONObject(cleaned)
                if (jsonObject.has("questions")) {
                    return jsonObject.getJSONArray("questions").toString()
                }
            } catch (e: Exception) {
                Log.w("WeeklyContentActivity", "Failed to parse as JSON object: ${e.message}")
            }
        }
        
        return cleaned
    }
    
    private fun findMatchingBrace(content: String, startIndex: Int): Int {
        var braceCount = 0
        for (i in startIndex until content.length) {
            when (content[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) return i
                }
            }
        }
        return -1
    }
    
    private fun findMatchingBracket(content: String, startIndex: Int): Int {
        var bracketCount = 0
        for (i in startIndex until content.length) {
            when (content[i]) {
                '[' -> bracketCount++
                ']' -> {
                    bracketCount--
                    if (bracketCount == 0) return i
                }
            }
        }
        return -1
    }

    private fun parseAIGeneratedQuestions(content: String): List<Question> {
        val questions = mutableListOf<Question>()
        
        try {
            // Clean the content to extract JSON from markdown code blocks
            val cleanedContent = cleanJsonFromMarkdown(content)
            Log.d("WeeklyContentActivity", "Cleaned content: ${cleanedContent.take(200)}...")
            
            // Try to parse JSON response
            val jsonQuestions = org.json.JSONArray(cleanedContent)
            
            for (i in 0 until jsonQuestions.length()) {
                val jsonQuestion = jsonQuestions.getJSONObject(i)
                
                val question = Question(
                    id = UUID.randomUUID().toString(),
                    question = jsonQuestion.optString("question", ""),
                    type = when(jsonQuestion.optString("type", "multiple_choice").lowercase()) {
                        "true_false", "true/false" -> QuestionType.TRUE_FALSE
                        "short_answer", "short answer" -> QuestionType.SHORT_ANSWER
                        else -> QuestionType.MULTIPLE_CHOICE
                    },
                    options = if (jsonQuestion.has("options")) {
                        val optionsArray = jsonQuestion.getJSONArray("options")
                        (0 until optionsArray.length()).map { optionsArray.getString(it) }
                    } else emptyList(),
                    correctAnswers = if (jsonQuestion.has("correct_answer")) {
                        listOf(jsonQuestion.getString("correct_answer"))
                    } else if (jsonQuestion.has("correct_answers")) {
                        val answersArray = jsonQuestion.getJSONArray("correct_answers")
                        (0 until answersArray.length()).map { answersArray.getString(it) }
                    } else emptyList(),
                    explanation = jsonQuestion.optString("explanation", ""),
                    points = jsonQuestion.optInt("points", 1),
                    difficulty = when(jsonQuestion.optString("difficulty", "medium").lowercase()) {
                        "easy" -> QuestionDifficulty.EASY
                        "hard" -> QuestionDifficulty.HARD
                        else -> QuestionDifficulty.MEDIUM
                    },
                    aiGenerated = true,
                    order = i
                )
                
                if (question.question.isNotEmpty()) {
                    questions.add(question)
                }
            }
        } catch (e: Exception) {
            // If JSON parsing fails, return empty list
            Log.e("WeeklyContentActivity", "Failed to parse AI questions: ${e.message}", e)
            Log.e("WeeklyContentActivity", "Original content: $content")
        }
        
        return questions
    }

    private fun generateLocalFallbackQuiz(topic: String, questionCount: Int, difficulty: String): List<Question> {
        val questions = mutableListOf<Question>()
        
        // Generate basic template questions based on topic
        val templates = getQuestionTemplatesForTopic(topic, difficulty)
        
        for (i in 0 until minOf(questionCount, templates.size)) {
            val template = templates[i]
            val question = Question(
                id = UUID.randomUUID().toString(),
                question = template.question,
                type = template.type,
                options = template.options,
                correctAnswers = template.correctAnswers,
                explanation = template.explanation,
                points = when(difficulty.lowercase()) {
                    "easy" -> 1
                    "hard" -> 3
                    else -> 2
                },
                difficulty = when(difficulty.lowercase()) {
                    "easy" -> QuestionDifficulty.EASY
                    "hard" -> QuestionDifficulty.HARD
                    else -> QuestionDifficulty.MEDIUM
                },
                aiGenerated = false,
                order = i
            )
            questions.add(question)
        }
        
        return questions
    }
    
    private fun getQuestionTemplatesForTopic(topic: String, difficulty: String): List<QuestionTemplate> {
        val templates = mutableListOf<QuestionTemplate>()
        
        when {
            topic.contains("math", ignoreCase = true) || topic.contains("algebra", ignoreCase = true) -> {
                templates.addAll(getMathTemplates(difficulty))
            }
            topic.contains("science", ignoreCase = true) || topic.contains("biology", ignoreCase = true) -> {
                templates.addAll(getScienceTemplates(difficulty))
            }
            topic.contains("history", ignoreCase = true) -> {
                templates.addAll(getHistoryTemplates(difficulty))
            }
            topic.contains("english", ignoreCase = true) || topic.contains("literature", ignoreCase = true) -> {
                templates.addAll(getEnglishTemplates(difficulty))
            }
            else -> {
                templates.addAll(getGeneralTemplates(topic, difficulty))
            }
        }
        
        return templates.shuffled().take(10) // Return up to 10 random templates
    }
    
    private fun getMathTemplates(difficulty: String): List<QuestionTemplate> {
        return listOf(
            QuestionTemplate(
                "What is 2 + 2?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("3", "4", "5", "6"),
                listOf("4"),
                "Basic addition: 2 + 2 = 4"
            ),
            QuestionTemplate(
                "Is 10 greater than 5?",
                QuestionType.TRUE_FALSE,
                listOf("True", "False"),
                listOf("True"),
                "10 is indeed greater than 5"
            ),
            QuestionTemplate(
                "What is the result of 3 × 4?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("10", "11", "12", "13"),
                listOf("12"),
                "3 × 4 = 12"
            )
        )
    }
    
    private fun getScienceTemplates(difficulty: String): List<QuestionTemplate> {
        return listOf(
            QuestionTemplate(
                "What is the chemical symbol for water?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("H2O", "CO2", "O2", "H2"),
                listOf("H2O"),
                "Water is composed of two hydrogen atoms and one oxygen atom"
            ),
            QuestionTemplate(
                "Is the sun a star?",
                QuestionType.TRUE_FALSE,
                listOf("True", "False"),
                listOf("True"),
                "The sun is indeed a star - our nearest star"
            ),
            QuestionTemplate(
                "How many legs does a spider have?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("6", "8", "10", "12"),
                listOf("8"),
                "Spiders are arachnids and have 8 legs"
            )
        )
    }
    
    private fun getHistoryTemplates(difficulty: String): List<QuestionTemplate> {
        return listOf(
            QuestionTemplate(
                "In which year did World War II end?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("1944", "1945", "1946", "1947"),
                listOf("1945"),
                "World War II ended in 1945"
            ),
            QuestionTemplate(
                "Was the Great Wall of China built to defend against invasions?",
                QuestionType.TRUE_FALSE,
                listOf("True", "False"),
                listOf("True"),
                "The Great Wall was built primarily for defense purposes"
            )
        )
    }
    
    private fun getEnglishTemplates(difficulty: String): List<QuestionTemplate> {
        return listOf(
            QuestionTemplate(
                "What is a noun?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("An action word", "A describing word", "A person, place, or thing", "A connecting word"),
                listOf("A person, place, or thing"),
                "A noun is a word that represents a person, place, or thing"
            ),
            QuestionTemplate(
                "Is 'quickly' an adverb?",
                QuestionType.TRUE_FALSE,
                listOf("True", "False"),
                listOf("True"),
                "'Quickly' is an adverb that describes how an action is performed"
            )
        )
    }
    
    private fun getGeneralTemplates(topic: String, difficulty: String): List<QuestionTemplate> {
        return listOf(
            QuestionTemplate(
                "What is the main topic of this lesson about $topic?",
                QuestionType.SHORT_ANSWER,
                emptyList(),
                listOf(topic),
                "The main topic is $topic"
            ),
            QuestionTemplate(
                "Is this lesson about $topic?",
                QuestionType.TRUE_FALSE,
                listOf("True", "False"),
                listOf("True"),
                "Yes, this lesson focuses on $topic"
            ),
            QuestionTemplate(
                "Which of the following best describes $topic?",
                QuestionType.MULTIPLE_CHOICE,
                listOf("Important concept", "Basic knowledge", "Advanced topic", "General information"),
                listOf("Important concept"),
                "$topic is an important concept to understand"
            )
        )
    }
    
    data class QuestionTemplate(
        val question: String,
        val type: QuestionType,
        val options: List<String>,
        val correctAnswers: List<String>,
        val explanation: String
    )

    private fun updateQuizSection() {
        if (currentQuiz != null) {
            cardQuizSection.visibility = View.VISIBLE
            val quiz = currentQuiz!!
            val questionCount = quiz.questions.size
            val totalPoints = quiz.totalPoints
            
            tvQuizTitle.text = quiz.title
            tvQuizDetails.text = "Questions: $questionCount | Points: $totalPoints | " +
                    "Time: ${if (quiz.timeLimit > 0) "${quiz.timeLimit} min" else "No limit"} | " +
                    "Difficulty: ${quiz.difficulty.name}"
            tvQuizDetails.visibility = View.VISIBLE
            
            // Show quiz questions
            if (quiz.questions.isNotEmpty()) {
                quizQuestionsAdapter.updateQuestions(quiz.questions)
                rvQuizQuestions.visibility = View.VISIBLE
            } else {
                rvQuizQuestions.visibility = View.GONE
            }
            
            btnAddQuiz.text = "Edit Quiz"
            btnEditQuiz.visibility = View.VISIBLE
            btnRemoveQuiz.visibility = View.VISIBLE
            btnPreviewQuiz.visibility = View.VISIBLE
        } else {
            cardQuizSection.visibility = View.VISIBLE // Always show the section
            tvQuizTitle.text = "No quiz added"
            tvQuizDetails.visibility = View.GONE
            rvQuizQuestions.visibility = View.GONE
            btnAddQuiz.text = "Add Quiz"
            btnEditQuiz.visibility = View.GONE
            btnRemoveQuiz.visibility = View.GONE
            btnPreviewQuiz.visibility = View.GONE
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

        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage(if (publish) "Publishing weekly content..." else "Saving draft...")
        progressDialog.setCancelable(true)
        progressDialog.setOnCancelListener {
            Toast.makeText(this, "Save operation cancelled", Toast.LENGTH_SHORT).show()
        }
        progressDialog.show()

        // If publishing and we have a quiz, save it to Firestore first
        if (publish && currentQuiz != null) {
            Log.d("WeeklyContentActivity", "Publishing quiz to Firestore: ${currentQuiz!!.id}")
            
            // Add timeout for quiz publishing
            var quizSaveCompleted = false
            val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!quizSaveCompleted) {
                    Log.w("WeeklyContentActivity", "Quiz save timed out, proceeding with content save")
                    saveWeeklyContentToFirestore(weeklyContent, publish, progressDialog)
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 15000) // 15 second timeout
            
            // Create a simplified quiz object for Firestore
            val quizToSave = hashMapOf<String, Any?>(
                "id" to currentQuiz!!.id,
                "title" to currentQuiz!!.title,
                "description" to currentQuiz!!.description,
                "courseId" to currentQuiz!!.courseId,
                "teacherId" to currentQuiz!!.teacherId,
                "questions" to currentQuiz!!.questions.map { question ->
                    hashMapOf<String, Any?>(
                        "id" to question.id,
                        "question" to question.question,
                        "type" to question.type.name,
                        "options" to question.options,
                        "correctAnswers" to question.correctAnswers,
                        "explanation" to question.explanation,
                        "points" to question.points,
                        "difficulty" to question.difficulty.name,
                        "topic" to question.topic,
                        "order" to question.order,
                        "aiGenerated" to question.aiGenerated,
                        "createdAt" to question.createdAt
                    )
                },
                "totalPoints" to currentQuiz!!.totalPoints,
                "timeLimit" to currentQuiz!!.timeLimit,
                "passingScore" to currentQuiz!!.passingScore,
                "maxAttempts" to currentQuiz!!.maxAttempts,
                "isPublished" to currentQuiz!!.isPublished,
                "difficulty" to currentQuiz!!.difficulty.name,
                "category" to currentQuiz!!.category,
                "tags" to currentQuiz!!.tags,
                "aiGenerated" to currentQuiz!!.aiGenerated,
                "createdAt" to currentQuiz!!.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            
            // Try to save with retry logic
            saveWithRetry("quizzes", currentQuiz!!.id, quizToSave, 3) { success ->
                quizSaveCompleted = true
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (success) {
                    Log.d("WeeklyContentActivity", "Quiz published successfully")
                } else {
                    Log.e("WeeklyContentActivity", "Failed to publish quiz after retries")
                }
                // Continue with content save regardless
                saveWeeklyContentToFirestore(weeklyContent, publish, progressDialog)
            }
        } else {
            // Save weekly content without publishing quiz
            saveWeeklyContentToFirestore(weeklyContent, publish, progressDialog)
        }
    }
    
    private fun saveWithRetry(
        collection: String, 
        documentId: String, 
        data: HashMap<String, Any?>, 
        maxRetries: Int,
        callback: (Boolean) -> Unit
    ) {
        var retryCount = 0
        
        fun attemptSave() {
            db.collection(collection)
                .document(documentId)
                .set(data)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener { exception ->
                    Log.e("WeeklyContentActivity", "Save attempt ${retryCount + 1} failed: ${exception.message}")
                    retryCount++
                    
                    if (retryCount < maxRetries) {
                        // Exponential backoff: 1s, 2s, 4s
                        val delayMillis = (1000 * Math.pow(2.0, retryCount.toDouble())).toLong()
                        Log.d("WeeklyContentActivity", "Retrying in ${delayMillis}ms...")
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            attemptSave()
                        }, delayMillis)
                    } else {
                        callback(false)
                    }
                }
        }
        
        attemptSave()
    }
    
    private fun saveWeeklyContentToFirestore(weeklyContent: WeeklyContent, publish: Boolean, progressDialog: android.app.ProgressDialog) {
        Log.d("WeeklyContentActivity", "Saving weekly content to Firestore: ${weeklyContent.id}")
        
        // Add timeout for content saving
        var contentSaveCompleted = false
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!contentSaveCompleted) {
                Log.w("WeeklyContentActivity", "Content save timed out")
                progressDialog.dismiss()
                Toast.makeText(this, "Save operation timed out. Please check your connection and try again.", Toast.LENGTH_LONG).show()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 20000) // 20 second timeout for content
        
        // Create a simplified weekly content object for Firestore
        val contentToSave = hashMapOf(
            "id" to weeklyContent.id,
            "courseId" to weeklyContent.courseId,
            "weekNumber" to weeklyContent.weekNumber,
            "title" to weeklyContent.title,
            "description" to weeklyContent.description,
            "learningObjectives" to weeklyContent.learningObjectives,
            "contentItems" to weeklyContent.contentItems.map { item ->
                hashMapOf(
                    "id" to item.id,
                    "type" to item.type.name,
                    "title" to item.title,
                    "content" to item.content,
                    "mediaUrl" to item.mediaUrl,
                    "duration" to item.duration,
                    "order" to item.order,
                    "isRequired" to item.isRequired,
                    "createdAt" to item.createdAt
                )
            },
            "assignments" to weeklyContent.assignments.map { assignment ->
                hashMapOf(
                    "id" to assignment.id,
                    "title" to assignment.title,
                    "description" to assignment.description,
                    "instructions" to assignment.instructions,
                    "maxPoints" to assignment.maxPoints,
                    "dueDate" to assignment.dueDate
                )
            },
            "quizId" to weeklyContent.quiz?.id,
            "isPublished" to weeklyContent.isPublished,
            "createdBy" to weeklyContent.createdBy,
            "createdAt" to weeklyContent.createdAt,
            "updatedAt" to weeklyContent.updatedAt
        )
        
        db.collection("weekly_content")
            .document(weeklyContent.id)
            .set(contentToSave)
            .addOnSuccessListener {
                contentSaveCompleted = true
                timeoutHandler.removeCallbacks(timeoutRunnable)
                Log.d("WeeklyContentActivity", "Weekly content saved successfully")
                progressDialog.dismiss()
                Toast.makeText(this, 
                    if (publish) "Weekly content published successfully!" 
                    else "Weekly content saved as draft!", 
                    Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                contentSaveCompleted = true
                timeoutHandler.removeCallbacks(timeoutRunnable)
                Log.e("WeeklyContentActivity", "Failed to save weekly content: ${exception.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(): Boolean {
        if (getSelectedCourse() == null) {
            Toast.makeText(this, "Please select a course", Toast.LENGTH_SHORT).show()
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
                if (document.exists()) {
                    currentWeeklyContent = document.toObject(WeeklyContent::class.java)
                    populateFields()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load content: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
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
        
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_ADD_CONTENT -> {
                    // Handle content item result
                    val contentTitle = data.getStringExtra("content_title") ?: ""
                    val contentType = data.getStringExtra("content_type") ?: "TEXT"
                    val contentDescription = data.getStringExtra("content_description") ?: ""
                    val contentUrl = data.getStringExtra("content_url") ?: ""
                    val contentText = data.getStringExtra("content_text") ?: ""
                    val isRequired = data.getBooleanExtra("is_required", false)
                    
                    if (contentTitle.isNotEmpty()) {
                        val contentItem = ContentItem(
                            id = UUID.randomUUID().toString(),
                            title = contentTitle,
                            type = ContentType.valueOf(contentType),
                            content = "$contentDescription\n$contentText".trim(),
                            mediaUrl = contentUrl,
                            isRequired = isRequired,
                            order = contentItems.size,
                            createdAt = System.currentTimeMillis()
                        )
                        contentItems.add(contentItem)
                        contentAdapter.notifyItemInserted(contentItems.size - 1)
                        Toast.makeText(this, "Content added successfully", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Assignment added successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_CREATE_QUIZ, REQUEST_EDIT_QUIZ -> {
                    val quizId = data.getStringExtra("quiz_id")
                    if (quizId != null) {
                        // If it's the same quiz we're editing, just update the local reference
                        if (currentQuiz?.id == quizId) {
                            // Quiz was edited, try to reload from Firestore or keep current
                            loadQuizById(quizId)
                        } else {
                            // New quiz created
                            loadQuizById(quizId)
                        }
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
                if (document.exists()) {
                    currentQuiz = document.toObject(Quiz::class.java)
                    updateQuizSection()
                }
            }
    }

    private fun editContentItem(contentItem: ContentItem) {
        // TODO: Implement content item editing
        Toast.makeText(this, "Edit content item: ${contentItem.title}", Toast.LENGTH_SHORT).show()
    }

    private fun deleteContentItem(contentItem: ContentItem) {
        val position = contentItems.indexOf(contentItem)
        if (position != -1) {
            contentItems.removeAt(position)
            contentAdapter.notifyItemRemoved(position)
            Toast.makeText(this, "Content item removed", Toast.LENGTH_SHORT).show()
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
        private const val REQUEST_ADD_ACTIVITY = 1002
        private const val REQUEST_CREATE_QUIZ = 1003
        private const val REQUEST_EDIT_QUIZ = 1004
    }
}

// Simple adapter to preview quiz questions
class QuizQuestionsPreviewAdapter(private var questions: List<Question>) : 
    RecyclerView.Adapter<QuizQuestionsPreviewAdapter.QuestionViewHolder>() {

    fun updateQuestions(newQuestions: List<Question>) {
        questions = newQuestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        holder.bind(question, position + 1)
    }

    override fun getItemCount() = questions.size

    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(question: Question, questionNumber: Int) {
            text1.text = "Q$questionNumber: ${question.question}"
            
            val details = when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> {
                    "Multiple Choice | ${question.options.size} options | ${question.points} pts"
                }
                QuestionType.TRUE_FALSE -> {
                    "True/False | ${question.points} pts"
                }
                QuestionType.SHORT_ANSWER -> {
                    "Short Answer | ${question.points} pts"
                }
                else -> {
                    "${question.type.name} | ${question.points} pts"
                }
            }
            
            text2.text = details
            text2.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
        }
    }
}