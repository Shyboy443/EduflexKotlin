package com.example.ed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivityThemeSelectionBinding

class ThemeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeSelectionBinding
    private var selectedTheme: String = ThemeManager.THEME_LIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set default selection to light mode
        selectTheme(ThemeManager.THEME_LIGHT)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardLightMode.setOnClickListener {
            selectTheme(ThemeManager.THEME_LIGHT)
            // Apply theme instantly
            ThemeManager.saveTheme(this, ThemeManager.THEME_LIGHT)
            ThemeManager.applyTheme(ThemeManager.THEME_LIGHT)
        }

        binding.cardDarkMode.setOnClickListener {
            selectTheme(ThemeManager.THEME_DARK)
            // Apply theme instantly
            ThemeManager.saveTheme(this, ThemeManager.THEME_DARK)
            ThemeManager.applyTheme(ThemeManager.THEME_DARK)
        }

        binding.btnContinue.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun selectTheme(theme: String) {
        selectedTheme = theme
        
        // Update UI to show selection
        when (theme) {
            ThemeManager.THEME_LIGHT -> {
                binding.ivLightModeSelected.visibility = android.view.View.VISIBLE
                binding.ivDarkModeSelected.visibility = android.view.View.GONE
                // Note: CardView stroke properties are not available in this version
                // Using visibility of check icons instead
            }
            ThemeManager.THEME_DARK -> {
                binding.ivLightModeSelected.visibility = android.view.View.GONE
                binding.ivDarkModeSelected.visibility = android.view.View.VISIBLE
                // Note: CardView stroke properties are not available in this version
                // Using visibility of check icons instead
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}