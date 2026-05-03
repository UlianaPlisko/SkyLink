package com.codepalace.accelerometer.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.data.model.dto.ChatListItem
import com.codepalace.accelerometer.data.model.dto.MessageUi
class MessageAdapter : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatListItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_DATE_HEADER = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatListItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is ChatListItem.Message -> {
                if ((getItem(position) as ChatListItem.Message).message.isFromCurrentUser) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false))
            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false))
            else -> DateHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is ChatListItem.Message -> {
                if (item.message.isFromCurrentUser) {
                    (holder as SentViewHolder).bind(item.message)
                } else {
                    (holder as ReceivedViewHolder).bind(item.message)
                }
            }
        }
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageSent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeSent)

        fun bind(message: MessageUi) {
            tvMessage.text = message.content
            tvTime.text = message.time
        }
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatarInitials)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageReceived)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeReceived)

        fun bind(message: MessageUi) {
            tvMessage.text = message.content
            tvTime.text = message.time

            // Initials from sender name (or email)
            val name = message.senderDisplayName ?: "?"
            val initials = name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }
            tvAvatar.text = if (initials.length > 2) initials.take(2) else initials

            // Click on avatar → show name
            tvAvatar.setOnClickListener {
                Toast.makeText(itemView.context, message.senderDisplayName ?: "Unknown", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDateHeader)
        fun bind(header: ChatListItem.DateHeader) {
            tvDate.text = header.formattedDate
        }
    }

    class ChatListItemDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            return when {
                oldItem is ChatListItem.Message && newItem is ChatListItem.Message -> oldItem.message.id == newItem.message.id
                oldItem is ChatListItem.DateHeader && newItem is ChatListItem.DateHeader -> oldItem.formattedDate == newItem.formattedDate
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean = oldItem == newItem
    }
}