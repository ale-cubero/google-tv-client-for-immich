package com.immichtv.ui.viewer

import android.content.Context
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.ColorInfo

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
    fun label(colorInfo: ColorInfo?, context: Context): String {
        val source = sourceType(colorInfo)   // "HDR10 (PQ)" / "HLG (HDR)" / "SDR" / null (not reported)
        val output = outputState(context)    // "shown in HDR" / "displayed as SDR" / null (unknown)
        return when {
            source != null && output != null -> "$source  ·  $output"
            source != null -> source
            output != null -> output
            else -> "SDR"
        }
    }

    /** Source dynamic range from the decoded colour info, or null if the decoder didn't report it. */
    private fun sourceType(colorInfo: ColorInfo?): String? {
        colorInfo ?: return null
        return when (colorInfo.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> "HDR10 (PQ)"
            C.COLOR_TRANSFER_HLG -> "HLG (HDR)"
            C.COLOR_TRANSFER_SDR -> "SDR"
            // Transfer unset but wide-gamut BT.2020 primaries strongly imply HDR.
            else -> if (colorInfo.colorSpace == C.COLOR_SPACE_BT2020) "HDR (BT.2020)" else null
        }
    }

    /** Whether the display is presently emitting HDR (API 34+); null if it can't be determined. */
    private fun outputState(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val display = runCatching { context.display }.getOrNull() ?: return null
        if (!display.isHdrSdrRatioAvailable) return null
        return if (display.hdrSdrRatio > 1.05f) "shown in HDR" else "displayed as SDR"
    }
}
