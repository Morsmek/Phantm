package com.morsmek.phantm.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morsmek.phantm.components.*
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.crypto.PhantmLinkCode
import com.morsmek.phantm.repository.BroadcastState
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel
import kotlinx.coroutines.delay
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
    
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> 
        hasCameraPermission = granted
        if (granted) {
            showScanner = true
        } else {
            viewModel.showToast("Camera permission required", "error")
        }
    }

    var actualResolveError: String? by remember { mutableStateOf(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    val identityForCode by viewModel.identitySettings.collectAsStateWithLifecycle()
    val myCode = remember(identityForCode?.publicKey) {
        identityForCode?.publicKey?.let { PhantmLinkCode.generate(it) } ?: "----"
    }
    var countdown by remember { mutableStateOf(PhantmLinkCode.secondsRemaining()) }
    val broadcastState by viewModel.broadcastState.collectAsStateWithLifecycle()

    var activeSegment by remember { mutableStateOf(0) }
    var tabWidth by remember { mutableStateOf(0.dp) }
    val density = androidx.compose.ui.platform.LocalDensity.current

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
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgPage)
            .drawDotGrid()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = Cyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "LINK NEW PEER",
                    color = TxtPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Segment Tabs Selector
            val tabs = listOf("Manual", "Link Code", "Scan QR")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(4.dp)
            ) {
                val indicatorOffset by animateDpAsState(
                    targetValue = when (activeSegment) {
                        0 -> 0.dp
                        1 -> tabWidth
                        else -> tabWidth * 2
                    },
                    animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
                    label = "tabIndicator"
                )
                
                // Sliding indicator backdrop
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(34.dp)
                        .offset(x = indicatorOffset)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cyan)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    tabWidth = with(density) { coords.size.width.toDp() }
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    activeSegment = i
                                    if (i == 2) {
                                        // Auto-trigger scan QR actions
                                        if (hasCameraPermission) {
                                            showScanner = true
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (activeSegment == i) Color.Black else TxtTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Main Content Area based on Segment
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pending Requests Banner — visible globally
                val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
                if (pendingRequests.isNotEmpty()) {
                    Text("PENDING REQUESTS", color = Cyan, fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                    pendingRequests.forEach { request ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgCard)
                                .border(1.dp, CyanBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(request.name, color = TxtPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("\"${request.introMessage}\"", color = TxtSecondary, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.acceptRequest(request.id, request.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ACCEPT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.rejectRequest(request.id) },
                                    border = BorderStroke(1.dp, SemanticRed),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticRed),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("REJECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                when (activeSegment) {
                    0 -> { // Manual entry
                        Text(
                            text = "ENTER PEER'S CODE",
                            color = Cyan,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        
                        LinkCodeInput(
                            code = linkCodeInput,
                            onCodeChange = { 
                                linkCodeInput = it 
                                actualResolveError = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        actualResolveError?.let {
                            Text(it, color = SemanticRed, fontSize = 12.sp, fontFamily = MonospaceFontFamily)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                actualResolveError = null
                                pendingIntroCode = linkCodeInput
                            },
                            enabled = linkCodeInput.length == 8 && !isResolving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Cyan,
                                contentColor = Color.Black,
                                disabledContainerColor = Cyan.copy(alpha = 0.2f),
                                disabledContentColor = TxtSecondary.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isResolving) {
                                CircularProgressIndicator(
                                    color = Color.Black,
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

                        // Info section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgCard)
                                .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text("HOW IT WORKS", color = Cyan, fontSize = 10.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "1. Tell your peer your 8-character code verbally, on paper, or in any chat.\n" +
                                "2. Switch to Link Code tab and tap BROADCAST MY CODE.\n" +
                                "3. They enter your code on their phone and tap CONNECT.",
                                color = TxtSecondary,
                                fontSize = 11.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    1 -> { // Link code broadcast
                        val infiniteTransition = rememberInfiniteTransition(label = "glow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgCard)
                                .border(1.dp, CyanBorder, RoundedCornerShape(16.dp))
                                .drawBehind {
                                    drawRoundRect(
                                        color = Cyan.copy(alpha = glowAlpha),
                                        cornerRadius = CornerRadius(16.dp.toPx()),
                                        blendMode = BlendMode.Screen
                                    )
                                }
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "My Code · Expires in ${countdown}s",
                                color = TxtTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = myCode,
                                color = Cyan,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 8.sp,
                                fontFamily = MonospaceFontFamily
                            )
                            Spacer(Modifier.height(12.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(BgInset)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(countdown / 600f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Cyan)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Broadcast trigger
                        when (val state = broadcastState) {
                            is BroadcastState.Idle -> {
                                PhantmActionButton(
                                    text = "BROADCAST MY CODE",
                                    icon = Icons.Default.TapAndPlay,
                                    onClick = { viewModel.startBroadcast() }
                                )
                            }
                            is BroadcastState.Listening -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyanDim),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Cyan,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("WAITING FOR PEER...", color = Cyan, fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                            is BroadcastState.PeerConnected -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyanDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${state.name} CONNECTED ✓", color = SemanticGreen, fontSize = 12.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    2 -> { // Scan QR & Share QR
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PhantmActionButton(
                                text = "SHARE MY QR CODE",
                                icon = Icons.Default.QrCode,
                                onClick = { 
                                    viewModel.startBroadcast()
                                    showQrDialog = true 
                                }
                            )

                            PhantmActionButton(
                                text = "SCAN QR CODE",
                                icon = Icons.Default.QrCodeScanner,
                                onClick = {
                                    if (hasCameraPermission) {
                                        showScanner = true
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                ghost = true
                            )
                        }
                    }
                }
            }
        }

        // Qr Code Dialog Popup
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

        // Qr Scanner screen overlay
        if (showScanner) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
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

        // Welcome Message input dialog
        if (pendingIntroCode != null) {
            val codeToJoin = pendingIntroCode!!
            var introText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { pendingIntroCode = null },
                containerColor = BgCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, CyanBorder, RoundedCornerShape(16.dp)),
                title = { Text("WELCOME MESSAGE", color = Cyan, fontFamily = MonospaceFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Send a short intro so they know who you are.", color = TxtSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = introText,
                            onValueChange = { if (it.length <= 50) introText = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Cyan,
                                unfocusedBorderColor = CyanBorder,
                                focusedContainerColor = BgDeep,
                                unfocusedContainerColor = BgDeep
                            ),
                            placeholder = { Text("e.g. It's Alice!", color = TxtTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${introText.length}/50", color = TxtTertiary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                    ) {
                        Text("SEND REQUEST", fontWeight = FontWeight.Bold, fontFamily = MonospaceFontFamily, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingIntroCode = null }) {
                        Text("CANCEL", color = TxtSecondary, fontFamily = MonospaceFontFamily)
                    }
                }
            )
        }
    }
}
