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
package org.spotube.spotube.modules.shell.spotify_connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.spotify.SpotifyConnectApi
import org.spotube.spotube.core.spotify.SpotifyConnectException
import org.spotube.spotube.core.spotify.SpotifyDevice
import org.spotube.spotube.core.spotify.SpotifyPlayback
import kotlin.time.Duration.Companion.milliseconds

enum class SpotifyConnectError { NotLoggedIn, PremiumRequired, NoDevices, Failed }

data class SpotifyConnectUiState(
    val isOpen: Boolean = false,
    val isLoading: Boolean = false,
    val devices: List<SpotifyDevice> = emptyList(),
    val playback: SpotifyPlayback? = null,
    val error: SpotifyConnectError? = null,
    /** Technical detail of the last failure (HTTP status and Spotify's message), for reports. */
    val errorDetail: String? = null,
    /** Device id Prisma handed playback to; null while Prisma plays itself. */
    val remoteDeviceId: String? = null,
) {
    val remoteDevice: SpotifyDevice? get() = devices.firstOrNull { it.id == remoteDeviceId } ?: playback?.device
    val isRemoteActive: Boolean get() = remoteDeviceId != null
}

/**
 * Spotify Connect remote: lists the account's Spotify devices (desktop app, speakers, TVs),
 * hands the current track over to one of them and controls it, and brings playback back.
 * Polls Spotify while the sheet is open or a device is playing for Prisma.
 */
class SpotifyConnectViewModel(
    private val api: SpotifyConnectApi,
    private val audioPlayer: AudioPlayerInterface,
    private val queue: AudioPlayerQueue,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpotifyConnectUiState())
    val uiState: StateFlow<SpotifyConnectUiState> = _uiState.asStateFlow()
    private var pollJob: Job? = null

    fun open() {
        _uiState.update { it.copy(isOpen = true, isLoading = true, error = null) }
        startPolling()
    }

    fun dismiss() {
        _uiState.update { it.copy(isOpen = false) }
        if (!_uiState.value.isRemoteActive) stopPolling()
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            // Spotify pushes changes over the socket; the slow loop only keeps the position fresh.
            launch { api.changes.collect { refreshOnce() } }
            while (isActive) {
                refreshOnce()
                // After a refusal wait longer, so a rate limit is not made worse by retrying.
                val pause = when {
                    _uiState.value.error == SpotifyConnectError.Failed -> 15_000L
                    _uiState.value.isOpen -> 4_000L
                    else -> 8_000L
                }
                delay(pause)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        api.disconnect()
    }

    private suspend fun refreshOnce() {
        if (!api.isAvailable()) {
            _uiState.update { it.copy(isLoading = false, error = SpotifyConnectError.NotLoggedIn) }
            return
        }
        runCatching {
            val devices = api.devices()
            val playback = api.playback()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    devices = devices,
                    playback = playback,
                    error = if (devices.isEmpty()) SpotifyConnectError.NoDevices else null,
                    errorDetail = null
                )
            }
        }.onFailure { e -> report(e) }
    }

    private fun report(e: Throwable) {
        val error = when ((e as? SpotifyConnectException)?.status) {
            401 -> SpotifyConnectError.NotLoggedIn
            403 -> SpotifyConnectError.PremiumRequired
            404 -> SpotifyConnectError.NoDevices
            else -> SpotifyConnectError.Failed
        }
        val detail = when (e) {
            is SpotifyConnectException -> "HTTP ${e.status}: ${e.message.orEmpty().take(160)}"
            else -> "${e::class.simpleName}: ${e.message.orEmpty().take(160)}"
        }
        _uiState.update { it.copy(isLoading = false, error = error, errorDetail = detail) }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { report(it) }
            delay(400)
            refreshOnce()
        }
    }

    /** Plays what Prisma is playing on [device], from the same point, and pauses Prisma. */
    fun playOn(device: SpotifyDevice) {
        val deviceId = device.id ?: return
        action {
            val entry = queue.currentQueueEntryFlow.value as? QueueEntry.StreamingTrack
            val trackId = entry?.track?.id?.takeIf { SPOTIFY_ID.matches(it) }
            val position = audioPlayer.positionFlow.value.inWholeMilliseconds
            if (trackId != null) api.playOn(deviceId, "spotify:track:$trackId", position)
            else api.transfer(deviceId, play = true)
            if (audioPlayer.playerStateFlow.value == PlayerState.PLAYING) audioPlayer.pause()
            _uiState.update { it.copy(remoteDeviceId = deviceId) }
            startPolling()
        }
    }

    /** Stops Spotify and resumes here; same track: from Spotify's position. */
    fun bringBack() {
        action {
            val playback = _uiState.value.playback
            runCatching { api.pause() }
            val entry = queue.currentQueueEntryFlow.value as? QueueEntry.StreamingTrack
            val sameTrack = playback?.item?.id != null && playback.item.id == entry?.track?.id
            if (sameTrack) playback?.progressMs?.let { audioPlayer.seekTo(it.milliseconds) }
            audioPlayer.play()
            _uiState.update { it.copy(remoteDeviceId = null) }
            if (!_uiState.value.isOpen) stopPolling()
        }
    }

    fun togglePlay() = action {
        if (_uiState.value.playback?.isPlaying == true) api.pause() else api.resume()
    }

    fun next() = action { api.next() }
    fun previous() = action { api.previous() }
    fun setVolume(percent: Int) = action { api.setVolume(percent) }
    fun seek(fraction: Float) = action {
        val duration = _uiState.value.playback?.item?.durationMs ?: return@action
        api.seek((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    override fun onCleared() {
        stopPolling()
    }

    private companion object {
        val SPOTIFY_ID = Regex("^[0-9A-Za-z]{22}$")
    }
}
