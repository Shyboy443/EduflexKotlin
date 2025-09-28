package com.example.ed.services

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class PointsRewardsService private constructor(private val context: Context) {
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "PointsRewards"
        
        // Points earning rates
        const val POINTS_QUIZ_GAME_WIN = 50
        const val POINTS_MEMORY_GAME_WIN = 30
        const val POINTS_PUZZLE_GAME_WIN = 40
        const val POINTS_COURSE_COMPLETION = 500
        const val POINTS_QUIZ_PASS = 100
        const val POINTS_DAILY_LOGIN = 10
        const val POINTS_STREAK_BONUS = 20
        
        // Discount rates (points to percentage)
        const val POINTS_PER_DISCOUNT_PERCENT = 100 // 100 points = 1% discount
        const val MAX_DISCOUNT_PERCENT = 50 // Maximum 50% discount
        
        @Volatile
        private var INSTANCE: PointsRewardsService? = null
        
        fun getInstance(context: Context): PointsRewardsService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PointsRewardsService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Award points for different activities
    suspend fun awardPoints(pointsType: PointsType, amount: Int = 0, metadata: Map<String, Any> = emptyMap()): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val pointsAmount = if (amount > 0) amount else getDefaultPoints(pointsType)
            
            // Create points transaction
            val transaction = hashMapOf(
                "userId" to userId,
                "pointsType" to pointsType.name,
                "amount" to pointsAmount,
                "timestamp" to System.currentTimeMillis(),
                "metadata" to metadata
            )
            
            // Add to points_transactions collection
            db.collection("points_transactions").add(transaction)
            
            // Update user's total points
            val userPointsRef = db.collection("user_points").document(userId)
            val currentData = try {
                userPointsRef.get().await()
            } catch (e: Exception) {
                null
            }
            
            val currentPoints = currentData?.getLong("totalPoints") ?: 0L
            val newTotalPoints = currentPoints + pointsAmount
            
            val userPointsData = hashMapOf(
                "userId" to userId,
                "totalPoints" to newTotalPoints,
                "lifetimePoints" to FieldValue.increment(pointsAmount.toLong()),
                "lastUpdated" to System.currentTimeMillis(),
                "level" to calculateLevel(newTotalPoints),
                "nextLevelPoints" to getPointsForNextLevel(newTotalPoints)
            )
            
            userPointsRef.set(userPointsData)
            
            Log.d(TAG, "Awarded $pointsAmount points for ${pointsType.name}. Total: $newTotalPoints")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error awarding points", e)
            false
        }
    }
    
    // Get user's current points (simple version)
    suspend fun getCurrentPoints(): Long {
        return try {
            val userId = auth.currentUser?.uid ?: return 0L
            val doc = db.collection("user_points").document(userId).get().await()
            doc.getLong("totalPoints") ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current points", e)
            0L
        }
    }
    
    // Use points for discount (alias for spendPoints)
    suspend fun usePointsForDiscount(pointsToUse: Int): Boolean {
        return spendPoints(pointsToUse, "Course discount")
    }
    
    // Get user's current points
    suspend fun getUserPoints(): UserPoints? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val doc = db.collection("user_points").document(userId).get().await()
            
            if (doc.exists()) {
                UserPoints(
                    userId = userId,
                    totalPoints = doc.getLong("totalPoints") ?: 0,
                    lifetimePoints = doc.getLong("lifetimePoints") ?: 0,
                    level = doc.getLong("level")?.toInt() ?: 1,
                    nextLevelPoints = doc.getLong("nextLevelPoints") ?: 100
                )
            } else {
                // Create initial user points record
                val initialPoints = UserPoints(userId, 0, 0, 1, 100)
                val data = hashMapOf(
                    "userId" to userId,
                    "totalPoints" to 0L,
                    "lifetimePoints" to 0L,
                    "level" to 1,
                    "nextLevelPoints" to 100L,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("user_points").document(userId).set(data)
                initialPoints
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user points", e)
            null
        }
    }
    
    // Calculate discount percentage based on points
    fun calculateDiscountPercentage(totalPoints: Long): Int {
        val discountPercent = (totalPoints / POINTS_PER_DISCOUNT_PERCENT).toInt()
        return minOf(discountPercent, MAX_DISCOUNT_PERCENT)
    }
    
    // Apply discount to course price
    fun applyDiscount(originalPrice: Double, pointsToUse: Int): DiscountResult {
        val maxPointsUsable = (originalPrice * MAX_DISCOUNT_PERCENT / 100 * POINTS_PER_DISCOUNT_PERCENT).toInt()
        val actualPointsUsed = minOf(pointsToUse, maxPointsUsable)
        val discountAmount = actualPointsUsed / POINTS_PER_DISCOUNT_PERCENT.toDouble()
        val finalPrice = maxOf(originalPrice - discountAmount, originalPrice * 0.5) // Minimum 50% of original price
        
        return DiscountResult(
            originalPrice = originalPrice,
            discountAmount = originalPrice - finalPrice,
            finalPrice = finalPrice,
            pointsUsed = actualPointsUsed,
            discountPercentage = ((originalPrice - finalPrice) / originalPrice * 100).toInt()
        )
    }
    
    // Spend points for discounts
    suspend fun spendPoints(amount: Int, reason: String = "Course discount"): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val userPoints = getUserPoints() ?: return false
            
            if (userPoints.totalPoints < amount) {
                return false // Not enough points
            }
            
            // Create spending transaction
            val transaction = hashMapOf(
                "userId" to userId,
                "pointsType" to "SPENT",
                "amount" to -amount,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )
            
            db.collection("points_transactions").add(transaction)
            
            // Update user's total points
            val newTotalPoints = userPoints.totalPoints - amount
            val userPointsData = hashMapOf(
                "totalPoints" to newTotalPoints,
                "level" to calculateLevel(newTotalPoints),
                "nextLevelPoints" to getPointsForNextLevel(newTotalPoints),
                "lastUpdated" to System.currentTimeMillis()
            )
            
            db.collection("user_points").document(userId).update(userPointsData.toMap()).await()
            
            Log.d(TAG, "Spent $amount points for $reason. Remaining: $newTotalPoints")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error spending points", e)
            false
        }
    }
    
    // Get points transaction history
    suspend fun getPointsHistory(limit: Int = 20): List<PointsTransaction> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            
            val docs = db.collection("points_transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            docs.documents.mapNotNull { doc ->
                try {
                    PointsTransaction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        pointsType = doc.getString("pointsType") ?: "",
                        amount = doc.getLong("amount")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        reason = doc.getString("reason"),
                        metadata = doc.get("metadata") as? Map<String, Any> ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing points transaction", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting points history", e)
            emptyList()
        }
    }
    
    private fun getDefaultPoints(pointsType: PointsType): Int {
        return when (pointsType) {
            PointsType.QUIZ_GAME_WIN -> POINTS_QUIZ_GAME_WIN
            PointsType.MEMORY_GAME_WIN -> POINTS_MEMORY_GAME_WIN
            PointsType.PUZZLE_GAME_WIN -> POINTS_PUZZLE_GAME_WIN
            PointsType.COURSE_COMPLETION -> POINTS_COURSE_COMPLETION
            PointsType.QUIZ_PASS -> POINTS_QUIZ_PASS
            PointsType.DAILY_LOGIN -> POINTS_DAILY_LOGIN
            PointsType.STREAK_BONUS -> POINTS_STREAK_BONUS
        }
    }
    
    private fun calculateLevel(totalPoints: Long): Int {
        return when {
            totalPoints < 100 -> 1
            totalPoints < 300 -> 2
            totalPoints < 600 -> 3
            totalPoints < 1000 -> 4
            totalPoints < 1500 -> 5
            totalPoints < 2500 -> 6
            totalPoints < 4000 -> 7
            totalPoints < 6000 -> 8
            totalPoints < 10000 -> 9
            else -> 10
        }
    }
    
    private fun getPointsForNextLevel(currentPoints: Long): Long {
        val level = calculateLevel(currentPoints)
        return when (level) {
            1 -> 100 - currentPoints
            2 -> 300 - currentPoints
            3 -> 600 - currentPoints
            4 -> 1000 - currentPoints
            5 -> 1500 - currentPoints
            6 -> 2500 - currentPoints
            7 -> 4000 - currentPoints
            8 -> 6000 - currentPoints
            9 -> 10000 - currentPoints
            else -> 0
        }
    }
}

// Data classes
data class UserPoints(
    val userId: String,
    val totalPoints: Long,
    val lifetimePoints: Long,
    val level: Int,
    val nextLevelPoints: Long
)

data class PointsTransaction(
    val id: String,
    val userId: String,
    val pointsType: String,
    val amount: Int,
    val timestamp: Long,
    val reason: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

data class DiscountResult(
    val originalPrice: Double,
    val discountAmount: Double,
    val finalPrice: Double,
    val pointsUsed: Int,
    val discountPercentage: Int
)

enum class PointsType {
    QUIZ_GAME_WIN,
    MEMORY_GAME_WIN,
    PUZZLE_GAME_WIN,
    COURSE_COMPLETION,
    QUIZ_PASS,
    DAILY_LOGIN,
    STREAK_BONUS
}
