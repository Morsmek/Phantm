package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identity_settings")
data class IdentitySettings(
    @PrimaryKey val id: Int = 1,
    val mnemonic: String? = null,
    val publicKey: String? = null,
    val displayName: String = "Chosen Cipher",
    val isOnboarded: Boolean = false,
    val pinCode: String? = null, // pin code input for view recovery / app lock
    val appLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val showNotificationPreview: Boolean = true,
    val autoDeleteDays: Int = 0 // Auto-delete timer: 0 (off), 7, 30, 90 days
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String, // 64-character public key hex
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
    val passphrase: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String, // UUID string acting as ULID
    val contactId: String, // the conversation target contact public key ID
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean,
    val status: String // 'sent' | 'delivered' | 'read'
)
