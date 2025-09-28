package com.example.ed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.example.ed.models.User
import java.text.SimpleDateFormat
import java.util.*

class UserRoleAdapter(
    private val onEditRole: (User) -> Unit,
    private val onDeleteRole: (User) -> Unit,
    private val onToggleStatus: (User) -> Unit
) : RecyclerView.Adapter<UserRoleAdapter.UserRoleViewHolder>() {

    private var users = mutableListOf<User>()

    fun updateUserRoles(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRoleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_role, parent, false)
        return UserRoleViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserRoleViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserRoleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
        private val roleTextView: TextView = itemView.findViewById(R.id.roleTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val lastLoginTextView: TextView = itemView.findViewById(R.id.lastLoginTextView)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        private val toggleStatusButton: MaterialButton = itemView.findViewById(R.id.toggleStatusButton)

        fun bind(user: User) {
            nameTextView.text = user.fullName
            emailTextView.text = user.email
            roleTextView.text = user.role
            statusTextView.text = if (user.isActive) "Active" else "Inactive"
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            lastLoginTextView.text = "Joined: ${dateFormat.format(user.createdAt)}"
            
            // Set status color
            val statusColor = if (user.isActive) {
                itemView.context.getColor(android.R.color.holo_green_dark)
            } else {
                itemView.context.getColor(android.R.color.holo_red_dark)
            }
            statusTextView.setTextColor(statusColor)
            
            // Set toggle button text
            toggleStatusButton.text = if (user.isActive) "Deactivate" else "Activate"
            
            // Set click listeners
            editButton.setOnClickListener { onEditRole(user) }
            deleteButton.setOnClickListener { onDeleteRole(user) }
            toggleStatusButton.setOnClickListener { onToggleStatus(user) }
        }
    }
}