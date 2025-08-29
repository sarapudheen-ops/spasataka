package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.diagnostic.autel.EcuInfo

class EcuInfoAdapter(
    private val ecuList: List<EcuInfo>
) : RecyclerView.Adapter<EcuInfoAdapter.EcuViewHolder>() {

    class EcuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ecuName: TextView = itemView.findViewById(R.id.tvEcuName)
        val ecuAddress: TextView = itemView.findViewById(R.id.tvEcuAddress)
        val ecuSoftware: TextView = itemView.findViewById(R.id.tvEcuSoftware)
        val ecuHardware: TextView = itemView.findViewById(R.id.tvEcuHardware)
        val ecuStatus: TextView = itemView.findViewById(R.id.tvEcuStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EcuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ecu_info, parent, false)
        return EcuViewHolder(view)
    }

    override fun onBindViewHolder(holder: EcuViewHolder, position: Int) {
        val ecu = ecuList[position]
        holder.ecuName.text = ecu.name
        holder.ecuAddress.text = ecu.address
        holder.ecuSoftware.text = ecu.software
        holder.ecuHardware.text = ecu.hardware
        holder.ecuStatus.text = ecu.status
    }

    override fun getItemCount(): Int = ecuList.size
}
