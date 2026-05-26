@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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









val SineInOutEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

enum class SphereState {
    IDLE,
    PAIRING,
    LINKED
}

data class ParticleData(
    val theta: Float,
    val phi: Float,
    val rFactor: Float,
    val speedFactor: Float,
    val colorType: Int
)

@Composable
fun PhantmSphere(
    state: SphereState,
    modifier: Modifier = Modifier,
    qrBitmap: Bitmap? = null
) {
    val particles = remember {
        val random = java.util.Random(42)
        List(180) {
            val theta = kotlin.math.acos(random.nextFloat() * 2f - 1f)
            val phi = random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val rFactor = 0.85f + random.nextFloat() * 0.3f
            val speedFactor = 0.6f + random.nextFloat() * 0.8f
            val colorType = random.nextInt(2) // 0: Cyan, 1: Magenta
            ParticleData(theta, phi, rFactor, speedFactor, colorType)
        }
    }

    var angleX by remember { mutableStateOf(0f) }
    var angleY by remember { mutableStateOf(0f) }

    val targetSpeed = when (state) {
        SphereState.IDLE -> 0.006f
        SphereState.PAIRING -> 0.025f
        SphereState.LINKED -> 0.002f
    }
    val currentSpeed by animateFloatAsState(
        targetValue = targetSpeed,
        animationSpec = tween(1000),
        label = "speed"
    )

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                angleX += currentSpeed * 0.7f
                angleY += currentSpeed
            }
        }
    }

    val flattenFactor by animateFloatAsState(
        targetValue = if (state == SphereState.LINKED) 0.08f else 1.0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "flatten"
    )

    val expansionFactor by animateFloatAsState(
        targetValue = if (state == SphereState.LINKED) 2.2f else 1.0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "expand"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = SineInOutEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseVal"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) * 0.35f

        val rBase = baseRadius * expansionFactor * (if (state == SphereState.IDLE) pulseFactor else 1.0f)

        if (state != SphereState.LINKED) {
            drawCircle(
                color = CyberCyan.copy(alpha = 0.05f * (if (state == SphereState.PAIRING) 2f else 1f)),
                radius = rBase * 1.15f,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = CyberCyan.copy(alpha = 0.1f * (if (state == SphereState.PAIRING) 2f else 1f)),
                radius = rBase,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        } else {
            drawCircle(
                color = CyberCyan.copy(alpha = 0.15f),
                radius = rBase * 1.1f,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx())
            )
        }

        val projected = particles.map { p ->
            val r = rBase * p.rFactor
            val sinTheta = kotlin.math.sin(p.theta)
            val cosTheta = kotlin.math.cos(p.theta)
            val sinPhi = kotlin.math.sin(p.phi)
            val cosPhi = kotlin.math.cos(p.phi)

            val x0 = r * sinTheta * cosPhi
            val y0 = r * sinTheta * sinPhi * flattenFactor
            val z0 = r * cosTheta

            val cosY = kotlin.math.cos(angleY * p.speedFactor)
            val sinY = kotlin.math.sin(angleY * p.speedFactor)
            val x1 = x0 * cosY - z0 * sinY
            val z1 = x0 * sinY + z0 * cosY

            val cosX = kotlin.math.cos(angleX * p.speedFactor)
            val sinX = kotlin.math.sin(angleX * p.speedFactor)
            val y2 = y0 * cosX - z1 * sinX
            val z2 = y0 * sinX + z1 * cosX

            val px = centerX + x1
            val py = centerY + y2
            val zDepth = z2

            Triple(px, py, zDepth) to p
        }.sortedBy { it.first.third }

        // Helper to draw a single particle
        val drawParticle: (Triple<Float, Float, Float>, ParticleData) -> Unit = { pos, p ->
            val (px, py, zDepth) = pos
            val depthRatio = (zDepth / rBase).coerceIn(-1f, 1f)
            val alphaVal = (0.35f + 0.65f * (depthRatio + 1f) / 2f).coerceIn(0.1f, 1.0f)
            val scaleVal = (0.7f + 0.6f * (depthRatio + 1f) / 2f)

            val basePtSize = if (p.colorType == 0) 3.dp.toPx() else 4.dp.toPx()
            val ptSize = basePtSize * scaleVal

            val color = if (p.colorType == 0) {
                CyberCyan.copy(alpha = alphaVal)
            } else {
                Color(0xFFEA00FF).copy(alpha = alphaVal)
            }

            drawCircle(
                color = color,
                radius = ptSize,
                center = Offset(px, py)
            )

            if (depthRatio > 0.4f && state == SphereState.PAIRING) {
                drawCircle(
                    color = color.copy(alpha = alphaVal * 0.3f),
                    radius = ptSize * 2.5f,
                    center = Offset(px, py)
                )
            }
        }

        // Stage 1: Draw particles behind the center (zDepth <= 0)
        projected.filter { it.first.third <= 0f }.forEach { (pos, p) ->
            drawParticle(pos, p)
        }

        // Stage 2: Draw the QR Code in the center (only if provided)
        qrBitmap?.let { bitmap ->
            val imageBitmap = bitmap.asImageBitmap()
            val sizePx = (rBase * 0.85f).coerceIn(120.dp.toPx(), 200.dp.toPx())
            val left = centerX - sizePx / 2f
            val top = centerY - sizePx / 2f
            drawImage(
                image = imageBitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(sizePx.toInt(), sizePx.toInt())
            )
        }

        // Stage 3: Draw particles in front of the center (zDepth > 0)
        projected.filter { it.first.third > 0f }.forEach { (pos, p) ->
            drawParticle(pos, p)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
class PhantmBarcodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { qrValue ->
                            onQrDetected(qrValue)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun CameraScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // Stable callback ref â€” prevents analyzer from holding a stale lambda
    val onQrScannedRef = rememberUpdatedState(onQrScanned)

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            // Bind camera once in factory, never in update
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, PhantmBarcodeAnalyzer { qrValue ->
                            previewView.post { onQrScannedRef.value(qrValue) }
                        })
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
        // No update{} block â€” camera is bound once in factory
    )

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}

