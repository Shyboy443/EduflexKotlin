package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.Question

class QuestionAdapter(
    private val questions: MutableList<Question>,
    private val onEditQuestion: (Int) -> Unit,
    private val onDeleteQuestion: (Int) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tvQuestionNumber)
        val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
        val tvQuestionType: TextView = itemView.findViewById(R.id.tvQuestionType)
        val tvPoints: TextView = itemView.findViewById(R.id.tvPoints)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]

        holder.tvQuestionNumber.text = "Q${position + 1}"
        holder.tvQuestionText.text = question.question
        holder.tvQuestionType.text = question.type.name
        holder.tvPoints.text = "${question.points} pts"

        holder.btnEdit.setOnClickListener {
            onEditQuestion(position)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteQuestion(position)
        }
    }

    override fun getItemCount(): Int = questions.size
}