package com.codepalace.accelerometer.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.data.model.dto.ChatRoomUi
import com.google.android.material.button.MaterialButton

class ChatRoomAdapter(
    private val onClick: (ChatRoomUi) -> Unit,
    private val onSubscribeClick: (ChatRoomUi) -> Unit,
    private val onUnsubscribeClick: (ChatRoomUi) -> Unit
) : ListAdapter<ChatRoomUi, ChatRoomAdapter.ViewHolder>(ChatRoomDiffCallback()) {

    private val expandedRooms = mutableSetOf<Long>() // rooms where Unsubscribe button is shown

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val badge: TextView = itemView.findViewById(R.id.badge)
        private val btnAction: MaterialButton = itemView.findViewById(R.id.btnAction)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(getItem(position))
                }
            }

            // Long press shows Unsubscribe button (only in normal mode)
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val room = getItem(position)
                    if (room.isSubscribed) {
                        if (expandedRooms.contains(room.id)) {
                            expandedRooms.remove(room.id)
                        } else {
                            expandedRooms.add(room.id)
                        }
                        notifyItemChanged(position)
                    }
                }
                true
            }

            btnAction.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val room = getItem(position)
                    if (!room.isSubscribed) {
                        onSubscribeClick(room)
                    } else {
                        onUnsubscribeClick(room)
                        expandedRooms.remove(room.id) // hide button after click
                    }
                }
            }
        }

        fun bind(room: ChatRoomUi) {
            tvName.text = room.name

            // Badge
            if (room.isSubscribed && room.unreadCount > 0) {
                badge.text = room.unreadCount.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }

            // Action button logic
            when {
                !room.isSubscribed -> {
                    // Search mode - Subscribe button
                    btnAction.text = "Subscribe"
                    btnAction.visibility = View.VISIBLE
                }
                expandedRooms.contains(room.id) -> {
                    // Normal mode + long pressed - Unsubscribe button
                    btnAction.text = "Unsubscribe"
                    btnAction.visibility = View.VISIBLE
                }
                else -> {
                    btnAction.visibility = View.GONE
                }
            }
        }
    }

    private class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoomUi>() {
        override fun areItemsTheSame(oldItem: ChatRoomUi, newItem: ChatRoomUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatRoomUi, newItem: ChatRoomUi): Boolean =
            oldItem == newItem
    }
}