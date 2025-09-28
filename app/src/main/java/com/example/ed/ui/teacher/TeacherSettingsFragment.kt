package com.example.ed.ui.teacher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.ed.LoginActivity
import com.example.ed.R
import com.example.ed.TeacherSettingsActivity
import com.example.ed.ThemeManager
import com.example.ed.databinding.FragmentTeacherSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TeacherSettingsFragment : Fragment() {

    private var _binding: FragmentTeacherSettingsBinding? = null
    private val binding get() = _binding
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    private var currentUserData: Map<String, Any>? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherSettingsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupUI()
        setupClickListeners()
        loadUserProfile()
        setupThemeToggle()
    }

    private fun setupUI() {
        // Show loading state initially
        binding?.progressBarProfile?.visibility = View.VISIBLE
        binding?.layoutContent?.visibility = View.GONE
    }

    private fun setupClickListeners() {
        // Top logout button click
        binding?.btnLogoutTop?.setOnClickListener {
            showSignOutConfirmation()
        }
        
        // Profile picture click
        binding?.ivProfilePicture?.setOnClickListener {
            showProfilePictureOptions()
        }
        
        // Edit profile click
        binding?.btnEditProfile?.setOnClickListener {
            showEditProfileDialog()
        }
        
        // Account settings
        binding?.layoutAccountSettings?.setOnClickListener {
            startActivity(Intent(requireContext(), TeacherSettingsActivity::class.java))
        }
        
        // Notifications
        binding?.layoutNotifications?.setOnClickListener {
            showNotificationSettings()
        }
        
        // Privacy
        binding?.layoutPrivacy?.setOnClickListener {
            showPrivacySettings()
        }
        
        // Help & Support
        binding?.layoutHelp?.setOnClickListener {
            showHelpAndSupport()
        }
        
        // About
        binding?.layoutAbout?.setOnClickListener {
            showAboutDialog()
        }
        
        // Backup & Sync
        binding?.layoutBackup?.setOnClickListener {
            showBackupOptions()
        }
        
        // Analytics
        binding?.layoutAnalytics?.setOnClickListener {
            showAnalyticsSettings()
        }
        
        // Sign out
        binding?.layoutSignOut?.setOnClickListener {
            showSignOutConfirmation()
        }
    }

    private fun setupThemeToggle() {
        // Set current theme state
        binding?.switchDarkMode?.isChecked = ThemeManager.isDarkMode(requireContext())
        
        // Set up theme toggle listener
        binding?.switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            val newTheme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(requireContext(), newTheme)
            ThemeManager.applyTheme(newTheme)
            // Restart activity to apply theme
            requireActivity().recreate()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                // Load user data from Firestore
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    currentUserData = userDoc.data
                    updateProfileUI(userDoc.data ?: emptyMap())
                } else {
                    // Create basic profile from Firebase Auth
                    val basicData = mapOf(
                        "fullName" to (currentUser.displayName ?: "Teacher"),
                        "email" to (currentUser.email ?: ""),
                        "role" to "Teacher",
                        "profileImageUrl" to (currentUser.photoUrl?.toString() ?: "")
                    )
                    currentUserData = basicData
                    updateProfileUI(basicData)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                // Fallback to Firebase Auth data
                updateProfileUI(mapOf(
                    "fullName" to (currentUser.displayName ?: "Teacher"),
                    "email" to (currentUser.email ?: "")
                ))
            } finally {
                binding?.progressBarProfile?.visibility = View.GONE
                binding?.layoutContent?.visibility = View.VISIBLE
            }
        }
    }

    private fun updateProfileUI(userData: Map<String, Any>) {
        binding?.tvUserName?.text = userData["fullName"]?.toString() ?: "Teacher"
        binding?.tvUserEmail?.text = userData["email"]?.toString() ?: ""
        binding?.tvUserRole?.text = userData["role"]?.toString() ?: "Teacher"
        
        // Load profile picture
        val profileImageUrl = userData["profileImageUrl"]?.toString()
        if (!profileImageUrl.isNullOrEmpty() && binding?.ivProfilePicture != null) {
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivProfilePicture)
        } else {
            binding?.ivProfilePicture?.setImageResource(R.drawable.ic_person)
        }
        
        // Set additional info
        binding?.tvMemberSince?.text = "Member since ${userData["createdAt"]?.let { 
            java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(it.toString().toLongOrNull() ?: System.currentTimeMillis()))
        } ?: "Recently"}"
        
        // Set stats
        binding?.tvTotalCourses?.text = userData["totalCourses"]?.toString() ?: "0"
        binding?.tvTotalStudents?.text = userData["totalStudents"]?.toString() ?: "0"
        binding?.tvAverageRating?.text = String.format("%.1f", userData["averageRating"]?.toString()?.toDoubleOrNull() ?: 0.0)
    }

    private fun showProfilePictureOptions() {
        val options = arrayOf("Choose from Gallery", "Remove Picture")
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> removeProfilePicture()
                }
            }
            .show()
    }

    private fun uploadProfileImage(uri: Uri) {
        val currentUser = auth.currentUser ?: return
        
        binding.progressBarProfile.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val imageRef = storage.reference
                    .child("profile_images")
                    .child("${currentUser.uid}.jpg")
                
                val uploadTask = imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                
                // Update Firestore
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("profileImageUrl", downloadUrl.toString())
                    .await()
                
                // Update UI
                Glide.with(this@TeacherSettingsFragment)
                    .load(downloadUrl)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
                
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarProfile.visibility = View.GONE
            }
        }
    }

    private fun removeProfilePicture() {
        val currentUser = auth.currentUser ?: return
        
        lifecycleScope.launch {
            try {
                // Remove from Firestore
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("profileImageUrl", "")
                    .await()
                
                // Update UI
                binding.ivProfilePicture.setImageResource(R.drawable.ic_person)
                
                Toast.makeText(requireContext(), "Profile picture removed", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to remove picture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etFullName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_full_name)
        val etBio = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_bio)
        val etSpecialization = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_specialization)
        
        // Pre-fill current data
        etFullName.setText(currentUserData?.get("fullName")?.toString() ?: "")
        etBio.setText(currentUserData?.get("bio")?.toString() ?: "")
        etSpecialization.setText(currentUserData?.get("specialization")?.toString() ?: "")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveProfileChanges(
                    etFullName.text.toString().trim(),
                    etBio.text.toString().trim(),
                    etSpecialization.text.toString().trim()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfileChanges(fullName: String, bio: String, specialization: String) {
        val currentUser = auth.currentUser ?: return
        
        if (fullName.isEmpty()) {
            Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBarProfile.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val updates = mapOf(
                    "fullName" to fullName,
                    "bio" to bio,
                    "specialization" to specialization,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update(updates)
                    .await()
                
                // Update local data and UI
                currentUserData = currentUserData?.toMutableMap()?.apply {
                    putAll(updates)
                } ?: updates
                
                updateProfileUI(currentUserData!!)
                
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarProfile.visibility = View.GONE
            }
        }
    }

    private fun showNotificationSettings() {
        Toast.makeText(requireContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showPrivacySettings() {
        Toast.makeText(requireContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showHelpAndSupport() {
        val options = arrayOf("FAQ", "Contact Support", "User Guide", "Report Issue")
        AlertDialog.Builder(requireContext())
            .setTitle("Help & Support")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(requireContext(), "FAQ coming soon", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(requireContext(), "Contact support coming soon", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(requireContext(), "User guide coming soon", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(requireContext(), "Report issue coming soon", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About")
            .setMessage("Educational Platform for Teachers\nVersion 1.0.0\n\nA comprehensive learning management system designed to help teachers create, manage, and deliver engaging educational content.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBackupOptions() {
        Toast.makeText(requireContext(), "Backup & sync coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showAnalyticsSettings() {
        Toast.makeText(requireContext(), "Analytics settings coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showSignOutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out? You will need to log in again to access your account.")
            .setPositiveButton("Sign Out") { _, _ ->
                performSignOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSignOut() {
        binding.progressBarProfile.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Sign out from Firebase
                auth.signOut()
                
                // Clear any cached data
                val sharedPrefs = requireContext().getSharedPreferences("teacher_cache", android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()
                
                // Navigate to login
                navigateToLogin()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Sign out failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBarProfile.visibility = View.GONE
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}