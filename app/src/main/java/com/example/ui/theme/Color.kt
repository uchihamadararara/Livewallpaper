package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Cinematic Palette
val DarkBackground = Color(0xFF080B10)
val DarkBackgroundAlt = Color(0xFF0C1017)
val DarkSurface = Color(0xFF101622)
val DarkSurfaceVariant = Color(0xFF151D2C)
val DarkSurfaceElevated = Color(0xFF1B2436)
val DarkBorder = Color(0xFF1E2A3C)
val DarkBorderSubtle = Color(0xFF162030)

// Premium Cinematic Blue Accent Family
val ElectricBluePrimary = Color(0xFF2563EB)      // Primary Interactive Accent
val ElectricBlueHover = Color(0xFF1D4ED8)        // Primary Darker / Pressed
val CinematicBlueAccent = Color(0xFF3B82F6)      // Vibrant Accent / Active State
val CyanHighlight = Color(0xFF60A5FA)            // High-visibility icons / tags
val IceBlueText = Color(0xFF93C5FD)              // Secondary accent text / badges
val SoftBlueTint = Color(0xFFBFDBFE)             // Light tint for icons
val BlueVipSurface = Color(0xFF0F1E33)           // VIP Card surface
val BlueVipBorder = Color(0xFF1E3E6B)            // VIP Card border

// Neutral Typography & Muted Grays
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Legacy alias for compatibility with centralized references
val ChampagnePrimary = CyanHighlight
val BronzeSecondary = ElectricBluePrimary
val DeepCharcoal = DarkBackground
val GraphiteSurface = DarkSurface
val GraphiteSurfaceVariant = DarkSurfaceVariant
val WarmBlack = DarkBackground
val SoftWhite = TextPrimary
val MutedTextDark = TextMuted

// Light Palette (Fallback)
val IvoryBackground = Color(0xFFF8FAFC)
val SoftGraySurface = Color(0xFFF1F5F9)
val SoftGraySurfaceVariant = Color(0xFFE2E8F0)
val CharcoalText = Color(0xFF0F172A)
val CharcoalPrimary = ElectricBluePrimary
val ChampagneAccentLight = CinematicBlueAccent
val BronzeSecondaryLight = ElectricBlueHover
val MutedTextLight = TextSecondary

