package com.example.bluetoothguys.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BluetoothChatManager(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val onMessage: (address: String, text: String) -> Unit,
    private val onPeerConnected: (address: String, name: String?) -> Unit,
) {
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state

    private val managerJob = SupervisorJob()
    private var serverJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null

    private var peer: BluetoothPeer? = null
    private var peerAddress: String? = null

    fun hasBluetooth(): Boolean = adapter != null

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun startServer() {
        if (serverJob != null) return
        if (!hasConnectPermission()) {
            _state.value = ConnectionState.Error("Нет разрешения BLUETOOTH_CONNECT")
            return
        }

        serverJob =
            scope.launch(Dispatchers.IO + managerJob) {
                _state.value = ConnectionState.Listening
                try {
                    val a = adapter ?: return@launch
                    @SuppressLint("MissingPermission")
                    val ss =
                        a.listenUsingRfcommWithServiceRecord(
                            BluetoothConstants.SERVICE_NAME,
                            BluetoothConstants.SPP_UUID,
                        )
                    serverSocket = ss
                    while (true) {
                        val socket = ss.accept()
                        handleIncomingSocket(socket)
                    }
                } catch (t: Throwable) {
                    _state.value = ConnectionState.Error(t.message ?: "Ошибка сервера Bluetooth")
                } finally {
                    closeServerQuietly()
                    _state.value = ConnectionState.Idle
                }
            }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        closeServerQuietly()
        if (_state.value is ConnectionState.Listening) _state.value = ConnectionState.Idle
    }

    suspend fun connect(address: String): Result<Unit> {
        val a = adapter ?: return Result.failure(IllegalStateException("Bluetooth не поддерживается"))
        if (!hasConnectPermission()) return Result.failure(SecurityException("Нет BLUETOOTH_CONNECT"))
        if (!a.isEnabled) return Result.failure(IllegalStateException("Bluetooth выключен"))

        return withContext(Dispatchers.IO) {
            try {
                _state.value = ConnectionState.Connecting(address)
                peer?.close()
                peer = null

                @SuppressLint("MissingPermission")
                val device = a.getRemoteDevice(address)
                val socket =
                    device.createRfcommSocketToServiceRecord(
                        BluetoothConstants.SPP_UUID,
                    )
                if (a.isDiscovering) {
                    @SuppressLint("MissingPermission")
                    a.cancelDiscovery()
                }
                socket.connect()
                handleConnectedSocket(socket)
                Result.success(Unit)
            } catch (t: Throwable) {
                _state.value = ConnectionState.Error(t.message ?: "Ошибка подключения")
                Result.failure(t)
            }
        }
    }

    suspend fun send(text: String): Result<Unit> {
        val p = peer ?: return Result.failure(IllegalStateException("Нет активного соединения"))
        return try {
            p.send(text)
            Result.success(Unit)
        } catch (t: Throwable) {
            _state.value = ConnectionState.Error(t.message ?: "Ошибка отправки")
            Result.failure(t)
        }
    }

    fun disconnect() {
        peer?.close()
        peer = null
        peerAddress = null
        _state.value = ConnectionState.Idle
    }

    fun release() {
        disconnect()
        stopServer()
        managerJob.cancel()
    }

    private fun handleIncomingSocket(socket: BluetoothSocket) {
        // If someone connects to us, we accept it as the active peer.
        handleConnectedSocket(socket)
    }

    @SuppressLint("MissingPermission")
    private fun handleConnectedSocket(socket: BluetoothSocket) {
        peer?.close()

        val device: BluetoothDevice? = socket.remoteDevice
        val address = device?.address ?: "unknown"
        val name = if (hasConnectPermission()) device?.name else null
        peerAddress = address

        _state.value = ConnectionState.Connected(address = address, name = name)
        onPeerConnected(address, name)

        val newPeer =
            BluetoothPeer(
                socket = socket,
                scope = scope,
                onIncomingLine = { line ->
                    onMessage(address, line)
                },
                onClosed = { throwable ->
                    if (throwable != null) {
                        _state.value =
                            ConnectionState.Error(
                                throwable.message ?: "Соединение закрыто с ошибкой",
                            )
                    } else {
                        _state.value = ConnectionState.Idle
                    }
                },
            )
        peer = newPeer
        newPeer.start()
    }

    private fun closeServerQuietly() {
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        serverSocket = null
    }

    private fun hasConnectPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
}
