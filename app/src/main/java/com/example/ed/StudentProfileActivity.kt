package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentEmail: TextView
    
    private lateinit var tvEnrolledCoursesCount: TextView
    private lateinit var tvCompletedCoursesCount: TextView
    
    // Action Buttons
    private lateinit var btnEditProfile: Button
    private lateinit var btnEnrolledCourses: Button
    private lateinit var btnCompletedCourses: Button
    private lateinit var btnSettings: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupToolbar()
        setupClickListeners()
        loadProfileData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        ivProfilePicture = findViewById(R.id.iv_profile_picture)
        tvStudentName = findViewById(R.id.tv_user_name)
        tvStudentEmail = findViewById(R.id.tv_user_email)
        
        tvEnrolledCoursesCount = findViewById(R.id.tv_enrolled_courses_count)
        tvCompletedCoursesCount = findViewById(R.id.tv_completed_courses_count)
        
        // Action buttons
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnEnrolledCourses = findViewById(R.id.btn_enrolled_courses)
        btnCompletedCourses = findViewById(R.id.btn_completed_courses)
        btnSettings = findViewById(R.id.btn_settings)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        // Action buttons
        btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }

        btnEnrolledCourses.setOnClickListener {
            val intent = Intent(this, StudentCoursesActivity::class.java)
            startActivity(intent)
        }

        btnCompletedCourses.setOnClickListener {
            val intent = Intent(this, StudentCoursesActivity::class.java)
            intent.putExtra("SHOW_COMPLETED", true)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfileData() {
        val currentUser = auth.currentUser ?: return

        // Load basic user info
        tvStudentName.text = currentUser.displayName ?: "Student"
        tvStudentEmail.text = currentUser.email ?: ""

        // Load profile picture
        val photoUrl = currentUser.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(ivProfilePicture)
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_person)
        }

        // Load student analytics
        loadStudentStats()
    }

    private fun loadStudentStats() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("studentAnalytics")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val enrolledCourses = (document.getLong("enrolledCourses") ?: 0).toInt()
                    val completedCourses = (document.getLong("completedCourses") ?: 0).toInt()

                    tvEnrolledCoursesCount.text = enrolledCourses.toString()
                    tvCompletedCoursesCount.text = completedCourses.toString()
                } else {
                    // Load default values
                    tvEnrolledCoursesCount.text = "3"
                    tvCompletedCoursesCount.text = "1"
                }
            }
            .addOnFailureListener {
                // Load default values on error
                tvEnrolledCoursesCount.text = "3"
                tvCompletedCoursesCount.text = "1"
            }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        
        // Clear any cached data
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        
        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}