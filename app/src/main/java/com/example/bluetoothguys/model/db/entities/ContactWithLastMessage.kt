package com.example.bluetoothguys.model.db.entities

import androidx.room.Embedded

data class ContactWithLastMessage(
    @Embedded
    val contact: ContactEntity,
    val lastMessageText: String?,
    val lastMessageAt: Long?,
    val unreadCount: Int,
)

