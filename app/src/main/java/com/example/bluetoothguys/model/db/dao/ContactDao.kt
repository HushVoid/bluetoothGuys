package com.example.bluetoothguys.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bluetoothguys.model.db.entities.ContactEntity
import com.example.bluetoothguys.model.db.entities.ContactWithLastMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT c.*,
               m.text AS lastMessageText,
               m.sentAt AS lastMessageAt
        FROM contacts c
        LEFT JOIN messages m
          ON m.id = (
            SELECT id FROM messages
            WHERE contactId = c.id
            ORDER BY sentAt DESC, id DESC
            LIMIT 1
          )
        ORDER BY COALESCE(m.sentAt, 0) DESC, c.name COLLATE NOCASE ASC
        """,
    )
    fun observeAllWithLastMessage(): Flow<List<ContactWithLastMessage>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getByMac(macAddress: String): ContactEntity?
}

