package com.example.detectai

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class TextDetectionFragment : Fragment(R.layout.fragment_text_detection) {

    private lateinit var textInputEditText: TextInputEditText
    private lateinit var detectButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val textInputStateLayout = view.findViewById<View>(R.id.textInputStateLayout)
        val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingIndicatorText)
        val resultLayout = view.findViewById<View>(R.id.resultLayoutText)
        val tryAgainButton = resultLayout.findViewById<Button>(R.id.tryAgainButton)
        val originalTextCard = resultLayout.findViewById<MaterialCardView>(R.id.originalTextCard)
        val originalTextView = resultLayout.findViewById<TextView>(R.id.originalTextView)

        textInputEditText = view.findViewById(R.id.textInputEditText)
        detectButton = view.findViewById(R.id.detectTextButton)

        val type = arguments?.getString("type")
        val result = arguments?.getString("result")
        val originalData = arguments?.getString("originalData")
        val detailedResult = arguments?.getString("detailedResult")

        val historyItem = if (type != null) {
            HistoryItem(
                type = type,
                result = result,
                originalData = originalData,
                detailedResult = detailedResult
            )
        } else {
            null
        }

        if (historyItem != null) {
            // HISTORY MODE: Display the saved results
            textInputStateLayout.visibility = View.GONE
            loadingIndicator.visibility = View.GONE
            resultLayout.visibility = View.VISIBLE

            // Show original text
            originalTextCard.visibility = View.VISIBLE
            originalTextView.text = historyItem.originalData ?: "No text available"

            val resultString = historyItem.result ?: "0% Unknown"
            val parts = resultString.split("%", limit = 2)
            val percent = parts.getOrNull(0)?.trim() ?: "0"
            val prediction = parts.getOrNull(1)?.trim() ?: "Unknown"

            val summaryText = resultLayout.findViewById<TextView>(R.id.summaryText)
            val reasonsText = resultLayout.findViewById<TextView>(R.id.reasonsText)
            val probabilityText = resultLayout.findViewById<TextView>(R.id.probabilityText)
            val probabilityIndicator = resultLayout.findViewById<CircularProgressIndicator>(R.id.probabilityIndicator)

            summaryText.text = prediction
            reasonsText.text = historyItem.detailedResult ?: "No detailed results available"
            probabilityText.text = "$percent%"
            probabilityIndicator.progress = percent.toIntOrNull() ?: 0

            // Change button to "Back to History"
            tryAgainButton.text = "Back to History"
            tryAgainButton.setOnClickListener {
                HapticUtils.performLightTap(it)
                findNavController().popBackStack()
            }
        } else {
            // DETECTION MODE: Set up for a new detection
            originalTextCard.visibility = View.GONE
            checkServerConnection()

            textInputEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    detectButton.isEnabled = s?.isNotBlank() == true
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            detectButton.setOnClickListener {
                HapticUtils.performMediumTap(it)

                val text = textInputEditText.text.toString().trim()

                if (text.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter some text", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (text.length < 10) {
                    Toast.makeText(requireContext(), "Text too short (minimum 10 characters)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                analyzeText(text, textInputStateLayout, loadingIndicator, resultLayout)
            }

            tryAgainButton.text = "Try Again"
            tryAgainButton.setOnClickListener {
                HapticUtils.performLightTap(it)

                resultLayout.visibility = View.GONE
                textInputStateLayout.visibility = View.VISIBLE
                textInputEditText.text?.clear()
                detectButton.isEnabled = false
            }
        }
    }

    private fun checkServerConnection() {
        lifecycleScope.launch {
            try {
                val health = withContext(Dispatchers.IO) {
                    DetectionAPIClient.api.checkHealth()
                }
                Log.d("TextDetection", "Server connected: ${health.status}")
            } catch (e: Exception) {
                Log.w("TextDetection", "Server not reachable: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Warning: AI server not reachable. Check network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun analyzeText(
        text: String,
        textInputStateLayout: View,
        loadingIndicator: ProgressBar,
        resultLayout: View
    ) {
        textInputStateLayout.visibility = View.GONE
        loadingIndicator.visibility = View.VISIBLE

        detectButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    DetectionAPIClient.api.analyzeText(AnalyzeRequest(text))
                }

                HapticUtils.performSuccessVibration(requireContext())

                displayResult(result, resultLayout, loadingIndicator, text)

            } catch (e: Exception) {
                Log.e("TextDetection", "Analysis failed", e)

                loadingIndicator.visibility = View.GONE
                textInputStateLayout.visibility = View.VISIBLE

                Toast.makeText(
                    requireContext(),
                    "Analysis failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                detectButton.isEnabled = true
            }
        }
    }

    private fun displayResult(
        result: AnalyzeResponse,
        resultLayout: View,
        loadingIndicator: ProgressBar,
        originalText: String
    ) {
        val summaryText = resultLayout.findViewById<TextView>(R.id.summaryText)
        val reasonsText = resultLayout.findViewById<TextView>(R.id.reasonsText)
        val probabilityText = resultLayout.findViewById<TextView>(R.id.probabilityText)
        val probabilityIndicator = resultLayout.findViewById<CircularProgressIndicator>(R.id.probabilityIndicator)

        val probability = if (result.prediction == "AI") {
            result.ai_probability.toInt()
        } else {
            result.human_probability.toInt()
        }

        val predictionLabel = if (result.prediction == "AI") {
            "AI-Generated"
        } else {
            "Human-Written"
        }

        // ✅ FIXED: Build sentence-based reasons with CORRECT confidence display
        val reasonsContent = buildString {
            append("Reasons:\n\n")

            if (result.prediction == "AI" && result.ai_indicators.isNotEmpty()) {
                result.ai_indicators.forEachIndexed { index, indicator ->
                    append("${index + 1}. \"${indicator.sentence}\"\n\n")
                    append("   ${indicator.reason}\n")
                    // ✅ Show AI confidence (as percentage)
                    append("   Confidence: ${(indicator.score * 100).toInt()}%\n")
                    if (index < result.ai_indicators.size - 1) append("\n")
                }
            } else if (result.prediction == "Human" && result.human_indicators.isNotEmpty()) {
                result.human_indicators.forEachIndexed { index, indicator ->
                    append("${index + 1}. \"${indicator.sentence}\"\n\n")
                    append("   ${indicator.reason}\n")
                    // ✅ FIX: For human indicators, show HUMAN confidence (100 - AI score)
                    append("   Confidence: ${((1 - indicator.score) * 100).toInt()}%\n")
                    if (index < result.human_indicators.size - 1) append("\n")
                }
            } else {
                append("No specific indicators found.")
            }

            append("\n\nOverall Confidence: ${result.confidence}")
        }

        summaryText.text = predictionLabel
        reasonsText.text = reasonsContent
        probabilityText.text = "$probability%"
        probabilityIndicator.progress = probability

        val historyResult = "$probability% $predictionLabel"
        saveHistoryItem("Text", historyResult, originalText, reasonsContent)

        loadingIndicator.visibility = View.GONE
        resultLayout.visibility = View.VISIBLE

        detectButton.isEnabled = true
    }

    private fun saveHistoryItem(
        type: String,
        result: String,
        originalData: String?,
        detailedResult: String? = null
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("History", "Error: User is not logged in, cannot save history.")
            return
        }

        val historyItem = hashMapOf(
            "userId" to userId,
            "type" to type,
            "result" to result,
            "originalData" to originalData,
            "detailedResult" to detailedResult,
            "timestamp" to Date()
        )

        db.collection("history")
            .add(historyItem)
            .addOnSuccessListener { Log.d("History", "History item saved successfully!") }
            .addOnFailureListener { e ->
                Log.w("History", "Error saving history item", e)
                Toast.makeText(requireContext(), "Error saving history", Toast.LENGTH_SHORT).show()
            }
    }
}