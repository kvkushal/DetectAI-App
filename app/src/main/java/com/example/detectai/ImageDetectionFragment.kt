package com.example.detectai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageDetectionFragment : Fragment(R.layout.fragment_image_detection) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedImageBitmap: Bitmap? = null
    private var isHistoryMode = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream)

                view?.findViewById<ImageView>(R.id.imagePreview)?.apply {
                    setImageBitmap(selectedImageBitmap)
                    visibility = View.VISIBLE
                }

                view?.findViewById<TextView>(R.id.placeholderText)?.visibility = View.GONE
                view?.findViewById<Button>(R.id.detectImageButton)?.isEnabled = true

                Toast.makeText(requireContext(), "Image loaded", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error loading image", e)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val uploadStateLayout = view.findViewById<View>(R.id.uploadStateLayout)
        val uploadCard = view.findViewById<MaterialCardView>(R.id.uploadCard)
        val placeholderText = view.findViewById<TextView>(R.id.placeholderText)
        val imagePreview = view.findViewById<ImageView>(R.id.imagePreview)
        val detectButton = view.findViewById<Button>(R.id.detectImageButton)
        val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingIndicator)
        val resultLayout = view.findViewById<View>(R.id.resultLayout)
        val tryAgainButton = resultLayout.findViewById<Button>(R.id.tryAgainButton)

        val type = arguments?.getString("type")
        val result = arguments?.getString("result")
        val originalData = arguments?.getString("originalData")
        val detailedResult = arguments?.getString("detailedResult")

        if (type == "Image" && result != null) {
            // History mode
            isHistoryMode = true

            uploadCard.visibility = View.VISIBLE
            uploadCard.isClickable = false
            detectButton.visibility = View.GONE
            uploadStateLayout.visibility = View.VISIBLE
            loadingIndicator.visibility = View.GONE
            resultLayout.visibility = View.VISIBLE

            val parts = result.split("%", limit = 2)
            val percent = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 50
            val predictionLabel = parts.getOrNull(1)?.trim() ?: "Unknown"

            val summaryText = resultLayout.findViewById<TextView>(R.id.summaryText)
            val reasonsText = resultLayout.findViewById<TextView>(R.id.reasonsText)
            val probabilityText = resultLayout.findViewById<TextView>(R.id.probabilityText)
            val probabilityIndicator = resultLayout.findViewById<CircularProgressIndicator>(R.id.probabilityIndicator)

            summaryText.text = predictionLabel
            probabilityText.text = "$percent%"
            probabilityIndicator.progress = percent
            reasonsText.text = detailedResult ?: "No detailed results available"

            loadHistoryImage(originalData, imagePreview, placeholderText)

            tryAgainButton.text = "Back to History"
            tryAgainButton.setOnClickListener {
                HapticUtils.performLightTap(it)
                findNavController().popBackStack()
            }

        } else {
            // Detection mode
            isHistoryMode = false

            uploadCard.visibility = View.VISIBLE
            uploadCard.isClickable = true
            detectButton.visibility = View.VISIBLE
            detectButton.isEnabled = false

            uploadCard.setOnClickListener {
                HapticUtils.performLightTap(it)
                pickImageLauncher.launch("image/*")
            }

            detectButton.setOnClickListener {
                HapticUtils.performMediumTap(it)
                selectedImageBitmap?.let { bitmap ->
                    analyzeImage(bitmap, uploadStateLayout, loadingIndicator, resultLayout)
                }
            }

            tryAgainButton.text = "Try Again"
            tryAgainButton.setOnClickListener {
                HapticUtils.performLightTap(it)

                resultLayout.visibility = View.GONE
                uploadStateLayout.visibility = View.VISIBLE
                uploadCard.visibility = View.VISIBLE
                detectButton.visibility = View.VISIBLE
                imagePreview.setImageURI(null)
                imagePreview.visibility = View.GONE
                placeholderText.visibility = View.VISIBLE
                detectButton.isEnabled = false
                selectedImageBitmap = null
            }
        }
    }

    private fun loadHistoryImage(originalData: String?, imageView: ImageView, placeholder: TextView) {
        originalData?.let { data ->
            when {
                data.length > 1000 -> {
                    loadImageFromBase64(data, imageView, placeholder)
                }
                data.startsWith("/data/") -> {
                    placeholder.text = "Image unavailable"
                    placeholder.visibility = View.VISIBLE
                    imageView.visibility = View.GONE
                }
                else -> {
                    placeholder.text = "Image not saved"
                    placeholder.visibility = View.VISIBLE
                    imageView.visibility = View.GONE
                }
            }
        } ?: run {
            placeholder.text = "No image data"
            placeholder.visibility = View.VISIBLE
            imageView.visibility = View.GONE
        }
    }

    private fun loadImageFromBase64(base64String: String, imageView: ImageView, placeholder: TextView) {
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            } else {
                throw Exception("Failed to decode image")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from history", e)
            placeholder.text = "Could not load image"
            placeholder.visibility = View.VISIBLE
            imageView.visibility = View.GONE
        }
    }

    private fun analyzeImage(
        bitmap: Bitmap,
        uploadStateLayout: View,
        loadingIndicator: ProgressBar,
        resultLayout: View
    ) {
        uploadStateLayout.visibility = View.GONE
        loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val apiBase64 = bitmapToBase64(bitmap, 80)

                val result = withContext(Dispatchers.IO) {
                    DetectionAPIClient.api.analyzeImage(ImageAnalyzeRequest(apiBase64))
                }

                HapticUtils.performSuccessVibration(requireContext())

                val thumbnailBase64 = createThumbnailBase64(bitmap)
                displayResult(result, resultLayout, loadingIndicator, thumbnailBase64)

            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)

                loadingIndicator.visibility = View.GONE
                uploadStateLayout.visibility = View.VISIBLE

                Toast.makeText(
                    requireContext(),
                    "Analysis failed. Please try again.",
                    Toast.LENGTH_LONG
                ).show()

                view?.findViewById<Button>(R.id.detectImageButton)?.isEnabled = true
            }
        }
    }

    private fun createThumbnailBase64(bitmap: Bitmap): String {
        val maxDimension = 800
        val ratio = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        val thumbnail = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return bitmapToBase64(thumbnail, 70)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun displayResult(
        result: ImageAnalyzeResponse,
        resultLayout: View,
        loadingIndicator: ProgressBar,
        imageBase64: String
    ) {
        val summaryText = resultLayout.findViewById<TextView>(R.id.summaryText)
        val reasonsText = resultLayout.findViewById<TextView>(R.id.reasonsText)
        val probabilityText = resultLayout.findViewById<TextView>(R.id.probabilityText)
        val probabilityIndicator = resultLayout.findViewById<CircularProgressIndicator>(R.id.probabilityIndicator)

        val probability = if (result.prediction == "AI") {
            result.ai_probability.toInt()
        } else {
            result.real_probability.toInt()
        }

        val predictionLabel = if (result.prediction == "AI") {
            "AI-Generated"
        } else {
            "Real Image"
        }

        val reasonsContent = buildString {
            append("Analysis:\n\n")

            result.explanations.forEachIndexed { index, explanation ->
                append("${index + 1}. ${explanation.indicator}\n")
                append("   ${explanation.description}\n")
                if (index < result.explanations.size - 1) append("\n")
            }

            append("\n\nOverall Confidence: ${result.confidence}")
        }

        summaryText.text = predictionLabel
        reasonsText.text = reasonsContent
        probabilityText.text = "$probability%"
        probabilityIndicator.progress = probability

        val historyResult = "$probability% $predictionLabel"
        saveHistoryItem("Image", historyResult, imageBase64, reasonsContent)

        loadingIndicator.visibility = View.GONE
        resultLayout.visibility = View.VISIBLE

        view?.findViewById<Button>(R.id.detectImageButton)?.isEnabled = true
    }

    private fun saveHistoryItem(
        type: String,
        result: String,
        originalData: String,
        detailedResult: String?
    ) {
        val userId = auth.currentUser?.uid ?: return

        val historyItem: Map<String, Any?> = mapOf(
            "userId" to userId,
            "type" to type,
            "result" to result,
            "originalData" to originalData,
            "detailedResult" to detailedResult,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("history")
            .add(historyItem)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Saved to history", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save history", e)
            }
    }

    companion object {
        private const val TAG = "ImageDetection"
    }
}
