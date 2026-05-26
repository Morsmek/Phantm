package com.morsmek.phantm.viewmodel

import com.morsmek.phantm.BuildConfig

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
import com.morsmek.phantm.crypto.Bip39
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.crypto.PhantmLinkCode
import com.morsmek.phantm.db.*
import com.morsmek.phantm.types.Contact
import com.morsmek.phantm.types.Conversation
import com.morsmek.phantm.types.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.morsmek.phantm.repository.PhantmRepository
import com.morsmek.phantm.repository.BroadcastState

class PhantmViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val identityDao = db.identityDao()
    private val contactDao = db.contactDao()
    private val messageDao = db.messageDao()
    private val pendingRequestDao = db.pendingRequestDao()

    val pendingRequests: StateFlow<List<PendingRequestEntity>> = pendingRequestDao.getAllPendingRequestsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val phantmRepository = PhantmRepository(
        identityDao = identityDao,
        contactDao = contactDao,
        messageDao = messageDao,
        pendingRequestDao = pendingRequestDao,
        coroutineScope = viewModelScope,
        onNotificationTrigger = { title, body -> showSystemNotification(title, body) },
        onToastTrigger = { msg, type -> showToast(msg, type) }
    )

    val broadcastState: StateFlow<BroadcastState> = phantmRepository.broadcastState

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

    val isConnected: StateFlow<Boolean> = phantmRepository.isConnected

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
     * Verifies the App Lock PIN using PBKDF2 hashing.
     */
    fun verifyAppLockPin(pin: String): Boolean {
        val prefs = identitySettings.value ?: return false
        if (!prefs.appLockEnabled || prefs.appLockPinHash.isNullOrBlank() || prefs.appLockSalt.isNullOrBlank()) {
            return pin.length == 4 // Fallback if somehow lock is on without pin setup
        }
        val computedHash = PhantmCrypto.hashAppLockPin(pin, prefs.appLockSalt)
        val isCorrect = computedHash == prefs.appLockPinHash
        if (isCorrect) {
            if (prefs.appLockFailedAttempts > 0) {
                viewModelScope.launch {
                    identityDao.insertOrUpdate(prefs.copy(appLockFailedAttempts = 0, appLockLastFailedAt = 0L))
                }
            }
        } else {
            viewModelScope.launch {
                identityDao.insertOrUpdate(prefs.copy(
                    appLockFailedAttempts = prefs.appLockFailedAttempts + 1,
                    appLockLastFailedAt = System.currentTimeMillis()
                ))
            }
        }
        return isCorrect
    }

    fun setAppLockPin(pin: String) {
        viewModelScope.launch {
            val prefs = identitySettings.value ?: return@launch
            if (pin.length < 4) {
                showToast("PIN must be at least 4 digits", "error")
                return@launch
            }
            val salt = PhantmCrypto.generateSalt()
            val hash = PhantmCrypto.hashAppLockPin(pin, salt)
            identityDao.insertOrUpdate(prefs.copy(
                appLockEnabled = true,
                appLockPinHash = hash,
                appLockSalt = salt,
                appLockFailedAttempts = 0,
                appLockLastFailedAt = 0L
            ))
            showToast("App Lock Enabled", "success")
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            val prefs = identitySettings.value ?: return@launch
            identityDao.insertOrUpdate(prefs.copy(
                appLockEnabled = false,
                appLockPinHash = null,
                appLockSalt = null,
                appLockFailedAttempts = 0,
                appLockLastFailedAt = 0L
            ))
            showToast("App Lock Disabled", "info")
        }
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
                    phantmRepository.setupWebSocketConnection(settings.publicKey, settings.displayName)
                } else {
                    phantmRepository.disconnectWebSocket()
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
            phantmRepository.disconnectWebSocket()
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
        phantmRepository.sendMessage(contactId, content)
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

    // toggleAppLock removed, handled by setAppLockPin and disableAppLock through UI

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
        val launchIntent = Intent(context, Class.forName("com.morsmek.phantm.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationCounter.get(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.morsmek.phantm.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationCounter.getAndIncrement(), notification)
    }

    val linkedContactId: SharedFlow<String> = phantmRepository.linkedContactId

    private val _nfcIncomingContact = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val nfcIncomingContact = _nfcIncomingContact.asSharedFlow()

    fun onNfcContactReceived(publicKey: String, displayName: String) {
        viewModelScope.launch {
            _nfcIncomingContact.emit(Pair(publicKey, displayName))
        }
    }

    fun startBroadcast() {
        phantmRepository.startBroadcast()
    }

    fun acceptRequest(peerKey: String, peerName: String) {
        phantmRepository.acceptRequest(peerKey, peerName)
    }

    fun rejectRequest(peerKey: String) {
        viewModelScope.launch {
            pendingRequestDao.deleteRequest(peerKey)
        }
    }

    fun stopBroadcast() {
        phantmRepository.stopBroadcast()
    }

    fun joinByCode(
        enteredCode: String,
        introMessage: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        phantmRepository.joinByCode(enteredCode, introMessage, onResult)
    }

    override fun onCleared() {
        super.onCleared()
        phantmRepository.disconnectWebSocket()
    }
}

data class ToastMessage(
    val message: String,
    val type: String // "info" | "success" | "error"
)
