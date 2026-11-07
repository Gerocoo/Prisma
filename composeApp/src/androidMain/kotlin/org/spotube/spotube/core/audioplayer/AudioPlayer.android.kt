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

package org.spotube.spotube.core.audioplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.util.UnstableApi
import org.spotube.spotube.core.cast.LanMediaItemConverter
import org.spotube.spotube.media.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(context: Any) : AudioPlayerInterface {

    actual val context: Any = context

    private val appContext: Context = (context as Context).applicationContext

    private fun ensureServiceStarted() {
        // A plain start, never startForegroundService: that obliges the service to call
        // startForeground() within seconds of *every* call, and once Media3 has demoted it
        // on pause nothing does, so the app is killed (ForegroundServiceDidNotStartInTime).
        // Media3's MediaSessionService promotes itself to the foreground when playback starts.
        // Starting from the background (jam sync, remote control) throws; playback still runs
        // in-process without the service, so never let that crash the app.
        try {
            appContext.startService(Intent(appContext, PlaybackService::class.java))
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Failed to start playback service", e)
        }
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        // Start as soon as half a second of audio is buffered (default 2.5s), and resume after a
        // stall with 1.5s instead of 5s: playback begins noticeably faster on every track.
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 50_000,
                    /* bufferForPlaybackMs = */ 500,
                    /* bufferForPlaybackAfterRebufferMs = */ 1_500
                )
                .build()
        )
        .build()

    /**
     * Local ExoPlayer wrapped in Media3's CastPlayer: playback moves to a Chromecast when one is
     * picked (cast button, notification or system output switcher) and back to the phone after.
     * Without Google Play services it falls back to plain local playback.
     */
    @OptIn(UnstableApi::class)
    private val activePlayer: Player = try {
        CastPlayer.Builder(appContext)
            .setLocalPlayer(exoPlayer)
            .setRemotePlayer(
                RemoteCastPlayer.Builder(appContext)
                    .setMediaItemConverter(LanMediaItemConverter())
                    .build()
            )
            .build()
    } catch (e: Exception) {
        Log.w("AudioPlayer", "Google Cast unavailable, local playback only", e)
        exoPlayer
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var disposed = false

    private val currentPlaylist = mutableListOf<MediaItem>()
    private val originalPlaylist = mutableListOf<MediaItem>()
    private var shuffleEnabled = false
    private val urlIndexMap = mutableMapOf<String, Int>()

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _duration = MutableStateFlow(Duration.ZERO)
    private val _position = MutableStateFlow(Duration.ZERO)
    private val _bufferingPosition = MutableStateFlow(Duration.ZERO)
    private val _loopState = MutableStateFlow(LoopState.NONE)
    private val _shuffleMode = MutableStateFlow(false)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    private val _volume = MutableStateFlow(1.0f)
    private val _completion = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _error = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)

    actual override val playerStateFlow: StateFlow<PlayerState> = _playerState.asStateFlow()
    actual override val currentMediaItemFlow: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()
    actual override val playlistFlow: StateFlow<List<MediaItem>> = _playlist.asStateFlow()
    actual override val durationFlow: StateFlow<Duration> = _duration.asStateFlow()
    actual override val positionFlow: StateFlow<Duration> = _position.asStateFlow()
    actual override val bufferingPositionFlow: StateFlow<Duration> = _bufferingPosition.asStateFlow()
    actual override val loopStateFlow: StateFlow<LoopState> = _loopState.asStateFlow()
    actual override val shuffleModeFlow: StateFlow<Boolean> = _shuffleMode.asStateFlow()
    actual override val playbackSpeedFlow: StateFlow<Float> = _playbackSpeed.asStateFlow()
    actual override val volumeFlow: StateFlow<Float> = _volume.asStateFlow()
    actual override val completionFlow: Flow<Unit> = _completion.asSharedFlow()
    actual override val errorFlow: Flow<Throwable> = _error.asSharedFlow()

    private var lastPlaybackState = Player.STATE_IDLE

    init {
        activePlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                syncPlaybackState(playbackState)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                syncPlayWhenReady(playWhenReady)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                syncCurrentItem()
            }

            override fun onPlayerError(error: PlaybackException) {
                _error.tryEmit(error)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _loopState.tryEmit(
                    when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> LoopState.NONE
                        Player.REPEAT_MODE_ONE -> LoopState.ONE
                        Player.REPEAT_MODE_ALL -> LoopState.ALL
                        else -> LoopState.NONE
                    }
                )
            }

            override fun onVolumeChanged(volume: Float) {
                _volume.tryEmit(volume)
            }
        })

        scope.launch {
            while (isActive && !disposed) {
                _position.tryEmit(activePlayer.currentPosition.milliseconds)
                _duration.tryEmit(
                    if (activePlayer.duration > 0) activePlayer.duration.milliseconds else Duration.ZERO
                )
                _bufferingPosition.tryEmit(activePlayer.bufferedPosition.milliseconds)
                delay(250)
            }
        }
    }

    private fun syncPlaybackState(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && lastPlaybackState != Player.STATE_ENDED) {
            _completion.tryEmit(Unit)
            _playerState.tryEmit(PlayerState.COMPLETED)
        }
        lastPlaybackState = playbackState
        if (playbackState != Player.STATE_ENDED) {
            syncPlayWhenReady(activePlayer.playWhenReady)
        }
    }

    private fun syncPlayWhenReady(playWhenReady: Boolean) {
        val state = when {
            activePlayer.playbackState == Player.STATE_IDLE -> PlayerState.IDLE
            activePlayer.playbackState == Player.STATE_BUFFERING -> PlayerState.BUFFERING
            playWhenReady -> PlayerState.PLAYING
            else -> PlayerState.PAUSED
        }
        _playerState.tryEmit(state)
    }

    private fun syncCurrentItem() {
        val index = activePlayer.currentMediaItemIndex
        _currentMediaItem.tryEmit(
            if (index in currentPlaylist.indices) currentPlaylist[index] else null
        )
    }

    internal val player: Player get() = activePlayer

    private fun MediaItem.toExoMediaItem(): androidx.media3.common.MediaItem {
        return androidx.media3.common.MediaItem.Builder()
            .setMediaId(url)
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .apply {
                        if (coverBytes != null) {
                            setArtworkData(coverBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        } else if (coverURL.isNotBlank()) {
                            setArtworkUri(android.net.Uri.parse(coverURL))
                        }
                    }
                    .build()
            )
            .build()
    }

    actual override suspend fun play() {
        withContext(Dispatchers.Main) {
            ensureServiceStarted()
            activePlayer.play()
        }
    }

    actual override suspend fun pause() {
        withContext(Dispatchers.Main) {
            activePlayer.pause()
        }
    }

    actual override suspend fun stop() {
        withContext(Dispatchers.Main) {
            activePlayer.stop()
            _playerState.tryEmit(PlayerState.IDLE)
        }
    }

    actual override suspend fun seekTo(position: Duration) {
        withContext(Dispatchers.Main) {
            val duration = activePlayer.duration
            val targetMs = if (duration > 0) {
                position.inWholeMilliseconds.coerceIn(0, duration)
            } else {
                position.inWholeMilliseconds.coerceAtLeast(0)
            }
            activePlayer.seekTo(targetMs)
            _position.tryEmit(activePlayer.currentPosition.milliseconds)
        }
    }

    actual override suspend fun loop(state: LoopState) {
        withContext(Dispatchers.Main) {
            val repeatMode = when (state) {
                LoopState.NONE -> Player.REPEAT_MODE_OFF
                LoopState.ONE -> Player.REPEAT_MODE_ONE
                LoopState.ALL -> Player.REPEAT_MODE_ALL
            }
            activePlayer.repeatMode = repeatMode
            _loopState.tryEmit(state)
        }
    }

    actual override suspend fun shuffle(enabled: Boolean) {
        withContext(Dispatchers.Main) {
            if (disposed || currentPlaylist.isEmpty() || shuffleEnabled == enabled) return@withContext

            val currentIndex = activePlayer.currentMediaItemIndex
            val current = currentPlaylist.getOrNull(currentIndex) ?: return@withContext

            val rest = if (enabled) {
                originalPlaylist.filterNot { it.url == current.url }.shuffled()
            } else {
                originalPlaylist.filterNot { it.url == current.url }
            }
            // Disabling restores original order around the current item.
            val before = if (enabled) emptyList() else originalPlaylist.takeWhile { it.url != current.url }
            val after = if (enabled) rest else originalPlaylist.dropWhile { it.url != current.url }.drop(1)

            // Reorder in place around the playing item so playback is not interrupted.
            if (currentIndex < activePlayer.mediaItemCount - 1) {
                activePlayer.removeMediaItems(currentIndex + 1, activePlayer.mediaItemCount)
            }
            if (currentIndex > 0) activePlayer.removeMediaItems(0, currentIndex)
            activePlayer.addMediaItems(0, before.map { it.toExoMediaItem() })
            activePlayer.addMediaItems(after.map { it.toExoMediaItem() })

            currentPlaylist.clear()
            currentPlaylist.addAll(before + current + after)
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { i, item -> urlIndexMap[item.url] = i }

            shuffleEnabled = enabled
            _shuffleMode.tryEmit(enabled)
            _playlist.tryEmit(currentPlaylist.toList())
            syncCurrentItem()
        }
    }

    actual override suspend fun load(
        playlist: List<MediaItem>,
        autoPlay: Boolean,
        startPosition: Int
    ) {
        withContext(Dispatchers.Main) {
            currentPlaylist.clear()
            currentPlaylist.addAll(playlist)
            originalPlaylist.clear()
            originalPlaylist.addAll(playlist)
            shuffleEnabled = false
            _shuffleMode.tryEmit(false)
            urlIndexMap.clear()
            playlist.forEachIndexed { index, item ->
                urlIndexMap[item.url] = index
            }
            _playlist.tryEmit(currentPlaylist.toList())

            if (playlist.isEmpty()) {
                _playerState.tryEmit(PlayerState.IDLE)
                _currentMediaItem.tryEmit(null)
                _position.tryEmit(Duration.ZERO)
                _duration.tryEmit(Duration.ZERO)
                return@withContext
            }

            val exoItems = playlist.map { it.toExoMediaItem() }
            val safeIndex = startPosition.coerceIn(0, exoItems.lastIndex)

            activePlayer.setMediaItems(exoItems, safeIndex, 0L)
            activePlayer.prepare()

            if (autoPlay) {
                ensureServiceStarted()
                activePlayer.playWhenReady = true
            }

            _currentMediaItem.tryEmit(playlist[safeIndex])
        }
    }

    actual override suspend fun addMediaItem(mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            currentPlaylist.add(mediaItem)
            originalPlaylist.add(mediaItem)
            urlIndexMap[mediaItem.url] = currentPlaylist.lastIndex
            _playlist.tryEmit(currentPlaylist.toList())
            activePlayer.addMediaItem(mediaItem.toExoMediaItem())
        }
    }

    actual override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            val currentIndex = activePlayer.currentMediaItemIndex
            val insertIndex = (currentIndex + 1).coerceAtMost(currentPlaylist.size)

            currentPlaylist.add(insertIndex, mediaItem)
            val origCurrent = currentPlaylist.getOrNull(currentIndex)?.let { c -> originalPlaylist.indexOfFirst { it.url == c.url } } ?: -1
            originalPlaylist.add((origCurrent + 1).coerceIn(0, originalPlaylist.size), mediaItem)
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { index, item ->
                urlIndexMap[item.url] = index
            }
            _playlist.tryEmit(currentPlaylist.toList())

            activePlayer.addMediaItem(insertIndex, mediaItem.toExoMediaItem())
        }
    }

    actual override suspend fun removeMediaItem(mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            val index = urlIndexMap[mediaItem.url] ?: return@withContext
            currentPlaylist.removeAt(index)
            originalPlaylist.removeAll { it.url == mediaItem.url }
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { i, item ->
                urlIndexMap[item.url] = i
            }
            _playlist.tryEmit(currentPlaylist.toList())
            activePlayer.removeMediaItem(index)
        }
    }

    actual override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        withContext(Dispatchers.Main) {
            if (fromIndex !in currentPlaylist.indices || toIndex !in currentPlaylist.indices || fromIndex == toIndex) return@withContext

            val item = currentPlaylist.removeAt(fromIndex)
            currentPlaylist.add(toIndex, item)
            if (!shuffleEnabled && fromIndex in originalPlaylist.indices && toIndex in originalPlaylist.indices) {
                originalPlaylist.add(toIndex, originalPlaylist.removeAt(fromIndex))
            }
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { i, it ->
                urlIndexMap[it.url] = i
            }
            _playlist.tryEmit(currentPlaylist.toList())

            activePlayer.moveMediaItem(fromIndex, toIndex)
        }
    }

    actual override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            activePlayer.seekToNextMediaItem()
        }
    }

    actual override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            activePlayer.seekToPreviousMediaItem()
        }
    }

    actual override suspend fun jumpTo(index: Int) {
        withContext(Dispatchers.Main) {
            if (index in currentPlaylist.indices) {
                activePlayer.seekToDefaultPosition(index)
            }
        }
    }

    actual override suspend fun setVolume(volume: Float) {
        withContext(Dispatchers.Main) {
            val clamped = volume.coerceIn(0f, 1f)
            activePlayer.setVolume(clamped)
            _volume.tryEmit(clamped)
        }
    }

    actual override suspend fun setPlaybackSpeed(speed: Float) {
        withContext(Dispatchers.Main) {
            val clamped = speed.coerceIn(0.25f, 4f)
            activePlayer.setPlaybackSpeed(clamped)
            _playbackSpeed.tryEmit(clamped)
        }
    }

    actual override fun isDisposed(): Boolean = disposed

    actual override fun dispose() {
        disposed = true
        activePlayer.release()
        currentPlaylist.clear()
        originalPlaylist.clear()
        urlIndexMap.clear()

        _playerState.tryEmit(PlayerState.IDLE)
        _currentMediaItem.tryEmit(null)
        _playlist.tryEmit(emptyList())
        _duration.tryEmit(Duration.ZERO)
        _position.tryEmit(Duration.ZERO)
        _bufferingPosition.tryEmit(Duration.ZERO)
    }
}
