package com.immichtv.ui.common

import androidx.compose.runtime.Composable
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme

/**
 * Colors for primary action buttons, filled with the Immich brand primary at rest (indigo in light,
 * light-blue in dark) and inverting to a bright high-contrast state on focus so the D-pad selection
 * stays unmistakable on a TV.
 */
@Composable
fun brandButtonColors(): ButtonColors = ButtonDefaults.colors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    focusedContainerColor = MaterialTheme.colorScheme.onSurface,
    focusedContentColor = MaterialTheme.colorScheme.surface,
)
