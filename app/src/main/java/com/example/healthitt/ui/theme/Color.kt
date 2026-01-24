package com.example.healthitt.ui.theme

import androidx.compose.ui.graphics.Color

// RESTORED: Premium Aesthetic Palette (Emerald & Deep Slate)
val DeepSlate = Color(0xFF0F172A) // Dark background
val PureWhite = Color(0xFFFFFFFF)
val MutedSlate = Color(0xFF1E293B) // Card background

// Emerald Green (Symbol of health/vitality)
val EmeraldPrimary = Color(0xFF10B981) 
val EmeraldGlow = Color(0xFF34D399)
val EmeraldSoft = Color(0xFFD1FAE5)

// Accent Colors
val SkyAccent = Color(0xFF0EA5E9)
val RoseAccent = Color(0xFFF43F5E)
val AmberAccent = Color(0xFFF59E0B)

// Gradients
val ActiveGradient = listOf(EmeraldPrimary, EmeraldGlow)

// Legacy Compatibility Mapping (Ensuring all screens look consistent)
val NightDark = DeepSlate
val GlassCard = MutedSlate
val NeonCyan = SkyAccent
val NeonGreen = EmeraldPrimary
val SoftViolet = Color(0xFFBF5AF2)
val SunsetOrange = RoseAccent
val IndigoPrimary = EmeraldPrimary
val IndigoLight = EmeraldGlow
val SlateBackground = DeepSlate
val SlateSurface = MutedSlate
val OffWhite = Color(0xFFF1F5F9)
val CultBlack = DeepSlate
val CultGrey = MutedSlate
val CultSurface = MutedSlate
val CultRed = EmeraldPrimary
val MutedText = PureWhite.copy(alpha = 0.6f)
val SuccessGreen = EmeraldPrimary
val InfoBlue = SkyAccent
val WarningAmber = AmberAccent
val ErrorRed = RoseAccent
val WarningOrange = AmberAccent
