package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Palette
val CupertinoSystemBackground = Color(0xFFF7F8FC) // Crisp, bright modern light background
val CupertinoCardBackground = Color(0xFFFFFFFF)
val CupertinoCardSecondary = Color(0xFFF9FAFD)
val CupertinoPrimary = Color(0xFF007AFF)
val CupertinoPrimaryVariant = Color(0xFF0051A8)
val CupertinoGreen = Color(0xFF34C759)
val CupertinoGreenLight = Color(0xFFE8F9ED)
val CupertinoOrange = Color(0xFFFF9500)
val CupertinoOrangeLight = Color(0xFFFFF4E5)
val CupertinoRed = Color(0xFFFF3B30)
val CupertinoRedLight = Color(0xFFFFEBEA)
val CupertinoPurple = Color(0xFF5856D6)
val CupertinoPurpleLight = Color(0xFFEFEFFC)
val CupertinoTeal = Color(0xFF30B0C7)
val CupertinoTealLight = Color(0xFFE5F7FA)
val CupertinoIndigo = Color(0xFF5856D6)

// Static Light Labels & Fills (Crisp contrast against light surfaces)
val CupertinoLabelLight = Color(0xFF1C1C1E)
val CupertinoSecondaryLabelLight = Color(0xFF6C6C70)
val CupertinoTertiaryLabelLight = Color(0xFF8E8E93)
val CupertinoSeparatorLight = Color(0xFFE5E5EA)
val CupertinoFillLight = Color(0xFFEBEBF0)
val CupertinoFillSecondaryLight = Color(0xFFF2F2F7)

// Dark Palette
val CupertinoSystemBackgroundDark = Color(0xFF141416) // Deep modern iOS dark background
val CupertinoCardBackgroundDark = Color(0xFF1E1E22)
val CupertinoCardSecondaryDark = Color(0xFF28282C)
val CupertinoPrimaryDark = Color(0xFF0A84FF)
val CupertinoGreenDark = Color(0xFF30D158)
val CupertinoOrangeDark = Color(0xFFFF9F0A)
val CupertinoRedDark = Color(0xFFFF453A)
val CupertinoPurpleDark = Color(0xFF5E5CE6)

// Static Dark Labels & Fills (Bright & High Contrast against dark surfaces)
val CupertinoLabelDark = Color(0xFFFFFFFF) // Pure bright white for primary text
val CupertinoSecondaryLabelDark = Color(0xFFD1D1D6) // Bright silver for secondary text
val CupertinoTertiaryLabelDark = Color(0xFFA0A0A5) // Soft light gray for tertiary text
val CupertinoSeparatorDark = Color(0xFF38383A)
val CupertinoFillDark = Color(0xFF2C2C2E)

// Glassmorphism iOS Tokens
val CupertinoGlassSurfaceLight = Color(0xF4FFFFFF) // 96% opacity luminous white glass
val CupertinoGlassSurfaceSecondaryLight = Color(0xEBFFFFFF)
val CupertinoGlassBorderLight = Color(0x35007AFF) // Subtle sapphire tint crystal border
val CupertinoGlassBorderHairlineLight = Color(0x25D1D5DB)

val CupertinoGlassSurfaceDark = Color(0xD81E1E22) // 85% opacity dark glass
val CupertinoGlassSurfaceSecondaryDark = Color(0xB82C2C2E)
val CupertinoGlassBorderDark = Color(0x35FFFFFF)
val CupertinoGlassBorderHairlineDark = Color(0x20545458)

// Ambient Glass Mesh Glow Accents
val AmbientGlowBlue = Color(0x18007AFF)
val AmbientGlowPurple = Color(0x145856D6)
val AmbientGlowTeal = Color(0x1430B0C7)
val AmbientGlowOrange = Color(0x14FF9500)

// Dynamic Composable Getters that automatically resolve based on the active theme
val CupertinoLabel: Color
    @Composable
    get() = CupertinoTheme.colors.label

val CupertinoSecondaryLabel: Color
    @Composable
    get() = CupertinoTheme.colors.secondaryLabel

val CupertinoTertiaryLabel: Color
    @Composable
    get() = CupertinoTheme.colors.tertiaryLabel

val CupertinoFill: Color
    @Composable
    get() = CupertinoTheme.colors.fill

val CupertinoFillSecondary: Color
    @Composable
    get() = CupertinoTheme.colors.fillSecondary

val CupertinoSeparator: Color
    @Composable
    get() = CupertinoTheme.colors.separator
