package com.morsmek.phantm.screens

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
import com.morsmek.phantm.components.*
import com.morsmek.phantm.crypto.Bip39
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.types.Contact
import com.morsmek.phantm.types.Conversation
import com.morsmek.phantm.types.Message
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.morsmek.phantm.repository.BroadcastState
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
import com.morsmek.phantm.crypto.PhantmLinkCode
import androidx.compose.material3.CircularProgressIndicator
import com.morsmek.phantm.crypto.PhantmNfc
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
