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

class ScheduledEventsAdapter :
    ListAdapter<ScheduledEvent, ScheduledEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val btnSave: Button = itemView.findViewById(R.id.btnSave)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnEnroll: Button = itemView.findViewById(R.id.btnEnroll)

        fun bind(event: ScheduledEvent) {
            tvEventName.text = event.name
            tvDescription.text = event.description
            tvLocation.text = event.location
            tvStartTime.text = event.startTime
            tvEndTime.text = event.endTime
            tvCapacity.text = event.capacity

            // TODO: Implement button click listeners
            btnSave.setOnClickListener {
                // Handle save event
            }

            btnDelete.setOnClickListener {
                // Handle delete event
            }

            btnEnroll.setOnClickListener {
                // Handle enroll event
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