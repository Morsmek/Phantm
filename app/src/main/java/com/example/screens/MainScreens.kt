@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.components.*
import com.example.crypto.Bip39
import com.example.crypto.PhantmCrypto
import com.example.types.Contact
import com.example.types.Conversation
import com.example.types.Message
import com.example.ui.theme.*
import com.example.viewmodel.PhantmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatsListScreen(
    viewModel: PhantmViewModel,
    onChatSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filteredChats = remember(searchQuery, conversations) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter {
                it.contactName.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhantmLogoIcon(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CHATS",
                    color = CyberTextPrimary,
                    fontSize = 24.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                ShimmerBadge()
            }

            // Cyber search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter cipher channels...", color = CyberTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = CyberTextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 12.dp)
                    .testTag("chats_search_input")
            )

            // Dynamic conversation items stream
            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = CyberBorder,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO SECURE REPLIES FOUND",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Link a contact node to initiate handshake.",
                            color = CyberTextSecondary.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredChats) { chat ->
                        ConversationItem(
                            conversation = chat,
                            onClick = { onChatSelected(chat.id) },
                            onDelete = { viewModel.removeContact(chat.id) }
                        )
                    }
                }
            }
        }

        // Cyber FAB corresponding to start chats flow
        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = CyberCyan,
            contentColor = CyberBlack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "New chat node")
        }

        // New Chat Dialog Popup
        if (showNewChatDialog) {
            var selectedContactId by remember { mutableStateOf("") }
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()

            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                title = {
                    Text(
                        text = "START SECURE handshakes",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Select a linked contact node to open an encrypted communication tunnel.",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (contacts.isEmpty()) {
                            Text(
                                text = "No contacts linked. Go to Contacts and link a peer's public key first.",
                                color = CyberRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(contacts) { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedContactId = contact.id
                                            }
                                            .background(if (selectedContactId == contact.id) CyberCyan.copy(alpha = 0.1f) else Color.Transparent)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Avatar(name = contact.name, size = 32.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = contact.name,
                                                color = CyberTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = PhantmCrypto.truncateKey(contact.id),
                                                color = CyberTextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = MonospaceFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedContactId.isNotEmpty()) {
                                onChatSelected(selectedContactId)
                                showNewChatDialog = false
                            }
                        },
                        enabled = selectedContactId.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack)
                    ) {
                        Text("OPEN CHANNEL")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("CANCEL", color = CyberTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showActionSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showActionSheet = true }
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = conversation.contactName, size = 48.dp)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.contactName,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (conversation.isEncrypted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "End-to-End Encrypted",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val timeStr = remember(conversation.lastMessageAt) { formatter.format(Date(conversation.lastMessageAt)) }
                Text(
                    text = timeStr,
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessagePreview,
                    color = if (conversation.unreadCount > 0) CyberCyan else CyberTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(CyberCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = CyberBlack,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                }
            }
        }
    }

    // Long press sheet options
    PhantmBottomSheet(
        isOpen = showActionSheet,
        onClose = { showActionSheet = false },
        title = "SECURE CHANNEL CONTROLS"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDelete()
                    showActionSheet = false
                }
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = CyberRed)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "TRUNCATE CHANNEL & PURGE DATA",
                color = CyberRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = MonospaceFontFamily
            )
        }
    }
}

