package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R

class PrerequisiteAdapter(
    private val prerequisites: MutableList<String>,
    private val onPrerequisiteAction: (String, Int) -> Unit
) : RecyclerView.Adapter<PrerequisiteAdapter.PrerequisiteViewHolder>() {

    class PrerequisiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val prerequisiteTextView: TextView = itemView.findViewById(R.id.tv_prerequisite_text)
        val deleteButton: Button = itemView.findViewById(R.id.btn_delete_prerequisite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrerequisiteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prerequisite, parent, false)
        return PrerequisiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrerequisiteViewHolder, position: Int) {
        val prerequisite = prerequisites[position]
        
        holder.prerequisiteTextView.text = prerequisite
        
        holder.deleteButton.setOnClickListener {
            onPrerequisiteAction(prerequisite, position)
        }
    }

    override fun getItemCount(): Int = prerequisites.size

    fun addPrerequisite(prerequisite: String) {
        prerequisites.add(prerequisite)
        notifyItemInserted(prerequisites.size - 1)
    }

    fun removePrerequisite(position: Int) {
        if (position >= 0 && position < prerequisites.size) {
            prerequisites.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updatePrerequisite(position: Int, prerequisite: String) {
        if (position >= 0 && position < prerequisites.size) {
            prerequisites[position] = prerequisite
            notifyItemChanged(position)
        }
    }
}