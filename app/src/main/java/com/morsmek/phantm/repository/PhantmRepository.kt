package com.morsmek.phantm.repository

import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.crypto.PhantmLinkCode
import com.morsmek.phantm.db.ContactDao
import com.morsmek.phantm.db.ContactEntity
import com.morsmek.phantm.db.IdentityDao
import com.morsmek.phantm.db.MessageDao
import com.morsmek.phantm.db.MessageEntity
import com.morsmek.phantm.db.PendingRequestDao
import com.morsmek.phantm.db.PendingRequestEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class BroadcastState {
    object Idle : BroadcastState()
    object Listening : BroadcastState()
    data class PeerConnected(val name: String) : BroadcastState()
}

class PhantmRepository(
    private val identityDao: IdentityDao,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val pendingRequestDao: PendingRequestDao,
    private val coroutineScope: CoroutineScope,
    private val onNotificationTrigger: (title: String, body: String) -> Unit,
    private val onToastTrigger: (msg: String, type: String) -> Unit
) {
    private var currentConnectedKey: String? = null
    private var webSocket: WebSocket? = null
    private var rendezvousWs: WebSocket? = null
    private var wsJob: kotlinx.coroutines.Job? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _broadcastState = MutableStateFlow<BroadcastState>(BroadcastState.Idle)
    val broadcastState: StateFlow<BroadcastState> = _broadcastState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _linkedContactId = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val linkedContactId: SharedFlow<String> = _linkedContactId.asSharedFlow()

    fun setupWebSocketConnection(publicKey: String, displayName: String) {
        if (currentConnectedKey == publicKey && webSocket != null) return

        disconnectWebSocket()
        currentConnectedKey = publicKey

        wsJob = coroutineScope.launch(Dispatchers.IO) {
            val topic = PhantmCrypto.deriveTopic(publicKey)
            val wsUrl = "wss://ntfy.sh/$topic/ws"
            val request = Request.Builder().url(wsUrl).build()

            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    coroutineScope.launch {
                        _isConnected.value = true
                        onToastTrigger("Secure Broker Link active", "success")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val outerJson = JSONObject(text)
                        if (outerJson.optString("event") != "message") return

                        val msgText = outerJson.optString("message")
                        val innerJson = try {
                            JSONObject(msgText)
                        } catch (_: Exception) {
                            return
                        }

                        val senderId = innerJson.optString("senderId")
                        val senderName = innerJson.optString("senderName")
                        val msgId = innerJson.optString("msgId")
                        val timestamp = innerJson.optLong("timestamp", System.currentTimeMillis())

                        if (senderId.length != 64 || senderId == publicKey || msgId.isBlank()) return

                        coroutineScope.launch(Dispatchers.IO) {
                            var existing = contactDao.getContactOnce(senderId)
                            if (existing == null) {
                                val newContact = ContactEntity(
                                    id = senderId,
                                    name = if (senderName.isNotBlank()) senderName else "Peer_${senderId.substring(0, 8)}"
                                )
                                contactDao.insertContact(newContact)
                                existing = newContact
                            }

                            val messageType = innerJson.optString("type", "")

                            if (messageType == "handshake_intro") {
                                _linkedContactId.tryEmit(senderId)

                                val settings = identityDao.getIdentityOnce()
                                if (settings?.notificationsEnabled == true) {
                                    onNotificationTrigger("New Phantm Contact", "${existing.name} added you on Phantm")
                                }
                                onToastTrigger("${existing.name} added you as a contact", "success")
                                return@launch
                            }

                            if (messageDao.getMessageOnce(msgId) != null) return@launch

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
                                    } catch (_: SecurityException) {
                                        "[Decryption Failed: Verification check failed. Passphrase mismatch.]"
                                    } catch (_: Exception) {
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
                                val displayTitle =
                                    if (settings.showNotificationPreview) "New Phantm from $peerName" else "Phantm Secure Alert"
                                val displayBody =
                                    if (settings.showNotificationPreview) decryptedContent else "End-to-end encrypted packet received."
                                onNotificationTrigger(displayTitle, displayBody)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isConnected.value = false
                    scheduleReconnect(displayName)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    if (code != 1000) scheduleReconnect(displayName)
                }
            })
        }
    }

    private fun scheduleReconnect(displayName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            delay(5000)
            val retryKey = currentConnectedKey ?: return@launch
            currentConnectedKey = null
            val currentName = identityDao.getIdentityOnce()?.displayName ?: displayName
            setupWebSocketConnection(retryKey, currentName)
        }
    }

    fun disconnectWebSocket() {
        _isConnected.value = false
        webSocket?.close(1000, "Clean shutdown")
        webSocket = null
        wsJob?.cancel()
        wsJob = null
        currentConnectedKey = null
    }

    fun startBroadcast() {
        coroutineScope.launch(Dispatchers.IO) {
            val settings = identityDao.getIdentityOnce() ?: return@launch
            val myPublicKey = settings.publicKey ?: return@launch
            val code = PhantmLinkCode.generate(myPublicKey)
            val topic = PhantmLinkCode.rendezvousTopic(code)

            rendezvousWs?.cancel()

            val request = Request.Builder()
                .url("wss://ntfy.sh/$topic/ws")
                .build()

            rendezvousWs = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    coroutineScope.launch { _broadcastState.value = BroadcastState.Listening }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val outer = JSONObject(text)
                        if (outer.optString("event") != "message") return
                        val body = outer.optString("message")
                        val hello = JSONObject(body)
                        if (hello.optString("type") != "rv_hello") return

                        val peerKey = hello.optString("publicKey")
                        val peerName = hello.optString("displayName").ifBlank { "Peer_${peerKey.take(8)}" }
                        val introMessage = hello.optString("introMessage").ifBlank { "Let's connect on Phantm" }
                        if (peerKey.length != 64) return

                        coroutineScope.launch(Dispatchers.IO) {
                            pendingRequestDao.insertRequest(
                                PendingRequestEntity(id = peerKey, name = peerName, introMessage = introMessage)
                            )
                            onToastTrigger("New connection request from $peerName", "info")
                        }
                    } catch (_: Exception) {
                    }
                }
            })
        }
    }

    fun stopBroadcast() {
        rendezvousWs?.cancel()
        rendezvousWs = null
        _broadcastState.value = BroadcastState.Idle
    }

    fun acceptRequest(peerKey: String, peerName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val settings = identityDao.getIdentityOnce() ?: return@launch
            val myPublicKey = settings.publicKey ?: return@launch
            val myName = settings.displayName

            if (contactDao.getContactOnce(peerKey) == null) {
                contactDao.insertContact(ContactEntity(id = peerKey, name = peerName))
            }
            pendingRequestDao.deleteRequest(peerKey)

            sendRendezvousAck(peerKey, myPublicKey, myName)

            _linkedContactId.tryEmit(peerKey)
            onToastTrigger("$peerName added", "success")
            stopBroadcast()
        }
    }

    private fun sendRendezvousAck(peerKey: String, myPublicKey: String, myName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("type", "handshake_intro")
                    put("senderId", myPublicKey)
                    put("senderName", myName)
                    put("msgId", UUID.randomUUID().toString())
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                val topic = PhantmCrypto.deriveTopic(peerKey)
                val request = Request.Builder()
                    .url("https://ntfy.sh/$topic")
                    .post(body.toRequestBody("text/plain".toMediaTypeOrNull()))
                    .build()

                httpClient.newCall(request).execute().close()
            } catch (_: Exception) {
            }
        }
    }

    fun joinByCode(
        enteredCode: String,
        introMessage: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val normalised = enteredCode.trim().uppercase().replace("-", "")
            if (normalised.length != 8) {
                withContext(Dispatchers.Main) { onResult(false, "Code must be 8 characters") }
                return@launch
            }

            val settings = identityDao.getIdentityOnce()
            val myPublicKey = settings?.publicKey
            val myName = settings?.displayName ?: "Phantm User"
            if (myPublicKey == null) {
                withContext(Dispatchers.Main) { onResult(false, "Identity not ready") }
                return@launch
            }

            val topic = PhantmLinkCode.rendezvousTopic(normalised)

            try {
                val hello = JSONObject().apply {
                    put("type", "rv_hello")
                    put("publicKey", myPublicKey)
                    put("displayName", myName)
                    put("introMessage", introMessage.take(50))
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                val request = Request.Builder()
                    .url("https://ntfy.sh/$topic")
                    .post(hello.toRequestBody("text/plain".toMediaTypeOrNull()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            onResult(true, "Connection request sent")
                        } else {
                            onResult(false, "Could not reach rendezvous channel")
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Network error — check connection") }
            }
        }
    }

    fun sendMessage(contactId: String, content: String) {
        if (content.isBlank()) return

        coroutineScope.launch(Dispatchers.IO) {
            val msgId = UUID.randomUUID().toString()
            val localMsg = MessageEntity(
                id = msgId,
                contactId = contactId,
                content = content,
                isSent = true,
                status = "sent"
            )
            messageDao.insertMessage(localMsg)

            val settings = identityDao.getIdentityOnce()
            val ourPublicKey = settings?.publicKey ?: ""
            val ourDisplayName = settings?.displayName ?: "Chosen Cipher"
            val contact = contactDao.getContactOnce(contactId)
            val passphrase = contact?.passphrase

            try {
                val payload = JSONObject().apply {
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
                }.toString()

                val topic = PhantmCrypto.deriveTopic(contactId)
                val request = Request.Builder()
                    .url("https://ntfy.sh/$topic")
                    .post(payload.toRequestBody("text/plain".toMediaTypeOrNull()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        messageDao.insertMessage(localMsg.copy(status = "delivered"))
                    } else {
                        onToastTrigger("Broker queued; counterpart offline.", "info")
                    }
                }
            } catch (_: Exception) {
                onToastTrigger("Packet cached locally. Delivery pending network.", "info")
            }
        }
    }
}
