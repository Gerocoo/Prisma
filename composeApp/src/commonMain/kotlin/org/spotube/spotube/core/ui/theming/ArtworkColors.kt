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
package org.spotube.spotube.core.ui.theming

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import coil3.Image
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Tonal palette built from the dominant color of the current cover, used to tint the player
 * and the mini player (the "colors from artwork" look of Material You music players).
 */
@Immutable
data class ArtworkColors(
    /** Page background of the full player. */
    val container: Color,
    /** Raised surfaces: mini player, control groups. */
    val containerHigh: Color,
    /** Secondary buttons (previous/next, toggles). */
    val tonal: Color,
    /** The main action (play/pause) and the progress wave. */
    val accent: Color,
    val onAccent: Color,
    val onContainer: Color,
    val onContainerMuted: Color,
)

/** ARGB pixels of a small, downscaled copy of [this] image, or null if unreadable. */
internal expect fun Image.samplePixels(maxSize: Int): IntArray?

/** Loads [model] (URL, bytes...) and derives its palette; keeps the last palette while loading. */
@Composable
fun rememberArtworkColors(model: Any?): ArtworkColors? {
    val context = LocalPlatformContext.current
    var colors by remember { mutableStateOf<ArtworkColors?>(null) }
    LaunchedEffect(model) {
        if (model == null) {
            colors = null
            return@LaunchedEffect
        }
        val seed = withContext(Dispatchers.Default) {
            runCatching {
                val request = ImageRequest.Builder(context).data(model).size(96).build()
                val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
                image?.samplePixels(48)?.let(::dominantColor)
            }.getOrNull()
        }
        if (seed != null) colors = artworkColorsFrom(seed)
    }
    return colors
}

/** Animates between palettes when the track changes; [fallback] is used before the first cover. */
@Composable
fun ArtworkColors?.animated(fallback: ArtworkColors): ArtworkColors {
    val target = this ?: fallback
    val spec = tween<Color>(durationMillis = 450)
    val container by animateColorAsState(target.container, spec)
    val containerHigh by animateColorAsState(target.containerHigh, spec)
    val tonal by animateColorAsState(target.tonal, spec)
    val accent by animateColorAsState(target.accent, spec)
    val onAccent by animateColorAsState(target.onAccent, spec)
    val onContainer by animateColorAsState(target.onContainer, spec)
    val onContainerMuted by animateColorAsState(target.onContainerMuted, spec)
    return ArtworkColors(container, containerHigh, tonal, accent, onAccent, onContainer, onContainerMuted)
}

/** Palette from the app theme, for when there is no cover yet. */
@Composable
fun themeArtworkColors(): ArtworkColors = artworkColorsFrom(AppTheme.colors.primary)

/** Builds a dark tonal palette around [seed]'s hue, softening very loud or very grey seeds. */
fun artworkColorsFrom(seed: Color): ArtworkColors {
    val (h, s, _) = seed.toHsl()
    val sat = s.coerceIn(0f, 0.5f)
    return ArtworkColors(
        container = hsl(h, sat * 0.85f, 0.17f),
        containerHigh = hsl(h, sat, 0.27f),
        tonal = hsl(h, sat, 0.34f),
        accent = hsl(h, min(s + 0.15f, 0.8f), 0.82f),
        onAccent = hsl(h, sat, 0.14f),
        onContainer = hsl(h, min(sat, 0.3f), 0.95f),
        onContainerMuted = hsl(h, min(sat, 0.25f), 0.76f),
    )
}

/**
 * Most prominent colorful hue of the pixels: hues are bucketed and weighted by saturation and
 * how far from black/white they are; greyscale covers fall back to their average color.
 */
internal fun dominantColor(pixels: IntArray): Color? {
    if (pixels.isEmpty()) return null
    val bins = 36
    val weight = FloatArray(bins)
    val sumR = FloatArray(bins); val sumG = FloatArray(bins); val sumB = FloatArray(bins)
    var avgR = 0f; var avgG = 0f; var avgB = 0f; var count = 0
    for (p in pixels) {
        if ((p ushr 24) < 128) continue
        val r = ((p shr 16) and 0xFF) / 255f
        val g = ((p shr 8) and 0xFF) / 255f
        val b = (p and 0xFF) / 255f
        avgR += r; avgG += g; avgB += b; count++
        val (h, s, l) = Color(r, g, b).toHsl()
        if (s < 0.18f || l < 0.12f || l > 0.9f) continue
        val bin = ((h / 360f) * bins).toInt().coerceIn(0, bins - 1)
        val w = s * (1f - abs(l - 0.5f) * 2f)
        weight[bin] += w; sumR[bin] += r * w; sumG[bin] += g * w; sumB[bin] += b * w
    }
    if (count == 0) return null
    val best = weight.indices.maxByOrNull { weight[it] } ?: -1
    return if (best >= 0 && weight[best] > count * 0.01f) {
        Color(sumR[best] / weight[best], sumG[best] / weight[best], sumB[best] / weight[best])
    } else Color(avgR / count, avgG / count, avgB / count)
}

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val mx = max(red, max(green, blue)); val mn = min(red, min(green, blue))
    val l = (mx + mn) / 2f
    if (mx == mn) return Triple(0f, 0f, l)
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        red -> ((green - blue) / d + (if (green < blue) 6f else 0f))
        green -> ((blue - red) / d + 2f)
        else -> ((red - green) / d + 4f)
    } * 60f
    return Triple(h, s, l)
}

private fun hsl(h: Float, s: Float, l: Float): Color = Color.hsl(
    hue = ((h % 360f) + 360f) % 360f,
    saturation = s.coerceIn(0f, 1f),
    lightness = l.coerceIn(0f, 1f)
)
