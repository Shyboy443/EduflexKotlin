package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.LessonContent
import com.example.ed.models.LessonContentType
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class LessonContentAdapter(
    private val lessons: MutableList<LessonContent>,
    private val onLessonClick: (LessonContent) -> Unit,
    private val onEditClick: (LessonContent) -> Unit,
    private val onDeleteClick: (LessonContent) -> Unit,
    private val isTeacher: Boolean = false
) : RecyclerView.Adapter<LessonContentAdapter.LessonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson_content, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(lessons[position])
    }

    override fun getItemCount() = lessons.size

    fun updateLessons(newLessons: List<LessonContent>) {
        lessons.clear()
        lessons.addAll(newLessons)
        notifyDataSetChanged()
    }

    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardLesson)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvLessonTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvLessonDescription)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvType: TextView = itemView.findViewById(R.id.tvContentType)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val ivContentType: ImageView = itemView.findViewById(R.id.ivContentType)
        private val ivAiGenerated: ImageView = itemView.findViewById(R.id.ivAiGenerated)
        private val btnEdit: View = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: View = itemView.findViewById(R.id.btnDelete)

        fun bind(lesson: LessonContent) {
            tvTitle.text = lesson.title
            tvDescription.text = lesson.description
            tvDuration.text = "${lesson.duration} min"
            tvType.text = lesson.type.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }

            // Set content type icon
            val iconRes = when (lesson.type) {
                LessonContentType.VIDEO -> R.drawable.ic_play_circle
                LessonContentType.AUDIO -> R.drawable.ic_volume_up
                LessonContentType.DOCUMENT -> R.drawable.ic_description
                LessonContentType.PRESENTATION -> R.drawable.ic_slideshow
                LessonContentType.QUIZ -> R.drawable.ic_quiz
                LessonContentType.ASSIGNMENT -> R.drawable.ic_assignment
                LessonContentType.INTERACTIVE -> R.drawable.ic_touch_app
                else -> R.drawable.ic_article
            }
            ivContentType.setImageResource(iconRes)

            // Show AI generated indicator
            ivAiGenerated.visibility = if (lesson.aiGenerated) View.VISIBLE else View.GONE

            // Format creation date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvCreatedAt.text = "Created: ${dateFormat.format(Date(lesson.createdAt))}"

            // Show/hide teacher controls
            if (isTeacher) {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                
                btnEdit.setOnClickListener { onEditClick(lesson) }
                btnDelete.setOnClickListener { onDeleteClick(lesson) }
            } else {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }

            // Set click listener
            cardView.setOnClickListener { onLessonClick(lesson) }

            // Set card appearance based on content
            if (lesson.quiz != null) {
                cardView.strokeColor = itemView.context.getColor(android.R.color.holo_orange_dark)
                cardView.strokeWidth = 2
            } else {
                cardView.strokeColor = itemView.context.getColor(android.R.color.darker_gray)
                cardView.strokeWidth = 1
            }
        }
    }
}
