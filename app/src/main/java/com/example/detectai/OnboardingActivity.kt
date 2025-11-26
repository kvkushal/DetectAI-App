package com.example.detectai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nextButton: Button
    private lateinit var skipButton: TextView

    private val onboardingItems = listOf(
        OnboardingItem(
            R.drawable.ic_image,
            "Detect AI Images",
            "Upload images to check if they're AI-generated or real"
        ),
        OnboardingItem(
            R.drawable.ic_text,
            "Analyze Text Content",
            "Paste any text to detect if it was written by AI or humans"
        ),
        OnboardingItem(
            R.drawable.ic_history,
            "Track Your History",
            "All your detections are saved for easy access anytime"
        ),
        OnboardingItem(
            R.drawable.ic_settings,
            "Free & Private",
            "100% free to use with no tracking or data collection"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // ✅ FIX STATUS BAR OVERLAP
        val onboardingContainer = findViewById<View>(R.id.onboarding_container)
        ViewCompat.setOnApplyWindowInsetsListener(onboardingContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        viewPager = findViewById(R.id.onboarding_viewpager)
        tabLayout = findViewById(R.id.tab_layout)
        nextButton = findViewById(R.id.next_button)
        skipButton = findViewById(R.id.skip_button)

        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        nextButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }

        skipButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            finishOnboarding()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == onboardingItems.size - 1) {
                    nextButton.text = "Get Started"
                } else {
                    nextButton.text = "Next"
                }
            }
        })
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_complete", true).apply()

        HapticUtils.performSuccessVibration(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

data class OnboardingItem(
    val image: Int,
    val title: String,
    val description: String
)
