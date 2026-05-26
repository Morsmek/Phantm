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
