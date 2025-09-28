package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.ed.R
import com.example.ed.databinding.ItemCourseCardBinding
import com.example.ed.models.Course
import com.example.ed.utils.PerformanceOptimizer
import com.example.ed.utils.ImageLoader
import com.example.ed.utils.ImageDebugHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

class CourseAdapter(
    private val courses: MutableList<Course>,
    private val enrolledCourses: List<Course>, // Pass enrolled courses to filter them out from available
    private val onCourseClick: (Course) -> Unit,
    private val onEditClick: ((Course) -> Unit)? = null,
    private val onMenuClick: ((Course, View) -> Unit)? = null,
    private val showAsEnrolled: Boolean = false, // True for enrolled courses view, false for available courses
    private val isTeacherView: Boolean = false // True for teacher course management, false for student view
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    // Coroutine scope for image loading
    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Optimized Glide request options
    private val glideOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
        .placeholder(R.drawable.ic_course_placeholder)
        .error(R.drawable.ic_course_placeholder)

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

                // Set price display - prioritize price value over isFree flag
                val isFree = course.price <= 0.0
                if (isFree) {
                    tvPrice.text = "Free"
                    tvPrice.setTextColor(root.context.getColor(R.color.success_color))
                } else {
                    tvPrice.text = "$${"%.2f".format(course.price)}"
                    tvPrice.setTextColor(root.context.getColor(R.color.text_primary))
                }

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

                // Debug: Long press to show image path info
                ivCourseImage.setOnLongClickListener {
                    ImageDebugHelper.showImagePathInfo(it.context, course.thumbnailUrl)
                    true
                }

                // Handle button visibility and text based on context
                if (isTeacherView) {
                    // For teacher view: show Edit button and Menu button, hide Enroll/View buttons
                    btnEdit.visibility = View.VISIBLE
                    btnMenu.visibility = View.VISIBLE
                    btnEnroll.visibility = View.GONE
                    btnView.visibility = View.GONE
                } else if (showAsEnrolled) {
                    // For enrolled courses: show View button, hide other buttons
                    btnView.visibility = View.VISIBLE
                    btnEnroll.visibility = View.GONE
                    btnEdit.visibility = View.GONE
                    btnMenu.visibility = View.GONE
                } else {
                    // For available courses: show appropriate button based on enrollment status
                    btnEdit.visibility = View.GONE
                    btnMenu.visibility = View.GONE
                    if (enrolledCourses.any { it.id == course.id }) {
                        // Already enrolled: show "Already Enrolled" disabled button
                        btnEnroll.text = "Already Enrolled"
                        btnEnroll.isEnabled = false
                        btnView.visibility = View.GONE
                        btnEnroll.visibility = View.VISIBLE
                    } else {
                        // Not enrolled: show "Enroll" button
                        btnEnroll.text = "Enroll"
                        btnEnroll.isEnabled = true
                        btnView.visibility = View.GONE
                        btnEnroll.visibility = View.VISIBLE
                    }
                }

                // Set click listeners
                root.setOnClickListener { onCourseClick(course) }
                btnEnroll.setOnClickListener { onCourseClick(course) }
                btnView.setOnClickListener { onCourseClick(course) }
                btnEdit.setOnClickListener { onEditClick?.invoke(course) }
                btnMenu.setOnClickListener { onMenuClick?.invoke(course, it) }
            }
        }

        private fun loadCourseImage(thumbnailUrl: String) {
            if (thumbnailUrl.isNotEmpty()) {
                if (thumbnailUrl.startsWith("/")) {
                    // Local file - use ImageLoader
                    ImageLoader.loadThumbnail(
                        context = binding.root.context,
                        imageView = binding.ivCourseImage,
                        imagePath = thumbnailUrl,
                        placeholder = R.drawable.ic_course_placeholder,
                        scope = adapterScope
                    )
                } else {
                    // URL or other - use PerformanceOptimizer
                    PerformanceOptimizer.loadImageOptimized(
                        imageView = binding.ivCourseImage,
                        imagePath = thumbnailUrl,
                        placeholder = R.drawable.ic_course_placeholder,
                        targetWidth = 200,
                        targetHeight = 150
                    )
                }
            } else {
                binding.ivCourseImage.setImageResource(R.drawable.ic_course_placeholder)
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
