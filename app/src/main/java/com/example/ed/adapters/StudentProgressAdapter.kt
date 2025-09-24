package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.LessonProgressUI
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class StudentProgressAdapter(
    private val onLessonClick: (LessonProgressUI) -> Unit
) : RecyclerView.Adapter<StudentProgressAdapter.ProgressViewHolder>() {

    private var progressList = listOf<LessonProgressUI>()

    fun updateProgress(newProgressList: List<LessonProgressUI>) {
        progressList = newProgressList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(progressList[position])
    }

    override fun getItemCount() = progressList.size

    inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.progressCard)
        private val lessonTitle: TextView = itemView.findViewById(R.id.lessonTitle)
        private val progressIndicator: LinearProgressIndicator = itemView.findViewById(R.id.progressIndicator)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
        private val completedDate: TextView = itemView.findViewById(R.id.completedDate)
        private val timeSpent: TextView = itemView.findViewById(R.id.timeSpent)

        fun bind(lessonProgress: LessonProgressUI) {
            lessonTitle.text = lessonProgress.title
            progressIndicator.progress = lessonProgress.completionPercentage
            progressText.text = "${lessonProgress.completionPercentage}%"

            // Set status icon and card appearance
            when {
                lessonProgress.isCompleted -> {
                    statusIcon.setImageResource(R.drawable.ic_check_circle)
                    statusIcon.setColorFilter(itemView.context.getColor(R.color.green_accent))
                    card.strokeColor = itemView.context.getColor(R.color.green_accent)
                    card.strokeWidth = 2
                }
                lessonProgress.completionPercentage > 0 -> {
                    statusIcon.setImageResource(R.drawable.ic_play_circle)
                    statusIcon.setColorFilter(itemView.context.getColor(R.color.blue_accent))
                    card.strokeColor = itemView.context.getColor(R.color.blue_accent)
                    card.strokeWidth = 2
                }
                else -> {
                    statusIcon.setImageResource(R.drawable.ic_lock)
                    statusIcon.setColorFilter(itemView.context.getColor(R.color.text_secondary))
                    card.strokeColor = itemView.context.getColor(R.color.divider_color)
                    card.strokeWidth = 1
                }
            }

            // Set completion date
            if (lessonProgress.completedAt != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                completedDate.text = "Completed: ${dateFormat.format(Date(lessonProgress.completedAt))}"
                completedDate.visibility = View.VISIBLE
            } else {
                completedDate.visibility = View.GONE
            }

            // Set time spent (mock data for now)
            val timeSpentMinutes = when {
                lessonProgress.isCompleted -> (45..120).random()
                lessonProgress.completionPercentage > 0 -> (15..60).random()
                else -> 0
            }
            
            if (timeSpentMinutes > 0) {
                timeSpent.text = "${timeSpentMinutes} min"
                timeSpent.visibility = View.VISIBLE
            } else {
                timeSpent.visibility = View.GONE
            }

            // Set click listener
            card.setOnClickListener {
                onLessonClick(lessonProgress)
            }

            // Add ripple effect for better UX
            card.isClickable = true
            card.isFocusable = true
        }
    }
}