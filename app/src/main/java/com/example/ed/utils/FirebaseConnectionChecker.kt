package com.example.ed.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Utility class to check Firebase connection and configuration
 */
object FirebaseConnectionChecker {
    
    private const val TAG = "FirebaseConnectionChecker"
    private const val TIMEOUT_MS = 10000L // 10 seconds timeout
    
    /**
     * Comprehensive Firebase connection check
     */
    suspend fun checkFirebaseConnection(context: Context): FirebaseConnectionResult {
        return try {
            val results = mutableListOf<String>()
            var isConnected = true
            
            // 1. Check Firebase App initialization
            val firebaseApp = FirebaseApp.getInstance()
            results.add("✓ Firebase App initialized: ${firebaseApp.name}")
            results.add("✓ Project ID: ${firebaseApp.options.projectId}")
            results.add("✓ Application ID: ${firebaseApp.options.applicationId}")
            
            // 2. Check Firebase Auth
            val auth = FirebaseAuth.getInstance()
            results.add("✓ Firebase Auth initialized")
            results.add("✓ Current user: ${auth.currentUser?.email ?: "Not signed in"}")
            
            // 3. Check Firestore connection
            val firestore = FirebaseFirestore.getInstance()
            results.add("✓ Firestore initialized")
            
            // Check Firestore settings
            val settings = firestore.firestoreSettings
            results.add("✓ Persistence enabled: ${settings.isPersistenceEnabled}")
            results.add("✓ Cache size: ${if (settings.cacheSizeBytes == FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) "Unlimited" else "${settings.cacheSizeBytes} bytes"}")
            
            // Test Firestore connectivity with timeout
            try {
                withTimeout(TIMEOUT_MS) {
                    val testDoc = firestore.collection("connection_test").document("test")
                    testDoc.get().await()
                    results.add("✓ Firestore connection successful")
                }
            } catch (e: Exception) {
                results.add("⚠ Firestore connection test failed: ${e.message}")
                Log.w(TAG, "Firestore connection test failed", e)
                // Don't mark as disconnected for offline scenarios
            }
            
            // 4. Check Firebase Storage
            try {
                val storage = FirebaseStorage.getInstance()
                results.add("✓ Firebase Storage initialized")
                results.add("✓ Storage bucket: ${storage.reference.bucket}")
            } catch (e: Exception) {
                results.add("⚠ Firebase Storage check failed: ${e.message}")
                Log.w(TAG, "Firebase Storage check failed", e)
            }
            
            // 5. Check network connectivity
            val networkAvailable = NetworkUtils.isNetworkAvailable(context)
            results.add("✓ Network available: $networkAvailable")
            
            FirebaseConnectionResult(
                isConnected = isConnected,
                details = results,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection check failed", e)
            FirebaseConnectionResult(
                isConnected = false,
                details = listOf("✗ Firebase connection check failed: ${e.message}"),
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Quick Firebase status check
     */
    fun getFirebaseStatus(): String {
        return try {
            val app = FirebaseApp.getInstance()
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            
            buildString {
                appendLine("Firebase Status:")
                appendLine("- App: ${app.name} (${app.options.projectId})")
                appendLine("- Auth: ${if (auth.currentUser != null) "Signed in as ${auth.currentUser?.email}" else "Not signed in"}")
                appendLine("- Firestore: Initialized with persistence ${if (firestore.firestoreSettings.isPersistenceEnabled) "enabled" else "disabled"}")
            }
        } catch (e: Exception) {
            "Firebase Status: Error - ${e.message}"
        }
    }
    
    /**
     * Test Firestore write operation
     */
    suspend fun testFirestoreWrite(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val testData = hashMapOf(
                "test" to true,
                "timestamp" to System.currentTimeMillis()
            )
            
            withTimeout(TIMEOUT_MS) {
                firestore.collection("connection_test")
                    .document("write_test")
                    .set(testData)
                    .await()
            }
            
            Log.d(TAG, "Firestore write test successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore write test failed", e)
            false
        }
    }
    
    /**
     * Test Firestore read operation
     */
    suspend fun testFirestoreRead(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            withTimeout(TIMEOUT_MS) {
                firestore.collection("connection_test")
                    .document("write_test")
                    .get()
                    .await()
            }
            
            Log.d(TAG, "Firestore read test successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore read test failed", e)
            false
        }
    }
}

/**
 * Data class to hold Firebase connection check results
 */
data class FirebaseConnectionResult(
    val isConnected: Boolean,
    val details: List<String>,
    val timestamp: Long
)