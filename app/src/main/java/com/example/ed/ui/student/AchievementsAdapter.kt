package com.example.ed.ui.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.StudentAchievement
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class AchievementsAdapter(
    private val achievements: List<StudentAchievement>,
    private val onAchievementClick: (StudentAchievement) -> Unit
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardAchievement: MaterialCardView = itemView.findViewById(R.id.card_achievement)
        val tvAchievementIcon: TextView = itemView.findViewById(R.id.tv_achievement_icon)
        val tvAchievementTitle: TextView = itemView.findViewById(R.id.tv_achievement_title)
        val tvAchievementDescription: TextView = itemView.findViewById(R.id.tv_achievement_description)
        val tvAchievementReward: TextView = itemView.findViewById(R.id.tv_achievement_reward)
        val tvAchievementDate: TextView = itemView.findViewById(R.id.tv_achievement_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        
        // Set achievement icon based on type
        holder.tvAchievementIcon.text = getAchievementIcon(achievement.achievementType.name)
        holder.tvAchievementTitle.text = achievement.title
        holder.tvAchievementDescription.text = achievement.description
        holder.tvAchievementReward.text = "+$${String.format("%.2f", achievement.rewardAmount)}"
        
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        holder.tvAchievementDate.text = dateFormat.format(Date(achievement.unlockedAt))
        
        holder.cardAchievement.setOnClickListener {
            onAchievementClick(achievement)
        }
    }

    private fun getAchievementIcon(achievementType: String): String {
        return when (achievementType) {
            "FIRST_WIN" -> "🥇"
            "STREAK_3" -> "🔥"
            "STREAK_5" -> "⚡"
            "STREAK_10" -> "💫"
            "PERFECT_SCORE" -> "⭐"
            "SPEED_DEMON" -> "🚀"
            "GAME_MASTER" -> "👑"
            "QUIZ_CHAMPION" -> "🧠"
            "PUZZLE_SOLVER" -> "🧩"
            "MEMORY_EXPERT" -> "🃏"
            else -> "🏆"
        }
    }

    override fun getItemCount(): Int = achievements.size
}