fun generateQrCode(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if (bitMatrix.get(x, y)) AndroidColor.WHITE else 0xFF070B0E.toInt()
                bitmap.setPixel(x, y, color)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun parsePhantmJoinUri(uriStr: String): String? {
    // Returns the 8-char link code, or null
    try {
        val uri = Uri.parse(uriStr)
        if (uri.scheme == "phantm" && uri.host == "join") {
            val code = uri.getQueryParameter("code")
            if (code != null && code.length == 8) return code
        }
    } catch (e: Exception) { }
    return null
}

fun parsePhantmSyncUri(uriStr: String): Pair<String, String>? {
    try {
        val uri = Uri.parse(uriStr)
        if (uri.scheme == "phantm" && uri.host == "sync") {
            val key = uri.getQueryParameter("key")
            val name = uri.getQueryParameter("name")
            if (key != null && key.length == 64) {
                return Pair(key, name ?: "")
            }
        }
    } catch (e: Exception) {
        // manual decode
    }
    if (uriStr.startsWith("phantm://sync?")) {
        val query = uriStr.substringAfter("phantm://sync?")
        val params = query.split("&").associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }
        val key = params["key"]
        val name = params["name"]?.let {
            try {
                java.net.URLDecoder.decode(it, "UTF-8")
            } catch (ex: Exception) {
                it
            }
        }
        if (key != null && key.length == 64) {
            return Pair(key, name ?: "")
        }
    }
    return null
}

@Composable
fun ShareMySphereDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    myPublicKey: String,
    myName: String
) {
    if (!isOpen) return

    val qrContent = "phantm://sync?key=$myPublicKey&name=${java.net.URLEncoder.encode(myName, "UTF-8")}"
    val qrBitmap = remember(qrContent) {
        generateQrCode(qrContent, 512, 512)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PHANTM SYNC IDENTITY",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(CyberCard)
                        .border(2.dp, CyberCyan, CircleShape)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.08f,
                        targetValue = 0.22f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = SineInOutEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = glowAlpha))
                    )

                    if (qrBitmap != null) {
                        PhantmSphere(
                            state = SphereState.IDLE,
                            modifier = Modifier.fillMaxSize(),
                            qrBitmap = qrBitmap
                        )
                    } else {
                        Text("FAIL TO GENERATE QR", color = CyberRed, fontFamily = MonospaceFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = myName.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "KEY: ${PhantmCrypto.truncateKey(myPublicKey, 16)}",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("CLOSE TRANSMISSION", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StandaloneQrDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    myPublicKey: String,
    myName: String
) {
    if (!isOpen) return

    val myCode = remember(myPublicKey) { PhantmLinkCode.generate(myPublicKey) }
    val qrContent = "phantm://join?code=${myCode.replace("-", "")}"
    val qrBitmap = remember(qrContent) { generateQrCode(qrContent, 768, 768) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCAN TO ADD ME",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Plain QR â€” no sphere overlay, readable by any QR app
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B0E))
                            .border(2.dp, CyberCyan, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCard)
                            .border(1.dp, CyberRed, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR GENERATION FAILED", color = CyberRed, fontFamily = MonospaceFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = myName.uppercase(),
                    color = CyberTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PhantmCrypto.truncateKey(myPublicKey, 12),
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Share button â€” lets the user send the QR URI via any app they choose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Phantm Sync Link", qrContent))
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY LINK", fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, qrContent)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Phantm Link"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberCyan), border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHARE", fontSize = 11.sp, fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("CLOSE", color = CyberTextSecondary, fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}








@Composable
fun LinkCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Strip hyphens for state logic
    val cleanCode = code.replace("-", "").take(8)

    androidx.compose.foundation.text.BasicTextField(
        value = cleanCode,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase().take(8)
            onCodeChange(filtered)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        decorationBox = {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 8) {
                    val char = cleanCode.getOrNull(i)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(36.dp, 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (char.isNotEmpty()) CyberSurface else CyberCard)
                            .border(1.dp, if (char.isNotEmpty()) CyberCyan else CyberBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (i == 3) {
                        Text(
                            text = "-",
                            color = CyberTextSecondary,
                            fontSize = 20.sp,
                            fontFamily = MonospaceFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    )
}
