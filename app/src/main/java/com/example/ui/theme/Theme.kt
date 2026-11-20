package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode

@Immutable
data class CooldownSemanticPalette(
    val fast: Color,
    val fastContainer: Color,
    val moderate: Color,
    val moderateContainer: Color,
    val long: Color,
    val longContainer: Color,
    val max: Color,
    val maxContainer: Color,
    val hairline: Color,
    val mutedText: Color
)

private val DarkCooldownPalette = CooldownSemanticPalette(
    fast = SignalEmerald,
    fastContainer = SignalEmeraldContainer,
    moderate = SignalCyan,
    moderateContainer = SignalCyanContainer,
    long = SignalAmber,
    longContainer = SignalAmberContainer,
    max = SignalCoral,
    maxContainer = SignalCoralContainer,
    hairline = ObsidianBorder,
    mutedText = ObsidianTextMuted
)

private val LightCooldownPalette = CooldownSemanticPalette(
    fast = LightEmerald,
    fastContainer = LightEmeraldContainer,
    moderate = LightCyan,
    moderateContainer = LightCyanContainer,
    long = LightAmber,
    longContainer = LightAmberContainer,
    max = LightCoral,
    maxContainer = LightCoralContainer,
    hairline = PorcelainBorder,
    mutedText = PorcelainTextMuted
)

val LocalCooldownPalette = staticCompositionLocalOf { DarkCooldownPalette }

private val DarkColorScheme = darkColorScheme(
    primary = SignalEmerald,
    onPrimary = SignalEmeraldOn,
    primaryContainer = SignalEmeraldContainer,
    onPrimaryContainer = SignalEmerald,
    secondary = SignalCyan,
    onSecondary = Color(0xFF031B29),
    secondaryContainer = SignalCyanContainer,
    onSecondaryContainer = SignalCyan,
    tertiary = SignalAmber,
    onTertiary = Color(0xFF2B1B02),
    tertiaryContainer = SignalAmberContainer,
    onTertiaryContainer = SignalAmber,
    error = SignalCoral,
    onError = Color.White,
    errorContainer = SignalCoralContainer,
    onErrorContainer = SignalCoral,
    background = ObsidianBg,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = ObsidianTextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = Color(0xFF252F3F),
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1D2532)
)

private val LightColorScheme = lightColorScheme(
    primary = LightEmerald,
    onPrimary = Color.White,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = LightCyan,
    onSecondary = Color.White,
    secondaryContainer = LightCyanContainer,
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = LightAmber,
    onTertiary = Color.White,
    tertiaryContainer = LightAmberContainer,
    onTertiaryContainer = Color(0xFF78350F),
    error = LightCoral,
    onError = Color.White,
    errorContainer = LightCoralContainer,
    onErrorContainer = Color(0xFF881337),
    background = PorcelainBg,
    onBackground = PorcelainTextPrimary,
    surface = PorcelainSurface,
    onSurface = PorcelainTextPrimary,
    surfaceVariant = PorcelainSurfaceElevated,
    onSurfaceVariant = PorcelainTextSecondary,
    surfaceContainerLowest = PorcelainSurface,
    surfaceContainerLow = PorcelainBg,
    surfaceContainer = PorcelainSurfaceElevated,
    surfaceContainerHigh = PorcelainSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD5DEE9),
    outline = PorcelainBorder,
    outlineVariant = Color(0xFFE5EAF2)
)

val CoordShiftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CoordShiftTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val cooldownPalette = if (isDark) DarkCooldownPalette else LightCooldownPalette

    CompositionLocalProvider(LocalCooldownPalette provides cooldownPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CoordShiftTypography,
            shapes = CoordShiftShapes,
            content = content
        )
    }
}
