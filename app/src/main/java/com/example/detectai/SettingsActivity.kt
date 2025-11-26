package com.example.detectai

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ ADD THIS: Handle edge-to-edge insets
        val rootLayout = findViewById<LinearLayout>(R.id.settings_root_layout)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            HapticUtils.performLightTap(it)
            finish()
        }

        val darkModeSwitch = findViewById<SwitchMaterial>(R.id.dark_mode_switch)
        val clearHistoryButton = findViewById<LinearLayout>(R.id.clear_history_option)
        val changePasswordButton = findViewById<LinearLayout>(R.id.change_password_option)
        val deleteAccountButton = findViewById<LinearLayout>(R.id.delete_account_option)
        val appVersionText = findViewById<TextView>(R.id.app_version_text)

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        darkModeSwitch.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        darkModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            HapticUtils.performLightTap(buttonView)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        clearHistoryButton.setOnClickListener {
            HapticUtils.performMediumTap(it)
            showClearHistoryDialog()
        }

        changePasswordButton.setOnClickListener {
            HapticUtils.performMediumTap(it)
            showChangePasswordDialog()
        }

        deleteAccountButton.setOnClickListener {
            HapticUtils.performHeavyTap(this)
            showDeleteAccountDialog()
        }

        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            appVersionText.text = "Version $versionName"
        } catch (e: Exception) {
            appVersionText.text = "Version 1.0"
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear All History?")
            .setMessage("This will permanently delete all your detection history. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                HapticUtils.performHeavyTap(this)
                clearAllHistory()
            }
            .setNegativeButton("Cancel") { _, _ ->
                HapticUtils.performLightTap(findViewById(android.R.id.content))
            }
            .show()
    }

    private fun clearAllHistory() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("history")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        HapticUtils.performSuccessVibration(this)
                        Toast.makeText(this, "History cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        HapticUtils.performErrorVibration(this)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                HapticUtils.performErrorVibration(this)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.current_password_input)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.new_password_input)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.confirm_password_input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                HapticUtils.performMediumTap(findViewById(android.R.id.content))
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (newPassword != confirmPassword) {
                    HapticUtils.performErrorVibration(this)
                    Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    HapticUtils.performErrorVibration(this)
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel") { _, _ ->
                HapticUtils.performLightTap(findViewById(android.R.id.content))
            }
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        HapticUtils.performSuccessVibration(this)
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        HapticUtils.performErrorVibration(this)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                HapticUtils.performErrorVibration(this)
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account?")
            .setMessage("This will permanently delete your account and all your data. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                HapticUtils.performHeavyTap(this)
                showPasswordConfirmationDialog()
            }
            .setNegativeButton("Cancel") { _, _ ->
                HapticUtils.performLightTap(findViewById(android.R.id.content))
            }
            .show()
    }

    private fun showPasswordConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_password, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.password_input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Password")
            .setMessage("Enter your password to delete your account")
            .setView(dialogView)
            .setPositiveButton("Delete Account") { _, _ ->
                HapticUtils.performHeavyTap(this)
                val password = passwordInput.text.toString()
                deleteAccount(password)
            }
            .setNegativeButton("Cancel") { _, _ ->
                HapticUtils.performLightTap(findViewById(android.R.id.content))
            }
            .show()
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val userId = user.uid

        val credential = EmailAuthProvider.getCredential(email, password)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                db.collection("history")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        val batch = db.batch()
                        for (document in documents) {
                            batch.delete(document.reference)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                user.delete()
                                    .addOnSuccessListener {
                                        HapticUtils.performSuccessVibration(this)
                                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, LoginActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        HapticUtils.performErrorVibration(this)
                                        Toast.makeText(this, "Error deleting account: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                    }
            }
            .addOnFailureListener {
                HapticUtils.performErrorVibration(this)
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
            }
    }
}
