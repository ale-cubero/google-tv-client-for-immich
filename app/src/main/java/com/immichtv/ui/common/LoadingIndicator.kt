package com.immichtv.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.immichtv.ui.theme.AppColors

/** Indeterminate spinner (tv-material ships no progress indicator). */
@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "spinnerAngle",
    )
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = stroke,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = Size(this.size.width - stroke.width, this.size.height - stroke.width),
        )
    }
}

/** Full-screen centered loading state: spinner over a label. */
@Composable
fun LoadingState(text: String, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spinner()
            Text(
                text = text,
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Full-screen centered empty state: a big glyph over a message. */
@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = subtitle,
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
