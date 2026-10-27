/*
 * TV client for Immich — a read-only Android TV client for a self-hosted Immich server.
 * Copyright (C) 2026 Alejandro Cubero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.immichtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/** User-selectable UI theme, persisted in settings. */
enum class ThemeMode { DARK, LIGHT }

/**
 * User-selectable text size, persisted in settings. [scale] multiplies the base type sizes.
 * [NORMAL] (the default) matches what used to be "Small"; [SMALL] and [LARGE] step 0.10 on either
 * side of it.
 */
enum class TextSize(val scale: Float) { SMALL(0.68f), NORMAL(0.78f), LARGE(0.88f) }

/**
 * User-selectable size of the Home screen's thumbnails, persisted in settings. Home only: other
 * screens keep their own sizes. [scale] multiplies the tile dimensions; [LARGE] is the original
 * size, and [NORMAL] (the default) and [SMALL] fit more tiles on each rail.
 */
enum class HomeTileSize(val scale: Float) { SMALL(0.7f), NORMAL(0.85f), LARGE(1f) }

/** Active text size, provided by [ImmichTvTheme] so nested themes (e.g. the viewer) inherit it. */
val LocalTextSize = staticCompositionLocalOf { TextSize.NORMAL }

/**
 * Shared layout constants for a consistent 10-foot feel. Overscan is ~5% of a 1080p TV frame
 * (≈48/27 dp at typical TV density); a single set of corner radii and one focus treatment keep the
 * UI cohesive across screens.
 */
object AppDimens {
    val overscanH = 48.dp
    val overscanV = 27.dp
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 20.dp
    const val cardFocusScale = 1.10f
    val cardFocusBorder = 4.dp // thick enough to read against any thumbnail, in the brand color
    val railSpacing = 22.dp // vertical gap between Home rails, room for the focused-tile scale-up
}

// Base type sizes (the LARGE tier) — bumped from Material's phone defaults for legibility at TV
// distance. [scale] shrinks them for the Normal/Small tiers. Only fontSize is scaled; the default
// line heights keep enough headroom. Cached per scale so recomposition doesn't rebuild it.
private fun immichTypography(scale: Float): Typography = Typography().let { d ->
    d.copy(
        headlineMedium = d.headlineMedium.copy(fontSize = (32f * scale).sp),
        headlineSmall = d.headlineSmall.copy(fontSize = (26f * scale).sp),
        titleLarge = d.titleLarge.copy(fontSize = (24f * scale).sp),
        titleMedium = d.titleMedium.copy(fontSize = (20f * scale).sp),
        titleSmall = d.titleSmall.copy(fontSize = (18f * scale).sp),
        bodyLarge = d.bodyLarge.copy(fontSize = (18f * scale).sp),
        bodyMedium = d.bodyMedium.copy(fontSize = (16f * scale).sp),
        bodySmall = d.bodySmall.copy(fontSize = (14f * scale).sp),
        labelLarge = d.labelLarge.copy(fontSize = (16f * scale).sp),
        labelMedium = d.labelMedium.copy(fontSize = (14f * scale).sp),
        labelSmall = d.labelSmall.copy(fontSize = (12f * scale).sp),
    )
}

private val typographyByScale = TextSize.entries.associateWith { immichTypography(it.scale) }

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
    val chromeBg: Color,     // pill background (Home Search/Settings); opaque so the pattern doesn't show through
    val chromeBorder: Color, // its border when unfocused
    val onChrome: Color,     // its text/icon when unfocused
    val menuFocus: Color,    // focused row/chip background in menus & dialogs
    val pattern: Color,      // screen-background maze lines; Transparent = plain background
)

val DarkAppColors = AppColorScheme(
    placeholder = Color(0xFF212121),   // Immich dark-gray card color
    textSecondary = Color(0xFF9CA3AF),
    textTertiary = Color(0xFF8B95A6),  // lightened for readable small labels on near-black
    panel = Color(0xFF212121),         // menus/dialogs = Immich card gray
    panelTranslucent = Color(0xF00A0A0A),
    rail = Color(0xFF141414),
    scrim = Color(0xCC000000),
    chip = Color(0x88000000),
    outline = Color(0x1FFFFFFF),
    error = Color(0xFFF87171),
    chromeBg = Color(0xFF080808),     // was 20% black over the #0A0A0A background
    chromeBorder = Color(0x66FFFFFF),
    onChrome = Color(0xFFC3C8CF),
    menuFocus = Color(0xFF33373B),     // hover/focus lighter than the #212121 panel
    pattern = Color(0xFF1A1A1A),
)

val LightAppColors = AppColorScheme(
    placeholder = Color(0xFFE3E6F0),
    textSecondary = Color(0xFF5B6472),
    textTertiary = Color(0xFF6B7480),  // darkened for readable small labels on white
    panel = Color(0xFFFFFFFF),
    panelTranslucent = Color(0xF00A0A0A), // viewer stays dark
    rail = Color(0xFFEEF0F7),
    scrim = Color(0xCC000000),
    chip = Color(0x88000000),
    outline = Color(0x1F000000),
    error = Color(0xFFB3261E),
    chromeBg = Color(0xFFF2F2F2),     // was 5% black over white
    chromeBorder = Color(0x33000000),
    onChrome = Color(0xFF3A434D),
    menuFocus = Color(0xFFE7EAF6),     // subtle indigo-tinted focus (matches primary)
    pattern = Color(0xFFE6E6E6),
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
    val pattern: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.pattern
}

/**
 * App-wide TV Material 3 theme, honoring the user's light/dark and text-size choices. [textSize]
 * defaults to the value already in scope ([LocalTextSize]), so nested calls — e.g. the viewer, which
 * forces dark mode — keep the user's chosen size without threading it through.
 */
@Composable
fun ImmichTvTheme(
    mode: ThemeMode = ThemeMode.DARK,
    textSize: TextSize = LocalTextSize.current,
    content: @Composable () -> Unit,
) {
    val dark = mode == ThemeMode.DARK
    CompositionLocalProvider(
        LocalAppColors provides if (dark) DarkAppColors else LightAppColors,
        LocalTextSize provides textSize,
    ) {
        MaterialTheme(
            colorScheme = if (dark) ImmichDarkColors else ImmichLightColors,
            typography = typographyByScale.getValue(textSize),
            content = content,
        )
    }
}
