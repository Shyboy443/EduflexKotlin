package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ed.utils.GooglePlayServicesHelper

class SignUpActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    
    private var selectedRole: String = "Student" // Default role
    
    companion object {
        private const val RC_SIGN_IN = 9001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Check Google Play Services availability before configuring Google Sign-In
        if (GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)) {
            configureGoogleSignIn()
        } else {
            // Disable Google Sign-In button if Google Play Services is not available
            disableGoogleSignIn()
            // Log the status for debugging
            GooglePlayServicesHelper.logGooglePlayServicesStatus(this)
        }
        
        setupRoleSelection()
        setupClickListeners()
    }
    
    private fun configureGoogleSignIn() {
        try {
            // Configure Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            android.util.Log.e("SignUpActivity", "Failed to configure Google Sign-In: ${e.message}", e)
            disableGoogleSignIn()
        }
    }
    
    private fun disableGoogleSignIn() {
        // Find and disable Google Sign-In button if it exists
        try {
            // This assumes you have a Google Sign-In button in your layout
            // You may need to adjust this based on your actual layout
            android.util.Log.w("SignUpActivity", "Google Play Services not available - Google Sign-In disabled")
        } catch (e: Exception) {
            android.util.Log.e("SignUpActivity", "Error disabling Google Sign-In: ${e.message}", e)
        }
    }
    
    private fun setupRoleSelection() {
        // Remove teacher role selection - only allow student registration
        // Set student as the only option and hide role selection
        selectedRole = "Student"
        
        // Role selection UI has been removed from layout
        // Only students can register through the signup form
    }

    private fun selectRole(role: String) {
        // Only allow student role selection
        selectedRole = "Student"
    }
    
    private fun setupClickListeners() {
        binding.btnCreateAccount.setOnClickListener {
            if (validateForm()) {
                createAccountWithEmail()
            }
        }
        
        binding.btnGoogleSignup.setOnClickListener {
            signInWithGoogle()
        }
        
        binding.tvLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun validateForm(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Reset errors
        binding.etFullName.error = null
        binding.etEmail.error = null
        binding.etPassword.error = null
        binding.etConfirmPassword.error = null
        
        // Validate full name
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            binding.etFullName.requestFocus()
            return false
        }
        
        // Validate email
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Please enter a valid email"
            binding.etEmail.requestFocus()
            return false
        }
        
        // Validate password
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return false
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return false
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Please confirm your password"
            binding.etConfirmPassword.requestFocus()
            return false
        }
        
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return false
        }
        
        // Validate terms acceptance
        if (!binding.cbTerms.isChecked) {
            Toast.makeText(this, "Please accept the Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun createAccountWithEmail() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val fullName = binding.etFullName.text.toString().trim()
        
        android.util.Log.d("SignUpActivity", "Starting account creation for email: $email, role: $selectedRole")
        
        // Show loading state
        showLoadingState(true)
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                android.util.Log.d("SignUpActivity", "Firebase Auth completed. Success: ${task.isSuccessful}")
                
                if (task.isSuccessful) {
                    // Sign up success, create user profile
                    val user = auth.currentUser
                    android.util.Log.d("SignUpActivity", "Current user: ${user?.uid}")
                    
                    user?.let {
                        android.util.Log.d("SignUpActivity", "Creating user profile for UID: ${it.uid}")
                        createUserProfile(it.uid, fullName, email, selectedRole)
                    } ?: run {
                        android.util.Log.e("SignUpActivity", "User is null after successful auth")
                        showLoadingState(false)
                        Toast.makeText(this, "Registration failed: User creation error", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Sign up failed
                    android.util.Log.e("SignUpActivity", "Firebase Auth failed: ${task.exception?.message}")
                    showLoadingState(false)
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true -> 
                            "This email is already registered. Please use a different email or try logging in."
                        task.exception?.message?.contains("weak password") == true -> 
                            "Password is too weak. Please use a stronger password."
                        task.exception?.message?.contains("invalid email") == true -> 
                            "Please enter a valid email address."
                        task.exception?.message?.contains("network error") == true -> 
                            "Network error. Please check your internet connection and try again."
                        else -> "Registration failed: ${task.exception?.message ?: "Unknown error"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SignUpActivity", "Firebase Auth failure: ${e.message}", e)
                showLoadingState(false)
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun signInWithGoogle() {
        // Check if Google Play Services is available before attempting sign-in
        if (!GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)) {
            val errorMessage = GooglePlayServicesHelper.getGooglePlayServicesErrorMessage(this)
            Toast.makeText(this, "$errorMessage Please use email registration.", Toast.LENGTH_LONG).show()
            return
        }
        
        if (!::googleSignInClient.isInitialized) {
            Toast.makeText(this, "Google Sign-In not available. Please try email registration.", Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            android.util.Log.e("SignUpActivity", "Failed to start Google Sign-In: ${e.message}", e)
            Toast.makeText(this, "Failed to start Google Sign-In. Please try email registration.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                } else {
                    Toast.makeText(this, "Google Sign-In failed: No account data received", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                android.util.Log.e("SignUpActivity", "Google Sign-In ApiException: ${e.statusCode} - ${e.message}", e)
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in was cancelled"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign-in failed. Please try again or use email registration."
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error. Please check your internet connection."
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in already in progress"
                    GoogleSignInStatusCodes.DEVELOPER_ERROR -> "Google Play Services configuration error. Please use email registration."
                    else -> "Google Sign-In error. Please try email registration instead."
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("SignUpActivity", "Unexpected error during Google Sign-In: ${e.message}", e)
                Toast.makeText(this, "Unexpected error during Google Sign-In. Please try email registration.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        // Show loading state for Google sign-in
        showLoadingState(true, "Signing in with Google...")
        
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Check if user already exists in Firestore
                        firestore.collection("users").document(it.uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // New user, create profile
                                    createUserProfile(
                                        it.uid,
                                        account.displayName ?: "",
                                        account.email ?: "",
                                        selectedRole
                                    )
                                } else {
                                    // Existing user, navigate to home
                                    showLoadingState(false)
                                    navigateToHome()
                                }
                            }
                            .addOnFailureListener { e ->
                                showLoadingState(false)
                                Toast.makeText(this, "Failed to check user profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } ?: run {
                        showLoadingState(false)
                        Toast.makeText(this, "Google sign-in failed: User data not available", Toast.LENGTH_LONG).show()
                    }
                } else {
                    showLoadingState(false)
                    val errorMessage = when {
                        task.exception?.message?.contains("network error") == true -> 
                            "Network error. Please check your internet connection and try again."
                        task.exception?.message?.contains("account-exists-with-different-credential") == true -> 
                            "An account already exists with this email using a different sign-in method."
                        else -> "Google authentication failed: ${task.exception?.message ?: "Unknown error"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun createUserProfile(uid: String, fullName: String, email: String, role: String) {
        android.util.Log.d("SignUpActivity", "Creating user profile - UID: $uid, Name: $fullName, Email: $email, Role: $role")
        
        val userProfile = hashMapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "role" to role,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection("users").document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                android.util.Log.d("SignUpActivity", "User profile created successfully")
                showLoadingState(false)
                navigateToHome()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SignUpActivity", "Failed to create user profile: ${e.message}", e)
                showLoadingState(false)
                Toast.makeText(this, "Failed to create user profile: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Delete the Firebase Auth user if profile creation fails
                auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        android.util.Log.d("SignUpActivity", "Firebase Auth user deleted after profile creation failure")
                    } else {
                        android.util.Log.e("SignUpActivity", "Failed to delete Firebase Auth user: ${deleteTask.exception?.message}")
                    }
                }
            }
    }
    
    private fun navigateToHome() {
        android.util.Log.d("SignUpActivity", "Navigating to home for role: $selectedRole")
        
        try {
            // Since only students can register through signup, always navigate to student dashboard
            val intent = Intent(this, StudentDashboardActivity::class.java)
            android.util.Log.d("SignUpActivity", "Creating intent for StudentDashboardActivity")
            
            android.util.Log.d("SignUpActivity", "Starting activity and clearing task stack")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            android.util.Log.e("SignUpActivity", "Error navigating to dashboard: ${e.message}", e)
            showLoadingState(false)
            Toast.makeText(this, "Navigation error. Please try logging in again.", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showLoadingState(isLoading: Boolean, customMessage: String = "Creating Account...") {
        runOnUiThread {
            if (isLoading) {
                // Disable all interactive elements during loading
                binding.btnCreateAccount.isEnabled = false
                binding.btnCreateAccount.text = customMessage
                binding.btnGoogleSignup.isEnabled = false
                binding.etEmail.isEnabled = false
                binding.etPassword.isEnabled = false
                binding.etConfirmPassword.isEnabled = false
                binding.etFullName.isEnabled = false
                binding.cbTerms.isEnabled = false
                
                // Show progress indicator
                binding.btnCreateAccount.alpha = 0.6f
                binding.btnGoogleSignup.alpha = 0.6f
            } else {
                // Re-enable all interactive elements
                binding.btnCreateAccount.isEnabled = true
                binding.btnCreateAccount.text = "Create Account"
                binding.btnGoogleSignup.isEnabled = true
                binding.etEmail.isEnabled = true
                binding.etPassword.isEnabled = true
                binding.etConfirmPassword.isEnabled = true
                binding.etFullName.isEnabled = true
                binding.cbTerms.isEnabled = true
                
                // Restore normal appearance
                binding.btnCreateAccount.alpha = 1.0f
                binding.btnGoogleSignup.alpha = 1.0f
            }
        }
    }
}