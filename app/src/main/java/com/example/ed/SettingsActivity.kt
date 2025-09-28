package com.example.ed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme before setting content view
        ThemeManager.applyCurrentTheme(this)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Set user profile information
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            binding.tvUserName.text = user.displayName ?: "Amal Perera"
            binding.tvUserEmail.text = user.email ?: "amal.p@example.com"
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
            // TODO: Navigate to profile edit screen
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
        }
        
        // Language section click
        binding.layoutLanguage.setOnClickListener {
            // TODO: Navigate to language selection
        }
        
        // Data & Privacy section click
        binding.layoutDataPrivacy.setOnClickListener {
            // TODO: Navigate to data & privacy settings
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
        }
        
        // Language support section click
        binding.layoutLanguageSupport.setOnClickListener {
            // TODO: Navigate to language support
        }
        
        // Help & Support section click
        binding.layoutHelpSupport.setOnClickListener {
            // TODO: Navigate to help & support
        }
        
        // About EduFlex section click
        binding.layoutAbout.setOnClickListener {
            // TODO: Navigate to about screen
        }
        
        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, StudentDashboardFragmentActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_courses -> {
                    // Navigate to course details for now (later can be a courses list)
                    val intent = Intent(this, CourseDetailsActivity::class.java)
                    intent.putExtra("COURSE_ID", "course1") // Sample course ID
                    startActivity(intent)
                    true
                }
                R.id.nav_live -> {
                    // TODO: Navigate to live sessions
                    true
                }
                R.id.nav_profile -> {
                    // TODO: Navigate to profile
                    true
                }
                R.id.nav_settings -> {
                    // Already in settings
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
    }
}