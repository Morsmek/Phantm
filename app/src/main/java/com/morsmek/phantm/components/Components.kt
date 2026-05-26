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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsmek.phantm.crypto.PhantmCrypto
import com.morsmek.phantm.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

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
    dotColor: Color = CyberDotColor,   // Now 3% opacity — barely visible
    gridSize: Dp = 40.dp,              // Wider grid = less dense = more minimal
    dotRadius: Dp = 0.75.dp            // Smaller dots
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
            .clip(CircleShape)
            .background(CyberSurface)
            .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
            fontFamily = MonospaceFontFamily
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 2.dp,
                        bottomEnd = if (isSent) 2.dp else 16.dp
                    )
                )
                .background(if (isSent) CyberCyan else CyberSurface)
                .border(
                    1.dp,
                    if (isSent) Color.Transparent else Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 2.dp,
                        bottomEnd = if (isSent) 2.dp else 16.dp
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = content,
                color = if (isSent) CyberBlack else CyberTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = if (isSent) FontWeight.Medium else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    color = if (isSent) CyberBlack.copy(alpha = 0.6f) else CyberTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = MonospaceFontFamily
                )
                if (isSent) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (status) {
                        "sent" -> {
                            Text(
                                text = "✓",
                                color = CyberBlack.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                        "delivered", "read" -> {
                            val checkColor = if (status == "read") CyberBlack else CyberBlack.copy(alpha = 0.6f)
                            Text(
                                text = "✓✓",
                                color = checkColor,
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerBadge(
    modifier: Modifier = Modifier
) {
    // Visibility checker state linked to Composition Lifecycle.
    // If composable leaves scope, animation values are garbage-collected instantly.
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
            CyberCyan.copy(alpha = 0.1f),
            CyberCyan.copy(alpha = 0.5f),
            CyberCyan.copy(alpha = 0.1f)
        ),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset, 150f)
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush)
            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "END-TO-END ENCRYPTED",
            color = CyberCyan,
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
        // Simulating the 0.6s cyberpunk mount glitch
        repeat(5) {
            glitchX = (-4..4).random().toFloat()
            glitchY = (-2..2).random().toFloat()
            delay(100)
        }
        isSettled = true
    }

    Box(modifier = modifier) {
        if (!isSettled) {
            // Cyan replica offset shadow
            Text(
                text = text,
                color = CyberCyan.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = MonospaceFontFamily,
                modifier = Modifier.offset(x = glitchX.dp, y = glitchY.dp)
            )
            // Red replica offset shadow
            Text(
                text = text,
                color = CyberRed.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = MonospaceFontFamily,
                modifier = Modifier.offset(x = -glitchX.dp, y = -glitchY.dp)
            )
        }

        Text(
            text = text,
            color = CyberTextPrimary,
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
        // Pulse ring drawings on custom Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val baseRadius = 18.dp.toPx()

            // Draw Ring 1
            drawCircle(
                color = CyberCyan.copy(alpha = ring1Alpha),
                radius = baseRadius * ring1Scale,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw Ring 2
            drawCircle(
                color = CyberCyan.copy(alpha = ring2Alpha),
                radius = baseRadius * ring2Scale,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Concentric Core Icon Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CyberSurface)
                .border(2.dp, CyberCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = checked, label = "toggle")
    val offsetThumb by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "thumbOffset"
    ) { state ->
        if (state) 20.dp else 4.dp
    }
    val trackColor by transition.animateColor(
        transitionSpec = { tween(150) },
        label = "trackColor"
    ) { state ->
        if (state) CyberCyan else CyberSurface
    }

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .border(1.dp, if (checked) CyberCyan else CyberBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = !disabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetThumb)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) CyberBlack else CyberTextColorSecondary)
        )
    }
}

val CyberTextColorSecondary = Color(0xFF9CA3AF)

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
                .clickable(enabled = false) {} // block Click propagation
                .padding(24.dp)
        ) {
            // Drag indicator handle
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

