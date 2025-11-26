package com.example.detectai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val historyList: MutableList<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var listener: OnItemClickListener? = null
    private var longClickListener: OnItemLongClickListener? = null  // ✅ NEW

    interface OnItemClickListener {
        fun onItemClick(historyItem: HistoryItem)
    }

    // ✅ NEW: Long click interface
    interface OnItemLongClickListener {
        fun onItemLongClick(historyItem: HistoryItem, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    // ✅ NEW: Set long click listener
    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        this.longClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.bind(item)
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<HistoryItem>) {
        historyList.clear()
        historyList.addAll(newList)
        notifyDataSetChanged()
    }

    // ✅ NEW: Remove item with animation
    fun removeItem(position: Int) {
        historyList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, historyList.size)
    }

    // ✅ NEW: Restore item (for undo)
    fun restoreItem(item: HistoryItem, position: Int) {
        historyList.add(position, item)
        notifyItemInserted(position)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.historyIcon)
        private val title: TextView = itemView.findViewById(R.id.historyTitle)
        private val result: TextView = itemView.findViewById(R.id.historyResult)
        private val date: TextView = itemView.findViewById(R.id.historyDate)

        init {
            // Regular click
            itemView.setOnClickListener {
                HapticUtils.performLightTap(it)
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(historyList[position])
                }
            }

            // ✅ NEW: Long click
            itemView.setOnLongClickListener {
                HapticUtils.performHeavyTap(it.context)
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    longClickListener?.onItemLongClick(historyList[position], position)
                }
                true  // Consume the event
            }
        }

        fun bind(item: HistoryItem) {
            val type = item.type ?: "Detection"
            val resultText = item.result ?: "No result"

            title.text = "$type Detection"
            result.text = "Result: $resultText"

            item.timestampLong?.let {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                date.text = sdf.format(Date(it))
                date.visibility = View.VISIBLE
            } ?: run {
                date.visibility = View.GONE
            }

            val iconRes = if (item.type == "Text") {
                R.drawable.ic_text
            } else {
                R.drawable.ic_image
            }
            icon.setImageResource(iconRes)
        }
    }
}
