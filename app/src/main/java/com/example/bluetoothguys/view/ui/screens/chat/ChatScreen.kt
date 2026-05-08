package com.example.bluetoothguys.view.ui.screens.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bluetoothguys.model.Message
import com.example.bluetoothguys.view.ui.components.AppHeader
import com.example.bluetoothguys.view.ui.components.InputBar
import com.example.bluetoothguys.view.ui.components.MessageBubble
import kotlin.collections.reversed

@Composable
fun ChatScreen(
    chatName: String,
    messages: List<Message>,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        AppHeader(
            title = chatName,
            subtitle = "в сети • Bluetooth",
            showBack = true,
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) {
                MessageBubble(it)
            }
        }

        InputBar(
            text = text,
            onTextChange = onTextChange,
            onSend = onSend
        )
    }
}
