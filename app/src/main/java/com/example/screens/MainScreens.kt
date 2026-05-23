@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontFamily
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.concurrent.Executors
import com.example.crypto.PhantmLinkCode
import androidx.compose.material3.CircularProgressIndicator
import com.example.crypto.PhantmNfc
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
            .drawDotGrid()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhantmScreenHeader(title = "Chats", trailingContent = { E2eeBadge() })

            // Minimalist bottom-line search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W300,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberCyan),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "SEARCH CONVERSATIONS",
                                        color = CyberTextTertiary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.W300,
                                        letterSpacing = 2.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                            // Bottom hairline — animates width on focus
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(CyberBorderMid)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Dynamic conversation items stream
            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Large ghost icon with ambient glow
                        Box(contentAlignment = Alignment.Center) {
                            // Glow bloom
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                CyberCyan.copy(alpha = 0.04f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = CyberTextTertiary.copy(alpha = 0.15f),
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        Text(
                            text = "CHATS",
                            color = CyberTextPrimary,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.W100,
                            letterSpacing = 8.sp
                        )
                        Text(
                            text = "NO SECURE REPLIES FOUND",
                            color = CyberCyan.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W300,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "LINK A CONTACT NODE TO INITIATE HANDSHAKE",
                            color = CyberTextTertiary.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W300,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
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
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .border(
                    width = 0.5.dp,
                    color = CyberCyan.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable { showNewChatDialog = true }
                .testTag("new_chat_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New chat node",
                tint = CyberCyan.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
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
            .combinedClickable(onClick = onClick, onLongClick = { showActionSheet = true })
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar — minimal outlined circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(0.5.dp, CyberBorderMid, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = conversation.contactName.take(2).uppercase(),
                color = CyberCyan.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.W300,
                letterSpacing = 1.sp
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.contactName,
                    color = CyberTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W300,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val timeStr = remember(conversation.lastMessageAt) {
                    formatter.format(Date(conversation.lastMessageAt))
                }
                Text(
                    text = timeStr,
                    color = CyberTextTertiary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (conversation.isEncrypted) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyberCyan.copy(alpha = 0.3f),
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = conversation.lastMessagePreview,
                    color = if (conversation.unreadCount > 0)
                        CyberTextSecondary
                    else
                        CyberTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W200,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(CyberCyan, CircleShape)
                    )
                }
            }
        }
    }

    // Thin hairline separator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 74.dp)  // Indented — doesn't reach edge
            .background(CyberBorder)
    )

    if (showActionSheet) {
        PhantmBottomSheet(
            isOpen = true,
            onClose = { showActionSheet = false },
            title = "CONTACT OPTIONS"
        ) {
            TextButton(
                onClick = { onDelete(); showActionSheet = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DELETE CONTACT", color = CyberRed, letterSpacing = 2.sp, fontSize = 11.sp)
            }
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

    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawDotGrid()
            .imePadding()
    ) {
        // Chat Header View block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .statusBarsPadding()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "encrypted â€¢ online",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = CyberCyan
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "COPY PEER KEY",
                                color = CyberTextPrimary,
                                fontFamily = MonospaceFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Peer ID", contactId))
                            viewModel.showToast("Copied peer key", "success")
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "PURGE CHAT HISTORY",
                                color = CyberRed,
                                fontFamily = MonospaceFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            viewModel.removeContact(contactId)
                            onBack()
                        }
                    )
                }
            }
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
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    cursorColor = CyberCyan
                ),
                prefix = { Text("> ", color = CyberCyan, fontFamily = MonospaceFontFamily) },
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

            HorizontalDivider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.deleteMessage(selectedMsg.id)
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
            .drawDotGrid()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhantmScreenHeader(title = "Contacts")

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
                    unfocusedContainerColor = CyberCard,
                    cursorColor = CyberCyan
                ),
                prefix = { Text("> ", color = CyberCyan, fontFamily = MonospaceFontFamily) },
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

val SineInOutEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

enum class SphereState {
    IDLE,
    PAIRING,
    LINKED
}

data class ParticleData(
    val theta: Float,
    val phi: Float,
    val rFactor: Float,
    val speedFactor: Float,
    val colorType: Int
)

