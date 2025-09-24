package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.CourseModule

class ModuleAdapter(
    private val modules: MutableList<CourseModule>,
    private val onModuleAction: (CourseModule, String) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tv_module_title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.tv_module_description)
        val lessonsCountTextView: TextView = itemView.findViewById(R.id.tv_lessons_count)
        val durationTextView: TextView = itemView.findViewById(R.id.tv_module_duration)
        val editButton: Button = itemView.findViewById(R.id.btn_edit_module)
        val deleteButton: Button = itemView.findViewById(R.id.btn_delete_module)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_module, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        
        holder.titleTextView.text = module.title
        holder.descriptionTextView.text = module.description
        holder.lessonsCountTextView.text = "${module.lessons.size} lessons"
        
        // Format duration from milliseconds to readable format
        val hours = module.estimatedDuration / (1000 * 60 * 60)
        val minutes = (module.estimatedDuration % (1000 * 60 * 60)) / (1000 * 60)
        holder.durationTextView.text = when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
        
        holder.editButton.setOnClickListener {
            onModuleAction(module, "edit")
        }
        
        holder.deleteButton.setOnClickListener {
            onModuleAction(module, "delete")
        }
    }

    override fun getItemCount(): Int = modules.size

    fun addModule(module: CourseModule) {
        modules.add(module)
        notifyItemInserted(modules.size - 1)
    }

    fun removeModule(position: Int) {
        if (position >= 0 && position < modules.size) {
            modules.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateModule(position: Int, module: CourseModule) {
        if (position >= 0 && position < modules.size) {
            modules[position] = module
            notifyItemChanged(position)
        }
    }
}