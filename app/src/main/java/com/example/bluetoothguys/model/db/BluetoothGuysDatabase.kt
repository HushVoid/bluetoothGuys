package com.example.bluetoothguys.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.bluetoothguys.model.db.dao.ContactDao
import com.example.bluetoothguys.model.db.dao.MessageDao
import com.example.bluetoothguys.model.db.entities.ContactEntity
import com.example.bluetoothguys.model.db.entities.MessageEntity

@Database(
    entities = [
        ContactEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BluetoothGuysDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: BluetoothGuysDatabase? = null

        fun getInstance(context: Context): BluetoothGuysDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BluetoothGuysDatabase::class.java,
                    "bluetooth_guys.db",
                ).build().also { INSTANCE = it }
            }
    }
}

