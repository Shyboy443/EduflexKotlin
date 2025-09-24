package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.Assignment
import java.text.SimpleDateFormat
import java.util.*

class AssignmentAdapter(
    private val assignments: List<Assignment>,
    private val onAssignmentClick: (Assignment) -> Unit
) : RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assignment, parent, false)
        return AssignmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssignmentViewHolder, position: Int) {
        holder.bind(assignments[position])
    }

    override fun getItemCount(): Int = assignments.size

    inner class AssignmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val pointsTextView: TextView = itemView.findViewById(R.id.pointsTextView)

        fun bind(assignment: Assignment) {
            titleTextView.text = assignment.title
            descriptionTextView.text = assignment.description
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dueDateTextView.text = "Due: ${dateFormat.format(Date(assignment.dueDate))}"
            
            pointsTextView.text = "${assignment.maxPoints} pts"

            itemView.setOnClickListener {
                onAssignmentClick(assignment)
            }
        }
    }
}