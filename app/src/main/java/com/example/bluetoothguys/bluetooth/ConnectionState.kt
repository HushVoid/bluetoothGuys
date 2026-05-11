package com.example.bluetoothguys.bluetooth

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Listening : ConnectionState()
    data class Connecting(val address: String) : ConnectionState()
    data class Connected(val address: String, val name: String?) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

