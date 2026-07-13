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
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.MaterialTheme
import com.immichtv.ui.theme.AppDimens

/**
 * One consistent focus signal for every browsable card: a brand-primary ring (inherits the card's
 * rounded shape) plus a subtle scale bump. Primary is bright enough to read against thumbnails while
 * matching the rest of the UI (the previous pure-white ring was off-brand).
 */
object CardFocus {
    @Composable
    fun border(): CardBorder = CardDefaults.border(
        focusedBorder = Border(BorderStroke(AppDimens.cardFocusBorder, MaterialTheme.colorScheme.primary)),
    )

    @Composable
    fun scale(): CardScale = CardDefaults.scale(focusedScale = AppDimens.cardFocusScale)
}
