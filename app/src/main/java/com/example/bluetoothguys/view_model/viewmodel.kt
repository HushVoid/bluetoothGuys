package com.example.bluetoothguys.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothguys.model.db.BluetoothGuysDatabase
import com.example.bluetoothguys.model.db.entities.ContactEntity
import com.example.bluetoothguys.model.db.entities.MessageDirection
import com.example.bluetoothguys.model.db.entities.MessageEntity
import com.example.bluetoothguys.model.repo.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BluetoothGuysDatabase.getInstance(application)
    private val repo = ChatRepository(db.contactDao(), db.messageDao())

    val contacts: StateFlow<List<ContactEntity>> =
        repo.observeContacts().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun observeMessages(contactId: Long): Flow<List<MessageEntity>> = repo.observeMessages(contactId)

    fun addContact(name: String, macAddress: String, serviceUuid: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.createContact(name = name, macAddress = macAddress, serviceUuid = serviceUuid)
        }
    }

    fun updateContact(contact: ContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateContact(contact)
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteContact(contact)
        }
    }

    fun sendMessage(contactId: Long, text: String, sentAt: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addMessage(
                contactId = contactId,
                direction = MessageDirection.OUT,
                text = text,
                sentAt = sentAt,
            )
        }
    }

    fun receiveMessage(contactId: Long, text: String, sentAt: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addMessage(
                contactId = contactId,
                direction = MessageDirection.IN,
                text = text,
                sentAt = sentAt,
            )
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteMessage(message)
        }
    }
}

