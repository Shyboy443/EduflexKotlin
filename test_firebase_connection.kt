package com.example.ed.test

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking

/**
 * Standalone test script to verify Firebase connectivity and authentication
 * This can be run to diagnose Firebase issues without installing the full app
 */
object FirebaseConnectionTest {
    
    private const val TAG = "FirebaseConnectionTest"
    
    fun runTests(context: Context) {
        Log.d(TAG, "Starting Firebase connection tests...")
        
        runBlocking {
            try {
                // Test 1: Firebase App initialization
                testFirebaseAppInitialization()
                
                // Test 2: Firebase Auth
                testFirebaseAuth()
                
                // Test 3: Firestore connectivity
                testFirestoreConnectivity()
                
                // Test 4: Firestore write permissions
                testFirestoreWritePermissions()
                
                Log.d(TAG, "All Firebase tests completed successfully!")
                
            } catch (e: Exception) {
                Log.e(TAG, "Firebase tests failed", e)
            }
        }
    }
    
    private fun testFirebaseAppInitialization() {
        Log.d(TAG, "Test 1: Firebase App Initialization")
        
        val app = FirebaseApp.getInstance()
        Log.d(TAG, "✓ Firebase App Name: ${app.name}")
        Log.d(TAG, "✓ Project ID: ${app.options.projectId}")
        Log.d(TAG, "✓ Application ID: ${app.options.applicationId}")
        Log.d(TAG, "✓ API Key: ${app.options.apiKey?.take(10)}...")
        
        if (app.options.projectId == "eduflex-f62b5") {
            Log.d(TAG, "✓ Correct Firebase project configured")
        } else {
            Log.w(TAG, "⚠ Unexpected project ID: ${app.options.projectId}")
        }
    }
    
    private fun testFirebaseAuth() {
        Log.d(TAG, "Test 2: Firebase Authentication")
        
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            Log.d(TAG, "✓ User is authenticated")
            Log.d(TAG, "✓ User ID: ${currentUser.uid}")
            Log.d(TAG, "✓ User Email: ${currentUser.email}")
            Log.d(TAG, "✓ Email Verified: ${currentUser.isEmailVerified}")
        } else {
            Log.w(TAG, "⚠ No user is currently authenticated")
            Log.w(TAG, "⚠ Data seeding requires authentication")
        }
    }
    
    private suspend fun testFirestoreConnectivity() {
        Log.d(TAG, "Test 3: Firestore Connectivity")
        
        try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "✓ Firestore instance created")
            
            // Test basic connectivity by reading a document
            val testDoc = firestore.collection("connection_test")
                .document("test")
                .get()
                .await()
            
            Log.d(TAG, "✓ Firestore connectivity test successful")
            Log.d(TAG, "✓ Document exists: ${testDoc.exists()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Firestore connectivity test failed", e)
            throw e
        }
    }
    
    private suspend fun testFirestoreWritePermissions() {
        Log.d(TAG, "Test 4: Firestore Write Permissions")
        
        try {
            val firestore = FirebaseFirestore.getInstance()
            val testData = mapOf(
                "test" to true,
                "timestamp" to System.currentTimeMillis(),
                "message" to "Firebase connection test"
            )
            
            // Try to write a test document
            firestore.collection("connection_test")
                .document("write_test")
                .set(testData)
                .await()
            
            Log.d(TAG, "✓ Firestore write test successful")
            
            // Try to read it back
            val readDoc = firestore.collection("connection_test")
                .document("write_test")
                .get()
                .await()
            
            if (readDoc.exists()) {
                Log.d(TAG, "✓ Firestore read-after-write test successful")
                Log.d(TAG, "✓ Test data: ${readDoc.data}")
            } else {
                Log.w(TAG, "⚠ Document was written but could not be read back")
            }
            
            // Clean up test document
            firestore.collection("connection_test")
                .document("write_test")
                .delete()
                .await()
            
            Log.d(TAG, "✓ Test document cleaned up")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Firestore write permissions test failed", e)
            
            // Check specific error types
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    Log.e(TAG, "✗ Permission denied - check Firestore security rules")
                    Log.e(TAG, "✗ Ensure rules allow authenticated users to write")
                }
                e.message?.contains("UNAUTHENTICATED") == true -> {
                    Log.e(TAG, "✗ User not authenticated - sign in required")
                }
                else -> {
                    Log.e(TAG, "✗ Unknown error: ${e.message}")
                }
            }
            
            throw e
        }
    }
    
    /**
     * Quick diagnostic summary
     */
    fun getFirebaseDiagnostic(): String {
        return try {
            val app = FirebaseApp.getInstance()
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            
            buildString {
                appendLine("=== Firebase Diagnostic Summary ===")
                appendLine("Project ID: ${app.options.projectId}")
                appendLine("App ID: ${app.options.applicationId}")
                appendLine("Authentication: ${if (user != null) "✓ Signed in as ${user.email}" else "✗ Not authenticated"}")
                appendLine("User ID: ${user?.uid ?: "N/A"}")
                appendLine("Email Verified: ${user?.isEmailVerified ?: "N/A"}")
                appendLine("=====================================")
            }
        } catch (e: Exception) {
            "Firebase Diagnostic Error: ${e.message}"
        }
    }
}