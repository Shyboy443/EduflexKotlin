package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.Badge
import com.example.ed.models.BadgeCategory
import java.text.SimpleDateFormat
import java.util.*

class BadgeAdapter(
    private val onBadgeClick: (Badge) -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private var badges = listOf<Badge>()

    fun updateBadges(newBadges: List<Badge>) {
        badges = newBadges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount(): Int = badges.size

    inner class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val badgeIcon: TextView = itemView.findViewById(R.id.badgeIcon)
        private val badgeName: TextView = itemView.findViewById(R.id.badgeName)
        private val badgeDescription: TextView = itemView.findViewById(R.id.badgeDescription)
        private val badgeCategory: TextView = itemView.findViewById(R.id.badgeCategory)
        private val earnedDate: TextView = itemView.findViewById(R.id.earnedDate)

        fun bind(badge: Badge) {
            badgeIcon.text = badge.iconUrl
            badgeName.text = badge.name
            badgeDescription.text = badge.description
            badgeCategory.text = badge.category.name.lowercase().replaceFirstChar { it.uppercase() }
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            earnedDate.text = "Earned ${dateFormat.format(Date(badge.earnedAt))}"

            itemView.setOnClickListener {
                onBadgeClick(badge)
            }
        }
    }
}