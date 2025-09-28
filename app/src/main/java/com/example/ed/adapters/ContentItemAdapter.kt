package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.ContentItem
import com.example.ed.models.ContentType
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class ContentItemAdapter(
    private val contentItems: MutableList<ContentItem>,
    private val onEditClick: (ContentItem) -> Unit,
    private val onDeleteClick: (ContentItem) -> Unit,
    private val onMoveUpClick: (ContentItem) -> Unit,
    private val onMoveDownClick: (ContentItem) -> Unit
) : RecyclerView.Adapter<ContentItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_content_item)
        val ivContentType: ImageView = itemView.findViewById(R.id.iv_content_type)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_content_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_content_description)
        val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        val tvOrder: TextView = itemView.findViewById(R.id.tv_order)
        val ivRequired: ImageView = itemView.findViewById(R.id.iv_required)
        val ivAIGenerated: ImageView = itemView.findViewById(R.id.iv_ai_generated)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        val btnMoveUp: MaterialButton = itemView.findViewById(R.id.btn_move_up)
        val btnMoveDown: MaterialButton = itemView.findViewById(R.id.btn_move_down)
        val layoutActions: LinearLayout = itemView.findViewById(R.id.layout_actions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = contentItems[position]
        
        holder.tvTitle.text = item.title
        holder.tvDescription.text = if (item.content.length > 100) {
            "${item.content.take(100)}..."
        } else {
            item.content
        }
        
        holder.tvDuration.text = "${item.duration} min"
        holder.tvOrder.text = "${position + 1}"
        
        // Set content type icon
        val iconRes = when (item.type) {
            ContentType.TEXT -> R.drawable.ic_text_fields
            ContentType.VIDEO -> R.drawable.ic_video_library
            ContentType.AUDIO -> R.drawable.ic_volume_up
            ContentType.PRESENTATION -> R.drawable.ic_slideshow
            ContentType.DOCUMENT -> R.drawable.ic_article
            ContentType.INTERACTIVE -> R.drawable.ic_touch_app
            ContentType.LIVE_SESSION -> R.drawable.ic_live_tv
            ContentType.DISCUSSION -> R.drawable.ic_forum
            ContentType.CASE_STUDY -> R.drawable.ic_case_study
            ContentType.SIMULATION -> R.drawable.ic_smart_toy
        }
        holder.ivContentType.setImageResource(iconRes)
        
        // Required indicator
        holder.ivRequired.visibility = if (item.isRequired) View.VISIBLE else View.GONE
        
        // AI Generated indicator
        holder.ivAIGenerated.visibility = if (item.aiGenerated) View.VISIBLE else View.GONE
        
        // Move buttons visibility
        holder.btnMoveUp.visibility = if (position > 0) View.VISIBLE else View.GONE
        holder.btnMoveDown.visibility = if (position < contentItems.size - 1) View.VISIBLE else View.GONE
        
        // Click listeners
        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        holder.btnMoveUp.setOnClickListener { onMoveUpClick(item) }
        holder.btnMoveDown.setOnClickListener { onMoveDownClick(item) }
        
        // Card click to toggle actions visibility
        holder.cardView.setOnClickListener {
            val isVisible = holder.layoutActions.visibility == View.VISIBLE
            holder.layoutActions.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
        
        // Set card background based on content type
        val cardBackgroundRes = when (item.type) {
            ContentType.VIDEO -> R.drawable.card_background_video
            ContentType.INTERACTIVE -> R.drawable.card_background_interactive
            ContentType.LIVE_SESSION -> R.drawable.card_background_live
            else -> R.drawable.card_background_default
        }
        holder.cardView.background = holder.itemView.context.getDrawable(cardBackgroundRes)
    }

    override fun getItemCount(): Int = contentItems.size

    fun addItem(item: ContentItem) {
        contentItems.add(item)
        notifyItemInserted(contentItems.size - 1)
    }

    fun removeItem(item: ContentItem) {
        val index = contentItems.indexOf(item)
        if (index >= 0) {
            contentItems.removeAt(index)
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, contentItems.size)
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                contentItems[i] = contentItems.set(i + 1, contentItems[i])
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                contentItems[i] = contentItems.set(i - 1, contentItems[i])
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun updateItem(position: Int, item: ContentItem) {
        if (position >= 0 && position < contentItems.size) {
            contentItems[position] = item
            notifyItemChanged(position)
        }
    }
}
