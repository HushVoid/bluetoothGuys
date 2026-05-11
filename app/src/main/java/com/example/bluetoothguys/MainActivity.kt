package com.example.bluetoothguys

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.bluetoothguys.bluetooth.ConnectionState
import androidx.compose.ui.platform.LocalContext
import com.example.bluetoothguys.view.ui.screens.chat.ChatScreen
import com.example.bluetoothguys.view.ui.screens.chat_list.ChatListScreen
import com.example.bluetoothguys.view.ui.screens.devices.DevicesScreen
import com.example.bluetoothguys.view.ui.theme.BluetoothGuysTheme
import com.example.bluetoothguys.view_model.ChatViewModel

class MainActivity : ComponentActivity() {
    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothGuysTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    BluetoothMessengerApp(vm = vm)
                }
            }
        }
    }
}

private sealed interface Screen {
    data object ChatList : Screen
    data object Devices : Screen
    data class Chat(val contactId: Long) : Screen
}

@Composable
private fun BluetoothMessengerApp(vm: ChatViewModel) {
    var screen: Screen by rememberSaveable { mutableStateOf(Screen.ChatList) }

    val hasConnectPermission = rememberPermission(Manifest.permission.BLUETOOTH_CONNECT)
    val hasScanPermission = rememberPermission(Manifest.permission.BLUETOOTH_SCAN)

    val permissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.refreshBondedDevices()
            vm.refreshBluetoothEnabled()
        }

    val enableBtLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            vm.refreshBluetoothEnabled()
            vm.refreshBondedDevices()
        }

    LaunchedEffect(Unit) {
        vm.startServer()
        vm.refreshBluetoothEnabled()
        vm.refreshBondedDevices()
    }

    val bluetoothEnabled by vm.bluetoothEnabled.collectAsState()

    val missingPermissions = remember(hasConnectPermission.value, hasScanPermission.value) {
        buildList {
            if (!hasConnectPermission.value) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasScanPermission.value) add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    if (missingPermissions.isNotEmpty()) {
        PermissionScreen(
            onRequest = { permissionsLauncher.launch(missingPermissions.toTypedArray()) },
        )
        return
    }

    when (val s = screen) {
        Screen.ChatList -> {
            val chats by vm.chats.collectAsState()
            ChatListScreen(
                chats = chats,
                onChatClick = { chat ->
                    val id = chat.id.toLongOrNull() ?: return@ChatListScreen
                    screen = Screen.Chat(id)
                },
                onBluetoothClick = { screen = Screen.Devices },
            )
        }

        Screen.Devices -> {
            val bonded by vm.bondedDevices.collectAsState()
            val discovered by vm.discoveredDevices.collectAsState()
            val isDiscovering by vm.isDiscovering.collectAsState()

            DevicesScreen(
                bluetoothEnabled = bluetoothEnabled,
                isDiscovering = isDiscovering,
                bonded = bonded,
                discovered = discovered,
                onBack = {
                    vm.stopDiscovery()
                    screen = Screen.ChatList
                },
                onEnableBluetooth = {
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                },
                onRefreshBonded = { vm.refreshBondedDevices() },
                onStartScan = {
                    if (bluetoothEnabled) vm.startDiscovery(clearPrevious = true)
                },
                onStopScan = { vm.stopDiscovery() },
                onDeviceClick = { d ->
                    vm.ensureContactForMac(d.address, d.name) { id ->
                        vm.stopDiscovery()
                        screen = Screen.Chat(id)
                    }
                },
            )
        }

        is Screen.Chat -> {
            val contacts by vm.contacts.collectAsState()
            val contact = contacts.firstOrNull { it.id == s.contactId }
            if (contact == null) {
                screen = Screen.ChatList
                return
            }

            val messages by vm.observeMessagesUi(contact.id).collectAsState(initial = emptyList())
            var text by rememberSaveable(contact.id) { mutableStateOf("") }

            val conn by vm.connectionState.collectAsState()
            val subtitle =
                when (conn) {
                    is ConnectionState.Connected ->
                        if ((conn as ConnectionState.Connected).address == contact.macAddress) {
                            "в сети • Bluetooth"
                        } else {
                            "не подключено • Bluetooth"
                        }
                    is ConnectionState.Connecting ->
                        if ((conn as ConnectionState.Connecting).address == contact.macAddress) {
                            "подключение… • Bluetooth"
                        } else {
                            "не подключено • Bluetooth"
                        }
                    ConnectionState.Listening -> "ожидание входящих… • Bluetooth"
                    ConnectionState.Idle -> "не подключено • Bluetooth"
                    is ConnectionState.Error -> "ошибка • Bluetooth"
                }

            LaunchedEffect(contact.id) {
                vm.setActiveContact(contact.id)
                vm.connectToMac(contact.macAddress)
            }

            ChatScreen(
                chatName = contact.name,
                subtitle = subtitle,
                messages = messages,
                text = text,
                onTextChange = { text = it },
                onSend = {
                    val msg = text.trim()
                    if (msg.isBlank()) return@ChatScreen
                    text = ""
                    vm.sendMessage(contactId = contact.id, text = msg)
                    vm.sendMessageToMac(address = contact.macAddress, text = msg)
                },
                onBack = {
                    vm.setActiveContact(null)
                    screen = Screen.ChatList
                },
            )
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRequest) {
            Text("Разрешить Bluetooth-доступ")
        }
    }
}

@Composable
private fun rememberPermission(permission: String) =
    run {
        val context = LocalContext.current
        remember(permission, context) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == PackageManager.PERMISSION_GRANTED,
            )
        }.also { state ->
            // Refresh each composition; cheap and keeps UI correct after system dialogs.
            state.value =
                ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == PackageManager.PERMISSION_GRANTED
        }
    }
