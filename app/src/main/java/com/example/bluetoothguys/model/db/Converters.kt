package com.example.bluetoothguys.model.db

import androidx.room.TypeConverter
import com.example.bluetoothguys.model.db.entities.MessageDirection
import com.example.bluetoothguys.model.db.entities.MessageStatus

class Converters {
    @TypeConverter
    fun messageDirectionToString(value: MessageDirection): String = value.name

    @TypeConverter
    fun stringToMessageDirection(value: String): MessageDirection = MessageDirection.valueOf(value)

    @TypeConverter
    fun messageStatusToString(value: MessageStatus): String = value.name

    @TypeConverter
    fun stringToMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}

