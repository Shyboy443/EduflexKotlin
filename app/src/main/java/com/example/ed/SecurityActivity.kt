package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.UserRoleAdapter
import com.example.ed.models.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SecurityActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var userRolesRecyclerView: RecyclerView
    private lateinit var addUserButton: MaterialButton
    private lateinit var securitySettingsCard: MaterialCardView
    private lateinit var twoFactorSwitch: SwitchMaterial
    private lateinit var sessionTimeoutSpinner: Spinner
    private lateinit var passwordPolicyCard: MaterialCardView
    private lateinit var auditLogButton: MaterialButton
    private lateinit var backupButton: MaterialButton
    
    private lateinit var userRoleAdapter: UserRoleAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userRoles = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupSecuritySettings()
        setupButtons()
        loadUserRoles()
        checkCurrentUserPermissions()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        userRolesRecyclerView = findViewById(R.id.userRolesRecyclerView)
        addUserButton = findViewById(R.id.addUserButton)
        securitySettingsCard = findViewById(R.id.securitySettingsCard)
        twoFactorSwitch = findViewById(R.id.twoFactorSwitch)
        sessionTimeoutSpinner = findViewById(R.id.sessionTimeoutSpinner)
        passwordPolicyCard = findViewById(R.id.passwordPolicyCard)
        auditLogButton = findViewById(R.id.auditLogButton)
        backupButton = findViewById(R.id.backupButton)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        userRoleAdapter = UserRoleAdapter(
            onEditRole = { user: User -> editUserRole(user) },
            onDeleteRole = { user: User -> deleteUserRole(user) },
            onToggleStatus = { user: User -> toggleUserStatus(user) }
        )
        
        userRolesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SecurityActivity)
            adapter = userRoleAdapter
        }
    }

    private fun setupSecuritySettings() {
        // Setup session timeout spinner
        val timeoutOptions = arrayOf("15 minutes", "30 minutes", "1 hour", "2 hours", "4 hours", "Never")
        val timeoutAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeoutOptions)
        timeoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sessionTimeoutSpinner.adapter = timeoutAdapter
        sessionTimeoutSpinner.setSelection(2) // Default to 1 hour
        
        // Setup two-factor authentication switch
        twoFactorSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showTwoFactorSetupDialog()
            } else {
                disableTwoFactor()
            }
        }
        
        // Load current security settings
        loadSecuritySettings()
    }

    private fun setupButtons() {
        addUserButton.setOnClickListener {
            showAddUserDialog()
        }
        
        auditLogButton.setOnClickListener {
            openAuditLog()
        }
        
        backupButton.setOnClickListener {
            performSecurityBackup()
        }
    }

    private fun loadUserRoles() {
        // Mock user role data
        userRoles.clear()
        
        val mockUserRoles = listOf(
            User(
                id = "user1",
                email = "admin@school.edu",
                name = "John Admin",
                role = UserRole.ADMIN,
                isActive = true,
                lastLoginAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis() - 86400000 * 30
            ),
            User(
                id = "user2",
                email = "teacher1@school.edu",
                name = "Alice Teacher",
                role = UserRole.TEACHER,
                isActive = true,
                lastLoginAt = System.currentTimeMillis() - 3600000,
                createdAt = System.currentTimeMillis() - 86400000 * 15
            ),
            User(
                id = "user3",
                email = "student1@school.edu",
                name = "Bob Student",
                role = UserRole.STUDENT,
                isActive = true,
                lastLoginAt = System.currentTimeMillis() - 1800000,
                createdAt = System.currentTimeMillis() - 86400000 * 7
            ),
            User(
                id = "user4",
                email = "teacher2@school.edu",
                name = "Carol Instructor",
                role = UserRole.TEACHER,
                isActive = false,
                lastLoginAt = System.currentTimeMillis() - 86400000 * 5,
                createdAt = System.currentTimeMillis() - 86400000 * 60
            )
        )
        
        userRoles.addAll(mockUserRoles)
        userRoleAdapter.updateUserRoles(userRoles)
    }

    private fun checkCurrentUserPermissions() {
        // Check if current user has admin permissions
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        // Mock permission check - in real app, check against Firestore
        val hasAdminPermissions = true // Mock admin access
        
        if (!hasAdminPermissions) {
            Toast.makeText(this, "Access denied: Admin permissions required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.emailInput)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.roleSpinner)
        
        // Setup role spinner
        val roles = arrayOf("Student", "Teacher", "Admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = roleAdapter
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add New User")
            .setView(dialogView as View)
            .setPositiveButton("Add User") { dialog, which ->
                val email = emailInput.text.toString()
                val name = nameInput.text.toString()
                val selectedRole = when (roleSpinner.selectedItemPosition) {
                    0 -> UserRole.STUDENT
                    1 -> UserRole.TEACHER
                    else -> UserRole.ADMIN
                }
                
                if (email.isNotEmpty() && name.isNotEmpty()) {
                    addNewUser(email, name, selectedRole)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewUser(email: String, name: String, role: UserRole) {
        val newUser = User(
            id = "user_${System.currentTimeMillis()}",
            email = email,
            name = name,
            role = role,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = 0
        )
        
        // Add to Firebase (mock)
        // firestore.collection("users").document(newUser.id).set(newUser)
        
        // Add to local list and update UI
        userRoles.add(newUser)
        userRoleAdapter.updateUserRoles(userRoles)
        
        // Send invitation email (mock)
        sendInvitationEmail(email, name, role)
        
        Toast.makeText(this, "User added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun getPermissionsForRole(role: UserRole): List<Permission> {
        return when (role) {
            UserRole.ADMIN -> listOf(
                Permission.MANAGE_USERS,
                Permission.MANAGE_COURSES,
                Permission.VIEW_ANALYTICS,
                Permission.MANAGE_SYSTEM,
                Permission.GRADE_ASSIGNMENTS,
                Permission.VIEW_STUDENT_PROGRESS
            )
            UserRole.TEACHER -> listOf(
                Permission.MANAGE_COURSES,
                Permission.GRADE_ASSIGNMENTS,
                Permission.VIEW_STUDENT_PROGRESS
            )
            UserRole.STUDENT -> listOf(
                Permission.VIEW_COURSES,
                Permission.SUBMIT_ASSIGNMENTS,
                Permission.VIEW_GRADES
            )
        }
    }

    private fun editUserRole(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user_role, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.roleSpinner)
        val permissionsLayout = dialogView.findViewById<LinearLayout>(R.id.permissionsLayout)
        
        nameInput.setText(user.name)
        
        // Setup role spinner
        val roles = arrayOf("Student", "Teacher", "Admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = roleAdapter
        roleSpinner.setSelection(when (user.role) {
            UserRole.STUDENT -> 0
            UserRole.TEACHER -> 1
            UserRole.ADMIN -> 2
        })
        
        // Simple role editing without complex permissions
        val permissionsText = TextView(this).apply {
            text = "Role permissions will be automatically assigned based on the selected role."
            setPadding(16, 16, 16, 16)
        }
        permissionsLayout.addView(permissionsText)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit User Role")
            .setView(dialogView as View)
            .setPositiveButton("Save Changes") { dialog, which ->
                // Update user role logic here
                Toast.makeText(this, "User role updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserRole(user: User) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                userRoles.remove(user)
                userRoleAdapter.updateUserRoles(userRoles)
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleUserStatus(user: User) {
        user.isActive = !user.isActive
        userRoleAdapter.notifyDataSetChanged()
        
        val status = if (user.isActive) "activated" else "deactivated"
        Toast.makeText(this, "User $status", Toast.LENGTH_SHORT).show()
    }

    private fun loadSecuritySettings() {
        // Load current security settings from Firebase (mock)
        twoFactorSwitch.isChecked = false // Mock setting
    }

    private fun showTwoFactorSetupDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Enable Two-Factor Authentication")
            .setMessage("Two-factor authentication adds an extra layer of security to your account. You'll need to verify your identity using a second method when signing in.")
            .setPositiveButton("Enable") { _, _ ->
                // Setup 2FA logic
                Toast.makeText(this, "Two-factor authentication enabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                twoFactorSwitch.isChecked = false
            }
            .show()
    }

    private fun disableTwoFactor() {
        Toast.makeText(this, "Two-factor authentication disabled", Toast.LENGTH_SHORT).show()
    }

    private fun openAuditLog() {
        // Open audit log activity
        Toast.makeText(this, "Opening audit log...", Toast.LENGTH_SHORT).show()
    }

    private fun performSecurityBackup() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Security Backup")
            .setMessage("This will create a backup of all security settings, user roles, and permissions. Continue?")
            .setPositiveButton("Create Backup") { dialog, which ->
                // Perform backup logic
                Toast.makeText(this, "Security backup created successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendInvitationEmail(email: String, name: String, role: UserRole) {
        // Mock email sending
        Toast.makeText(this, "Invitation email sent to $email", Toast.LENGTH_SHORT).show()
    }
}