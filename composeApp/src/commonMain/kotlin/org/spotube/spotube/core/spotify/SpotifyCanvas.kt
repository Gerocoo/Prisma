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
package org.spotube.spotube.core.spotify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import kotlin.math.roundToInt
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.compose.koinInject
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.plugin.DeviceControlPluginSource

/**
 * Spotify Canvas: the short looping video Spotify shows behind a track.
 *
 * Works only through a plugin that implements device control for the account (the Spotify
 * plugin, normally): the app itself never calls Spotify's unofficial canvaz endpoint, the
 * plugin does, sandboxed, through its own HttpClientAPI bridge. Every failure (no such plugin,
 * not logged in, request failed) simply means "no canvas" and the cover stays.
 */
class SpotifyCanvasRepository(
    private val deviceControl: DeviceControlPluginSource,
) {
    private val logger = Logger.withTag("SpotifyCanvas")
    private val cache = mutableMapOf<String, String?>()
    private val mutex = Mutex()

    /** Video URL of the canvas for a Spotify track id, or null when there is none. */
    suspend fun canvasUrl(trackId: String): String? {
        mutex.withLock { if (cache.containsKey(trackId)) return cache[trackId] }
        val url = runCatching {
            val plugin = deviceControl.selectedDeviceControlPlugin.first() ?: return@runCatching null
            plugin.use { deviceControlAPI.canvasUrl(trackId) }
        }.onFailure { logger.w(it) { "Canvas request failed" } }.getOrNull()
        mutex.withLock { cache[trackId] = url }
        return url
    }

    /** Canvas already looked up for [trackId]: known at once, with no request. */
    fun cachedCanvasUrl(trackId: String?): String? = trackId?.let { cache[it] }
}

/** Canvas video URL for the playing entry, or null (no Spotify plugin, no canvas, disabled). */
@Composable
fun rememberSpotifyCanvasUrl(entry: QueueEntry?, enabled: Boolean): String? {
    val repository = koinInject<SpotifyCanvasRepository>()
    val queue = koinInject<AudioPlayerQueue>()
    val trackId = (entry as? QueueEntry.StreamingTrack)?.track?.id
    // A canvas looked up in advance is there from the first frame: no cover flash before it.
    val url by produceState(
        if (enabled) repository.cachedCanvasUrl(trackId) else null, trackId, enabled
    ) {
        value = if (enabled) repository.cachedCanvasUrl(trackId) else null
        if (enabled && trackId != null) value = repository.canvasUrl(trackId)
    }
    // Looks up the canvases of the next tracks while this one plays.
    LaunchedEffect(trackId, enabled) {
        if (!enabled || trackId == null) return@LaunchedEffect
        val entries = queue.queueFlow.value
        val index = entries.indexOfFirst { (it as? QueueEntry.StreamingTrack)?.track?.id == trackId }
        if (index < 0) return@LaunchedEffect
        entries.drop(index + 1).take(2).forEach { next ->
            (next as? QueueEntry.StreamingTrack)?.track?.id?.let { repository.canvasUrl(it) }
        }
    }
    return url
}

/**
 * Plays a canvas muted and looping, cropped to fill [modifier]. Calls [onFirstFrame] once the
 * video is actually showing. [onBackdrop] receives a tiny copy of the current frame a few times
 * a second: stretched back to full size it is the blurred picture drawn behind the controls.
 * Android only; elsewhere nothing is drawn and the cover stays.
 */
@Composable
expect fun CanvasVideo(
    url: String,
    modifier: Modifier = Modifier,
    onFirstFrame: () -> Unit = {},
    onBackdrop: (ImageBitmap) -> Unit = {},
)

/** The blurred copy of the canvas video shared with the controls drawn over it. */
@Stable
class CanvasBackdrop {
    var image by mutableStateOf<ImageBitmap?>(null)
    var container by mutableStateOf<LayoutCoordinates?>(null)

    /** 0 = no canvas (nothing is drawn), 1 = canvas fully shown. */
    var amount by mutableFloatStateOf(0f)
}

val LocalCanvasBackdrop = staticCompositionLocalOf<CanvasBackdrop?> { null }

/**
 * Frosted-glass fill for a control drawn over a canvas video: paints the part of the blurred
 * video that lies behind the control, then a dark veil so icons and text stay readable. Put it
 * after `clip(...)` and before the control's own `background(...)`. Does nothing without a canvas.
 */
@Composable
fun Modifier.canvasBlur(): Modifier {
    val backdrop = LocalCanvasBackdrop.current ?: return this
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { coordinates ->
            val container = backdrop.container
            if (container != null && container.isAttached && coordinates.isAttached) {
                origin = container.localPositionOf(coordinates, Offset.Zero)
            }
        }
        .drawBehind {
            val image = backdrop.image ?: return@drawBehind
            val amount = backdrop.amount
            val container = backdrop.container
            if (amount <= 0f || container == null || !container.isAttached) return@drawBehind
            val containerWidth = container.size.width.toFloat()
            val containerHeight = container.size.height.toFloat()
            // The video is cropped to fill the screen: place the small frame the same way.
            val scale = maxOf(containerWidth / image.width, containerHeight / image.height)
            val width = image.width * scale
            val height = image.height * scale
            drawImage(
                image = image,
                dstOffset = IntOffset(
                    ((containerWidth - width) / 2f - origin.x).roundToInt(),
                    ((containerHeight - height) / 2f - origin.y).roundToInt()
                ),
                dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                alpha = amount,
                filterQuality = FilterQuality.High
            )
            drawRect(Color.Black.copy(alpha = 0.34f * amount))
        }
}
