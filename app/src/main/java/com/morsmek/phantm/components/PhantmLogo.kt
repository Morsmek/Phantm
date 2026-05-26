package com.morsmek.phantm.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsmek.phantm.DashboardTab
import com.morsmek.phantm.R
import com.morsmek.phantm.ui.theme.*

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
        Image(
            painter = painterResource(id = R.drawable.logo_icon),
            contentDescription = "Phantm",
            modifier = Modifier.size(size)
        )
        if (showText) {
            Image(
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
fun PhantmScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDeep)
            .drawBehind {
                drawLine(
                    color = Color(0x1400F0FF),  // rgba(0,240,255,0.08)
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .statusBarsPadding()
            .padding(vertical = 14.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_text),
            contentDescription = "Phantm",
            modifier = Modifier
                .height(28.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )
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

@Composable
fun PhantmNavigationBar(
    selectedTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    data class NavItem(val tab: DashboardTab, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)
    val items = listOf(
        NavItem(DashboardTab.Chats,    Icons.Outlined.ChatBubbleOutline, "Chats"),
        NavItem(DashboardTab.Contacts, Icons.Outlined.PeopleOutline,     "Contacts"),
        NavItem(DashboardTab.Profile,  Icons.Outlined.Fingerprint,       "Identity"),
        NavItem(DashboardTab.Settings, Icons.Outlined.Settings,          "Settings")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(BgDeep)
            .drawBehind {
                drawLine(
                    color = Color(0x1400F0FF),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { item ->
            val active = selectedTab == item.tab
            val scale by animateFloatAsState(
                targetValue = if (active) 1.1f else 1f,
                animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
                label = "scale"
            )
            val yOffset by animateDpAsState(
                targetValue = if (active) (-2).dp else 0.dp,
                animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
                label = "yOffset"
            )
            val iconColor = if (active) Cyan else TxtTertiary.copy(alpha = 0.7f)
            val labelColor = if (active) Cyan else TxtTertiary.copy(alpha = 0.7f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .offset(y = yOffset)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabChange(item.tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = item.label,
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}
