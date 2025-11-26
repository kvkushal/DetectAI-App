package com.example.detectai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var userEmailText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        userEmailText = view.findViewById(R.id.user_email_text)
        val settingsButton = view.findViewById<LinearLayout>(R.id.settings_button)
        val aboutUsButton = view.findViewById<LinearLayout>(R.id.about_us_button)
        val logoutButton = view.findViewById<Button>(R.id.logout_button)

        // ✅ NEW: Help Button
        val helpButton = view.findViewById<LinearLayout>(R.id.help_button)

        loadUserProfile()

        settingsButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        aboutUsButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)
        }

        // ✅ NEW: Help Button Click Listener
        helpButton.setOnClickListener {
            HapticUtils.performLightTap(it)
            val intent = Intent(requireContext(), HelpActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            HapticUtils.performHeavyTap(requireContext())
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userEmailText.text = currentUser.email ?: "No email available"
        } else {
            userEmailText.text = "Not logged in"
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
