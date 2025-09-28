package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.QuestionType
import com.example.ed.models.QuizQuestion
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton

class QuizQuestionAdapter(
    private val questions: List<QuizQuestion>,
    private val onAnswerSelected: (String, List<String>) -> Unit
) : RecyclerView.Adapter<QuizQuestionAdapter.QuestionViewHolder>() {

    private val answers = mutableMapOf<String, MutableList<String>>()

    init {
        // Initialize answers map
        questions.forEach { question ->
            answers[question.id] = mutableListOf()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        holder.bind(question, position + 1)
    }

    override fun getItemCount() = questions.size

    fun getAnswers(): Map<String, List<String>> {
        return answers.toMap()
    }

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardQuestion)
        private val tvQuestionNumber: TextView = itemView.findViewById(R.id.tvQuestionNumber)
        private val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvPoints)
        private val layoutOptions: LinearLayout = itemView.findViewById(R.id.layoutOptions)
        private val etShortAnswer: EditText = itemView.findViewById(R.id.etShortAnswer)

        fun bind(question: QuizQuestion, questionNumber: Int) {
            tvQuestionNumber.text = "Question $questionNumber"
            tvQuestion.text = question.question
            tvPoints.text = "${question.points} ${if (question.points == 1) "point" else "points"}"

            // Clear previous options
            layoutOptions.removeAllViews()
            layoutOptions.visibility = View.GONE
            etShortAnswer.visibility = View.GONE

            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> setupMultipleChoice(question)
                QuestionType.TRUE_FALSE -> setupTrueFalse(question)
                QuestionType.SHORT_ANSWER, QuestionType.FILL_IN_BLANK -> setupShortAnswer(question)
                QuestionType.ESSAY -> setupEssay(question)
                else -> setupMultipleChoice(question) // Default fallback
            }
        }

        private fun setupMultipleChoice(question: QuizQuestion) {
            layoutOptions.visibility = View.VISIBLE
            val radioGroup = RadioGroup(itemView.context)
            radioGroup.orientation = RadioGroup.VERTICAL

            question.options.forEachIndexed { index, option ->
                val radioButton = MaterialRadioButton(itemView.context).apply {
                    text = option
                    id = index
                    textSize = 16f
                    setPadding(16, 12, 16, 12)
                }

                radioButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        answers[question.id]?.clear()
                        answers[question.id]?.add(option)
                        onAnswerSelected(question.id, answers[question.id] ?: emptyList())
                    }
                }

                radioGroup.addView(radioButton)
            }

            layoutOptions.addView(radioGroup)
        }

        private fun setupTrueFalse(question: QuizQuestion) {
            layoutOptions.visibility = View.VISIBLE
            val radioGroup = RadioGroup(itemView.context)
            radioGroup.orientation = RadioGroup.HORIZONTAL

            val trueButton = MaterialRadioButton(itemView.context).apply {
                text = "True"
                id = 0
                textSize = 16f
                setPadding(16, 12, 16, 12)
            }

            val falseButton = MaterialRadioButton(itemView.context).apply {
                text = "False"
                id = 1
                textSize = 16f
                setPadding(16, 12, 16, 12)
            }

            trueButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    answers[question.id]?.clear()
                    answers[question.id]?.add("True")
                    onAnswerSelected(question.id, answers[question.id] ?: emptyList())
                }
            }

            falseButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    answers[question.id]?.clear()
                    answers[question.id]?.add("False")
                    onAnswerSelected(question.id, answers[question.id] ?: emptyList())
                }
            }

            radioGroup.addView(trueButton)
            radioGroup.addView(falseButton)
            layoutOptions.addView(radioGroup)
        }

        private fun setupShortAnswer(question: QuizQuestion) {
            etShortAnswer.visibility = View.VISIBLE
            etShortAnswer.hint = if (question.type == QuestionType.FILL_IN_BLANK) {
                "Fill in the blank..."
            } else {
                "Enter your answer..."
            }

            etShortAnswer.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val answer = etShortAnswer.text.toString().trim()
                    answers[question.id]?.clear()
                    if (answer.isNotEmpty()) {
                        answers[question.id]?.add(answer)
                    }
                    onAnswerSelected(question.id, answers[question.id] ?: emptyList())
                }
            }
        }

        private fun setupEssay(question: QuizQuestion) {
            etShortAnswer.visibility = View.VISIBLE
            etShortAnswer.hint = "Write your essay answer here..."
            etShortAnswer.minLines = 4
            etShortAnswer.maxLines = 10

            etShortAnswer.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val answer = etShortAnswer.text.toString().trim()
                    answers[question.id]?.clear()
                    if (answer.isNotEmpty()) {
                        answers[question.id]?.add(answer)
                    }
                    onAnswerSelected(question.id, answers[question.id] ?: emptyList())
                }
            }
        }
    }
}
