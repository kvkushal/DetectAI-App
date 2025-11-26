package com.example.detectai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loadingOverlay: View  // ✅ NEW
    private lateinit var loadingProgress: ProgressBar  // ✅ NEW

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showLoading(false)  // ✅ NEW: Hide loading on error
            Log.w("LoginActivity", "Google sign in failed", e)
            HapticUtils.performErrorVibration(this)
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Fix Status Bar Overlap
        val loginContainer = findViewById<View>(R.id.login_container)
        ViewCompat.setOnApplyWindowInsetsListener(loginContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize views
        emailInput = findViewById(R.id.emailEditText)
        passwordInput = findViewById(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupLinkText = findViewById<TextView>(R.id.signupLinkText)
        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)

        // ✅ NEW: Initialize loading views
        loadingOverlay = findViewById(R.id.loading_overlay)
        loadingProgress = findViewById(R.id.loading_progress)

        // Email/Password Login
        loginButton.setOnClickListener {
            HapticUtils.performMediumTap(it)
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithEmail(email, password)
        }

        // Google Sign-In
        googleSignInButton.setOnClickListener {
            HapticUtils.performMediumTap(it)
            signInWithGoogle()
        }

        // Navigate to Sign Up
        signupLinkText.setOnClickListener {
            HapticUtils.performLightTap(it)
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Forgot Password
        forgotPasswordText.setOnClickListener {
            HapticUtils.performLightTap(it)
            showForgotPasswordDialog()
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        showLoading(true)  // ✅ NEW

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)  // ✅ NEW

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        HapticUtils.performSuccessVibration(this)
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        HapticUtils.performErrorVibration(this)
                        Toast.makeText(
                            this,
                            "Please verify your email first",
                            Toast.LENGTH_SHORT
                        ).show()
                        auth.signOut()
                    }
                } else {
                    HapticUtils.performErrorVibration(this)
                    Toast.makeText(
                        this,
                        "Login failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun signInWithGoogle() {
        showLoading(true)  // ✅ NEW
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)  // ✅ NEW

                if (task.isSuccessful) {
                    HapticUtils.performSuccessVibration(this)
                    Toast.makeText(this, "Google sign in successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    HapticUtils.performErrorVibration(this)
                    Toast.makeText(
                        this,
                        "Authentication failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val resetEmailInput = dialogView.findViewById<TextInputEditText>(R.id.reset_email_input)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a password reset link")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                HapticUtils.performMediumTap(findViewById(android.R.id.content))
                val email = resetEmailInput.text.toString().trim()
                if (email.isEmpty()) {
                    HapticUtils.performErrorVibration(this)
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendPasswordResetEmail(email)
            }
            .setNegativeButton("Cancel") { _, _ ->
                HapticUtils.performLightTap(findViewById(android.R.id.content))
            }
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)  // ✅ NEW

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)  // ✅ NEW

                if (task.isSuccessful) {
                    HapticUtils.performSuccessVibration(this)
                    Toast.makeText(
                        this,
                        "Password reset email sent!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    HapticUtils.performErrorVibration(this)
                    Toast.makeText(
                        this,
                        "Failed to send reset email",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // ✅ NEW: Show/hide loading
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
    }
}
