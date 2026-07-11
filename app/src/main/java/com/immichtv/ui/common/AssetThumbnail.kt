package com.immichtv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.immichtv.domain.Asset
import com.immichtv.ui.theme.AppColors

/**
 * A grid/rail thumbnail for a photo or video. Videos get a centered play button overlaid on the
 * poster — which also stands in cleanly when Immich hasn't generated a thumbnail yet (recent
 * uploads 404 on both thumbnail and preview), instead of rendering an empty/black tile — plus a
 * duration chip in the corner.
 */
@Composable
fun AssetThumbnail(asset: Asset, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(AppColors.placeholder),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = asset.thumbnailUrl,
            contentDescription = when {
                asset.isVideo -> "Video"
                asset.hasMotion -> "Motion photo"
                else -> "Photo"
            },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (asset.isVideo) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppColors.chip, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (asset.duration.isNotEmpty()) {
                Text(
                    text = asset.duration,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(AppColors.chip, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
