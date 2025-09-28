package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.adapters.QuizQuestionAdapter
import com.example.ed.databinding.ActivityStudentQuizBinding
import com.example.ed.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class StudentQuizActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStudentQuizBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private var quizId: String = ""
    private var courseId: String = ""
    private var weekNumber: Int = 0
    private var passingScore: Int = 75
    
    private var quiz: Quiz? = null
    private var currentQuestionIndex = 0
    private var userAnswers = mutableMapOf<String, List<String>>()
    private var timer: CountDownTimer? = null
    private var timeRemaining: Long = 0
    private var startTime: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        quizId = intent.getStringExtra("QUIZ_ID") ?: ""
        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        weekNumber = intent.getIntExtra("WEEK_NUMBER", 0)
        passingScore = intent.getIntExtra("PASSING_SCORE", 75)
        
        if (quizId.isEmpty()) {
            Toast.makeText(this, "Invalid quiz", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupToolbar()
        loadQuiz()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quiz"
        binding.toolbar.setNavigationOnClickListener { 
            showExitConfirmation()
        }
    }
    
    private fun loadQuiz() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutQuizContent.visibility = View.GONE
        
        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    quiz = document.toObject(Quiz::class.java)
                    quiz?.let {
                        displayQuiz(it)
                    }
                } else {
                    Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load quiz: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    
    private fun displayQuiz(quiz: Quiz) {
        binding.progressBar.visibility = View.GONE
        binding.layoutQuizContent.visibility = View.VISIBLE
        
        // Set quiz info
        binding.tvQuizTitle.text = quiz.title
        binding.tvQuizDescription.text = quiz.description
        binding.tvTotalQuestions.text = "Total Questions: ${quiz.questions.size}"
        binding.tvPassingScore.text = "Passing Score: $passingScore%"
        
        // Setup timer if quiz has time limit
        if (quiz.timeLimit > 0) {
            timeRemaining = quiz.timeLimit * 60 * 1000L // Convert minutes to milliseconds
            startTimer()
        } else {
            binding.tvTimer.visibility = View.GONE
        }
        
        // Start quiz
        startTime = System.currentTimeMillis()
        displayQuestion(0)
        
        // Setup navigation buttons
        binding.btnPrevious.setOnClickListener {
            if (currentQuestionIndex > 0) {
                saveCurrentAnswer()
                currentQuestionIndex--
                displayQuestion(currentQuestionIndex)
            }
        }
        
        binding.btnNext.setOnClickListener {
            if (currentQuestionIndex < quiz.questions.size - 1) {
                saveCurrentAnswer()
                currentQuestionIndex++
                displayQuestion(currentQuestionIndex)
            }
        }
        
        binding.btnSubmit.setOnClickListener {
            showSubmitConfirmation()
        }
    }
    
    private fun displayQuestion(index: Int) {
        val question = quiz?.questions?.get(index) ?: return
        
        // Update question counter
        binding.tvQuestionNumber.text = "Question ${index + 1} of ${quiz?.questions?.size}"
        
        // Update progress bar
        val progress = ((index + 1) * 100) / (quiz?.questions?.size ?: 1)
        binding.progressBarQuiz.progress = progress
        
        // Display question
        binding.tvQuestion.text = question.question
        binding.tvQuestionPoints.text = "${question.points} points"
        
        // Clear previous options
        binding.layoutAnswerOptions.removeAllViews()
        
        // Display answer options based on question type
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                displayMultipleChoiceOptions(question)
            }
            QuestionType.TRUE_FALSE -> {
                displayTrueFalseOptions(question)
            }
            QuestionType.SHORT_ANSWER -> {
                displayShortAnswerInput(question)
            }
            else -> {
                // Handle other question types
            }
        }
        
        // Update navigation buttons
        binding.btnPrevious.isEnabled = index > 0
        binding.btnNext.visibility = if (index < (quiz?.questions?.size ?: 0) - 1) View.VISIBLE else View.GONE
        binding.btnSubmit.visibility = if (index == (quiz?.questions?.size ?: 0) - 1) View.VISIBLE else View.GONE
        
        // Restore saved answer if exists
        val savedAnswer = userAnswers[question.id]
        if (savedAnswer != null) {
            restoreAnswer(question, savedAnswer)
        }
    }
    
    private fun displayMultipleChoiceOptions(question: Question) {
        val radioGroup = android.widget.RadioGroup(this)
        radioGroup.orientation = android.widget.RadioGroup.VERTICAL
        
        question.options.forEach { option ->
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.textSize = 16f
            radioButton.setPadding(0, 16, 0, 16)
            radioGroup.addView(radioButton)
        }
        
        binding.layoutAnswerOptions.addView(radioGroup)
    }
    
    private fun displayTrueFalseOptions(question: Question) {
        val radioGroup = android.widget.RadioGroup(this)
        radioGroup.orientation = android.widget.RadioGroup.VERTICAL
        
        val trueButton = RadioButton(this)
        trueButton.text = "True"
        trueButton.textSize = 16f
        trueButton.setPadding(0, 16, 0, 16)
        radioGroup.addView(trueButton)
        
        val falseButton = RadioButton(this)
        falseButton.text = "False"
        falseButton.textSize = 16f
        falseButton.setPadding(0, 16, 0, 16)
        radioGroup.addView(falseButton)
        
        binding.layoutAnswerOptions.addView(radioGroup)
    }
    
    private fun displayShortAnswerInput(question: Question) {
        val editText = android.widget.EditText(this)
        editText.hint = "Type your answer here"
        editText.textSize = 16f
        editText.setPadding(16, 16, 16, 16)
        binding.layoutAnswerOptions.addView(editText)
    }
    
    private fun saveCurrentAnswer() {
        val question = quiz?.questions?.get(currentQuestionIndex) ?: return
        val answers = mutableListOf<String>()
        
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                val radioGroup = binding.layoutAnswerOptions.getChildAt(0) as? android.widget.RadioGroup
                val checkedId = radioGroup?.checkedRadioButtonId ?: -1
                if (checkedId != -1) {
                    val radioButton = radioGroup?.findViewById<RadioButton>(checkedId)
                    radioButton?.text?.toString()?.let { answers.add(it) }
                }
            }
            QuestionType.SHORT_ANSWER -> {
                val editText = binding.layoutAnswerOptions.getChildAt(0) as? android.widget.EditText
                editText?.text?.toString()?.trim()?.let { 
                    if (it.isNotEmpty()) answers.add(it)
                }
            }
            else -> {
                // Handle other question types
            }
        }
        
        if (answers.isNotEmpty()) {
            userAnswers[question.id] = answers
        }
    }
    
    private fun restoreAnswer(question: Question, savedAnswer: List<String>) {
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                val radioGroup = binding.layoutAnswerOptions.getChildAt(0) as? android.widget.RadioGroup
                radioGroup?.let { group ->
                    for (i in 0 until group.childCount) {
                        val radioButton = group.getChildAt(i) as? RadioButton
                        if (radioButton?.text?.toString() == savedAnswer.firstOrNull()) {
                            radioButton?.isChecked = true
                            break
                        }
                    }
                }
            }
            QuestionType.SHORT_ANSWER -> {
                val editText = binding.layoutAnswerOptions.getChildAt(0) as? android.widget.EditText
                editText?.setText(savedAnswer.firstOrNull() ?: "")
            }
            else -> {
                // Handle other question types
            }
        }
    }
    
    private fun startTimer() {
        timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                updateTimerDisplay()
            }
            
            override fun onFinish() {
                // Auto-submit when time runs out
                submitQuiz()
            }
        }.start()
    }
    
    private fun updateTimerDisplay() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60
        binding.tvTimer.text = String.format("Time: %02d:%02d", minutes, seconds)
        
        // Change color when less than 5 minutes remaining
        if (minutes < 5) {
            binding.tvTimer.setTextColor(getColor(R.color.error_red))
        }
    }
    
    private fun showSubmitConfirmation() {
        saveCurrentAnswer()
        
        val answeredCount = userAnswers.size
        val totalQuestions = quiz?.questions?.size ?: 0
        
        val message = if (answeredCount < totalQuestions) {
            "You have answered $answeredCount out of $totalQuestions questions.\n\nAre you sure you want to submit?"
        } else {
            "Are you sure you want to submit your quiz?"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Submit Quiz")
            .setMessage(message)
            .setPositiveButton("Submit") { _, _ ->
                submitQuiz()
            }
            .setNegativeButton("Review", null)
            .show()
    }
    
    private fun submitQuiz() {
        timer?.cancel()
        
        // Calculate score
        val results = calculateScore()
        
        // Save attempt to Firestore
        saveQuizAttempt(results)
        
        // Show results
        showResults(results)
    }
    
    private fun calculateScore(): QuizResults {
        var correctAnswers = 0
        var totalPoints = 0
        var earnedPoints = 0
        
        quiz?.questions?.forEach { question ->
            totalPoints += question.points
            val userAnswer = userAnswers[question.id]
            
            if (userAnswer != null && isAnswerCorrect(question, userAnswer)) {
                correctAnswers++
                earnedPoints += question.points
            }
        }
        
        val percentage = if (totalPoints > 0) {
            (earnedPoints * 100f) / totalPoints
        } else {
            0f
        }
        
        return QuizResults(
            totalQuestions = quiz?.questions?.size ?: 0,
            correctAnswers = correctAnswers,
            totalPoints = totalPoints,
            earnedPoints = earnedPoints,
            percentage = percentage,
            passed = percentage >= passingScore
        )
    }
    
    private fun isAnswerCorrect(question: Question, userAnswer: List<String>): Boolean {
        return when (question.type) {
            QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                userAnswer.firstOrNull() == question.correctAnswers.firstOrNull()
            }
            QuestionType.SHORT_ANSWER -> {
                // For short answer, check if user answer contains key terms
                val userText = userAnswer.firstOrNull()?.lowercase() ?: ""
                question.correctAnswers.any { correct ->
                    userText.contains(correct.lowercase())
                }
            }
            else -> false
        }
    }
    
    private fun saveQuizAttempt(results: QuizResults) {
        val userId = auth.currentUser?.uid ?: return
        val endTime = System.currentTimeMillis()
        
        val attemptData = hashMapOf(
            "studentId" to userId,
            "quizId" to quizId,
            "courseId" to courseId,
            "weekNumber" to weekNumber,
            "score" to results.percentage,
            "earnedPoints" to results.earnedPoints,
            "totalPoints" to results.totalPoints,
            "correctAnswers" to results.correctAnswers,
            "totalQuestions" to results.totalQuestions,
            "passed" to results.passed,
            "startTime" to startTime,
            "endTime" to endTime,
            "timeSpent" to (endTime - startTime),
            "answers" to userAnswers,
            "attemptedAt" to System.currentTimeMillis()
        )
        
        db.collection("quiz_attempts")
            .add(attemptData)
            .addOnSuccessListener {
                // Successfully saved
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save quiz attempt", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showResults(results: QuizResults) {
        val intent = Intent()
        intent.putExtra("QUIZ_SCORE", results.percentage)
        intent.putExtra("PASSED", results.passed)
        setResult(RESULT_OK, intent)
        
        AlertDialog.Builder(this)
            .setTitle(if (results.passed) "Congratulations! 🎉" else "Quiz Complete")
            .setMessage(buildResultMessage(results))
            .setPositiveButton("Finish") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun buildResultMessage(results: QuizResults): String {
        return """
            Your Score: ${String.format("%.1f", results.percentage)}%
            
            Correct Answers: ${results.correctAnswers} / ${results.totalQuestions}
            Points Earned: ${results.earnedPoints} / ${results.totalPoints}
            
            ${if (results.passed) {
                "You passed! You need $passingScore% to pass."
            } else {
                "You need $passingScore% to pass. Please review the content and try again."
            }}
        """.trimIndent()
    }
    
    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Quiz")
            .setMessage("Are you sure you want to exit? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .setNegativeButton("Continue", null)
            .show()
    }
    
    override fun onBackPressed() {
        showExitConfirmation()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
    
    data class QuizResults(
        val totalQuestions: Int,
        val correctAnswers: Int,
        val totalPoints: Int,
        val earnedPoints: Int,
        val percentage: Float,
        val passed: Boolean
    )
}
