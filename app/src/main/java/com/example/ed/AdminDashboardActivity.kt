package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.ed.utils.FirebaseDataSeeder
import kotlinx.coroutines.launch
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.ed.services.DatabaseBackupService
import androidx.appcompat.app.AlertDialog
import android.app.ProgressDialog

class AdminDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var backupService: DatabaseBackupService
    
    companion object {
        private const val TAG = "AdminDashboard"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme before setting content view
        ThemeManager.applyCurrentTheme(this)
        
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        backupService = DatabaseBackupService(this)
        
        setupUI()
        setupBottomNavigation()
        loadAdminData()
        
        // Set dashboard as selected
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
    }
    
    private fun setupUI() {
        // User Management
        binding.btnManageUsers.setOnClickListener {
            showUserManagement()
        }
        
        binding.btnCreateTeacher.setOnClickListener {
            showCreateTeacherDialog()
        }
        
        // Data Management
        binding.btnDebugDatabase.setOnClickListener {
            showDatabaseStatus()
        }

        binding.btnSeedSampleData.setOnClickListener {
            importSampleData()
        }

        binding.btnDeleteCourses.setOnClickListener {
            deleteCourses()
        }
        
        // System
        binding.btnViewReports.setOnClickListener {
            showReports()
        }
        
        binding.btnSystemSettings.setOnClickListener {
            showSystemSettings()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.nav_users -> {
                    showUserManagement()
                    true
                }
                R.id.nav_data -> {
                    showDatabaseStatus()
                    true
                }
                R.id.nav_reports -> {
                    showReports()
                    true
                }
                R.id.nav_settings -> {
                    showSystemSettings()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun showUserManagement() {
        val intent = Intent(this, UserManagementActivity::class.java)
        startActivity(intent)
    }
    
    private fun showAllUsers(userDocuments: List<Pair<String, com.google.firebase.firestore.DocumentSnapshot>>) {
        val message = userDocuments.map { it.first }.joinToString("\n\n")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("All Users (${userDocuments.size})")
            .setMessage(message)
            .setPositiveButton("Back") { _, _ -> showUserManagement() }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showDeleteUserDialog(userDocuments: List<Pair<String, com.google.firebase.firestore.DocumentSnapshot>>) {
        if (userDocuments.isEmpty()) {
            Toast.makeText(this, "No users to delete", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userNames = userDocuments.map { pair ->
            val doc = pair.second
            val name = doc.getString("fullName") ?: "Unknown"
            val email = doc.getString("email") ?: "No email"
            val role = doc.getString("role") ?: "Student"
            "$name ($email) - $role"
        }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select User to Delete")
            .setItems(userNames) { _, which ->
                val selectedUser = userDocuments[which]
                val doc = selectedUser.second
                val name = doc.getString("fullName") ?: "Unknown"
                val email = doc.getString("email") ?: "No email"
                val role = doc.getString("role") ?: "Student"
                
                // Confirm deletion
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete User")
                    .setMessage("Are you sure you want to delete this user?\n\n👤 $name\n📧 $email\n🏷️ $role\n\n⚠️ This action cannot be undone!")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteUser(doc.id, name)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Back") { _, _ -> showUserManagement() }
            .show()
    }
    
    private fun deleteUser(userId: String, userName: String) {
        try {
            Toast.makeText(this, "Deleting user: $userName...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                try {
                    // Delete user document from Firestore
                    firestore.collection("users")
                        .document(userId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this@AdminDashboardActivity, "User '$userName' deleted successfully", Toast.LENGTH_LONG).show()
                            Log.d(TAG, "User deleted: $userId - $userName")
                            
                            // Also delete related data (enrollments, bookmarks, etc.)
                            deleteUserRelatedData(userId, userName)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error deleting user", e)
                            Toast.makeText(this@AdminDashboardActivity, "Error deleting user: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in deleteUser", e)
                    Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteUser", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteUserRelatedData(userId: String, userName: String) {
        try {
            // Delete user enrollments
            firestore.collection("enrollments")
                .whereEqualTo("studentId", userId)
                .get()
                .addOnSuccessListener { enrollments ->
                    val batch = firestore.batch()
                    for (doc in enrollments) {
                        batch.delete(doc.reference)
                    }
                    if (!enrollments.isEmpty) {
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Deleted ${enrollments.size()} enrollments for user: $userName")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error deleting enrollments for user: $userName", e)
                            }
                    }
                }
            
            // Delete user's courses if they are a teacher
            firestore.collection("courses")
                .whereEqualTo("teacherId", userId)
                .get()
                .addOnSuccessListener { courses ->
                    val batch = firestore.batch()
                    for (doc in courses) {
                        batch.delete(doc.reference)
                    }
                    if (!courses.isEmpty) {
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Deleted ${courses.size()} courses for teacher: $userName")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error deleting courses for teacher: $userName", e)
                            }
                    }
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteUserRelatedData", e)
        }
    }
    
    private fun showDeleteAllUsersDialog(totalUsers: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ DELETE ALL USERS")
            .setMessage("WARNING: This will DELETE ALL $totalUsers users from the database!\n\n" +
                    "This includes:\n" +
                    "• All user accounts\n" +
                    "• All enrollments\n" +
                    "• All user-created courses\n" +
                    "• All user data\n\n" +
                    "⚠️ THIS ACTION CANNOT BE UNDONE!\n\n" +
                    "Are you absolutely sure?")
            .setPositiveButton("DELETE ALL USERS") { _, _ ->
                // Second confirmation - simple button press
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("FINAL CONFIRMATION")
                    .setMessage("This is your FINAL WARNING!\n\n" +
                            "You are about to delete ALL $totalUsers users.\n\n" +
                            "This action is PERMANENT and cannot be undone!")
                    .setPositiveButton("YES, DELETE ALL") { _, _ ->
                        deleteAllUsers()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteAllUsers() {
        try {
            Toast.makeText(this, "Deleting all users... This may take a moment.", Toast.LENGTH_LONG).show()
            
            lifecycleScope.launch {
                try {
                    var totalDeleted = 0
                    
                    // Get all users
                    firestore.collection("users")
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                Toast.makeText(this@AdminDashboardActivity, "No users found to delete", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            
                            val batch = firestore.batch()
                            
                            // Add all user deletions to batch
                            for (document in documents) {
                                batch.delete(document.reference)
                                totalDeleted++
                            }
                            
                            // Execute batch delete
                            batch.commit()
                                .addOnSuccessListener {
                                    Toast.makeText(this@AdminDashboardActivity, 
                                        "Successfully deleted $totalDeleted users", Toast.LENGTH_LONG).show()
                                    Log.d(TAG, "Deleted all $totalDeleted users")
                                    
                                    // Also delete all related data
                                    deleteAllUserRelatedData(totalDeleted)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error deleting all users", e)
                                    Toast.makeText(this@AdminDashboardActivity, 
                                        "Error deleting users: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching users for deletion", e)
                            Toast.makeText(this@AdminDashboardActivity, 
                                "Error fetching users: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in deleteAllUsers", e)
                    Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteAllUsers", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteAllUserRelatedData(deletedUserCount: Int) {
        try {
            // Delete all enrollments
            firestore.collection("enrollments")
                .get()
                .addOnSuccessListener { enrollments ->
                    if (!enrollments.isEmpty) {
                        val batch = firestore.batch()
                        for (doc in enrollments) {
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Deleted ${enrollments.size()} enrollments after user cleanup")
                                Toast.makeText(this@AdminDashboardActivity, 
                                    "Cleanup complete: $deletedUserCount users and ${enrollments.size()} enrollments deleted", 
                                    Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error deleting enrollments during cleanup", e)
                            }
                    }
                }
                
            // Note: We don't delete all courses here as some might be system/admin courses
            // Only user-specific courses were deleted in individual user deletion
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteAllUserRelatedData", e)
        }
    }
    
    private fun showCreateTeacherDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        val editTextLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        
        val etName = android.widget.EditText(this).apply {
            hint = "Full Name"
            setPadding(20, 20, 20, 20)
        }
        val etEmail = android.widget.EditText(this).apply {
            hint = "Email Address"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(20, 20, 20, 20)
        }
        val etPassword = android.widget.EditText(this).apply {
            hint = "Password (min 6 characters)"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 20, 20, 20)
        }
        
        editTextLayout.addView(etName)
        editTextLayout.addView(etEmail)
        editTextLayout.addView(etPassword)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Create Teacher Account")
            .setView(editTextLayout)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                createTeacherAccount(name, email, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createTeacherAccount(name: String, email: String, password: String) {
        try {
            binding.btnCreateTeacher.isEnabled = false
            Toast.makeText(this, "Creating teacher account...", Toast.LENGTH_SHORT).show()
            
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        // Update profile
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        
                        user.updateProfile(profileUpdates)
                            .addOnSuccessListener {
                                // Create user document in Firestore
                                val userData = hashMapOf(
                                    "fullName" to name,
                                    "email" to email,
                                    "role" to "Teacher",
                                    "isActive" to true,
                                    "createdAt" to System.currentTimeMillis(),
                                    "createdBy" to auth.currentUser?.uid,
                                    "profilePicture" to "",
                                    "bio" to "Teacher at EduFlex",
                                    "specialization" to "",
                                    "yearsOfExperience" to 0
                                )
                                
                                firestore.collection("users")
                                    .document(user.uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        binding.btnCreateTeacher.isEnabled = true
                                        Toast.makeText(this, "Teacher account created successfully!", Toast.LENGTH_LONG).show()
                                        Log.d(TAG, "Teacher account created: $email")
                                    }
                                    .addOnFailureListener { e ->
                                        binding.btnCreateTeacher.isEnabled = true
                                        Log.e(TAG, "Error creating user document", e)
                                        Toast.makeText(this, "Account created but error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                binding.btnCreateTeacher.isEnabled = true
                                Log.e(TAG, "Error updating profile", e)
                                Toast.makeText(this, "Account created but error updating profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    binding.btnCreateTeacher.isEnabled = true
                    Log.e(TAG, "Error creating teacher account", e)
                    Toast.makeText(this, "Error creating account: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            binding.btnCreateTeacher.isEnabled = true
            Log.e(TAG, "Exception in createTeacherAccount", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDatabaseStatus() {
        try {
            binding.btnDebugDatabase.isEnabled = false
            Toast.makeText(this, "Checking database status...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                try {
                    val result = FirebaseDataSeeder.debugDatabaseStatus()
                    binding.btnDebugDatabase.isEnabled = true
                    
                    val message = result.details.joinToString("\n")
                    androidx.appcompat.app.AlertDialog.Builder(this@AdminDashboardActivity)
                        .setTitle("Database Status")
                        .setMessage("Total documents: ${result.totalRecords}\n\n$message")
                        .setPositiveButton("Refresh") { _, _ -> showDatabaseStatus() }
                        .setNegativeButton("Close", null)
                        .show()
                } catch (e: Exception) {
                    binding.btnDebugDatabase.isEnabled = true
                    Log.e(TAG, "Error checking database status", e)
                    Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            binding.btnDebugDatabase.isEnabled = true
            Log.e(TAG, "Exception in showDatabaseStatus", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun importSampleData() {
        try {
            binding.btnSeedSampleData.isEnabled = false
            Toast.makeText(this, "Starting sample data import...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                try {
                    val result = FirebaseDataSeeder.seedDatabase(this@AdminDashboardActivity)
                    binding.btnSeedSampleData.isEnabled = true
                    
                    val msg = if (result.success) {
                        "Imported ${result.totalRecords} records successfully"
                    } else {
                        "Import failed: ${result.details.firstOrNull() ?: "Unknown error"}"
                    }
                    Toast.makeText(this@AdminDashboardActivity, msg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    binding.btnSeedSampleData.isEnabled = true
                    Log.e(TAG, "Error importing sample data", e)
                    Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            binding.btnSeedSampleData.isEnabled = true
            Log.e(TAG, "Exception in importSampleData", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteCourses() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete All Courses")
                .setMessage("This will delete ALL courses from the database. This action cannot be undone. Are you sure?")
                .setPositiveButton("Delete All Courses") { _, _ ->
                    binding.btnDeleteCourses.isEnabled = false
                    Toast.makeText(this, "Deleting all courses...", Toast.LENGTH_SHORT).show()
                    
                    lifecycleScope.launch {
                        try {
                            firestore.collection("courses")
                                .get()
                                .addOnSuccessListener { documents ->
                                    val batch = firestore.batch()
                                    var deleteCount = 0
                                    
                                    for (document in documents) {
                                        batch.delete(document.reference)
                                        deleteCount++
                                    }
                                    
                                    if (deleteCount > 0) {
                                        batch.commit()
                                            .addOnSuccessListener {
                                                binding.btnDeleteCourses.isEnabled = true
                                                Toast.makeText(this@AdminDashboardActivity, "Deleted $deleteCount courses successfully", Toast.LENGTH_LONG).show()
                                                Log.d(TAG, "Deleted $deleteCount courses")
                                            }
                                            .addOnFailureListener { e ->
                                                binding.btnDeleteCourses.isEnabled = true
                                                Log.e(TAG, "Error deleting courses", e)
                                                Toast.makeText(this@AdminDashboardActivity, "Error deleting courses: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        binding.btnDeleteCourses.isEnabled = true
                                        Toast.makeText(this@AdminDashboardActivity, "No courses found to delete", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    binding.btnDeleteCourses.isEnabled = true
                                    Log.e(TAG, "Error fetching courses for deletion", e)
                                    Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } catch (e: Exception) {
                            binding.btnDeleteCourses.isEnabled = true
                            Log.e(TAG, "Exception in deleteCourses", e)
                            Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteCourses dialog", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showReports() {
        try {
            Toast.makeText(this, "Loading system reports...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                try {
                    val reports = mutableListOf<String>()
                    
                    // Get user counts
                    firestore.collection("users").get().addOnSuccessListener { users ->
                        val totalUsers = users.size()
                        val teachers = users.documents.count { it.getString("role") == "Teacher" }
                        val students = users.documents.count { it.getString("role") == "Student" }
                        val admins = users.documents.count { it.getString("role") == "Admin" }
                        
                        // Get course counts
                        firestore.collection("courses").get().addOnSuccessListener { courses ->
                            val totalCourses = courses.size()
                            val publishedCourses = courses.documents.count { it.getBoolean("isPublished") == true }
                            
                            // Get enrollment counts
                            firestore.collection("enrollments").get().addOnSuccessListener { enrollments ->
                                val totalEnrollments = enrollments.size()
                                
                                val reportMessage = """
                                    📊 SYSTEM REPORTS
                                    
                                    👥 Users:
                                    • Total: $totalUsers
                                    • Teachers: $teachers
                                    • Students: $students
                                    • Admins: $admins
                                    
                                    📚 Courses:
                                    • Total: $totalCourses
                                    • Published: $publishedCourses
                                    • Draft: ${totalCourses - publishedCourses}
                                    
                                    📝 Enrollments:
                                    • Total: $totalEnrollments
                                    
                                    📅 Generated: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                                """.trimIndent()
                                
                                androidx.appcompat.app.AlertDialog.Builder(this@AdminDashboardActivity)
                                    .setTitle("System Reports")
                                    .setMessage(reportMessage)
                                    .setPositiveButton("Refresh") { _, _ -> showReports() }
                                    .setNegativeButton("Close", null)
                                    .show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating reports", e)
                    Toast.makeText(this@AdminDashboardActivity, "Error generating reports: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in showReports", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSystemSettings() {
        val settings = arrayOf(
            "App Settings",
            "Firebase Configuration", 
            "User Permissions",
            "Backup Database",
            "Export Data",
            "Logout"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("System Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "App Settings - Coming Soon", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Firebase Configuration - Coming Soon", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "User Permissions - Coming Soon", Toast.LENGTH_SHORT).show()
                    3 -> showBackupDatabaseDialog()
                    4 -> Toast.makeText(this, "Export Data - Coming Soon", Toast.LENGTH_SHORT).show()
                    5 -> {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
            .show()
    }
    
    private fun loadAdminData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: "Admin"
                        binding.tvWelcome.text = "Welcome, $fullName"
                    }
                }
        }
    }
    
    private fun showBackupDatabaseDialog() {
        val options = arrayOf(
            "Create New Backup",
            "View Backup Files",
            "Backup Information"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Database Backup")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createDatabaseBackup()
                    1 -> showBackupFiles()
                    2 -> showBackupInfo()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createDatabaseBackup() {
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Creating Backup")
            setMessage("Exporting database collections...")
            setCancelable(false)
            show()
        }
        
        lifecycleScope.launch {
            try {
                val result = backupService.createFullDatabaseBackup()
                progressDialog.dismiss()
                
                result.fold(
                    onSuccess = { backupFile ->
                        AlertDialog.Builder(this@AdminDashboardActivity)
                            .setTitle("Backup Created Successfully")
                            .setMessage("Backup saved to: ${backupFile.name}\n\nWould you like to share this backup file?")
                            .setPositiveButton("Share") { _, _ ->
                                backupService.shareBackupFile(backupFile)
                            }
                            .setNegativeButton("Done", null)
                            .show()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Backup failed", exception)
                        Toast.makeText(this@AdminDashboardActivity, "Backup failed: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                )
                    
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(TAG, "Backup failed", e)
                Toast.makeText(this@AdminDashboardActivity, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showBackupFiles() {
        val backupFiles = backupService.getAllBackupFiles()
        
        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileNames = backupFiles.map { file -> 
            "${file.name} (${android.text.format.Formatter.formatFileSize(this, file.length())})" 
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Backup Files")
            .setItems(fileNames) { _, which ->
                val selectedFile = backupFiles[which]
                showBackupFileOptions(selectedFile)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showBackupFileOptions(file: java.io.File) {
        val options = arrayOf("Share", "Delete", "File Info")
        
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> backupService.shareBackupFile(file)
                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Backup")
                            .setMessage("Are you sure you want to delete this backup file?")
                            .setPositiveButton("Delete") { _, _ ->
                                if (backupService.deleteBackupFile(file)) {
                                    Toast.makeText(this, "Backup file deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Failed to delete backup file", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    2 -> {
                        val info = backupService.getBackupFileInfo(file)
                        AlertDialog.Builder(this)
                            .setTitle("Backup File Info")
                            .setMessage(info)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Back", null)
            .show()
    }
    
    private fun showBackupInfo() {
        val info = """
            Database Backup Information:
            
            • Backup includes all Firestore collections
            • Collections backed up: users, courses, enrollments, weekly_content, student_progress, student_analytics, image_metadata, securityLogs
            • Backup format: JSON
            • Files are saved to: Android/data/com.example.ed/files/backups/
            • Backups can be shared via email, cloud storage, etc.
            
            Note: Backups contain sensitive user data. Handle with care and ensure secure storage.
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Backup Information")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }
}