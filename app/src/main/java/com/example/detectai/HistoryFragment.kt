package com.example.detectai

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var noHistoryText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.historyRecyclerView)
        loadingIndicator = view.findViewById(R.id.historyLoadingIndicator)
        noHistoryText = view.findViewById(R.id.noHistoryText)

        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(mutableListOf())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        // Regular click listener
        historyAdapter.setOnItemClickListener(object : HistoryAdapter.OnItemClickListener {
            override fun onItemClick(historyItem: HistoryItem) {
                if (historyItem.type == null) {
                    Log.e("HistoryFragment", "Clicked item has a null type. Cannot navigate.")
                    return
                }

                val bundle = bundleOf(
                    "type" to historyItem.type,
                    "result" to historyItem.result,
                    "originalData" to historyItem.originalData,
                    "detailedResult" to historyItem.detailedResult  // ✅ ADDED THIS LINE
                )

                val destination = if (historyItem.type == "Text") {
                    R.id.textDetectionFragment
                } else {
                    R.id.imageDetectionFragment
                }

                try {
                    val fragmentContainer = requireActivity().findViewById<View>(R.id.fragment_container)
                    val navController = Navigation.findNavController(fragmentContainer)
                    navController.navigate(destination, bundle)
                } catch (e: Exception) {
                    Log.e("HistoryFragment", "Navigation error: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Long click listener for delete
        historyAdapter.setOnItemLongClickListener(object : HistoryAdapter.OnItemLongClickListener {
            override fun onItemLongClick(historyItem: HistoryItem, position: Int) {
                showDeleteConfirmation(historyItem, position)
            }
        })
    }

    // Show delete confirmation dialog
    private fun showDeleteConfirmation(item: HistoryItem, position: Int) {
        val type = item.type ?: "Detection"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete History Item?")
            .setMessage("Are you sure you want to delete this $type detection?")
            .setPositiveButton("Delete") { _, _ ->
                deleteHistoryItem(item, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Delete item from Firestore and UI
    private fun deleteHistoryItem(item: HistoryItem, position: Int) {
        val documentId = item.documentId

        if (documentId == null) {
            Toast.makeText(requireContext(), "Error: Cannot delete item", Toast.LENGTH_SHORT).show()
            return
        }

        // Remove from UI immediately for better UX
        historyAdapter.removeItem(position)

        // Delete from Firestore
        db.collection("history")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                HapticUtils.performSuccessVibration(requireContext())
                showUndoSnackbar(item, position, documentId)
            }
            .addOnFailureListener { e ->
                HapticUtils.performErrorVibration(requireContext())
                Log.e("HistoryFragment", "Error deleting document", e)

                // Restore item in UI if deletion failed
                historyAdapter.restoreItem(item, position)
                Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show()
            }
    }

    // Show undo snackbar
    private fun showUndoSnackbar(item: HistoryItem, position: Int, documentId: String) {
        Snackbar.make(
            requireView(),
            "History item deleted",
            Snackbar.LENGTH_LONG
        ).setAction("UNDO") {
            // Restore in UI
            historyAdapter.restoreItem(item, position)

            // Restore in Firestore
            restoreToFirestore(item, documentId)

            HapticUtils.performLightTap(requireView())
        }.show()
    }

    // Restore item to Firestore
    private fun restoreToFirestore(item: HistoryItem, documentId: String) {
        val itemData = hashMapOf(
            "userId" to item.userId,
            "type" to item.type,
            "result" to item.result,
            "originalData" to item.originalData,
            "detailedResult" to item.detailedResult,
            "timestamp" to item.timestamp
        )

        db.collection("history")
            .document(documentId)
            .set(itemData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item restored", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("HistoryFragment", "Error restoring document", e)
                Toast.makeText(requireContext(), "Failed to restore item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadHistoryData() {
        loadingIndicator.visibility = View.VISIBLE
        noHistoryText.visibility = View.GONE
        recyclerView.visibility = View.GONE

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("History", "User not logged in.")
            loadingIndicator.visibility = View.GONE
            noHistoryText.text = "Please log in to see history."
            noHistoryText.visibility = View.VISIBLE
            return
        }

        db.collection("history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                loadingIndicator.visibility = View.GONE

                val items = documents.mapNotNull { doc ->
                    try {
                        val historyItem = doc.toObject(HistoryItem::class.java)
                        historyItem.timestampLong = historyItem.timestamp?.time
                        historyItem
                    } catch (e: Exception) {
                        Log.e("HistoryFragment", "Error parsing document ${doc.id}, skipping.", e)
                        null
                    }
                }

                if (items.isEmpty()) {
                    noHistoryText.text = "No history found."
                    noHistoryText.visibility = View.VISIBLE
                } else {
                    historyAdapter.updateData(items)
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                loadingIndicator.visibility = View.GONE
                noHistoryText.text = "Error loading history."
                noHistoryText.visibility = View.VISIBLE
                Log.w("History", "Error getting documents: ", exception)
            }
    }
}