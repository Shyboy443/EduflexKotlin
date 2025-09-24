package com.example.ed.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.AppNotification
import com.example.ed.models.NotificationType
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onNotificationClick: (AppNotification) -> Unit,
    private val onNotificationAction: (AppNotification, String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notifications = listOf<AppNotification>()

    fun updateNotifications(newNotifications: List<AppNotification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
        private val actionButton: MaterialButton = itemView.findViewById(R.id.actionButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(notification: AppNotification) {
            titleTextView.text = notification.title
            messageTextView.text = notification.message
            timeTextView.text = getRelativeTimeSpan(notification.createdAt)

            // Set notification icon based on type
            val iconRes = when (notification.type) {
                NotificationType.ASSIGNMENT_DUE -> R.drawable.ic_assignment
                NotificationType.GRADE_POSTED -> R.drawable.ic_grade
                NotificationType.DISCUSSION_REPLY -> R.drawable.ic_forum
                NotificationType.COURSE_UPDATE -> R.drawable.ic_school
                NotificationType.SYSTEM -> R.drawable.ic_info
                NotificationType.REMINDER -> R.drawable.ic_alarm
            }
            iconImageView.setImageResource(iconRes)

            // Set icon color based on type
            val iconColor = when (notification.type) {
                NotificationType.ASSIGNMENT_DUE -> R.color.warning
                NotificationType.GRADE_POSTED -> R.color.success
                NotificationType.DISCUSSION_REPLY -> R.color.primary
                NotificationType.COURSE_UPDATE -> R.color.info
                NotificationType.SYSTEM -> R.color.text_secondary
                NotificationType.REMINDER -> R.color.accent
            }
            iconImageView.setColorFilter(ContextCompat.getColor(itemView.context, iconColor))

            // Show/hide unread indicator
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Set card appearance based on read status
            if (notification.isRead) {
                cardView.alpha = 0.7f
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.divider)
            } else {
                cardView.alpha = 1.0f
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary)
                cardView.strokeWidth = 2
            }

            // Setup action button based on notification type
            setupActionButton(notification)

            // Set click listeners
            cardView.setOnClickListener {
                onNotificationClick(notification)
            }

            deleteButton.setOnClickListener {
                onNotificationAction(notification, "delete")
            }
        }

        private fun setupActionButton(notification: AppNotification) {
            when (notification.type) {
                NotificationType.ASSIGNMENT_DUE -> {
                    actionButton.text = "View Assignment"
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener {
                        onNotificationClick(notification)
                    }
                }
                NotificationType.GRADE_POSTED -> {
                    actionButton.text = "View Grade"
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener {
                        onNotificationClick(notification)
                    }
                }
                NotificationType.DISCUSSION_REPLY -> {
                    actionButton.text = "Reply"
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener {
                        onNotificationClick(notification)
                    }
                }
                NotificationType.COURSE_UPDATE -> {
                    actionButton.text = "View Course"
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener {
                        onNotificationClick(notification)
                    }
                }
                NotificationType.REMINDER -> {
                    actionButton.text = "Snooze"
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener {
                        onNotificationAction(notification, "snooze")
                    }
                }
                else -> {
                    actionButton.visibility = View.GONE
                }
            }
        }

        private fun getRelativeTimeSpan(date: Date): String {
            val now = System.currentTimeMillis()
            val time = date.time
            
            return when {
                now - time < DateUtils.MINUTE_IN_MILLIS -> "Just now"
                now - time < DateUtils.HOUR_IN_MILLIS -> {
                    val minutes = (now - time) / DateUtils.MINUTE_IN_MILLIS
                    "${minutes}m ago"
                }
                now - time < DateUtils.DAY_IN_MILLIS -> {
                    val hours = (now - time) / DateUtils.HOUR_IN_MILLIS
                    "${hours}h ago"
                }
                now - time < DateUtils.WEEK_IN_MILLIS -> {
                    val days = (now - time) / DateUtils.DAY_IN_MILLIS
                    "${days}d ago"
                }
                else -> {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
            }
        }
    }
}