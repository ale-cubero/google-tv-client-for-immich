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

// Palette matches Immich's own brand colors (web `app.css`): indigo primary in light,
// light-blue primary in dark, near-black surfaces (#0A0A0A bg, #212121 cards) in dark.
private val ImmichDarkColors = darkColorScheme(
    primary = Color(0xFFACCBFA),      // --immich-dark-primary
    onPrimary = Color(0xFF0A0F1A),
    surface = Color(0xFF212121),      // --immich-dark-gray (cards/panels)
    onSurface = Color(0xFFE5E7EB),    // --immich-dark-fg
    background = Color(0xFF0A0A0A),   // --immich-dark-bg
    onBackground = Color(0xFFE5E7EB),
    border = Color(0xFF2E2E2E),
)

private val ImmichLightColors = lightColorScheme(
    primary = Color(0xFF4250AF),      // --immich-primary
    onPrimary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),    // --immich-fg (near-black)
    background = Color(0xFFFFFFFF),   // --immich-bg
    onBackground = Color(0xFF0A0A0A),
    border = Color(0xFFDDE0EC),
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
    placeholder = Color(0xFF212121),   // Immich dark-gray card color
    textSecondary = Color(0xFF9CA3AF),
    textTertiary = Color(0xFF6B7280),
    panel = Color(0xFF212121),         // menus/dialogs = Immich card gray
    panelTranslucent = Color(0xF00A0A0A),
    rail = Color(0xFF141414),
    scrim = Color(0xCC000000),
    chip = Color(0x88000000),
    outline = Color(0x1FFFFFFF),
    error = Color(0xFFF87171),
    chromeBg = Color(0x33000000),
    chromeBorder = Color(0x66FFFFFF),
    onChrome = Color(0xFFC3C8CF),
    menuFocus = Color(0xFF33373B),     // hover/focus lighter than the #212121 panel
)

val LightAppColors = AppColorScheme(
    placeholder = Color(0xFFE3E6F0),
    textSecondary = Color(0xFF5B6472),
    textTertiary = Color(0xFF8A93A3),
    panel = Color(0xFFFFFFFF),
    panelTranslucent = Color(0xF00A0A0A), // viewer stays dark
    rail = Color(0xFFEEF0F7),
    scrim = Color(0xCC000000),
    chip = Color(0x88000000),
    outline = Color(0x1F000000),
    error = Color(0xFFB3261E),
    chromeBg = Color(0x0D000000),
    chromeBorder = Color(0x33000000),
    onChrome = Color(0xFF3A434D),
    menuFocus = Color(0xFFE7EAF6),     // subtle indigo-tinted focus (matches primary)
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
