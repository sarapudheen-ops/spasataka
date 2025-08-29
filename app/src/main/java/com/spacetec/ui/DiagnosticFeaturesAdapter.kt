package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.google.android.material.chip.Chip

class DiagnosticFeaturesAdapter(
    private val features: List<DiagnosticFeature>,
    private val onFeatureClick: (DiagnosticFeature) -> Unit
) : RecyclerView.Adapter<DiagnosticFeaturesAdapter.FeatureViewHolder>() {

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ivFeatureIcon)
        val title: TextView = itemView.findViewById(R.id.tvFeatureTitle)
        val description: TextView = itemView.findViewById(R.id.tvFeatureDescription)
        val connectionStatus: Chip = itemView.findViewById(R.id.chipConnectionStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnostic_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        
        holder.title.text = feature.title
        holder.description.text = feature.description
        holder.icon.setImageResource(feature.iconResId)
        
        if (feature.requiresConnection) {
            holder.connectionStatus.text = "Requires Connection"
            holder.connectionStatus.visibility = View.VISIBLE
        } else {
            holder.connectionStatus.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onFeatureClick(feature)
        }
    }

    override fun getItemCount(): Int = features.size
}
