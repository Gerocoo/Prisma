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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Seek bar whose played part is a gently moving wave (Material 3 Expressive style) and whose
 * remaining part is a flat track. The wave flattens while paused. Tap or drag to seek.
 */
@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    playing: Boolean = true,
    label: String? = null,
) {
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val amplitude by animateFloatAsState(if (playing) 1f else 0f, tween(400))
    val phase by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart)
    )
    val fraction = value.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .semantics {
                label?.let { contentDescription = it }
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (enabled) setProgress { target ->
                    currentOnChange(target.coerceIn(0f, 1f)); currentOnFinished(); true
                }
            }
            .then(
                if (enabled) Modifier
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            currentOnChange((offset.x / size.width).coerceIn(0f, 1f))
                            currentOnFinished()
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { currentOnFinished() },
                            onDragCancel = { currentOnFinished() },
                        ) { change, _ ->
                            change.consume()
                            currentOnChange((change.position.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                else Modifier
            )
    ) {
        val stroke = 4.dp.toPx()
        val thumb = 7.dp.toPx()
        val midY = size.height / 2f
        val endX = size.width * fraction
        val waveLength = 26.dp.toPx()
        val waveHeight = 3.dp.toPx() * amplitude

        // Remaining part: flat, dim track.
        if (endX + thumb < size.width) {
            drawLine(inactiveColor, Offset(endX + thumb, midY), Offset(size.width, midY), stroke, StrokeCap.Round)
        }
        // Played part: the wave.
        if (endX > stroke) {
            val path = Path()
            var x = 0f
            path.moveTo(0f, midY)
            while (x <= endX) {
                val y = midY + sin((x / waveLength) * 2f * PI.toFloat() - phase) * waveHeight
                path.lineTo(x, y)
                x += 2f
            }
            drawPath(path, activeColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        // Thumb: a short vertical pill.
        drawLine(activeColor, Offset(endX, midY - thumb), Offset(endX, midY + thumb), stroke * 1.3f, StrokeCap.Round)
    }
}
