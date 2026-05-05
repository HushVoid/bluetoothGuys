package com.example.bluetoothguys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bluetoothguys.view.ui.theme.BluetoothGuysTheme
import com.example.bluetoothguys.view_model.ChatViewModel

class MainActivity : ComponentActivity() {
    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothGuysTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        vm = vm,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val contacts by vm.contacts.collectAsState()

    Column(modifier = modifier) {
        Button(
            onClick = {
                vm.addContact(
                    name = "Test contact",
                    macAddress = "00:11:22:33:44:55",
                )
            },
        ) {
            Text("Add contact")
        }

        Button(
            onClick = {
                val first = contacts.firstOrNull() ?: return@Button
                vm.sendMessage(contactId = first.id, text = "Hello (OUT)")
                vm.receiveMessage(contactId = first.id, text = "Hi (IN)")
            },
        ) {
            Text("Add 2 messages to first contact")
        }

        Text("Contacts: ${contacts.size}")
        contacts.forEach { c ->
            Text("- ${c.id}: ${c.name} (${c.macAddress})")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothGuysTheme {
        Text("Preview")
    }
}