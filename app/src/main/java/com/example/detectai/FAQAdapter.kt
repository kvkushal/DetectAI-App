package com.example.detectai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class FAQAdapter(private val items: List<FAQItem>) :
    RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    inner class FAQViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.faq_card)
        val question: TextView = view.findViewById(R.id.faq_question)
        val answer: TextView = view.findViewById(R.id.faq_answer)
        val expandIcon: ImageView = view.findViewById(R.id.expand_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FAQViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val item = items[position]
        holder.question.text = item.question
        holder.answer.text = item.answer

        // Show/hide answer
        holder.answer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        holder.expandIcon.rotation = if (item.isExpanded) 180f else 0f

        // Click to expand/collapse
        holder.card.setOnClickListener {
            HapticUtils.performLightTap(it)
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = items.size
}