@Composable
fun PhantmSphere(
    state: SphereState,
    modifier: Modifier = Modifier,
    qrBitmap: Bitmap? = null
) {
    val particles = remember {
        val random = java.util.Random(42)
        List(180) {
            val theta = kotlin.math.acos(random.nextFloat() * 2f - 1f)
            val phi = random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val rFactor = 0.85f + random.nextFloat() * 0.3f
            val speedFactor = 0.6f + random.nextFloat() * 0.8f
            val colorType = random.nextInt(2) // 0: Cyan, 1: Magenta
            ParticleData(theta, phi, rFactor, speedFactor, colorType)
        }
    }

    var angleX by remember { mutableStateOf(0f) }
    var angleY by remember { mutableStateOf(0f) }

    val targetSpeed = when (state) {
        SphereState.IDLE -> 0.006f
        SphereState.PAIRING -> 0.025f
        SphereState.LINKED -> 0.002f
    }
    val currentSpeed by animateFloatAsState(
        targetValue = targetSpeed,
        animationSpec = tween(1000),
        label = "speed"
    )

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                angleX += currentSpeed * 0.7f
                angleY += currentSpeed
            }
        }
    }

    val flattenFactor by animateFloatAsState(
        targetValue = if (state == SphereState.LINKED) 0.08f else 1.0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "flatten"
    )

    val expansionFactor by animateFloatAsState(
        targetValue = if (state == SphereState.LINKED) 2.2f else 1.0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "expand"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = SineInOutEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseVal"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) * 0.35f

        val rBase = baseRadius * expansionFactor * (if (state == SphereState.IDLE) pulseFactor else 1.0f)

        if (state != SphereState.LINKED) {
            drawCircle(
                color = CyberCyan.copy(alpha = 0.05f * (if (state == SphereState.PAIRING) 2f else 1f)),
                radius = rBase * 1.15f,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = CyberCyan.copy(alpha = 0.1f * (if (state == SphereState.PAIRING) 2f else 1f)),
                radius = rBase,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        } else {
            drawCircle(
                color = CyberCyan.copy(alpha = 0.15f),
                radius = rBase * 1.1f,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx())
            )
        }

        val projected = particles.map { p ->
            val r = rBase * p.rFactor
            val sinTheta = kotlin.math.sin(p.theta)
            val cosTheta = kotlin.math.cos(p.theta)
            val sinPhi = kotlin.math.sin(p.phi)
            val cosPhi = kotlin.math.cos(p.phi)

            val x0 = r * sinTheta * cosPhi
            val y0 = r * sinTheta * sinPhi * flattenFactor
            val z0 = r * cosTheta

            val cosY = kotlin.math.cos(angleY * p.speedFactor)
            val sinY = kotlin.math.sin(angleY * p.speedFactor)
            val x1 = x0 * cosY - z0 * sinY
            val z1 = x0 * sinY + z0 * cosY

            val cosX = kotlin.math.cos(angleX * p.speedFactor)
            val sinX = kotlin.math.sin(angleX * p.speedFactor)
            val y2 = y0 * cosX - z1 * sinX
            val z2 = y0 * sinX + z1 * cosX

            val px = centerX + x1
            val py = centerY + y2
            val zDepth = z2

            Triple(px, py, zDepth) to p
        }.sortedBy { it.first.third }

        // Helper to draw a single particle
        val drawParticle: (Triple<Float, Float, Float>, ParticleData) -> Unit = { pos, p ->
            val (px, py, zDepth) = pos
            val depthRatio = (zDepth / rBase).coerceIn(-1f, 1f)
            val alphaVal = (0.35f + 0.65f * (depthRatio + 1f) / 2f).coerceIn(0.1f, 1.0f)
            val scaleVal = (0.7f + 0.6f * (depthRatio + 1f) / 2f)

            val basePtSize = if (p.colorType == 0) 3.dp.toPx() else 4.dp.toPx()
            val ptSize = basePtSize * scaleVal

            val color = if (p.colorType == 0) {
                CyberCyan.copy(alpha = alphaVal)
            } else {
                Color(0xFFEA00FF).copy(alpha = alphaVal)
            }

            drawCircle(
                color = color,
                radius = ptSize,
                center = Offset(px, py)
            )

            if (depthRatio > 0.4f && state == SphereState.PAIRING) {
                drawCircle(
                    color = color.copy(alpha = alphaVal * 0.3f),
                    radius = ptSize * 2.5f,
                    center = Offset(px, py)
                )
            }
        }

        // Stage 1: Draw particles behind the center (zDepth <= 0)
        projected.filter { it.first.third <= 0f }.forEach { (pos, p) ->
            drawParticle(pos, p)
        }

        // Stage 2: Draw the QR Code in the center (only if provided)
        qrBitmap?.let { bitmap ->
            val imageBitmap = bitmap.asImageBitmap()
            val sizePx = (rBase * 0.85f).coerceIn(120.dp.toPx(), 200.dp.toPx())
            val left = centerX - sizePx / 2f
            val top = centerY - sizePx / 2f
            drawImage(
                image = imageBitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(sizePx.toInt(), sizePx.toInt())
            )
        }

        // Stage 3: Draw particles in front of the center (zDepth > 0)
        projected.filter { it.first.third > 0f }.forEach { (pos, p) ->
            drawParticle(pos, p)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
class PhantmBarcodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { qrValue ->
                            onQrDetected(qrValue)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun CameraScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // Stable callback ref â€” prevents analyzer from holding a stale lambda
    val onQrScannedRef = rememberUpdatedState(onQrScanned)

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            // Bind camera once in factory, never in update
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, PhantmBarcodeAnalyzer { qrValue ->
                            previewView.post { onQrScannedRef.value(qrValue) }
                        })
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
        // No update{} block â€” camera is bound once in factory
    )

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}

