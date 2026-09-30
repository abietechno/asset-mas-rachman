package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CupertinoGlassTokens(
    val surface: Color,
    val surfaceSecondary: Color,
    val border: Color,
    val hairline: Color,
    val isDark: Boolean
)

data class CupertinoDynamicColors(
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val fill: Color,
    val fillSecondary: Color,
    val separator: Color,
    val cardBackground: Color,
    val surfaceBackground: Color,
    val isDark: Boolean
)

val LocalCupertinoGlass = staticCompositionLocalOf {
    CupertinoGlassTokens(
        surface = CupertinoGlassSurfaceLight,
        surfaceSecondary = CupertinoGlassSurfaceSecondaryLight,
        border = CupertinoGlassBorderLight,
        hairline = CupertinoGlassBorderHairlineLight,
        isDark = false
    )
}

val LocalCupertinoColors = staticCompositionLocalOf {
    CupertinoDynamicColors(
        label = CupertinoLabelLight,
        secondaryLabel = CupertinoSecondaryLabelLight,
        tertiaryLabel = CupertinoTertiaryLabelLight,
        fill = CupertinoFillLight,
        fillSecondary = CupertinoFillSecondaryLight,
        separator = CupertinoSeparatorLight,
        cardBackground = CupertinoCardBackground,
        surfaceBackground = CupertinoSystemBackground,
        isDark = false
    )
}

object CupertinoTheme {
    val colors: CupertinoDynamicColors
        @Composable
        get() = LocalCupertinoColors.current

    val glass: CupertinoGlassTokens
        @Composable
        get() = LocalCupertinoGlass.current
}

private val CupertinoLightColorScheme = lightColorScheme(
    primary = CupertinoPrimary,
    onPrimary = Color.White,
    primaryContainer = CupertinoPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = CupertinoPrimaryVariant,
    secondary = CupertinoPurple,
    onSecondary = Color.White,
    secondaryContainer = CupertinoPurpleLight,
    onSecondaryContainer = CupertinoPurple,
    tertiary = CupertinoTeal,
    onTertiary = Color.White,
    tertiaryContainer = CupertinoTealLight,
    onTertiaryContainer = CupertinoTeal,
    background = CupertinoSystemBackground,
    onBackground = CupertinoLabelLight,
    surface = CupertinoCardBackground,
    onSurface = CupertinoLabelLight,
    surfaceVariant = CupertinoCardSecondary,
    onSurfaceVariant = CupertinoSecondaryLabelLight,
    outline = CupertinoSeparatorLight,
    outlineVariant = CupertinoFillLight,
    error = CupertinoRed,
    onError = Color.White,
    errorContainer = CupertinoRedLight,
    onErrorContainer = CupertinoRed
)

private val CupertinoDarkColorScheme = darkColorScheme(
    primary = CupertinoPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = CupertinoPrimaryDark.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = CupertinoPurpleDark,
    onSecondary = Color.Black,
    secondaryContainer = CupertinoPurpleDark.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    tertiary = CupertinoTeal,
    onTertiary = Color.Black,
    tertiaryContainer = CupertinoTeal.copy(alpha = 0.2f),
    onTertiaryContainer = Color.White,
    background = CupertinoSystemBackgroundDark,
    onBackground = CupertinoLabelDark, // Pure bright white
    surface = CupertinoCardBackgroundDark,
    onSurface = CupertinoLabelDark, // Pure bright white
    surfaceVariant = CupertinoCardSecondaryDark,
    onSurfaceVariant = CupertinoSecondaryLabelDark, // Bright silver
    outline = CupertinoSeparatorDark,
    outlineVariant = CupertinoFillDark,
    error = CupertinoRedDark,
    onError = Color.Black,
    errorContainer = CupertinoRedDark.copy(alpha = 0.2f),
    onErrorContainer = CupertinoRedDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CupertinoDarkColorScheme else CupertinoLightColorScheme

    val glassTokens = if (darkTheme) {
        CupertinoGlassTokens(
            surface = CupertinoGlassSurfaceDark,
            surfaceSecondary = CupertinoGlassSurfaceSecondaryDark,
            border = CupertinoGlassBorderDark,
            hairline = CupertinoGlassBorderHairlineDark,
            isDark = true
        )
    } else {
        CupertinoGlassTokens(
            surface = CupertinoGlassSurfaceLight,
            surfaceSecondary = CupertinoGlassSurfaceSecondaryLight,
            border = CupertinoGlassBorderLight,
            hairline = CupertinoGlassBorderHairlineLight,
            isDark = false
        )
    }

    val dynamicColors = if (darkTheme) {
        CupertinoDynamicColors(
            label = CupertinoLabelDark,
            secondaryLabel = CupertinoSecondaryLabelDark,
            tertiaryLabel = CupertinoTertiaryLabelDark,
            fill = CupertinoFillDark,
            fillSecondary = CupertinoCardSecondaryDark,
            separator = CupertinoSeparatorDark,
            cardBackground = CupertinoCardBackgroundDark,
            surfaceBackground = CupertinoSystemBackgroundDark,
            isDark = true
        )
    } else {
        CupertinoDynamicColors(
            label = CupertinoLabelLight,
            secondaryLabel = CupertinoSecondaryLabelLight,
            tertiaryLabel = CupertinoTertiaryLabelLight,
            fill = CupertinoFillLight,
            fillSecondary = CupertinoFillSecondaryLight,
            separator = CupertinoSeparatorLight,
            cardBackground = CupertinoCardBackground,
            surfaceBackground = CupertinoSystemBackground,
            isDark = false
        )
    }

    CompositionLocalProvider(
        LocalCupertinoGlass provides glassTokens,
        LocalCupertinoColors provides dynamicColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
