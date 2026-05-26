package com.morsmek.phantm.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morsmek.phantm.components.*
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel

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

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhantmScreenHeader(title = "Identity")

            val self = identity
            if (self != null) {
                val displayName = self.displayName
                val publicKey = self.publicKey ?: ""

                // Profile Hero Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .drawBehind {
                            drawLine(Color(0x0FFFFFFF), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CyanDim)
                            .border(3.dp, Color(0x4D00F0FF), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2).uppercase(),
                            color = Cyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

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
                                    focusedBorderColor = Cyan,
                                    unfocusedBorderColor = CyanBorder,
                                    cursorColor = Cyan
                                ),
                                prefix = { Text("> ", color = Cyan, fontFamily = MonospaceFontFamily) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                viewModel.setDisplayName(editingNameValue)
                                isEditingName = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Cyan)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = displayName, color = TxtPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                editingNameValue = displayName
                                isEditingName = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit alias", tint = Cyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = "${publicKey.take(4)}...${publicKey.takeLast(4)} · ed25519",
                        color = TxtTertiary,
                        fontSize = 10.sp,
                        fontFamily = MonospaceFontFamily,
                        letterSpacing = 1.sp
                    )

                    // Identity Secured badge
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyanBg)
                            .border(1.dp, Color(0x4000F0FF), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(11.dp))
                        Text("Identity Secured", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy/Share User Details Block
                    SettingRow(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy Identity Details",
                        onClick = {
                            val fullDetails = "Alias: $displayName\nPhantm ID: $publicKey\nLink: phantm://sync?key=$publicKey&name=${java.net.URLEncoder.encode(displayName, "UTF-8")}"
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Phantm User Details", fullDetails))
                            viewModel.showToast("Identity details copied", "success")
                        }
                    )

                    SettingRow(
                        icon = Icons.Default.Share,
                        label = "Share Identity Details",
                        onClick = {
                            val fullDetails = "Alias: $displayName\nPhantm ID: $publicKey\nLink: phantm://sync?key=$publicKey&name=${java.net.URLEncoder.encode(displayName, "UTF-8")}"
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, fullDetails)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Phantm Details"))
                        }
                    )

                    SectionLabel(text = "NODE DIAGNOSTICS")

                    // diagnostics using custom rows
                    SettingRow(
                        icon = Icons.Default.TapAndPlay,
                        label = "Tunnel Broadcaster",
                        badge = if (isConnected) Pair("ONLINE", SemanticGreen) else Pair("OFFLINE", SemanticRed)
                    )

                    SettingRow(
                        icon = Icons.Default.Lock,
                        label = "Entropy Shield",
                        value = "ACTIVE"
                    )

                    SettingRow(
                        icon = Icons.Default.Info,
                        label = "Cipher Specification",
                        value = "Curve25519"
                    )
                }
            }
        }
    }
}
