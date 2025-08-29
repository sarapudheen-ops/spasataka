package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R

class QuickScanResultsAdapter(
    private val results: List<ScanResult>
) : RecyclerView.Adapter<QuickScanResultsAdapter.ResultViewHolder>() {

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ecuName: TextView = itemView.findViewById(R.id.tvEcuName)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val dtcCount: TextView = itemView.findViewById(R.id.tvDtcCount)
        val responseTime: TextView = itemView.findViewById(R.id.tvResponseTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]
        holder.ecuName.text = result.ecuName
        holder.status.text = result.status
        holder.dtcCount.text = "${result.dtcCount} DTCs"
        holder.responseTime.text = result.responseTime
    }

    override fun getItemCount(): Int = results.size
}
