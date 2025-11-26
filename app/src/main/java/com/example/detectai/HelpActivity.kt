package com.example.detectai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class HelpActivity : AppCompatActivity() {

    private val faqItems = listOf(
        FAQItem(
            "How does DetectAI work?",
            "DetectAI uses advanced machine learning models to analyze images and text. For images, it checks visual patterns and artifacts common in AI-generated content. For text, it analyzes writing patterns, consistency, and style."
        ),
        FAQItem(
            "Is DetectAI free to use?",
            "Yes! DetectAI is completely free with no hidden costs, subscriptions, or in-app purchases."
        ),
        FAQItem(
            "How accurate is the detection?",
            "Our AI detection model has high accuracy, but no system is 100% perfect. Results should be used as guidance rather than definitive proof."
        ),
        FAQItem(
            "What image formats are supported?",
            "DetectAI supports JPG, JPEG, PNG, and WebP image formats. Maximum file size is 10MB."
        ),
        FAQItem(
            "Is my data stored or shared?",
            "No. We prioritize your privacy. Images and text you analyze are processed temporarily and not stored on our servers. Your detection history is saved locally on your device only."
        ),
        FAQItem(
            "Can I delete my history?",
            "Yes! Go to the History tab, long-press any item to delete it, or use the 'Clear All History' option in Settings."
        ),
        FAQItem(
            "Why was email verification required?",
            "Email verification ensures account security and helps prevent spam accounts. It also allows you to recover your account if you forget your password."
        ),
        FAQItem(
            "Can I use DetectAI offline?",
            "Not at the moment. DetectAI needs an internet connection so our models can process the content and return accurate results."
        ),
        FAQItem(
            "How do I report a bug?",
            "You can report bugs or suggest features by tapping the 'Contact Us' button at the bottom of this screen."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Fix status bar overlap
        val helpContainer = findViewById<View>(R.id.help_container)
        ViewCompat.setOnApplyWindowInsetsListener(helpContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Setup toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.help_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            HapticUtils.performLightTap(it)
            finish()
        }

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.faq_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FAQAdapter(faqItems)

        // ✅ Setup Contact Us FAB
        val contactFab = findViewById<ExtendedFloatingActionButton>(R.id.contact_fab)
        contactFab.setOnClickListener {
            HapticUtils.performMediumTap(it)
            openContactDialog()
        }
    }

    private fun openContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_contact_us, null)

        val emailButton = dialogView.findViewById<Button>(R.id.email_button)
        val bugButton = dialogView.findViewById<Button>(R.id.bug_button)
        val featureButton = dialogView.findViewById<Button>(R.id.feature_button)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Contact Us")
            .setMessage("How can we help you?")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        emailButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            sendEmail("support@detectai.com", "Support Request")
            dialog.dismiss()
        }

        bugButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            sendEmail("support@detectai.com", "Bug Report")
            dialog.dismiss()
        }

        featureButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            sendEmail("support@detectai.com", "Feature Suggestion")
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun sendEmail(email: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            HapticUtils.performErrorVibration(this)
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}

data class FAQItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)
