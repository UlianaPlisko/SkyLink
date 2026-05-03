package com.codepalace.accelerometer.data.model.dto

sealed class ChatListItem {
    data class Message(val message: MessageUi) : ChatListItem()
    data class DateHeader(val formattedDate: String) : ChatListItem()
}