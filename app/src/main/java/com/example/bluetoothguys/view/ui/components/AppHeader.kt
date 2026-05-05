package com.example.bluetoothguys.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    rightIcon: ImageVector? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D6CDF))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            if (showBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 20.sp)
                subtitle?.let {
                    Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            rightIcon?.let {
                Icon(it, contentDescription = null, tint = Color.White)
            }
        }
    }
}