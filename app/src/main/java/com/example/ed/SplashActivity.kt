package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    private val splashTimeOut: Long = 3000 // 3 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Initialize views
        val logoImageView = findViewById<ImageView>(R.id.ivLogo)
        val appNameTextView = findViewById<TextView>(R.id.tvAppName)
        val taglineTextView = findViewById<TextView>(R.id.tvTagline)
        
        // Load animations
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        
        // Apply animations
        logoImageView.startAnimation(fadeInAnimation)
        appNameTextView.startAnimation(slideUpAnimation)
        taglineTextView.startAnimation(slideUpAnimation)
        
        // Navigate to main activity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMainActivity()
        }, splashTimeOut)
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        
        // Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    override fun onBackPressed() {
        // Disable back button on splash screen
        // Do nothing
    }
}