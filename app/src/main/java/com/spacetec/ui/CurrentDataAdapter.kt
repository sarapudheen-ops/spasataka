package com.spacetec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R

class CurrentDataAdapter : RecyclerView.Adapter<CurrentDataAdapter.DataViewHolder>() {

    private val dataList = mutableListOf<CurrentDataItem>()

    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val paramName: TextView = itemView.findViewById(R.id.tvParamName)
        val paramValue: TextView = itemView.findViewById(R.id.tvParamValue)
        val paramUnit: TextView = itemView.findViewById(R.id.tvParamUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_current_data, parent, false)
        return DataViewHolder(view)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val dataItem = dataList[position]
        holder.paramName.text = dataItem.name
        holder.paramValue.text = dataItem.value
        holder.paramUnit.text = dataItem.unit
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(newData: List<CurrentDataItem>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
    }
}
