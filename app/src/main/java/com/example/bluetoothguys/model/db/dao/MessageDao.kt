package com.example.bluetoothguys.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.bluetoothguys.model.db.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteForContact(contactId: Long)

    @Query(
        """
        SELECT * FROM messages
        WHERE contactId = :contactId
        ORDER BY sentAt ASC, id ASC
        """,
    )
    fun observeForContact(contactId: Long): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE contactId = :contactId
        ORDER BY sentAt DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestForContact(contactId: Long): MessageEntity?
}

