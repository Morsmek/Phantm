package com.morsmek.phantm.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsmek.phantm.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WordChip(
    number: Int,
    word: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%02d".format(number),
            color = CyberCyan,
            fontSize = 11.sp,
            fontFamily = MonospaceFontFamily,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = word,
            color = CyberTextPrimary,
            fontSize = 14.sp,
            fontFamily = MonospaceFontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun Modifier.drawDotGrid(
    dotColor: Color = CyanSubtle,
    gridSize: Dp = 40.dp,
    dotRadius: Dp = 0.75.dp
) = drawBehind {
    val sizePx = gridSize.toPx()
    val radiusPx = dotRadius.toPx()
    val width = size.width
    val height = size.height

    var x = 0f
    while (x < width) {
        var y = 0f
        while (y < height) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(x, y)
            )
            y += sizePx
        }
        x += sizePx
    }
}

@Composable
fun E2eeBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(0.5.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(CyberCyan, CircleShape)
        )
        Text(
            text = "E2EE",
            color = CyberCyan.copy(alpha = 0.6f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W300,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun Avatar(
    name: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initials = if (name.isNotBlank()) {
        val parts = name.trim().split("\\s+".toRegex())
        if (parts.size >= 2) {
            parts[0].take(1).uppercase() + parts[1].take(1).uppercase()
        } else {
            name.take(2).uppercase()
        }
    } else {
        "?"
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(CyanDim)
            .border(2.dp, CyanBorderHi, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
fun MessageBubble(
    content: String,
    timestamp: Long,
    isSent: Boolean,
    status: String,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(timestamp) { formatter.format(Date(timestamp)) }

    val ticks = if (isSent) {
        when (status) {
            "sent" -> "✓"
            "delivered", "read" -> "✓✓"
            else -> null
        }
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                },
            horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        if (isSent) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
                        else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
                    )
                    .background(if (isSent) Cyan else BgCard)
                    .then(
                        if (!isSent) Modifier.border(
                            1.dp,
                            Color(0x0FFFFFFF),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
                        ) else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(
                    text = content,
                    color = if (isSent) Color.Black else TxtPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (isSent) FontWeight.Medium else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(text = timeString, color = TxtTertiary, fontSize = 9.sp)
                if (ticks != null) {
                    Text(
                        text = ticks,
                        color = if (status == "read") Cyan else TxtTertiary,
                        fontSize = 9.sp,
                        fontWeight = if (status == "read") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerBadge(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Cyan.copy(alpha = 0.1f),
            Cyan.copy(alpha = 0.5f),
            Cyan.copy(alpha = 0.1f)
        ),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset, 150f)
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush)
            .border(1.dp, Cyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "END-TO-END ENCRYPTED",
            color = Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            fontFamily = MonospaceFontFamily,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier
) {
    var isSettled by remember { mutableStateOf(false) }
    var glitchX by remember { mutableStateOf(0f) }
    var glitchY by remember { mutableStateOf(0f) }

    LaunchedEffect(text) {
        isSettled = false
        repeat(5) {
            glitchX = (-4..4).random().toFloat()
            glitchY = (-2..2).random().toFloat()
            delay(100)
        }
        isSettled = true
    }

    Box(modifier = modifier) {
        if (!isSettled) {
            Text(
                text = text,
                color = Cyan.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = MonospaceFontFamily,
                modifier = Modifier.offset(x = glitchX.dp, y = glitchY.dp)
            )
            Text(
                text = text,
                color = SemanticRed.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = MonospaceFontFamily,
                modifier = Modifier.offset(x = -glitchX.dp, y = -glitchY.dp)
            )
        }

        Text(
            text = text,
            color = TxtPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = MonospaceFontFamily,
            fontSize = 20.sp
        )
    }
}

@Composable
fun DataPulse(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = 2000, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = 2000, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val baseRadius = 18.dp.toPx()

            drawCircle(
                color = Cyan.copy(alpha = ring1Alpha),
                radius = baseRadius * ring1Scale,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                color = Cyan.copy(alpha = ring2Alpha),
                radius = baseRadius * ring2Scale,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CyberSurface)
                .border(2.dp, Cyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun FigmaToggle(on: Boolean, onToggle: ((Boolean) -> Unit)?) {
    val trackColor by animateColorAsState(if (on) Cyan else BgInset, tween(200), label = "trackColor")
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                onToggle?.invoke(!on)
            }
            .padding(2.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val thumbScale by animateFloatAsState(if (on) 1f else 0.9f, label = "thumbScale")
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    FigmaToggle(on = checked, onToggle = if (disabled) null else onCheckedChange)
}

@Composable
fun PhantmInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    prefix: String? = null,
    monospace: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(leadingIcon, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
        if (prefix != null) {
            Text(prefix, color = Cyan, fontSize = 11.sp, fontFamily = MonospaceFontFamily)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = TxtPrimary,
                fontSize = 13.sp,
                fontFamily = if (monospace) MonospaceFontFamily else FontFamily.Default,
                letterSpacing = if (monospace) 4.sp else 0.sp
            ),
            cursorBrush = SolidColor(Cyan),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = TxtTertiary, fontSize = 13.sp)
                    inner()
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PhantmActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    ghost: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "buttonScale")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(if (ghost) CyanDim else Cyan)
            .then(if (ghost) Modifier.border(2.dp, CyanBorder, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (ghost) Cyan else Color.Black, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (ghost) Cyan else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    toggle: Boolean = false,
    toggleOn: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    hasArrow: Boolean = false,
    danger: Boolean = false,
    badge: Pair<String, Color>? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.99f else 1f, label = "rowScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, if (danger) SemanticRed.copy(alpha = 0.15f) else Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) SemanticRed else TxtTertiary, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            color = if (danger) SemanticRed else TxtPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        when {
            value != null -> Text(value, color = Cyan, fontSize = 11.sp)
            toggle -> FigmaToggle(on = toggleOn, onToggle = onToggle)
            hasArrow -> Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (danger) SemanticRed else TxtTertiary, modifier = Modifier.size(16.dp))
            badge != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val infiniteTransition = rememberInfiniteTransition(label = "badge")
                val pulse by infiniteTransition.animateFloat(1f, 1.4f,
                    infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "bp")
                Box(Modifier.size(8.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.background(badge.second, CircleShape))
                Text(badge.first, color = badge.second, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TxtTertiary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun PhantmToast(
    message: String,
    type: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        isVisible = true
        delay(2700)
        isVisible = false
        delay(300)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { 100 },
            animationSpec = tween(200, easing = EaseOutCubic)
        ) + fadeIn(tween(150)),
        exit = slideOutVertically(
            targetOffsetY = { 100 },
            animationSpec = tween(250, easing = EaseInCubic)
        ) + fadeOut(tween(150))
    ) {
        val themeColor = when (type) {
            "success" -> CyberGreen
            "error" -> CyberRed
            else -> CyberCyan
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, themeColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.0.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    color = CyberTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = MonospaceFontFamily,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PhantmBottomSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!isOpen) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CyberBorder)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title.uppercase(),
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonospaceFontFamily,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PhantmLogoIcon(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.morsmek.phantm.R.drawable.logo_icon),
        contentDescription = "Phantm Logo Icon",
        modifier = modifier
    )
}

@Composable
fun PhantmLogoWithText(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.morsmek.phantm.R.drawable.logo_text),
        contentDescription = "Phantm Logo with Text",
        modifier = modifier
    )
}
