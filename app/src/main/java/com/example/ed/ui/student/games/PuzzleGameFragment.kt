package com.example.ed.ui.student.games

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ed.R
import com.example.ed.databinding.FragmentPuzzleGameBinding
import com.example.ed.models.*
import kotlin.random.Random

class PuzzleGameFragment : Fragment() {

    private var _binding: FragmentPuzzleGameBinding? = null
    private val binding get() = _binding!!
    
    private var difficulty: GameDifficulty = GameDifficulty.EASY
    private var puzzlePieces: MutableList<PuzzlePiece> = mutableListOf()
    private var puzzleAdapter: PuzzleAdapter? = null
    private var startTime = 0L
    private var gameTimer: CountDownTimer? = null
    private var moves = 0
    private var isGameCompleted = false
    
    private var gameResultListener: ((GameResult) -> Unit)? = null

    companion object {
        private const val ARG_DIFFICULTY = "difficulty"
        private const val GAME_TIME_LIMIT = 300000L // 5 minutes
        
        fun newInstance(difficulty: GameDifficulty): PuzzleGameFragment {
            val fragment = PuzzleGameFragment()
            val args = Bundle()
            args.putString(ARG_DIFFICULTY, difficulty.name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            difficulty = GameDifficulty.valueOf(it.getString(ARG_DIFFICULTY, GameDifficulty.EASY.name))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuzzleGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        initializePuzzle()
        startGame()
    }

    private fun setupUI() {
        binding.tvDifficulty.text = "Difficulty: ${difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}"
        binding.tvGameType.text = "Puzzle Game"
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnShuffle.setOnClickListener {
            shufflePuzzle()
        }
        
        binding.btnHint.setOnClickListener {
            showHint()
        }
    }

    private fun initializePuzzle() {
        val gridSize = when (difficulty) {
            GameDifficulty.EASY -> 3 // 3x3 puzzle
            GameDifficulty.MEDIUM -> 4 // 4x4 puzzle
            GameDifficulty.HARD -> 5 // 5x5 puzzle
        }
        
        // Create puzzle pieces
        puzzlePieces.clear()
        for (i in 0 until gridSize * gridSize) {
            puzzlePieces.add(
                PuzzlePiece(
                    id = i,
                    correctPosition = i,
                    currentPosition = i,
                    imageResource = "puzzle_piece_$i"
                )
            )
        }
        
        // Setup RecyclerView
        binding.rvPuzzle.layoutManager = GridLayoutManager(requireContext(), gridSize)
        puzzleAdapter = PuzzleAdapter(puzzlePieces) { piece ->
            movePiece(piece)
        }
        binding.rvPuzzle.adapter = puzzleAdapter
        
        // Shuffle the puzzle
        shufflePuzzle()
    }

    private fun shufflePuzzle() {
        // Shuffle puzzle pieces
        val shuffledPositions = puzzlePieces.indices.shuffled()
        puzzlePieces.forEachIndexed { index, piece ->
            piece.currentPosition = shuffledPositions[index]
        }
        
        // Sort by current position for display
        puzzlePieces.sortBy { it.currentPosition }
        puzzleAdapter?.notifyDataSetChanged()
        
        moves = 0
        binding.tvMoves.text = "Moves: $moves"
    }

    private fun movePiece(piece: PuzzlePiece) {
        if (isGameCompleted) return
        
        // Find empty space (last piece)
        val emptyPiece = puzzlePieces.find { it.id == puzzlePieces.size - 1 }
        if (emptyPiece == null) return
        
        // Check if pieces are adjacent
        val gridSize = when (difficulty) {
            GameDifficulty.EASY -> 3
            GameDifficulty.MEDIUM -> 4
            GameDifficulty.HARD -> 5
        }
        
        if (areAdjacent(piece.currentPosition, emptyPiece.currentPosition, gridSize)) {
            // Swap positions
            val tempPosition = piece.currentPosition
            piece.currentPosition = emptyPiece.currentPosition
            emptyPiece.currentPosition = tempPosition
            
            // Update moves
            moves++
            binding.tvMoves.text = "Moves: $moves"
            
            // Sort and update display
            puzzlePieces.sortBy { it.currentPosition }
            puzzleAdapter?.notifyDataSetChanged()
            
            // Check if puzzle is solved
            checkIfSolved()
        }
    }

    private fun areAdjacent(pos1: Int, pos2: Int, gridSize: Int): Boolean {
        val row1 = pos1 / gridSize
        val col1 = pos1 % gridSize
        val row2 = pos2 / gridSize
        val col2 = pos2 % gridSize
        
        return (kotlin.math.abs(row1 - row2) == 1 && col1 == col2) ||
               (kotlin.math.abs(col1 - col2) == 1 && row1 == row2)
    }

    private fun checkIfSolved() {
        val isSolved = puzzlePieces.all { it.id == it.currentPosition }
        
        if (isSolved && !isGameCompleted) {
            isGameCompleted = true
            finishGame()
        }
    }

    private fun showHint() {
        // Find a piece that's not in the correct position
        val wrongPiece = puzzlePieces.find { it.id != it.currentPosition && it.id != puzzlePieces.size - 1 }
        if (wrongPiece != null) {
            Toast.makeText(
                requireContext(), 
                "Piece ${wrongPiece.id + 1} should be in position ${wrongPiece.correctPosition + 1}", 
                Toast.LENGTH_SHORT
            ).show()
        }
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
                if (!isGameCompleted) {
                    finishGame()
                }
            }
        }.start()
    }

