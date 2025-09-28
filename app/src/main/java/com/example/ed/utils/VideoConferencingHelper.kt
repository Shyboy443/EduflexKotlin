package com.example.ed.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.ed.models.LiveLecture
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class VideoConferencingHelper {
    
    companion object {
        private const val TAG = "VideoConferencingHelper"
        
        enum class Platform {
            ZOOM, GOOGLE_MEET, MICROSOFT_TEAMS, JITSI_MEET, CUSTOM
        }

        fun joinLecture(context: Context, lecture: LiveLecture, userId: String) {
            val platform = detectPlatform(lecture.meetingLink)
            
            when (platform) {
                Platform.ZOOM -> joinZoomMeeting(context, lecture.meetingLink)
                Platform.GOOGLE_MEET -> joinGoogleMeet(context, lecture.meetingLink)
                Platform.MICROSOFT_TEAMS -> joinTeamsMeeting(context, lecture.meetingLink)
                Platform.JITSI_MEET -> joinJitsiMeeting(context, lecture.meetingLink)
                Platform.CUSTOM -> joinCustomMeeting(context, lecture.meetingLink)
            }
            
            // Update participant count
            updateParticipantCount(lecture.id, userId, isJoining = true)
        }

        fun generateMeetingLink(platform: Platform, lectureId: String, lectureName: String): String {
            return when (platform) {
                Platform.ZOOM -> generateZoomLink()
                Platform.GOOGLE_MEET -> generateGoogleMeetLink()
                Platform.MICROSOFT_TEAMS -> generateTeamsLink()
                Platform.JITSI_MEET -> generateJitsiLink(lectureId, lectureName)
                Platform.CUSTOM -> generateCustomLink(lectureId)
            }
        }

        private fun detectPlatform(meetingLink: String): Platform {
            return when {
                meetingLink.contains("zoom.us") -> Platform.ZOOM
                meetingLink.contains("meet.google.com") -> Platform.GOOGLE_MEET
                meetingLink.contains("teams.microsoft.com") -> Platform.MICROSOFT_TEAMS
                meetingLink.contains("meet.jit.si") -> Platform.JITSI_MEET
                else -> Platform.CUSTOM
            }
        }

        private fun joinZoomMeeting(context: Context, meetingLink: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                intent.setPackage("us.zoom.videomeetings")
                context.startActivity(intent)
            } catch (e: Exception) {
                // Zoom app not installed, try web browser
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                    context.startActivity(webIntent)
                } catch (e2: Exception) {
                    showAppDownloadDialog(context, "Zoom", "us.zoom.videomeetings")
                }
            }
        }

        private fun joinGoogleMeet(context: Context, meetingLink: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                intent.setPackage("com.google.android.apps.meetings")
                context.startActivity(intent)
            } catch (e: Exception) {
                // Google Meet app not installed, try web browser
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                    context.startActivity(webIntent)
                } catch (e2: Exception) {
                    showAppDownloadDialog(context, "Google Meet", "com.google.android.apps.meetings")
                }
            }
        }

        private fun joinTeamsMeeting(context: Context, meetingLink: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                intent.setPackage("com.microsoft.teams")
                context.startActivity(intent)
            } catch (e: Exception) {
                // Teams app not installed, try web browser
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                    context.startActivity(webIntent)
                } catch (e2: Exception) {
                    showAppDownloadDialog(context, "Microsoft Teams", "com.microsoft.teams")
                }
            }
        }

        private fun joinJitsiMeeting(context: Context, meetingLink: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                intent.setPackage("org.jitsi.meet")
                context.startActivity(intent)
            } catch (e: Exception) {
                // Jitsi Meet app not installed, try web browser
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                    context.startActivity(webIntent)
                } catch (e2: Exception) {
                    showAppDownloadDialog(context, "Jitsi Meet", "org.jitsi.meet")
                }
            }
        }

        private fun joinCustomMeeting(context: Context, meetingLink: String) {
            openInBrowser(context, meetingLink)
        }

        private fun openInBrowser(context: Context, url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open meeting link", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error opening meeting link", e)
            }
        }

        private fun showAppDownloadDialog(context: Context, appName: String, packageName: String) {
            AlertDialog.Builder(context)
                .setTitle("Install $appName")
                .setMessage("$appName is required to join this meeting. Would you like to install it?")
                .setPositiveButton("Install") { _, _ ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                        context.startActivity(intent)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun generateJitsiLink(lectureId: String, lectureName: String): String {
            val roomName = lectureName.replace(" ", "-").lowercase() + "-" + lectureId.substring(0, 8)
            return "https://meet.jit.si/$roomName"
        }

        private fun generateGoogleMeetLink(): String {
            // This would typically require Google Meet API integration
            val meetingId = UUID.randomUUID().toString().substring(0, 10)
            return "https://meet.google.com/$meetingId"
        }

        private fun generateZoomLink(): String {
            // This would typically require Zoom API integration
            val meetingId = (100000000..999999999).random()
            return "https://zoom.us/j/$meetingId"
        }

        private fun generateTeamsLink(): String {
            // This would typically require Microsoft Teams API integration
            val meetingId = UUID.randomUUID().toString()
            return "https://teams.microsoft.com/l/meetup-join/$meetingId"
        }

        private fun generateCustomLink(lectureId: String): String {
            return "https://meet.edtech.com/room/${lectureId.substring(0, 8)}"
        }

        private fun updateParticipantCount(lectureId: String, userId: String, isJoining: Boolean) {
            try {
                val db = FirebaseFirestore.getInstance()
                val lectureRef = db.collection("live_lectures").document(lectureId)
                
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(lectureRef)
                    val currentParticipants = snapshot.getLong("currentParticipants") ?: 0
                    val participantsList = snapshot.get("participants") as? MutableList<String> ?: mutableListOf()
                    
                    if (isJoining) {
                        if (!participantsList.contains(userId)) {
                            participantsList.add(userId)
                            transaction.update(lectureRef, "currentParticipants", currentParticipants + 1)
                            transaction.update(lectureRef, "participants", participantsList)
                        }
                    } else {
                        if (participantsList.contains(userId)) {
                            participantsList.remove(userId)
                            transaction.update(lectureRef, "currentParticipants", maxOf(0, currentParticipants - 1))
                            transaction.update(lectureRef, "participants", participantsList)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating participant count", e)
            }
        }

        fun leaveLecture(lectureId: String, userId: String) {
            updateParticipantCount(lectureId, userId, isJoining = false)
        }

        fun startLectureRecording(lectureId: String) {
            try {
                val db = FirebaseFirestore.getInstance()
                val updates = mapOf(
                    "isRecording" to true,
                    "recordingStartTime" to Date()
                )
                
                db.collection("live_lectures")
                    .document(lectureId)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Recording started for lecture: $lectureId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error starting recording", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting lecture recording", e)
            }
        }

        fun stopLectureRecording(lectureId: String, recordingUrl: String? = null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val updates = mutableMapOf<String, Any>(
                    "isRecording" to false,
                    "recordingEndTime" to Date()
                )
                
                recordingUrl?.let { url ->
                    updates["recordingUrl"] = url
                }
                
                db.collection("live_lectures")
                    .document(lectureId)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Recording stopped for lecture: $lectureId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error stopping recording", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping lecture recording", e)
            }
        }
    }
}
