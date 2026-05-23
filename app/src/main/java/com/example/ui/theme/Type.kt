package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Monospace for keys, IDs, codes, timestamps
val MonospaceFontFamily = FontFamily.Monospace

// Phantom type scale — ultra-thin with extreme tracking
val Typography = Typography(
    // Screen titles: "CHATS", "IDENTITY", "SETTINGS"
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W100,       // Ultra-thin
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 8.sp               // 0.35em equivalent — extreme tracking
    ),
    // Section headers and card titles
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W200,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 4.sp
    ),
    // Contact names, message senders
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W300,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.sp
    ),
    // Body text, message content
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W200,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W200,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    // Nav labels, badges, timestamps
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.W300,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        letterSpacing = 2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.W300,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        letterSpacing = 1.5.sp
    )
)
