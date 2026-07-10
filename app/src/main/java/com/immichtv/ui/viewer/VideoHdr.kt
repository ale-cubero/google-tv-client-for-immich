package com.immichtv.ui.viewer

import android.content.Context
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.ColorInfo

/**
 * Human-readable dynamic-range status for a video: whether the source is HDR (from the decoded
 * video format's color transfer) and — on Android 14+ — whether the display is currently emitting
 * HDR (the live HDR/SDR luminance ratio rises above 1 only when HDR is actually being output).
 */
internal object VideoHdr {

    /** e.g. "HDR (HLG) · shown in HDR", "HDR (PQ / HDR10) · tone-mapped to SDR", "SDR", or null if unknown. */
    fun label(colorInfo: ColorInfo?, context: Context): String? {
        val type = when (colorInfo?.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> "HDR (PQ / HDR10)"
            C.COLOR_TRANSFER_HLG -> "HDR (HLG)"
            C.COLOR_TRANSFER_SDR -> return "SDR"
            else -> return null // not yet known / unset
        }
        return outputState(context)?.let { "$type  ·  $it" } ?: type
    }

    /** Whether the display is presently emitting HDR (API 34+); null if it can't be determined. */
    private fun outputState(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val display = runCatching { context.display }.getOrNull() ?: return null
        if (!display.isHdrSdrRatioAvailable) return null
        return if (display.hdrSdrRatio > 1.05f) "shown in HDR" else "tone-mapped to SDR"
    }
}
