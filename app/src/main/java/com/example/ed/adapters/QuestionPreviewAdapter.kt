package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.Question
import com.example.ed.models.QuestionType

class QuestionPreviewAdapter(
    private var questions: MutableList<Question> = mutableListOf(),
    private val onEditClick: (Question, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<QuestionPreviewAdapter.QuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_preview, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size

    fun updateQuestions(newQuestions: List<Question>) {
        questions.clear()
        questions.addAll(newQuestions)
        notifyDataSetChanged()
    }

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionText: TextView = itemView.findViewById(R.id.questionText)
        private val questionType: TextView = itemView.findViewById(R.id.questionType)
        private val questionPoints: TextView = itemView.findViewById(R.id.questionPoints)
        private val answersLayout: LinearLayout = itemView.findViewById(R.id.answersLayout)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)

        fun bind(question: Question) {
            questionText.text = question.question
            questionType.text = question.type.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
            questionPoints.text = "${question.points} pts"

            // Clear previous answers
            answersLayout.removeAllViews()

            // Display answers based on question type
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                    question.options.forEachIndexed { index, option ->
                        val answerView = TextView(itemView.context).apply {
                            text = "• $option"
                            setTextColor(
                                if (question.correctAnswers.contains(option) || question.correctAnswers.contains(index.toString())) 
                                    android.graphics.Color.GREEN 
                                else 
                                    android.graphics.Color.BLACK
                            )
                            setPadding(16, 8, 16, 8)
                        }
                        answersLayout.addView(answerView)
                    }
                }
                else -> {
                    val answerView = TextView(itemView.context).apply {
                        text = "Answer: ${question.correctAnswers.firstOrNull() ?: "N/A"}"
                        setTextColor(android.graphics.Color.GREEN)
                        setPadding(16, 8, 16, 8)
                    }
                    answersLayout.addView(answerView)
                }
            }

            editButton.setOnClickListener {
                onEditClick(question, adapterPosition)
            }
        }
    }
}