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
