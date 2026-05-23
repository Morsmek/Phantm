package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
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
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(CyberBlack)
            .drawBehind {
                drawRect(
                    color = CyberCyan.copy(alpha = 0.08f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 2.dp.toPx())
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = CyberBlack,
        contentColor = CyberCyan
    ) {
        DashboardTab.values().forEach { tab ->
            val icon = when (tab) {
                DashboardTab.Chats -> Icons.Default.ChatBubble
                DashboardTab.Contacts -> Icons.Default.People
                DashboardTab.Profile -> Icons.Default.Lock
                DashboardTab.Settings -> Icons.Default.Settings
            }
            val label = when (tab) {
                DashboardTab.Chats -> "Chats"
                DashboardTab.Contacts -> "Contacts"
                DashboardTab.Profile -> "Identity"
                DashboardTab.Settings -> "Settings"
            }
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        label.uppercase(),
                        fontFamily = MonospaceFontFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    unselectedIconColor = CyberTextSecondary,
                    unselectedTextColor = CyberTextSecondary,
                    indicatorColor = CyberCyan.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun PhantmScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhantmLogo(showText = true, size = 24.dp)
            if (trailingContent != null) {
                trailingContent()
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            title.uppercase(),
            color = CyberTextPrimary,
            fontFamily = MonospaceFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}
