package com.example.healthitt.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * FITNESS APP THEME: "MIDNIGHT LIME"
 * Optimized for OLED battery saving and high-energy feel.
 */

// Backgrounds (True Black for battery saving)
val MidnightBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF121212)
val CardGrey = Color(0xFF1E1E1E)

// Energetic Accents
val ElectricLime = Color(0xFFCCFF00) // The primary "Action" color
val NeonCyan = Color(0xFF00F2FF)
val BlazeOrange = Color(0xFFFF5F1F)
val HotPink = Color(0xFFFF007F)

// Text
val PureWhite = Color(0xFFFFFFFF)
val MutedText = Color(0xFF888888)
val OffWhite = Color(0xFFF1F5F9) // Added missing OffWhite for Light Mode

// Legacy Mappings
val RoyalDeep = MidnightBlack
val IndigoNight = CardGrey
val PrimaryRoyal = ElectricLime
val BrandRose = HotPink
val AccentGold = BlazeOrange
val EmeraldPrimary = ElectricLime
val EmeraldGlow = ElectricLime.copy(alpha = 0.8f)
val SkyAccent = NeonCyan
val RoseAccent = BlazeOrange
val AmberAccent = Color(0xFFFFD700)
val DeepSlate = MidnightBlack
val MutedSlate = CardGrey
val ActiveGradient = listOf(ElectricLime, NeonCyan)
val NightDark = MidnightBlack
val GlassCard = CardGrey
val NeonGreen = ElectricLime
val SunsetOrange = BlazeOrange
val SuccessGreen = ElectricLime
val ErrorRed = BlazeOrange
val SlateBackground = MidnightBlack
val SlateSurface = DarkSurface
