package com.immichtv.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale

/**
 * Shared, highly-visible focus style for all browsable cards: a thick white ring (inherits the
 * card's rounded shape) plus a clear scale bump. The default tv-material focus (faint grey border,
 * small scale) is hard to see against photo thumbnails.
 */
object CardFocus {
    @Composable
    fun border(): CardBorder = CardDefaults.border(
        focusedBorder = Border(BorderStroke(4.dp, Color.White)),
    )

    @Composable
    fun scale(): CardScale = CardDefaults.scale(focusedScale = 1.15f)
}
