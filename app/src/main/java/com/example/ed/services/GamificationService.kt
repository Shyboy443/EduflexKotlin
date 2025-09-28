package com.example.ed.services

import android.content.Context
import android.util.Log
import com.example.ed.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.math.min
import kotlin.random.Random

class GamificationService private constructor(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        @Volatile
        private var INSTANCE: GamificationService? = null
        private const val TAG = "GamificationService"
        
        // Collections
        private const val COLLECTION_GAME_RESULTS = "game_results"
        private const val COLLECTION_STUDENT_REWARDS = "student_rewards"
        private const val COLLECTION_STUDENT_ACHIEVEMENTS = "student_achievements"
        private const val COLLECTION_GAME_PROGRESS = "game_progress"
        private const val COLLECTION_QUIZ_QUESTIONS = "quiz_questions"
        
        // Reward Configuration
        private const val BASE_REWARD_AMOUNT = 0.50 // $0.50 base reward
        private const val PERFECT_SCORE_BONUS = 1.00 // $1.00 bonus for perfect score
        private const val STREAK_BONUS_MULTIPLIER = 0.25 // $0.25 per streak
        private const val MAX_DAILY_REWARDS = 10.00 // Maximum $10 per day
        
        fun getInstance(context: Context): GamificationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamificationService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Save game result and calculate rewards
    suspend fun saveGameResult(gameResult: GameResult): StudentReward? {
        return try {
            val studentId = auth.currentUser?.uid ?: return null
            val resultWithStudentId = gameResult.copy(studentId = studentId)
            
            // Save game result
            firestore.collection(COLLECTION_GAME_RESULTS)
                .document(resultWithStudentId.id)
                .set(resultWithStudentId)
                .await()
            
            // Calculate and save reward
            val reward = calculateReward(resultWithStudentId)
            if (reward.discountAmount > 0) {
                firestore.collection(COLLECTION_STUDENT_REWARDS)
                    .document(reward.id)
                    .set(reward)
                    .await()
                
                // Update game progress
                updateGameProgress(resultWithStudentId, reward)
                
                // Check for achievements
                checkAndUnlockAchievements(studentId, resultWithStudentId)
                
                return reward
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result", e)
            null
        }
    }
    
    // Calculate reward based on game performance
    private suspend fun calculateReward(gameResult: GameResult): StudentReward {
        val studentId = gameResult.studentId
        val scorePercentage = (gameResult.score.toDouble() / gameResult.maxScore) * 100
        
        // Base reward calculation
        var rewardAmount = 0.0
        var description = ""
        
        when {
            scorePercentage >= 90 -> {
                rewardAmount = BASE_REWARD_AMOUNT + PERFECT_SCORE_BONUS
                description = "Perfect Performance! 🌟"
            }
            scorePercentage >= 80 -> {
                rewardAmount = BASE_REWARD_AMOUNT + 0.50
                description = "Excellent Work! 🎉"
            }
            scorePercentage >= 70 -> {
                rewardAmount = BASE_REWARD_AMOUNT + 0.25
                description = "Great Job! 👏"
            }
            scorePercentage >= 60 -> {
                rewardAmount = BASE_REWARD_AMOUNT
                description = "Good Effort! 👍"
            }
        }
        
        // Apply streak bonus
        val progress = getGameProgress(studentId)
        val streakBonus = min(progress.currentStreak * STREAK_BONUS_MULTIPLIER, 2.0)
        rewardAmount += streakBonus
        
        if (streakBonus > 0) {
            description += " Streak Bonus: +$${String.format("%.2f", streakBonus)}"
        }
        
        // Check daily limit
        val todayRewards = getTodayRewards(studentId)
        val totalTodayRewards = todayRewards.sumOf { it.discountAmount }
        
        if (totalTodayRewards + rewardAmount > MAX_DAILY_REWARDS) {
            rewardAmount = MAX_DAILY_REWARDS - totalTodayRewards
            if (rewardAmount <= 0) {
                rewardAmount = 0.0
                description = "Daily reward limit reached. Try again tomorrow!"
            }
        }
        
        return StudentReward(
            studentId = studentId,
            gameResultId = gameResult.id,
            discountAmount = rewardAmount,
            discountPercentage = (rewardAmount / 50.0) * 100, // Assume $50 average course price
            description = description
        )
    }
    
    // Update game progress
    private suspend fun updateGameProgress(gameResult: GameResult, reward: StudentReward) {
        val studentId = gameResult.studentId
        val currentProgress = getGameProgress(studentId)
        
        val scorePercentage = (gameResult.score.toDouble() / gameResult.maxScore) * 100
        val isWin = scorePercentage >= 60
        val isPerfect = scorePercentage >= 90
        
        val updatedProgress = currentProgress.copy(
            totalGamesPlayed = currentProgress.totalGamesPlayed + 1,
            totalScore = currentProgress.totalScore + gameResult.score,
            averageScore = (currentProgress.totalScore + gameResult.score).toDouble() / (currentProgress.totalGamesPlayed + 1),
            currentStreak = if (isWin) currentProgress.currentStreak + 1 else 0,
            longestStreak = if (isWin) maxOf(currentProgress.longestStreak, currentProgress.currentStreak + 1) else currentProgress.longestStreak,
            totalRewardsEarned = currentProgress.totalRewardsEarned + reward.discountAmount,
            gamesWon = if (isWin) currentProgress.gamesWon + 1 else currentProgress.gamesWon,
            perfectScores = if (isPerfect) currentProgress.perfectScores + 1 else currentProgress.perfectScores,
            lastPlayedAt = System.currentTimeMillis(),
            experiencePoints = currentProgress.experiencePoints + calculateExperiencePoints(gameResult),
            level = calculateLevel(currentProgress.experiencePoints + calculateExperiencePoints(gameResult))
        )
        
        firestore.collection(COLLECTION_GAME_PROGRESS)
            .document(studentId)
            .set(updatedProgress)
            .await()
    }
    
    // Calculate experience points
    private fun calculateExperiencePoints(gameResult: GameResult): Int {
        val baseXP = when (gameResult.difficulty) {
            GameDifficulty.EASY -> 10
            GameDifficulty.MEDIUM -> 20
            GameDifficulty.HARD -> 30
        }
        
        val scoreMultiplier = gameResult.score.toDouble() / gameResult.maxScore
        return (baseXP * scoreMultiplier).toInt()
    }
    
    // Calculate level from experience points
    private fun calculateLevel(xp: Int): Int {
        return (xp / 100) + 1 // 100 XP per level
    }
    
    // Get game progress for student
    suspend fun getGameProgress(studentId: String): GameProgress {
        return try {
            val document = firestore.collection(COLLECTION_GAME_PROGRESS)
                .document(studentId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(GameProgress::class.java) ?: GameProgress(studentId = studentId)
            } else {
                GameProgress(studentId = studentId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting game progress", e)
            GameProgress(studentId = studentId)
        }
    }
    
    // Get student rewards
    suspend fun getStudentRewards(studentId: String): List<StudentReward> {
        return try {
            val snapshot = firestore.collection(COLLECTION_STUDENT_REWARDS)
                .whereEqualTo("studentId", studentId)
                .orderBy("earnedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.toObject(StudentReward::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student rewards", e)
            emptyList()
        }
    }
    
    // Get today's rewards
    private suspend fun getTodayRewards(studentId: String): List<StudentReward> {
        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        
        return try {
            val snapshot = firestore.collection(COLLECTION_STUDENT_REWARDS)
                .whereEqualTo("studentId", studentId)
                .whereGreaterThanOrEqualTo("earnedAt", startOfDay)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.toObject(StudentReward::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's rewards", e)
            emptyList()
        }
    }
    
    // Redeem reward
    suspend fun redeemReward(rewardId: String): Boolean {
        return try {
            firestore.collection(COLLECTION_STUDENT_REWARDS)
                .document(rewardId)
                .update(
                    mapOf(
                        "isRedeemed" to true,
                        "redeemedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error redeeming reward", e)
            false
        }
    }
    
    // Check and unlock achievements
    private suspend fun checkAndUnlockAchievements(studentId: String, gameResult: GameResult) {
        val progress = getGameProgress(studentId)
        val achievements = mutableListOf<StudentAchievement>()
        
        // First win achievement
        if (progress.gamesWon == 1) {
            achievements.add(createAchievement(studentId, AchievementType.FIRST_WIN, "First Victory!", "Won your first game", 1.0))
        }
        
        // Streak achievements
        when (progress.currentStreak) {
            3 -> achievements.add(createAchievement(studentId, AchievementType.STREAK_3, "On Fire!", "3 games won in a row", 2.0))
            5 -> achievements.add(createAchievement(studentId, AchievementType.STREAK_5, "Unstoppable!", "5 games won in a row", 3.0))
            10 -> achievements.add(createAchievement(studentId, AchievementType.STREAK_10, "Legendary!", "10 games won in a row", 5.0))
        }
        
        // Perfect score achievement
        if ((gameResult.score.toDouble() / gameResult.maxScore) >= 0.9 && progress.perfectScores == 1) {
            achievements.add(createAchievement(studentId, AchievementType.PERFECT_SCORE, "Perfectionist!", "Achieved a perfect score", 2.0))
        }
        
        // Save achievements
        achievements.forEach { achievement ->
            firestore.collection(COLLECTION_STUDENT_ACHIEVEMENTS)
                .document(achievement.id)
                .set(achievement)
                .await()
        }
    }
    
    // Create achievement
    private fun createAchievement(studentId: String, type: AchievementType, title: String, description: String, rewardAmount: Double): StudentAchievement {
        return StudentAchievement(
            studentId = studentId,
            achievementType = type,
            title = title,
            description = description,
            rewardAmount = rewardAmount
        )
    }
    
    // Get quiz questions
    suspend fun getQuizQuestions(difficulty: GameDifficulty, count: Int = 10): List<GameQuizQuestion> {
        return try {
            val snapshot = firestore.collection(COLLECTION_QUIZ_QUESTIONS)
                .whereEqualTo("difficulty", difficulty.name)
                .limit(count.toLong())
                .get()
                .await()
            
            val questions = snapshot.documents.mapNotNull { it.toObject(GameQuizQuestion::class.java) }
            
            // If no questions in database, return sample questions
            if (questions.isEmpty()) {
                getSampleQuizQuestions(difficulty, count)
            } else {
                questions.shuffled().take(count)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quiz questions", e)
            getSampleQuizQuestions(difficulty, count)
        }
    }
    
    // Get sample quiz questions
    private fun getSampleQuizQuestions(difficulty: GameDifficulty, count: Int): List<GameQuizQuestion> {
        val questions = when (difficulty) {
            GameDifficulty.EASY -> listOf(
                GameQuizQuestion(question = "What is 2 + 2?", options = listOf("3", "4", "5", "6"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "What color do you get when you mix red and blue?", options = listOf("Green", "Purple", "Orange", "Yellow"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "How many days are in a week?", options = listOf("5", "6", "7", "8"), correctAnswer = 2, difficulty = difficulty),
                GameQuizQuestion(question = "What is the capital of France?", options = listOf("London", "Berlin", "Paris", "Madrid"), correctAnswer = 2, difficulty = difficulty),
                GameQuizQuestion(question = "Which animal is known as the King of the Jungle?", options = listOf("Tiger", "Lion", "Elephant", "Bear"), correctAnswer = 1, difficulty = difficulty)
            )
            GameDifficulty.MEDIUM -> listOf(
                GameQuizQuestion(question = "What is 15 × 8?", options = listOf("120", "125", "115", "130"), correctAnswer = 0, difficulty = difficulty),
                GameQuizQuestion(question = "Who wrote Romeo and Juliet?", options = listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "What is the chemical symbol for gold?", options = listOf("Go", "Gd", "Au", "Ag"), correctAnswer = 2, difficulty = difficulty),
                GameQuizQuestion(question = "Which planet is closest to the Sun?", options = listOf("Venus", "Mercury", "Earth", "Mars"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "In which year did World War II end?", options = listOf("1944", "1945", "1946", "1947"), correctAnswer = 1, difficulty = difficulty)
            )
            GameDifficulty.HARD -> listOf(
                GameQuizQuestion(question = "What is the square root of 144?", options = listOf("11", "12", "13", "14"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "Who developed the theory of relativity?", options = listOf("Isaac Newton", "Albert Einstein", "Galileo Galilei", "Stephen Hawking"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "What is the largest ocean on Earth?", options = listOf("Atlantic", "Indian", "Arctic", "Pacific"), correctAnswer = 3, difficulty = difficulty),
                GameQuizQuestion(question = "Which element has the atomic number 1?", options = listOf("Helium", "Hydrogen", "Lithium", "Carbon"), correctAnswer = 1, difficulty = difficulty),
                GameQuizQuestion(question = "Who painted the Mona Lisa?", options = listOf("Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci", "Michelangelo"), correctAnswer = 2, difficulty = difficulty)
            )
        }
        
        return questions.shuffled().take(count)
    }
    
    // Get student achievements
    suspend fun getStudentAchievements(studentId: String): List<StudentAchievement> {
        return try {
            val snapshot = firestore.collection(COLLECTION_STUDENT_ACHIEVEMENTS)
                .whereEqualTo("studentId", studentId)
                .orderBy("unlockedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.toObject(StudentAchievement::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student achievements", e)
            emptyList()
        }
    }
}