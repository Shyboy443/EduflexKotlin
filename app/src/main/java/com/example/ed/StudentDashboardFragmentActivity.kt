package com.example.ed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ed.databinding.ActivityStudentDashboardFragmentBinding
import com.example.ed.ui.student.StudentCoursesFragment
import com.example.ed.ui.student.StudentDashboardFragment
import com.example.ed.ui.student.StudentGamificationFragment
import com.example.ed.ui.student.StudentSettingsFragment
import com.example.ed.ui.student.StudentWeeklyContentFragment
import com.google.firebase.auth.FirebaseAuth

class StudentDashboardFragmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDashboardFragmentBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme
        ThemeManager.applyCurrentTheme(this)
        
        binding = ActivityStudentDashboardFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()
        
        // Check if we need to open weekly content directly
        if (intent.getBooleanExtra("OPEN_WEEKLY_CONTENT", false)) {
            val courseId = intent.getStringExtra("COURSE_ID")
            if (!courseId.isNullOrEmpty()) {
                val fragment = StudentWeeklyContentFragment.newInstance(courseId)
                switchToFragment(fragment)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchToFragment(StudentDashboardFragment())
                    true
                }
                R.id.nav_courses -> {
                    switchToFragment(StudentCoursesFragment())
                    true
                }
                R.id.nav_games -> {
                    switchToFragment(StudentGamificationFragment())
                    true
                }
                R.id.nav_settings -> {
                    switchToFragment(StudentSettingsFragment())
                    true
                }
                else -> false
            }
        }
        
        // Set initial fragment
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            switchToFragment(StudentDashboardFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun switchToFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun switchToCoursesFragment() {
        switchToFragment(StudentCoursesFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_courses
    }

    override fun onBackPressed() {
        // Handle back press - maybe show confirmation dialog
        super.onBackPressed()
        finishAffinity() // Close the app completely
    }
}