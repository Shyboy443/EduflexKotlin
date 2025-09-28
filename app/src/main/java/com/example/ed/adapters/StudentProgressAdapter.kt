package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.CourseProgress
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class StudentProgressAdapter(
    private val progressList: MutableList<CourseProgress>,
    private val onStudentClick: (CourseProgress) -> Unit
) : RecyclerView.Adapter<StudentProgressAdapter.ProgressViewHolder>() {

    private var filteredList = progressList.toList()

    fun filter(predicate: (CourseProgress) -> Boolean) {
        filteredList = progressList.filter(predicate)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount() = filteredList.size

    inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.progressCard)
        private val lessonTitle: TextView = itemView.findViewById(R.id.lessonTitle)
        private val progressIndicator: LinearProgressIndicator = itemView.findViewById(R.id.progressIndicator)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
        private val completedDate: TextView = itemView.findViewById(R.id.completedDate)
        private val timeSpent: TextView = itemView.findViewById(R.id.timeSpent)

        fun bind(courseProgress: CourseProgress) {
            lessonTitle.text = "Student: ${courseProgress.studentId.take(8)}..." // Show partial student ID
            progressIndicator.progress = courseProgress.overallProgress
            progressText.text = "${courseProgress.overallProgress}%"

            // Set status icon and card appearance
            when {
                courseProgress.overallProgress >= 100 -> {
                    statusIcon.setImageResource(R.drawable.ic_check_circle)
                    statusIcon.setColorFilter(itemView.context.getColor(android.R.color.holo_green_dark))
                    card.strokeColor = itemView.context.getColor(android.R.color.holo_green_dark)
                    card.strokeWidth = 2
                }
                courseProgress.overallProgress > 0 -> {
                    statusIcon.setImageResource(R.drawable.ic_play_circle)
                    statusIcon.setColorFilter(itemView.context.getColor(android.R.color.holo_blue_dark))
                    card.strokeColor = itemView.context.getColor(android.R.color.holo_blue_dark)
                    card.strokeWidth = 2
                }
                else -> {
                    statusIcon.setImageResource(R.drawable.ic_schedule)
                    statusIcon.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
                    card.strokeColor = itemView.context.getColor(android.R.color.darker_gray)
                    card.strokeWidth = 1
                }
            }

            // Set completion date
            if (courseProgress.certificateEarnedAt > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                completedDate.text = "Completed: ${dateFormat.format(Date(courseProgress.certificateEarnedAt))}"
                completedDate.visibility = View.VISIBLE
            } else {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                completedDate.text = "Last active: ${dateFormat.format(Date(courseProgress.lastAccessedAt))}"
                completedDate.visibility = View.VISIBLE
            }

            // Set time spent
            val timeSpentHours = courseProgress.totalTimeSpent / (1000 * 60 * 60) // Convert to hours
            val timeSpentMinutes = (courseProgress.totalTimeSpent / (1000 * 60)) % 60 // Remaining minutes
            
            timeSpent.text = when {
                timeSpentHours > 0 -> "${timeSpentHours}h ${timeSpentMinutes}m"
                timeSpentMinutes > 0 -> "${timeSpentMinutes}m"
                else -> "No time logged"
            }
            timeSpent.visibility = View.VISIBLE

            // Set click listener
            card.setOnClickListener {
                onStudentClick(courseProgress)
            }

            // Add ripple effect for better UX
            card.isClickable = true
            card.isFocusable = true
        }
    }
}