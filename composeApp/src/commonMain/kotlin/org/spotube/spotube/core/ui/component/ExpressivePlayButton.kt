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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay

/**
 * Play/pause button that answers the tap with its shape, like the Pixel player: a circle while
 * stopped, a rounded square while playing, and in between — from the tap until playback really
 * starts — it keeps morphing between the two and gently breathes, so a slow server never leaves
 * you wondering whether the tap was taken. Pressing squeezes it.
 */
@Composable
fun MorphingPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    loading: Boolean = false,
    container: Color = AppTheme.colors.primary,
    content: Color = AppTheme.colors.onPrimary,
    contentDescription: String? = null,
) {
    // "Pending" from the tap until the play state flips (or a safety timeout).
    var pending by remember { mutableStateOf(false) }
    var lastPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (isPlaying != lastPlaying) pending = false
        lastPlaying = isPlaying
    }
    LaunchedEffect(pending) {
        if (pending) {
            delay(10_000)
            pending = false
        }
    }
    val busy = pending || loading

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val restPercent = if (isPlaying) 30f else 50f
    val settled by animateFloatAsState(
        if (pressed) 22f else restPercent,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "playCorner"
    )
    val wobble = rememberInfiniteTransition(label = "playPending")
    val wave by wobble.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "playWave"
    )
    val corner = if (busy) 26f + 24f * wave else settled
    val scale by animateFloatAsState(
        when {
            pressed -> 0.88f
            busy -> 0.94f
            else -> 1f
        },
        spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "playScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (busy) rotationZ = 18f * (wave - 0.5f)
            }
            .clip(RoundedCornerShape(corner.toInt()))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                pending = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // While waiting for the stream the icon becomes the Expressive loading shape.
        Crossfade(targetState = busy, label = "playButtonGlyph") { loadingNow ->
            if (loadingNow) {
                ExpressiveLoadingIndicator(size = size * 0.5f, color = content)
            } else {
                MillenniumIcon(
                    if (isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                    contentDescription = contentDescription,
                    tint = content,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
    }
}
