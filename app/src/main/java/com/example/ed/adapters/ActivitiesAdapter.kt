package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.WeeklyAssignment
import java.text.SimpleDateFormat
import java.util.*

class ActivitiesAdapter(
    private val activities: MutableList<WeeklyAssignment>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_activity_title)
        val tvType: TextView = itemView.findViewById(R.id.tv_activity_type)
        val tvDueDate: TextView = itemView.findViewById(R.id.tv_activity_due_date)
        val tvPoints: TextView = itemView.findViewById(R.id.tv_activity_points)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove_activity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.tvTitle.text = activity.title
        holder.tvType.text = activity.type.name.replace("_", " ").lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        
        if (activity.dueDate > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.tvDueDate.text = "Due: ${dateFormat.format(Date(activity.dueDate))}"
        } else {
            holder.tvDueDate.text = "No due date"
        }
        
        holder.tvPoints.text = "${activity.maxPoints} pts"
        
        holder.btnRemove.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount(): Int = activities.size

    fun addItem(activity: WeeklyAssignment) {
        activities.add(activity)
        notifyItemInserted(activities.size - 1)
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < activities.size) {
            activities.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}