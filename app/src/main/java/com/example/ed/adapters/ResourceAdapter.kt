package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.WeeklyResource
import com.google.android.material.card.MaterialCardView

class ResourceAdapter(
    private val resources: MutableList<WeeklyResource>,
    private val onResourceClick: (WeeklyResource) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_resource)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_resource_title)
        val tvType: TextView = itemView.findViewById(R.id.tv_resource_type)
        val tvUrl: TextView = itemView.findViewById(R.id.tv_resource_url)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = resources[position]
        
        holder.tvTitle.text = resource.title
        holder.tvType.text = resource.type.name.replace("_", " ").lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        holder.tvUrl.text = resource.url
        
        holder.cardView.setOnClickListener {
            onResourceClick(resource)
        }
    }

    override fun getItemCount(): Int = resources.size

    fun addResource(resource: WeeklyResource) {
        resources.add(resource)
        notifyItemInserted(resources.size - 1)
    }

    fun removeResource(position: Int) {
        if (position >= 0 && position < resources.size) {
            resources.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateResource(position: Int, resource: WeeklyResource) {
        if (position >= 0 && position < resources.size) {
            resources[position] = resource
            notifyItemChanged(position)
        }
    }
}