package com.codepalace.accelerometer.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent

class ScheduledEventsAdapter(
    private val onEnrollClick: (ScheduledEvent) -> Unit
) : ListAdapter<ScheduledEvent, ScheduledEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled_event, parent, false)
        return EventViewHolder(view, onEnrollClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        itemView: View,
        private val onEnrollClick: (ScheduledEvent) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val labelStart: TextView = itemView.findViewById(R.id.labelStart)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        private val labelEnd: TextView = itemView.findViewById(R.id.labelEnd)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val labelCapacity: TextView = itemView.findViewById(R.id.labelCapacity)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val btnEnroll: Button = itemView.findViewById(R.id.btnEnroll)

        fun bind(event: ScheduledEvent) {
            tvEventName.text = event.name
            tvDescription.text = event.description

            // Location (hide row if null/empty)
            tvLocation.text = event.location
            tvLocation.visibility = if (!event.location.isNullOrBlank()) View.VISIBLE else View.GONE

            // Start time (always visible)
            tvStartTime.text = event.startTime

            // End time (hide both label and value if null/empty)
            tvEndTime.text = event.endTime
            val hasEndTime = !event.endTime.isNullOrBlank()
            labelEnd.visibility = if (hasEndTime) View.VISIBLE else View.GONE
            tvEndTime.visibility = if (hasEndTime) View.VISIBLE else View.GONE

            // Capacity
            val capacityText = event.capacityDisplay   // e.g. "3/20" or null if unlimited
            labelCapacity.visibility = if (capacityText != null) View.VISIBLE else View.GONE
            tvCapacity.visibility    = if (capacityText != null) View.VISIBLE else View.GONE
            tvCapacity.text          = capacityText

            // Enroll button
            if (event.isEnrolled) {
                btnEnroll.text = "Sign out"
                btnEnroll.isEnabled = true
                btnEnroll.alpha = 0.85f
            } else {
                btnEnroll.text = "Enroll"
                btnEnroll.isEnabled = true
                btnEnroll.alpha = 1.0f
            }

            btnEnroll.setOnClickListener {
                onEnrollClick(event)
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<ScheduledEvent>() {
        override fun areItemsTheSame(oldItem: ScheduledEvent, newItem: ScheduledEvent) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ScheduledEvent, newItem: ScheduledEvent) =
            oldItem == newItem
    }
}