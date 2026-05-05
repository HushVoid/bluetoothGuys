package com.example.bluetoothguys.model.db

import androidx.room.TypeConverter
import com.example.bluetoothguys.model.db.entities.MessageDirection

class Converters {
    @TypeConverter
    fun messageDirectionToString(value: MessageDirection): String = value.name

    @TypeConverter
    fun stringToMessageDirection(value: String): MessageDirection = MessageDirection.valueOf(value)
}

