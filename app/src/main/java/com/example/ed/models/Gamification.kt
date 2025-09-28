package com.example.ed.models

import java.util.*

// Game Types
enum class GameType {
    QUIZ,
    PUZZLE,
    MEMORY_GAME,
    WORD_MATCH,
    MATH_CHALLENGE
}

// Difficulty Levels
enum class GameDifficulty {
    EASY,
    MEDIUM,
    HARD
}

// Game Result
data class GameResult(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val gameType: GameType = GameType.QUIZ,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val score: Int = 0,
    val maxScore: Int = 100,
    val timeSpent: Long = 0, // in milliseconds
    val completed: Boolean = false,
    val rewardEarned: Double = 0.0, // discount amount in dollars
    val playedAt: Long = System.currentTimeMillis()
)

// Student Reward
data class StudentReward(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val gameResultId: String = "",
    val discountAmount: Double = 0.0, // in dollars
    val discountPercentage: Double = 0.0, // percentage off
    val description: String = "",
    val isRedeemed: Boolean = false,
    val redeemedAt: Long? = null,
    val expiresAt: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L), // 30 days
    val earnedAt: Long = System.currentTimeMillis()
)

// Student Achievement
data class StudentAchievement(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val achievementType: AchievementType = AchievementType.FIRST_WIN,
    val title: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val rewardAmount: Double = 0.0,
    val unlockedAt: Long = System.currentTimeMillis()
)

// Achievement Types
enum class AchievementType {
    FIRST_WIN,
    STREAK_3,
    STREAK_5,
    STREAK_10,
    PERFECT_SCORE,
    SPEED_DEMON,
    GAME_MASTER,
    QUIZ_CHAMPION,
    PUZZLE_SOLVER,
    MEMORY_EXPERT
}

// Game Progress
data class GameProgress(
    val studentId: String = "",
    val totalGamesPlayed: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Double = 0.0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalRewardsEarned: Double = 0.0,
    val totalRewardsRedeemed: Double = 0.0,
    val gamesWon: Int = 0,
    val perfectScores: Int = 0,
    val lastPlayedAt: Long = 0L,
    val level: Int = 1,
    val experiencePoints: Int = 0
)

// Game Quiz Question
data class GameQuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: Int = 0,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val category: String = "",
    val explanation: String = ""
)

// Puzzle Piece
data class PuzzlePiece(
    val id: Int = 0,
    val correctPosition: Int = 0,
    var currentPosition: Int = 0,
    val imageResource: String = "",
    var isPlaced: Boolean = false
)

// Memory Card
data class MemoryCard(
    val id: Int = 0,
    val pairId: Int = 0,
    val imageResource: String = "",
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false,
    val content: String = ""
)

// Game Session
data class GameSession(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val gameType: GameType = GameType.QUIZ,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val isActive: Boolean = true,
    val currentScore: Int = 0,
    val questionsAnswered: Int = 0,
    val totalQuestions: Int = 0
)