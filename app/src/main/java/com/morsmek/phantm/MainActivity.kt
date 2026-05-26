package com.morsmek.phantm

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.morsmek.phantm.components.*
import com.morsmek.phantm.screens.*
import com.morsmek.phantm.ui.theme.MyApplicationTheme
import com.morsmek.phantm.ui.theme.*
import com.morsmek.phantm.viewmodel.PhantmViewModel
import android.content.Intent
import android.nfc.NfcAdapter
import com.morsmek.phantm.crypto.PhantmNfc
import android.os.Build

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        setContent {
            MyApplicationTheme {
                PhantmAppContainer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PhantmNfc.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        PhantmNfc.disableForegroundDispatch(this)
    }

    override fun onStop() {
        super.onStop()
        val vm = androidx.lifecycle.ViewModelProvider(this)[PhantmViewModel::class.java]
        vm.lockAppIfEnabled()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val parsed = PhantmNfc.parseNfcIntent(intent)
        if (parsed != null) {
            val vm = androidx.lifecycle.ViewModelProvider(this)[PhantmViewModel::class.java]
            vm.onNfcContactReceived(parsed.first, parsed.second)
        }
    }
}

// Sealed screen hierarchy representing the full stack routes
sealed class Screen {
    object Welcome : Screen()
    object Passphrase : Screen()
    data class PassphraseConfirm(val mnemonic: String) : Screen()
    object Recover : Screen()
    object Dashboard : Screen()
    data class ChatDetail(val contactId: String) : Screen()
    object AddContact : Screen()
}

// Bottom tab tabs enum representing the Main bottom bar targets
enum class DashboardTab {
    Chats,
    Contacts,
    Profile,
    Settings
}

@Composable
fun PhantmAppContainer(
    viewModel: PhantmViewModel = viewModel()
) {
    val identityState by viewModel.identitySettings.collectAsStateWithLifecycle()
    val toastState by viewModel.toastState.collectAsStateWithLifecycle()
    val appIsLocked by viewModel.appIsLocked.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }
    var activeTab by remember { mutableStateOf(DashboardTab.Chats) }

    // Read isOnboarded on initial launch and route appropriately
    LaunchedEffect(identityState) {
        identityState?.let { identity ->
            if (identity.isOnboarded) {
                // Bypass onboarding if onboarded state is valid
                if (currentScreen is Screen.Welcome || currentScreen is Screen.Passphrase || currentScreen is Screen.Recover) {
                    currentScreen = Screen.Dashboard
                }
            } else {
                // Ensure onboarding is showed
                if (currentScreen is Screen.Dashboard || currentScreen is Screen.ChatDetail || currentScreen is Screen.AddContact) {
                    currentScreen = Screen.Welcome
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.nfcIncomingContact.collect { (key, name) ->
            viewModel.addContact(key, name)
            viewModel.showToast("NFC contact linked: $name", "success")
            currentScreen = Screen.ChatDetail(key)
        }
    }

    // Custom debounced back button handling
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current

    val handleBackPress: () -> Unit = {
        when (val screen = currentScreen) {
            is Screen.ChatDetail -> {
                currentScreen = Screen.Dashboard
            }
            Screen.AddContact -> {
                currentScreen = Screen.Dashboard
            }
            is Screen.PassphraseConfirm -> {
                currentScreen = Screen.Passphrase
            }
            Screen.Passphrase, Screen.Recover -> {
                currentScreen = Screen.Welcome
            }
            Screen.Dashboard -> {
                if (activeTab != DashboardTab.Chats) {
                    activeTab = DashboardTab.Chats
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000L) {
                        (context as? Activity)?.finish()
                    } else {
                        lastBackPressTime = now
                        viewModel.showToast("Press back again to exit Phantm", "info")
                    }
                }
            }
            else -> {
                (context as? Activity)?.finish()
            }
        }
    }

    BackHandler(enabled = true, onBack = handleBackPress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Main Screen routing layout switch
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(100))
            },
            label = "screen_routing"
        ) { screen ->
            when (screen) {
                Screen.Welcome -> {
                    WelcomeScreen(
                        onCreateIdentity = { currentScreen = Screen.Passphrase },
                        onRecoverIdentity = { currentScreen = Screen.Recover }
                    )
                }
                Screen.Passphrase -> {
                    PassphraseScreen(
                        onNext = { mn -> currentScreen = Screen.PassphraseConfirm(mn) }
                    )
                }
                is Screen.PassphraseConfirm -> {
                    PassphraseConfirmScreen(
                        mnemonic = screen.mnemonic,
                        onSuccess = { mn ->
                            viewModel.createIdentity(mn)
                            currentScreen = Screen.Dashboard
                        }
                    )
                }
                Screen.Recover -> {
                    RecoverScreen(
                        onRecoverSuccess = { mn ->
                            viewModel.recoverIdentity(mn)
                            currentScreen = Screen.Dashboard
                        }
                    )
                }
                Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        onChatSelected = { id -> currentScreen = Screen.ChatDetail(id) },
                        onAddContactSelected = { currentScreen = Screen.AddContact }
                    )
                }
                is Screen.ChatDetail -> {
                    ChatDetailScreen(
                        viewModel = viewModel,
                        contactId = screen.contactId,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }
                Screen.AddContact -> {
                    AddContactScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Dashboard },
                        onContactLinked = { id -> currentScreen = Screen.ChatDetail(id) }
                    )
                }
            }
        }

        // Global Toast System overlay
        toastState?.let { message ->
            PhantmToast(
                message = message.message,
                type = message.type,
                onDismiss = { viewModel.dismissToast() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Offset above the tab bar if on dashboard
            )
        }

        // App Lock overlay — shown when user backgrounds the app with App Lock enabled
        if (appIsLocked) {
            var lockPin by remember { mutableStateOf("") }
            var lockPinError by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Phantm locked",
                        color = CyberTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your PIN to continue",
                        color = CyberTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = lockPin,
                        onValueChange = {
                            lockPin = it.filter { ch -> ch.isDigit() }.take(4)
                            lockPinError = false
                            if (lockPin.length == 4) {
                                if (viewModel.verifyAppLockPin(lockPin)) {
                                    viewModel.unlockApp()
                                    lockPin = ""
                                } else {
                                    lockPinError = true
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                "4-digit PIN",
                                color = CyberTextSecondary.copy(alpha = 0.5f)
                            )
                        },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (lockPinError) CyberRed else CyberCyan,
                            unfocusedBorderColor = if (lockPinError) CyberRed else CyberBorder,
                            cursorColor = CyberCyan
                        ),
                        isError = lockPinError,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (lockPinError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Incorrect PIN",
                            color = CyberRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: PhantmViewModel,
    activeTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    onChatSelected: (String) -> Unit,
    onAddContactSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack),
        bottomBar = {
            PhantmNavigationBar(
                selectedTab = activeTab,
                onTabSelected = onTabChange
            )
        },
        containerColor = CyberBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                DashboardTab.Chats -> {
                    ChatsListScreen(
                        viewModel = viewModel,
                        onChatSelected = onChatSelected
                    )
                }
                DashboardTab.Contacts -> {
                    ContactsListScreen(
                        viewModel = viewModel,
                        onContactSelected = onChatSelected,
                        onAddContactSelected = onAddContactSelected
                    )
                }
                DashboardTab.Profile -> {
                    ProfileScreen(
                        viewModel = viewModel
                    )
                }
                DashboardTab.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}


