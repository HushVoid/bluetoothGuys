package com.example.bluetoothguys.view_model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.bluetoothguys.model.Chat
import com.example.bluetoothguys.model.Message

data class BluetoothMessengerUiState(
    val chats: List<Chat> = emptyList(),
    val selectedChat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val currentInput: String = "",
    val isBluetoothEnabled: Boolean = true,
    val isSearchingDevices: Boolean = false,
    val isConnected: Boolean = false
)

class BluetoothMessengerViewModel : ViewModel() {

    private val chatMessages = mutableMapOf(
        "1" to mutableListOf(
            Message("1", "1", "Привет! Как дела?", "14:28", false),
            Message("2", "1", "Привет! Всё отлично, спасибо!", "14:29", true),
            Message("3", "1", "А у тебя как?", "14:29", true),
            Message("4", "1", "Тоже хорошо, работаю над проектом", "14:30", false)
        ),
        "2" to mutableListOf(
            Message("5", "2", "Отправил тебе файл", "12:15", false)
        ),
        "3" to mutableListOf(
            Message("6", "3", "Созвонимся позже?", "Вчера", false)
        )
    )

    private val _uiState = MutableStateFlow(
        BluetoothMessengerUiState(
            chats = listOf(
                Chat(
                    id = "1",
                    name = "iPhone 13",
                    lastMessage = "Привет! Как дела?",
                    time = "14:30",
                    unreadCount = 2,
                    isOnline = true
                ),
                Chat(
                    id = "2",
                    name = "Samsung Galaxy S21",
                    lastMessage = "Отправил тебе файл",
                    time = "12:15",
                    unreadCount = 0,
                    isOnline = true
                ),
                Chat(
                    id = "3",
                    name = "MacBook Pro",
                    lastMessage = "Созвонимся позже?",
                    time = "Вчера",
                    unreadCount = 1,
                    isOnline = true
                )
            )
        )
    )
    val uiState: StateFlow<BluetoothMessengerUiState> = _uiState.asStateFlow()

    fun selectChat(chat: Chat) {
        _uiState.update { state ->
            state.copy(
                selectedChat = chat.copy(unreadCount = 0),
                messages = chatMessages[chat.id].orEmpty(),
                isConnected = chat.isOnline,
                chats = state.chats.map {
                    if (it.id == chat.id) it.copy(unreadCount = 0) else it
                }
            )
        }
    }

    fun closeChat() {
        _uiState.update { state ->
            state.copy(
                selectedChat = null,
                messages = emptyList(),
                currentInput = ""
            )
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val selectedChat = state.selectedChat ?: return
        val text = state.currentInput.trim()

        if (text.isEmpty()) return

        val newMessage = Message(
            id = System.currentTimeMillis().toString(),
            chatId = selectedChat.id,
            text = text,
            time = "Сейчас",
            isMine = true
        )

        val updatedMessages = (chatMessages[selectedChat.id] ?: mutableListOf()).apply {
            add(newMessage)
        }
        chatMessages[selectedChat.id] = updatedMessages

        _uiState.update { currentState ->
            currentState.copy(
                currentInput = "",
                messages = updatedMessages.toList(),
                chats = currentState.chats.map { chat ->
                    if (chat.id == selectedChat.id) {
                        chat.copy(
                            lastMessage = newMessage.text,
                            time = newMessage.time,
                            unreadCount = 0
                        )
                    } else {
                        chat
                    }
                },
                selectedChat = currentState.selectedChat?.copy(
                    lastMessage = newMessage.text,
                    time = newMessage.time,
                    unreadCount = 0
                )
            )
        }
    }

    fun receiveMessage(chatId: String, text: String, time: String = "Сейчас") {
        val incomingMessage = Message(
            id = System.currentTimeMillis().toString(),
            chatId = chatId,
            text = text,
            time = time,
            isMine = false
        )

        val updatedMessages = (chatMessages[chatId] ?: mutableListOf()).apply {
            add(incomingMessage)
        }
        chatMessages[chatId] = updatedMessages

        _uiState.update { state ->
            val isCurrentChat = state.selectedChat?.id == chatId
            state.copy(
                messages = if (isCurrentChat) updatedMessages.toList() else state.messages,
                chats = state.chats.map { chat ->
                    if (chat.id == chatId) {
                        chat.copy(
                            lastMessage = text,
                            time = time,
                            unreadCount = if (isCurrentChat) 0 else chat.unreadCount + 1,
                            isOnline = true
                        )
                    } else {
                        chat
                    }
                },
                selectedChat = if (isCurrentChat) {
                    state.selectedChat?.copy(
                        lastMessage = text,
                        time = time,
                        unreadCount = 0,
                        isOnline = true
                    )
                } else {
                    state.selectedChat
                }
            )
        }
    }

    fun startDeviceSearch() {
        _uiState.update { it.copy(isSearchingDevices = true) }
    }

    fun stopDeviceSearch() {
        _uiState.update { it.copy(isSearchingDevices = false) }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isBluetoothEnabled = enabled,
                isConnected = if (enabled) it.isConnected else false,
                isSearchingDevices = if (enabled) it.isSearchingDevices else false
            )
        }
    }

    fun connectToChatDevice(chatId: String) {
        _uiState.update { state ->
            state.copy(
                isConnected = true,
                chats = state.chats.map { chat ->
                    if (chat.id == chatId) chat.copy(isOnline = true) else chat
                },
                selectedChat = if (state.selectedChat?.id == chatId) {
                    state.selectedChat.copy(isOnline = true)
                } else {
                    state.selectedChat
                }
            )
        }
    }

    fun disconnectCurrentChat() {
        val currentChatId = _uiState.value.selectedChat?.id ?: return

        _uiState.update { state ->
            state.copy(
                isConnected = false,
                chats = state.chats.map { chat ->
                    if (chat.id == currentChatId) chat.copy(isOnline = false) else chat
                },
                selectedChat = state.selectedChat?.copy(isOnline = false)
            )
        }
    }
}
