package com.morsmek.phantm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MonospaceFontFamily = FontFamily.Monospace

// Figma text sizes (px → sp 1:1 since base is 16px):
// text-[9px]  → 9.sp
// text-[10px] → 10.sp
// text-[11px] → 11.sp
// text-[12px] → 12.sp
// text-[13px] → 13.sp
// text-[14px] → 14.sp
// text-[16px] → 16.sp
// text-[28px] → 28.sp  (link code display)

val Typography = Typography(
    bodyLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    bodySmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    labelLarge  = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 10.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 9.sp,  letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 9.sp),
    titleLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    titleSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 13.sp),
)
