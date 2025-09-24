package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Check authentication state
        checkAuthenticationState()
    }
    
    private fun checkAuthenticationState() {
        val currentUser = auth.currentUser
        Log.d("HomeActivity", "Checking auth state for user: ${currentUser?.uid}")
        
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Log.d("HomeActivity", "No current user, redirecting to login")
            redirectToLogin()
            return
        }
        
        // User is authenticated, get their role and redirect to appropriate dashboard
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                Log.d("HomeActivity", "User document exists: ${document.exists()}")
                if (document.exists()) {
                    val userRole = document.getString("role") ?: "Student"
                    Log.d("HomeActivity", "User role: $userRole")
                    Log.d("HomeActivity", "Document data: ${document.data}")
                    redirectToDashboard(userRole)
                } else {
                    // User document doesn't exist, redirect to login
                    Log.w("HomeActivity", "User document doesn't exist, redirecting to login")
                    redirectToLogin()
                }
            }
            .addOnFailureListener { e ->
                // Error getting user data, redirect to login
                Log.e("HomeActivity", "Error getting user data: ${e.message}", e)
                redirectToLogin()
            }
    }
    
    private fun redirectToDashboard(role: String) {
        val intent = when (role) {
            "Admin" -> Intent(this, AdminDashboardActivity::class.java)
            "Teacher" -> Intent(this, TeacherDashboardActivity::class.java)
            "Student" -> Intent(this, StudentDashboardActivity::class.java)
            else -> Intent(this, StudentDashboardActivity::class.java) // Default to student
        }
        startActivity(intent)
        finish()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
