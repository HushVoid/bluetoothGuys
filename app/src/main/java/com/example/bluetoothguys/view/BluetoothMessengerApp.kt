package com.example.bluetoothguys.view

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.bluetoothguys.bluetooth.ConnectionState
import com.example.bluetoothguys.view.ui.screens.chat.ChatScreen
import com.example.bluetoothguys.view.ui.screens.chat_list.ChatListScreen
import com.example.bluetoothguys.view.ui.screens.common.LoadingScreen
import com.example.bluetoothguys.view.ui.screens.common.PermissionScreen
import com.example.bluetoothguys.view.ui.screens.devices.DevicesScreen
import com.example.bluetoothguys.view_model.ChatViewModel
import com.example.bluetoothguys.view_model.Screen

@Composable
fun BluetoothMessengerApp(vm: ChatViewModel) {
    var screen: Screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf(Screen.ChatList) }

    // Forces permission re-check after system dialogs return.
    var permissionNonce by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val hasConnectPermission = remember(permissionNonce, context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }
    val hasScanPermission = remember(permissionNonce, context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionNonce++
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

    val missingPermissions = remember(hasConnectPermission, hasScanPermission) {
        buildList {
            if (!hasConnectPermission) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasScanPermission) add(Manifest.permission.BLUETOOTH_SCAN)
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
            BackHandler {
                vm.setActiveContact(null)
                screen = Screen.ChatList
            }

            val contacts by vm.contacts.collectAsState()
            val contact = contacts.firstOrNull { it.id == s.contactId }
            if (contact == null) {
                if (contacts.isNotEmpty()) {
                    screen = Screen.ChatList
                } else {
                    LoadingScreen(text = "Загрузка чата…")
                }
                return
            }

            val messages by vm.observeMessagesUi(contact.id).collectAsState(initial = emptyList())
            var text by rememberSaveable(contact.id) { mutableStateOf("") }

            val conn by vm.connectionState.collectAsState()
            val connectionState = conn
            val subtitle =
                when (connectionState) {
                    is ConnectionState.Connected ->
                        if (connectionState.address == contact.macAddress) {
                            "в сети • Bluetooth"
                        } else {
                            "не подключено • Bluetooth"
                        }
                    is ConnectionState.Connecting ->
                        if (connectionState.address == contact.macAddress) {
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
                vm.markChatRead(contact.id, contact.macAddress)
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
                    vm.sendOutgoingMessage(contactId = contact.id, address = contact.macAddress, text = msg)
                },
                onBack = {
                    vm.setActiveContact(null)
                    screen = Screen.ChatList
                },
            )
        }
    }
}

private val ScreenSaver: Saver<Screen, String> =
    Saver(
        save = { s ->
            when (s) {
                Screen.ChatList -> "chat_list"
                Screen.Devices -> "devices"
                is Screen.Chat -> "chat:${s.contactId}"
            }
        },
        restore = { raw ->
            when {
                raw == "chat_list" -> Screen.ChatList
                raw == "devices" -> Screen.Devices
                raw.startsWith("chat:") -> {
                    val id = raw.removePrefix("chat:").toLongOrNull()
                    if (id == null) Screen.ChatList else Screen.Chat(id)
                }
                else -> Screen.ChatList
            }
        },
    )
