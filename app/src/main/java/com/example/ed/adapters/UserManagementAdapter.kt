package com.example.ed.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.databinding.ItemUserManagementBinding
import com.example.ed.models.User
import java.text.SimpleDateFormat
import java.util.*

class UserManagementAdapter(
    private val users: List<User>,
    private val onUserClick: (User) -> Unit,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.UserViewHolder>() {

    private val selectedUsers = mutableSetOf<String>()
    private val expandedUsers = mutableSetOf<String>()

    inner class UserViewHolder(private val binding: ItemUserManagementBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            // Basic user info
            binding.tvUserName.text = user.fullName
            binding.tvUserEmail.text = user.email
            binding.tvUserInitials.text = user.getInitials()
            
            // Role chip
            binding.roleChip.text = user.role
            try {
                val color = Color.parseColor(user.getRoleColor())
                binding.roleChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color))
            } catch (e: Exception) {
                binding.roleChip.setChipBackgroundColorResource(R.color.brand_primary)
            }
            
            // Status
            binding.tvUserStatus.text = user.getStatusText()
            if (user.isActive) {
                binding.tvUserStatus.setBackgroundResource(R.drawable.bg_status_active)
            } else {
                binding.tvUserStatus.setBackgroundResource(R.drawable.bg_status_inactive)
            }
            
            // Join date
            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            binding.tvJoinDate.text = "Joined: ${dateFormat.format(user.createdAt)}"
            
            // Avatar background color based on role
            try {
                val color = Color.parseColor(user.getRoleColor())
                binding.tvUserInitials.setBackgroundColor(color)
            } catch (e: Exception) {
                binding.tvUserInitials.setBackgroundResource(R.color.brand_primary)
            }
            
            // Selection state
            binding.checkboxSelect.isChecked = selectedUsers.contains(user.id)
            binding.checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedUsers.add(user.id)
                } else {
                    selectedUsers.remove(user.id)
                }
                onSelectionChanged(user.id, isChecked)
            }
            
            // Expanded state for quick actions
            val isExpanded = expandedUsers.contains(user.id)
            binding.quickActionsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Click listeners
            binding.root.setOnClickListener {
                onUserClick(user)
            }
            
            binding.btnMoreActions.setOnClickListener {
                if (isExpanded) {
                    expandedUsers.remove(user.id)
                } else {
                    expandedUsers.add(user.id)
                }
                notifyItemChanged(adapterPosition)
            }
            
            binding.btnEditUser.setOnClickListener {
                onEditClick(user)
            }
            
            binding.btnViewDetails.setOnClickListener {
                onUserClick(user)
            }
            
            binding.btnDeleteUser.setOnClickListener {
                onDeleteClick(user)
            }
        }
        
        private fun getRoleColorResource(role: String): Int {
            return when (role.lowercase()) {
                "admin" -> R.color.role_admin
                "teacher" -> R.color.role_teacher
                "student" -> R.color.role_student
                else -> R.color.brand_primary
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserManagementBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun getSelectedUsers(): Set<String> = selectedUsers.toSet()
    
    fun clearSelection() {
        selectedUsers.clear()
        notifyDataSetChanged()
    }
    
    fun selectAll() {
        selectedUsers.clear()
        selectedUsers.addAll(users.map { it.id })
        notifyDataSetChanged()
    }
    
    fun isUserSelected(userId: String): Boolean = selectedUsers.contains(userId)
}