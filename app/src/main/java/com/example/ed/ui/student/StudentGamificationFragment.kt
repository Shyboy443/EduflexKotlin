package com.example.ed.ui.student

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.R
import com.example.ed.databinding.FragmentStudentGamificationBinding
import com.example.ed.models.*
import com.example.ed.services.GamificationService
import com.example.ed.ui.student.games.QuizGameFragment
import com.example.ed.ui.student.games.PuzzleGameFragment
import com.example.ed.ui.student.games.MemoryGameFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class StudentGamificationFragment : Fragment() {

    private var _binding: FragmentStudentGamificationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var gamificationService: GamificationService
    
    private lateinit var rewardsAdapter: RewardsAdapter
    private lateinit var achievementsAdapter: AchievementsAdapter
    
    private val rewards = mutableListOf<StudentReward>()
    private val achievements = mutableListOf<StudentAchievement>()
    
    private var gameProgress: GameProgress? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentGamificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize services
        auth = FirebaseAuth.getInstance()
        gamificationService = GamificationService.getInstance(requireContext())
        
        setupUI()
        setupClickListeners()
        setupRecyclerViews()
        loadGameData()
    }

    private fun setupUI() {
        // Set up progress bars and initial values
        binding.progressBarLevel.max = 100
        binding.progressBarDailyGoal.max = 100
        
        // Show loading state
        binding.progressBarMain.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
    }

    private fun setupClickListeners() {
        // Game buttons
        binding.btnQuizGame.setOnClickListener {
            startGame(GameType.QUIZ)
        }
        
        binding.btnPuzzleGame.setOnClickListener {
            startGame(GameType.PUZZLE)
        }
        
        binding.btnMemoryGame.setOnClickListener {
            startGame(GameType.MEMORY_GAME)
        }
        
        binding.btnMathChallenge.setOnClickListener {
            startGame(GameType.MATH_CHALLENGE)
        }
        
        binding.btnWordMatch.setOnClickListener {
            startGame(GameType.WORD_MATCH)
        }
        
        // Difficulty selection
        binding.chipEasy.setOnClickListener {
            updateDifficultySelection(GameDifficulty.EASY)
        }
        
        binding.chipMedium.setOnClickListener {
            updateDifficultySelection(GameDifficulty.MEDIUM)
        }
        
        binding.chipHard.setOnClickListener {
            updateDifficultySelection(GameDifficulty.HARD)
        }
        
        // Refresh button
        binding.btnRefresh.setOnClickListener {
            loadGameData()
        }
        
        // View all rewards
        binding.btnViewAllRewards.setOnClickListener {
            showAllRewards()
        }
        
        // View all achievements
        binding.btnViewAllAchievements.setOnClickListener {
            showAllAchievements()
        }
    }

    private fun setupRecyclerViews() {
        // Rewards RecyclerView
        rewardsAdapter = RewardsAdapter(rewards) { reward ->
            showRewardDetails(reward)
        }
        binding.rvRewards.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRewards.adapter = rewardsAdapter
        
        // Achievements RecyclerView
        achievementsAdapter = AchievementsAdapter(achievements) { achievement ->
            showAchievementDetails(achievement)
        }
        binding.rvAchievements.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvAchievements.adapter = achievementsAdapter
    }

    private fun loadGameData() {
        val currentUser = auth.currentUser ?: return
        
        lifecycleScope.launch {
            try {
                // Load game progress
                gameProgress = gamificationService.getGameProgress(currentUser.uid)
                
                // Load rewards
                val allRewards = gamificationService.getStudentRewards(currentUser.uid)
                rewards.clear()
                rewards.addAll(allRewards.take(5)) // Show latest 5 rewards
                
                // Load achievements
                val allAchievements = gamificationService.getStudentAchievements(currentUser.uid)
                achievements.clear()
                achievements.addAll(allAchievements.take(5)) // Show latest 5 achievements
                
                // Update UI
                updateUI()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load game data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarMain.visibility = View.GONE
                binding.layoutContent.visibility = View.VISIBLE
            }
        }
    }

    private fun updateUI() {
        gameProgress?.let { progress ->
            // Update stats
            binding.tvLevel.text = "Level ${progress.level}"
            binding.tvTotalScore.text = NumberFormat.getNumberInstance().format(progress.totalScore)
            binding.tvGamesPlayed.text = progress.totalGamesPlayed.toString()
            binding.tvCurrentStreak.text = progress.currentStreak.toString()
            binding.tvTotalRewards.text = "$${String.format("%.2f", progress.totalRewardsEarned)}"
            
            // Update progress bars with animation
            val levelProgress = (progress.experiencePoints % 100)
            animateProgressBar(binding.progressBarLevel, levelProgress)
            
            val dailyGoalProgress = minOf((progress.totalGamesPlayed % 10) * 10, 100)
            animateProgressBar(binding.progressBarDailyGoal, dailyGoalProgress)
            
            // Update average score
            binding.tvAverageScore.text = "${String.format("%.1f", progress.averageScore)}%"
            
            // Update experience points
            binding.tvExperiencePoints.text = "${progress.experiencePoints} XP"
        }
        
        // Update adapters
        rewardsAdapter.notifyDataSetChanged()
        achievementsAdapter.notifyDataSetChanged()
        
        // Update empty states
        binding.layoutEmptyRewards.visibility = if (rewards.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRewards.visibility = if (rewards.isEmpty()) View.GONE else View.VISIBLE
        
        binding.layoutEmptyAchievements.visibility = if (achievements.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAchievements.visibility = if (achievements.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun animateProgressBar(progressBar: android.widget.ProgressBar, targetProgress: Int) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
        animator.duration = 1000
        animator.start()
    }

    private fun updateDifficultySelection(difficulty: GameDifficulty) {
        // Reset all chips
        binding.chipEasy.isChecked = false
        binding.chipMedium.isChecked = false
        binding.chipHard.isChecked = false
        
        // Set selected chip
        when (difficulty) {
            GameDifficulty.EASY -> binding.chipEasy.isChecked = true
            GameDifficulty.MEDIUM -> binding.chipMedium.isChecked = true
            GameDifficulty.HARD -> binding.chipHard.isChecked = true
        }
    }

    private fun getSelectedDifficulty(): GameDifficulty {
        return when {
            binding.chipEasy.isChecked -> GameDifficulty.EASY
            binding.chipMedium.isChecked -> GameDifficulty.MEDIUM
            binding.chipHard.isChecked -> GameDifficulty.HARD
            else -> GameDifficulty.EASY
        }
    }

    private fun startGame(gameType: GameType) {
        val difficulty = getSelectedDifficulty()
        
        val fragment = when (gameType) {
            GameType.QUIZ -> QuizGameFragment.newInstance(difficulty)
            GameType.PUZZLE -> PuzzleGameFragment.newInstance(difficulty)
            GameType.MEMORY_GAME -> MemoryGameFragment.newInstance(difficulty)
            GameType.MATH_CHALLENGE -> QuizGameFragment.newInstance(difficulty, true) // Math-focused quiz
            GameType.WORD_MATCH -> MemoryGameFragment.newInstance(difficulty, true) // Word-focused memory
        }
        
        // Set result listener
        fragment.setGameResultListener { gameResult ->
            handleGameResult(gameResult)
        }
        
        // Set points update callback for UI refresh
        when (fragment) {
            is QuizGameFragment -> {
                fragment.setPointsUpdateCallback {
                    loadGameData() // Refresh the UI when points are updated
                }
            }
            is MemoryGameFragment -> {
                fragment.setPointsUpdateCallback {
                    loadGameData() // Refresh the UI when points are updated
                }
            }
        }
        
        // Navigate to game
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleGameResult(gameResult: GameResult) {
        lifecycleScope.launch {
            try {
                // Save game result and get reward
                val reward = gamificationService.saveGameResult(gameResult)
                
                // Show result dialog
                showGameResultDialog(gameResult, reward)
                
                // Refresh data
                loadGameData()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save game result: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showGameResultDialog(gameResult: GameResult, reward: StudentReward?) {
        val scorePercentage = (gameResult.score.toDouble() / gameResult.maxScore) * 100
        
        val title = when {
            scorePercentage >= 90 -> "🌟 Perfect!"
            scorePercentage >= 80 -> "🎉 Excellent!"
            scorePercentage >= 70 -> "👏 Great Job!"
            scorePercentage >= 60 -> "👍 Good Work!"
            else -> "💪 Keep Trying!"
        }
        
        val message = buildString {
            append("Score: ${gameResult.score}/${gameResult.maxScore} (${String.format("%.1f", scorePercentage)}%)\n")
            append("Time: ${formatTime(gameResult.timeSpent)}\n\n")
            
            if (reward != null && reward.discountAmount > 0) {
                append("🎁 Reward Earned!\n")
                append("${reward.description}\n")
                append("Discount: $${String.format("%.2f", reward.discountAmount)}")
            } else {
                append("Keep playing to earn rewards!")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                // Return to gamification fragment
            }
            .setNeutralButton("Play Again") { _, _ ->
                startGame(gameResult.gameType)
            }
            .show()
    }

    private fun formatTime(timeInMillis: Long): String {
        val seconds = timeInMillis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }

    private fun showRewardDetails(reward: StudentReward) {
        val message = buildString {
            append("${reward.description}\n\n")
            append("Discount Amount: $${String.format("%.2f", reward.discountAmount)}\n")
            append("Earned: ${java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(reward.earnedAt))}\n")
            append("Status: ${if (reward.isRedeemed) "Redeemed" else "Available"}\n")
            
            if (!reward.isRedeemed) {
                append("Expires: ${java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(reward.expiresAt))}")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reward Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAchievementDetails(achievement: StudentAchievement) {
        val message = buildString {
            append("${achievement.description}\n\n")
            append("Reward: $${String.format("%.2f", achievement.rewardAmount)}\n")
            append("Unlocked: ${java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(achievement.unlockedAt))}")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(achievement.title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAllRewards() {
        // TODO: Navigate to detailed rewards screen
        Toast.makeText(requireContext(), "All rewards screen coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showAllAchievements() {
        // TODO: Navigate to detailed achievements screen
        Toast.makeText(requireContext(), "All achievements screen coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Interface for game result callback
interface GameResultListener {
    fun onGameResult(gameResult: GameResult)
}

// Extension function for game fragments
fun Fragment.setGameResultListener(listener: (GameResult) -> Unit) {
    if (this is GameResultListener) {
        // This would be implemented in each game fragment
    }
}