package com.example.ed

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DebugUserDataActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var debugText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        debugText = TextView(this).apply {
            text = "Loading user data..."
            textSize = 14f
        }
        
        val checkButton = Button(this).apply {
            text = "Check User Data"
            setOnClickListener { checkUserData() }
        }
        
        layout.addView(debugText)
        layout.addView(checkButton)
        setContentView(layout)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Automatically check on start
        checkUserData()
    }
    
    private fun checkUserData() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                val debugInfo = StringBuilder()
                
                debugInfo.append("=== USER DEBUG INFO ===\n\n")
                
                if (currentUser == null) {
                    debugInfo.append("❌ No current user found\n")
                } else {
                    debugInfo.append("✅ Current User Found:\n")
                    debugInfo.append("UID: ${currentUser.uid}\n")
                    debugInfo.append("Email: ${currentUser.email}\n")
                    debugInfo.append("Display Name: ${currentUser.displayName}\n")
                    debugInfo.append("Provider: ${currentUser.providerId}\n\n")
                    
                    // Check Firebase document
                    debugInfo.append("=== FIREBASE DOCUMENT ===\n")
                    val document = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                    
                    if (document.exists()) {
                        debugInfo.append("✅ Document exists\n")
                        debugInfo.append("Document ID: ${document.id}\n")
                        debugInfo.append("All data: ${document.data}\n\n")
                        
                        // Check specific fields
                        debugInfo.append("=== FIELD VALUES ===\n")
                        debugInfo.append("role: '${document.getString("role")}'\n")
                        debugInfo.append("fullName: '${document.getString("fullName")}'\n")
                        debugInfo.append("name: '${document.getString("name")}'\n")
                        debugInfo.append("displayName: '${document.getString("displayName")}'\n")
                        debugInfo.append("email: '${document.getString("email")}'\n")
                        debugInfo.append("provider: '${document.getString("provider")}'\n")
                        debugInfo.append("createdAt: ${document.getLong("createdAt")}\n")
                    } else {
                        debugInfo.append("❌ Document does NOT exist\n")
                        debugInfo.append("This is the problem! User document is missing.\n")
                    }
                }
                
                Log.d("DebugUserData", debugInfo.toString())
                debugText.text = debugInfo.toString()
                
            } catch (e: Exception) {
                val errorInfo = "❌ Error: ${e.message}\n${e.stackTrace.joinToString("\n")}"
                Log.e("DebugUserData", errorInfo, e)
                debugText.text = errorInfo
            }
        }
    }
}