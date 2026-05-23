package com.example

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
import com.example.components.*
import com.example.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import com.example.viewmodel.PhantmViewModel
import android.content.Intent
import android.nfc.NfcAdapter
import com.example.crypto.PhantmNfc
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

        if (appIsLocked) {
            AppLockOverlay(viewModel = viewModel)
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

// Private helper data class to hold bottom tab structures with no complex nested destructuring
private data class BottomTabItem(
    val tab: DashboardTab,
    val name: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

// Standard bottom navigation custom elements (64px height + bottom layout inset support)
@Composable
fun CustomNavigationBar(
    activeTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .windowInsetsPadding(WindowInsets.navigationBars) // Essential adaptive notch padding!
            .height(64.dp)
            .drawBehind {
                // Top border stroke
                drawLine(
                    color = CyberBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabItems = listOf(
            BottomTabItem(DashboardTab.Chats, "Chats", Icons.Default.ChatBubble, Icons.Outlined.ChatBubbleOutline),
            BottomTabItem(DashboardTab.Contacts, "Contacts", Icons.Default.People, Icons.Outlined.PeopleOutline),
            BottomTabItem(DashboardTab.Profile, "Identity", Icons.Default.Lock, Icons.Default.LockOpen),
            BottomTabItem(DashboardTab.Settings, "Settings", Icons.Default.Settings, Icons.Outlined.Settings)
        )

        tabItems.forEach { item ->
            val isSelected = activeTab == item.tab

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabChange(item.tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                    contentDescription = item.name,
                    tint = if (isSelected) CyberCyan else CyberTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    color = if (isSelected) CyberCyan else CyberTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun AppLockOverlay(
    viewModel: PhantmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorState by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        (context as? Activity)?.finish()
    }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            val success = viewModel.verifyBioPin(pin)
            if (success) {
                viewModel.unlockApp()
            } else {
                errorState = true
                viewModel.showToast("ACCESS DENIED: INVALID KEY CODE", "error")
                kotlinx.coroutines.delay(1000)
                pin = ""
                errorState = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .clickable(enabled = false) {}, // Intercept touch events
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = if (errorState) CyberRed else CyberCyan,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PHANTM PROTOCOL SECURED",
                color = if (errorState) CyberRed else CyberCyan,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ENTER DECRYPTION PIN",
                color = CyberTextSecondary,
                fontFamily = MonospaceFontFamily,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // PIN dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val active = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (active) (if (errorState) CyberRed else CyberCyan) else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (errorState) CyberRed else CyberCyan,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "CLEAR")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(72.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberSurface)
                                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (key == "CLEAR") {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            } else if (pin.length < 4) {
                                                pin += key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = if (key == "CLEAR") CyberRed else CyberTextPrimary,
                                        fontFamily = MonospaceFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (key == "CLEAR") 12.sp else 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
