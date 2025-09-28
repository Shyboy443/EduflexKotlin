package com.example.ed

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivityLoginBinding
import com.example.ed.services.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.GoogleAuthProvider
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme before setting content view
        ThemeManager.applyCurrentTheme(this)
        
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize Firebase and SessionManager
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            sessionManager = SessionManager.getInstance(this)

            // Check if user is already logged in with valid session
            if (sessionManager.isSessionValid()) {
                val userRole = sessionManager.getUserRole()
                if (userRole != null) {
                    navigateToRoleDashboard(userRole)
                    return
                }
            }

            // Configure Google Sign-In with error handling
            try {
                // Check Google Play Services availability first
                val googleApiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
                
                if (resultCode != ConnectionResult.SUCCESS) {
                    // Disable Google Sign-In if Google Play Services is not available
                    binding.btnGoogleSignin.isEnabled = false
                    binding.btnGoogleSignin.alpha = 0.5f
                    android.util.Log.w("LoginActivity", "Google Play Services not available - Google Sign-In disabled")
                } else {
                    val webClientId = getString(R.string.default_web_client_id)
                    if (webClientId.contains("placeholder") || webClientId.isEmpty()) {
                        // Disable Google Sign-In if not properly configured
                        binding.btnGoogleSignin.isEnabled = false
                        binding.btnGoogleSignin.alpha = 0.5f
                        Toast.makeText(this, "Google Sign-In not configured", Toast.LENGTH_SHORT).show()
                    } else {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        googleSignInClient = GoogleSignIn.getClient(this, gso)
                    }
                }
            } catch (e: Exception) {
                // Disable Google Sign-In on configuration error
                binding.btnGoogleSignin.isEnabled = false
                binding.btnGoogleSignin.alpha = 0.5f
                android.util.Log.e("LoginActivity", "Google Sign-In configuration error: ${e.message}", e)
                Toast.makeText(this, "Google Sign-In configuration error", Toast.LENGTH_SHORT).show()
            }

            // Configure Facebook Login with error handling
            try {
                callbackManager = CallbackManager.Factory.create()
            } catch (e: Exception) {
                // Disable Facebook Sign-In on configuration error
                binding.btnFacebookSignin.isEnabled = false
                binding.btnFacebookSignin.alpha = 0.5f
                Toast.makeText(this, "Facebook Sign-In configuration error", Toast.LENGTH_SHORT).show()
            }

            // Setup click listeners for unified login
            setupClickListeners()
            
            // Add fade in animation to the main content
            try {
                binding.root.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in))
            } catch (e: Exception) {
                // Continue without animation if it fails
            }

            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing login screen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private var selectedRole = "Student" // Default role
    
    private fun setupClickListeners() {
        // Remove role selection buttons - unified login
        binding.btnLogin.setOnClickListener { doLogin() }

        binding.tvForgot.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first.", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email).addOnCompleteListener {
                    Toast.makeText(this,
                        if (it.isSuccessful) "Reset email sent." else it.exception?.localizedMessage ?: "Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        
        // Add long press to create test users (for development only)
        binding.tvSignup.setOnLongClickListener {
            if (BuildConfig.DEBUG) {
                showCreateTestUsersDialog()
            }
            true
        }

        // Google Sign-In button
        binding.btnGoogleSignin.setOnClickListener {
            signInWithGoogle()
        }

        // Facebook Sign-In button
        binding.btnFacebookSignin.setOnClickListener {
            signInWithFacebook()
        }

        // Remove admin access link - admins login with regular email/password
        // Admin access is handled through role-based authentication after login
    }

    private fun doLogin() {
        try {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val pass  = binding.etPassword.text?.toString()?.trim().orEmpty()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Email and password required.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!com.example.ed.utils.NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_LONG).show()
                return
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Signing in..."

            // Log authentication attempt for debugging
            android.util.Log.d("LoginActivity", "Attempting login for email: $email")
            
            // Proceed directly with authentication (App Check temporarily disabled)
            performAuthentication(email, pass)
        } catch (e: Exception) {
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Login"
            Toast.makeText(this, "Unexpected error during login: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            android.util.Log.e("LoginActivity", "Unexpected error in doLogin", e)
        }
    }
    
    private fun performAuthentication(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Login"
            
            android.util.Log.d("LoginActivity", "Authentication task completed. Success: ${task.isSuccessful}")
            
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    android.util.Log.d("LoginActivity", "User authenticated successfully: ${user.uid}")
                    // Get user role from database and navigate accordingly
                    getUserRoleAndNavigate(user.uid)
                } else {
                    android.util.Log.e("LoginActivity", "Authentication succeeded but user is null")
                    Toast.makeText(this, "Authentication failed - no user", Toast.LENGTH_SHORT).show()
                }
            } else {
                android.util.Log.e("LoginActivity", "Authentication failed", task.exception)
                handleAuthenticationError(task.exception)
            }
        }.addOnFailureListener { e ->
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Login"
            android.util.Log.e("LoginActivity", "Authentication failure callback", e)
            handleAuthenticationError(e)
        }
    }

    private fun handleAuthenticationError(exception: Exception?) {
        val errorMessage = when {
            exception is com.google.firebase.FirebaseNetworkException -> {
                "A network error occurred. Please check your internet connection and try again."
            }
            exception?.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> {
                // Check if user exists in Firestore but not in Auth
                checkUserInFirestore(binding.etEmail.text?.toString()?.trim() ?: "")
                "Invalid email or password. The user may not exist in Firebase Auth.\n\nPlease use the 'Reset Password' option or Sign Up to create your account."
            }
            exception?.message?.contains("USER_NOT_FOUND") == true -> {
                "No account found with this email address. Please sign up first."
            }
            exception?.message?.contains("WRONG_PASSWORD") == true -> {
                "Incorrect password. Please try again or reset your password."
            }
            exception?.message?.contains("USER_DISABLED") == true -> {
                "This account has been disabled. Please contact support."
            }
            exception?.message?.contains("TOO_MANY_REQUESTS") == true -> {
                "Too many failed login attempts. Please try again later."
            }
            exception?.message?.contains("NETWORK_ERROR") == true -> {
                "Network error. Please check your internet connection and try again."
            }
            exception?.message?.contains("INVALID_EMAIL") == true -> {
                "Invalid email format. Please enter a valid email address."
            }
            exception?.message?.contains("EMAIL_NOT_VERIFIED") == true -> {
                "Please verify your email address before signing in."
            }
            else -> {
                "Login failed: ${exception?.localizedMessage ?: "Unknown error occurred"}\n\nIf this persists, it might be due to Firebase App Check configuration."
            }
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        
        // Log the error for debugging
        android.util.Log.e("LoginActivity", "Authentication error: ${exception?.message}", exception)
        
        // Additional debugging information
        android.util.Log.e("LoginActivity", "Full exception details:", exception)
    }

    private fun getUserRoleAndNavigate(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userRole = document.getString("role") ?: "Student"
                    val userEmail = document.getString("email") ?: auth.currentUser?.email ?: ""
                    val userName = document.getString("fullName") ?: auth.currentUser?.displayName ?: ""
                    
                    // Create session with SessionManager
                    sessionManager.createSession(uid, userRole, userEmail, userName)
                    
                    navigateToRoleDashboard(userRole)
                } else {
                    // User document doesn't exist, redirect to registration or create default
                    Toast.makeText(this, "User profile not found. Please complete registration.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, SignUpActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving user profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                // Default to student dashboard as fallback
                sessionManager.createSession(uid, "Student", auth.currentUser?.email ?: "", auth.currentUser?.displayName ?: "")
                navigateToRoleDashboard("Student")
            }
    }

    private fun showCreateTestUsersDialog() {
        val options = arrayOf("Admin", "Teacher", "Student", "Create All Test Users")
        
        AlertDialog.Builder(this)
            .setTitle("Create Test Users")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createTestUser("admin@gmail.com", "admin123", "Admin", "Admin User")
                    1 -> createTestUser("teacher@gmail.com", "teacher123", "Teacher", "Teacher User")
                    2 -> createTestUser("student@gmail.com", "student123", "Student", "Student User")
                    3 -> createAllTestUsers()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createAllTestUsers() {
        // Create all three test users
        createTestUser("admin@gmail.com", "admin123", "Admin", "Admin User")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            createTestUser("teacher@gmail.com", "teacher123", "Teacher", "Teacher User")
        }, 1000)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            createTestUser("student@gmail.com", "student123", "Student", "Student User")
        }, 2000)
    }
    
    private fun createTestUser(email: String, password: String, role: String, fullName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Also update Firestore
                    val userData = hashMapOf(
                        "email" to email,
                        "fullName" to fullName,
                        "role" to role,
                        "uid" to user.uid,
                        "provider" to "email",
                        "createdAt" to System.currentTimeMillis(),
                        "profileCompleted" to true,
                        "isActive" to true
                    )
                    
                    db.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "$role user created successfully!\nEmail: $email\nPassword: $password", Toast.LENGTH_LONG).show()
                            android.util.Log.d("LoginActivity", "Test user created: $email with role: $role")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "User created but Firestore update failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("already in use") == true) {
                    // User exists, try to sign in instead
                    Toast.makeText(this, "User already exists. Try logging in with:\nEmail: $email\nPassword: $password", Toast.LENGTH_LONG).show()
                    android.util.Log.d("LoginActivity", "User already exists: $email")
                } else {
                    Toast.makeText(this, "Failed to create user: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("LoginActivity", "Failed to create user: $email", e)
                }
            }
    }

    private fun checkUserInFirestore(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.first()
                    val role = userDoc.getString("role") ?: "Unknown"
                    val fullName = userDoc.getString("fullName") ?: "User"
                    
                    android.util.Log.d("LoginActivity", "User found in Firestore: $fullName ($role)")
                    android.util.Log.d("LoginActivity", "But user NOT found in Firebase Auth!")
                    
                    // Show dialog to help user
                    AlertDialog.Builder(this)
                        .setTitle("Account Migration Needed")
                        .setMessage("Your account exists in our database but needs to be activated in the authentication system.\n\nUser: $fullName\nRole: $role\n\nWould you like to:\n1. Reset your password (recommended)\n2. Create a new account with this email")
                        .setPositiveButton("Reset Password") { _, _ ->
                            auth.sendPasswordResetEmail(email).addOnCompleteListener {
                                Toast.makeText(this,
                                    if (it.isSuccessful) "Password reset email sent to $email" 
                                    else "Failed to send reset email: ${it.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .setNegativeButton("Sign Up") { _, _ ->
                            startActivity(Intent(this, SignUpActivity::class.java))
                        }
                        .show()
                } else {
                    android.util.Log.d("LoginActivity", "User NOT found in Firestore either")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("LoginActivity", "Error checking Firestore: ${e.message}")
            }
    }

    private fun navigateToRoleDashboard(role: String) {
        try {
            val intent = when (role) {
                "Student" -> Intent(this, StudentDashboardFragmentActivity::class.java)
                "Teacher" -> Intent(this, TeacherDashboardActivity::class.java)
                "Admin" -> Intent(this, AdminDashboardActivity::class.java)
                else -> Intent(this, StudentDashboardFragmentActivity::class.java) // Default to student
            }
            
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            val fallbackIntent = Intent(this, HomeActivity::class.java)
            fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(fallbackIntent)
            finish()
        }
    }

    private fun signInWithGoogle() {
        try {
            if (!::googleSignInClient.isInitialized) {
                Toast.makeText(this, "Google Sign-In not available", Toast.LENGTH_SHORT).show()
                return
            }
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithFacebook() {
        try {
            if (!::callbackManager.isInitialized) {
                Toast.makeText(this, "Facebook Sign-In not available", Toast.LENGTH_SHORT).show()
                return
            }
            
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        handleFacebookAccessToken(loginResult.accessToken)
                    }

                    override fun onCancel() {
                        Toast.makeText(this@LoginActivity, "Facebook login cancelled", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(exception: FacebookException) {
                        Toast.makeText(this@LoginActivity, "Facebook login error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })

            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
        } catch (e: Exception) {
            Toast.makeText(this, "Facebook Sign-In error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFacebookAccessToken(token: com.facebook.AccessToken) {
        try {
            val credential = FacebookAuthProvider.getCredential(token.token)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            // For social login, check if user exists, if not create with default role
                            db.collection("users").document(it.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val userRole = document.getString("role") ?: "Student"
                                        val userEmail = document.getString("email") ?: it.email ?: ""
                                        val userName = document.getString("fullName") ?: it.displayName ?: ""
                                        
                                        // Create session with SessionManager
                                        sessionManager.createSession(it.uid, userRole, userEmail, userName)
                                        
                                        navigateToRoleDashboard(userRole)
                                    } else {
                                        // New user, create profile with default Student role
                                        val userData = hashMapOf(
                                            "uid" to it.uid,
                                            "email" to it.email,
                                            "displayName" to it.displayName,
                                            "role" to "Student", // Default role for social login
                                            "provider" to "facebook"
                                        )
                                        db.collection("users").document(it.uid)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                sessionManager.createSession(user.uid, "Student", user.email ?: "", user.displayName ?: "")
                                                navigateToRoleDashboard("Student")
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error saving user data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                sessionManager.createSession(user.uid, "Student", user.email ?: "", user.displayName ?: "")
                                                navigateToRoleDashboard("Student")
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error checking user profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    sessionManager.createSession(it.uid, "Student", it.email ?: "", it.displayName ?: "")
                                    navigateToRoleDashboard("Student")
                                }
                        }
                    } else {
                        Toast.makeText(this, "Facebook authentication failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Facebook authentication error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        try {
            // Facebook callback
            if (::callbackManager.isInitialized) {
                callbackManager.onActivityResult(requestCode, resultCode, data)
            }
            
            // Google Sign-In result
            if (requestCode == RC_SIGN_IN) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Google sign in error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Activity result error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            // For social login, check if user exists, if not create with default role
                            db.collection("users").document(it.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val userRole = document.getString("role") ?: "Student"
                                        val userEmail = document.getString("email") ?: it.email ?: ""
                                        val userName = document.getString("fullName") ?: it.displayName ?: ""
                                        
                                        // Create session with SessionManager
                                        sessionManager.createSession(it.uid, userRole, userEmail, userName)
                                        
                                        navigateToRoleDashboard(userRole)
                                    } else {
                                        // New user, create profile with default Student role
                                        val userData = hashMapOf(
                                            "uid" to it.uid,
                                            "email" to it.email,
                                            "fullName" to it.displayName,
                                            "role" to "Student", // Default role for social login
                                            "provider" to "google"
                                        )
                                        db.collection("users").document(it.uid)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                sessionManager.createSession(user.uid, "Student", user.email ?: "", user.displayName ?: "")
                                                navigateToRoleDashboard("Student")
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error saving user data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                sessionManager.createSession(user.uid, "Student", user.email ?: "", user.displayName ?: "")
                                                navigateToRoleDashboard("Student")
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error checking user profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    sessionManager.createSession(it.uid, "Student", it.email ?: "", it.displayName ?: "")
                                    navigateToRoleDashboard("Student")
                                }
                        }
                    } else {
                        Toast.makeText(this, "Google authentication failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Google authentication error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
