package com.example.ed

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.adapters.UserManagementAdapter
import com.example.ed.databinding.ActivityUserManagementBinding
import com.example.ed.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class UserManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userAdapter: UserManagementAdapter
    
    private var allUsers = mutableListOf<User>()
    private var filteredUsers = mutableListOf<User>()
    private var selectedUsers = mutableSetOf<String>()
    
    companion object {
        private const val TAG = "UserManagement"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        firestore = FirebaseFirestore.getInstance()
        
        setupUI()
        setupRecyclerView()
        setupSearchAndFilters()
        loadUsers()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadUsers()
        }
        
        binding.btnSelectAll.setOnClickListener {
            toggleSelectAll()
        }
        
        binding.btnDeleteSelected.setOnClickListener {
            showDeleteSelectedDialog()
        }
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserManagementAdapter(
            users = filteredUsers,
            onUserClick = { user -> showUserDetails(user) },
            onEditClick = { user -> showEditUserDialog(user) },
            onDeleteClick = { user -> showDeleteUserDialog(user) },
            onSelectionChanged = { userId, isSelected ->
                if (isSelected) {
                    selectedUsers.add(userId)
                } else {
                    selectedUsers.remove(userId)
                }
                updateSelectionUI()
            }
        )
        
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = userAdapter
        }
    }
    
    private fun setupSearchAndFilters() {
        // Search functionality
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterUsers()
            }
        })
        
        // Filter chips
        binding.chipAll.setOnCheckedChangeListener { _, _ -> filterUsers() }
        binding.chipStudents.setOnCheckedChangeListener { _, _ -> filterUsers() }
        binding.chipTeachers.setOnCheckedChangeListener { _, _ -> filterUsers() }
        binding.chipAdmins.setOnCheckedChangeListener { _, _ -> filterUsers() }
        binding.chipActive.setOnCheckedChangeListener { _, _ -> filterUsers() }
    }
    
    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewUsers.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                allUsers.clear()
                
                for (document in documents) {
                    try {
                        val user = User(
                            id = document.id,
                            fullName = document.getString("fullName") ?: "Unknown",
                            email = document.getString("email") ?: "No email",
                            role = document.getString("role") ?: "Student",
                            isActive = document.getBoolean("isActive") ?: true,
                            createdAt = document.getTimestamp("createdAt")?.toDate() ?: Date()
                        )
                        allUsers.add(user)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user document: ${document.id}", e)
                    }
                }
                
                // Sort users by name
                allUsers.sortBy { it.fullName.lowercase() }
                
                filterUsers()
                updateUserCountDisplay()
                
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE
                
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading users", e)
                Toast.makeText(this, "Error loading users: ${e.message}", Toast.LENGTH_LONG).show()
                
                binding.progressBar.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            }
    }
    
    private fun filterUsers() {
        val searchQuery = binding.etSearch.text.toString().lowercase().trim()
        
        filteredUsers.clear()
        
        for (user in allUsers) {
            // Apply search filter
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                user.fullName.lowercase().contains(searchQuery) ||
                user.email.lowercase().contains(searchQuery) ||
                user.role.lowercase().contains(searchQuery)
            }
            
            if (!matchesSearch) continue
            
            // Apply role filters
            val roleFilter = when {
                binding.chipStudents.isChecked && user.role.equals("Student", ignoreCase = true) -> true
                binding.chipTeachers.isChecked && user.role.equals("Teacher", ignoreCase = true) -> true
                binding.chipAdmins.isChecked && user.role.equals("Admin", ignoreCase = true) -> true
                binding.chipAll.isChecked -> true
                !binding.chipStudents.isChecked && !binding.chipTeachers.isChecked && 
                !binding.chipAdmins.isChecked -> true
                else -> false
            }
            
            if (!roleFilter) continue
            
            // Apply active filter
            val activeFilter = if (binding.chipActive.isChecked) {
                user.isActive
            } else {
                true
            }
            
            if (!activeFilter) continue
            
            filteredUsers.add(user)
        }
        
        userAdapter.notifyDataSetChanged()
        
        // Show/hide empty state
        if (filteredUsers.isEmpty()) {
            binding.recyclerViewUsers.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewUsers.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
        
        // Clear selection when filtering
        selectedUsers.clear()
        updateSelectionUI()
    }
    
    private fun updateUserCountDisplay() {
        val totalUsers = allUsers.size
        val filteredCount = filteredUsers.size
        
        binding.tvUserCount.text = if (totalUsers == filteredCount) {
            "$totalUsers users"
        } else {
            "$filteredCount of $totalUsers users"
        }
    }
    
    private fun toggleSelectAll() {
        if (selectedUsers.size == filteredUsers.size) {
            // Deselect all
            selectedUsers.clear()
        } else {
            // Select all visible users
            selectedUsers.clear()
            selectedUsers.addAll(filteredUsers.map { it.id })
        }
        
        userAdapter.notifyDataSetChanged()
        updateSelectionUI()
    }
    
    private fun updateSelectionUI() {
        val selectedCount = selectedUsers.size
        val totalVisible = filteredUsers.size
        
        binding.btnSelectAll.text = if (selectedCount == totalVisible && totalVisible > 0) {
            "Deselect All"
        } else {
            "Select All"
        }
        
        binding.btnDeleteSelected.isEnabled = selectedCount > 0
        binding.btnDeleteSelected.text = if (selectedCount > 0) {
            "Delete Selected ($selectedCount)"
        } else {
            "Delete Selected"
        }
    }
    
    private fun showUserDetails(user: User) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val joinDate = dateFormat.format(user.createdAt)
        
        val message = """
            👤 Name: ${user.fullName}
            📧 Email: ${user.email}
            🏷️ Role: ${user.role}
            📅 Joined: $joinDate
            ✅ Status: ${if (user.isActive) "Active" else "Inactive"}
            🆔 User ID: ${user.id}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("User Details")
            .setMessage(message)
            .setPositiveButton("Edit") { _, _ -> showEditUserDialog(user) }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showEditUserDialog(user: User) {
        // TODO: Implement edit user dialog
        Toast.makeText(this, "Edit user functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeleteUserDialog(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user?\n\n👤 ${user.fullName}\n📧 ${user.email}\n🏷️ ${user.role}\n\n⚠️ This action cannot be undone!")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteSelectedDialog() {
        if (selectedUsers.isEmpty()) return
        
        val selectedCount = selectedUsers.size
        
        AlertDialog.Builder(this)
            .setTitle("Delete Multiple Users")
            .setMessage("Are you sure you want to delete $selectedCount selected users?\n\n⚠️ This action cannot be undone!")
            .setPositiveButton("Delete All") { _, _ ->
                deleteSelectedUsers()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteUser(user: User) {
        lifecycleScope.launch {
            try {
                // Delete user document
                firestore.collection("users").document(user.id).delete().await()
                
                // Remove from local lists
                allUsers.removeAll { it.id == user.id }
                filteredUsers.removeAll { it.id == user.id }
                selectedUsers.remove(user.id)
                
                userAdapter.notifyDataSetChanged()
                updateUserCountDisplay()
                updateSelectionUI()
                
                Toast.makeText(this@UserManagementActivity, "User deleted successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting user", e)
                Toast.makeText(this@UserManagementActivity, "Error deleting user: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun deleteSelectedUsers() {
        lifecycleScope.launch {
            try {
                val usersToDelete = selectedUsers.toList()
                var deletedCount = 0
                
                for (userId in usersToDelete) {
                    try {
                        firestore.collection("users").document(userId).delete().await()
                        deletedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting user $userId", e)
                    }
                }
                
                // Remove deleted users from local lists
                allUsers.removeAll { it.id in usersToDelete }
                filteredUsers.removeAll { it.id in usersToDelete }
                selectedUsers.clear()
                
                userAdapter.notifyDataSetChanged()
                updateUserCountDisplay()
                updateSelectionUI()
                
                Toast.makeText(this@UserManagementActivity, "$deletedCount users deleted successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting selected users", e)
                Toast.makeText(this@UserManagementActivity, "Error deleting users: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}