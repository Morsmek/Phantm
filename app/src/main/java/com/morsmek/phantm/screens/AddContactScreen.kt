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
fun AddContactScreen(
    viewModel: PhantmViewModel,
    onBack: () -> Unit,
    onContactLinked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var linkCodeInput by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    var pendingIntroCode by remember { mutableStateOf<String?>(null) }
    
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
        viewModel.linkedContactId.collect { contactId ->
            onContactLinked(contactId)
        }
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
                is BroadcastState.Idle -> {
                    Button(
                        onClick = { viewModel.startBroadcast() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("BROADCAST MY CODE", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                is BroadcastState.Listening -> {
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
                is BroadcastState.PeerConnected -> {
                    Text("${state.name} CONNECTED ✓", color = CyberGreen, fontSize = 12.sp, fontFamily = MonospaceFontFamily)
                }
            }
        }

        val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
        if (pendingRequests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("PENDING REQUESTS", color = CyberCyan, fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            pendingRequests.forEach { request ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberCyan.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(request.name, color = CyberTextPrimary, fontWeight = FontWeight.Bold)
                    Text("\"${request.introMessage}\"", color = CyberTextSecondary, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.acceptRequest(request.id, request.name) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("ACCEPT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.rejectRequest(request.id) },
                            border = BorderStroke(1.dp, CyberRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("REJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                actualResolveError = null
                pendingIntroCode = linkCodeInput
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
                            pendingIntroCode = code
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

        pendingIntroCode?.let { codeToJoin ->
            var introText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { pendingIntroCode = null },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                title = { Text("WELCOME MESSAGE", color = CyberCyan, fontFamily = MonospaceFontFamily, fontSize = 14.sp) },
                text = {
                    Column {
                        Text("Send a short intro so they know who you are.", color = CyberTextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = introText,
                            onValueChange = { if (it.length <= 50) introText = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack
                            ),
                            placeholder = { Text("e.g. It's Alice!", color = CyberTextSecondary.copy(alpha=0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${introText.length}/50", color = CyberTextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val msg = introText.trim()
                            pendingIntroCode = null
                            isResolving = true
                            actualResolveError = null
                            viewModel.joinByCode(codeToJoin, msg) { success, message ->
                                isResolving = false
                                if (success) {
                                    viewModel.showToast("Request sent! Waiting for peer...", "success")
                                } else {
                                    actualResolveError = message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack)
                    ) {
                        Text("SEND REQUEST", fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingIntroCode = null }) {
                        Text("CANCEL", color = CyberTextSecondary, fontFamily = MonospaceFontFamily)
                    }
                }
            )
        }
    }
}
