package com.morsmek.phantm.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morsmek.phantm.R
import com.morsmek.phantm.components.*
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel

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
            .background(BgPage)
    ) {
        PhantmScreenHeader(title = "Preferences")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val prefs = identity
            if (prefs != null) {
                SectionLabel(text = "COMMUNICATION PROTOCOL")

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingRow(
                        icon = Icons.Default.Notifications,
                        label = "Channel Notifications",
                        toggle = true,
                        toggleOn = prefs.notificationsEnabled,
                        onToggle = { viewModel.toggleNotifications() }
                    )

                    SettingRow(
                        icon = Icons.Default.Visibility,
                        label = "Notification Previews",
                        toggle = true,
                        toggleOn = prefs.showNotificationPreview && prefs.notificationsEnabled,
                        onToggle = { viewModel.toggleShowNotificationPreview() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(text = "SECURITY INTEGRITY")

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingRow(
                        icon = Icons.Default.Lock,
                        label = "Node App Lock",
                        toggle = true,
                        toggleOn = prefs.appLockEnabled,
                        onToggle = { 
                            if (prefs.appLockEnabled) {
                                disablePinDialog = true
                                pinValue = ""
                            } else {
                                showPinDialog = true
                                pinValue = ""
                            }
                        }
                    )

                    // Auto-delete timer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text("Auto-Delete Timer", color = TxtPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Purge old decrypted local history packets", color = TxtSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

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
                                        .background(if (prefs.autoDeleteDays == days) Cyan else BgInset)
                                        .border(1.dp, if (prefs.autoDeleteDays == days) Cyan else CyanBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setAutoDeleteDays(days) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (prefs.autoDeleteDays == days) Color.Black else TxtSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = MonospaceFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel(text = "DANGER TUNNEL ZONE")

                SettingRow(
                    icon = Icons.Default.DeleteForever,
                    label = "WIPE SECURE IDENTITY",
                    danger = true,
                    onClick = { showWipeConfirm = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PHANTM SECURE NODE v1.0.0-CYBER",
                    color = TxtTertiary,
                    fontSize = 10.sp,
                    fontFamily = MonospaceFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Stagic footer
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Developed by", color = TxtTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.stagic_logo),
                        contentDescription = "Stagic",
                        modifier = Modifier.height(22.dp).alpha(0.7f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // Wipe Warning Alert
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, CyanBorder, RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = "CRITICAL DATA PURGE WARNING",
                    color = SemanticRed,
                    fontSize = 14.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to delete your Phantm node keys and erase any linked partners or messages history forever? This is destructive and irreversible.",
                    color = TxtSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetIdentity()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticRed, contentColor = Color.White)
                ) {
                    Text("PURGE DESTRUCTIVELY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("CANCEL", color = TxtSecondary)
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, CyanBorder, RoundedCornerShape(16.dp)),
            title = { Text("SETUP APP LOCK PIN", color = Cyan, fontFamily = MonospaceFontFamily, fontSize = 14.sp) },
            text = {
                Column {
                    Text("Enter a 4+ digit PIN to lock this app.", color = TxtSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 8) pinValue = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = CyanBorder
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
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                ) {
                    Text("ENABLE LOCK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("CANCEL", color = TxtSecondary)
                }
            }
        )
    }

    if (disablePinDialog) {
        AlertDialog(
            onDismissRequest = { disablePinDialog = false },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, CyanBorder, RoundedCornerShape(16.dp)),
            title = { Text("DISABLE APP LOCK", color = Cyan, fontFamily = MonospaceFontFamily, fontSize = 14.sp) },
            text = {
                Column {
                    Text("Enter your current PIN to disable.", color = TxtSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 8) pinValue = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = CyanBorder
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
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticRed, contentColor = Color.White)
                ) {
                    Text("DISABLE")
                }
            },
            dismissButton = {
                TextButton(onClick = { disablePinDialog = false }) {
                    Text("CANCEL", color = TxtSecondary)
                }
            }
        )
    }
}
