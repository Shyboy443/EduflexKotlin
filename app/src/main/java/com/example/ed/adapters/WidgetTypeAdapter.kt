package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.WidgetType
import com.google.android.material.card.MaterialCardView

class WidgetTypeAdapter(
    private val widgetTypes: List<WidgetType>,
    private val onWidgetTypeClick: (WidgetType) -> Unit
) : RecyclerView.Adapter<WidgetTypeAdapter.WidgetTypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_widget_type, parent, false)
        return WidgetTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: WidgetTypeViewHolder, position: Int) {
        holder.bind(widgetTypes[position])
    }

    override fun getItemCount(): Int = widgetTypes.size

    inner class WidgetTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)

        fun bind(widgetType: WidgetType) {
            titleTextView.text = getWidgetTypeTitle(widgetType)
            descriptionTextView.text = getWidgetTypeDescription(widgetType)
            iconImageView.setImageResource(getWidgetTypeIcon(widgetType))

            cardView.setOnClickListener {
                onWidgetTypeClick(widgetType)
            }
        }

        private fun getWidgetTypeTitle(widgetType: WidgetType): String {
            return when (widgetType) {
                WidgetType.RECENT_COURSES -> "Recent Courses"
                WidgetType.UPCOMING_ASSIGNMENTS -> "Upcoming Assignments"
                WidgetType.GRADE_DISTRIBUTION -> "Grade Distribution"
                WidgetType.STUDENT_ACTIVITY -> "Student Activity"
                WidgetType.RECENT_SUBMISSIONS -> "Recent Submissions"
                WidgetType.ANNOUNCEMENTS -> "Announcements"
                WidgetType.CALENDAR -> "Calendar"
                WidgetType.QUICK_ACTIONS -> "Quick Actions"
            }
        }

        private fun getWidgetTypeDescription(widgetType: WidgetType): String {
            return when (widgetType) {
                WidgetType.RECENT_COURSES -> "View your recently accessed courses"
                WidgetType.UPCOMING_ASSIGNMENTS -> "See assignments due soon"
                WidgetType.GRADE_DISTRIBUTION -> "View grade statistics"
                WidgetType.STUDENT_ACTIVITY -> "Monitor student engagement"
                WidgetType.RECENT_SUBMISSIONS -> "Check latest submissions"
                WidgetType.ANNOUNCEMENTS -> "Important announcements"
                WidgetType.CALENDAR -> "Upcoming events and deadlines"
                WidgetType.QUICK_ACTIONS -> "Frequently used actions"
            }
        }

        private fun getWidgetTypeIcon(widgetType: WidgetType): Int {
            return when (widgetType) {
                WidgetType.RECENT_COURSES -> R.drawable.ic_book
                WidgetType.UPCOMING_ASSIGNMENTS -> R.drawable.ic_assignment
                WidgetType.GRADE_DISTRIBUTION -> R.drawable.ic_analytics
                WidgetType.STUDENT_ACTIVITY -> R.drawable.ic_people
                WidgetType.RECENT_SUBMISSIONS -> R.drawable.ic_assessment
                WidgetType.ANNOUNCEMENTS -> R.drawable.ic_notification
                WidgetType.CALENDAR -> R.drawable.ic_schedule
                WidgetType.QUICK_ACTIONS -> R.drawable.ic_dashboard
            }
        }
    }
}