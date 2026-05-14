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
import com.example.bluetoothguys.model.db.entities.MessageStatus
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
import java.util.UUID

/**
 * ViewModel чата: Bluetooth-подключение, сканирование устройств, локальная БД и синхронизация UI.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    // --- Хранилище и репозиторий ---
    private val db = BluetoothGuysDatabase.getInstance(application)
    private val repo = ChatRepository(db.contactDao(), db.messageDao())

    // Сканер для поиска и списка сопряжённых устройств
    private val scanner = BluetoothScanner(application.applicationContext)

    // Сопряжённые (bonded) устройства — обновляются вручную через refreshBondedDevices()
    private val _bondedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val bondedDevices: StateFlow<List<DiscoveredDevice>> = _bondedDevices.asStateFlow()

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = scanner.devices
    val isDiscovering: StateFlow<Boolean> = scanner.isDiscovering

    // Включён ли адаптер Bluetooth (обновляется через refreshBluetoothEnabled())
    private val _bluetoothEnabled =
        MutableStateFlow(
            application.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true,
        )
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    // Открытый в UI чат (для пометки входящих как прочитанных и квитанций READ)
    private val _activeContactId = MutableStateFlow<Long?>(null)
    val activeContactId: StateFlow<Long?> = _activeContactId.asStateFlow()

    // Формат времени для сообщений в списке и в чате
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    // RFCOMM/чат: приём строк, создание контакта при подключении пира
    private val bluetooth =
        BluetoothChatManager(
            appContext = application.applicationContext,
            scope = viewModelScope,
            onMessage = { address, text ->
                viewModelScope.launch(Dispatchers.IO) {
                    handleIncomingLine(address, text)
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

    // Состояние сокета: Idle / Connecting / Connected / Error
    val connectionState: StateFlow<ConnectionState> =
        bluetooth.state.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionState.Idle,
        )

    // Все контакты из БД (для экранов, где нужен полный список без агрегации последнего сообщения)
    val contacts: StateFlow<List<ContactEntity>> =
        repo.observeContacts().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // Список чатов для главного экрана: контакты + последнее сообщение + признак «онлайн» по Bluetooth
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

    /** Сырые сообщения из БД по [contactId]. */
    fun observeMessages(contactId: Long): Flow<List<MessageEntity>> = repo.observeMessages(contactId)

    /** Сообщения в виде [Message] для Compose/UI. */
    fun observeMessagesUi(contactId: Long): Flow<List<Message>> =
        repo.observeMessages(contactId).map { messages ->
            messages.map { m ->
                Message(
                    id = m.id.toString(),
                    chatId = m.contactId.toString(),
                    text = m.text,
                    time = formatTime(m.sentAt),
                    isMine = m.direction == MessageDirection.OUT,
                    status = m.status,
                )
            }
        }

    /** Добавить контакт вручную. */
    fun addContact(name: String, macAddress: String, serviceUuid: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.createContact(name = name, macAddress = macAddress, serviceUuid = serviceUuid)
        }
    }

    /**
     * Гарантирует наличие контакта с данным MAC; при отсутствии создаёт и вызывает [onReady] на Main.
     */
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

    /** Обновить поля контакта в БД. */
    fun updateContact(contact: ContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateContact(contact)
        }
    }

    /** Удалить контакт и связанные сообщения (логика в DAO/репозитории). */
    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteContact(contact)
        }
    }

    /** Сохранить исходящее сообщение только в БД (без отправки по Bluetooth). */
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

    /** Установить текущий открытый чат (или null). */
    fun setActiveContact(contactId: Long?) {
        _activeContactId.value = contactId
    }

    /** Перечитать флаг «Bluetooth включён» с системного адаптера. */
    fun refreshBluetoothEnabled() {
        _bluetoothEnabled.value = bluetooth.isBluetoothEnabled()
    }

    /** Обновить список сопряжённых устройств. */
    fun refreshBondedDevices() {
        _bondedDevices.value = scanner.bondedDevices()
    }

    /** Запуск поиска устройств; при [clearPrevious] очищает ранее найденные. */
    fun startDiscovery(clearPrevious: Boolean = false) {
        if (clearPrevious) scanner.clearDiscovered()
        scanner.startDiscovery()
    }

    /** Остановить поиск устройств. */
    fun stopDiscovery() {
        scanner.stopDiscovery()
    }

    /** Запустить серверное ожидание входящего RFCOMM-подключения. */
    fun startServer() {
        bluetooth.startServer()
    }

    /** Разорвать текущее Bluetooth-соединение. */
    fun disconnect() {
        bluetooth.disconnect()
    }

    /** Подключиться к устройству по MAC-адресу. */
    fun connectToMac(address: String) {
        viewModelScope.launch {
            bluetooth.connect(address)
        }
    }

    /**
     * Подключиться при необходимости и отправить текст по уже установленному каналу
     * (без протокола MSG| с clientId).
     */
    fun sendMessageToMac(address: String, text: String) {
        viewModelScope.launch {
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

    /** Записать входящее сообщение в БД (например, при ручном вводе или тесте). */
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

    /** Удалить одно сообщение из БД. */
    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteMessage(message)
        }
    }

    /** Освободить ресурсы сканера и Bluetooth-менеджера при уничтожении ViewModel. */
    override fun onCleared() {
        super.onCleared()
        scanner.release()
        bluetooth.release()
    }

    /** Форматирование метки времени сообщения (часы:минуты, локальная зона). */
    private fun formatTime(epochMillis: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMillis))

    /** Преобразование контакта из БД в модель [Chat] для списка с учётом [conn]. */
    private fun ContactWithLastMessage.toChatUi(conn: ConnectionState): Chat {
        val isOnline =
            (conn is ConnectionState.Connected && conn.address == contact.macAddress) ||
                (conn is ConnectionState.Connecting && conn.address == contact.macAddress)

        return Chat(
            id = contact.id.toString(),
            name = contact.name,
            lastMessage = lastMessageText ?: "Нет сообщений",
            time = lastMessageAt?.let { formatTime(it) } ?: "",
            unreadCount = unreadCount,
            isOnline = isOnline,
        )
    }

    /**
     * Разбор одной строки по Bluetooth: `MSG|clientId|текст`, квитанции `DELIVERED|`, `READ|`
     * или обычный текст для совместимости со старыми клиентами.
     */
    private suspend fun handleIncomingLine(address: String, raw: String) {
        when {
            raw.startsWith("MSG|") -> {
                val parts = raw.split("|", limit = 3)
                val clientId = parts.getOrNull(1)
                val text = parts.getOrNull(2) ?: return

                val contactId = getOrCreateContactId(address)
                val isActiveChat = activeContactId.value == contactId

                repo.addMessage(
                    contactId = contactId,
                    direction = MessageDirection.IN,
                    clientMessageId = clientId,
                    text = text,
                    sentAt = System.currentTimeMillis(),
                    status = MessageStatus.SENT,
                    isRead = isActiveChat,
                )

                if (!clientId.isNullOrBlank()) {
                    bluetooth.send("DELIVERED|$clientId")
                    if (isActiveChat) {
                        bluetooth.send("READ|$clientId")
                    }
                }
            }

            raw.startsWith("DELIVERED|") -> {
                val id = raw.removePrefix("DELIVERED|").trim()
                if (id.isNotBlank()) repo.updateOutgoingStatusByClientId(id, MessageStatus.DELIVERED)
            }

            raw.startsWith("READ|") -> {
                val id = raw.removePrefix("READ|").trim()
                if (id.isNotBlank()) repo.updateOutgoingStatusByClientId(id, MessageStatus.READ)
            }

            else -> {
                // Обратная совместимость: голый текст считается сообщением без квитанций.
                val contactId = getOrCreateContactId(address)
                repo.addMessage(
                    contactId = contactId,
                    direction = MessageDirection.IN,
                    text = raw,
                    sentAt = System.currentTimeMillis(),
                    status = MessageStatus.SENT,
                    isRead = activeContactId.value == contactId,
                )
            }
        }
    }

    /**
     * Пометить входящие прочитанными в БД и при возможности отправить пиру `READ|clientId`.
     */
    fun markChatRead(contactId: Long, address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = repo.getUnreadIncomingClientIds(contactId)
            repo.markIncomingRead(contactId)
            if (ids.isEmpty()) return@launch

            withContext(Dispatchers.Default) {
                val state = connectionState.value
                if (state !is ConnectionState.Connected || state.address != address) {
                    bluetooth.connect(address)
                } else {
                    Result.success(Unit)
                }
            }.onFailure {
                return@launch
            }

            ids.forEach { clientId ->
                bluetooth.send("READ|$clientId")
            }
        }
    }

    /** ID контакта по MAC или создание контакта с именем по умолчанию = адрес. */
    private suspend fun getOrCreateContactId(address: String): Long {
        val existing = repo.getContactByMac(address)
        return existing?.id
            ?: repo.createContact(
                name = address,
                macAddress = address,
                serviceUuid = null,
            )
    }

    /**
     * Отправка с протоколом доставки: запись в БД со статусом, подключение, `MSG|uuid|текст`,
     * обновление статуса по результату [bluetooth.send].
     */
    fun sendOutgoingMessage(contactId: Long, address: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val clientId = UUID.randomUUID().toString()
            val sentAt = System.currentTimeMillis()

            repo.addMessage(
                contactId = contactId,
                direction = MessageDirection.OUT,
                clientMessageId = clientId,
                text = text,
                sentAt = sentAt,
                status = MessageStatus.SENDING,
                isRead = true,
            )

            val res =
                withContext(Dispatchers.Default) {
                    val state = connectionState.value
                    if (state !is ConnectionState.Connected || state.address != address) {
                        bluetooth.connect(address)
                    } else {
                        Result.success(Unit)
                    }
                }

            if (res.isFailure) {
                repo.updateOutgoingStatusByClientId(clientId, MessageStatus.ERROR)
                return@launch
            }

            val sendRes = bluetooth.send("MSG|$clientId|$text")
            if (sendRes.isSuccess) {
                repo.updateOutgoingStatusByClientId(clientId, MessageStatus.SENT)
            } else {
                repo.updateOutgoingStatusByClientId(clientId, MessageStatus.ERROR)
            }
        }
    }
}
