package com.example.ed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * CourseCreationActivity - Redirects to EnhancedCourseCreationActivity
 * 
 * This activity serves as a redirect to maintain backward compatibility
 * while consolidating course creation functionality into the enhanced version.
 */
class CourseCreationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to Enhanced Course Creation Activity
        val intent = Intent(this, EnhancedCourseCreationActivity::class.java)
        
        // Pass any existing intent extras to maintain functionality
        this.intent.extras?.let { extras ->
            intent.putExtras(extras)
        }
        
        startActivity(intent)
        finish()
    }
}