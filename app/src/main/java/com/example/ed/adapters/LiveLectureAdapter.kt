package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.ed.R
import com.example.ed.databinding.ItemLiveLectureBinding
import com.example.ed.models.LiveLecture
import com.example.ed.models.Status
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LiveLectureAdapter(
    private val lectures: MutableList<LiveLecture>,
    private val onJoinClick: (LiveLecture) -> Unit,
    private val onReminderClick: (LiveLecture) -> Unit
) : RecyclerView.Adapter<LiveLectureAdapter.LiveLectureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveLectureViewHolder {
        val binding = ItemLiveLectureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LiveLectureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LiveLectureViewHolder, position: Int) {
        val lecture = lectures[position]
        holder.bind(lecture)
        
        // Set click listeners
        holder.binding.btnJoin.setOnClickListener { onJoinClick(lecture) }
        holder.binding.btnReminder.setOnClickListener { onReminderClick(lecture) }
        holder.binding.root.setOnClickListener { onJoinClick(lecture) }
    }

    override fun getItemCount(): Int = lectures.size

    inner class LiveLectureViewHolder(
        val binding: ItemLiveLectureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lecture: LiveLecture) {
            binding.apply {
                // Basic info
                tvLectureTitle.text = lecture.title
                tvCourseName.text = lecture.courseName.ifEmpty { "No Course" }
                tvInstructorName.text = "by ${lecture.instructorName}"
                tvDescription.text = lecture.description

                // Format date and time
                tvLectureDate.text = lecture.formattedDate
                tvLectureTime.text = lecture.formattedTime
                tvDuration.text = lecture.formattedDuration

                // Set status and actions based on lecture state
                when (lecture.status) {
                    Status.LIVE -> setupLiveState(lecture)
                    Status.SCHEDULED -> setupUpcomingState(lecture)
                    Status.ENDED -> setupEndedState(lecture)
                }

                // Participant count
                tvParticipantCount.text = "${lecture.currentParticipants}/${lecture.maxParticipants}"
                
                // Progress bar for participants
                val participantProgress = if (lecture.maxParticipants > 0) {
                    (lecture.currentParticipants.toFloat() / lecture.maxParticipants * 100).toInt()
                } else 0
                progressParticipants.progress = participantProgress
                
                // Load thumbnail if available
                if (!lecture.thumbnailUrl.isNullOrEmpty()) {
                    Glide.with(itemView)
                        .load(lecture.thumbnailUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.ic_video_placeholder)
                        .error(R.drawable.ic_video_placeholder)
                        .centerCrop()
                        // .into(binding.ivThumbnail) // TODO: Add thumbnail ImageView to layout
                } else {
                    // binding.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder) // TODO: Add thumbnail ImageView to layout
                }
            }
        }

        private fun setupLiveState(lecture: LiveLecture) {
            binding.apply {
                // Status indicator
                statusIndicator.setBackgroundResource(R.drawable.bg_status_live)
                tvStatus.text = "🔴 ${lecture.statusText}"
                tvStatus.setBackgroundResource(R.drawable.bg_status_live)
                
                // Countdown to end
                val timeLeft = lecture.timeUntilEnd
                tvCountdown.text = "Ends in ${formatDuration(timeLeft)}"
                tvCountdown.visibility = View.VISIBLE
                
                // Action button
                btnJoin.text = "JOIN NOW"
                btnJoin.setBackgroundColor(itemView.context.getColor(R.color.status_live))
                btnJoin.visibility = View.VISIBLE
                
                // Hide reminder button
                btnReminder.visibility = View.GONE
                
                // Highlight card
                cardLecture.strokeWidth = 4
                cardLecture.strokeColor = itemView.context.getColor(R.color.status_live)
            }
        }

        private fun setupUpcomingState(lecture: LiveLecture) {
            binding.apply {
                // Status indicator
                statusIndicator.setBackgroundResource(R.drawable.bg_status_upcoming)
                tvStatus.text = "📅 ${lecture.statusText}"
                tvStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                
                // Countdown to start
                val timeLeft = lecture.timeUntilStart
                tvCountdown.text = "Starts $timeLeft"
                tvCountdown.visibility = View.VISIBLE
                
                // Action buttons
                btnJoin.text = "VIEW DETAILS"
                btnJoin.setBackgroundColor(itemView.context.getColor(R.color.status_upcoming))
                btnJoin.visibility = View.VISIBLE
                
                btnReminder.visibility = View.VISIBLE
                
                // Normal card appearance
                cardLecture.strokeWidth = 0
            }
        }

        private fun setupEndedState(lecture: LiveLecture) {
            binding.apply {
                // Status indicator
                statusIndicator.setBackgroundResource(R.drawable.bg_status_ended)
                tvStatus.text = "⏹️ ${lecture.statusText}"
                tvStatus.setBackgroundResource(R.drawable.bg_status_ended)
                
                // Hide countdown
                tvCountdown.visibility = View.GONE
                
                // Action button for recording (if available)
                if (lecture.isRecorded) {
                    btnJoin.text = "WATCH RECORDING"
                    btnJoin.setBackgroundColor(itemView.context.getColor(R.color.status_ended))
                    btnJoin.visibility = View.VISIBLE
                } else {
                    btnJoin.visibility = View.GONE
                }
                
                // Hide reminder button
                btnReminder.visibility = View.GONE
                
                // Dim the card
                root.alpha = 0.7f
            }
        }

        private fun formatDuration(milliseconds: Long): String {
            if (milliseconds <= 0) return "0s"
            
            val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
            
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }
}