fun generateQrCode(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if (bitMatrix.get(x, y)) AndroidColor.WHITE else 0xFF070B0E.toInt()
                bitmap.setPixel(x, y, color)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun parsePhantmJoinUri(uriStr: String): String? {
    // Returns the 8-char link code, or null
    try {
        val uri = Uri.parse(uriStr)
        if (uri.scheme == "phantm" && uri.host == "join") {
            val code = uri.getQueryParameter("code")
            if (code != null && code.length == 8) return code
        }
    } catch (e: Exception) { }
    return null
}

fun parsePhantmSyncUri(uriStr: String): Pair<String, String>? {
    try {
        val uri = Uri.parse(uriStr)
        if (uri.scheme == "phantm" && uri.host == "sync") {
            val key = uri.getQueryParameter("key")
            val name = uri.getQueryParameter("name")
            if (key != null && key.length == 64) {
                return Pair(key, name ?: "")
            }
        }
    } catch (e: Exception) {
        // manual decode
    }
    if (uriStr.startsWith("phantm://sync?")) {
        val query = uriStr.substringAfter("phantm://sync?")
        val params = query.split("&").associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }
        val key = params["key"]
        val name = params["name"]?.let {
            try {
                java.net.URLDecoder.decode(it, "UTF-8")
            } catch (ex: Exception) {
                it
            }
        }
        if (key != null && key.length == 64) {
            return Pair(key, name ?: "")
        }
    }
    return null
}

