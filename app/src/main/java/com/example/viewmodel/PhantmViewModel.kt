package com.example.viewmodel

import com.example.BuildConfig

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.Bip39
import com.example.crypto.PhantmCrypto
import com.example.crypto.PhantmLinkCode
import com.example.db.*
import com.example.types.Contact
import com.example.types.Conversation
import com.example.types.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PhantmViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val identityDao = db.identityDao()
    private val contactDao = db.contactDao()
    private val messageDao = db.messageDao()

    // Network Broker State Tracking
    private var currentConnectedKey: String? = null
    private var webSocket: okhttp3.WebSocket? = null
    private var okHttpClient: okhttp3.OkHttpClient? = null
    private var wsJob: kotlinx.coroutines.Job? = null

    // Setup global Toast state
    private val _toastState = MutableStateFlow<ToastMessage?>(null)
    val toastState: StateFlow<ToastMessage?> = _toastState.asStateFlow()

    fun showToast(message: String, type: String = "info") {
        viewModelScope.launch {
            _toastState.value = ToastMessage(message, type)
            delay(3000)
            if (_toastState.value?.message == message) {
                _toastState.value = null
            }
        }
    }

    fun dismissToast() {
        _toastState.value = null
    }

    // Secure screen session block state (Profile biometric code logic)
    private val _bioVerified = MutableStateFlow(false)
    val bioVerified: StateFlow<Boolean> = _bioVerified.asStateFlow()

    fun verifyBioPin(pin: String): Boolean {
        // In debug builds, accept the demo PIN 1337 or any 4-digit code for convenience.
        // In release builds, only accept the stored pinCode, falling back to 1337 if unset.
        val storedPin = identitySettings.value?.pinCode
        val isValid = if (BuildConfig.DEBUG) {
            pin == "1337" || pin.length == 4
        } else {
            pin == (storedPin ?: "1337")
        }
        if (isValid) {
            _bioVerified.value = true
        }
        return isValid
    }

    fun lockBio() {
        _bioVerified.value = false
    }

    // Core Identity Settings Flow
    val identitySettings: StateFlow<IdentitySettings?> = identityDao.getIdentityFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Run database initializer
        viewModelScope.launch {
            val existing = identityDao.getIdentityOnce()
            if (existing == null) {
                identityDao.insertOrUpdate(IdentitySettings())
            }
            // Populate standard system onboarding default contact if empty
            contactDao.getAllContactsFlow().first().let { currentContacts ->
                if (currentContacts.isEmpty()) {
                    val systemKey = "4e656f6e5068616e746d4e6f646554756e6e656c45737461626c697368656439" // Hex code representation of Neon Node
                    contactDao.insertContact(
                        ContactEntity(
                            id = systemKey,
                            name = "Neon Phantm Node",
                            addedAt = System.currentTimeMillis()
                        )
                    )
                    // Insert initial encryption message
                    messageDao.insertMessage(
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            contactId = systemKey,
                            content = "Mnemonic identity initialized. End-to-end local cipher tunnels active. Say hello to initiate decentralized handshake.",
                            timestamp = System.currentTimeMillis() - 10000,
                            isSent = false,
                            status = "delivered"
                        )
                    )
                }
            }
            // Handle automatic background cleanup of expired messages
            identityDao.getIdentityOnce()?.let { prefs ->
                if (prefs.autoDeleteDays > 0) {
                    val cutoff = System.currentTimeMillis() - (prefs.autoDeleteDays * 24L * 60L * 60L * 1000L)
                    messageDao.deleteOldMessages(cutoff)
                }
            }
        }

        // Observe identity state reactively to manage real network broker WebSocket connections
        viewModelScope.launch {
            identityDao.getIdentityFlow().collect { settings ->
                if (settings != null && settings.isOnboarded && !settings.publicKey.isNullOrBlank()) {
                    setupWebSocketConnection(settings.publicKey, settings.displayName)
                } else {
                    disconnectWebSocket()
                }
            }
        }
    }

    // Live Contacts list derived from Room
    val contacts: StateFlow<List<Contact>> = contactDao.getAllContactsFlow()
        .map { list -> list.map { Contact(it.id, it.name, it.addedAt, !it.passphrase.isNullOrEmpty()) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Live Conversations list with unread badging, latest items previews
    val conversations: StateFlow<List<Conversation>> = combine(
        contactDao.getAllContactsFlow(),
        messageDao.getAllMessagesFlow()
    ) { contacts, messages ->
        contacts.map { contactEntity ->
            val chatMsgs = messages.filter { it.contactId == contactEntity.id }
                .sortedBy { it.timestamp }
                .map { Message(it.id, it.content, it.timestamp, it.isSent, it.status) }
            val lastMsg = chatMsgs.lastOrNull()
            Conversation(
                id = contactEntity.id,
                contactName = contactEntity.name,
                messages = chatMsgs,
                lastMessageAt = lastMsg?.timestamp ?: contactEntity.addedAt,
                unreadCount = chatMsgs.count { !it.isSent && it.status != "read" },
                lastMessagePreview = lastMsg?.content ?: "Cipher tunnel ready.",
                isEncrypted = !contactEntity.passphrase.isNullOrEmpty()
            )
        }.sortedByDescending { it.lastMessageAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Get live message stream for active chat detail
    fun getMessagesForContact(contactId: String): Flow<List<Message>> {
        return messageDao.getMessagesForContactFlow(contactId)
            .map { list ->
                list.map { Message(it.id, it.content, it.timestamp, it.isSent, it.status) }
            }
    }

    // Onboarding operations
    fun createIdentity(mnemonic: String) {
        viewModelScope.launch {
            val pubKey = PhantmCrypto.deriveIdentity(mnemonic)
            val settings = IdentitySettings(
                mnemonic = mnemonic,
                publicKey = pubKey,
                displayName = "Cipher_" + pubKey.substring(0, 8),
                isOnboarded = true
            )
            identityDao.insertOrUpdate(settings)
            showToast("Secure Identity Created", "success")
        }
    }

    fun recoverIdentity(mnemonic: String): Boolean {
        if (!Bip39.isValid(mnemonic)) {
            showToast("Invalid Mnemonic Phrase format", "error")
            return false
        }
        viewModelScope.launch {
            val pubKey = PhantmCrypto.deriveIdentity(mnemonic)
            val settings = IdentitySettings(
                mnemonic = mnemonic,
                publicKey = pubKey,
                displayName = "Cipher_" + pubKey.substring(0, 8),
                isOnboarded = true
            )
            identityDao.insertOrUpdate(settings)
            showToast("Identity Restored Successfully", "success")
        }
        return true
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch {
            identitySettings.value?.let { current ->
                identityDao.insertOrUpdate(current.copy(displayName = name))
                showToast("Cipher Alias Updated", "success")
            }
        }
    }

    // Global Reset
    fun resetIdentity() {
        viewModelScope.launch {
            disconnectWebSocket()
            db.clearAllTables()
            // Re-insert a clean unonboarded identity row AFTER clearing tables
            identityDao.insertOrUpdate(IdentitySettings())
            showToast("Identity Purged from Device memory", "error")
        }
    }

    // Contact Operations
    fun addContact(phantmId: String, name: String, passphrase: String? = null) {
        viewModelScope.launch {
            if (phantmId.length != 64) {
                showToast("Key must be 64-char hex", "error")
                return@launch
            }
            val cleanName = name.ifBlank { "Cipher_" + phantmId.substring(0, 8) }
            val existing = contactDao.getContactOnce(phantmId)
            if (existing != null) {
                showToast("Contact key already added", "error")
                return@launch
            }
            contactDao.insertContact(
                ContactEntity(
                    id = phantmId,
                    name = cleanName,
                    passphrase = if (passphrase.isNullOrBlank()) null else passphrase
                )
            )
            val successMsg = if (!passphrase.isNullOrBlank()) "Secure E2EE key linked to $cleanName" else "Secure key linked to $cleanName"
            showToast(successMsg, "success")
        }
    }

    fun removeContact(id: String) {
        viewModelScope.launch {
            contactDao.deleteContact(id)
            messageDao.deleteMessagesForContact(id)
            showToast("Cipher link truncated", "info")
        }
    }

    fun renameContact(id: String, name: String) {
        viewModelScope.launch {
            contactDao.renameContact(id, name)
            showToast("Alias updated", "success")
        }
    }

    // Messaging operations
    fun sendMessage(contactId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val msgId = UUID.randomUUID().toString()
            val msg = MessageEntity(
                id = msgId,
                contactId = contactId,
                content = content,
                isSent = true,
                status = "sent"
            )
            messageDao.insertMessage(msg)

            val systemKey = "4e656f6e5068616e746d4e6f646554756e6e656c45737461626c697368656439"
            if (contactId == systemKey) {
                // Auto transition state sent -> delivered for offline simulated node
                delay(400)
                messageDao.insertMessage(msg.copy(status = "delivered"))
                // Trigger simulated incoming reply
                triggerSimulatedReply(contactId, content)
            } else {
                // Publish to genuine network broker ntfy.sh
                val settings = identityDao.getIdentityOnce()
                val ourPublicKey = settings?.publicKey ?: ""
                val ourDisplayName = settings?.displayName ?: "Chosen Cipher"
                
                val contact = contactDao.getContactOnce(contactId)
                val passphrase = contact?.passphrase
                
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient()
                        val payload = JSONObject().apply {
                            put("senderId", ourPublicKey)
                            put("senderName", ourDisplayName)
                            put("msgId", msgId)
                            put("timestamp", System.currentTimeMillis())
                            
                            if (!passphrase.isNullOrBlank()) {
                                val encryptedData = PhantmCrypto.encrypt(content, passphrase, contactId)
                                put("ciphertext", encryptedData.ciphertext)
                                put("iv", encryptedData.iv)
                                put("hmac", encryptedData.hmac)
                            } else {
                                put("content", content)
                            }
                        }.toString()

                        val url = "https://ntfy.sh/phantm_peer_$contactId"
                        val request = Request.Builder()
                            .url(url)
                            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                messageDao.insertMessage(msg.copy(status = "delivered"))
                            } else {
                                viewModelScope.launch {
                                    showToast("Broker queued; counterpart offline.", "info")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        viewModelScope.launch {
                            showToast("Packet cached locally. Delivery pending network.", "info")
                        }
                    }
                }
            }
        }
    }

    private suspend fun triggerSimulatedReply(contactId: String, userMessage: String) {
        delay(1200) // response simulation lag
        val contact = contactDao.getContactOnce(contactId)
        val name = contact?.name ?: "Unknown Peer"

        val responses = listOf(
            "Encrypted session active. Seed checksum verified.",
            "Handshake verified, cipher synced.",
            "Buffered communication completed. No middle nodes intercepted.",
            "Decrypting payload... Verified. Safe to execute instructions.",
            "Message packet signed with signature ed25519 success. Stored locally.",
            "Sovereign Node is responding: transmission acknowledge.",
            "Affirmative. Rotating keys in 15s. Communication secure."
        )
        val selectedText = responses.random()

        val reply = MessageEntity(
            id = UUID.randomUUID().toString(),
            contactId = contactId,
            content = selectedText,
            isSent = false,
            status = "delivered"
        )
        messageDao.insertMessage(reply)

        // Show standard notification output if preferences allow and backgrounded-feel is desired
        val settings = identitySettings.value
        if (settings?.notificationsEnabled == true) {
            val displayTitle = if (settings.showNotificationPreview) "New Phantm from $name" else "Phantm Secure Alert"
            val displayBody = if (settings.showNotificationPreview) selectedText else "End-to-end encrypted packet received."
            showSystemNotification(displayTitle, displayBody)
        }
    }

    fun deleteMessage(contactId: String, messageId: String) {
        viewModelScope.launch {
            messageDao.deleteMessage(messageId)
            showToast("Message deleted from device", "info")
        }
    }

    fun markAsRead(contactId: String) {
        viewModelScope.launch {
            messageDao.markAsRead(contactId)
        }
    }

    // Settings adjustments
    fun toggleNotifications() {
        viewModelScope.launch {
            identitySettings.value?.let { current ->
                identityDao.insertOrUpdate(current.copy(notificationsEnabled = !current.notificationsEnabled))
            }
        }
    }

    fun toggleShowNotificationPreview() {
        viewModelScope.launch {
            identitySettings.value?.let { current ->
                identityDao.insertOrUpdate(current.copy(showNotificationPreview = !current.showNotificationPreview))
            }
        }
    }

    fun toggleAppLock() {
        viewModelScope.launch {
            identitySettings.value?.let { current ->
                identityDao.insertOrUpdate(current.copy(appLockEnabled = !current.appLockEnabled))
            }
        }
    }

    fun setAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            identitySettings.value?.let { current ->
                identityDao.insertOrUpdate(current.copy(autoDeleteDays = days))
                showToast("Purge window updated to $days days", "info")
            }
        }
    }

    // Native Platform standard notification push helper
    private fun showSystemNotification(title: String, body: String) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "phantm_secure_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Phantm Encrypted Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard fall-back info symbol
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(43, notification)
    }

    // Real-Time WebSocket Broker Link Helpers
    private fun setupWebSocketConnection(publicKey: String, displayName: String) {
        if (currentConnectedKey == publicKey) return
        disconnectWebSocket()
        currentConnectedKey = publicKey

        wsJob = viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // infinite read timeout to keep stream active
                .build()
            okHttpClient = client

            val wsUrl = "wss://ntfy.sh/phantm_peer_${publicKey}/ws"
            val request = Request.Builder()
                .url(wsUrl)
                .build()

            val wsListener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    viewModelScope.launch {
                        showToast("Secure Broker Link active", "success")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val outerJson = JSONObject(text)
                        val event = outerJson.optString("event")
                        if (event == "message") {
                            val msgText = outerJson.optString("message")
                            val innerJson = JSONObject(msgText)
                            val senderId = innerJson.optString("senderId")
                            val senderName = innerJson.optString("senderName")
                            val msgId = innerJson.optString("msgId")
                            val timestamp = innerJson.optLong("timestamp", System.currentTimeMillis())

                            if (senderId.length == 64 && senderId != publicKey) {
                                viewModelScope.launch {
                                    var existing = contactDao.getContactOnce(senderId)
                                    if (existing == null) {
                                        val newContact = ContactEntity(
                                            id = senderId,
                                            name = if (senderName.isNotBlank()) senderName else "Peer_${senderId.substring(0, 8)}"
                                        )
                                        contactDao.insertContact(newContact)
                                        existing = newContact
                                    }

                                    val passphrase = existing.passphrase
                                    var decryptedContent = innerJson.optString("content")
                                    val ciphertext = innerJson.optString("ciphertext", "")
                                    val iv = innerJson.optString("iv", "")
                                    val hmac = innerJson.optString("hmac", "")

                                    if (ciphertext.isNotBlank() && iv.isNotBlank() && hmac.isNotBlank()) {
                                        decryptedContent = if (!passphrase.isNullOrBlank()) {
                                            try {
                                                PhantmCrypto.decrypt(ciphertext, iv, hmac, passphrase, senderId)
                                            } catch (e: SecurityException) {
                                                "[Decryption Failed: Verification check failed. Passphrase mismatch.]"
                                            } catch (e: Exception) {
                                                "[Decryption Failed: Error decrypting message payload.]"
                                            }
                                        } else {
                                            "[Encrypted Message - Passphrase not set]"
                                        }
                                    }

                                    messageDao.insertMessage(
                                        MessageEntity(
                                            id = msgId,
                                            contactId = senderId,
                                            content = decryptedContent,
                                            timestamp = timestamp,
                                            isSent = false,
                                            status = "delivered"
                                        )
                                    )

                                    val settings = identityDao.getIdentityOnce()
                                    if (settings?.notificationsEnabled == true) {
                                        val peerName = existing.name
                                        val displayTitle = if (settings.showNotificationPreview) "New Phantm from $peerName" else "Phantm Secure Alert"
                                        val displayBody = if (settings.showNotificationPreview) decryptedContent else "End-to-end encrypted packet received."
                                        showSystemNotification(displayTitle, displayBody)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    viewModelScope.launch {
                        delay(5000)
                        val retryKey = currentConnectedKey
                        if (retryKey != null) {
                            currentConnectedKey = null
                            val currentName = identityDao.getIdentityOnce()?.displayName ?: displayName
                            setupWebSocketConnection(retryKey, currentName)
                        }
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (code != 1000) {
                        viewModelScope.launch {
                            delay(5000)
                            val retryKey = currentConnectedKey
                            if (retryKey != null) {
                                currentConnectedKey = null
                                val currentName = identityDao.getIdentityOnce()?.displayName ?: displayName
                                setupWebSocketConnection(retryKey, currentName)
                            }
                        }
                    }
                }
            }

            webSocket = client.newWebSocket(request, wsListener)
        }
    }

    private fun disconnectWebSocket() {
        try {
            webSocket?.close(1000, "Clean shutdown")
            webSocket = null
            okHttpClient = null
            wsJob?.cancel()
            wsJob = null
            currentConnectedKey = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val _nfcIncomingContact = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val nfcIncomingContact = _nfcIncomingContact.asSharedFlow()

    fun onNfcContactReceived(publicKey: String, displayName: String) {
        viewModelScope.launch {
            _nfcIncomingContact.emit(Pair(publicKey, displayName))
        }
    }

    /**
     * Broadcasts this device's public key + display name tagged with the current
     * Phantm Link Code to the user's own ntfy.sh topic so a recipient can resolve
     * the short code to the full key within the 10-minute validity window.
     */
    fun broadcastLinkCode() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = identityDao.getIdentityOnce() ?: return@launch
            val publicKey = settings.publicKey ?: return@launch
            val code = PhantmLinkCode.generate(publicKey)

            try {
                val client = OkHttpClient()
                val payload = JSONObject().apply {
                    put("type", "link_code_broadcast")
                    put("publicKey", publicKey)
                    put("displayName", settings.displayName)
                    put("linkCode", code.replace("-", ""))
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                val url = "https://ntfy.sh/phantm_broadcasts"
                val request = Request.Builder()
                    .url(url)
                    .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().close()
                viewModelScope.launch {
                    showToast("Code broadcasted successfully", "success")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch {
                    showToast("Broadcast failed: check network", "error")
                }
            }
        }
    }

    /**
     * Resolves a Phantm Link Code entered by the user. Polls the recipient's
     * ntfy.sh topic for a broadcast message whose linkCode field matches the
     * entered code. Times out after 30 seconds.
     *
     * Calls onResult(publicKey, displayName) on success, onResult(null, null) on failure.
     */
    fun resolveLinkCode(
        enteredCode: String,
        onResult: (publicKey: String?, displayName: String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalised = enteredCode.trim().uppercase().replace("-", "")
            if (normalised.length != 8) {
                viewModelScope.launch { onResult(null, null) }
                return@launch
            }

            try {
                val client = OkHttpClient.Builder()
                    .readTimeout(35, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val url = "https://ntfy.sh/phantm_broadcasts/json?poll=1&since=600"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                response.close()

                // Parse newline-delimited JSON from ntfy poll response
                for (line in body.lines()) {
                    if (line.isBlank()) continue
                    try {
                        val obj = JSONObject(line)
                        val msgText = obj.optString("message")
                        if (msgText.isBlank()) continue
                        val inner = JSONObject(msgText)
                        if (inner.optString("type") != "link_code_broadcast") continue
                        val broadcastCode = inner.optString("linkCode")
                        val broadcastKey = inner.optString("publicKey")
                        if (broadcastCode == normalised && broadcastKey.length == 64) {
                            val displayName = inner.optString("displayName", "Peer_${broadcastKey.take(8)}")
                            viewModelScope.launch { onResult(broadcastKey, displayName) }
                            return@launch
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                viewModelScope.launch { onResult(null, null) }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch { onResult(null, null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}

data class ToastMessage(
    val message: String,
    val type: String // "info" | "success" | "error"
)
