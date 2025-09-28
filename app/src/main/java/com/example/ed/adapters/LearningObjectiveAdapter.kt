package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R

class LearningObjectiveAdapter(
    private val objectives: MutableList<String>,
    private val onObjectiveAction: (String, Int) -> Unit
) : RecyclerView.Adapter<LearningObjectiveAdapter.ObjectiveViewHolder>() {

    class ObjectiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val objectiveTextView: TextView = itemView.findViewById(R.id.tvObjective)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_learning_objective, parent, false)
        return ObjectiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObjectiveViewHolder, position: Int) {
        val objective = objectives[position]
        
        holder.objectiveTextView.text = objective
        
        holder.deleteButton.setOnClickListener {
            onObjectiveAction(objective, position)
        }
    }

    override fun getItemCount(): Int = objectives.size

    fun addObjective(objective: String) {
        objectives.add(objective)
        notifyItemInserted(objectives.size - 1)
    }

    fun removeObjective(position: Int) {
        if (position >= 0 && position < objectives.size) {
            objectives.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateObjective(position: Int, objective: String) {
        if (position >= 0 && position < objectives.size) {
            objectives[position] = objective
            notifyItemChanged(position)
        }
    }
}