package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R

class LiveDataAdapter : RecyclerView.Adapter<LiveDataAdapter.LiveDataViewHolder>() {

    private var dataItems = listOf<LiveDataItem>()

    class LiveDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pidName: TextView = itemView.findViewById(R.id.tvPidName)
        val pidValue: TextView = itemView.findViewById(R.id.tvPidValue)
        val pidUnit: TextView = itemView.findViewById(R.id.tvPidUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveDataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_live_data, parent, false)
        return LiveDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: LiveDataViewHolder, position: Int) {
        val item = dataItems[position]
        holder.pidName.text = item.name
        holder.pidValue.text = item.value
        holder.pidUnit.text = item.unit
    }

    override fun getItemCount(): Int = dataItems.size

    fun updateData(newData: List<LiveDataItem>) {
        dataItems = newData
        notifyDataSetChanged()
    }
}
