package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivityTeacherSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class TeacherSettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTeacherSettingsBinding
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme before setting content view
        ThemeManager.applyCurrentTheme(this)
        
        binding = ActivityTeacherSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Set user profile information
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            binding.tvUserName.text = user.displayName ?: "Teacher Name"
            binding.tvUserEmail.text = user.email ?: "teacher@example.com"
            binding.tvUserRole.text = "Teacher"
        }
        
        // Set current theme switch state
        binding.switchDarkMode.isChecked = ThemeManager.isDarkMode(this)
        
        // Set notifications switch (default enabled)
        binding.switchNotifications.isChecked = true
        
        // Set text size value
        binding.tvTextSizeValue.text = "3"
        
        // Set high contrast switch (default disabled)
        binding.switchHighContrast.isChecked = false
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Profile section click
        binding.layoutProfile.setOnClickListener {
            // TODO: Navigate to teacher profile edit screen
            Toast.makeText(this, "Profile editing coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val newTheme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(this, newTheme)
            ThemeManager.applyTheme(newTheme)
            
            // Recreate activity to apply theme changes
            recreate()
        }
        
        // Notifications toggle
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Handle notification preferences
            Toast.makeText(this, "Notification preferences updated", Toast.LENGTH_SHORT).show()
        }
        
        // Language section click
        binding.layoutLanguage.setOnClickListener {
            // TODO: Navigate to language selection
            Toast.makeText(this, "Language selection coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Course management section click
        binding.layoutCourseManagement.setOnClickListener {
            // Navigate to course management
            startActivity(Intent(this, TeacherDashboardActivity::class.java))
        }
        
        // Analytics section click
        binding.layoutAnalytics.setOnClickListener {
            // TODO: Navigate to analytics dashboard
            Toast.makeText(this, "Analytics dashboard coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Text size adjustment
        binding.btnDecreaseTextSize.setOnClickListener {
            adjustTextSize(-1)
        }
        
        binding.btnIncreaseTextSize.setOnClickListener {
            adjustTextSize(1)
        }
        
        // High contrast toggle
        binding.switchHighContrast.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Handle high contrast mode
            Toast.makeText(this, "High contrast mode ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
        
        // Help & Support section click
        binding.layoutHelpSupport.setOnClickListener {
            // TODO: Navigate to help & support
            Toast.makeText(this, "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // About EduFlex section click
        binding.layoutAbout.setOnClickListener {
            // TODO: Navigate to about screen
            Toast.makeText(this, "About EduFlex coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }
        
        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, TeacherDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_courses -> {
                    // Navigate to course management
                    startActivity(Intent(this, TeacherDashboardActivity::class.java))
                    true
                }
                R.id.nav_weekly_content -> {
                    // TODO: Navigate to weekly content
                    Toast.makeText(this, "Weekly content coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_students -> {
                    // TODO: Navigate to students
                    Toast.makeText(this, "Students coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    // Already in settings/profile
                    true
                }
                else -> false
            }
        }
        
        // Set settings as selected in bottom navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
    }
    
    private fun adjustTextSize(delta: Int) {
        val currentSize = binding.tvTextSizeValue.text.toString().toIntOrNull() ?: 3
        val newSize = (currentSize + delta).coerceIn(1, 5)
        binding.tvTextSizeValue.text = newSize.toString()
        
        // TODO: Apply text size changes to app
        Toast.makeText(this, "Text size: $newSize", Toast.LENGTH_SHORT).show()
    }
    
    private fun logout() {
        // Show confirmation dialog or directly logout
        auth.signOut()
        
        // Clear any cached data if needed
        // SessionManager.getInstance(this).clearSession()
        
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}