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
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsListScreen(
    viewModel: PhantmViewModel,
    onContactSelected: (String) -> Unit,
    onAddContactSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.identitySettings.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var searchContactQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchContactQuery) {
        if (searchContactQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchContactQuery, ignoreCase = true) ||
                it.id.contains(searchContactQuery, ignoreCase = true)
            }
        }
    }

    var activeDialogContact by remember { mutableStateOf<Contact?>(null) }
    var renameValue by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawDotGrid()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhantmScreenHeader(title = "Contacts")

            // Search Filter
            OutlinedTextField(
                value = searchContactQuery,
                onValueChange = { searchContactQuery = it },
                placeholder = { Text("Filter contacts...", color = CyberTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard,
                    cursorColor = CyberCyan
                ),
                prefix = { Text("> ", color = CyberCyan, fontFamily = MonospaceFontFamily) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 12.dp)
                    .testTag("contacts_search_input")
            )

            // Contact items column
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = CyberBorder,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO PEERS CONNECTED",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredContacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onContactSelected(contact.id) },
                                    onLongClick = {
                                        activeDialogContact = contact
                                        renameValue = contact.name
                                    }
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(name = contact.name, size = 44.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.name,
                                        color = CyberTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (contact.hasPassphrase) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "E2EE Active",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = PhantmCrypto.truncateKey(contact.id, 10),
                                    color = CyberTextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = MonospaceFontFamily
                                )
                            }
                            // Call Action Chevron
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyberBorder)
                        }
                    }
                }
            }
        }

        // Add Contacts trigger FAB
        FloatingActionButton(
            onClick = { onAddContactSelected() },
            containerColor = CyberCyan,
            contentColor = CyberBlack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add contact link")
        }

        // Options sheet for selected contact
        PhantmBottomSheet(
            isOpen = activeDialogContact != null,
            onClose = { activeDialogContact = null },
            title = "PEER LINK MANIPULATIONS"
        ) {
            activeDialogContact?.let { selection ->
                Text(
                    text = "ID: ${selection.id}",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Update contact handle", color = CyberCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.renameContact(selection.id, renameValue)
                        activeDialogContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RENAME HANDLE")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.removeContact(selection.id)
                        activeDialogContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DELETE LINK")
                }
            }
        }
    }
}
