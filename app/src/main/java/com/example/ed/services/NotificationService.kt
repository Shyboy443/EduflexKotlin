package com.example.ed.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ed.MainActivity
import com.example.ed.R
import com.example.ed.models.*
import java.util.*

class NotificationService {

    companion object {
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val CHANNEL_ID_ASSIGNMENTS = "assignment_notifications"
        private const val CHANNEL_ID_GRADES = "grade_notifications"
        private const val CHANNEL_ID_DISCUSSIONS = "discussion_notifications"
        
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // General notifications channel
                val generalChannel = NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General app notifications"
                    enableLights(true)
                    enableVibration(true)
                }
                
                // Assignment notifications channel
                val assignmentChannel = NotificationChannel(
                    CHANNEL_ID_ASSIGNMENTS,
                    "Assignment Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Assignment deadlines and updates"
                    enableLights(true)
                    enableVibration(true)
                }
                
                // Grade notifications channel
                val gradeChannel = NotificationChannel(
                    CHANNEL_ID_GRADES,
                    "Grade Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Grade updates and feedback"
                    enableLights(true)
                    enableVibration(false)
                }
                
                // Discussion notifications channel
                val discussionChannel = NotificationChannel(
                    CHANNEL_ID_DISCUSSIONS,
                    "Discussion Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Discussion replies and mentions"
                    enableLights(false)
                    enableVibration(false)
                }
                
                notificationManager.createNotificationChannels(listOf(
                    generalChannel, assignmentChannel, gradeChannel, discussionChannel
                ))
            }
        }
        
        fun sendLocalNotification(
            context: Context,
            type: NotificationType,
            title: String,
            message: String,
            data: Map<String, String> = emptyMap()
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val channelId = when (type) {
                NotificationType.ASSIGNMENT_DUE -> CHANNEL_ID_ASSIGNMENTS
                NotificationType.GRADE_POSTED -> CHANNEL_ID_GRADES
                NotificationType.DISCUSSION_REPLY -> CHANNEL_ID_DISCUSSIONS
                else -> CHANNEL_ID_GENERAL
            }
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(getNotificationPriority(type))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
        
        private fun getNotificationPriority(type: NotificationType): Int {
            return when (type) {
                NotificationType.ASSIGNMENT_DUE -> NotificationCompat.PRIORITY_HIGH
                NotificationType.GRADE_POSTED -> NotificationCompat.PRIORITY_DEFAULT
                NotificationType.DISCUSSION_REPLY -> NotificationCompat.PRIORITY_LOW
                else -> NotificationCompat.PRIORITY_DEFAULT
            }
        }
    }
}

// Notification scheduling service
class NotificationScheduler(private val context: Context) {
    
    fun scheduleAssignmentReminders() {
        // Mock implementation - in real app, get assignments from database
        val mockAssignments = listOf<Assignment>()
        mockAssignments.forEach { assignment ->
            scheduleAssignmentReminder(assignment)
        }
    }
    
    private fun scheduleAssignmentReminder(assignment: Assignment) {
        val reminderTime = assignment.dueDate - (24 * 60 * 60 * 1000) // 24 hours before
        
        if (reminderTime > System.currentTimeMillis()) {
            // Schedule notification using WorkManager or AlarmManager
            val title = "Assignment Due Tomorrow"
            val message = "${assignment.title} is due tomorrow at ${formatTime(Date(assignment.dueDate))}"
            
            // Mock scheduling - in real app, use WorkManager
            scheduleNotification(reminderTime, title, message, NotificationType.ASSIGNMENT_DUE)
        }
    }
    
    private fun scheduleNotification(
        triggerTime: Long,
        title: String,
        message: String,
        type: NotificationType
    ) {
        // Implementation would use WorkManager or AlarmManager
        // For now, this is a mock implementation
    }
    
    private fun formatTime(date: Date): String {
        val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun sendGradeNotification(studentId: String, assignmentTitle: String, grade: Double) {
        val title = "New Grade Posted"
        val message = "You received ${grade}% on $assignmentTitle"
        val data = mapOf(
            "studentId" to studentId,
            "assignmentTitle" to assignmentTitle,
            "grade" to grade.toString()
        )
        
        NotificationService.sendLocalNotification(
            context,
            NotificationType.GRADE_POSTED,
            title,
            message,
            data
        )
        
        // Also send FCM notification to student's device
        sendFCMNotification(studentId, title, message, data)
    }
    
    fun sendDiscussionReplyNotification(userId: String, discussionTitle: String, replyAuthor: String) {
        val title = "New Discussion Reply"
        val message = "$replyAuthor replied to \"$discussionTitle\""
        val data = mapOf(
            "userId" to userId,
            "discussionTitle" to discussionTitle,
            "replyAuthor" to replyAuthor
        )
        
        NotificationService.sendLocalNotification(
            context,
            NotificationType.DISCUSSION_REPLY,
            title,
            message,
            data
        )
        
        sendFCMNotification(userId, title, message, data)
    }
    
    private fun sendFCMNotification(
        userId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ) {
        // Mock implementation - FCM functionality removed
        // In a real implementation, this would send notifications via Firebase Admin SDK
    }
}