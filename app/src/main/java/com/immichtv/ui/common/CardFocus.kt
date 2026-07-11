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
