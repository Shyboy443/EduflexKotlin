package com.example.ed.utils

import android.content.Context
import android.util.Log
import com.example.ed.models.LiveLecture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper {
    
    companion object {
        private const val TAG = "NotificationHelper"
        
        suspend fun sendLectureScheduledNotification(
            context: Context,
            lecture: LiveLecture,
            courseId: String
        ) {
            try {
                // Get enrolled students for the course
                val enrolledStudents = getEnrolledStudents(courseId)
                
                if (enrolledStudents.isNotEmpty()) {
                    // Prepare notification data
                    val notificationData = mapOf(
                        "type" to "live_lecture_scheduled",
                        "title" to "New Live Lecture Scheduled",
                        "message" to "${lecture.instructorName} has scheduled a live lecture: ${lecture.title}",
                        "lectureId" to lecture.id,
                        "courseName" to lecture.courseName,
                        "instructorName" to lecture.instructorName,
                        "lectureTitle" to lecture.title,
                        "scheduledTime" to formatDateTime(lecture.scheduledTime),
                        "studentIds" to enrolledStudents
                    )
                    
                    // Send notification via local database (simplified approach)
                    sendNotificationToStudents(notificationData)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending lecture scheduled notification", e)
            }
        }
        
        suspend fun sendLectureStartingNotification(
            context: Context,
            lecture: LiveLecture,
            courseId: String
        ) {
            try {
                val enrolledStudents = getEnrolledStudents(courseId)
                
                if (enrolledStudents.isNotEmpty()) {
                    val notificationData = mapOf(
                        "type" to "live_lecture_starting",
                        "title" to "Live Lecture Starting Now!",
                        "message" to "${lecture.title} is starting now. Join to participate!",
                        "lectureId" to lecture.id,
                        "courseName" to lecture.courseName,
                        "instructorName" to lecture.instructorName,
                        "lectureTitle" to lecture.title,
                        "meetingLink" to lecture.meetingLink,
                        "studentIds" to enrolledStudents
                    )
                    
                    sendNotificationToStudents(notificationData)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending lecture starting notification", e)
            }
        }
        
        suspend fun sendLectureReminderNotification(
            context: Context,
            lecture: LiveLecture,
            courseId: String,
            minutesUntilStart: Long
        ) {
            try {
                val enrolledStudents = getEnrolledStudents(courseId)
                
                if (enrolledStudents.isNotEmpty()) {
                    val timeText = when {
                        minutesUntilStart <= 1 -> "in 1 minute"
                        minutesUntilStart < 60 -> "in $minutesUntilStart minutes"
                        else -> "in ${minutesUntilStart / 60} hour(s)"
                    }
                    
                    val notificationData = mapOf(
                        "type" to "live_lecture_reminder",
                        "title" to "Live Lecture Reminder",
                        "message" to "${lecture.title} starts $timeText",
                        "lectureId" to lecture.id,
                        "courseName" to lecture.courseName,
                        "instructorName" to lecture.instructorName,
                        "lectureTitle" to lecture.title,
                        "timeUntilStart" to timeText,
                        "studentIds" to enrolledStudents
                    )
                    
                    sendNotificationToStudents(notificationData)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending lecture reminder notification", e)
            }
        }
        
        suspend fun sendLectureCancelledNotification(
            context: Context,
            lecture: LiveLecture,
            courseId: String,
            reason: String = ""
        ) {
            try {
                val enrolledStudents = getEnrolledStudents(courseId)
                
                if (enrolledStudents.isNotEmpty()) {
                    val message = if (reason.isNotEmpty()) {
                        "${lecture.title} has been cancelled. Reason: $reason"
                    } else {
                        "${lecture.title} has been cancelled by the instructor."
                    }
                    
                    val notificationData = mapOf(
                        "type" to "live_lecture_cancelled",
                        "title" to "Live Lecture Cancelled",
                        "message" to message,
                        "lectureId" to lecture.id,
                        "courseName" to lecture.courseName,
                        "instructorName" to lecture.instructorName,
                        "lectureTitle" to lecture.title,
                        "reason" to reason,
                        "studentIds" to enrolledStudents
                    )
                    
                    sendNotificationToStudents(notificationData)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending lecture cancelled notification", e)
            }
        }
        
        private suspend fun getEnrolledStudents(courseId: String): List<String> {
            return try {
                val firestore = FirebaseFirestore.getInstance()
                val enrollments = firestore.collection("enrollments")
                    .whereEqualTo("courseId", courseId)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                enrollments.documents.mapNotNull { doc ->
                    doc.getString("studentId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting enrolled students", e)
                emptyList()
            }
        }
        
        private suspend fun sendNotificationToStudents(notificationData: Map<String, Any>) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val studentIds = notificationData["studentIds"] as? List<String> ?: return
                
                // Create individual notifications for each student
                studentIds.forEach { studentId ->
                    val notification = mapOf(
                        "studentId" to studentId,
                        "type" to notificationData["type"],
                        "title" to notificationData["title"],
                        "message" to notificationData["message"],
                        "lectureId" to notificationData["lectureId"],
                        "courseName" to notificationData["courseName"],
                        "instructorName" to notificationData["instructorName"],
                        "lectureTitle" to notificationData["lectureTitle"],
                        "timestamp" to Timestamp.now(),
                        "isRead" to false,
                        "data" to notificationData.filterKeys { it != "studentIds" }
                    )
                    
                    firestore.collection("notifications")
                        .add(notification)
                        .await()
                }
                
                Log.d(TAG, "Notifications sent to ${studentIds.size} students")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notifications to students", e)
            }
        }
        
        private fun formatDateTime(timestamp: Timestamp?): String {
            return try {
                if (timestamp != null) {
                    val date = timestamp.toDate()
                    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    formatter.format(date)
                } else {
                    "Not scheduled"
                }
            } catch (e: Exception) {
                "Invalid date"
            }
        }
        
        fun scheduleReminderNotifications(lecture: LiveLecture, courseId: String) {
            // This would typically use WorkManager or AlarmManager
            // For now, we'll just log the scheduling
            Log.d(TAG, "Reminder notifications scheduled for lecture: ${lecture.title}")
            
            // TODO: Implement actual scheduling with WorkManager
            // - Schedule notification 1 hour before
            // - Schedule notification 15 minutes before
            // - Schedule notification 5 minutes before
        }
        
        fun cancelScheduledNotifications(lectureId: String) {
            // Cancel any scheduled notifications for this lecture
            Log.d(TAG, "Cancelled scheduled notifications for lecture: $lectureId")
            
            // TODO: Implement actual cancellation with WorkManager
        }
    }
}
