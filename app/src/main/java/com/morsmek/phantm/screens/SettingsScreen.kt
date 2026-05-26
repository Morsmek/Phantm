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
fun SettingsScreen(
    viewModel: PhantmViewModel,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    var showWipeConfirm by remember { mutableStateOf(false) }

    var showPinDialog by remember { mutableStateOf(false) }
    var disablePinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }

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
                            onCheckedChange = { 
                                if (prefs.appLockEnabled) {
                                    disablePinDialog = true
                                    pinValue = ""
                                } else {
                                    showPinDialog = true
                                    pinValue = ""
                                }
                            }
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

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                title = { Text("SETUP APP LOCK PIN", color = CyberCyan, fontFamily = MonospaceFontFamily, fontSize = 14.sp) },
                text = {
                    Column {
                        Text("Enter a 4+ digit PIN to lock this app.", color = CyberTextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = { if (it.length <= 8) pinValue = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinValue.length >= 4) {
                                viewModel.setAppLockPin(pinValue)
                                showPinDialog = false
                            } else {
                                viewModel.showToast("PIN must be at least 4 digits", "error")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack)
                    ) {
                        Text("ENABLE LOCK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("CANCEL", color = CyberTextSecondary)
                    }
                }
            )
        }

        if (disablePinDialog) {
            AlertDialog(
                onDismissRequest = { disablePinDialog = false },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                title = { Text("DISABLE APP LOCK", color = CyberCyan, fontFamily = MonospaceFontFamily, fontSize = 14.sp) },
                text = {
                    Column {
                        Text("Enter your current PIN to disable.", color = CyberTextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = { if (it.length <= 8) pinValue = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (viewModel.verifyAppLockPin(pinValue)) {
                                viewModel.disableAppLock()
                                disablePinDialog = false
                            } else {
                                viewModel.showToast("Incorrect PIN", "error")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = Color.White)
                    ) {
                        Text("DISABLE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { disablePinDialog = false }) {
                        Text("CANCEL", color = CyberTextSecondary)
                    }
                }
            )
        }
    }
}
