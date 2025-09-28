package com.example.ed.ui.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.ed.LoginActivity
import com.example.ed.R
import com.example.ed.SettingsActivity
import com.example.ed.databinding.FragmentStudentSettingsBinding
import com.example.ed.ThemeManager
import com.google.firebase.auth.FirebaseAuth

class StudentSettingsFragment : Fragment() {

    private var _binding: FragmentStudentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        setupUserProfile()
        setupClickListeners()
        setupThemeToggle()
    }

    private fun setupUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.tvUserName.text = currentUser.displayName ?: "Student"
            binding.tvUserEmail.text = currentUser.email ?: ""
            
            // Load profile picture
            if (currentUser.photoUrl != null) {
                Glide.with(this)
                    .load(currentUser.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivProfilePicture)
            } else {
                binding.ivProfilePicture.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun setupThemeToggle() {
        // Set current theme state
        binding.switchDarkMode.isChecked = ThemeManager.isDarkMode(requireContext())
        
        // Set up theme toggle listener
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val newTheme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(requireContext(), newTheme)
            ThemeManager.applyTheme(newTheme)
            // Restart activity to apply theme
            requireActivity().recreate()
        }
    }

    private fun setupClickListeners() {
        // Profile section click
        binding.layoutProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile editing coming soon", Toast.LENGTH_SHORT).show()
        }

        // Account settings
        binding.layoutAccountSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        // Notifications
        binding.layoutNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // Privacy
        binding.layoutPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // Help & Support
        binding.layoutHelp.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }

        // About
        binding.layoutAbout.setOnClickListener {
            showAboutDialog()
        }

        // Logout
        binding.layoutLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About")
            .setMessage("Educational App\nVersion 1.0.0\n\nA comprehensive learning management system for students and teachers.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        
        // Clear any cached data
        val sharedPrefs = requireContext().getSharedPreferences("student_analytics_cache", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        
        // Navigate to login
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