@Composable
fun ChatDetailScreen(
    viewModel: PhantmViewModel,
    contactId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.getMessagesForContact(contactId).collectAsStateWithLifecycle(initialValue = emptyList())
    var inputText by remember { mutableStateOf("") }
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val peerName = remember(contacts, contactId) {
        contacts.find { it.id == contactId }?.name ?: "Secure Peer"
    }
    val isE2EE = remember(contacts, contactId) {
        contacts.find { it.id == contactId }?.hasPassphrase == true
    }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Slide-up Bottom Options sheet
    var activeBottomSheetMessage by remember { mutableStateOf<Message?>(null) }
    val context = LocalContext.current

    // Set communications as read
    LaunchedEffect(contactId, messages) {
        viewModel.markAsRead(contactId)
        if (messages.isNotEmpty()) {
            scope.launch {
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Chat Header View block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .drawBehind {
                    // bottom highlight cyber divider
                    drawLine(
                        color = CyberBorder,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Exit channel", tint = CyberCyan)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Avatar(name = peerName, size = 40.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peerName,
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isE2EE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "End-to-End Encrypted",
                            tint = CyberCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = PhantmCrypto.truncateKey(contactId),
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            ShimmerBadge()
        }

        // messages scrolling canvas
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { msg ->
                MessageBubble(
                    content = msg.content,
                    timestamp = msg.timestamp,
                    isSent = msg.isSent,
                    status = msg.status,
                    onLongPress = { activeBottomSheetMessage = msg }
                )
            }
        }

        // bottom chat entry bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBlack)
                .drawBehind {
                    // top elegant thin border
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .navigationBarsPadding() // Notch/gesture bar safe padding!
                .imePadding() // Adjust for software keyboard height!
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = if (isE2EE) "Write E2EE message..." else "Write packet (E2EE disabled)...",
                        color = CyberTextSecondary.copy(alpha = 0.5f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_textfield"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(contactId, inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CyberCyan)
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Transmit",
                    tint = CyberBlack
                )
            }
        }
    }

    // Message Sheet Actions
    PhantmBottomSheet(
        isOpen = activeBottomSheetMessage != null,
        onClose = { activeBottomSheetMessage = null },
        title = "SECURE PACKET DECOY"
    ) {
        activeBottomSheetMessage?.let { selectedMsg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Encrypted Message", selectedMsg.content))
                        activeBottomSheetMessage = null
                        viewModel.showToast("Copied content", "success")
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(16.dp))
                Text("COPY CIPHERTEXT", color = CyberTextPrimary, fontSize = 13.sp, fontFamily = MonospaceFontFamily)
            }

            Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.deleteMessage(contactId, selectedMsg.id)
                        activeBottomSheetMessage = null
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = CyberRed)
                Spacer(modifier = Modifier.width(16.dp))
                Text("PURGE MESSAGE MEMORY", color = CyberRed, fontSize = 13.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContactsListScreen(
    viewModel: PhantmViewModel,
    onContactSelected: (String) -> Unit,
    onAddContactSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var searchContactQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchContactQuery) {
        if (searchContactQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchContactQuery, ignoreCase = true) ||
                it.id.contains(searchContactQuery, ignoreCase = true)
            }
        }
    }

    var activeDialogContact by remember { mutableStateOf<Contact?>(null) }
    var renameValue by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhantmLogoIcon(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PEER CONTACTS",
                    color = CyberTextPrimary,
                    fontSize = 24.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Self Sovereign Identity Card
            identity?.let { self ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp).padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCard)
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MY CRYPTOGRAPHIC SHIELD",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = self.displayName,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = self.publicKey?.let { PhantmCrypto.truncateKey(it, 12) } ?: "Unavailable",
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }

                        // Copy identity key actions on Click
                        IconButton(onClick = {
                            self.publicKey?.let { pk ->
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Phantm ID", pk))
                                viewModel.showToast("Public identity copied", "success")
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy key", tint = CyberCyan)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Animating Pulse
                        DataPulse(modifier = Modifier.size(44.dp))
                    }
                }
            }

            // Search Filter
            OutlinedTextField(
                value = searchContactQuery,
                onValueChange = { searchContactQuery = it },
                placeholder = { Text("Filter contacts...", color = CyberTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 12.dp)
                    .testTag("contacts_search_input")
            )

            // Contact items column
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = CyberBorder,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO PEERS CONNECTED",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredContacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onContactSelected(contact.id) },
                                    onLongClick = {
                                        activeDialogContact = contact
                                        renameValue = contact.name
                                    }
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(name = contact.name, size = 44.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.name,
                                        color = CyberTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (contact.hasPassphrase) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "E2EE Active",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = PhantmCrypto.truncateKey(contact.id, 10),
                                    color = CyberTextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = MonospaceFontFamily
                                )
                            }
                            // Call Action Chevron
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyberBorder)
                        }
                    }
                }
            }
        }

        // Add Contacts trigger FAB
        FloatingActionButton(
            onClick = { onAddContactSelected() },
            containerColor = CyberCyan,
            contentColor = CyberBlack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add contact link")
        }

        // Options sheet for selected contact
        PhantmBottomSheet(
            isOpen = activeDialogContact != null,
            onClose = { activeDialogContact = null },
            title = "PEER LINK MANIPULATIONS"
        ) {
            activeDialogContact?.let { selection ->
                Text(
                    text = "ID: ${selection.id}",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Update contact handle", color = CyberCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.renameContact(selection.id, renameValue)
                        activeDialogContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RENAME HANDLE")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.removeContact(selection.id)
                        activeDialogContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DELETE LINK")
                }
            }
        }
    }
}

