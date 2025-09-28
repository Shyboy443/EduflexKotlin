package com.example.ed.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.*

enum class Status {
    SCHEDULED,
    LIVE,
    ENDED
}

@IgnoreExtraProperties
data class LiveLecture(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var instructorId: String = "",
    var instructorName: String = "",
    var courseId: String = "",
    var courseName: String = "",
    var scheduledTime: Timestamp? = null,
    var scheduledDate: Date? = null,
    var duration: Int = 60, // in minutes
    var status: Status = Status.SCHEDULED,
    var statusString: String = "scheduled",
    var meetingLink: String = "",
    var recordingUrl: String = "",
    var participantIds: List<String> = listOf(),
    var maxParticipants: Int = 100,
    var currentParticipants: Int = 0,
    var thumbnailUrl: String = "",
    var isRecorded: Boolean = false,
    var timeUntilEnd: Long = 0,
    @ServerTimestamp
    var createdAt: Date? = null,
    @ServerTimestamp
    var updatedAt: Date? = null,
    var startedAt: Timestamp? = null,
    var endedAt: Timestamp? = null,
    var tags: List<String> = listOf()
) {
    // Empty constructor for Firestore
    constructor() : this(
        id = "", 
        title = "", 
        description = "", 
        instructorId = "", 
        instructorName = "", 
        courseId = "", 
        courseName = "", 
        scheduledTime = null,
        scheduledDate = null,
        duration = 60, 
        status = Status.SCHEDULED,
        statusString = "scheduled",
        meetingLink = "", 
        recordingUrl = "", 
        participantIds = listOf(), 
        maxParticipants = 100, 
        currentParticipants = 0, 
        thumbnailUrl = "",
        isRecorded = false,
        timeUntilEnd = 0,
        createdAt = null, 
        updatedAt = null, 
        startedAt = null, 
        endedAt = null,
        tags = listOf()
    )

    // Helper properties
    @get:Exclude
    val endTime: Timestamp?
        get() = when {
            isLive && startedAt != null -> {
                val endMillis = startedAt!!.toDate().time + (duration * 60 * 1000L)
                Timestamp(Date(endMillis))
            }
            scheduledTime != null -> {
                val endMillis = scheduledTime!!.toDate().time + (duration * 60 * 1000L)
                Timestamp(Date(endMillis))
            }
            else -> null
        }
    
    @get:Exclude
    val isUpcoming: Boolean
        get() = status == Status.SCHEDULED && 
                (scheduledTime?.toDate()?.time ?: 0) > System.currentTimeMillis()

    @get:Exclude
    val isLive: Boolean
        get() = status == Status.LIVE

    @get:Exclude
    val hasEnded: Boolean
        get() = status == Status.ENDED
        
    @get:Exclude
    val formattedDate: String
        get() = scheduledTime?.toDate()?.let { date ->
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        } ?: "Not scheduled"
        
    @get:Exclude
    val formattedTime: String
        get() = scheduledTime?.toDate()?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
        } ?: ""
    
    @get:Exclude
    val timeUntilStart: String
        get() {
            val now = System.currentTimeMillis()
            val lectureTime = scheduledTime?.toDate()?.time ?: return ""
            val diff = lectureTime - now
            
            return when {
                diff <= 0 -> "Now"
                diff < 60 * 60 * 1000 -> "in ${diff / (60 * 1000)} min"
                diff < 24 * 60 * 60 * 1000 -> "in ${diff / (60 * 60 * 1000)} hours"
                else -> "in ${diff / (24 * 60 * 60 * 1000)} days"
            }
        }
        
    @get:Exclude
    val statusColor: Int
        get() = when (status) {
            Status.LIVE -> android.R.color.holo_red_light
            Status.SCHEDULED -> android.R.color.holo_blue_light
            Status.ENDED -> android.R.color.darker_gray
        }
        
    @get:Exclude
    val statusText: String
        get() = when (status) {
            Status.LIVE -> "LIVE"
            Status.SCHEDULED -> "UPCOMING"
            Status.ENDED -> "ENDED"
        }
        
    @get:Exclude
    val formattedDuration: String
        get() = "${duration} min"
        
    @get:Exclude
    val isFull: Boolean
        get() = currentParticipants >= maxParticipants
}
