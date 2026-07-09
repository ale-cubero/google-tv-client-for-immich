package com.immichtv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.immichtv.domain.Asset

/**
 * A grid/rail thumbnail for a photo or video. Videos get a centered play button overlaid on the
 * poster — which also stands in cleanly when Immich hasn't generated a thumbnail yet (recent
 * uploads 404 on both thumbnail and preview), instead of rendering an empty/black tile.
 */
@Composable
fun AssetThumbnail(asset: Asset, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(PLACEHOLDER_BG),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = asset.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (asset.isVideo) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x80000000), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "▶", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

private val PLACEHOLDER_BG = Color(0xFF20262C)
