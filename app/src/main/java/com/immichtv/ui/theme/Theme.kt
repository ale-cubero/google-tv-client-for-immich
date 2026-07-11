package com.immichtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/** User-selectable UI theme, persisted in settings. */
enum class ThemeMode { DARK, LIGHT }

private val ImmichDarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00131F),
    surface = Color(0xFF161A1E),
    onSurface = Color(0xFFE6E9EC),
    background = Color(0xFF0B0E11),
    onBackground = Color(0xFFE6E9EC),
    border = Color(0xFF2A3138),
)

private val ImmichLightColors = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B2530),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF1B2530),
    border = Color(0xFFCBD3DA),
)

/**
 * Semantic colors used across screens that have no tv-material colorScheme slot. Theme-aware:
 * resolved from [LocalAppColors], which [ImmichTvTheme] provides per selected [ThemeMode].
 *
 * Media-overlay tokens ([chip], [scrim], [panelTranslucent]) stay dark in both themes — they sit on
 * top of photos/videos or dim the background, where a dark treatment reads correctly regardless of
 * the app theme.
 */
@Immutable
data class AppColorScheme(
    val placeholder: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val panel: Color,
    val panelTranslucent: Color,
    val rail: Color,
    val scrim: Color,
    val chip: Color,
    val outline: Color,
    val error: Color,
    val chromeBg: Color,     // translucent pill background (e.g. Home Settings button)
    val chromeBorder: Color, // its border when unfocused
    val onChrome: Color,     // its text/icon when unfocused
    val menuFocus: Color,    // focused row/chip background in menus & dialogs
)

val DarkAppColors = AppColorScheme(
    placeholder = Color(0xFF20262C),
    textSecondary = Color(0xFF9AA4AD),
    textTertiary = Color(0xFF6D7680),
    panel = Color(0xFF11161B),
    panelTranslucent = Color(0xF00E1216),
    rail = Color(0xFF0E1216),
    scrim = Color(0xCC000000),
    chip = Color(0x88000000),
    outline = Color(0x33FFFFFF),
    error = Color(0xFFFF6E6E),
    chromeBg = Color(0x33000000),
    chromeBorder = Color(0x66FFFFFF),
    onChrome = Color(0xFFB8C0C8),
    menuFocus = Color(0xFF2A333B),
)

val LightAppColors = AppColorScheme(
    placeholder = Color(0xFFDDE3E8),
    textSecondary = Color(0xFF566573),
    textTertiary = Color(0xFF8A94A0),
    panel = Color(0xFFFFFFFF),
    panelTranslucent = Color(0xF00E1216), // viewer stays dark
    rail = Color(0xFFEDEFF2),
    scrim = Color(0xCC000000),
    chip = Color(0x88000000),
    outline = Color(0x22000000),
    error = Color(0xFFC62828),
    chromeBg = Color(0x0D000000),
    chromeBorder = Color(0x33000000),
    onChrome = Color(0xFF3A434D),
    menuFocus = Color(0xFFE1E7EC),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/** Accessors so existing `AppColors.x` call sites read the active theme's palette. */
object AppColors {
    val placeholder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.placeholder
    val textSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary
    val textTertiary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textTertiary
    val panel: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.panel
    val panelTranslucent: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.panelTranslucent
    val rail: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.rail
    val scrim: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.scrim
    val chip: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.chip
    val outline: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.outline
    val error: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.error
    val chromeBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.chromeBg
    val chromeBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.chromeBorder
    val onChrome: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onChrome
    val menuFocus: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.menuFocus
}

/** App-wide TV Material 3 theme, honoring the user's light/dark choice. */
@Composable
fun ImmichTvTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (dark) ImmichDarkColors else ImmichLightColors,
            content = content,
        )
    }
}
