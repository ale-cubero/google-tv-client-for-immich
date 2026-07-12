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
package com.immichtv.ui.viewer

import android.content.Context
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import com.immichtv.ui.i18n.Strings

/**
 * Human-readable dynamic-range status for a video: whether the source is HDR (from the decoded
 * video format's color info) and — on Android 14+ — whether the display is actually emitting HDR
 * (the live HDR/SDR luminance ratio rises above 1 only when HDR is really being output).
 *
 * The output signal is derived independently of the source color info, so we can still report
 * "shown in HDR" / "displayed as SDR" even when the decoder didn't surface a colour transfer.
 */
internal object VideoHdr {

    /**
     * Always returns a string for a video (so the panel row is never blank once the video renders):
     * e.g. "HDR10 (PQ) · shown in HDR", "HLG (HDR) · displayed as SDR", or "SDR".
     */
    fun label(colorInfo: ColorInfo?, context: Context, strings: Strings): String {
        val source = sourceType(colorInfo, strings) // "HDR10 (PQ)" / "HLG (HDR)" / SDR / null (not reported)
        val output = outputState(context, strings)  // shown-in-HDR / SDR-output / null (unknown)
        return when {
            source != null && output != null -> "$source  ·  $output"
            source != null -> source
            output != null -> output
            else -> strings.hdrSdr
        }
    }

    /** Source dynamic range from the decoded colour info, or null if the decoder didn't report it. */
    private fun sourceType(colorInfo: ColorInfo?, strings: Strings): String? {
        colorInfo ?: return null
        return when (colorInfo.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> "HDR10 (PQ)" // technical labels, not translated
            C.COLOR_TRANSFER_HLG -> "HLG (HDR)"
            C.COLOR_TRANSFER_SDR -> strings.hdrSdr
            // Transfer unset but wide-gamut BT.2020 primaries strongly imply HDR.
            else -> if (colorInfo.colorSpace == C.COLOR_SPACE_BT2020) "HDR (BT.2020)" else null
        }
    }

    /** Whether the display is presently emitting HDR (API 34+); null if it can't be determined. */
    private fun outputState(context: Context, strings: Strings): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val display = runCatching { context.display }.getOrNull() ?: return null
        if (!display.isHdrSdrRatioAvailable) return null
        return if (display.hdrSdrRatio > 1.05f) strings.hdrShown else strings.hdrToneMapped
    }
}
