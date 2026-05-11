package com.example.bluetoothguys.view.ui.screens.chat_list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.runtime.Composable
import com.example.bluetoothguys.model.Chat
import com.example.bluetoothguys.view.ui.components.AppHeader
import com.example.bluetoothguys.view.ui.components.ChatItem

@Composable
fun ChatListScreen(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit,
    onBluetoothClick: (() -> Unit)? = null,
) {
    Column {
        AppHeader(
            title = "Сообщения",
            subtitle = "Bluetooth-мессенджер",
            rightIcon = Icons.Default.Bluetooth,
            onRightIconClick = onBluetoothClick,
        )

        LazyColumn {
            items(chats) { chat ->
                ChatItem(chat) { onChatClick(chat) }
            }
        }
    }
}

