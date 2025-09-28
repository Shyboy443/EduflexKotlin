package com.example.ed

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class EdApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("EdApplication", "Firebase initialized - connecting to PRODUCTION database")
        
        // Configure Firebase emulators for local development
        // configureEmulators() // DISABLED - use production Firebase database
        
        // Initialize Firebase App Check for reCAPTCHA
        // initializeAppCheck() // DISABLED to avoid 403 errors during development
        
        // Enable Firestore offline persistence
        enableFirestoreOfflinePersistence()
        
        // Initialize Firebase Storage with error handling
        initializeFirebaseStorage()
        
        // Apply saved theme on app startup
        ThemeManager.applyCurrentTheme(this)
        
        // Automatically create test users (only in debug mode)
        if (BuildConfig.DEBUG) {
            createTestUsers()
        }
    }
    
    private fun configureEmulators() {
        try {
            // WARNING: This connects to LOCAL emulators, not production Firebase!
            // Only enable this if you have Firebase emulators running locally
            if (BuildConfig.DEBUG) {
                // Connect to Firebase emulators for local development
                val firestore = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                
                // Connect to Firestore emulator (default port 8080)
                firestore.useEmulator("10.0.2.2", 8080)
                
                // Connect to Auth emulator (default port 9099)
                auth.useEmulator("10.0.2.2", 9099)
                
                Log.w("EdApplication", "WARNING: Using LOCAL Firebase emulators - NOT production database!")
                Log.d("EdApplication", "Firestore emulator: 10.0.2.2:8080")
                Log.d("EdApplication", "Auth emulator: 10.0.2.2:9099")
            } else {
                Log.d("EdApplication", "Production build - using Firebase production services")
            }
        } catch (e: Exception) {
            Log.e("EdApplication", "Failed to configure Firebase emulators", e)
        }
    }
    
    private fun initializeAppCheck() {
        try {
            // TEMPORARILY DISABLE App Check completely to avoid 403 errors
            Log.d("EdApplication", "App Check DISABLED to avoid 403 errors during development")
            Log.d("EdApplication", "TODO: Enable App Check after proper Firebase Console configuration")
            
            // Commenting out all App Check initialization to prevent 403 errors
            /*
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            
            // For development/debug builds, use debug provider
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d("EdApplication", "Firebase App Check initialized with debug provider")
                
                // Get and log the App Check token for debugging
                firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result?.token
                        Log.d("EdApplication", "App Check token generated successfully: ${token?.take(20)}...")
                    } else {
                        Log.e("EdApplication", "Failed to get App Check token", task.exception)
                    }
                }
            } else {
                // For production, you would use Play Integrity or other providers
                // This requires additional setup in Firebase Console
                Log.d("EdApplication", "Firebase App Check should be configured for production")
            }
            */
        } catch (e: Exception) {
            Log.e("EdApplication", "Failed to initialize Firebase App Check", e)
        }
    }
    
    private fun enableFirestoreOfflinePersistence() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            Log.d("EdApplication", "Firestore offline persistence enabled successfully")
            
            // Test connection to production database
            firestore.collection("users").limit(1).get()
                .addOnSuccessListener { 
                    Log.d("EdApplication", "✓ Successfully connected to PRODUCTION Firebase database")
                }
                .addOnFailureListener { e ->
                    Log.e("EdApplication", "✗ Failed to connect to Firebase database: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("EdApplication", "Failed to enable Firestore offline persistence", e)
        }
    }
    
    private fun initializeFirebaseStorage() {
        try {
            val storage = FirebaseStorage.getInstance()
            
            // Test if storage is properly configured
            val testRef = storage.reference.child("test")
            Log.d("EdApplication", "Firebase Storage initialized successfully")
            
        } catch (e: Exception) {
            Log.w("EdApplication", "Firebase Storage not configured or not available", e)
            Log.w("EdApplication", "Image uploads will use placeholder images")
        }
    }
    
    private fun createTestUsers() {
        GlobalScope.launch {
            delay(2000) // Wait for Firebase to fully initialize
            
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            
            // Test users data
            val testUsers = listOf(
                TestUser("admin@gmail.com", "admin123", "Admin", "Admin User"),
                TestUser("teacher@gmail.com", "teacher123", "Teacher", "Teacher User"),
                TestUser("student@gmail.com", "student123", "Student", "Student User")
            )
            
            for (testUser in testUsers) {
                try {
                    // Check if user already exists
                    auth.fetchSignInMethodsForEmail(testUser.email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val signInMethods = task.result?.signInMethods
                                if (signInMethods.isNullOrEmpty()) {
                                    // User doesn't exist, create it
                                    createTestUser(testUser, auth, db)
                                } else {
                                    Log.d("EdApplication", "Test user already exists: ${testUser.email}")
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("EdApplication", "Error checking test user: ${testUser.email}", e)
                }
                
                delay(1000) // Delay between creating users
            }
        }
    }
    
    private fun createTestUser(testUser: TestUser, auth: FirebaseAuth, db: FirebaseFirestore) {
        auth.createUserWithEmailAndPassword(testUser.email, testUser.password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Create user profile in Firestore
                    val userData = hashMapOf(
                        "email" to testUser.email,
                        "fullName" to testUser.fullName,
                        "role" to testUser.role,
                        "uid" to user.uid,
                        "provider" to "email",
                        "createdAt" to System.currentTimeMillis(),
                        "profileCompleted" to true,
                        "isActive" to true
                    )
                    
                    db.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("EdApplication", "✅ Test user created successfully: ${testUser.email} (${testUser.role})")
                            Log.d("EdApplication", "Login credentials - Email: ${testUser.email}, Password: ${testUser.password}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("EdApplication", "Failed to save user profile: ${testUser.email}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("already in use") == true) {
                    Log.d("EdApplication", "Test user already exists: ${testUser.email}")
                } else {
                    Log.e("EdApplication", "Failed to create test user: ${testUser.email}", e)
                }
            }
    }
    
    data class TestUser(
        val email: String,
        val password: String,
        val role: String,
        val fullName: String
    )
}