@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.morsmek.phantm.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morsmek.phantm.components.*
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.types.Conversation
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            .background(BgPage)
            .drawDotGrid()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhantmScreenHeader(title = "Chats", trailingContent = null)

            // E2EE Status Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyanBg)
                    .border(1.dp, Color(0x2600F0FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(14.dp))
                Text(
                    text = "End-to-End Encrypted",
                    color = Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = "dot"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                        .background(SemanticGreen, CircleShape)
                )
            }

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TxtTertiary, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = TxtPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(Cyan),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text("Search conversations...", color = TxtTertiary, fontSize = 13.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic conversation items stream
            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                Cyan.copy(alpha = 0.04f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = TxtTertiary.copy(alpha = 0.15f),
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        Text(
                            text = "CHATS",
                            color = TxtPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W100,
                            letterSpacing = 8.sp
                        )
                        Text(
                            text = "NO SECURE REPLIES FOUND",
                            color = Cyan.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.W300,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "LINK A CONTACT NODE TO INITIATE HANDSHAKE",
                            color = TxtTertiary.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.W300,
                            letterSpacing = 2.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // New Conversation Button
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "buttonScale")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp)
                    .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cyan)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        showNewChatDialog = true
                    }
                    .testTag("new_chat_fab"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Conversation",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // New Chat Dialog Popup
        if (showNewChatDialog) {
            var selectedContactId by remember { mutableStateOf("") }
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()

            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                containerColor = BgCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, CyanBorder, RoundedCornerShape(16.dp)),
                title = {
                    Text(
                        text = "START SECURE HANDSHAKES",
                        color = Cyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Select a linked contact node to open an encrypted communication tunnel.",
                            color = TxtSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (contacts.isEmpty()) {
                            Text(
                                text = "No contacts linked. Go to Contacts and link a peer's public key first.",
                                color = SemanticRed,
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
                                            .background(if (selectedContactId == contact.id) CyanDim else Color.Transparent)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Avatar(name = contact.name, size = 32.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = contact.name,
                                                color = TxtPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = PhantmCrypto.truncateKey(contact.id),
                                                color = TxtSecondary,
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
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                    ) {
                        Text("OPEN CHANNEL")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("CANCEL", color = TxtSecondary)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, spring(stiffness = 400f), label = "itemScale")

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(conversation.lastMessageAt) {
        formatter.format(Date(conversation.lastMessageAt))
    }

    val isActive = conversation.unreadCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) CyanDim else BgCard)
            .border(
                1.dp,
                if (isActive) Color(0x3300F0FF) else Color(0x0DFFFFFF),
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = { showActionSheet = true }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyanDim)
                    .border(2.dp, CyanBorderHi, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.contactName.take(2).uppercase(),
                    color = Cyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Text content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = conversation.contactName,
                        color = TxtPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.isEncrypted) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(11.dp))
                    }
                }
                Text(
                    text = conversation.lastMessagePreview,
                    color = TxtSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time + badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = timeStr, color = TxtTertiary, fontSize = 10.sp)
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(Cyan)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "${conversation.unreadCount}", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

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
                Text("DELETE CONTACT", color = SemanticRed, letterSpacing = 2.sp, fontSize = 11.sp)
            }
        }
    }
}
