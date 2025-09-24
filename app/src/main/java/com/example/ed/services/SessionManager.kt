package com.example.ed.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * SessionManager handles user session management and role-based authentication
 * Provides efficient session management and secure role verification
 */
class SessionManager private constructor(private val context: Context) {
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val PREFS_NAME = "user_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SESSION_TIMESTAMP = "session_timestamp"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val SESSION_TIMEOUT = 24 * 60 * 60 * 1000L // 24 hours
        
        @Volatile
        private var INSTANCE: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Create a new user session after successful authentication
     */
    fun createSession(userId: String, userRole: String, userEmail: String, userName: String) {
        val currentTime = System.currentTimeMillis()
        
        sharedPrefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_ROLE, userRole)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_NAME, userName)
            putLong(KEY_SESSION_TIMESTAMP, currentTime)
            putLong(KEY_LAST_ACTIVITY, currentTime)
            apply()
        }
        
        Log.d("SessionManager", "Session created for user: $userId with role: $userRole")
    }
    
    /**
     * Update last activity timestamp
     */
    fun updateLastActivity() {
        sharedPrefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }
    
    /**
     * Check if user session is valid
     */
    fun isSessionValid(): Boolean {
        val sessionTimestamp = sharedPrefs.getLong(KEY_SESSION_TIMESTAMP, 0)
        val lastActivity = sharedPrefs.getLong(KEY_LAST_ACTIVITY, 0)
        val currentTime = System.currentTimeMillis()
        
        // Check if session exists and is not expired
        if (sessionTimestamp == 0L || (currentTime - lastActivity) > SESSION_TIMEOUT) {
            Log.d("SessionManager", "Session expired or invalid")
            return false
        }
        
        // Check if Firebase user is still authenticated
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.d("SessionManager", "Firebase user not authenticated")
            clearSession()
            return false
        }
        
        return true
    }
    
    /**
     * Get current user role
     */
    fun getUserRole(): String? {
        return if (isSessionValid()) {
            sharedPrefs.getString(KEY_USER_ROLE, null)
        } else {
            null
        }
    }
    
    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return if (isSessionValid()) {
            sharedPrefs.getString(KEY_USER_ID, null)
        } else {
            null
        }
    }
    
    /**
     * Get current user email
     */
    fun getUserEmail(): String? {
        return if (isSessionValid()) {
            sharedPrefs.getString(KEY_USER_EMAIL, null)
        } else {
            null
        }
    }
    
    /**
     * Get current user name
     */
    fun getUserName(): String? {
        return if (isSessionValid()) {
            sharedPrefs.getString(KEY_USER_NAME, null)
        } else {
            null
        }
    }
    
    /**
     * Verify user role with database (for security-critical operations)
     */
    fun verifyUserRole(callback: (String?) -> Unit) {
        val userId = getUserId()
        if (userId == null) {
            callback(null)
            return
        }
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val dbRole = document.getString("role")
                    val sessionRole = getUserRole()
                    
                    // Update session if role has changed in database
                    if (dbRole != sessionRole && dbRole != null) {
                        sharedPrefs.edit().putString(KEY_USER_ROLE, dbRole).apply()
                        Log.d("SessionManager", "Role updated from $sessionRole to $dbRole")
                    }
                    
                    callback(dbRole)
                } else {
                    Log.w("SessionManager", "User document not found")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SessionManager", "Error verifying user role", e)
                callback(getUserRole()) // Fallback to cached role
            }
    }
    
    /**
     * Check if user has specific role
     */
    fun hasRole(requiredRole: String): Boolean {
        val userRole = getUserRole()
        return userRole == requiredRole || userRole == "Admin" // Admin has access to all roles
    }
    
    /**
     * Check if user can access teacher features
     */
    fun canAccessTeacherFeatures(): Boolean {
        return hasRole("Teacher") || hasRole("Admin")
    }
    
    /**
     * Check if user can access student features
     */
    fun canAccessStudentFeatures(): Boolean {
        return hasRole("Student") || hasRole("Admin")
    }
    
    /**
     * Check if user can access admin features
     */
    fun canAccessAdminFeatures(): Boolean {
        return hasRole("Admin")
    }
    
    /**
     * Clear user session
     */
    fun clearSession() {
        sharedPrefs.edit().clear().apply()
        Log.d("SessionManager", "Session cleared")
    }
    
    /**
     * Logout user completely
     */
    fun logout() {
        auth.signOut()
        clearSession()
        Log.d("SessionManager", "User logged out")
    }
    
    /**
     * Get session info for debugging
     */
    fun getSessionInfo(): Map<String, Any?> {
        return mapOf(
            "userId" to getUserId(),
            "userRole" to getUserRole(),
            "userEmail" to getUserEmail(),
            "userName" to getUserName(),
            "sessionValid" to isSessionValid(),
            "sessionTimestamp" to sharedPrefs.getLong(KEY_SESSION_TIMESTAMP, 0),
            "lastActivity" to sharedPrefs.getLong(KEY_LAST_ACTIVITY, 0)
        )
    }
}