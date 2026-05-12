package com.example.bluetoothguys.model.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["sentAt"]),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val direction: MessageDirection,
    /**
     * Stable id that is transmitted over Bluetooth to match delivery/read receipts.
     * For old rows (pre-migration) it can be null.
     */
    val clientMessageId: String? = null,
    val text: String,
    val sentAt: Long,
    /** Outgoing status; for incoming messages it can be left as SENT. */
    val status: MessageStatus = MessageStatus.SENT,
    /** Only meaningful for incoming messages. */
    val isRead: Boolean = true,
)