@Composable
fun AddContactScreen(
    viewModel: PhantmViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Enter ID, 1: Scan QR
    var peerIdInput by remember { mutableStateOf("") }
    var peerAliasInput by remember { mutableStateOf("") }
    var peerPassphraseInput by remember { mutableStateOf("") }
    var showPassphrase by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
    ) {
        // Simple back action header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = CyberCyan)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "LINK NEW PEER",
                color = CyberTextPrimary,
                fontSize = 20.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Custom segment slider tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (activeTab == 0) CyberCyan else Color.Transparent)
                    .clickable { activeTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ENTER KEY",
                    color = if (activeTab == 0) CyberBlack else CyberTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (activeTab == 1) CyberCyan else Color.Transparent)
                    .clickable { activeTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SCAN QR CODE",
                    color = if (activeTab == 1) CyberBlack else CyberTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (activeTab == 0) {
            // Text Enter ID
            Text(
                text = "PEER PUBLIC ID (64-CHAR HEX)",
                color = CyberCyan,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = peerIdInput,
                onValueChange = { peerIdInput = it.filter { ch -> ch.isLetterOrDigit() }.take(64) },
                placeholder = { Text("01af9b...", color = CyberTextSecondary.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontFamily = MonospaceFontFamily),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peer_id_input_textfield")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "CONTACT HANDLE ALIAS (OPTIONAL)",
                color = CyberCyan,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = peerAliasInput,
                onValueChange = { peerAliasInput = it },
                placeholder = { Text("Cipher Agent", color = CyberTextSecondary.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peer_alias_input_textfield")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PRE-SHARED PASSPHRASE (OPTIONAL FOR E2EE)",
                color = CyberCyan,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = peerPassphraseInput,
                onValueChange = { peerPassphraseInput = it },
                placeholder = { Text("Enter secret to encrypt conversation", color = CyberTextSecondary.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard
                ),
                shape = RoundedCornerShape(8.dp),
                visualTransformation = if (showPassphrase) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (showPassphrase) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { showPassphrase = !showPassphrase }) {
                        Icon(icon, contentDescription = "Toggle passphrase visibility", tint = CyberCyan)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peer_passphrase_input_textfield")
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (peerIdInput.length == 64) {
                        viewModel.addContact(peerIdInput, peerAliasInput, peerPassphraseInput.takeIf { it.isNotBlank() })
                        onBack()
                    } else {
                        viewModel.showToast("Handshake require 64-char key length.", "error")
                    }
                },
                enabled = peerIdInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = CyberBlack,
                    disabledContainerColor = CyberCyan.copy(alpha = 0.2f),
                    disabledContentColor = CyberTextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_peer_button")
            ) {
                Text(
                    text = "ESTABLISH SECURE LINK",
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily,
                    letterSpacing = 1.sp
                )
            }
        } else {
            // Viewfinder Mock Scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(2.dp, CyberCyan, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Viewfinder framing visual with animated scanner sweep
                val infiniteTransition = rememberInfiniteTransition(label = "scanner")
                val scannerY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 280f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scannerSweep"
                )

                // The glowing cyber scan wire
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = scannerY.dp - 140.dp)
                        .background(CyberCyan)
                        .drawBehind {
                            drawRect(
                                color = CyberCyan.copy(alpha = 0.4f),
                                size = size.copy(height = 20.dp.toPx())
                            )
                        }
                )

                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = CyberCyan.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "VISOR LOCK ACTIVE...",
                    color = CyberCyan.copy(alpha = 0.5f),
                    fontFamily = MonospaceFontFamily,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hold camera adjacent to a peer's shared Phantm QR code.",
                color = CyberTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Simulate scanning identity in 500ms
                    val mockPeerId = "a2002341b52bcce1030e4dbdf" + "0".repeat(39) // deterministic fake key length 64
                    viewModel.addContact(mockPeerId, "Scanned Cipher Node")
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SCAN SIMULATION KEY", fontFamily = MonospaceFontFamily)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ProfileScreen(
    viewModel: PhantmViewModel,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    val isVerified by viewModel.bioVerified.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var isEditingName by remember { mutableStateOf(false) }
    var editingNameValue by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as? Activity

    // Toggle FLAG_SECURE locally on the system window of current activity
    // whenever they enter or leave a session on viewing recovery seeds!
    LaunchedEffect(isVerified) {
        if (isVerified) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            viewModel.showToast("Secure Display On: Screenshot blocked", "info")
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Clean up window secure flags on Composable disposal
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            viewModel.lockBio()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start)
                    .padding(top = 24.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhantmLogoIcon(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CHANNELS ARCHIVE",
                    color = CyberTextPrimary,
                    fontSize = 21.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Shield Visual Pulse
            DataPulse(modifier = Modifier.size(96.dp))

            Spacer(modifier = Modifier.height(24.dp))

            identity?.let { self ->
                if (isEditingName) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editingNameValue,
                            onValueChange = { editingNameValue = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            viewModel.setDisplayName(editingNameValue)
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm", tint = CyberCyan)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlitchText(text = self.displayName)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            editingNameValue = self.displayName
                            isEditingName = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit alias", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phantm ID block
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            self.publicKey?.let { pk ->
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Phantm ID", pk))
                                viewModel.showToast("Phantm ID copied", "success")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: " + (self.publicKey?.let { PhantmCrypto.truncateKey(it, 8) } ?: "N/A"),
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Verification block / Display Recovery Seed Phrase
                if (isVerified) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCard)
                            .border(1.dp, CyberRed, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyberRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCREEN SECURED (FLAG_SECURE ON)",
                                color = CyberRed,
                                fontSize = 11.sp,
                                fontFamily = MonospaceFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Seed Words chips grid
                        self.mnemonic?.let { phrase ->
                            val words = phrase.split(" ")
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.height(140.dp)
                            ) {
                                items(words.size) { index ->
                                    WordChip(number = index + 1, word = words[index])
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.lockBio() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBorder, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("HIDE PRIVATE KEY", fontFamily = MonospaceFontFamily)
                        }
                    }
                } else {
                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("VIEW RECOVERY SEED", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Requires biometric or security PIN unlock",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Bio PIN verification dialogues
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinDialog = false
                    pinValue = ""
                    pinError = false
                },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                title = {
                    Text(
                        text = "SECURITY CHECK",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Enter your authorization PIN to unlock security fields (Demo PIN is 1337 or any 4 digits)",
                            color = CyberTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = { pinValue = it.filter { ch -> ch.isDigit() }.take(4) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            label = { Text("Enter 4-digit PIN", color = CyberCyan) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError) {
                            Text(
                                text = "Validation error: Input correct PIN code",
                                color = CyberRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (viewModel.verifyBioPin(pinValue)) {
                                showPinDialog = false
                                pinValue = ""
                                pinError = false
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack)
                    ) {
                        Text("VERIFY")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinDialog = false
                        pinValue = ""
                        pinError = false
                    }) {
                        Text("CANCEL", color = CyberTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: PhantmViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhantmLogoIcon(modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "PREFERENCES",
                color = CyberTextPrimary,
                fontSize = 24.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        identity?.let { prefs ->
            // Section 1: Notifications
            Text(
                text = "COMMUNICATION PROTOCOL",
                color = CyberCyan,
                fontSize = 12.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Channel Notifications", color = CyberTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Alert on packet transmissions", color = CyberTextSecondary, fontSize = 11.sp)
                        }
                        ToggleSwitch(
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Previews", color = CyberTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Leak sender and content ciphertext", color = CyberTextSecondary, fontSize = 11.sp)
                        }
                        ToggleSwitch(
                            checked = prefs.showNotificationPreview,
                            onCheckedChange = { viewModel.toggleShowNotificationPreview() },
                            disabled = !prefs.notificationsEnabled
                        )
                    }
                }
            }

            // Section 2: Integrity & Purge timers
            Text(
                text = "SECURITY INTEGRITY",
                color = CyberCyan,
                fontSize = 12.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Node App Lock", color = CyberTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Trigger screen lock on standby", color = CyberTextSecondary, fontSize = 11.sp)
                        }
                        ToggleSwitch(
                            checked = prefs.appLockEnabled,
                            onCheckedChange = { viewModel.toggleAppLock() }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Auto delete selection Row spinner
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Auto-Delete Timer", color = CyberTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Purge old decrypted local history packets", color = CyberTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(0 to "OFF", 7 to "7 DAYS", 30 to "30 DAYS", 90 to "90 DAYS")
                            options.forEach { (days, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (prefs.autoDeleteDays == days) CyberCyan else CyberSurface)
                                        .border(1.dp, if (prefs.autoDeleteDays == days) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setAutoDeleteDays(days) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (prefs.autoDeleteDays == days) CyberBlack else CyberTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = MonospaceFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Wipe Identity dangerous zone
            Text(
                text = "DANGER TUNNEL ZONE",
                color = CyberRed,
                fontSize = 12.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { showWipeConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("wipe_identity_button")
            ) {
                Text(
                    text = "WIPE SECURE IDENTITY",
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "PHANTM SECURE NODE v1.0.0-CYBER",
                color = CyberTextSecondary,
                fontSize = 10.sp,
                fontFamily = MonospaceFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Wipe Warning Alert
        if (showWipeConfirm) {
            AlertDialog(
                onDismissRequest = { showWipeConfirm = false },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                title = {
                    Text(
                        text = "CRITICAL DATA PURGE WARNING",
                        color = CyberRed,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you absolutely sure you want to delete your Phantm node keys and erase any linked partners or messages history forever? This is destructive and irreversible.",
                        color = CyberTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetIdentity()
                            showWipeConfirm = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = Color.White)
                    ) {
                        Text("PURGE DESTRUCTIVELY")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirm = false }) {
                        Text("CANCEL", color = CyberTextSecondary)
                    }
                }
            )
        }
    }
}
