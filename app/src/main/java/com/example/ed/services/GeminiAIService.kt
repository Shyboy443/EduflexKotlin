package com.example.ed.services

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAIService {
    companion object {
        private const val API_KEY = "AIzaSyBUhGE-Br5u7DBKZOyvs6ubxE3WtrpTgnU"
        // List of models to try in order (from newest to most stable)
        private val MODEL_PRIORITY = listOf(
            "gemini-2.0-flash-exp",      // Latest experimental model
            "gemini-1.5-flash-latest",   // Latest stable 1.5 flash
            "gemini-1.5-flash",          // Standard 1.5 flash
            "gemini-1.5-pro-latest",     // Latest 1.5 pro
            "gemini-1.5-pro",            // Standard 1.5 pro
            "gemini-pro"                 // Fallback to basic pro
        )
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Generate educational content based on a prompt
     */
    suspend fun generateContent(prompt: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fullPrompt = """
                    You are an educational content creator. Generate high-quality educational content based on the following request:
                    
                    $prompt
                    
                    Make the content:
                    - Clear and well-structured
                    - Educational and informative
                    - Engaging for students
                    - Include examples where appropriate
                    - Use proper formatting with sections and bullet points
                """.trimIndent()
                val json = buildPromptBody(fullPrompt)
                postGenerate(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Generate quiz questions with answers
     */
    suspend fun generateQuizQuestions(
        topic: String,
        numberOfQuestions: Int,
        questionType: String,
        difficulty: String,
        courseContext: String? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val contextInfo = if (!courseContext.isNullOrEmpty()) {
                    "Course Context: $courseContext"
                } else {
                    ""
                }
                
                val prompt = """
                    Generate $numberOfQuestions quiz questions about: $topic
                    $contextInfo
                    
                    Requirements:
                    - Question Type: $questionType
                    - Difficulty Level: $difficulty
                    - Include correct answers for each question
                    - Include explanations for each answer
                    
                    Return the response in the following JSON format:
                    {
                        "questions": [
                            {
                                "question": "Question text here",
                                "type": "MULTIPLE_CHOICE/TRUE_FALSE/SHORT_ANSWER",
                                "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                                "correctAnswer": "Correct answer here",
                                "explanation": "Explanation of why this is correct",
                                "points": 1,
                                "difficulty": "$difficulty"
                            }
                        ]
                    }
                    
                    For TRUE_FALSE questions, options should be ["True", "False"]
                    For SHORT_ANSWER questions, options should be empty array []
                    For MULTIPLE_CHOICE, provide 4 options with one correct answer
                    
                    Make sure the questions are:
                    - Relevant to the topic
                    - Clear and unambiguous
                    - Appropriate for the difficulty level
                    - Educational and testing understanding, not just memorization
                """.trimIndent()
                val json = buildPromptBody(prompt)
                postGenerate(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Enhance existing content with AI
     */
    suspend fun enhanceContent(existingContent: String, enhancementType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = when (enhancementType) {
                    "summary" -> """
                        Provide a concise summary of the following content:
                        
                        $existingContent
                        
                        The summary should:
                        - Capture the main points
                        - Be clear and concise
                        - Maintain the educational value
                    """.trimIndent()
                    
                    "expand" -> """
                        Expand and enhance the following content:
                        
                        $existingContent
                        
                        Add:
                        - More detailed explanations
                        - Additional examples
                        - Related concepts
                        - Practice exercises if applicable
                    """.trimIndent()
                    
                    "simplify" -> """
                        Simplify the following content for easier understanding:
                        
                        $existingContent
                        
                        Make it:
                        - More accessible for beginners
                        - Use simpler language
                        - Break down complex concepts
                        - Add analogies where helpful
                    """.trimIndent()
                    
                    "key_points" -> """
                        Extract the key learning points from the following content:
                        
                        $existingContent
                        
                        Format as:
                        - Bullet points
                        - Most important concepts first
                        - Clear and memorable statements
                    """.trimIndent()
                    
                    else -> """
                        Improve the following educational content:
                        
                        $existingContent
                        
                        Make it more engaging and educational.
                    """.trimIndent()
                }
                val json = buildPromptBody(prompt)
                postGenerate(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Generate content based on subject-specific templates
     */
    suspend fun generateTemplateContent(
        subject: String,
        topic: String,
        contentType: String,
        level: String
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Create educational content for:
                    Subject: $subject
                    Topic: $topic
                    Content Type: $contentType
                    Level: $level
                    
                    Structure the content appropriately for the subject:
                    ${getSubjectSpecificGuidelines(subject)}
                    
                    Make sure the content is:
                    - Appropriate for the specified level
                    - Well-organized with clear sections
                    - Includes relevant examples
                    - Engaging and educational
                """.trimIndent()
                val json = buildPromptBody(prompt)
                postGenerate(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Generate assignment or exercise content
     */
    suspend fun generateAssignment(
        topic: String,
        assignmentType: String,
        difficulty: String,
        numberOfProblems: Int
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Create an assignment on: $topic
                    
                    Assignment Type: $assignmentType
                    Difficulty: $difficulty
                    Number of Problems/Questions: $numberOfProblems
                    
                    Include:
                    - Clear instructions
                    - Well-structured problems
                    - Point values for each problem
                    - Expected time to complete
                    - Learning objectives being assessed
                    
                    For each problem, also provide:
                    - Solution or answer key (marked clearly as instructor-only)
                    - Grading rubric or criteria
                """.trimIndent()
                val json = buildPromptBody(prompt)
                postGenerate(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Generate feedback for student submissions
     */
    suspend fun generateFeedback(
        studentAnswer: String,
        correctAnswer: String,
        question: String
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Provide constructive feedback for a student's answer:
                    
                    Question: $question
                    Student's Answer: $studentAnswer
                    Correct Answer: $correctAnswer
                    
                    Generate feedback that:
                    - Acknowledges what the student got right
                    - Explains any mistakes clearly
                    - Provides guidance for improvement
                    - Is encouraging and constructive
                    - Suggests resources or concepts to review if needed
                """.trimIndent()
                val json = buildPromptBody(prompt)
                postGenerate(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    // Build request body for Gemini generateContent
    private fun buildPromptBody(prompt: String): String {
        val generationConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("topK", 40)
            put("topP", 0.95)
            put("maxOutputTokens", 8192)
        }
        val contents = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                }
            )
        }
        return JSONObject().apply {
            put("contents", contents)
            put("generationConfig", generationConfig)
        }.toString()
    }

    // Execute POST to Gemini and extract text from the first candidate
    private fun postGenerate(jsonBody: String): String? {
        // Try each model in priority order until one succeeds
        for ((index, modelName) in MODEL_PRIORITY.withIndex()) {
            Log.d("GeminiAIService", "Trying model ${index + 1}/${MODEL_PRIORITY.size}: $modelName")
            
            val result = tryGenerateWithModel(jsonBody, modelName)
            if (result != null) {
                Log.i("GeminiAIService", "Successfully generated content with model: $modelName")
                return result
            }
            
            // If this wasn't the last model, log that we're trying the next one
            if (index < MODEL_PRIORITY.size - 1) {
                Log.w("GeminiAIService", "Model $modelName failed, trying next model...")
            }
        }
        
        Log.e("GeminiAIService", "All models failed to generate content")
        return null
    }
    
    private fun tryGenerateWithModel(jsonBody: String, modelName: String): String? {
        // Retry up to 3 times for 503 errors (model overloaded)
        repeat(3) { attempt ->
            try {
                val url = "$BASE_URL$modelName:generateContent?key=$API_KEY"
                val body = jsonBody.toRequestBody(JSON)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                if (attempt > 0) {
                    Log.d("GeminiAIService", "Retry attempt ${attempt + 1} for $modelName")
                } else {
                    Log.d("GeminiAIService", "Making request to: $url")
                }
                
                client.newCall(request).execute().use { response ->
                    Log.d("GeminiAIService", "Response code: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e("GeminiAIService", "API Error with $modelName: ${response.code} - $errorBody")
                        
                        // If it's a 503 (overloaded) error, wait and retry
                        if (response.code == 503 && attempt < 2) {
                            val waitTime = (attempt + 1) * 2000L // 2s, 4s
                            Log.w("GeminiAIService", "Model overloaded, waiting ${waitTime}ms before retry...")
                            Thread.sleep(waitTime)
                            return@repeat // Continue to next retry
                        }
                        
                        // For other errors (404, 400, etc.) don't retry
                        return null
                    }
                    
                    val resp = response.body?.string() ?: return null
                    Log.d("GeminiAIService", "Response received from $modelName, length: ${resp.length}")
                    
                    return extractTextFromResponse(resp)
                }
            } catch (e: Exception) {
                Log.e("GeminiAIService", "Network error with $modelName (attempt ${attempt + 1}): ${e.message}", e)
                
                // If it's a timeout and we have retries left, wait and retry
                if (e.message?.contains("timeout", ignoreCase = true) == true && attempt < 2) {
                    val waitTime = (attempt + 1) * 1000L // 1s, 2s
                    Log.w("GeminiAIService", "Timeout error, waiting ${waitTime}ms before retry...")
                    Thread.sleep(waitTime)
                } else if (attempt == 2) {
                    // Last attempt failed
                    return null
                }
            }
        }
        return null
    }

    // Parse Gemini response into plain text
    private fun extractTextFromResponse(resp: String): String? {
        return try {
            Log.d("GeminiAIService", "Parsing response: ${resp.take(200)}...")
            
            val root = JSONObject(resp)
            
            // Check for error in response
            if (root.has("error")) {
                val error = root.getJSONObject("error")
                val message = error.optString("message", "Unknown error")
                Log.e("GeminiAIService", "API returned error: $message")
                return null
            }
            
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                Log.e("GeminiAIService", "No candidates in response")
                return null
            }
            
            val content = candidates.getJSONObject(0).optJSONObject("content")
            if (content == null) {
                Log.e("GeminiAIService", "No content in first candidate")
                return null
            }
            
            val parts = content.optJSONArray("parts")
            if (parts == null) {
                Log.e("GeminiAIService", "No parts in content")
                return null
            }
            
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text", "")
                if (text.isNotEmpty()) sb.append(text)
            }
            
            val result = if (sb.isEmpty()) null else sb.toString()
            Log.d("GeminiAIService", "Extracted text length: ${result?.length ?: 0}")
            
            return result
        } catch (e: Exception) {
            Log.e("GeminiAIService", "Error parsing response: ${e.message}", e)
            null
        }
    }

    private fun getSubjectSpecificGuidelines(subject: String): String {
        return when (subject.lowercase()) {
            "mathematics", "math" -> """
                - Start with definitions and theorems
                - Include step-by-step problem solving
                - Provide multiple worked examples
                - Include practice problems with varying difficulty
                - Use mathematical notation properly
            """.trimIndent()
            
            "science", "physics", "chemistry", "biology" -> """
                - Begin with fundamental concepts
                - Include real-world applications
                - Add diagrams or describe visual elements
                - Include experiments or demonstrations
                - Connect to current research or discoveries
            """.trimIndent()
            
            "programming", "computer science", "coding" -> """
                - Start with concept explanation
                - Include code examples with comments
                - Provide best practices
                - Include common pitfalls to avoid
                - Add exercises with increasing complexity
            """.trimIndent()
            
            "history" -> """
                - Provide historical context
                - Include important dates and figures
                - Explain cause and effect relationships
                - Connect to modern relevance
                - Include primary source references
            """.trimIndent()
            
            "literature", "english" -> """
                - Include literary analysis techniques
                - Provide context about authors and periods
                - Include examples from texts
                - Discuss themes and symbolism
                - Add writing exercises
            """.trimIndent()
            
            else -> """
                - Start with fundamental concepts
                - Build complexity gradually
                - Include practical examples
                - Provide exercises for practice
                - Connect to real-world applications
            """.trimIndent()
        }
    }
}
