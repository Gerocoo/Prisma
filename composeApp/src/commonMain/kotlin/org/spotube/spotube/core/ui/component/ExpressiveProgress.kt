/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
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
package org.spotube.spotube.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.spotube.spotube.core.ui.theming.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Material 3 Expressive style loading indicator: a filled, softly scalloped shape that rotates
 * and "breathes". Use it wherever a spinner is needed.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = AppTheme.colors.primary,
) {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)))
    val breath by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse))
    Canvas(modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f * 0.82f
        val lobes = 7
        val depth = radius * (0.06f + 0.08f * breath)
        val path = Path()
        val steps = 140
        for (i in 0..steps) {
            val a = (i.toFloat() / steps) * 2f * PI.toFloat()
            val r = radius - depth + depth * cos(lobes * a)
            val x = center.x + r * cos(a)
            val y = center.y + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        rotate(rotation, center) { drawPath(path, color) }
    }
}

/** Indeterminate wavy line (Material 3 Expressive linear progress) for "loading" strips. */
@Composable
fun WavyLinearProgress(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.primary,
    trackColor: Color = AppTheme.colors.surfaceContainerHigh,
) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(900, easing = LinearEasing)))
    val head by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)))
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val stroke = 4.dp.toPx()
        val midY = size.height / 2f
        val waveLength = 22.dp.toPx()
        val amp = 2.5.dp.toPx()
        drawLine(trackColor, Offset(0f, midY), Offset(size.width, midY), stroke, StrokeCap.Round)
        val segment = size.width * 0.4f
        val end = (size.width + segment) * head
        val start = (end - segment).coerceAtLeast(0f)
        val visibleEnd = end.coerceAtMost(size.width)
        if (visibleEnd > start) {
            val path = Path()
            var x = start
            path.moveTo(x, midY + sin(x / waveLength * 2f * PI.toFloat() - phase) * amp)
            while (x <= visibleEnd) {
                path.lineTo(x, midY + sin(x / waveLength * 2f * PI.toFloat() - phase) * amp)
                x += 2f
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

/**
 * Pull-to-refresh with a Material 3 Expressive indicator: a tonal circle holding the loading
 * shape slides down from the top while pulling and stays there while refreshing.
 */
@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }
    val maxPull = with(density) { 140.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val refreshing by rememberUpdatedState(isRefreshing)
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) offset.animateTo(threshold, spring()) else offset.animateTo(0f, spring())
    }

    val connection = remember(threshold, maxPull) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Pushing back up while the indicator is out first retracts it.
                if (source == NestedScrollSource.UserInput && available.y < 0f && offset.value > 0f && !refreshing) {
                    val consumed = available.y.coerceAtLeast(-offset.value)
                    scope.launch { offset.snapTo(offset.value + consumed) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (refreshing || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                scope.launch { offset.snapTo((offset.value + available.y * 0.5f).coerceAtMost(maxPull)) }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (refreshing) return Velocity.Zero
                if (offset.value >= threshold) {
                    currentOnRefresh()
                    offset.animateTo(threshold, spring())
                } else if (offset.value > 0f) {
                    offset.animateTo(0f, spring())
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier.nestedScroll(connection)) {
        content()
        val progress = (offset.value / threshold).coerceIn(0f, 1f)
        if (offset.value > 1f || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (offset.value - with(density) { 48.dp.toPx() }).roundToInt()) }
                    .padding(top = 8.dp)
                    .size(48.dp)
                    .graphicsLayer { alpha = progress; scaleX = 0.6f + 0.4f * progress; scaleY = scaleX }
                    .clip(CircleShape)
                    .background(AppTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveLoadingIndicator(size = 30.dp)
            }
        }
    }
}
