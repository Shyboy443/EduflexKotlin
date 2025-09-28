package com.example.ed.utils

import android.content.Context
import android.util.Log
import com.example.ed.models.LiveLecture
import com.example.ed.models.Status
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.*

object LiveLectureSeeder {
    
    private const val TAG = "LiveLectureSeeder"
    
    suspend fun seedSampleLiveLectures(context: Context): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Sample live lectures with different statuses
            val sampleLectures = createSampleLectures()
            
            // Add each lecture to Firestore
            sampleLectures.forEach { lecture ->
                firestore.collection("live_lectures")
                    .document(lecture.id)
                    .set(lecture)
                    .await()
                
                Log.d(TAG, "Added live lecture: ${lecture.title}")
            }
            
            Log.d(TAG, "Successfully seeded ${sampleLectures.size} live lectures")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed live lectures", e)
            false
        }
    }
    
    private fun createSampleLectures(): List<LiveLecture> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return listOf(
            // Live lecture happening now
            LiveLecture().apply {
                id = "live_lecture_1"
                courseId = "course_1" // Android Development course
                courseName = "Android Development Masterclass"
                instructorName = "Dr. Sarah Johnson"
                instructorId = "instructor_1"
                title = "Advanced RecyclerView Techniques"
                description = "Learn advanced RecyclerView patterns, custom animations, and performance optimization techniques for complex lists."
                scheduledTime = Timestamp(Date(currentTime - (10 * 60 * 1000))) // Started 10 minutes ago
                duration = 90 // 90 minutes
                meetingLink = "https://meet.google.com/abc-defg-hij"
                status = Status.LIVE
                statusString = "live"
                isRecorded = true
                maxParticipants = 50
                currentParticipants = 23
                tags = listOf("android", "recyclerview", "advanced", "ui")
            },
            
            // Upcoming lecture in 15 minutes
            LiveLecture().apply {
                id = "live_lecture_2"
                courseId = "course_2" // Web Development course
                courseName = "Full Stack Web Development"
                instructorName = "Prof. Michael Chen"
                instructorId = "instructor_2"
                title = "React Hooks Deep Dive"
                description = "Comprehensive exploration of React Hooks including useState, useEffect, useContext, and custom hooks."
                scheduledTime = Timestamp(Date(currentTime + (15 * 60 * 1000))) // In 15 minutes
                duration = 60
                meetingLink = "https://zoom.us/j/123456789"
                status = Status.SCHEDULED
                statusString = "scheduled"
                isRecorded = true
                maxParticipants = 100
                currentParticipants = 0
                tags = listOf("react", "javascript", "hooks", "frontend")
            },
            
            // Upcoming lecture in 2 hours
            LiveLecture().apply {
                id = "live_lecture_3"
                courseId = "course_1" // Same Android course
                courseName = "Android Development Masterclass"
                instructorName = "Dr. Sarah Johnson"
                instructorId = "instructor_1"
                title = "Kotlin Coroutines and Flow"
                description = "Master asynchronous programming in Android with Kotlin Coroutines and Flow for reactive programming."
                scheduledTime = Timestamp(Date(currentTime + (2 * 60 * 60 * 1000))) // In 2 hours
                duration = 75
                meetingLink = "https://meet.google.com/xyz-uvwx-rst"
                status = Status.SCHEDULED
                statusString = "scheduled"
                isRecorded = true
                maxParticipants = 75
                currentParticipants = 0
                tags = listOf("kotlin", "coroutines", "flow", "async")
            },
            
            // Tomorrow's lecture
            LiveLecture().apply {
                id = "live_lecture_4"
                courseId = "course_3" // Data Science course
                courseName = "Data Science with Python"
                instructorName = "Dr. Emily Rodriguez"
                instructorId = "instructor_3"
                title = "Machine Learning Fundamentals"
                description = "Introduction to machine learning concepts, algorithms, and practical implementation using scikit-learn."
                scheduledTime = Timestamp(Date(currentTime + (24 * 60 * 60 * 1000))) // Tomorrow
                duration = 120 // 2 hours
                meetingLink = "https://teams.microsoft.com/l/meetup-join/abc123"
                status = Status.SCHEDULED
                statusString = "scheduled"
                isRecorded = true
                maxParticipants = 200
                currentParticipants = 0
                tags = listOf("python", "machine-learning", "data-science", "ai")
            },
            
            // Past lecture (ended)
            LiveLecture().apply {
                id = "live_lecture_5"
                courseId = "course_2" // Web Development
                courseName = "Full Stack Web Development"
                instructorName = "Prof. Michael Chen"
                instructorId = "instructor_2"
                title = "Node.js and Express Fundamentals"
                description = "Building RESTful APIs with Node.js and Express framework, including middleware and error handling."
                scheduledTime = Timestamp(Date(currentTime - (3 * 60 * 60 * 1000))) // 3 hours ago
                duration = 90
                meetingLink = "https://zoom.us/j/987654321"
                status = Status.ENDED
                statusString = "ended"
                isRecorded = true
                maxParticipants = 80
                currentParticipants = 45
                tags = listOf("nodejs", "express", "backend", "api")
            }
        )
    }
    
    suspend fun clearLiveLectures(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Get all live lectures
            val lectures = firestore.collection("live_lectures")
                .get()
                .await()
            
            // Delete each lecture
            lectures.documents.forEach { document ->
                document.reference.delete().await()
            }
            
            Log.d(TAG, "Cleared ${lectures.size()} live lectures")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear live lectures", e)
            false
        }
    }
}
