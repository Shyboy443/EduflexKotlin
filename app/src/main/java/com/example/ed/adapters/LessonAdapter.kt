package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.databinding.ItemLessonBinding
import com.example.ed.models.Lesson
import com.example.ed.models.LessonType

class LessonAdapter(
    private val lessons: MutableList<Lesson>,
    private val onLessonClick: (String) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(lessons[position])
    }

    override fun getItemCount(): Int = lessons.size

    fun updateLessons(newLessons: List<Lesson>) {
        lessons.clear()
        lessons.addAll(newLessons)
        notifyDataSetChanged()
    }

    inner class LessonViewHolder(
        private val binding: ItemLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lesson: Lesson) {
            binding.tvLessonTitle.text = lesson.title
            binding.tvLessonDuration.text = lesson.duration

            // Set lesson type icon
            val iconRes = when (lesson.type) {
                LessonType.VIDEO -> R.drawable.ic_play_circle
                LessonType.READING -> R.drawable.ic_book
                LessonType.QUIZ -> R.drawable.ic_quiz
                LessonType.PRACTICE -> R.drawable.ic_practice
            }
            binding.ivLessonIcon.setImageResource(iconRes)

            // Set completion status
            if (lesson.isCompleted) {
                binding.ivCompletionStatus.visibility = View.VISIBLE
                binding.ivCompletionStatus.setImageResource(R.drawable.ic_check_circle)
                binding.ivCompletionStatus.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.brand_primary)
                )
                
                // Dim completed lessons
                binding.tvLessonTitle.alpha = 0.7f
                binding.tvLessonDuration.alpha = 0.7f
                binding.ivLessonIcon.alpha = 0.7f
            } else {
                binding.ivCompletionStatus.visibility = View.GONE
                
                // Full opacity for incomplete lessons
                binding.tvLessonTitle.alpha = 1.0f
                binding.tvLessonDuration.alpha = 1.0f
                binding.ivLessonIcon.alpha = 1.0f
            }

            // Handle lesson click
            binding.root.setOnClickListener {
                onLessonClick(lesson.id)
            }
        }
    }
}