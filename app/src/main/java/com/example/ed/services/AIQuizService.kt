package com.example.ed.services

import com.example.ed.models.Question
import com.example.ed.models.QuestionDifficulty
import com.example.ed.models.QuestionType
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class AIQuizService {
    
    private val geminiService = GeminiAIService()
    
    fun generateQuestions(
        topic: String,
        numberOfQuestions: Int,
        questionType: String,
        difficulty: QuestionDifficulty,
        courseContext: String,
        callback: (List<Question>?) -> Unit
    ) {
        runBlocking {
            try {
                val jsonResponse = geminiService.generateQuizQuestions(
                    topic = topic,
                    numberOfQuestions = numberOfQuestions,
                    questionType = questionType,
                    difficulty = difficulty.name,
                    courseContext = courseContext
                )
                
                if (jsonResponse != null) {
                    val questions = parseQuestionsFromJson(jsonResponse, difficulty)
                    callback(questions)
                } else {
                    // Fallback to local generation if API fails
                    val questions = when (questionType) {
                        "Multiple Choice" -> generateMultipleChoiceQuestions(topic, numberOfQuestions, difficulty)
                        "True/False" -> generateTrueFalseQuestions(topic, numberOfQuestions, difficulty)
                        "Short Answer" -> generateShortAnswerQuestions(topic, numberOfQuestions, difficulty)
                        else -> generateMixedQuestions(topic, numberOfQuestions, difficulty)
                    }
                    callback(questions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to local generation
                val questions = generateMixedQuestions(topic, numberOfQuestions, difficulty)
                callback(questions)
            }
        }
    }
    
    private fun parseQuestionsFromJson(jsonString: String, difficulty: QuestionDifficulty): List<Question> {
        val questions = mutableListOf<Question>()
        
        try {
            // Extract JSON from the response (Gemini might include extra text)
            val jsonStart = jsonString.indexOf("{")
            val jsonEnd = jsonString.lastIndexOf("}") + 1
            val cleanJson = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonString.substring(jsonStart, jsonEnd)
            } else {
                jsonString
            }
            
            val jsonObject = JSONObject(cleanJson)
            val questionsArray = jsonObject.getJSONArray("questions")
            
            for (i in 0 until questionsArray.length()) {
                val questionObj = questionsArray.getJSONObject(i)
                
                val questionType = when (questionObj.getString("type")) {
                    "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE
                    "TRUE_FALSE" -> QuestionType.TRUE_FALSE
                    "SHORT_ANSWER" -> QuestionType.SHORT_ANSWER
                    else -> QuestionType.MULTIPLE_CHOICE
                }
                
                val options = mutableListOf<String>()
                if (questionObj.has("options")) {
                    val optionsArray = questionObj.getJSONArray("options")
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(j))
                    }
                }
                
                val question = Question(
                    id = UUID.randomUUID().toString(),
                    type = questionType,
                    question = questionObj.getString("question"),
                    options = options,
                    correctAnswers = listOf(questionObj.getString("correctAnswer")),
                    explanation = questionObj.optString("explanation", ""),
                    points = questionObj.optInt("points", 1),
                    difficulty = difficulty,
                    topic = "",
                    order = i,
                    aiGenerated = true,
                    createdAt = System.currentTimeMillis()
                )
                
                questions.add(question)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return questions
    }
    
    private fun generateMultipleChoiceQuestions(
        topic: String,
        count: Int,
        difficulty: QuestionDifficulty
    ): List<Question> {
        val questions = mutableListOf<Question>()
        val templates = getMultipleChoiceTemplates(topic, difficulty)
        
        for (i in 0 until minOf(count, templates.size)) {
            val template = templates[i]
            questions.add(
                Question(
                    id = UUID.randomUUID().toString(),
                    type = QuestionType.MULTIPLE_CHOICE,
                    question = template.question,
                    options = template.options,
                    correctAnswers = listOf(template.correctAnswer),
                    explanation = template.explanation,
                    points = when (difficulty) {
                        QuestionDifficulty.EASY -> 1
                        QuestionDifficulty.MEDIUM -> 2
                        QuestionDifficulty.HARD -> 3
                    },
                    difficulty = difficulty,
                    topic = topic,
                    order = i,
                    aiGenerated = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        
        return questions
    }
    
    private fun generateTrueFalseQuestions(
        topic: String,
        count: Int,
        difficulty: QuestionDifficulty
    ): List<Question> {
        val questions = mutableListOf<Question>()
        val templates = getTrueFalseTemplates(topic, difficulty)
        
        for (i in 0 until minOf(count, templates.size)) {
            val template = templates[i]
            questions.add(
                Question(
                    id = UUID.randomUUID().toString(),
                    type = QuestionType.TRUE_FALSE,
                    question = template.statement,
                    options = listOf("True", "False"),
                    correctAnswers = listOf(if (template.isTrue) "True" else "False"),
                    explanation = template.explanation,
                    points = 1,
                    difficulty = difficulty,
                    topic = topic,
                    order = i,
                    aiGenerated = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        
        return questions
    }
    
    private fun generateShortAnswerQuestions(
        topic: String,
        count: Int,
        difficulty: QuestionDifficulty
    ): List<Question> {
        val questions = mutableListOf<Question>()
        val templates = getShortAnswerTemplates(topic, difficulty)
        
        for (i in 0 until minOf(count, templates.size)) {
            val template = templates[i]
            questions.add(
                Question(
                    id = UUID.randomUUID().toString(),
                    type = QuestionType.SHORT_ANSWER,
                    question = template.question,
                    options = emptyList(),
                    correctAnswers = template.acceptableAnswers,
                    explanation = template.explanation,
                    points = when (difficulty) {
                        QuestionDifficulty.EASY -> 2
                        QuestionDifficulty.MEDIUM -> 3
                        QuestionDifficulty.HARD -> 5
                    },
                    difficulty = difficulty,
                    topic = topic,
                    order = i,
                    aiGenerated = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        
        return questions
    }
    
    private fun generateMixedQuestions(
        topic: String,
        count: Int,
        difficulty: QuestionDifficulty
    ): List<Question> {
        val questions = mutableListOf<Question>()
        val mcCount = count / 2
        val tfCount = count / 3
        val saCount = count - mcCount - tfCount
        
        questions.addAll(generateMultipleChoiceQuestions(topic, mcCount, difficulty))
        questions.addAll(generateTrueFalseQuestions(topic, tfCount, difficulty))
        questions.addAll(generateShortAnswerQuestions(topic, saCount, difficulty))
        
        return questions.shuffled()
    }
    
    // Template data structures
    data class MCTemplate(
        val question: String,
        val options: List<String>,
        val correctAnswer: String,
        val explanation: String
    )
    
    data class TFTemplate(
        val statement: String,
        val isTrue: Boolean,
        val explanation: String
    )
    
    data class SATemplate(
        val question: String,
        val acceptableAnswers: List<String>,
        val explanation: String
    )
    
    // Sample template generators based on common topics
    private fun getMultipleChoiceTemplates(topic: String, difficulty: QuestionDifficulty): List<MCTemplate> {
        val topicLower = topic.lowercase()
        
        return when {
            topicLower.contains("math") || topicLower.contains("mathematics") -> getMathMCTemplates(difficulty)
            topicLower.contains("science") || topicLower.contains("physics") -> getScienceMCTemplates(difficulty)
            topicLower.contains("programming") || topicLower.contains("coding") -> getProgrammingMCTemplates(difficulty)
            topicLower.contains("history") -> getHistoryMCTemplates(difficulty)
            topicLower.contains("english") || topicLower.contains("literature") -> getEnglishMCTemplates(difficulty)
            else -> getGeneralMCTemplates(topic, difficulty)
        }
    }
    
    private fun getMathMCTemplates(difficulty: QuestionDifficulty): List<MCTemplate> {
        return when (difficulty) {
            QuestionDifficulty.EASY -> listOf(
                MCTemplate(
                    "What is 15 + 27?",
                    listOf("42", "41", "43", "40"),
                    "42",
                    "15 + 27 = 42. Add the units: 5 + 7 = 12 (write 2, carry 1). Add the tens: 1 + 2 + 1 = 4."
                ),
                MCTemplate(
                    "What is 8 × 7?",
                    listOf("56", "54", "58", "52"),
                    "56",
                    "8 × 7 = 56. This is a basic multiplication fact."
                ),
                MCTemplate(
                    "What is the value of x in: x + 5 = 12?",
                    listOf("7", "6", "8", "5"),
                    "7",
                    "To solve x + 5 = 12, subtract 5 from both sides: x = 12 - 5 = 7"
                )
            )
            QuestionDifficulty.MEDIUM -> listOf(
                MCTemplate(
                    "What is the derivative of f(x) = 3x² + 2x - 5?",
                    listOf("6x + 2", "6x - 2", "3x + 2", "6x² + 2"),
                    "6x + 2",
                    "Using the power rule: d/dx(3x²) = 6x, d/dx(2x) = 2, d/dx(-5) = 0. So f'(x) = 6x + 2"
                ),
                MCTemplate(
                    "What is the area of a circle with radius 5?",
                    listOf("25π", "10π", "5π", "50π"),
                    "25π",
                    "Area of a circle = πr². With r = 5, Area = π(5)² = 25π"
                )
            )
            QuestionDifficulty.HARD -> listOf(
                MCTemplate(
                    "What is the integral of ∫(2x³ - 3x² + x)dx?",
                    listOf("x⁴/2 - x³ + x²/2 + C", "2x⁴ - x³ + x²/2 + C", "x⁴/2 - x³ + x² + C", "x⁴ - x³ + x²/2 + C"),
                    "x⁴/2 - x³ + x²/2 + C",
                    "Using the power rule for integration: ∫x^n dx = x^(n+1)/(n+1) + C"
                )
            )
        }
    }
    
    private fun getScienceMCTemplates(difficulty: QuestionDifficulty): List<MCTemplate> {
        return when (difficulty) {
            QuestionDifficulty.EASY -> listOf(
                MCTemplate(
                    "What is the chemical symbol for water?",
                    listOf("H₂O", "HO₂", "H₂O₂", "HO"),
                    "H₂O",
                    "Water consists of 2 hydrogen atoms and 1 oxygen atom, hence H₂O"
                ),
                MCTemplate(
                    "What is the speed of light in vacuum?",
                    listOf("3×10⁸ m/s", "3×10⁶ m/s", "3×10¹⁰ m/s", "3×10⁷ m/s"),
                    "3×10⁸ m/s",
                    "The speed of light in vacuum is approximately 299,792,458 m/s, or 3×10⁸ m/s"
                )
            )
            QuestionDifficulty.MEDIUM -> listOf(
                MCTemplate(
                    "What is Newton's Second Law of Motion?",
                    listOf("F = ma", "F = mv", "E = mc²", "p = mv"),
                    "F = ma",
                    "Newton's Second Law states that Force equals mass times acceleration (F = ma)"
                )
            )
            QuestionDifficulty.HARD -> listOf(
                MCTemplate(
                    "What is the Heisenberg Uncertainty Principle about?",
                    listOf(
                        "Position and momentum cannot both be precisely known",
                        "Energy is conserved in all systems",
                        "Mass and energy are equivalent",
                        "Entropy always increases"
                    ),
                    "Position and momentum cannot both be precisely known",
                    "The Heisenberg Uncertainty Principle states that we cannot simultaneously know both the exact position and momentum of a particle"
                )
            )
        }
    }
    
    private fun getProgrammingMCTemplates(difficulty: QuestionDifficulty): List<MCTemplate> {
        return when (difficulty) {
            QuestionDifficulty.EASY -> listOf(
                MCTemplate(
                    "What does 'var' keyword do in Kotlin?",
                    listOf(
                        "Declares a mutable variable",
                        "Declares an immutable variable",
                        "Declares a function",
                        "Declares a class"
                    ),
                    "Declares a mutable variable",
                    "In Kotlin, 'var' declares a mutable (changeable) variable, while 'val' declares an immutable one"
                ),
                MCTemplate(
                    "What is the time complexity of binary search?",
                    listOf("O(log n)", "O(n)", "O(n²)", "O(1)"),
                    "O(log n)",
                    "Binary search divides the search space in half with each iteration, resulting in O(log n) complexity"
                )
            )
            QuestionDifficulty.MEDIUM -> listOf(
                MCTemplate(
                    "What is a lambda expression in Kotlin?",
                    listOf(
                        "An anonymous function",
                        "A type of loop",
                        "A class declaration",
                        "A variable type"
                    ),
                    "An anonymous function",
                    "Lambda expressions are anonymous functions that can be passed as arguments or stored in variables"
                )
            )
            QuestionDifficulty.HARD -> listOf(
                MCTemplate(
                    "What is the purpose of coroutines in Kotlin?",
                    listOf(
                        "Asynchronous programming without callbacks",
                        "Memory management",
                        "Type checking",
                        "Code compilation"
                    ),
                    "Asynchronous programming without callbacks",
                    "Coroutines provide a way to write asynchronous code in a sequential manner, avoiding callback hell"
                )
            )
        }
    }
    
    private fun getHistoryMCTemplates(difficulty: QuestionDifficulty): List<MCTemplate> {
        return listOf(
            MCTemplate(
                "When did World War II end?",
                listOf("1945", "1944", "1946", "1943"),
                "1945",
                "World War II ended in 1945 with the surrender of Japan on September 2, 1945"
            )
        )
    }
    
    private fun getEnglishMCTemplates(difficulty: QuestionDifficulty): List<MCTemplate> {
        return listOf(
            MCTemplate(
                "What is a metaphor?",
                listOf(
                    "A direct comparison without using 'like' or 'as'",
                    "A comparison using 'like' or 'as'",
                    "An exaggeration",
                    "A sound word"
                ),
                "A direct comparison without using 'like' or 'as'",
                "A metaphor directly states that one thing is another, while a simile uses 'like' or 'as'"
            )
        )
    }
    
    private fun getGeneralMCTemplates(topic: String, difficulty: QuestionDifficulty): List<MCTemplate> {
        return listOf(
            MCTemplate(
                "Which of the following best describes $topic?",
                listOf(
                    "A key concept in the field",
                    "An outdated practice",
                    "A minor detail",
                    "An unrelated concept"
                ),
                "A key concept in the field",
                "$topic is an important concept that students should understand"
            ),
            MCTemplate(
                "What is the primary purpose of studying $topic?",
                listOf(
                    "To gain comprehensive understanding",
                    "To memorize facts",
                    "To pass exams only",
                    "None of the above"
                ),
                "To gain comprehensive understanding",
                "Studying $topic helps develop a deep understanding of the subject matter"
            )
        )
    }
    
    private fun getTrueFalseTemplates(topic: String, difficulty: QuestionDifficulty): List<TFTemplate> {
        val topicLower = topic.lowercase()
        
        return when {
            topicLower.contains("math") -> listOf(
                TFTemplate(
                    "The sum of angles in a triangle is always 180 degrees.",
                    true,
                    "In Euclidean geometry, the sum of the interior angles of a triangle is always 180 degrees"
                ),
                TFTemplate(
                    "All prime numbers are odd.",
                    false,
                    "This is false because 2 is a prime number and it is even"
                )
            )
            topicLower.contains("science") -> listOf(
                TFTemplate(
                    "Water boils at 100°C at sea level.",
                    true,
                    "At standard atmospheric pressure (sea level), pure water boils at 100°C or 212°F"
                ),
                TFTemplate(
                    "The Earth is the largest planet in our solar system.",
                    false,
                    "Jupiter is the largest planet in our solar system, not Earth"
                )
            )
            else -> listOf(
                TFTemplate(
                    "$topic is an important subject to study.",
                    true,
                    "Understanding $topic is valuable for academic and practical purposes"
                ),
                TFTemplate(
                    "$topic has no real-world applications.",
                    false,
                    "$topic has many practical applications in various fields"
                )
            )
        }
    }
    
    private fun getShortAnswerTemplates(topic: String, difficulty: QuestionDifficulty): List<SATemplate> {
        val topicLower = topic.lowercase()
        
        return when {
            topicLower.contains("math") -> listOf(
                SATemplate(
                    "What is the formula for the area of a rectangle?",
                    listOf("length × width", "l × w", "length times width", "base × height"),
                    "The area of a rectangle is calculated by multiplying its length by its width"
                ),
                SATemplate(
                    "Define a prime number.",
                    listOf(
                        "A number divisible only by 1 and itself",
                        "A natural number greater than 1 that has no positive divisors other than 1 and itself"
                    ),
                    "A prime number is a natural number greater than 1 that cannot be formed by multiplying two smaller natural numbers"
                )
            )
            topicLower.contains("science") -> listOf(
                SATemplate(
                    "What is photosynthesis?",
                    listOf(
                        "The process by which plants convert light energy into chemical energy",
                        "Process where plants make food using sunlight",
                        "Conversion of light energy to chemical energy in plants"
                    ),
                    "Photosynthesis is the process by which plants use sunlight, water, and carbon dioxide to create oxygen and energy in the form of sugar"
                )
            )
            else -> listOf(
                SATemplate(
                    "Define $topic in your own words.",
                    listOf("$topic is", "It is"),
                    "$topic is a concept that encompasses various aspects of the subject matter"
                ),
                SATemplate(
                    "Give one example of $topic.",
                    listOf("Example:", "For instance", "Such as"),
                    "Examples help illustrate the practical application of $topic"
                )
            )
        }
    }
}
