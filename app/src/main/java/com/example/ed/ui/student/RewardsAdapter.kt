package com.example.ed.ui.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.StudentReward
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class RewardsAdapter(
    private val rewards: List<StudentReward>,
    private val onRewardClick: (StudentReward) -> Unit
) : RecyclerView.Adapter<RewardsAdapter.RewardViewHolder>() {

    class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardReward: MaterialCardView = itemView.findViewById(R.id.card_reward)
        val tvRewardAmount: TextView = itemView.findViewById(R.id.tv_reward_amount)
        val tvRewardDescription: TextView = itemView.findViewById(R.id.tv_reward_description)
        val tvRewardDate: TextView = itemView.findViewById(R.id.tv_reward_date)
        val tvRewardStatus: TextView = itemView.findViewById(R.id.tv_reward_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewards[position]
        
        holder.tvRewardAmount.text = "$${String.format("%.2f", reward.discountAmount)}"
        holder.tvRewardDescription.text = reward.description
        
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        holder.tvRewardDate.text = dateFormat.format(Date(reward.earnedAt))
        
        holder.tvRewardStatus.text = if (reward.isRedeemed) "Redeemed" else "Available"
        holder.tvRewardStatus.setTextColor(
            if (reward.isRedeemed) 
                holder.itemView.context.getColor(android.R.color.darker_gray)
            else 
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
        )
        
        // Set card appearance based on status
        holder.cardReward.alpha = if (reward.isRedeemed) 0.6f else 1.0f
        
        holder.cardReward.setOnClickListener {
            onRewardClick(reward)
        }
    }

    override fun getItemCount(): Int = rewards.size
}