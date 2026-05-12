package com.example.bluetoothguys.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetoothguys.model.Message
import com.example.bluetoothguys.model.db.entities.MessageStatus

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .background(
                    if (message.isMine) Color(0xFF2D6CDF) else Color(0xFFEFEFEF),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    message.text,
                    color = if (message.isMine) Color.White else Color.Black
                )
                Row(
                    modifier =
                        Modifier.align(
                            if (message.isMine) Alignment.End else Alignment.Start,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        message.time,
                        fontSize = 10.sp,
                        color = if (message.isMine) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    )
                    if (message.isMine) {
                        StatusIcon(
                            status = message.status,
                            tint =
                                when (message.status) {
                                    MessageStatus.READ -> Color(0xFFBFE3FF)
                                    MessageStatus.ERROR -> Color(0xFFFFB4AB)
                                    else -> Color.White.copy(alpha = 0.7f)
                                },
                            size = 14.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: MessageStatus?, tint: Color, size: Dp) {
    val icon =
        when (status) {
            MessageStatus.SENDING -> Icons.Default.Schedule
            MessageStatus.SENT -> Icons.Default.Done      // Всегда две галочки
            MessageStatus.DELIVERED -> Icons.Default.Done // Всегда две галочки
            MessageStatus.READ -> Icons.Default.DoneAll      // Всегда две галочки
            MessageStatus.ERROR -> Icons.Default.ErrorOutline
            null -> null
        } ?: return

    Spacer(modifier = Modifier.width(2.dp))
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size),
    )
}
