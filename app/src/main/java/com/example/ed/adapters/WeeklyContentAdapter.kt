package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.WeeklyContent
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

class WeeklyContentAdapter(
    private var weeklyContents: MutableList<WeeklyContent>,
    private val onEditClick: (WeeklyContent) -> Unit,
    private val onDeleteClick: (WeeklyContent) -> Unit,
    private val onPublishClick: (WeeklyContent) -> Unit,
    private val onAIEnhanceClick: (WeeklyContent) -> Unit,
    private val onViewAnalyticsClick: (WeeklyContent) -> Unit
) : RecyclerView.Adapter<WeeklyContentAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_weekly_content)
        val tvWeekNumber: TextView = itemView.findViewById(R.id.tv_week_number)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_content_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_content_description)
        val tvDuration: TextView = itemView.findViewById(R.id.tv_estimated_duration)
        val tvContentItems: TextView = itemView.findViewById(R.id.tv_content_items_count)
        val tvObjectives: TextView = itemView.findViewById(R.id.tv_objectives_count)
        val chipGroupTags: ChipGroup = itemView.findViewById(R.id.chip_group_tags)
        val tvReleaseDate: TextView = itemView.findViewById(R.id.tv_release_date)
        val tvDueDate: TextView = itemView.findViewById(R.id.tv_due_date)
        val ivPublishStatus: ImageView = itemView.findViewById(R.id.iv_publish_status)
        val tvPublishStatus: TextView = itemView.findViewById(R.id.tv_publish_status)
        val ivAIEnhanced: ImageView = itemView.findViewById(R.id.iv_ai_enhanced)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        val btnPublish: MaterialButton = itemView.findViewById(R.id.btn_publish)
        val btnAIEnhance: MaterialButton = itemView.findViewById(R.id.btn_ai_enhance)
        val btnAnalytics: MaterialButton = itemView.findViewById(R.id.btn_analytics)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar_completion)
        val tvProgress: TextView = itemView.findViewById(R.id.tv_progress_percentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_content, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = weeklyContents[position]
        
        holder.tvWeekNumber.text = "Week ${content.weekNumber}"
        holder.tvTitle.text = content.title
        holder.tvDescription.text = content.description
        holder.tvDuration.text = "${content.estimatedDuration} min"
        holder.tvContentItems.text = "${content.contentItems.size} items"
        holder.tvObjectives.text = "${content.learningObjectives.size} objectives"
        
        // Setup tags
        holder.chipGroupTags.removeAllViews()
        content.tags.take(3).forEach { tag ->
            val chip = Chip(holder.itemView.context)
            chip.text = tag
            chip.isClickable = false
            holder.chipGroupTags.addView(chip)
        }
        
        if (content.tags.size > 3) {
            val moreChip = Chip(holder.itemView.context)
            moreChip.text = "+${content.tags.size - 3} more"
            moreChip.isClickable = false
            holder.chipGroupTags.addView(moreChip)
        }
        
        // Format dates
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvReleaseDate.text = if (content.releaseDate > 0) {
            "Release: ${dateFormat.format(Date(content.releaseDate))}"
        } else {
            "Release: Not set"
        }
        
        holder.tvDueDate.text = if (content.dueDate > 0) {
            "Due: ${dateFormat.format(Date(content.dueDate))}"
        } else {
            "Due: Not set"
        }
        
        // Publish status
        if (content.isPublished) {
            holder.ivPublishStatus.setImageResource(R.drawable.ic_published)
            holder.tvPublishStatus.text = "Published"
            holder.tvPublishStatus.setTextColor(holder.itemView.context.getColor(R.color.success_green))
            holder.btnPublish.text = "Unpublish"
            holder.btnPublish.setIconResource(R.drawable.ic_unpublish)
        } else {
            holder.ivPublishStatus.setImageResource(R.drawable.ic_draft)
            holder.tvPublishStatus.text = "Draft"
            holder.tvPublishStatus.setTextColor(holder.itemView.context.getColor(R.color.warning_orange))
            holder.btnPublish.text = "Publish"
            holder.btnPublish.setIconResource(R.drawable.ic_publish)
        }
        
        // AI Enhanced indicator
        holder.ivAIEnhanced.visibility = if (content.isAIEnhanced) View.VISIBLE else View.GONE
        
        // Progress calculation (mock data - you can implement actual progress tracking)
        val mockProgress = (content.weekNumber * 15) % 100
        holder.progressBar.progress = mockProgress
        holder.tvProgress.text = "$mockProgress%"
        
        // Click listeners
        holder.btnEdit.setOnClickListener { onEditClick(content) }
        holder.btnDelete.setOnClickListener { onDeleteClick(content) }
        holder.btnPublish.setOnClickListener { onPublishClick(content) }
        holder.btnAIEnhance.setOnClickListener { onAIEnhanceClick(content) }
        holder.btnAnalytics.setOnClickListener { onViewAnalyticsClick(content) }
        
        // Card click for expansion/details
        holder.cardView.setOnClickListener {
            // Toggle expanded state or show details
        }
        
        // Disable AI enhance button if already enhanced
        holder.btnAIEnhance.isEnabled = !content.isAIEnhanced
        if (content.isAIEnhanced) {
            holder.btnAIEnhance.text = "AI Enhanced"
            holder.btnAIEnhance.setIconResource(R.drawable.ic_ai_check)
        }
    }

    override fun getItemCount(): Int = weeklyContents.size

    fun updateContent(newContent: List<WeeklyContent>) {
        weeklyContents.clear()
        weeklyContents.addAll(newContent)
        notifyDataSetChanged()
    }

    fun addContent(content: WeeklyContent) {
        weeklyContents.add(content)
        notifyItemInserted(weeklyContents.size - 1)
    }

    fun removeContent(content: WeeklyContent) {
        val index = weeklyContents.indexOf(content)
        if (index >= 0) {
            weeklyContents.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateContent(content: WeeklyContent) {
        val index = weeklyContents.indexOfFirst { it.id == content.id }
        if (index >= 0) {
            weeklyContents[index] = content
            notifyItemChanged(index)
        }
    }
}
