package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.ed.R
import com.example.ed.databinding.ItemCourseCardBinding
import com.example.ed.models.Course
import com.example.ed.utils.PerformanceOptimizer

class CourseAdapter(
    private val courses: MutableList<Course>,
    private val onCourseClick: (Course) -> Unit,
    private val onEditClick: ((Course) -> Unit)? = null,
    private val onMenuClick: ((Course, View) -> Unit)? = null
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    // Optimized Glide request options
    private val glideOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
        .placeholder(R.drawable.ic_book)
        .error(R.drawable.ic_book)

    fun updateCourses(newCourses: List<Course>) {
        val diffCallback = CourseDiffCallback(courses, newCourses)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        courses.clear()
        courses.addAll(newCourses)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class CourseViewHolder(private val binding: ItemCourseCardBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.apply {
                // Set course title
                tvCourseTitle.text = course.title
                
                // Set course category
                tvCourseCategory.text = course.category.ifEmpty { "General" }
                
                // Set course description
                tvCourseDescription.text = course.description
                
                // Set course stats
                tvStudentsCount.text = "${course.enrolledStudents} students"
                tvRating.text = "${course.rating}"
                
                // Set course status badge
                tvStatus.text = when {
                    course.isPublished -> "Published"
                    else -> "Draft"
                }
                
                // Set status badge color
                tvStatus.setBackgroundResource(
                    if (course.isPublished) R.drawable.bg_status_published 
                    else R.drawable.bg_status_draft
                )
                
                // Optimized image loading
                loadCourseImage(course.thumbnailUrl)
                
                // Set click listeners
                root.setOnClickListener { onCourseClick(course) }
                onEditClick?.let { callback ->
                    btnEdit.setOnClickListener { callback(course) }
                    btnEdit.visibility = View.VISIBLE
                } ?: run {
                    btnEdit.visibility = View.GONE
                }
                onMenuClick?.let { callback ->
                    btnMenu.setOnClickListener { callback(course, it) }
                    btnMenu.visibility = View.VISIBLE
                } ?: run {
                    btnMenu.visibility = View.GONE
                }
            }
        }

        private fun loadCourseImage(thumbnailUrl: String) {
            if (thumbnailUrl.isNotEmpty()) {
                // Use PerformanceOptimizer for optimized image loading
                PerformanceOptimizer.loadImageOptimized(
                    imageView = binding.ivCourseImage,
                    imagePath = thumbnailUrl,
                    placeholder = R.drawable.ic_book,
                    targetWidth = 200,
                    targetHeight = 150
                )
            } else {
                binding.ivCourseImage.setImageResource(R.drawable.ic_book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseCardBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun onViewRecycled(holder: CourseViewHolder) {
        super.onViewRecycled(holder)
        // Clear image to prevent memory leaks
        val imageView = holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_course_image)
        imageView?.let { Glide.with(holder.itemView.context).clear(it) }
    }

    override fun getItemCount(): Int = courses.size

    // DiffUtil callback for efficient list updates
    private class CourseDiffCallback(
        private val oldList: List<Course>,
        private val newList: List<Course>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldCourse = oldList[oldItemPosition]
            val newCourse = newList[newItemPosition]
            return oldCourse.title == newCourse.title &&
                    oldCourse.description == newCourse.description &&
                    oldCourse.enrolledStudents == newCourse.enrolledStudents &&
                    oldCourse.rating == newCourse.rating &&
                    oldCourse.isPublished == newCourse.isPublished &&
                    oldCourse.thumbnailUrl == newCourse.thumbnailUrl
        }
    }
}