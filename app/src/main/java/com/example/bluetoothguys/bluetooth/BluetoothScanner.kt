package com.example.bluetoothguys.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BluetoothScanner(
    private val appContext: Context,
) {
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices

    private var receiverRegistered = false

    private val receiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _isDiscovering.value = true
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _isDiscovering.value = false
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= 33) {
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java,
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        if (device == null || device.address.isNullOrBlank()) return

                        val bonded = device.bondState == BluetoothDevice.BOND_BONDED
                        val name = safeName(device)
                        val item =
                            DiscoveredDevice(
                                name = name,
                                address = device.address,
                                bonded = bonded,
                                rssi = if (rssi == Short.MIN_VALUE) null else rssi.toInt(),
                            )

                        _devices.update { old ->
                            val without = old.filterNot { it.address == item.address }
                            // Keep a stable ordering: bonded first, then by RSSI desc, then by name/address.
                            (without + item).sortedWith(
                                compareByDescending<DiscoveredDevice> { it.bonded }
                                    .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                                    .thenBy { it.name ?: "" }
                                    .thenBy { it.address },
                            )
                        }
                    }
                }
            }
        }

    fun bondedDevices(): List<DiscoveredDevice> {
        val a = adapter ?: return emptyList()
        if (!hasConnectPermission()) return emptyList()

        return a.bondedDevices
            .orEmpty()
            .map { d ->
                DiscoveredDevice(
                    name = safeName(d),
                    address = d.address,
                    bonded = true,
                    rssi = null,
                )
            }
            .sortedWith(compareBy<DiscoveredDevice> { it.name ?: "" }.thenBy { it.address })
    }

    fun clearDiscovered() {
        _devices.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        val a = adapter ?: return false
        if (!hasScanPermission()) return false

        if (!receiverRegistered) {
            val filter =
                IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    addAction(BluetoothDevice.ACTION_FOUND)
                }
            // On Android 13+ (and especially with higher targetSdk), registering a receiver
            // without explicit exported flags can throw SecurityException.
            try {
                ContextCompat.registerReceiver(
                    appContext,
                    receiver,
                    filter,
                    RECEIVER_NOT_EXPORTED,
                )
                receiverRegistered = true
            } catch (_: SecurityException) {
                _isDiscovering.value = false
                return false
            }
        }

        if (a.isDiscovering) a.cancelDiscovery()
        _isDiscovering.value = a.startDiscovery()
        return _isDiscovering.value
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val a = adapter ?: return
        if (hasScanPermission() && a.isDiscovering) {
            a.cancelDiscovery()
        }
        _isDiscovering.value = false
    }

    fun release() {
        stopDiscovery()
        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: Throwable) {
                // Ignore double-unregister / already-unregistered edge cases
            }
            receiverRegistered = false
        }
    }

    private fun hasScanPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasConnectPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice): String? {
        return if (hasConnectPermission()) device.name else null
    }
}