@Composable
fun ShareMySphereDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    myPublicKey: String,
    myName: String
) {
    if (!isOpen) return

    val qrContent = "phantm://sync?key=$myPublicKey&name=${java.net.URLEncoder.encode(myName, "UTF-8")}"
    val qrBitmap = remember(qrContent) {
        generateQrCode(qrContent, 512, 512)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PHANTM SYNC IDENTITY",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(CyberCard)
                        .border(2.dp, CyberCyan, CircleShape)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.08f,
                        targetValue = 0.22f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = SineInOutEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = glowAlpha))
                    )

                    if (qrBitmap != null) {
                        PhantmSphere(
                            state = SphereState.IDLE,
                            modifier = Modifier.fillMaxSize(),
                            qrBitmap = qrBitmap
                        )
                    } else {
                        Text("FAIL TO GENERATE QR", color = CyberRed, fontFamily = MonospaceFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = myName.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "KEY: ${PhantmCrypto.truncateKey(myPublicKey, 16)}",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("CLOSE TRANSMISSION", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StandaloneQrDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    myPublicKey: String,
    myName: String
) {
    if (!isOpen) return

    val myCode = remember(myPublicKey) { PhantmLinkCode.generate(myPublicKey) }
    val qrContent = "phantm://join?code=${myCode.replace("-", "")}"
    val qrBitmap = remember(qrContent) { generateQrCode(qrContent, 768, 768) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCAN TO ADD ME",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Plain QR â€” no sphere overlay, readable by any QR app
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B0E))
                            .border(2.dp, CyberCyan, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCard)
                            .border(1.dp, CyberRed, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR GENERATION FAILED", color = CyberRed, fontFamily = MonospaceFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = myName.uppercase(),
                    color = CyberTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PhantmCrypto.truncateKey(myPublicKey, 12),
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Share button â€” lets the user send the QR URI via any app they choose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Phantm Sync Link", qrContent))
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY LINK", fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, qrContent)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Phantm Link"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHARE", fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("CLOSE", color = CyberTextSecondary, fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}

@Composable
fun AddContactScreen(
    viewModel: PhantmViewModel,
    onBack: () -> Unit,
    onContactLinked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var linkCodeInput by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted; if (granted) showScanner = true }

    // Use a local state for resolve error to avoid type mismatches
    var actualResolveError: String? by remember { mutableStateOf(null) }

    var showQrDialog by remember { mutableStateOf(false) }

    val identityForCode by viewModel.identitySettings.collectAsStateWithLifecycle()
    val myCode = remember(identityForCode?.publicKey) {
        identityForCode?.publicKey?.let { PhantmLinkCode.generate(it) } ?: "----"
    }
    var countdown by remember { mutableStateOf(PhantmLinkCode.secondsRemaining()) }
    val broadcastState by viewModel.broadcastState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopBroadcast() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            countdown = PhantmLinkCode.secondsRemaining()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawDotGrid()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MY BROADCAST CODE",
                color = CyberCyan,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = myCode,
                color = CyberTextPrimary,
                fontSize = 36.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Expires in ${countdown}s",
                color = if (countdown < 60) CyberRed else CyberTextSecondary,
                fontSize = 12.sp,
                fontFamily = MonospaceFontFamily
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = broadcastState) {
                is PhantmViewModel.BroadcastState.Idle -> {
                    Button(
                        onClick = { viewModel.startBroadcast() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("BROADCAST MY CODE", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                is PhantmViewModel.BroadcastState.Listening -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WAITING FOR PEER...", color = CyberCyan, fontSize = 11.sp, fontFamily = MonospaceFontFamily)
                    }
                }
                is PhantmViewModel.BroadcastState.PeerConnected -> {
                    Text("${state.name} CONNECTED ✓", color = CyberGreen, fontSize = 12.sp, fontFamily = MonospaceFontFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ENTER PEER'S CODE",
            color = CyberCyan,
            fontSize = 11.sp,
            fontFamily = MonospaceFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LinkCodeInput(
            code = linkCodeInput,
            onCodeChange = { 
                linkCodeInput = it 
                actualResolveError = null
            },
            modifier = Modifier.fillMaxWidth()
        )

        actualResolveError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = CyberRed, fontSize = 12.sp, fontFamily = MonospaceFontFamily)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isResolving = true
                actualResolveError = null
                viewModel.joinByCode(linkCodeInput) { success, message ->
                    isResolving = false
                    if (success) {
                        viewModel.showToast("Connecting to peer...", "success")
                        onContactLinked("")
                    } else {
                        actualResolveError = message
                    }
                }
            },
            enabled = linkCodeInput.length == 8 && !isResolving,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCyan,
                contentColor = CyberBlack,
                disabledContainerColor = CyberCyan.copy(alpha = 0.2f),
                disabledContentColor = CyberTextSecondary.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    color = CyberBlack,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (isResolving) "CONNECTING..." else "CONNECT",
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Text("HOW IT WORKS", color = CyberCyan, fontSize = 10.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "1. Tell your peer your 8-character code verbally, on paper, or in any chat.\n" +
                "2. Tap BROADCAST MY CODE.\n" +
                "3. They enter your code on their phone and tap CONNECT.",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("MORE OPTIONS", color = CyberCyan, fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { 
                viewModel.startBroadcast()
                showQrDialog = true 
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SHARE MY QR CODE", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (hasCameraPermission) {
                    showScanner = true
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SCAN QR CODE", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showQrDialog) {
            identityForCode?.let { self ->
                StandaloneQrDialog(
                    isOpen = true,
                    onClose = { showQrDialog = false },
                    myPublicKey = self.publicKey ?: "",
                    myName = self.displayName
                )
            }
        }

        if (showScanner) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBlack)
            ) {
                CameraScannerView(
                    onQrScanned = { qrValue ->
                        val code = parsePhantmJoinUri(qrValue)
                        if (code != null) {
                            showScanner = false
                            viewModel.joinByCode(code) { success, message ->
                                if (success) {
                                    viewModel.showToast("Connecting...", "success")
                                    onContactLinked("")
                                } else {
                                    viewModel.showToast(message, "error")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close button
                IconButton(
                    onClick = { showScanner = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close scanner",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Scan guide text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Point camera at peer's QR code",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ask them to open Identity → Share QR",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: PhantmViewModel,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    var isEditingName by remember { mutableStateOf(false) }
    var editingNameValue by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as? Activity

    // Clean up window secure flags on Composable disposal
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawDotGrid()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhantmScreenHeader(title = "Identity")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            val self = identity
            if (self != null) {
                        // token (Identity / QR Code / Public Key)
                        // Central lock icon inside glowing avatar
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(CyberSurface)
                                .border(2.dp, CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(4.dp, CyberCyan.copy(alpha = 0.2f), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Identity secured",
                                tint = CyberCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                        unfocusedBorderColor = CyberBorder,
                                        cursorColor = CyberCyan
                                    ),
                                    prefix = { Text("> ", color = CyberCyan, fontFamily = MonospaceFontFamily) },
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

                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(24.dp))



                        // Share details card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurface)
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "SHARE PHANTM USER DETAILS",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontFamily = MonospaceFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "ALIAS: ${self.displayName}",
                                color = CyberTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = MonospaceFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "PUBLIC KEY:\n${self.publicKey}",
                                color = CyberTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily,
                                lineHeight = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        self.publicKey?.let { pk ->
                                            val fullDetails = "Alias: ${self.displayName}\nPhantm ID: $pk\nLink: phantm://sync?key=$pk&name=${java.net.URLEncoder.encode(self.displayName, "UTF-8")}"
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("Phantm User Details", fullDetails))
                                            viewModel.showToast("Full details copied to clipboard", "success")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("COPY DETAILS", fontSize = 10.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        self.publicKey?.let { pk ->
                                            val fullDetails = "Alias: ${self.displayName}\nPhantm ID: $pk\nLink: phantm://sync?key=$pk&name=${java.net.URLEncoder.encode(self.displayName, "UTF-8")}"
                                            val shareIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, fullDetails)
                                                type = "text/plain"
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Phantm Details"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = CyberBlack, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SHARE DETAILS", fontSize = 10.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Recovery credentials / Private keys
                        // Recovery seed — shown directly, protected by device lock and app lock
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Warning box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSurface)
                                    .border(1.dp, CyberRed, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CyberRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Keep this phrase secret",
                                        color = CyberRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Anyone with these 12 words can take full control of your identity. Never share them.",
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        
                            Spacer(modifier = Modifier.height(16.dp))
                        
                            // Mnemonic word grid
                            val phrase = self.mnemonic
                            if (phrase != null) {
                                val words = phrase.split(" ")
                                val rows = words.chunked(3)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rows.forEach { rowWords ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowWords.forEachIndexed { colIndex, word ->
                                                val wordIndex = rows.indexOf(rowWords) * 3 + colIndex
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(CyberCard)
                                                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${wordIndex + 1}",
                                                        color = CyberCyan,
                                                        fontSize = 9.sp,
                                                        fontFamily = MonospaceFontFamily,
                                                        modifier = Modifier.width(16.dp)
                                                    )
                                                    Text(
                                                        text = word,
                                                        color = CyberTextPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            // Fill remaining slots in last row if words.size % 3 != 0
                                            repeat(3 - rowWords.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Node diagnostics & memory logs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "NODE DIAGNOSTICS",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = MonospaceFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Card 1: Connection Status
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusColor = if (isConnected) CyberGreen else CyberRed
                                    val statusText = if (isConnected) "ONLINE" else "OFFLINE"
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("TUNNEL BROADCASTER", color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                                        Text("Network Broker Status: $statusText", color = CyberTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Card 2: Memory Encryption
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("ENTROPY SHIELD", color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                                        Text("SQLite database ciphertext status: ACTIVE", color = CyberTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Card 3: Cryptographic Specs
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("CIPHER SPECIFICATION", color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily)
                                        Text("Curve25519 / BIP39 compliant", color = CyberTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                            }
                        }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: PhantmViewModel,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        PhantmScreenHeader(title = "Preferences")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

        val prefs = identity
        if (prefs != null) {
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
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

@Composable
fun LinkCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Strip hyphens for state logic
    val cleanCode = code.replace("-", "").take(8)

    androidx.compose.foundation.text.BasicTextField(
        value = cleanCode,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase().take(8)
            onCodeChange(filtered)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        decorationBox = {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 8) {
                    val char = cleanCode.getOrNull(i)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(36.dp, 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (char.isNotEmpty()) CyberSurface else CyberCard)
                            .border(1.dp, if (char.isNotEmpty()) CyberCyan else CyberBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (i == 3) {
                        Text(
                            text = "-",
                            color = CyberTextSecondary,
                            fontSize = 20.sp,
                            fontFamily = MonospaceFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    )
}
