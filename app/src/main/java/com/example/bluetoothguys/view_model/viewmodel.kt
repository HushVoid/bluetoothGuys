package com.example.bluetoothguys.view_model

import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothguys.bluetooth.BluetoothChatManager
import com.example.bluetoothguys.bluetooth.BluetoothScanner
import com.example.bluetoothguys.bluetooth.ConnectionState
import com.example.bluetoothguys.bluetooth.DiscoveredDevice
import com.example.bluetoothguys.model.Chat
import com.example.bluetoothguys.model.Message
import com.example.bluetoothguys.model.db.BluetoothGuysDatabase
import com.example.bluetoothguys.model.db.entities.ContactEntity
import com.example.bluetoothguys.model.db.entities.ContactWithLastMessage
import com.example.bluetoothguys.model.db.entities.MessageDirection
import com.example.bluetoothguys.model.db.entities.MessageEntity
import com.example.bluetoothguys.model.repo.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BluetoothGuysDatabase.getInstance(application)
    private val repo = ChatRepository(db.contactDao(), db.messageDao())

    private val scanner = BluetoothScanner(application.applicationContext)

    private val _bondedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val bondedDevices: StateFlow<List<DiscoveredDevice>> = _bondedDevices.asStateFlow()

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = scanner.devices
    val isDiscovering: StateFlow<Boolean> = scanner.isDiscovering

    private val _bluetoothEnabled =
        MutableStateFlow(
            application.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true,
        )
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _activeContactId = MutableStateFlow<Long?>(null)
    val activeContactId: StateFlow<Long?> = _activeContactId.asStateFlow()

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    private val bluetooth =
        BluetoothChatManager(
            appContext = application.applicationContext,
            scope = viewModelScope,
            onMessage = { address, text ->
                viewModelScope.launch(Dispatchers.IO) {
                    val existing = repo.getContactByMac(address)
                    val contactId =
                        existing?.id
                            ?: repo.createContact(
                                name = address,
                                macAddress = address,
                                serviceUuid = null,
                            )
                    repo.addMessage(
                        contactId = contactId,
                        direction = MessageDirection.IN,
                        text = text,
                        sentAt = System.currentTimeMillis(),
                    )
                }
            },
            onPeerConnected = { address, name ->
                viewModelScope.launch(Dispatchers.IO) {
                    val existing = repo.getContactByMac(address)
                    if (existing == null) {
                        repo.createContact(
                            name = name ?: address,
                            macAddress = address,
                            serviceUuid = null,
                        )
                    } else if (!name.isNullOrBlank() && existing.name == existing.macAddress) {
                        repo.updateContact(existing.copy(name = name))
                    }
                }
            },
        )

    val connectionState: StateFlow<ConnectionState> =
        bluetooth.state.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionState.Idle,
        )

    val contacts: StateFlow<List<ContactEntity>> =
        repo.observeContacts().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val chats: StateFlow<List<Chat>> =
        combine(
            repo.observeContactsWithLastMessage(),
            connectionState,
        ) { items, conn ->
            items.map { it.toChatUi(conn) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun observeMessages(contactId: Long): Flow<List<MessageEntity>> = repo.observeMessages(contactId)

    fun observeMessagesUi(contactId: Long): Flow<List<Message>> =
        repo.observeMessages(contactId).map { messages ->
            messages.map { m ->
                Message(
                    id = m.id.toString(),
                    chatId = m.contactId.toString(),
                    text = m.text,
                    time = formatTime(m.sentAt),
                    isMine = m.direction == MessageDirection.OUT,
                )
            }
        }

    fun addContact(name: String, macAddress: String, serviceUuid: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.createContact(name = name, macAddress = macAddress, serviceUuid = serviceUuid)
        }
    }

    fun ensureContactForMac(
        macAddress: String,
        name: String?,
        onReady: (contactId: Long) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repo.getContactByMac(macAddress)
            val id =
                existing?.id
                    ?: repo.createContact(
                        name = name?.takeIf { it.isNotBlank() } ?: macAddress,
                        macAddress = macAddress,
                        serviceUuid = null,
                    )
            withContext(Dispatchers.Main) {
                onReady(id)
            }
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

    fun setActiveContact(contactId: Long?) {
        _activeContactId.value = contactId
    }

    fun refreshBluetoothEnabled() {
        _bluetoothEnabled.value = bluetooth.isBluetoothEnabled()
    }

    fun refreshBondedDevices() {
        _bondedDevices.value = scanner.bondedDevices()
    }

    fun startDiscovery(clearPrevious: Boolean = false) {
        if (clearPrevious) scanner.clearDiscovered()
        scanner.startDiscovery()
    }

    fun stopDiscovery() {
        scanner.stopDiscovery()
    }

    fun startServer() {
        bluetooth.startServer()
    }

    fun disconnect() {
        bluetooth.disconnect()
    }

    fun connectToMac(address: String) {
        viewModelScope.launch {
            bluetooth.connect(address)
        }
    }

    fun sendMessageToMac(address: String, text: String) {
        viewModelScope.launch {
            // Connect (if needed) and only send when we are actually connected.
            val state = connectionState.value
            if (state !is ConnectionState.Connected || state.address != address) {
                val res = bluetooth.connect(address)
                if (res.isFailure) return@launch
            }

            val now = connectionState.value
            if (now is ConnectionState.Connected && now.address == address) {
                bluetooth.send(text)
            }
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

    override fun onCleared() {
        super.onCleared()
        scanner.release()
        bluetooth.release()
    }

    private fun formatTime(epochMillis: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMillis))

    private fun ContactWithLastMessage.toChatUi(conn: ConnectionState): Chat {
        val isOnline =
            (conn is ConnectionState.Connected && conn.address == contact.macAddress) ||
                (conn is ConnectionState.Connecting && conn.address == contact.macAddress)

        return Chat(
            id = contact.id.toString(),
            name = contact.name,
            lastMessage = lastMessageText ?: "Нет сообщений",
            time = lastMessageAt?.let { formatTime(it) } ?: "",
            unreadCount = 0,
            isOnline = isOnline,
        )
    }
}
