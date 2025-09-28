package com.example.ed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.activities.AddContentItemActivity as NewAddContentItemActivity

class AddContentItemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirect to the newer, fully featured activity implementation
        val intent = Intent(this, NewAddContentItemActivity::class.java)
        // Pass through any extras if present (e.g., edit mode data)
        intent.putExtras(getIntent()?.extras ?: Bundle())
        startActivity(intent)
        finish()
    }
}