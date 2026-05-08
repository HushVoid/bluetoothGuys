package com.example.bluetoothguys.model

data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val isOnline: Boolean
)

data class Message(
    val id: String,
    val chatId: String,
    val text: String,
    val time: String,
    val isMine: Boolean
)
