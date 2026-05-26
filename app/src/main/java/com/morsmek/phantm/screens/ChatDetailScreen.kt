package com.morsmek.phantm.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morsmek.phantm.components.*
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.types.Message
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    viewModel: PhantmViewModel,
    contactId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.getMessagesForContact(contactId).collectAsStateWithLifecycle(initialValue = emptyList())
    var messageInput by remember { mutableStateOf("") }
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val contactName = remember(contacts, contactId) {
        contacts.find { it.id == contactId }?.name ?: "Secure Peer"
    }
    val isE2EE = remember(contacts, contactId) {
        contacts.find { it.id == contactId }?.hasPassphrase == true
    }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeBottomSheetMessage by remember { mutableStateOf<Message?>(null) }
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

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
            .background(BgPage)
            .drawDotGrid()
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDeep)
                .drawBehind { drawLine(Color(0x1400F0FF), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Cyan, modifier = Modifier.size(20.dp))
            }
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyanDim)
                    .border(2.dp, CyanBorderHi, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(contactName.take(2).uppercase(), color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            // Name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(contactName, color = TxtPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "status")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "p"
                    )
                    Box(
                        Modifier
                            .size(6.dp)
                            .graphicsLayer { scaleX = pulse; scaleY = pulse }
                            .background(SemanticGreen, CircleShape)
                    )
                    Text("online · encrypted", color = SemanticGreen, fontSize = 10.sp)
                }
            }
            // More button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .clickable { showMoreMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = TxtTertiary, modifier = Modifier.size(18.dp))
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier
                        .background(BgCard)
                        .border(1.dp, CyanBorder)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "COPY PEER KEY",
                                color = TxtPrimary,
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
                                color = SemanticRed,
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

        // Messages scrolling canvas
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

        // Bottom chat entry bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDeep)
                .drawBehind { drawLine(Color(0x0F00F0FF), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Input field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(12.dp))
                BasicTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    textStyle = TextStyle(color = TxtPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(Cyan),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (messageInput.isEmpty()) {
                                Text(
                                    text = if (isE2EE) "Type encrypted message..." else "Type message (E2EE off)...",
                                    color = TxtTertiary,
                                    fontSize = 13.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_textfield")
                )
            }
            // Send button
            val sendInteractionSource = remember { MutableInteractionSource() }
            val isSendPressed by sendInteractionSource.collectIsPressedAsState()
            val sendScale by animateFloatAsState(if (isSendPressed) 0.9f else 1f, label = "sendScale")
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = sendScale; scaleY = sendScale; rotationZ = if (isSendPressed) 15f else 0f }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cyan)
                    .clickable(interactionSource = sendInteractionSource, indication = null) {
                        if (messageInput.isNotBlank()) {
                            viewModel.sendMessage(contactId, messageInput)
                            messageInput = ""
                        }
                    }
                    .testTag("send_message_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
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
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Cyan)
                Spacer(modifier = Modifier.width(16.dp))
                Text("COPY CIPHERTEXT", color = TxtPrimary, fontSize = 13.sp, fontFamily = MonospaceFontFamily)
            }

            HorizontalDivider(color = CyanBorder, modifier = Modifier.padding(vertical = 4.dp))

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
                Icon(Icons.Default.Delete, contentDescription = null, tint = SemanticRed)
                Spacer(modifier = Modifier.width(16.dp))
                Text("PURGE MESSAGE MEMORY", color = SemanticRed, fontSize = 13.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}
