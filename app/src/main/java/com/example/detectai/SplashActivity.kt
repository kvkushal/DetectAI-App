package com.example.detectai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen  // ✅ NEW IMPORT
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ NEW: Install splash screen BEFORE super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Find all the pieces for our "Iris Scan" animation
        val scanRing = findViewById<ImageView>(R.id.scan_ring)
        val logoEye = findViewById<ImageView>(R.id.splash_logo_eye)
        val logoText = findViewById<TextView>(R.id.splash_logo_text)

        // Load the animations
        val irisAnim = AnimationUtils.loadAnimation(this, R.anim.iris_expand_anim)
        val revealAnim = AnimationUtils.loadAnimation(this, R.anim.logo_reveal_anim)

        // Add the listener to hide the ring
        irisAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                scanRing.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Start the Iris Scan Animation Sequence
        logoEye.visibility = View.VISIBLE
        logoEye.startAnimation(revealAnim)
        logoText.visibility = View.VISIBLE
        logoText.startAnimation(revealAnim)

        Handler(Looper.getMainLooper()).postDelayed({
            scanRing.visibility = View.VISIBLE
            scanRing.startAnimation(irisAnim)
        }, 300)

        // Smart navigation with onboarding check
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user has completed onboarding
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val onboardingComplete = prefs.getBoolean("onboarding_complete", false)

            val intent = if (onboardingComplete) {
                // Onboarding done - check if user is logged in
                if (auth.currentUser != null) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, LoginActivity::class.java)
                }
            } else {
                // First time user - show onboarding
                Intent(this, OnboardingActivity::class.java)
            }

            startActivity(intent)
            finish() // Can't press back to splash screen
        }, 2000)
    }
}
