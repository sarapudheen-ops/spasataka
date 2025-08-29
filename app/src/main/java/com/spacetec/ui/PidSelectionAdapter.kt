package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R

class PidSelectionAdapter(
    private val onSelectionChanged: (PidInfo, Boolean) -> Unit
) : RecyclerView.Adapter<PidSelectionAdapter.PidViewHolder>() {

    private val pidList = mutableListOf<PidInfo>()
    private val selectedPids = mutableSetOf<String>()

    class PidViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxPid)
        val pidName: TextView = itemView.findViewById(R.id.tvPidName)
        val pidUnit: TextView = itemView.findViewById(R.id.tvPidUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PidViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pid_selection, parent, false)
        return PidViewHolder(view)
    }

    override fun onBindViewHolder(holder: PidViewHolder, position: Int) {
        val pid = pidList[position]
        
        holder.pidName.text = pid.name
        holder.pidUnit.text = pid.unit
        holder.checkBox.isChecked = selectedPids.contains(pid.id)
        
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPids.add(pid.id)
            } else {
                selectedPids.remove(pid.id)
            }
            onSelectionChanged(pid, isChecked)
        }
        
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = pidList.size

    fun updatePids(newPids: List<PidInfo>) {
        pidList.clear()
        pidList.addAll(newPids)
        notifyDataSetChanged()
    }

    fun getPidInfo(pidId: String): PidInfo? {
        return pidList.find { it.id == pidId }
    }
}
