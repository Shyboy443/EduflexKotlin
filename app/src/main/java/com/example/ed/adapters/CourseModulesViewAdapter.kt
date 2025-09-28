package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R

class CourseModulesViewAdapter(
    private val modules: MutableList<ModuleItem>
) : RecyclerView.Adapter<CourseModulesViewAdapter.ModuleViewHolder>() {

    data class ModuleItem(
        val id: String,
        val title: String,
        val description: String,
        val lessonsCount: Int
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module_view, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(modules[position], position + 1)
    }

    override fun getItemCount(): Int = modules.size

    fun updateItems(newModules: List<ModuleItem>) {
        modules.clear()
        modules.addAll(newModules)
        notifyDataSetChanged()
    }

    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvModuleNumber: TextView = itemView.findViewById(R.id.tv_module_number)
        private val tvModuleTitle: TextView = itemView.findViewById(R.id.tv_module_title)
        private val tvModuleDescription: TextView = itemView.findViewById(R.id.tv_module_description)
        private val tvLessonsCount: TextView = itemView.findViewById(R.id.tv_lessons_count)

        fun bind(module: ModuleItem, moduleNumber: Int) {
            tvModuleNumber.text = "Module $moduleNumber"
            tvModuleTitle.text = module.title.ifEmpty { "Untitled Module" }
            tvModuleDescription.text = module.description.ifEmpty { "No description available" }
            tvLessonsCount.text = "${module.lessonsCount} lesson${if (module.lessonsCount != 1) "s" else ""}"
            
            // Show/hide description based on content
            if (module.description.isNotEmpty()) {
                tvModuleDescription.visibility = View.VISIBLE
            } else {
                tvModuleDescription.visibility = View.GONE
            }
        }
    }
}