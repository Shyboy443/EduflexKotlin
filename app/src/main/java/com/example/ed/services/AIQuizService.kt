package com.example.ed.services

import android.util.Log
import com.example.ed.BuildConfig
import com.example.ed.models.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<OpenAIMessage>,
    val max_tokens: Int = 2000,
    val temperature: Double = 0.7
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

data class OpenAIChoice(
    val message: OpenAIMessage
)

data class GeneratedQuizData(
    val title: String,
    val description: String,
    val questions: List<GeneratedQuestion>
)

data class GeneratedQuestion(
    val question: String,
    val type: String,
    val options: List<String>? = null,
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String = "medium",
    val points: Int = 10
)

class AIQuizService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val apiKey = BuildConfig.OPENAI_API_KEY // Use BuildConfig for API key
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    suspend fun generateQuiz(
        topic: String,
        difficulty: String,
        questionCount: Int,
        questionTypes: List<QuestionType>,
        courseContext: String? = null
    ): Result<Quiz> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildQuizPrompt(topic, difficulty, questionCount, questionTypes, courseContext)
            val response = callOpenAI(prompt)
            val quizData = parseQuizResponse(response)
            val quiz = convertToQuiz(quizData, topic, difficulty)
            Result.success(quiz)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateQuestionsForExistingQuiz(
        quizId: String,
        additionalQuestions: Int,
        topic: String,
        difficulty: String
    ): Result<List<Question>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildAdditionalQuestionsPrompt(topic, difficulty, additionalQuestions)
            val response = callOpenAI(prompt)
            val questionsData = parseQuestionsResponse(response)
            val questions = convertToQuestions(questionsData, quizId)
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun improveQuestion(
        question: Question,
        feedback: String
    ): Result<Question> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildQuestionImprovementPrompt(question, feedback)
            val response = callOpenAI(prompt)
            val improvedQuestionData = parseQuestionResponse(response)
            val improvedQuestion = convertToQuestion(improvedQuestionData, question.quizId)
            Result.success(improvedQuestion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateExplanation(
        question: String,
        correctAnswer: String,
        topic: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildExplanationPrompt(question, correctAnswer, topic)
            val response = callOpenAI(prompt)
            Result.success(response.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callOpenAI(prompt: String): String = withContext(Dispatchers.IO) {
        // For demo purposes, return mock response since we don't have a real API key
        if (apiKey == "demo-key-for-testing") {
            return@withContext generateMockQuizResponse(prompt)
        }
        
        val request = OpenAIRequest(
            messages = listOf(
                OpenAIMessage("system", "You are an expert educational content creator specializing in quiz generation."),
                OpenAIMessage("user", prompt)
            )
        )

        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw IOException("OpenAI API call failed: ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
        openAIResponse.choices.firstOrNull()?.message?.content ?: throw IOException("No content in response")
    }

    private fun buildQuizPrompt(
        topic: String,
        difficulty: String,
        questionCount: Int,
        questionTypes: List<QuestionType>,
        courseContext: String?
    ): String {
        val typesString = questionTypes.joinToString(", ") { it.name.lowercase() }
        val contextString = courseContext?.let { "\n\nCourse Context: $it" } ?: ""
        
        return """
        Create a comprehensive quiz about "$topic" with the following specifications:
        - Difficulty: $difficulty
        - Number of questions: $questionCount
        - Question types: $typesString
        - Educational level: College/University
        $contextString
        
        Please return the response in the following JSON format:
        {
            "title": "Quiz title",
            "description": "Brief description of the quiz",
            "questions": [
                {
                    "question": "Question text",
                    "type": "multiple_choice|true_false|short_answer|essay|fill_blank",
                    "options": ["Option 1", "Option 2", "Option 3", "Option 4"] (for multiple choice only),
                    "correctAnswer": "Correct answer",
                    "explanation": "Detailed explanation of why this is correct",
                    "difficulty": "easy|medium|hard",
                    "points": 10
                }
            ]
        }
        
        Ensure questions are:
        1. Educationally sound and accurate
        2. Appropriately challenging for the specified difficulty
        3. Include detailed explanations
        4. Cover different aspects of the topic
        5. Follow best practices for question writing
        """.trimIndent()
    }

    private fun buildAdditionalQuestionsPrompt(
        topic: String,
        difficulty: String,
        count: Int
    ): String {
        return """
        Generate $count additional questions about "$topic" with $difficulty difficulty.
        
        Return as JSON array:
        [
            {
                "question": "Question text",
                "type": "multiple_choice|true_false|short_answer|essay|fill_blank",
                "options": ["Option 1", "Option 2", "Option 3", "Option 4"] (if applicable),
                "correctAnswer": "Correct answer",
                "explanation": "Detailed explanation",
                "difficulty": "$difficulty",
                "points": 10
            }
        ]
        """.trimIndent()
    }

    private fun buildQuestionImprovementPrompt(question: Question, feedback: String): String {
        return """
        Improve the following question based on the feedback provided:
        
        Original Question: ${question.question}
        Type: ${question.type}
        Current Options: ${question.options}
        Feedback: $feedback
        
        Return improved question in JSON format:
        {
            "question": "Improved question text",
            "type": "${question.type}",
            "options": ["Option 1", "Option 2", "Option 3", "Option 4"] (if applicable),
            "correctAnswer": "Correct answer",
            "explanation": "Detailed explanation",
            "difficulty": "${question.difficulty}",
            "points": ${question.points}
        }
        """.trimIndent()
    }

    private fun buildExplanationPrompt(question: String, correctAnswer: String, topic: String): String {
        return """
        Provide a detailed, educational explanation for why "$correctAnswer" is the correct answer to this question about $topic:
        
        Question: $question
        Correct Answer: $correctAnswer
        
        The explanation should:
        1. Be clear and educational
        2. Help students understand the concept
        3. Be 2-3 sentences long
        4. Include relevant context when helpful
        """.trimIndent()
    }

    private fun parseQuizResponse(response: String): GeneratedQuizData {
        return try {
            gson.fromJson(response, GeneratedQuizData::class.java)
        } catch (e: Exception) {
            // Fallback parsing if JSON is malformed
            GeneratedQuizData(
                title = "AI Generated Quiz",
                description = "Generated quiz content",
                questions = emptyList()
            )
        }
    }

    private fun parseQuestionsResponse(response: String): List<GeneratedQuestion> {
        return try {
            gson.fromJson(response, Array<GeneratedQuestion>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseQuestionResponse(response: String): GeneratedQuestion {
        return gson.fromJson(response, GeneratedQuestion::class.java)
    }

    private fun convertToQuiz(data: GeneratedQuizData, topic: String, difficulty: String): Quiz {
        val quizId = UUID.randomUUID().toString()
        return Quiz(
            id = quizId,
            title = data.title,
            description = data.description,
            courseId = "", // Will be set by caller
            teacherId = "", // Will be set by caller
            questions = convertToQuestions(data.questions, quizId),
            timeLimit = 30, // Default 30 minutes
            maxAttempts = 3,
            passingScore = 70,
            isPublished = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            difficulty = QuizDifficulty.MEDIUM,
            category = topic,
            tags = listOf(topic, "ai-generated"),
            aiGenerated = true
        )
    }

    private fun convertToQuestions(questionsData: List<GeneratedQuestion>, quizId: String): List<Question> {
        return questionsData.mapIndexed { index, data ->
            convertToQuestion(data, quizId, index)
        }
    }

    private fun convertToQuestion(data: GeneratedQuestion, quizId: String, order: Int = 0): Question {
        val questionId = UUID.randomUUID().toString()
        val questionType = when (data.type.lowercase()) {
            "multiple_choice" -> QuestionType.MULTIPLE_CHOICE
            "true_false" -> QuestionType.TRUE_FALSE
            "short_answer" -> QuestionType.SHORT_ANSWER
            "essay" -> QuestionType.ESSAY
            "fill_blank" -> QuestionType.FILL_IN_BLANK
            else -> QuestionType.MULTIPLE_CHOICE
        }

        val answers: List<String> = when (questionType) {
            QuestionType.MULTIPLE_CHOICE -> {
                data.options?.map { option -> option } ?: emptyList()
            }
            QuestionType.TRUE_FALSE -> {
                listOf("True", "False")
            }
            else -> {
                listOf(data.correctAnswer)
            }
        }

        return Question(
            id = questionId,
            quizId = quizId,
            question = data.question,
            type = questionType,
            options = answers,
            correctAnswers = listOf(data.correctAnswer),
            points = data.points,
            order = order,
            explanation = data.explanation,
            difficulty = QuestionDifficulty.MEDIUM,
            aiGenerated = true
        )
    }

    private fun generateMockQuizResponse(prompt: String): String {
        // Extract topic from prompt for more relevant mock data
        val topic = when {
            prompt.contains("programming", ignoreCase = true) || prompt.contains("coding", ignoreCase = true) -> "Programming"
            prompt.contains("math", ignoreCase = true) || prompt.contains("mathematics", ignoreCase = true) -> "Mathematics"
            prompt.contains("science", ignoreCase = true) -> "Science"
            prompt.contains("history", ignoreCase = true) -> "History"
            else -> "General Knowledge"
        }
        
        return """
        {
            "title": "$topic Quiz - AI Generated",
            "description": "An AI-generated quiz covering fundamental concepts in $topic",
            "questions": [
                {
                    "question": "What is a fundamental concept in $topic?",
                    "type": "multiple_choice",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswer": "Option A",
                    "explanation": "This is the correct answer because it represents the most fundamental concept.",
                    "points": 10
                },
                {
                    "question": "True or False: $topic requires continuous learning and practice?",
                    "type": "true_false",
                    "options": ["True", "False"],
                    "correctAnswer": "True",
                    "explanation": "Continuous learning is essential for mastery in any field.",
                    "points": 5
                },
                {
                    "question": "Explain the importance of understanding basic principles in $topic.",
                    "type": "short_answer",
                    "correctAnswer": "Understanding basic principles provides a foundation for advanced concepts and practical application.",
                    "explanation": "A solid foundation in basics enables better comprehension of complex topics.",
                    "points": 15
                }
            ]
        }
        """.trimIndent()
    }
}