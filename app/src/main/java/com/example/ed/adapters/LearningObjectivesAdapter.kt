package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R

class LearningObjectivesAdapter(
    private val objectives: MutableList<String>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<LearningObjectivesAdapter.ObjectiveViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_learning_objective, parent, false)
        return ObjectiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObjectiveViewHolder, position: Int) {
        holder.bind(objectives[position], position)
    }

    override fun getItemCount() = objectives.size

    inner class ObjectiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvObjective: TextView = itemView.findViewById(R.id.tvObjective)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(objective: String, position: Int) {
            tvObjective.text = "• $objective"
            
            btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
