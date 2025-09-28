package com.example.ed.ui.student.games

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ed.R
import com.example.ed.databinding.FragmentMemoryGameBinding
import com.example.ed.models.*
import com.example.ed.services.AIGameContentGenerator
import com.example.ed.services.GamificationService
import com.example.ed.services.PointsRewardsService
import com.example.ed.services.PointsType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MemoryGameFragment : Fragment() {

    private var _binding: FragmentMemoryGameBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var gamificationService: GamificationService
    private lateinit var aiContentGenerator: AIGameContentGenerator
    private lateinit var pointsRewardsService: PointsRewardsService
    private lateinit var auth: FirebaseAuth
    
    // Callback for UI refresh
    private var onPointsUpdated: (() -> Unit)? = null
    
    private var difficulty: GameDifficulty = GameDifficulty.EASY
    private var isWordFocused: Boolean = false
    private var aiMemoryCards: MutableList<com.example.ed.services.MemoryGameCard> = mutableListOf()
    private var memoryAdapter: MemoryAdapter? = null
    private var startTime = 0L
    private var gameTimer: CountDownTimer? = null
    private var flippedCards: MutableList<com.example.ed.services.MemoryGameCard> = mutableListOf()
    private var matches = 0
    private var attempts = 0
    private var pointsEarned = 0
    private var isProcessing = false
    
    private var gameResultListener: ((GameResult) -> Unit)? = null

    companion object {
        private const val ARG_DIFFICULTY = "difficulty"
        private const val ARG_WORD_FOCUSED = "word_focused"
        private const val GAME_TIME_LIMIT = 300000L // 5 minutes
        
        fun newInstance(difficulty: GameDifficulty, isWordFocused: Boolean = false): MemoryGameFragment {
            val fragment = MemoryGameFragment()
            val args = Bundle()
            args.putString(ARG_DIFFICULTY, difficulty.name)
            args.putBoolean(ARG_WORD_FOCUSED, isWordFocused)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            difficulty = GameDifficulty.valueOf(it.getString(ARG_DIFFICULTY, GameDifficulty.EASY.name))
            isWordFocused = it.getBoolean(ARG_WORD_FOCUSED, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoryGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize services
        gamificationService = GamificationService.getInstance(requireContext())
        aiContentGenerator = AIGameContentGenerator.getInstance(requireContext())
        pointsRewardsService = PointsRewardsService.getInstance(requireContext())
        auth = FirebaseAuth.getInstance()
        
        setupGame()
        startGame()
    }

    private fun setupGame() {
        setupUI()
        setupClickListeners()
        initializeGame()
    }
    
    private fun setupUI() {
        binding.tvDifficulty.text = "Difficulty: ${difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}"
        binding.tvGameType.text = if (isWordFocused) "Word Match" else "Memory Game"
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnRestart.setOnClickListener {
            restartGame()
        }
    }

    private fun initializeGame() {
        // Limit grid to max 3x4 (12 cards = 6 pairs)
        val pairCount = when (difficulty) {
            GameDifficulty.EASY -> 3 // 6 cards (3 pairs) - 2x3 grid
            GameDifficulty.MEDIUM -> 4 // 8 cards (4 pairs) - 2x4 grid  
            GameDifficulty.HARD -> 6 // 12 cards (6 pairs) - 3x4 grid
        }
        
        val gridColumns = when (difficulty) {
            GameDifficulty.EASY -> 3 // 2x3 grid
            GameDifficulty.MEDIUM -> 4 // 2x4 grid
            GameDifficulty.HARD -> 4 // 3x4 grid
        }
        
        // Generate fallback content immediately for better reliability
        createFallbackMemoryContent(pairCount, gridColumns)
        
        // Reset game state
        matches = 0
        attempts = 0
        flippedCards.clear()
        updateStats()
    }
    
    private fun convertToMemoryCards(aiCards: List<com.example.ed.services.MemoryGameCard>): MutableList<MemoryCard> {
        return aiCards.map { aiCard ->
            MemoryCard(
                id = aiCard.id,
                pairId = aiCard.pairId,
                content = aiCard.text,
                isFlipped = aiCard.isFlipped,
                isMatched = aiCard.isMatched
            )
        }.toMutableList()
    }
    
    private fun convertToAIMemoryCard(memoryCard: MemoryCard): com.example.ed.services.MemoryGameCard {
        return com.example.ed.services.MemoryGameCard(
            id = memoryCard.id,
            pairId = memoryCard.pairId,
            text = memoryCard.content,
            category = "Game",
            isFlipped = memoryCard.isFlipped,
            isMatched = memoryCard.isMatched
        )
    }
    
    private fun createFallbackMemoryContent(pairCount: Int, gridColumns: Int) {
        aiMemoryCards.clear()
        
        val fallbackPairs = if (isWordFocused) {
            listOf(
                Pair("Cat", "🐱"),
                Pair("Dog", "🐶"),
                Pair("Sun", "☀️"),
                Pair("Moon", "🌙"),
                Pair("Tree", "🌳"),
                Pair("Flower", "🌸"),
                Pair("Book", "📚"),
                Pair("Apple", "🍎")
            )
        } else {
            listOf(
                Pair("H₂O", "Water"),
                Pair("CO₂", "Carbon Dioxide"),
                Pair("2+2", "4"),
                Pair("5×3", "15"),
                Pair("√16", "4"),
                Pair("Earth", "Planet"),
                Pair("Sun", "Star"),
                Pair("Gold", "Au")
            )
        }
        
        // Create pairs based on difficulty
        fallbackPairs.take(pairCount).forEachIndexed { index, (text1, text2) ->
            aiMemoryCards.add(com.example.ed.services.MemoryGameCard(
                id = index * 2 + 1,
                pairId = index + 1,
                text = text1,
                category = "Basic",
                isFlipped = false,
                isMatched = false
            ))
            aiMemoryCards.add(com.example.ed.services.MemoryGameCard(
                id = index * 2 + 2,
                pairId = index + 1,
                text = text2,
                category = "Basic",
                isFlipped = false,
                isMatched = false
            ))
        }
        
        // Shuffle cards for random placement
        aiMemoryCards.shuffle()
        
        // Setup RecyclerView with working adapter
        binding.rvMemoryCards.layoutManager = GridLayoutManager(requireContext(), gridColumns)
        
        // Create a simple working adapter
        memoryAdapter = MemoryAdapter(convertToMemoryCards(aiMemoryCards)) { card ->
            onCardClicked(card)
        }
        binding.rvMemoryCards.adapter = memoryAdapter
        
        Log.d("MemoryGame", "Created ${aiMemoryCards.size} cards with $pairCount pairs in ${gridColumns} columns")
    }
    
    private fun onCardClicked(card: MemoryCard) {
        if (isProcessing || card.isFlipped || card.isMatched || flippedCards.size >= 2) {
            return
        }
        
        // Find the corresponding AI card
        val aiCard = aiMemoryCards.find { it.id == card.id } ?: return
        
        // Flip the card
        aiCard.isFlipped = true
        card.isFlipped = true
        flippedCards.add(aiCard)
        
        // Update adapter
        memoryAdapter?.notifyDataSetChanged()
        
        // Check for matches when 2 cards are flipped
        if (flippedCards.size == 2) {
            attempts++
            checkForMatch()
        }
        
        updateStats()
    }

    private fun getEmojiPairs(pairCount: Int): List<Pair<String, String>> {
        val emojis = listOf(
            "🐶" to "🐕", "🐱" to "🐈", "🐭" to "🐁", "🐹" to "🐿️",
            "🐰" to "🐇", "🦊" to "🦝", "🐻" to "🐼", "🐨" to "🐯",
            "🦁" to "🐮", "🐷" to "🐸", "🐵" to "🙈", "🐔" to "🐧"
        )
        return emojis.take(pairCount)
    }

    private fun getWordPairs(pairCount: Int): List<Pair<String, String>> {
        val wordPairs = listOf(
            "Cat" to "Kitten", "Dog" to "Puppy", "Sun" to "Moon", "Hot" to "Cold",
            "Big" to "Small", "Fast" to "Slow", "Up" to "Down", "Left" to "Right",
            "Day" to "Night", "Fire" to "Water", "Happy" to "Sad", "Good" to "Bad"
        )
        return wordPairs.take(pairCount)
    }

    private fun checkForMatch() {
        if (flippedCards.size != 2) return
        
        val card1 = flippedCards[0]
        val card2 = flippedCards[1]
        
        isProcessing = true
        
        if (card1.pairId == card2.pairId) {
            // Match found!
            Handler(Looper.getMainLooper()).postDelayed({
                card1.isMatched = true
                card2.isMatched = true
                matches++
                flippedCards.clear()
                isProcessing = false
                
                // Update both AI cards and memory cards
                updateCardStates()
                memoryAdapter?.notifyDataSetChanged()
                updateStats()
                
                // Check if game is complete
                if (matches == aiMemoryCards.size / 2) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        finishGame()
                    }, 500)
                }
            }, 800) // Show match for a bit longer
        } else {
            // No match - flip cards back
            Handler(Looper.getMainLooper()).postDelayed({
                card1.isFlipped = false
                card2.isFlipped = false
                flippedCards.clear()
                isProcessing = false
                
                // Update both AI cards and memory cards
                updateCardStates()
                memoryAdapter?.notifyDataSetChanged()
            }, 1500) // Give time to see both cards
        }
    }
    
    private fun updateCardStates() {
        // Sync AI cards with memory cards in adapter
        memoryAdapter?.let { adapter ->
            for (i in 0 until adapter.itemCount) {
                val memoryCard = adapter.cards[i]
                val aiCard = aiMemoryCards.find { it.id == memoryCard.id }
                if (aiCard != null) {
                    memoryCard.isFlipped = aiCard.isFlipped
                    memoryCard.isMatched = aiCard.isMatched
                }
            }
        }
    }

    private fun updateStats() {
        binding.tvMatches.text = "Matches: $matches/${aiMemoryCards.size / 2}"
        binding.tvAttempts.text = "Attempts: $attempts"
    }

    private fun restartGame() {
        gameTimer?.cancel()
        initializeGame()
        startGame()
    }

    private fun startGame() {
        startTime = System.currentTimeMillis()
        
        // Start game timer
        gameTimer = object : CountDownTimer(GAME_TIME_LIMIT, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.tvGameTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                finishGame()
            }
        }.start()
    }

    private fun finishGame() {
        gameTimer?.cancel()
        
        val endTime = System.currentTimeMillis()
        val timeSpent = endTime - startTime
        val isCompleted = matches == aiMemoryCards.size / 2
        
        // Calculate score
        val maxScore = 1000
        val score = if (isCompleted) {
            val timeBonus = maxOf(0, (GAME_TIME_LIMIT - timeSpent).toInt() / 1000)
            val efficiencyBonus = maxOf(0, (aiMemoryCards.size / 2 - attempts + aiMemoryCards.size / 2) * 50)
            maxOf(100, maxScore - (attempts * 10) + timeBonus + efficiencyBonus)
        } else {
            // Partial score based on matches found
            (matches.toDouble() / (aiMemoryCards.size / 2) * maxScore * 0.6).toInt()
        }
        
        // Award points for good performance
        val scorePercentage = (score.toFloat() / maxScore * 100).toInt()
        
        lifecycleScope.launch {
            try {
                if (isCompleted && scorePercentage >= 60) { // Award points for completion with decent score
                    val pointsToAward = when {
                        scorePercentage >= 90 -> 35 // Excellent performance
                        scorePercentage >= 80 -> 30 // Good performance  
                        scorePercentage >= 70 -> 25 // Fair performance
                        else -> 20 // Basic completion
                    }
                    
                    // Award points using PointsRewardsService
                    val success = pointsRewardsService.awardPoints(
                        com.example.ed.services.PointsType.MEMORY_GAME_WIN,
                        pointsToAward,
                        mapOf(
                            "score" to score,
                            "maxScore" to maxScore,
                            "difficulty" to difficulty.name,
                            "timeSpent" to timeSpent,
                            "matches" to matches,
                            "attempts" to attempts
                        )
                    )
                    
                    // Also save to GamificationService for UI display
                    try {
                        val gameResult = GameResult(
                            studentId = auth.currentUser?.uid ?: "",
                            gameType = if (isWordFocused) GameType.WORD_MATCH else GameType.MEMORY_GAME,
                            difficulty = difficulty,
                            score = score,
                            maxScore = maxScore,
                            timeSpent = timeSpent,
                            completed = isCompleted,
                            rewardEarned = if (success) pointsToAward.toDouble() else 0.0,
                            playedAt = System.currentTimeMillis()
                        )
                        
                        gamificationService.saveGameResult(gameResult)
                        
                        // Trigger UI refresh callback
                        onPointsUpdated?.invoke()
                        
                    } catch (e: Exception) {
                        Log.e("MemoryGame", "Error saving to gamification service", e)
                    }
                    
                    if (success) {
                        showGameResults(pointsToAward, scorePercentage, timeSpent, isCompleted)
                    } else {
                        showGameResults(0, scorePercentage, timeSpent, isCompleted)
                    }
                } else {
                    showGameResults(0, scorePercentage, timeSpent, isCompleted)
                }
            } catch (e: Exception) {
                showGameResults(0, scorePercentage, timeSpent, isCompleted)
            }
        }
    }
    
    private fun showGameResults(earnedPoints: Int, scorePercentage: Int, timeSpent: Long, completed: Boolean) {
        val minutes = timeSpent / 60000
        val seconds = (timeSpent % 60000) / 1000
        
        val resultMessage = buildString {
            if (completed) {
                appendLine("🎉 Memory Game Complete!")
            } else {
                appendLine("⏰ Time's Up!")
            }
            appendLine()
            appendLine("📊 Your Results:")
            appendLine("Matches: $matches/${aiMemoryCards.size / 2}")
            appendLine("Attempts: $attempts")
            appendLine("Accuracy: ${scorePercentage}%")
            appendLine("Time: ${minutes}m ${seconds}s")
            appendLine()
            if (earnedPoints > 0) {
                appendLine("🎉 Points Earned: +$earnedPoints")
                appendLine("Great memory skills!")
            } else if (completed) {
                appendLine("💪 Keep practicing to earn points!")
                appendLine("Score 60%+ to earn rewards!")
            } else {
                appendLine("🔄 Try again to complete the game!")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Game Results")
            .setMessage(resultMessage)
            .setPositiveButton("Play Again") { _, _ ->
                restartGame()
            }
            .setNegativeButton("Back to Games") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    fun setGameResultListener(listener: (GameResult) -> Unit) {
        gameResultListener = listener
    }
    
    fun setPointsUpdateCallback(callback: () -> Unit) {
        onPointsUpdated = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gameTimer?.cancel()
        _binding = null
    }
}

// Adapter for memory cards
class MemoryAdapter(
    val cards: List<MemoryCard>,
    private val onCardClick: (MemoryCard) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>() {

    class MemoryViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val tvCardContent: android.widget.TextView = itemView.findViewById(R.id.tv_card_content)
        val cardMemory: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.card_memory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memory_card, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val card = cards[position]
        
        if (card.isFlipped || card.isMatched) {
            holder.tvCardContent.text = card.content
            holder.cardMemory.setCardBackgroundColor(
                if (card.isMatched) 
                    holder.itemView.context.getColor(android.R.color.holo_green_light)
                else 
                    holder.itemView.context.getColor(R.color.surface_color)
            )
        } else {
            holder.tvCardContent.text = "?"
            holder.cardMemory.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.primary_color)
            )
        }
        
        holder.cardMemory.setOnClickListener {
            onCardClick(card)
        }
    }

    override fun getItemCount(): Int = cards.size
}