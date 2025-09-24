package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.RecentActivity
import com.example.ed.models.ActivityType
import java.text.SimpleDateFormat
import java.util.*

class RecentActivityAdapter(
    private var activities: List<RecentActivity>,
    private val onItemClick: (RecentActivity) -> Unit
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivActivityIcon: ImageView = itemView.findViewById(R.id.iv_activity_icon)
        val tvActivityDescription: TextView = itemView.findViewById(R.id.tv_activity_description)
        val tvActivityTime: TextView = itemView.findViewById(R.id.tv_activity_time)
        val tvStudentName: TextView = itemView.findViewById(R.id.tv_student_name)
        val tvCourseName: TextView = itemView.findViewById(R.id.tv_course_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.tvActivityDescription.text = activity.description
        holder.tvStudentName.text = activity.studentName
        holder.tvCourseName.text = activity.courseName
        holder.tvActivityTime.text = dateFormat.format(Date(activity.timestamp))
        
        // Set appropriate icon based on activity type
        val iconRes = when (activity.type) {
            ActivityType.LESSON_COMPLETED -> R.drawable.ic_check_circle
            ActivityType.QUIZ_SUBMITTED -> R.drawable.ic_quiz
            ActivityType.COURSE_ENROLLED -> R.drawable.ic_person_add
            ActivityType.DISCUSSION_POSTED -> R.drawable.ic_chat
            ActivityType.ASSIGNMENT_SUBMITTED -> R.drawable.ic_assignment
            ActivityType.COURSE_COMPLETED -> R.drawable.ic_trophy
        }
        holder.ivActivityIcon.setImageResource(iconRes)
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(activity)
        }
    }

    override fun getItemCount(): Int = activities.size

    fun updateActivities(newActivities: List<RecentActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}