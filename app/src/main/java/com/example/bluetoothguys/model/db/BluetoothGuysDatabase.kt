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

/**
 * Room-база данных приложения.
 *
 * Содержит две таблицы:
 * - contacts (контакты/устройства для подключения)
 * - messages (история сообщений по контакту)
 */
@Database(
    entities = [
        ContactEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    // Для учебного/пет-проекта схему не экспортируем в файлы.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BluetoothGuysDatabase : RoomDatabase() {
    /** DAO для CRUD по контактам. */
    abstract fun contactDao(): ContactDao

    /** DAO для CRUD по сообщениям. */
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: BluetoothGuysDatabase? = null

        /**
         * Получить singleton-инстанс базы.
         *
         * Важно использовать `applicationContext`, чтобы база не держала ссылку на Activity/Screen
         * и не создавала утечки памяти.
         */
        fun getInstance(context: Context): BluetoothGuysDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BluetoothGuysDatabase::class.java,
                    // Имя файла базы данных на устройстве.
                    "bluetooth_guys.db",
                ).build().also { INSTANCE = it }
            }
    }
}

