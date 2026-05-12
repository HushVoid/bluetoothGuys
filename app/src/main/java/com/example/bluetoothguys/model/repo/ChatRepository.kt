package com.example.bluetoothguys.model.repo

import com.example.bluetoothguys.model.db.dao.ContactDao
import com.example.bluetoothguys.model.db.dao.MessageDao
import com.example.bluetoothguys.model.db.entities.ContactEntity
import com.example.bluetoothguys.model.db.entities.ContactWithLastMessage
import com.example.bluetoothguys.model.db.entities.MessageDirection
import com.example.bluetoothguys.model.db.entities.MessageEntity
import com.example.bluetoothguys.model.db.entities.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий чата — единая точка доступа к данным.
 *
 * Зачем нужен репозиторий:
 * - UI/ViewModel не знает про SQL-запросы и Room-аннотации
 * - можно централизованно менять правила (валидация, маппинг моделей, кеширование)
 * - проще тестировать (в будущем можно подменить DAO/источник данных)
 */
class ChatRepository(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
) {
    /** Наблюдать список контактов (будет обновляться при изменениях в БД). */
    fun observeContacts(): Flow<List<ContactEntity>> = contactDao.observeAll()

    fun observeContactsWithLastMessage(): Flow<List<ContactWithLastMessage>> =
        contactDao.observeAllWithLastMessage()

    /** Получить контакт по id (одноразовый запрос). */
    suspend fun getContactById(id: Long): ContactEntity? = contactDao.getById(id)

    /** Получить контакт по MAC-адресу (одноразовый запрос). */
    suspend fun getContactByMac(macAddress: String): ContactEntity? = contactDao.getByMac(macAddress)

    /**
     * Создать контакт.
     *
     * Возвращает `id` созданной записи.
     * `macAddress` в таблице уникальный (Room выбросит ошибку при дубликате).
     */
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

    /** Наблюдать историю сообщений конкретного контакта. */
    fun observeMessages(contactId: Long): Flow<List<MessageEntity>> = messageDao.observeForContact(contactId)

    /** Получить последнее сообщение по контакту (например, для списка чатов). */
    suspend fun getLatestMessage(contactId: Long): MessageEntity? = messageDao.getLatestForContact(contactId)

    /**
     * Добавить сообщение в историю.
     *
     * - `direction = OUT` если отправили мы
     * - `direction = IN` если получили от устройства
     */
    suspend fun addMessage(
        contactId: Long,
        direction: MessageDirection,
        clientMessageId: String? = null,
        text: String,
        sentAt: Long,
        status: MessageStatus = MessageStatus.SENT,
        isRead: Boolean = true,
    ): Long = messageDao.insert(
        MessageEntity(
            contactId = contactId,
            direction = direction,
            clientMessageId = clientMessageId,
            text = text,
            sentAt = sentAt,
            status = status,
            isRead = isRead,
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

    suspend fun markIncomingRead(contactId: Long) {
        messageDao.markIncomingRead(contactId)
    }

    suspend fun getUnreadIncomingClientIds(contactId: Long): List<String> {
        return messageDao.getUnreadIncomingClientIds(contactId)
    }

    suspend fun updateOutgoingStatusByClientId(clientMessageId: String, status: MessageStatus) {
        messageDao.updateOutgoingStatusByClientId(clientMessageId, status)
    }
}

