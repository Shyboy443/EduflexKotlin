package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.databinding.ItemCourseSectionBinding
import com.example.ed.models.CourseSection

class CourseSectionAdapter(
    private val sections: MutableList<CourseSection>,
    private val onLessonClick: (String, String) -> Unit // sectionId, lessonId
) : RecyclerView.Adapter<CourseSectionAdapter.SectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemCourseSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    fun updateSections(newSections: List<CourseSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    inner class SectionViewHolder(
        private val binding: ItemCourseSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var lessonAdapter: LessonAdapter

        fun bind(section: CourseSection) {
            binding.tvSectionTitle.text = section.title
            
            // Setup lessons adapter
            lessonAdapter = LessonAdapter(section.lessons.toMutableList()) { lessonId ->
                onLessonClick(section.id, lessonId)
            }
            
            binding.rvLessons.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = lessonAdapter
            }

            // Set initial visibility and arrow rotation
            if (section.isExpanded) {
                binding.rvLessons.visibility = View.VISIBLE
                binding.ivExpandArrow.rotation = 180f
            } else {
                binding.rvLessons.visibility = View.GONE
                binding.ivExpandArrow.rotation = 0f
            }

            // Handle section header click
            binding.layoutSectionHeader.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val currentSection = sections[position]
                    val newExpandedState = !currentSection.isExpanded
                    
                    // Update the section
                    sections[position] = currentSection.copy(isExpanded = newExpandedState)
                    
                    // Animate arrow rotation
                    val fromRotation = if (newExpandedState) 0f else 180f
                    val toRotation = if (newExpandedState) 180f else 0f
                    
                    val rotateAnimation = RotateAnimation(
                        fromRotation, toRotation,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f
                    ).apply {
                        duration = 200
                        fillAfter = true
                    }
                    
                    binding.ivExpandArrow.startAnimation(rotateAnimation)
                    
                    // Toggle visibility
                    if (newExpandedState) {
                        binding.rvLessons.visibility = View.VISIBLE
                    } else {
                        binding.rvLessons.visibility = View.GONE
                    }
                }
            }
        }
    }
}