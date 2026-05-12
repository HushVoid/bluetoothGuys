package com.example.bluetoothguys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.bluetoothguys.view.BluetoothMessengerApp
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
