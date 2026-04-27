package com.codepalace.accelerometer.util

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.data.model.dto.ConstellationCultureResponse

class SkyCulturesAdapter(
    private var items: List<ConstellationCultureResponse>,
    private val onClick: (ConstellationCultureResponse) -> Unit,
    private val onSetClick: (Long) -> Unit
) : RecyclerView.Adapter<SkyCulturesAdapter.ViewHolder>() {

    private var expandedId: Long? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRegion: TextView = view.findViewById(R.id.tvRegion)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val expandSection: View = view.findViewById(R.id.expandSection)
        val btnSet: Button = view.findViewById(R.id.btnSet)
        val root: View = view.findViewById(R.id.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_culture, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isExpanded = item.id == expandedId
        val isCurrent = item.current

        Log.d("SkyAdapter", "Bind: ${item.name} | expanded=$isExpanded | current=$isCurrent")

        holder.tvName.text = item.name
        holder.tvRegion.text = item.region
        holder.tvDescription.text = item.description ?: ""

        // Expanded section
        holder.expandSection.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Background - this should now work reliably
        holder.root.setBackgroundResource(
            if (isCurrent) R.drawable.bg_selected_item
            else R.drawable.bg_profile_section
        )

        // Click to toggle expand
        holder.root.setOnClickListener {
            Log.d("SkyAdapter", "CLICK: ${item.name}")
            onClick(item)
        }

        // Set button - completely hide when it's already current
        if (isCurrent) {
            holder.btnSet.visibility = View.GONE
        } else {
            holder.btnSet.visibility = View.VISIBLE
            holder.btnSet.setOnClickListener {
                Log.d("SkyAdapter", "SET CURRENT: ${item.name}")
                onSetClick(item.id)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(newItems: List<ConstellationCultureResponse>, expandedId: Long?) {
        Log.d("SkyAdapter", "UPDATE: items=${newItems.size}, expanded=$expandedId")
        this.items = newItems
        this.expandedId = expandedId
        notifyDataSetChanged()
    }
}