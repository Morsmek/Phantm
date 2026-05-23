package com.example.components

import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.DashboardTab

@Composable
fun PhantmLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Use the real logo mark — logo_icon.png, not the SVG approximation
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.logo_icon),
            contentDescription = "Phantm",
            modifier = Modifier.size(size)
        )
        if (showText) {
            // Use the real wordmark image instead of a Text composable
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.logo_text),
                contentDescription = null,
                modifier = Modifier
                    .height(size * 0.7f)
                    .wrapContentWidth()
            )
        }
    }
}

@Composable
fun PhantmNavigationBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(DashboardTab.Chats,    Icons.Outlined.ChatBubbleOutline, "CHATS"),
        Triple(DashboardTab.Contacts, Icons.Outlined.PeopleOutline,     "CONTACTS"),
        Triple(DashboardTab.Profile,  Icons.Outlined.Fingerprint, "IDENTITY"),
        Triple(DashboardTab.Settings, Icons.Outlined.Settings,          "SETTINGS")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xE6000000))  // 90% black
            .drawBehind {
                drawLine(
                    color = Color(0x0D00DBE9),  // 5% cyan hairline
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (tab, icon, label) ->
            val isActive = selectedTab == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(tab) }
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Bloom layer
                    if (isActive) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CyberCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)  // Larger = bloom spread
                        )
                    }
                    // Sharp layer
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isActive) CyberCyan else CyberTextSecondary.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = label,
                    color = if (isActive) CyberCyan else Color.Transparent,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.W300,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
fun PhantmScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0x66000000))  // 40% black — matches reference backdrop-blur
            .drawBehind {
                drawLine(
                    color = Color(0x0D00DBE9),  // 5% cyan hairline bottom border
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .statusBarsPadding()
    ) {
        // Logo centred
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.logo_icon),
                contentDescription = "Phantm",
                modifier = Modifier.size(28.dp)
            )
        }

        // Screen title — far left, bottom-aligned
        Text(
            text = title.uppercase(),
            color = CyberTextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W300,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 8.dp)
        )

        // Trailing content — far right, centred vertically
        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                trailingContent()
            }
        }
    }
}
