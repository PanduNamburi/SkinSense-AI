package com.skinsense.ai.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Medical Palette (Visily AI)
val MedicalPrimary = Color(0xFF1F4D7A)    // Deep Navy
val MedicalSecondary = Color(0xFF1F8CF9)  // Vibrant Blue 
val MedicalSurface = Color(0xFFFFFFFF)
val MedicalAccent = Color(0xFF2DD4BF)     // Serum Cyan (Kept for status/accents)
val MedicalBackground = Color(0xFFF8FAFC) // Soft Pearl
val OrangeAccent = Color(0xFFF97316)      // New Orange Accent
val OrangeLight = Color(0xFFFFF7ED)       // New Light Orange for buttons

// Functional Colors
val Info = Color(0xFF0EA5E9)
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

// Text Colors
val TextMain = Color(0xFF1E293B)
val TextMuted = Color(0xFF64748B)
val TextLight = Color(0xFF94A3B8)
val TextInverse = Color(0xFFFFFFFF)

// Risk Levels
val RiskLow = Success
val RiskLowBg = Color(0xFFDCFCE7)
val RiskMedium = Warning
val RiskMediumBg = Color(0xFFFEF3C7)
val RiskHigh = Error
val RiskHighBg = Color(0xFFFEE2E2)

// Legacy compatibility (mapping old names to new ones if needed by existing code)
val PrimaryDark = MedicalPrimary
val AccentBlue = MedicalSecondary
val BackgroundMain = MedicalBackground
val BackgroundCard = MedicalSurface
val TextPrimary = TextMain
val TextSecondary = TextMuted
val TextTertiary = TextLight
val TextOnPrimary = TextInverse
val GradientStart = Color(0xFFE0F2FE)
val Danger = Error
val BorderLight = Color(0xFFE2E8F0)
val AccentBlueLight = Color(0xFFBAE6FD)
