package com.example.ed.services

import android.content.Context
import android.util.Log
// Using existing GeminiAIService instead of direct API calls
import com.example.ed.services.GeminiAIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class AIGameContentGenerator private constructor(private val context: Context) {
    
    private val geminiService by lazy { GeminiAIService() }
    
    companion object {
        private const val TAG = "AIGameContentGenerator"
        
        @Volatile
        private var INSTANCE: AIGameContentGenerator? = null
        
        fun getInstance(context: Context): AIGameContentGenerator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIGameContentGenerator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Generate quiz questions for quiz game
    suspend fun generateQuizQuestions(
        subject: String = "General Knowledge",
        difficulty: String = "MEDIUM",
        count: Int = 10
    ): List<AIQuizQuestion> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Generate $count quiz questions for a mobile educational game.
                
                Subject: $subject
                Difficulty: $difficulty
                Format: Multiple choice with 4 options each
                
                Requirements:
                - Questions should be engaging and educational
                - Mix of topics within the subject
                - Avoid overly complex or trivial questions
                - Include a brief explanation for each correct answer
                
                Return ONLY a valid JSON array in this exact format:
                [
                    {
                        "question": "Question text here?",
                        "options": ["Option A", "Option B", "Option C", "Option D"],
                        "correctIndex": 0,
                        "explanation": "Brief explanation of why this is correct"
                    }
                ]
                
                Generate questions about: ${getRandomTopics(subject)}
            """.trimIndent()
            
            val response = geminiService.generateContent(prompt)
            parseQuizQuestions(response ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating quiz questions", e)
            generateFallbackQuizQuestions(subject, difficulty, count)
        }
    }
    
    // Generate memory game content
    suspend fun generateMemoryGameContent(
        theme: String = "Education",
        difficulty: String = "MEDIUM"
    ): List<MemoryGameCard> = withContext(Dispatchers.IO) {
        try {
            val cardCount = when (difficulty) {
                "EASY" -> 8
                "MEDIUM" -> 12
                "HARD" -> 16
                else -> 12
            }
            
            val prompt = """
                Generate ${cardCount / 2} pairs of educational content for a memory matching game.
                
                Theme: $theme
                Difficulty: $difficulty
                
                Create pairs that are:
                - Educationally meaningful (term-definition, question-answer, etc.)
                - Age-appropriate and engaging
                - Varied in difficulty within the level
                
                Return ONLY a valid JSON array in this exact format:
                [
                    {
                        "pairId": 1,
                        "card1Text": "Term or Question",
                        "card2Text": "Definition or Answer",
                        "category": "Subject area"
                    }
                ]
                
                Focus on: ${getRandomEducationalTopics()}
            """.trimIndent()
            
            val response = geminiService.generateContent(prompt)
            parseMemoryGameContent(response ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating memory game content", e)
            generateFallbackMemoryContent(theme, difficulty)
        }
    }
    
    // Generate puzzle game content
    suspend fun generatePuzzleGameContent(
        type: String = "Word Puzzle",
        difficulty: String = "MEDIUM"
    ): PuzzleGameContent = withContext(Dispatchers.IO) {
        try {
            return@withContext when (type) {
                "Word Puzzle" -> generateWordPuzzle(difficulty)
                "Math Puzzle" -> generateMathPuzzle(difficulty)
                "Logic Puzzle" -> generateLogicPuzzle(difficulty)
                else -> generateWordPuzzle(difficulty)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating puzzle content", e)
            return@withContext generateFallbackPuzzleContent(type, difficulty)
        }
    }
    
    private suspend fun generateWordPuzzle(difficulty: String): PuzzleGameContent {
        val prompt = """
            Generate a word puzzle for an educational mobile game.
            
            Difficulty: $difficulty
            Type: Word arrangement/anagram puzzle
            
            Create a puzzle where:
            - Players need to arrange letters to form educational terms
            - Include a helpful hint
            - Word should be educational and appropriate for students
            
            Return ONLY a valid JSON object in this exact format:
            {
                "word": "EDUCATION",
                "scrambledLetters": ["E", "D", "U", "C", "A", "T", "I", "O", "N"],
                "hint": "The process of learning and acquiring knowledge",
                "category": "General",
                "difficulty": "$difficulty"
            }
            
            Choose from educational topics like: science, mathematics, history, literature, technology
        """.trimIndent()
        
        val response = geminiService.generateContent(prompt)
        return parseWordPuzzle(response ?: "")
    }
    
    private suspend fun generateMathPuzzle(difficulty: String): PuzzleGameContent {
        val prompt = """
            Generate a math puzzle for an educational mobile game.
            
            Difficulty: $difficulty
            Type: Number sequence or equation puzzle
            
            Create a puzzle where:
            - Players solve a mathematical pattern or equation
            - Include a helpful hint
            - Appropriate difficulty for the level
            
            Return ONLY a valid JSON object in this exact format:
            {
                "question": "What comes next in the sequence: 2, 4, 8, 16, ?",
                "answer": "32",
                "hint": "Each number is double the previous one",
                "category": "Mathematics",
                "difficulty": "$difficulty"
            }
        """.trimIndent()
        
        val response = geminiService.generateContent(prompt)
        return parseMathPuzzle(response ?: "")
    }
    
    private suspend fun generateLogicPuzzle(difficulty: String): PuzzleGameContent {
        val prompt = """
            Generate a logic puzzle for an educational mobile game.
            
            Difficulty: $difficulty
            Type: Pattern recognition or logical reasoning
            
            Create a puzzle where:
            - Players use logical thinking to solve
            - Include a helpful hint
            - Engaging and educational
            
            Return ONLY a valid JSON object in this exact format:
            {
                "question": "If all roses are flowers, and some flowers are red, which statement must be true?",
                "options": ["All roses are red", "Some roses are red", "No roses are red", "Some roses might be red"],
                "correctIndex": 3,
                "hint": "Think about what 'must be true' means in logic",
                "category": "Logic",
                "difficulty": "$difficulty"
            }
        """.trimIndent()
        
        val response = geminiService.generateContent(prompt)
        return parseLogicPuzzle(response ?: "")
    }
    
    // Parsing functions
    private fun parseQuizQuestions(jsonText: String): List<AIQuizQuestion> {
        return try {
            val cleanJson = cleanJsonResponse(jsonText)
            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<AIQuizQuestion>()
            
            for (i in 0 until jsonArray.length()) {
                val questionObj = jsonArray.getJSONObject(i)
                val optionsArray = questionObj.getJSONArray("options")
                val options = mutableListOf<String>()
                
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                
                questions.add(
                    AIQuizQuestion(
                        question = questionObj.getString("question"),
                        options = options,
                        correctIndex = questionObj.getInt("correctIndex"),
                        explanation = questionObj.getString("explanation")
                    )
                )
            }
            
            questions
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing quiz questions", e)
            emptyList()
        }
    }
    
    private fun parseMemoryGameContent(jsonText: String): List<MemoryGameCard> {
        return try {
            val cleanJson = cleanJsonResponse(jsonText)
            val jsonArray = JSONArray(cleanJson)
            val cards = mutableListOf<MemoryGameCard>()
            
            for (i in 0 until jsonArray.length()) {
                val pairObj = jsonArray.getJSONObject(i)
                val pairId = pairObj.getInt("pairId")
                
                // Create two cards for each pair
                cards.add(
                    MemoryGameCard(
                        id = pairId * 2 - 1,
                        pairId = pairId,
                        text = pairObj.getString("card1Text"),
                        category = pairObj.getString("category")
                    )
                )
                cards.add(
                    MemoryGameCard(
                        id = pairId * 2,
                        pairId = pairId,
                        text = pairObj.getString("card2Text"),
                        category = pairObj.getString("category")
                    )
                )
            }
            
            cards.shuffled()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing memory game content", e)
            emptyList()
        }
    }
    
    private fun parseWordPuzzle(jsonText: String): PuzzleGameContent {
        return try {
            val cleanJson = cleanJsonResponse(jsonText)
            val jsonObj = JSONObject(cleanJson)
            val lettersArray = jsonObj.getJSONArray("scrambledLetters")
            val letters = mutableListOf<String>()
            
            for (i in 0 until lettersArray.length()) {
                letters.add(lettersArray.getString(i))
            }
            
            PuzzleGameContent.WordPuzzle(
                word = jsonObj.getString("word"),
                scrambledLetters = letters,
                hint = jsonObj.getString("hint"),
                category = jsonObj.getString("category")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing word puzzle", e)
            generateFallbackPuzzleContent("Word Puzzle", "MEDIUM")
        }
    }
    
    private fun parseMathPuzzle(jsonText: String): PuzzleGameContent {
        return try {
            val cleanJson = cleanJsonResponse(jsonText)
            val jsonObj = JSONObject(cleanJson)
            
            PuzzleGameContent.MathPuzzle(
                question = jsonObj.getString("question"),
                answer = jsonObj.getString("answer"),
                hint = jsonObj.getString("hint"),
                category = jsonObj.getString("category")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing math puzzle", e)
            generateFallbackPuzzleContent("Math Puzzle", "MEDIUM")
        }
    }
    
    private fun parseLogicPuzzle(jsonText: String): PuzzleGameContent {
        return try {
            val cleanJson = cleanJsonResponse(jsonText)
            val jsonObj = JSONObject(cleanJson)
            val optionsArray = jsonObj.getJSONArray("options")
            val options = mutableListOf<String>()
            
            for (i in 0 until optionsArray.length()) {
                options.add(optionsArray.getString(i))
            }
            
            PuzzleGameContent.LogicPuzzle(
                question = jsonObj.getString("question"),
                options = options,
                correctIndex = jsonObj.getInt("correctIndex"),
                hint = jsonObj.getString("hint"),
                category = jsonObj.getString("category")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing logic puzzle", e)
            generateFallbackPuzzleContent("Logic Puzzle", "MEDIUM")
        }
    }
    
    // Helper functions
    private fun cleanJsonResponse(response: String): String {
        return response
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
    
    private fun getRandomTopics(subject: String): String {
        val topics = when (subject.lowercase()) {
            "science" -> listOf("physics", "chemistry", "biology", "astronomy", "earth science")
            "mathematics" -> listOf("algebra", "geometry", "statistics", "calculus", "number theory")
            "history" -> listOf("world history", "ancient civilizations", "modern history", "geography")
            "literature" -> listOf("classic novels", "poetry", "drama", "literary devices")
            else -> listOf("science", "mathematics", "history", "literature", "technology")
        }
        return topics.shuffled().take(3).joinToString(", ")
    }
    
    private fun getRandomEducationalTopics(): String {
        val topics = listOf(
            "science vocabulary", "mathematical terms", "historical figures",
            "literary concepts", "geographical features", "technology terms"
        )
        return topics.shuffled().take(2).joinToString(", ")
    }
    
    // Fallback functions for when AI fails
    private fun generateFallbackQuizQuestions(subject: String, difficulty: String, count: Int): List<AIQuizQuestion> {
        val fallbackQuestions = listOf(
            AIQuizQuestion(
                "What is the largest planet in our solar system?",
                listOf("Earth", "Jupiter", "Saturn", "Mars"),
                1,
                "Jupiter is the largest planet by both mass and volume."
            ),
            AIQuizQuestion(
                "What is 15 + 27?",
                listOf("42", "41", "43", "40"),
                0,
                "15 + 27 = 42"
            ),
            AIQuizQuestion(
                "Who wrote 'Romeo and Juliet'?",
                listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"),
                1,
                "William Shakespeare wrote this famous tragedy."
            )
        )
        return fallbackQuestions.shuffled().take(count)
    }
    
    private fun generateFallbackMemoryContent(theme: String, difficulty: String): List<MemoryGameCard> {
        val fallbackPairs = listOf(
            Pair("H2O", "Water"),
            Pair("CO2", "Carbon Dioxide"),
            Pair("NaCl", "Salt"),
            Pair("2 + 2", "4"),
            Pair("5 × 3", "15"),
            Pair("Square root of 16", "4")
        )
        
        val cards = mutableListOf<MemoryGameCard>()
        fallbackPairs.forEachIndexed { index, pair ->
            cards.add(MemoryGameCard(index * 2 + 1, index + 1, pair.first, theme))
            cards.add(MemoryGameCard(index * 2 + 2, index + 1, pair.second, theme))
        }
        
        return cards.shuffled()
    }
    
    private fun generateFallbackPuzzleContent(type: String, difficulty: String): PuzzleGameContent {
        return when (type) {
            "Math Puzzle" -> PuzzleGameContent.MathPuzzle(
                "What is 8 × 7?",
                "56",
                "Multiply the two numbers",
                "Mathematics"
            )
            "Logic Puzzle" -> PuzzleGameContent.LogicPuzzle(
                "Which number comes next: 1, 1, 2, 3, 5, 8, ?",
                listOf("11", "13", "15", "16"),
                1,
                "This is the Fibonacci sequence - each number is the sum of the two preceding ones",
                "Logic"
            )
            else -> PuzzleGameContent.WordPuzzle(
                "STUDENT",
                listOf("S", "T", "U", "D", "E", "N", "T").shuffled(),
                "A person who is learning",
                "Education"
            )
        }
    }
}

// Data classes for game content
data class AIQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class MemoryGameCard(
    val id: Int,
    val pairId: Int,
    val text: String,
    val category: String,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

sealed class PuzzleGameContent {
    data class WordPuzzle(
        val word: String,
        val scrambledLetters: List<String>,
        val hint: String,
        val category: String
    ) : PuzzleGameContent()
    
    data class MathPuzzle(
        val question: String,
        val answer: String,
        val hint: String,
        val category: String
    ) : PuzzleGameContent()
    
    data class LogicPuzzle(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val hint: String,
        val category: String
    ) : PuzzleGameContent()
}
