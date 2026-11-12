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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.shell.spotify_connect.SpotifyConnectViewModel
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val SPOTIFY_TRACK_ID = Regex("^[0-9A-Za-z]{22}$")

/**
 * The player the UI talks to. While playback is handed to another Spotify device, play / pause /
 * skip / seek go to that device and the state flows mirror it; otherwise everything is local.
 */
class ConnectAudioPlayer(
    private val local: AudioPlayerInterface,
    private val connect: SpotifyConnectViewModel,
) : AudioPlayerInterface by local {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun play() {
        if (connect.isRemoteActive) connect.remoteResume() else local.play()
    }

    override suspend fun pause() {
        if (connect.isRemoteActive) connect.remotePause() else local.pause()
    }

    override suspend fun skipToNext() {
        if (connect.isRemoteActive) connect.remoteNext() else local.skipToNext()
    }

    override suspend fun skipToPrevious() {
        if (connect.isRemoteActive) connect.remotePrevious() else local.skipToPrevious()
    }

    override suspend fun seekTo(position: Duration) {
        if (connect.isRemoteActive) connect.remoteSeek(position.inWholeMilliseconds) else local.seekTo(position)
    }

    override val playerStateFlow: StateFlow<PlayerState> =
        combine(local.playerStateFlow, connect.uiState) { localState, connectState ->
            val playback = connectState.playback
            if (connectState.isRemoteActive && playback != null) {
                if (playback.isPlaying) PlayerState.PLAYING else PlayerState.PAUSED
            } else localState
        }.stateIn(scope, SharingStarted.Eagerly, local.playerStateFlow.value)

    override val durationFlow: StateFlow<Duration> =
        combine(local.durationFlow, connect.uiState) { localDuration, connectState ->
            val remote = connectState.playback?.item?.durationMs?.takeIf { it > 0 }
            if (connectState.isRemoteActive && remote != null) remote.milliseconds else localDuration
        }.stateIn(scope, SharingStarted.Eagerly, local.durationFlow.value)

    override val positionFlow: StateFlow<Duration> =
        combine(local.positionFlow, connect.uiState, ticker()) { localPosition, connectState, _ ->
            val playback = connectState.playback
            if (connectState.isRemoteActive && playback != null) {
                val base = playback.progressMs ?: 0L
                val elapsed = if (playback.isPlaying) {
                    (Clock.System.now().toEpochMilliseconds() - connectState.playbackAtMs).coerceAtLeast(0L)
                } else 0L
                val duration = playback.item?.durationMs ?: 0L
                val position = base + elapsed
                (if (duration > 0) position.coerceAtMost(duration) else position).milliseconds
            } else localPosition
        }.stateIn(scope, SharingStarted.Eagerly, local.positionFlow.value)

    private fun ticker() = flow {
        while (true) {
            emit(Unit)
            delay(500)
        }
    }
}

/**
 * The queue the UI talks to. While playback is handed to another Spotify device, whatever Prisma
 * starts (a track, an album, a playlist...) is played there; the local queue is still loaded,
 * paused, so the player shows the same tracks.
 */
class ConnectAudioPlayerQueue(
    private val local: AudioPlayerQueue,
    private val connect: SpotifyConnectViewModel,
) : AudioPlayerQueue by local {

    override suspend fun load(
        entries: List<QueueEntry>,
        autoPlay: Boolean,
        startPosition: Int,
        collectionEntry: QueueCollectionEntry?,
    ) {
        if (connect.isRemoteActive && autoPlay) {
            val uris = entries.mapNotNull { entry ->
                (entry as? QueueEntry.StreamingTrack)?.track?.id
                    ?.takeIf { SPOTIFY_TRACK_ID.matches(it) }?.let { "spotify:track:$it" }
            }
            // Only hand over lists made entirely of Spotify tracks; anything else plays here.
            if (uris.isNotEmpty() && uris.size == entries.size) {
                local.load(entries, autoPlay = false, startPosition = startPosition, collectionEntry = collectionEntry)
                connect.playTracksOnRemote(uris, startPosition)
                return
            }
        }
        local.load(entries, autoPlay, startPosition, collectionEntry)
    }

    override suspend fun jumpTo(index: Int, autoPlay: Boolean) {
        if (connect.isRemoteActive && autoPlay) {
            val uris = local.getQueue().mapNotNull { entry ->
                (entry as? QueueEntry.StreamingTrack)?.track?.id
                    ?.takeIf { SPOTIFY_TRACK_ID.matches(it) }?.let { "spotify:track:$it" }
            }
            if (uris.size == local.getQueue().size && index in uris.indices) {
                local.jumpTo(index, autoPlay = false)
                connect.playTracksOnRemote(uris, index)
                return
            }
        }
        local.jumpTo(index, autoPlay)
    }
}
