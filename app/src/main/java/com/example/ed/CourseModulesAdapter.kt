package com.example.ed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseModulesAdapter(
    private val modules: MutableList<TeacherCourseUploadActivity.CourseModule>,
    private val onDeleteClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit
) : RecyclerView.Adapter<CourseModulesAdapter.ModuleViewHolder>() {

    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvModuleTitle: TextView = itemView.findViewById(R.id.tv_module_title)
        val tvModuleDescription: TextView = itemView.findViewById(R.id.tv_module_description)
        val btnEditModule: Button = itemView.findViewById(R.id.btn_edit_module)
        val btnDeleteModule: Button = itemView.findViewById(R.id.btn_delete_module)
        val tvLessonsCount: TextView = itemView.findViewById(R.id.tv_lessons_count)
        val tvModuleDuration: TextView = itemView.findViewById(R.id.tv_module_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_module, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        
        holder.tvModuleTitle.text = module.title.ifEmpty { "Module ${position + 1}" }
        holder.tvModuleDescription.text = module.description.ifEmpty { "No description provided" }
        holder.tvLessonsCount.text = "${module.lessons.size} lessons"
        holder.tvModuleDuration.text = "0h 0m" // TODO: Calculate actual duration
        
        holder.btnEditModule.setOnClickListener {
            onEditClick(position)
        }
        
        holder.btnDeleteModule.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = modules.size
}