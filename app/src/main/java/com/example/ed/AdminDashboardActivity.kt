package com.example.ed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupUI()
        loadAdminData()
    }
    
    private fun setupUI() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        binding.btnManageUsers.setOnClickListener {
            // TODO: Navigate to user management screen
        }
        
        binding.btnViewReports.setOnClickListener {
            // TODO: Navigate to reports screen
        }
        
        binding.btnSystemSettings.setOnClickListener {
            // TODO: Navigate to system settings screen
        }
    }
    
    private fun loadAdminData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: "Admin"
                        binding.tvWelcome.text = "Welcome, $fullName"
                    }
                }
        }
    }
}