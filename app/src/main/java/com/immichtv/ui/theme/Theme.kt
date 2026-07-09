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

/** App-wide TV Material 3 theme. TV apps are dark by convention. */
@Composable
fun ImmichTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ImmichDarkColors, content = content)
}
