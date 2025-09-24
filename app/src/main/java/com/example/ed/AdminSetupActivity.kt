package com.example.ed

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminSetupActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        createAdminAccount()
    }
    
    private fun createAdminAccount() {
        val adminEmail = "admin@gmail.com"
        val adminPassword = "admin123"
        
        // Create admin account with Firebase Auth
        auth.createUserWithEmailAndPassword(adminEmail, adminPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Create admin profile in Firestore
                        val adminProfile = hashMapOf(
                            "fullName" to "System Administrator",
                            "email" to adminEmail,
                            "role" to "Admin",
                            "createdAt" to System.currentTimeMillis(),
                            "isSystemAdmin" to true
                        )
                        
                        firestore.collection("users").document(it.uid)
                            .set(adminProfile)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Admin account created successfully!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to create admin profile: ${e.message}", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    }
                } else {
                    // Check if account already exists
                    if (task.exception?.message?.contains("already in use") == true) {
                        Toast.makeText(this, "Admin account already exists!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to create admin account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
            }
    }
}