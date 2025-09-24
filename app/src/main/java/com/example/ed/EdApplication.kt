package com.example.ed

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class EdApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase App Check for reCAPTCHA
        initializeAppCheck()
        
        // Enable Firestore offline persistence
        enableFirestoreOfflinePersistence()
        
        // Apply saved theme on app startup
        ThemeManager.applyCurrentTheme(this)
    }
    
    private fun initializeAppCheck() {
        try {
            // Temporarily disable App Check until Firebase Console is configured
            // TODO: Enable App Check after configuring it in Firebase Console
            Log.d("EdApplication", "App Check temporarily disabled - needs Firebase Console configuration")
            
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
        } catch (e: Exception) {
            Log.e("EdApplication", "Failed to enable Firestore offline persistence", e)
        }
    }
}