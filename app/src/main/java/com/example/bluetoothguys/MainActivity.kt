package com.example.bluetoothguys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bluetoothguys.view.ui.screens.chat.ChatScreen
import com.example.bluetoothguys.view.ui.screens.chat_list.ChatListScreen
import com.example.bluetoothguys.view.ui.theme.BluetoothGuysTheme
import com.example.bluetoothguys.model.Chat
import com.example.bluetoothguys.view_model.BluetoothMessengerUiState
import com.example.bluetoothguys.view_model.BluetoothMessengerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BluetoothMessengerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothGuysTheme {
                val uiState by viewModel.uiState.collectAsState()

                BluetoothMessengerApp(
                    uiState = uiState,
                    onChatClick = viewModel::selectChat,
                    onBackClick = viewModel::closeChat,
                    onTextChange = viewModel::updateInput,
                    onSendClick = viewModel::sendMessage
                )
            }
        }
    }
}

@Composable
private fun BluetoothMessengerApp(
    uiState: BluetoothMessengerUiState,
    onChatClick: (Chat) -> Unit,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            val selectedChat = uiState.selectedChat

            if (selectedChat != null) {
                ChatScreen(
                    chatName = selectedChat.name,
                    messages = uiState.messages,
                    text = uiState.currentInput,
                    onTextChange = onTextChange,
                    onSend = onSendClick,
                    onBack = onBackClick
                )
            } else {
                ChatListScreen(
                    chats = uiState.chats,
                    onChatClick = onChatClick
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BluetoothMessengerAppPreview() {
    BluetoothGuysTheme {
        BluetoothMessengerApp(
            uiState = BluetoothMessengerUiState(),
            onChatClick = {},
            onBackClick = {},
            onTextChange = {},
            onSendClick = {}
        )
    }
}
