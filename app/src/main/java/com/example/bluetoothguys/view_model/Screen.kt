package com.example.bluetoothguys.view_model

sealed interface Screen {
    data object ChatList : Screen
    data object Devices : Screen
    data class Chat(val contactId: Long) : Screen
}