    private fun finishGame() {
        gameTimer?.cancel()
        
        val endTime = System.currentTimeMillis()
        val timeSpent = endTime - startTime
        
        // Calculate score based on completion and efficiency
        val maxScore = 1000
        val score = if (isGameCompleted) {
            val timeBonus = maxOf(0, (GAME_TIME_LIMIT - timeSpent).toInt() / 1000)
            val movesPenalty = moves * 5
            maxOf(100, maxScore - movesPenalty + timeBonus)
        } else {
            // Partial score based on correct pieces
            val correctPieces = puzzlePieces.count { it.id == it.currentPosition }
            (correctPieces.toDouble() / puzzlePieces.size * maxScore * 0.5).toInt()
        }
        
        val gameResult = GameResult(
            gameType = GameType.PUZZLE,
            difficulty = difficulty,
            score = score,
            maxScore = maxScore,
            timeSpent = timeSpent,
            completed = isGameCompleted
        )
        
        // Call result listener
        gameResultListener?.invoke(gameResult)
        
        // Return to gamification fragment
        parentFragmentManager.popBackStack()
    }

    fun setGameResultListener(listener: (GameResult) -> Unit) {
        gameResultListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gameTimer?.cancel()
        _binding = null
    }
}

// Simple adapter for puzzle pieces
class PuzzleAdapter(
    private val pieces: List<PuzzlePiece>,
    private val onPieceClick: (PuzzlePiece) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<PuzzleAdapter.PuzzleViewHolder>() {

    class PuzzleViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val tvPieceNumber: android.widget.TextView = itemView.findViewById(R.id.tv_piece_number)
        val cardPiece: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.card_piece)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuzzleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_puzzle_piece, parent, false)
        return PuzzleViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val piece = pieces[position]
        
        if (piece.id == pieces.size - 1) {
            // Empty space
            holder.tvPieceNumber.text = ""
            holder.cardPiece.setCardBackgroundColor(
                holder.itemView.context.getColor(android.R.color.transparent)
            )
        } else {
            holder.tvPieceNumber.text = (piece.id + 1).toString()
            holder.cardPiece.setCardBackgroundColor(
                if (piece.id == piece.currentPosition) 
                    holder.itemView.context.getColor(android.R.color.holo_green_light)
                else 
                    holder.itemView.context.getColor(R.color.surface_color)
            )
        }
        
        holder.cardPiece.setOnClickListener {
            onPieceClick(piece)
        }
    }

    override fun getItemCount(): Int = pieces.size
}