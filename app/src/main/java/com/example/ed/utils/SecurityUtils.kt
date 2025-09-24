package com.example.ed.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Security utility class for handling authentication and authorization
 */
object SecurityUtils {
    
    private const val TAG = "SecurityUtils"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Verifies if the current user has the required role
     */
    suspend fun verifyUserRole(requiredRole: String): Boolean {
        return try {
            val currentUser = auth.currentUser
            Log.d(TAG, "verifyUserRole: Checking role '$requiredRole' for user: ${currentUser?.uid}")
            
            if (currentUser == null) {
                Log.w(TAG, "verifyUserRole: No current user found")
                return false
            }
            
            val document = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            Log.d(TAG, "verifyUserRole: Document exists: ${document.exists()}")
            
            if (document.exists()) {
                val userRole = document.getString("role") ?: "Student"
                Log.d(TAG, "verifyUserRole: User role from Firebase: '$userRole'")
                Log.d(TAG, "verifyUserRole: All document data: ${document.data}")
                
                val hasAccess = when (requiredRole) {
                    "Admin" -> userRole == "Admin"
                    "Teacher" -> userRole == "Teacher" || userRole == "Admin"
                    "Student" -> true // All authenticated users can access student features
                    else -> false
                }
                
                Log.d(TAG, "verifyUserRole: Access granted: $hasAccess")
                return hasAccess
            } else {
                Log.w(TAG, "verifyUserRole: User document does not exist for UID: ${currentUser.uid}")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying user role: ${e.message}", e)
            false
        }
    }
    
    /**
     * Checks if the current user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Gets the current user's role
     */
    suspend fun getCurrentUserRole(): String? {
        return try {
            val currentUser = auth.currentUser
            Log.d(TAG, "getCurrentUserRole: Getting role for user: ${currentUser?.uid}")
            
            if (currentUser == null) {
                Log.w(TAG, "getCurrentUserRole: No current user found")
                return null
            }
            
            val document = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            Log.d(TAG, "getCurrentUserRole: Document exists: ${document.exists()}")
            
            val role = document.getString("role")
            Log.d(TAG, "getCurrentUserRole: Retrieved role: '$role'")
            
            return role
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user role: ${e.message}", e)
            null
        }
    }
    
    /**
     * Verifies if the current user can access teacher-specific features
     */
    suspend fun canAccessTeacherFeatures(): Boolean {
        Log.d(TAG, "canAccessTeacherFeatures: Checking teacher access")
        val result = verifyUserRole("Teacher")
        Log.d(TAG, "canAccessTeacherFeatures: Result = $result")
        return result
    }
    
    /**
     * Verifies if the current user can access admin features
     */
    suspend fun canAccessAdminFeatures(): Boolean {
        return verifyUserRole("Admin")
    }
    
    /**
     * Logs security events for audit purposes
     */
    fun logSecurityEvent(event: String, userId: String?, details: Map<String, Any> = emptyMap()) {
        try {
            val logData = hashMapOf(
                "event" to event,
                "userId" to (userId ?: "anonymous"),
                "timestamp" to System.currentTimeMillis(),
                "details" to details
            )
            
            firestore.collection("securityLogs")
                .add(logData)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to log security event: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging security event: ${e.message}")
        }
    }
    
    /**
     * Validates course ownership for teachers
     */
    suspend fun validateCourseOwnership(courseId: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val document = firestore.collection("courses")
                .document(courseId)
                .get()
                .await()
            
            if (document.exists()) {
                val teacherId = document.getString("teacherId")
                teacherId == currentUser.uid
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating course ownership: ${e.message}")
            false
        }
    }
    
    /**
     * Rate limiting for sensitive operations
     */
    private val operationTimestamps = mutableMapOf<String, Long>()
    
    fun isOperationAllowed(operation: String, cooldownMs: Long = 5000): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastOperation = operationTimestamps[operation] ?: 0
        
        return if (currentTime - lastOperation >= cooldownMs) {
            operationTimestamps[operation] = currentTime
            true
        } else {
            false
        }
    }
    
    /**
     * Sanitizes user input to prevent injection attacks
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("[<>\"'&]"), "")
            .take(1000) // Limit input length
    }
    
    /**
     * Validates email format
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}