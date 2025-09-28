package com.example.ed.services

import android.util.Log
import kotlinx.coroutines.runBlocking

class AIContentService {
    
    companion object {
        private const val TAG = "AIContentService"
    }
    
    private val geminiService = GeminiAIService()
    
    // Simple callback-based method for compatibility with existing code
    fun generateContent(prompt: String, callback: (String?) -> Unit) {
        runBlocking {
            val content = geminiService.generateContent(prompt)
            callback(content)
        }
    }
}
