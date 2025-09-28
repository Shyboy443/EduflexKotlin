package com.example.ed.ui.student.games

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.example.ed.R
import com.example.ed.databinding.FragmentQuizGameBinding
import com.example.ed.models.*
import com.example.ed.services.GamificationService
import com.example.ed.services.AIGameContentGenerator
import com.example.ed.services.PointsRewardsService
import com.example.ed.services.PointsType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class QuizGameFragment : Fragment() {

    private var _binding: FragmentQuizGameBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var gamificationService: GamificationService
    private lateinit var aiContentGenerator: AIGameContentGenerator
    private lateinit var pointsRewardsService: PointsRewardsService
    private lateinit var auth: FirebaseAuth
    
    // Callback for UI refresh
    private var onPointsUpdated: (() -> Unit)? = null
    
    private var difficulty: GameDifficulty = GameDifficulty.EASY
    private var isMathFocused: Boolean = false
    private var aiQuestions: List<com.example.ed.services.AIQuizQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var pointsEarned = 0
    private var startTime = 0L
    private var gameTimer: CountDownTimer? = null
    private var questionTimer: CountDownTimer? = null
    
    private var gameResultListener: ((GameResult) -> Unit)? = null

    companion object {
        private const val ARG_DIFFICULTY = "difficulty"
        private const val ARG_MATH_FOCUSED = "math_focused"
        private const val GAME_TIME_LIMIT = 300000L // 5 minutes
        private const val QUESTION_TIME_LIMIT = 30000L // 30 seconds per question
        
        fun newInstance(difficulty: GameDifficulty, isMathFocused: Boolean = false): QuizGameFragment {
            val fragment = QuizGameFragment()
            val args = Bundle()
            args.putString(ARG_DIFFICULTY, difficulty.name)
            args.putBoolean(ARG_MATH_FOCUSED, isMathFocused)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            difficulty = GameDifficulty.valueOf(it.getString(ARG_DIFFICULTY, GameDifficulty.EASY.name))
            isMathFocused = it.getBoolean(ARG_MATH_FOCUSED, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize services
        auth = FirebaseAuth.getInstance()
        gamificationService = GamificationService.getInstance(requireContext())
        aiContentGenerator = AIGameContentGenerator.getInstance(requireContext())
        pointsRewardsService = PointsRewardsService.getInstance(requireContext())
        
        setupUI()
        setupClickListeners()
        loadAIQuestions()
    }

    private fun setupUI() {
        binding.tvDifficulty.text = "Difficulty: ${difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}"
        binding.tvGameType.text = if (isMathFocused) "Math Challenge" else "Quiz Game"
        
        // Set progress bar max
        binding.progressBarTime.max = (QUESTION_TIME_LIMIT / 1000).toInt()
        
        // Show loading state
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutGame.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.btnOption1.setOnClickListener { selectAnswer(0) }
        binding.btnOption2.setOnClickListener { selectAnswer(1) }
        binding.btnOption3.setOnClickListener { selectAnswer(2) }
        binding.btnOption4.setOnClickListener { selectAnswer(3) }
        
        binding.btnNext.setOnClickListener { nextQuestion() }
        binding.btnFinish.setOnClickListener { finishGame() }
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadAIQuestions() {
        lifecycleScope.launch {
            try {
                // Show loading state with progress
                binding.layoutLoading.visibility = View.VISIBLE
                binding.layoutGame.visibility = View.GONE
                
                val subject = if (isMathFocused) "Mathematics" else getRandomSubject()
                val difficultyString = difficulty.name
                
                // Generate ALL questions before starting the game
                aiQuestions = aiContentGenerator.generateQuizQuestions(
                    subject = subject,
                    difficulty = difficultyString,
                    count = 10
                )
                
                // Ensure we have enough questions
                if (aiQuestions.size >= 5) { // Minimum 5 questions to play
                    // Questions ready - initialize game
                    initializeGameWithQuestions()
                } else {
                    // Not enough questions from AI, use fallback
                    generateFallbackQuestions()
                }
                
            } catch (e: Exception) {
                Log.e("QuizGame", "Error loading AI questions", e)
                generateFallbackQuestions()
            }
        }
    }
    
    private fun initializeGameWithQuestions() {
        // Hide loading and show game
        binding.layoutLoading.visibility = View.GONE
        binding.layoutGame.visibility = View.VISIBLE
        
        // Initialize game state
        currentQuestionIndex = 0
        score = 0
        pointsEarned = 0
        startTime = System.currentTimeMillis()
        
        // Update UI to show total questions
        binding.tvQuestionNumber.text = "Question 1 of ${aiQuestions.size}"
        
        // Start game timer
        startGameTimer()
        
        // Show first question
        showCurrentQuestion()
    }
    
    private fun generateFallbackQuestions() {
        // Create fallback questions when AI fails
        aiQuestions = if (isMathFocused) {
            mutableListOf(
                com.example.ed.services.AIQuizQuestion(
                    question = "What is 15 × 8?",
                    options = listOf("120", "130", "110", "140"),
                    correctIndex = 0,
                    explanation = "15 × 8 = 120"
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "Solve: 144 ÷ 12 = ?",
                    options = listOf("11", "12", "13", "14"),
                    correctIndex = 1,
                    explanation = "144 ÷ 12 = 12"
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "What is 25% of 80?",
                    options = listOf("15", "20", "25", "30"),
                    correctIndex = 1,
                    explanation = "25% of 80 = 0.25 × 80 = 20"
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "What is 7²?",
                    options = listOf("14", "21", "49", "42"),
                    correctIndex = 2,
                    explanation = "7² = 7 × 7 = 49"
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "Solve: 3x + 5 = 14, x = ?",
                    options = listOf("2", "3", "4", "5"),
                    correctIndex = 1,
                    explanation = "3x = 14 - 5 = 9, so x = 3"
                )
            )
        } else {
            mutableListOf(
                com.example.ed.services.AIQuizQuestion(
                    question = "What is the capital of France?",
                    options = listOf("London", "Paris", "Rome", "Berlin"),
                    correctIndex = 1,
                    explanation = "Paris is the capital and largest city of France."
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "Which planet is closest to the Sun?",
                    options = listOf("Venus", "Mercury", "Earth", "Mars"),
                    correctIndex = 1,
                    explanation = "Mercury is the closest planet to the Sun in our solar system."
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "What is H2O?",
                    options = listOf("Carbon dioxide", "Water", "Oxygen", "Hydrogen"),
                    correctIndex = 1,
                    explanation = "H2O is the chemical formula for water."
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "Who wrote 'Romeo and Juliet'?",
                    options = listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"),
                    correctIndex = 1,
                    explanation = "Romeo and Juliet is a tragedy written by William Shakespeare."
                ),
                com.example.ed.services.AIQuizQuestion(
                    question = "What year did World War II end?",
                    options = listOf("1944", "1945", "1946", "1947"),
                    correctIndex = 1,
                    explanation = "World War II ended in 1945."
                )
            )
        }
        
        // Initialize game with fallback questions
        initializeGameWithQuestions()
    }

    private fun startGameTimer() {
        gameTimer = object : CountDownTimer(GAME_TIME_LIMIT, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.tvGameTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                finishGame()
            }
        }.start()
    }

    private fun showCurrentQuestion() {
        if (currentQuestionIndex >= aiQuestions.size) {
            finishGame()
            return
        }
        
        val question = aiQuestions[currentQuestionIndex]
        
        // Update UI
        binding.tvQuestionNumber.text = "Question ${currentQuestionIndex + 1} of ${aiQuestions.size}"
        binding.tvQuestion.text = question.question
        
        // Set options
        val options = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        question.options.forEachIndexed { index, option ->
            if (index < options.size) {
                options[index].text = option
                options[index].visibility = View.VISIBLE
                options[index].isEnabled = true
                options[index].setBackgroundColor(requireContext().getColor(R.color.surface_color))
            }
        }
        
        // Hide unused options
        for (i in question.options.size until options.size) {
            options[i].visibility = View.GONE
        }
        
        // Update progress
        val progress = ((currentQuestionIndex + 1).toFloat() / aiQuestions.size * 100).toInt()
        animateProgressBar(binding.progressBarQuestions, progress)
        
        // Start question timer
        startQuestionTimer()
        
        // Hide next/finish buttons
        binding.btnNext.visibility = View.GONE
        binding.btnFinish.visibility = View.GONE
    }

    private fun startQuestionTimer() {
        questionTimer?.cancel()
        
        questionTimer = object : CountDownTimer(QUESTION_TIME_LIMIT, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.progressBarTime.progress = seconds
                binding.tvQuestionTimer.text = "${seconds}s"
            }

            override fun onFinish() {
                // Time's up - treat as wrong answer
                selectAnswer(-1)
            }
        }.start()
    }

    private fun selectAnswer(selectedIndex: Int) {
        questionTimer?.cancel()
        
        val question = aiQuestions[currentQuestionIndex]
        val options = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        
        // Disable all options
        options.forEach { it.isEnabled = false }
        
        // Show correct answer
        if (question.correctIndex < options.size) {
            options[question.correctIndex].setBackgroundColor(requireContext().getColor(android.R.color.holo_green_light))
        }
        
        // Show selected answer if wrong
        if (selectedIndex != -1 && selectedIndex != question.correctIndex && selectedIndex < options.size) {
            options[selectedIndex].setBackgroundColor(requireContext().getColor(android.R.color.holo_red_light))
        }
        
        // Update score and points
        if (selectedIndex == question.correctIndex) {
            val questionScore = getQuestionScore()
            score += questionScore
            pointsEarned += getPointsForCorrectAnswer()
            showFeedback("Correct! 🎉\n${question.explanation}", true)
        } else {
            showFeedback("Incorrect. ${question.explanation}", false)
        }
        
        // Update score display (add to feedback or status area)
        binding.tvFeedback.append("\nScore: $score | Points: +$pointsEarned")
        
        // Auto-advance to next question after 3 seconds (or show next/finish button)
        if (currentQuestionIndex < aiQuestions.size - 1) {
            binding.btnNext.visibility = View.VISIBLE
            // Auto-advance after 3 seconds for better flow
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded && currentQuestionIndex < aiQuestions.size - 1) {
                    nextQuestion()
                }
            }, 3000)
        } else {
            binding.btnFinish.visibility = View.VISIBLE
            // Auto-finish after 3 seconds if it's the last question
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) {
                    finishGame()
                }
            }, 3000)
        }
    }

    private fun getQuestionScore(): Int {
        return when (difficulty) {
            GameDifficulty.EASY -> 10
            GameDifficulty.MEDIUM -> 15
            GameDifficulty.HARD -> 20
        }
    }
    
    private fun getPointsForCorrectAnswer(): Int {
        return when (difficulty) {
            GameDifficulty.EASY -> 5
            GameDifficulty.MEDIUM -> 8
            GameDifficulty.HARD -> 12
        }
    }

    private fun showFeedback(message: String, isCorrect: Boolean) {
        binding.tvFeedback.text = message
        binding.tvFeedback.setTextColor(
            requireContext().getColor(
                if (isCorrect) android.R.color.holo_green_dark 
                else android.R.color.holo_red_dark
            )
        )
        binding.tvFeedback.visibility = View.VISIBLE
    }

    private fun nextQuestion() {
        currentQuestionIndex++
        binding.tvFeedback.visibility = View.GONE
        showCurrentQuestion()
    }

    private fun finishGame() {
        gameTimer?.cancel()
        questionTimer?.cancel()
        
        val endTime = System.currentTimeMillis()
        val timeSpent = endTime - startTime
        val maxScore = aiQuestions.size * getQuestionScore()
        val maxPoints = aiQuestions.size * getPointsForCorrectAnswer()
        
        // Award points if player did well
        val scorePercentage = (score.toFloat() / maxScore * 100).toInt()
        
        lifecycleScope.launch {
            try {
                if (scorePercentage >= 70) { // Award points for good performance
                    val bonusPoints = when {
                        scorePercentage >= 90 -> pointsEarned + 20 // Excellence bonus
                        scorePercentage >= 80 -> pointsEarned + 10 // Good performance bonus
                        else -> pointsEarned
                    }
                    
                    // Award points using PointsRewardsService
                    val pointsSuccess = pointsRewardsService.awardPoints(
                        PointsType.QUIZ_GAME_WIN, 
                        bonusPoints,
                        mapOf(
                            "score" to score,
                            "maxScore" to maxScore,
                            "difficulty" to difficulty.name,
                            "timeSpent" to timeSpent,
                            "subject" to (if (isMathFocused) "Mathematics" else "General")
                        )
                    )
                    
                    // Also save to GamificationService for UI display
                    try {
                        val gameResult = GameResult(
                            studentId = auth.currentUser?.uid ?: "",
                            gameType = if (isMathFocused) GameType.MATH_CHALLENGE else GameType.QUIZ,
                            difficulty = difficulty,
                            score = score,
                            maxScore = maxScore,
                            timeSpent = timeSpent,
                            completed = true,
                            rewardEarned = if (pointsSuccess) bonusPoints.toDouble() else 0.0,
                            playedAt = System.currentTimeMillis()
                        )
                        
                        gamificationService.saveGameResult(gameResult)
                        
                        // Trigger UI refresh callback
                        onPointsUpdated?.invoke()
                        
                    } catch (e: Exception) {
                        Log.e("QuizGame", "Error saving to gamification service", e)
                    }
                    
                    if (pointsSuccess) {
                        showGameResults(bonusPoints, scorePercentage, timeSpent)
                    } else {
                        showGameResults(0, scorePercentage, timeSpent)
                    }
                } else {
                    showGameResults(0, scorePercentage, timeSpent)
                }
            } catch (e: Exception) {
                showGameResults(0, scorePercentage, timeSpent)
            }
        }
    }
    
    private fun showGameResults(earnedPoints: Int, scorePercentage: Int, timeSpent: Long) {
        val minutes = timeSpent / 60000
        val seconds = (timeSpent % 60000) / 1000
        
        val resultMessage = buildString {
            appendLine("🎯 Quiz Complete!")
            appendLine()
            appendLine("📊 Your Results:")
            appendLine("Score: $score / ${aiQuestions.size * getQuestionScore()}")
            appendLine("Accuracy: ${scorePercentage}%")
            appendLine("Time: ${minutes}m ${seconds}s")
            appendLine()
            if (earnedPoints > 0) {
                appendLine("🎉 Points Earned: +$earnedPoints")
                appendLine("Keep playing to earn more points!")
            } else {
                appendLine("💪 Keep practicing to earn points!")
                appendLine("Score 70%+ to earn rewards!")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Game Results")
            .setMessage(resultMessage)
            .setPositiveButton("Play Again") { _, _ ->
                // Restart game with new AI questions
                loadAIQuestions()
            }
            .setNegativeButton("Back to Games") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun animateProgressBar(progressBar: android.widget.ProgressBar, targetProgress: Int) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
        animator.duration = 500
        animator.start()
    }

    fun setGameResultListener(listener: (GameResult) -> Unit) {
        gameResultListener = listener
    }
    
    fun setPointsUpdateCallback(callback: () -> Unit) {
        onPointsUpdated = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gameTimer?.cancel()
        questionTimer?.cancel()
        _binding = null
    }
    
    private fun getRandomSubject(): String {
        val subjects = listOf("Science", "History", "Geography", "Literature", "General Knowledge")
        return subjects.random()
    }
}
