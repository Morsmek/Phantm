package com.morsmek.phantm.types

data class Contact(
    val id: String,          // 64-char hex public key
    val name: String,
    val addedAt: Long,
    val hasPassphrase: Boolean = false
)

data class Message(
    val id: String,          // UUID acting as ULID
    val content: String,
    val timestamp: Long,
    val isSent: Boolean,
    val status: String       // 'sent' | 'delivered' | 'read'
)

data class Conversation(
    val id: String,          // Contact public key ID
    val contactName: String,
    val messages: List<Message>,
    val lastMessageAt: Long,
    val unreadCount: Int,
    val lastMessagePreview: String,
    val isEncrypted: Boolean = false
)

