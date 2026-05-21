package com.example.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WordChip
import com.example.components.PhantmLogoIcon
import com.example.components.PhantmLogoWithText
import com.example.crypto.Bip39
import com.example.ui.theme.*
import com.example.viewmodel.PhantmViewModel
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onCreateIdentity: () -> Unit,
    onRecoverIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Elegant signature logo drawn pure in Compose with neon glowing lines
            PhantmLogoIcon(
                modifier = Modifier
                    .size(120.dp)
                    .testTag("app_logo_icon")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PHANTM",
                color = CyberTextPrimary,
                fontSize = 36.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                modifier = Modifier.testTag("app_logo_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Private by design. Yours by default.",
                color = CyberTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = { onCreateIdentity() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = CyberBlack
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_identity_button")
            ) {
                Text(
                    text = "CREATE IDENTITY",
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onRecoverIdentity() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CyberCyan
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberCyan.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("recover_identity_button")
            ) {
                Text(
                    text = "RECOVER IDENTITY",
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PassphraseScreen(
    onNext: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val generatedMnemonic = remember { Bip39.generateMnemonic() }
    val wordsList = remember(generatedMnemonic) { generatedMnemonic.split(" ") }
    var checkedSaved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RECOVERY PHRASE",
            color = CyberCyan,
            fontSize = 20.sp,
            fontFamily = MonospaceFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Write down or securely backup this 12-word identity key. This is your seed phrase and cannot be recovered if lost.",
            color = CyberTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Grid presentation of mnemonic words list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 3) {
                            val index = row * 3 + col
                            if (index < wordsList.size) {
                                WordChip(
                                    number = index + 1,
                                    word = wordsList[index],
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Copy Clipboard Button
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Phantm Seed Phrase", generatedMnemonic)
                    clipboard.setPrimaryClip(clip)
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "COPY TO CLIPBOARD",
                color = CyberCyan,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Security warning card display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberRed.copy(alpha = 0.1f))
                .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CyberRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Never share this phrase. Anyone with it controls your identity.",
                color = CyberRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Confirm Gate Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { checkedSaved = !checkedSaved },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checkedSaved,
                onCheckedChange = { checkedSaved = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberCyan,
                    uncheckedColor = CyberBorder,
                    checkmarkColor = CyberBlack
                ),
                modifier = Modifier.testTag("phrase_saved_checkbox")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I have saved my recovery phrase in physical storage",
                color = CyberTextPrimary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onNext(generatedMnemonic) },
            enabled = checkedSaved,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCyan,
                contentColor = CyberBlack,
                disabledContainerColor = CyberCyan.copy(alpha = 0.2f),
                disabledContentColor = CyberTextSecondary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("phrase_next_button")
        ) {
            Text(
                text = "VALIDATE PHRASE",
                fontWeight = FontWeight.Bold,
                fontFamily = MonospaceFontFamily,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PassphraseConfirmScreen(
    mnemonic: String,
    onSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wordsList = remember(mnemonic) { mnemonic.split(" ") }

    // Select 3 random indices from 0 to 11 to quiz the user
    val quizIndices = remember {
        val indices = (0..11).shuffled()
        listOf(indices[0], indices[1], indices[2]).sorted()
    }

    var textInput1 by remember { mutableStateOf("") }
    var textInput2 by remember { mutableStateOf("") }
    var textInput3 by remember { mutableStateOf("") }

    var shakeTrigger1 by remember { mutableStateOf(false) }
    var shakeTrigger2 by remember { mutableStateOf(false) }
    var shakeTrigger3 by remember { mutableStateOf(false) }

    // Custom CSS-like horizontal offset animation for wrong input shake feedback
    val shakeOffset1 by animateFloatAsState(
        targetValue = if (shakeTrigger1) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { if (shakeTrigger1) shakeTrigger1 = false },
        label = "shake1"
    )

    val shakeOffset2 by animateFloatAsState(
        targetValue = if (shakeTrigger2) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { if (shakeTrigger2) shakeTrigger2 = false },
        label = "shake2"
    )

    val shakeOffset3 by animateFloatAsState(
        targetValue = if (shakeTrigger3) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { if (shakeTrigger3) shakeTrigger3 = false },
        label = "shake3"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "VERIFY SEED PHRASE",
            color = CyberCyan,
            fontSize = 20.sp,
            fontFamily = MonospaceFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Complete your security setup. Type the corresponding words of your private key seed below to verify backup compliance.",
            color = CyberTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Quiz Question 1
        Column(
            modifier = Modifier
                .offset(x = shakeOffset1.dp)
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${quizIndices[0] + 1}",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER WORD NUMBER #${quizIndices[0] + 1}",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = textInput1,
                onValueChange = { textInput1 = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    cursorColor = CyberCyan
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontFamily = MonospaceFontFamily),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_input_1")
            )
        }

        // Quiz Question 2
        Column(
            modifier = Modifier
                .offset(x = shakeOffset2.dp)
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${quizIndices[1] + 1}",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER WORD NUMBER #${quizIndices[1] + 1}",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = textInput2,
                onValueChange = { textInput2 = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    cursorColor = CyberCyan
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontFamily = MonospaceFontFamily),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_input_2")
            )
        }

        // Quiz Question 3
        Column(
            modifier = Modifier
                .offset(x = shakeOffset3.dp)
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${quizIndices[2] + 1}",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER WORD NUMBER #${quizIndices[2] + 1}",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = MonospaceFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = textInput3,
                onValueChange = { textInput3 = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    cursorColor = CyberCyan
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontFamily = MonospaceFontFamily),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_input_3")
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val ans1 = wordsList[quizIndices[0]]
                val ans2 = wordsList[quizIndices[1]]
                val ans3 = wordsList[quizIndices[2]]

                val norm1 = textInput1.trim().lowercase()
                val norm2 = textInput2.trim().lowercase()
                val norm3 = textInput3.trim().lowercase()

                var valid = true
                if (norm1 != ans1) {
                    shakeTrigger1 = true
                    valid = false
                }
                if (norm2 != ans2) {
                    shakeTrigger2 = true
                    valid = false
                }
                if (norm3 != ans3) {
                    shakeTrigger3 = true
                    valid = false
                }

                if (valid) {
                    onSuccess(mnemonic)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCyan,
                contentColor = CyberBlack
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("quiz_submit_button")
        ) {
            Text(
                text = "SUBMIT & INITIALIZE",
                fontWeight = FontWeight.Bold,
                fontFamily = MonospaceFontFamily,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RecoverScreen(
    onRecoverSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RECOVER PHANTM SEED",
            color = CyberCyan,
            fontSize = 20.sp,
            fontFamily = MonospaceFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Paste or type your 12-word space-separated recovery phrase below to restore your sovereign decentralized key and chats database.",
            color = CyberTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = rawText,
            onValueChange = {
                rawText = it
                if (inlineError != null) inlineError = null // clear on typing change
            },
            placeholder = {
                Text(
                    text = "abandon ability able act action actor actress actual adapt add addict address...",
                    color = CyberTextSecondary.copy(alpha = 0.5f)
                )
            },
            minLines = 4,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberBorder,
                cursorColor = CyberCyan,
                focusedContainerColor = CyberCard,
                unfocusedContainerColor = CyberCard
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontFamily = MonospaceFontFamily, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_phrase_textarea")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Inline error state display
        AnimatedVisibility(
            visible = inlineError != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            inlineError?.let { err ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberRed.copy(alpha = 0.1f))
                        .border(1.dp, CyberRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberRed)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = err,
                        color = CyberRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val phrase = rawText.trim()
                if (Bip39.isValid(phrase)) {
                    onRecoverSuccess(phrase)
                } else {
                    inlineError = "Phrase must be exactly 12 space-separated lowercase words."
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCyan,
                contentColor = CyberBlack
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("recovery_submit_button")
        ) {
            Text(
                text = "RESTORE ACCOUNT KEY",
                fontWeight = FontWeight.Bold,
                fontFamily = MonospaceFontFamily,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
