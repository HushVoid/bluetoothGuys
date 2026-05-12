package com.example.bluetoothguys.bluetooth

data class DiscoveredDevice(
    val name: String?,
    val address: String,
    val bonded: Boolean,
    val rssi: Int? = null,
)

