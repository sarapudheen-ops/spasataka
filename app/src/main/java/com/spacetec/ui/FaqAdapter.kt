package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R

class FaqAdapter(
    private val faqItems: List<FaqItem>
) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.tvQuestion)
        val answerText: TextView = itemView.findViewById(R.id.tvAnswer)
        val expandIcon: TextView = itemView.findViewById(R.id.tvExpandIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val faqItem = faqItems[position]
        
        holder.questionText.text = faqItem.question
        holder.answerText.text = faqItem.answer
        
        // Set visibility based on expanded state
        holder.answerText.visibility = if (faqItem.isExpanded) View.VISIBLE else View.GONE
        holder.expandIcon.text = if (faqItem.isExpanded) "▼" else "▶"
        
        // Handle click to expand/collapse
        holder.itemView.setOnClickListener {
            faqItem.isExpanded = !faqItem.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = faqItems.size
}
