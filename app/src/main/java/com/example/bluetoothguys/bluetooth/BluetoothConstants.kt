package com.example.bluetoothguys.bluetooth

import java.util.UUID

object BluetoothConstants {
    /**
     * Classic Bluetooth SPP UUID.
     * Widely supported and good enough for an MVP text chat over RFCOMM.
     */
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    const val SERVICE_NAME: String = "BluetoothGuys"
}

