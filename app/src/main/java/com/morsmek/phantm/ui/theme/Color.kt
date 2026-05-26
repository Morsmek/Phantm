package com.morsmek.phantm.ui.theme

import androidx.compose.ui.graphics.Color

// === Figma token mapping ===
// --bg  = #070A0D  → deepest background (screen bg, nav bg, header bg)
// --bg2 = #0D1117  → page container
// --bg3 = #111820  → cards, inputs, conversation items
// --bg4 = #16202A  → inner chips, toggle track off, deep insets
// --c   = #00F0FF  → primary cyan accent
// --c-dim   = rgba(0,240,255,0.12) → tinted surfaces
// --c-border = rgba(0,240,255,0.18) → cyan hairline borders
// --txt  = #FFFFFF → primary text
// --txt2 = #8899AA → secondary text (previews, subtitles)
// --txt3 = #445566 → tertiary text (timestamps, labels, placeholders)
// --red  = #FF4455 → destructive
// --green = #00E87A → online / success

val BgDeep        = Color(0xFF070A0D)   // --bg
val BgPage        = Color(0xFF0D1117)   // --bg2
val BgCard        = Color(0xFF111820)   // --bg3
val BgInset       = Color(0xFF16202A)   // --bg4

val Cyan          = Color(0xFF00F0FF)   // --c
val CyanDim       = Color(0x1F00F0FF)   // --c-dim (12%)
val CyanBorder    = Color(0x2E00F0FF)   // --c-border (18%)
val CyanBorderHi  = Color(0x4D00F0FF)   // 30% — active avatar border
val CyanSubtle    = Color(0x0D00F0FF)   // 5%  — dot grid, dividers
val CyanBg        = Color(0x1400F0FF)   // 8%  — E2EE strip, code card bg

val TxtPrimary    = Color(0xFFFFFFFF)   // --txt
val TxtSecondary  = Color(0xFF8899AA)   // --txt2
val TxtTertiary   = Color(0xFF445566)   // --txt3

val SemanticRed   = Color(0xFFFF4455)   // --red
val SemanticGreen = Color(0xFF00E87A)   // --green

// Legacy aliases — keep these so existing code compiles unchanged
val CyberBlack       = BgDeep
val CyberSurface     = BgPage
val CyberCard        = BgCard
val CyberSurfaceHigh = BgInset
val CyberCyan        = Cyan
val CyberCyanDim     = CyanDim
val CyberBorder      = CyanBorder
val CyberBorderMid   = CyanBorderHi
val CyberDotColor    = CyanSubtle
val CyberTextPrimary   = TxtPrimary
val CyberTextSecondary = TxtSecondary
val CyberTextTertiary  = TxtTertiary
val CyberGreen       = SemanticGreen
val CyberRed         = SemanticRed
val CyberCyanDark     = Color(0xFF004F54)
val CyberAmber        = Color(0xFFF59E0B)
