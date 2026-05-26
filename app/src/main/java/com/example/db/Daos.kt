package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identity_settings WHERE id = 1 LIMIT 1")
    fun getIdentityFlow(): Flow<IdentitySettings?>

    @Query("SELECT * FROM identity_settings WHERE id = 1 LIMIT 1")
    suspend fun getIdentityOnce(): IdentitySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: IdentitySettings)

    @Query("DELETE FROM identity_settings")
    suspend fun clearIdentity()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("UPDATE contacts SET name = :name WHERE id = :id")
    suspend fun renameContact(id: String, name: String)

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactOnce(id: String): ContactEntity?
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getMessagesForContactFlow(contactId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteMessagesForContact(contactId: String)

    @Query("UPDATE messages SET status = 'read' WHERE contactId = :contactId AND isSent = 0 AND status != 'read'")
    suspend fun markAsRead(contactId: String)

    @Query("DELETE FROM messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldMessages(cutoffTimestamp: Long)
}

@Dao
interface PendingRequestDao {
    @Query("SELECT * FROM pending_requests ORDER BY timestamp ASC")
    fun getAllPendingRequestsFlow(): Flow<List<PendingRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PendingRequestEntity)

    @Query("DELETE FROM pending_requests WHERE id = :id")
    suspend fun deleteRequest(id: String)
}
