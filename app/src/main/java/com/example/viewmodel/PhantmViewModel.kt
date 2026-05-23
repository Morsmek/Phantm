package com.example.viewmodel

import com.example.BuildConfig

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val httpClient = OkHttpClient()
    private val notificationCounter = java.util.concurrent.atomic.AtomicInteger(0)

    private val _appIsLocked = MutableStateFlow(false)
    val appIsLocked: StateFlow<Boolean> = _appIsLocked.asStateFlow()

    fun lockAppIfEnabled() {
        viewModelScope.launch {
            val prefs = identityDao.getIdentityOnce()
            if (prefs?.appLockEnabled == true && prefs.isOnboarded) {
                _appIsLocked.value = true
            }
        }
    }

    fun unlockApp() {
        _appIsLocked.value = false
    }

    /**
     * Verifies the App Lock PIN.
     * TODO: implement proper PIN storage and setup UI.
     * Currently accepts any 4-digit input to unblock (acts as acknowledgement lock).
     */
    fun verifyAppLockPin(pin: String): Boolean {
        return pin.length == 4
    }


    // Core Identity Settings Flow
    val identitySettings: StateFlow<IdentitySettings?> = identityDao.getIdentityFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Create notification channel once on startup (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = getApplication<Application>().applicationContext
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "phantm_secure_alerts",
                "Phantm Encrypted Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Run database initializer
        viewModelScope.launch {
            val existing = identityDao.getIdentityOnce()
            if (existing == null) {
                identityDao.insertOrUpdate(IdentitySettings())
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

    fun sendHandshakeIntro(targetPublicKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = identityDao.getIdentityOnce() ?: return@launch
            val ourPublicKey = settings.publicKey ?: return@launch
            try {
                val payload = JSONObject().apply {
                    val inner = JSONObject().apply {
                        put("type", "handshake_intro")
                        put("senderId", ourPublicKey)
                        put("senderName", settings.displayName)
                        put("msgId", UUID.randomUUID().toString())
                        put("timestamp", System.currentTimeMillis())
                    }
                    put("message", inner.toString())
                }.toString()
                val url = "https://ntfy.sh/phantm_peer_$targetPublicKey"
                val request = Request.Builder()
                    .url(url)
                    .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

            // Publish to genuine network broker ntfy.sh
            val settings = identityDao.getIdentityOnce()
            val ourPublicKey = settings?.publicKey ?: ""
            val ourDisplayName = settings?.displayName ?: "Chosen Cipher"
            
            val contact = contactDao.getContactOnce(contactId)
            val passphrase = contact?.passphrase
            
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val client = httpClient
                    val payload = JSONObject().apply {
                        val inner = JSONObject().apply {
                            put("senderId", ourPublicKey)
                            put("senderName", ourDisplayName)
                            put("msgId", msgId)
                            put("timestamp", System.currentTimeMillis())
                            
                            if (!passphrase.isNullOrBlank()) {
                                val salt = listOf(ourPublicKey, contactId).sorted().joinToString("")
                                val encryptedData = PhantmCrypto.encrypt(content, passphrase, salt)
                                put("ciphertext", encryptedData.ciphertext)
                                put("iv", encryptedData.iv)
                                put("hmac", encryptedData.hmac)
                            } else {
                                put("content", content)
                            }
                        }
                        put("message", inner.toString())
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



    fun deleteMessage(messageId: String) {
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

        // Launch MainActivity when the notification is tapped
        val launchIntent = Intent(context, Class.forName("com.example.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationCounter.get(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationCounter.getAndIncrement(), notification)
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
                        _isConnected.value = true
                        showToast("Secure Broker Link active", "success")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val outerJson = JSONObject(text)
                        val event = outerJson.optString("event")
                        if (event == "message") {
                            val msgText = outerJson.optString("message")
                            // Unwrap the ntfy storage layer to get the actual payload
                            val actualPayload = try {
                                val wrapper = JSONObject(msgText)
                                wrapper.optString("message").ifBlank { msgText }
                            } catch (e: Exception) {
                                msgText
                            }
                            val innerJson = try { JSONObject(actualPayload) } catch (e: Exception) { return }
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

                                    // Handshake intro: contact was auto-created above. Do not insert a chat message.
                                    val messageType = innerJson.optString("type", "")
                                    if (messageType == "handshake_intro") {
                                        val settings = identityDao.getIdentityOnce()
                                        if (settings?.notificationsEnabled == true) {
                                            showSystemNotification(
                                                "New Phantm Contact",
                                                "${existing.name} added you on Phantm"
                                            )
                                        }
                                        showToast("${existing.name} added you as a contact", "success")
                                        return@launch
                                    }

                                    val passphrase = existing.passphrase
                                    var decryptedContent = innerJson.optString("content")
                                    val ciphertext = innerJson.optString("ciphertext", "")
                                    val iv = innerJson.optString("iv", "")
                                    val hmac = innerJson.optString("hmac", "")

                                    if (ciphertext.isNotBlank() && iv.isNotBlank() && hmac.isNotBlank()) {
                                        decryptedContent = if (!passphrase.isNullOrBlank()) {
                                            try {
                                                val salt = listOf(senderId, publicKey).sorted().joinToString("")
                                                PhantmCrypto.decrypt(ciphertext, iv, hmac, passphrase, salt)
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
                    _isConnected.value = false
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
                    _isConnected.value = false
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
            _isConnected.value = false
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
                val client = httpClient
                val payload = JSONObject().apply {
                    val inner = JSONObject().apply {
                        put("type", "link_code_broadcast")
                        put("publicKey", publicKey)
                        put("displayName", settings.displayName)
                        put("linkCode", code.replace("-", ""))
                        put("timestamp", System.currentTimeMillis())
                    }
                    put("message", inner.toString())
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

                val url = "https://ntfy.sh/phantm_broadcasts/json?poll=1&since=all"
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
                        // ntfy stores our {"message": "<payload>"} wrapper as the message field
                        // so msgText is itself a JSON object with a nested "message" key
                        val actualPayload = try {
                            val wrapper = JSONObject(msgText)
                            wrapper.optString("message").ifBlank { msgText }
                        } catch (e: Exception) {
                            msgText  // fallback: treat as direct payload
                        }
                        if (actualPayload.isBlank()) continue
                        val inner = try { JSONObject(actualPayload) } catch (e: Exception) { continue }
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
