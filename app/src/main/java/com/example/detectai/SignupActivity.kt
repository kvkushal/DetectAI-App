package com.example.detectai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        // Code to Fix Status Bar Overlap
        val signupContainer = findViewById<View>(R.id.signup_container)
        ViewCompat.setOnApplyWindowInsetsListener(signupContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find all your views
        val emailInput = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordEditText)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val signupButton = findViewById<Button>(R.id.signupButton)
        val loginLinkText = findViewById<TextView>(R.id.loginLinkText) // ✅ ADD THIS

        // ✅ ADD: Login link click listener
        loginLinkText.setOnClickListener {
            HapticUtils.performLightTap(it)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close signup activity
        }

        // Sign Up Button
        signupButton.setOnClickListener {
            HapticUtils.performMediumTap(it) // ✅ ADD HAPTIC

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Validation
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                HapticUtils.performErrorVibration(this)
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                HapticUtils.performErrorVibration(this)
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                HapticUtils.performErrorVibration(this)
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // ✅ Send email verification
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    HapticUtils.performSuccessVibration(this)
                                    Toast.makeText(
                                        this,
                                        "Account created! Please check your email to verify your account.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Sign out the user until they verify
                                    auth.signOut()

                                    // Go back to login
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    HapticUtils.performErrorVibration(this)
                                    Toast.makeText(
                                        this,
                                        "Account created but failed to send verification email.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        HapticUtils.performErrorVibration(this)
                        Toast.makeText(
                            this,
                            "Sign up failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
