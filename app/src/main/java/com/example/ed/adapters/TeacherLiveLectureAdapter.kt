package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.ed.R
import com.example.ed.models.LiveLecture
import com.example.ed.models.Status
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class TeacherLiveLectureAdapter(
    private val onEditClick: (LiveLecture) -> Unit,
    private val onStartClick: (LiveLecture) -> Unit
) : RecyclerView.Adapter<TeacherLiveLectureAdapter.LectureViewHolder>() {

    private val lectures = mutableListOf<LiveLecture>()
    private var selectedPosition = -1

    fun updateLectures(newLectures: List<LiveLecture>) {
        lectures.clear()
        lectures.addAll(newLectures)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LectureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher_live_lecture, parent, false)
        return LectureViewHolder(view)
    }

    override fun onBindViewHolder(holder: LectureViewHolder, position: Int) {
        val lecture = lectures[position]
        holder.bind(lecture)
        
        // Highlight selected item
        holder.itemView.isSelected = selectedPosition == position
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onStartClick(lecture)
        }
    }

    override fun getItemCount() = lectures.size

    inner class LectureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_lecture)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_lecture_title)
        private val tvCourse: TextView = itemView.findViewById(R.id.tv_course_name)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_lecture_datetime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_lecture_status)
        private val tvTimeUntil: TextView = itemView.findViewById(R.id.tv_time_until)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnStartLecture: MaterialButton = itemView.findViewById(R.id.btn_start_lecture)
        private val ivStatusIndicator: View = itemView.findViewById(R.id.iv_status_indicator)

        fun bind(lecture: LiveLecture) {
            // Set basic info
            tvTitle.text = lecture.title
            tvCourse.text = lecture.courseName.ifEmpty { "No Course" }
            tvDateTime.text = "${lecture.formattedDate} • ${lecture.formattedTime}"
            
            // Set status and time until
            when (lecture.status) {
                Status.LIVE -> {
                    tvStatus.text = lecture.statusText
                    tvStatus.setBackgroundResource(R.drawable.bg_status_live)
                    tvTimeUntil.text = "In progress"
                    
                    btnStartLecture.visibility = View.VISIBLE
                    btnStartLecture.text = "JOIN LECTURE"
                    btnStartLecture.backgroundTintList = ContextCompat.getColorStateList(
                        itemView.context,
                        R.color.status_live
                    )
                    
                    btnEdit.visibility = View.GONE
                }
                Status.SCHEDULED -> {
                    tvStatus.text = lecture.statusText
                    tvStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                    tvTimeUntil.text = lecture.timeUntilStart
                    
                    btnStartLecture.visibility = View.VISIBLE
                    btnStartLecture.text = "START LECTURE"
                    btnStartLecture.backgroundTintList = ContextCompat.getColorStateList(
                        itemView.context,
                        R.color.status_upcoming
                    )
                    
                    btnEdit.visibility = View.VISIBLE
                    btnEdit.setOnClickListener { onEditClick(lecture) }
                }
                Status.ENDED -> {
                    tvStatus.text = lecture.statusText
                    tvStatus.setBackgroundResource(R.drawable.bg_status_ended)
                    tvTimeUntil.text = ""
                    
                    btnStartLecture.visibility = if (lecture.isRecorded) View.VISIBLE else View.GONE
                    btnStartLecture.text = "VIEW RECORDING"
                    btnStartLecture.backgroundTintList = ContextCompat.getColorStateList(
                        itemView.context,
                        R.color.status_ended
                    )
                    
                    btnEdit.visibility = View.GONE
                }
            }
            
            // Set status indicator color
            ivStatusIndicator.setBackgroundResource(
                when (lecture.status) {
                    Status.LIVE -> R.drawable.bg_status_live
                    Status.SCHEDULED -> R.drawable.bg_status_upcoming
                    Status.ENDED -> R.drawable.bg_status_ended
                }
            )
            
            // Load thumbnail if available
            if (!lecture.thumbnailUrl.isNullOrEmpty()) {
                Glide.with(itemView)
                    .load(lecture.thumbnailUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(ivThumbnail)
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)
            }
            
            // Set click listeners
            btnStartLecture.setOnClickListener {
                onStartClick(lecture)
            }
            
            // Update card elevation based on selection
            val elevation = if (selectedPosition == adapterPosition) {
                8f // Selected elevation
            } else {
                4f // Normal elevation
            }
            cardView.cardElevation = elevation
        }
    }
}