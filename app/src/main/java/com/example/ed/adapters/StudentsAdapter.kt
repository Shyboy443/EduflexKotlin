package com.example.ed.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ed.R
import com.example.ed.models.StudentEnrollment
import com.example.ed.models.StudentInfo
import com.example.ed.models.StudentStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
class StudentsAdapter(
    private val context: Context,
    private var students: List<StudentInfo> = emptyList(),
    private val onStudentClick: (StudentInfo) -> Unit,
    private val onViewProgressClick: (StudentInfo) -> Unit,
    private val onSendMessageClick: (StudentInfo) -> Unit
) : RecyclerView.Adapter<StudentsAdapter.StudentViewHolder>() {

    private var filteredStudents = students.toMutableList()

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardStudent: MaterialCardView = itemView.findViewById(R.id.card_student)
        private val ivStudentAvatar: ImageView = itemView.findViewById(R.id.iv_student_avatar)
        private val tvStudentName: TextView = itemView.findViewById(R.id.tv_student_name)
        private val tvStudentEmail: TextView = itemView.findViewById(R.id.tv_student_email)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvEnrolledCourses: TextView = itemView.findViewById(R.id.tv_enrolled_courses)
        private val tvProgress: TextView = itemView.findViewById(R.id.tv_progress)
        private val tvLastActive: TextView = itemView.findViewById(R.id.tv_last_active)
        private val progressOverall: ProgressBar = itemView.findViewById(R.id.progress_overall)
        private val tvProgressPercentage: TextView = itemView.findViewById(R.id.tv_progress_percentage)
        private val btnViewProgress: MaterialButton = itemView.findViewById(R.id.btn_view_progress)
        private val btnSendMessage: MaterialButton = itemView.findViewById(R.id.btn_send_message)

        fun bind(student: StudentInfo) {
            // Basic info
            tvStudentName.text = student.fullName.ifEmpty { "Unknown Student" }
            tvStudentEmail.text = student.email

            // Load profile image
            if (student.profileImageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(student.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivStudentAvatar)
            } else {
                ivStudentAvatar.setImageResource(R.drawable.ic_person)
            }

            // Status
            val status = getStudentStatus(student)
            tvStatus.text = status.name.replace("_", " ")
            tvStatus.setTextColor(getStatusColor(status))

            // Course info
            val courseCount = student.totalEnrolledCourses
            tvEnrolledCourses.text = "$courseCount Course${if (courseCount != 1) "s" else ""}"

            // Progress
            val progressPercentage = (student.averageProgress * 100).toInt()
            tvProgress.text = "$progressPercentage%"
            progressOverall.progress = progressPercentage
            tvProgressPercentage.text = "$progressPercentage%"

            // Last active
            tvLastActive.text = formatLastActive(student.lastActiveTimestamp)

            // Click listeners
            cardStudent.setOnClickListener { onStudentClick(student) }
            btnViewProgress.setOnClickListener { onViewProgressClick(student) }
            btnSendMessage.setOnClickListener { onSendMessageClick(student) }
        }

        private fun getStudentStatus(student: StudentInfo): StudentStatus {
            val daysSinceLastActive = getDaysSinceTimestamp(student.lastActiveTimestamp)
            return when {
                !student.isActive -> StudentStatus.DROPPED_OUT
                student.averageProgress >= 1.0 -> StudentStatus.COMPLETED
                daysSinceLastActive > 7 -> StudentStatus.INACTIVE
                else -> StudentStatus.ACTIVE
            }
        }

        private fun getStatusColor(status: StudentStatus): Int {
            return when (status) {
                StudentStatus.ACTIVE -> context.getColor(R.color.success)
                StudentStatus.COMPLETED -> context.getColor(R.color.primary_color)
                StudentStatus.INACTIVE -> context.getColor(R.color.warning_color)
                StudentStatus.DROPPED_OUT -> context.getColor(R.color.error_color)
            }
        }

        private fun formatLastActive(timestamp: Long): String {
            if (timestamp == 0L) return "Never"
            
            val now = System.currentTimeMillis()
            val diffMillis = now - timestamp
            
            return when {
                diffMillis < DateUtils.HOUR_IN_MILLIS -> "Just now"
                diffMillis < DateUtils.DAY_IN_MILLIS -> {
                    val hours = (diffMillis / DateUtils.HOUR_IN_MILLIS).toInt()
                    "$hours hour${if (hours != 1) "s" else ""} ago"
                }
                diffMillis < 7 * DateUtils.DAY_IN_MILLIS -> {
                    val days = (diffMillis / DateUtils.DAY_IN_MILLIS).toInt()
                    "$days day${if (days != 1) "s" else ""} ago"
                }
                else -> {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }

        private fun getDaysSinceTimestamp(timestamp: Long): Int {
            if (timestamp == 0L) return Int.MAX_VALUE
            return ((System.currentTimeMillis() - timestamp) / DateUtils.DAY_IN_MILLIS).toInt()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(filteredStudents[position])
    }

    override fun getItemCount(): Int = filteredStudents.size

    fun updateStudents(newStudents: List<StudentInfo>) {
        students = newStudents
        filteredStudents.clear()
        filteredStudents.addAll(newStudents)
        notifyDataSetChanged()
    }

    fun filterStudents(query: String, courseFilter: String? = null) {
        filteredStudents.clear()
        
        val filtered = students.filter { student ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                student.fullName.contains(query, ignoreCase = true) ||
                student.email.contains(query, ignoreCase = true)
            }
            
            val matchesCourse = if (courseFilter.isNullOrBlank() || courseFilter == "All Courses") {
                true

            } else {
                student.enrolledCourses.any { it.courseName.equals(courseFilter, ignoreCase = true) }
            }
            
            matchesQuery && matchesCourse
        }
        
        filteredStudents.addAll(filtered)
        notifyDataSetChanged()
    }

    fun sortStudents(sortBy: SortOption) {
        filteredStudents.sortWith { student1, student2 ->
            when (sortBy) {
                SortOption.NAME_ASC -> student1.fullName.compareTo(student2.fullName)
                SortOption.NAME_DESC -> student2.fullName.compareTo(student1.fullName)
                SortOption.PROGRESS_ASC -> student1.averageProgress.compareTo(student2.averageProgress)
                SortOption.PROGRESS_DESC -> student2.averageProgress.compareTo(student1.averageProgress)
                SortOption.LAST_ACTIVE_ASC -> student1.lastActiveTimestamp.compareTo(student2.lastActiveTimestamp)
                SortOption.LAST_ACTIVE_DESC -> student2.lastActiveTimestamp.compareTo(student1.lastActiveTimestamp)
                SortOption.ENROLLMENT_DATE_ASC -> student1.joinedTimestamp.compareTo(student2.joinedTimestamp)
                SortOption.ENROLLMENT_DATE_DESC -> student2.joinedTimestamp.compareTo(student1.joinedTimestamp)
            }
        }
        notifyDataSetChanged()
    }

    enum class SortOption {
        NAME_ASC,
        NAME_DESC,
        PROGRESS_ASC,
        PROGRESS_DESC,
        LAST_ACTIVE_ASC,
        LAST_ACTIVE_DESC,
        ENROLLMENT_DATE_ASC,
        ENROLLMENT_DATE_DESC
    }
}
