package com.immichtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val ImmichDarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00131F),
    surface = Color(0xFF161A1E),
    onSurface = Color(0xFFE6E9EC),
    background = Color(0xFF0B0E11),
    onBackground = Color(0xFFE6E9EC),
    border = Color(0xFF2A3138),
)

/**
 * Semantic colors used across screens that have no tv-material colorScheme slot.
 * Single source of truth instead of hex literals scattered per file.
 */
object AppColors {
    /** Neutral placeholder behind images while they load. */
    val placeholder = Color(0xFF20262C)

    /** Secondary text: captions, counts, subtitles. */
    val textSecondary = Color(0xFF9AA4AD)

    /** Tertiary text: row labels in detail panels. */
    val textTertiary = Color(0xFF6D7680)

    /** Solid dark panel background (menus, dialogs). */
    val panel = Color(0xFF11161B)

    /** Near-opaque side-panel background (viewer metadata). */
    val panelTranslucent = Color(0xF00E1216)

    /** Solid rail background (timeline year scrubber). */
    val rail = Color(0xFF0E1216)

    /** Full-screen scrim behind dialogs. */
    val scrim = Color(0xCC000000)

    /** Badge/overlay chip background on top of media. */
    val chip = Color(0x88000000)

    /** Hairline border for panels/dialogs. */
    val outline = Color(0x33FFFFFF)

    /** Error text. */
    val error = Color(0xFFFF6E6E)
}

/** App-wide TV Material 3 theme. TV apps are dark by convention. */
@Composable
fun ImmichTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ImmichDarkColors, content = content)
}
