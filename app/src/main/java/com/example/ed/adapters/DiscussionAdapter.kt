package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.Discussion
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

class DiscussionAdapter(
    private val onDiscussionClick: (Discussion) -> Unit
) : RecyclerView.Adapter<DiscussionAdapter.DiscussionViewHolder>() {

    private var discussions = listOf<Discussion>()

    fun updateDiscussions(newDiscussions: List<Discussion>) {
        discussions = newDiscussions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscussionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discussion, parent, false)
        return DiscussionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiscussionViewHolder, position: Int) {
        holder.bind(discussions[position])
    }

    override fun getItemCount() = discussions.size

    inner class DiscussionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.discussionCard)
        private val title: TextView = itemView.findViewById(R.id.discussionTitle)
        private val content: TextView = itemView.findViewById(R.id.discussionContent)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val createdDate: TextView = itemView.findViewById(R.id.createdDate)
        private val replyCount: TextView = itemView.findViewById(R.id.replyCount)
        private val upvoteCount: TextView = itemView.findViewById(R.id.upvoteCount)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
        private val tagsChipGroup: ChipGroup = itemView.findViewById(R.id.tagsChipGroup)
        private val upvoteButton: ImageView = itemView.findViewById(R.id.upvoteButton)

        fun bind(discussion: Discussion) {
            title.text = discussion.title
            content.text = discussion.content
            authorName.text = discussion.authorName
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            createdDate.text = dateFormat.format(discussion.createdAt)
            
            // Set reply count
            replyCount.text = "${discussion.replies.size} replies"
            
            // Set upvote count
            upvoteCount.text = discussion.upvotes.toString()
            
            // Set status icon based on resolution
            if (discussion.isResolved) {
                statusIcon.setImageResource(R.drawable.ic_check_circle)
                statusIcon.setColorFilter(itemView.context.getColor(R.color.green_accent))
                card.strokeColor = itemView.context.getColor(R.color.green_accent)
            } else {
                statusIcon.setImageResource(R.drawable.ic_help_outline)
                statusIcon.setColorFilter(itemView.context.getColor(R.color.blue_accent))
                card.strokeColor = itemView.context.getColor(R.color.blue_accent)
            }
            
            // Add tags as chips
            tagsChipGroup.removeAllViews()
            discussion.tags.forEach { tag ->
                val chip = Chip(itemView.context).apply {
                    text = tag
                    isClickable = false
                    setChipBackgroundColorResource(R.color.primary_light)
                    setTextColor(itemView.context.getColor(R.color.primary_color))
                }
                tagsChipGroup.addView(chip)
            }
            
            // Set click listeners
            card.setOnClickListener {
                onDiscussionClick(discussion)
            }
            
            upvoteButton.setOnClickListener {
                // Handle upvote
                handleUpvote(discussion)
            }
            
            // Truncate content if too long
            if (discussion.content.length > 150) {
                content.text = "${discussion.content.take(150)}..."
            }
        }
        
        private fun handleUpvote(discussion: Discussion) {
            // TODO: Implement upvote functionality
            upvoteCount.text = (discussion.upvotes + 1).toString()
            upvoteButton.setColorFilter(itemView.context.getColor(R.color.primary_color))
        }
    }
}