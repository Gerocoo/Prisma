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

import android.content.Context
import android.view.TextureView
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Holds the video's [TextureView] and lays it out larger than itself, centered, so the video
 * covers the whole area and the overflow is cropped (center crop). A TextureView (not a
 * SurfaceView) so its frames can be copied for the blurred backdrop.
 */
private class CanvasCropLayout(context: Context) : FrameLayout(context) {
    val texture = TextureView(context).apply { isOpaque = false }

    /** Width / height of the video; canvases are portrait 9:16. */
    var aspect: Float = 9f / 16f
        set(value) {
            if (value > 0f && value != field) {
                field = value
                requestLayout()
            }
        }

    init {
        clipChildren = true
        addView(texture)
    }

    private fun childSize(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 0 to 0
        return if (width.toFloat() / height > aspect) {
            width to (width / aspect).toInt()
        } else {
            (height * aspect).toInt() to height
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        val (childWidth, childHeight) = childSize(width, height)
        texture.measure(
            MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        val (childWidth, childHeight) = childSize(width, height)
        val childLeft = (width - childWidth) / 2
        val childTop = (height - childHeight) / 2
        texture.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
    }
}

@OptIn(UnstableApi::class)
@Composable
actual fun CanvasVideo(
    url: String,
    modifier: Modifier,
    onFirstFrame: () -> Unit,
    onBackdrop: (ImageBitmap) -> Unit,
) {
    val context = LocalContext.current
    val firstFrame by rememberUpdatedState(onFirstFrame)
    val backdrop by rememberUpdatedState(onBackdrop)
    var layout by remember { mutableStateOf<CanvasCropLayout?>(null) }
    var showing by remember(url) { mutableStateOf(false) }
    // A separate, silent player: the music keeps playing on the main one.
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                showing = true
                firstFrame()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    layout?.aspect = videoSize.width.toFloat() / videoSize.height
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    // In the background there is no surface to draw on and nobody to watch: the video stops and
    // starts again when the app comes back (a decoder left running there is a crash and a drain).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Small copies of the frame for the blurred backdrop: 28 px wide is plenty once stretched.
    LaunchedEffect(player, layout, showing) {
        val view = layout ?: return@LaunchedEffect
        if (!showing) return@LaunchedEffect
        while (isActive) {
            val texture = view.texture
            if (player.isPlaying && texture.isAvailable && texture.width > 0 && texture.height > 0) {
                val width = 28
                val height = (width / view.aspect).toInt().coerceIn(16, 96)
                runCatching { texture.getBitmap(width, height) }.getOrNull()?.let { bitmap ->
                    backdrop(bitmap.asImageBitmap())
                }
            }
            delay(110)
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CanvasCropLayout(ctx).also { view ->
                view.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                )
                player.setVideoTextureView(view.texture)
                view.tag = player
                layout = view
            }
        },
        update = { view ->
            // Only when the track (and so the player) changed: rebinding restarts the output.
            if (view.tag !== player) {
                player.setVideoTextureView(view.texture)
                view.tag = player
            }
            layout = view
        }
    )
}
