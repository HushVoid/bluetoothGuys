package com.example.bluetoothguys.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}

private val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add columns with defaults so old installs keep working.
            db.execSQL("ALTER TABLE messages ADD COLUMN clientMessageId TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT'")
            // Old messages are treated as already read to avoid surprising badges.
            db.execSQL("ALTER TABLE messages ADD COLUMN isRead INTEGER NOT NULL DEFAULT 1")
        }
    }

