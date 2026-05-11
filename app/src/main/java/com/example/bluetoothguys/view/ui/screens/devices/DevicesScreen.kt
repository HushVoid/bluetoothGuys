package com.example.bluetoothguys.view.ui.screens.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bluetoothguys.bluetooth.DiscoveredDevice
import com.example.bluetoothguys.view.ui.components.AppHeader

@Composable
fun DevicesScreen(
    bluetoothEnabled: Boolean,
    isDiscovering: Boolean,
    bonded: List<DiscoveredDevice>,
    discovered: List<DiscoveredDevice>,
    onBack: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRefreshBonded: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (DiscoveredDevice) -> Unit,
) {
    Column {
        AppHeader(
            title = "Устройства",
            subtitle = "Поиск и подключение по Bluetooth",
            showBack = true,
            onBackClick = onBack,
            rightIcon = Icons.AutoMirrored.Filled.BluetoothSearching,
            onRightIconClick = {
                if (isDiscovering) onStopScan() else onStartScan()
            },
        )

        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (bluetoothEnabled) "Bluetooth включён" else "Bluetooth выключен",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )

                if (!bluetoothEnabled) {
                    Button(onClick = onEnableBluetooth) { Text("Включить") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefreshBonded) { Text("Пары") }
                Button(onClick = { if (isDiscovering) onStopScan() else onStartScan() }) {
                    Text(if (isDiscovering) "Стоп" else "Сканировать")
                }
            }
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
            if (bonded.isNotEmpty()) {
                item { SectionTitle("Сопряжённые") }
                items(bonded) { d ->
                    DeviceRow(device = d, onClick = { onDeviceClick(d) })
                }
            }

            if (discovered.isNotEmpty()) {
                item { Spacer(modifier = Modifier.padding(top = 8.dp)) }
                item { SectionTitle("Найденные рядом") }
                items(discovered) { d ->
                    DeviceRow(device = d, onClick = { onDeviceClick(d) })
                }
            }

            if (bonded.isEmpty() && discovered.isEmpty()) {
                item {
                    Text(
                        "Нет устройств. Обновите список пар или запустите сканирование.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun DeviceRow(device: DiscoveredDevice, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
    ) {
        Text(text = device.name ?: "Без имени", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = buildString {
                append(device.address)
                if (device.rssi != null) append(" • RSSI ${device.rssi}")
                if (device.bonded) append(" • paired")
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
