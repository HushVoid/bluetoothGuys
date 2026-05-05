package com.example.bluetoothguys.model.repo

import com.example.bluetoothguys.model.db.dao.ContactDao
import com.example.bluetoothguys.model.db.dao.MessageDao
import com.example.bluetoothguys.model.db.entities.ContactEntity
import com.example.bluetoothguys.model.db.entities.MessageDirection
import com.example.bluetoothguys.model.db.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
) {
    fun observeContacts(): Flow<List<ContactEntity>> = contactDao.observeAll()

    suspend fun getContactById(id: Long): ContactEntity? = contactDao.getById(id)

    suspend fun getContactByMac(macAddress: String): ContactEntity? = contactDao.getByMac(macAddress)

    suspend fun createContact(
        name: String,
        macAddress: String,
        serviceUuid: String? = null,
    ): Long = contactDao.insert(
        ContactEntity(
            name = name,
            macAddress = macAddress,
            serviceUuid = serviceUuid,
        ),
    )

    suspend fun updateContact(contact: ContactEntity) {
        contactDao.update(contact)
    }

    suspend fun deleteContact(contact: ContactEntity) {
        contactDao.delete(contact)
    }

    suspend fun deleteContactById(id: Long) {
        contactDao.deleteById(id)
    }

    fun observeMessages(contactId: Long): Flow<List<MessageEntity>> = messageDao.observeForContact(contactId)

    suspend fun getLatestMessage(contactId: Long): MessageEntity? = messageDao.getLatestForContact(contactId)

    suspend fun addMessage(
        contactId: Long,
        direction: MessageDirection,
        text: String,
        sentAt: Long,
    ): Long = messageDao.insert(
        MessageEntity(
            contactId = contactId,
            direction = direction,
            text = text,
            sentAt = sentAt,
        ),
    )

    suspend fun updateMessage(message: MessageEntity) {
        messageDao.update(message)
    }

    suspend fun deleteMessage(message: MessageEntity) {
        messageDao.delete(message)
    }

    suspend fun deleteMessageById(id: Long) {
        messageDao.deleteById(id)
    }

    suspend fun deleteMessagesForContact(contactId: Long) {
        messageDao.deleteForContact(contactId)
    }
}

