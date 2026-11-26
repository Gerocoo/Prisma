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

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.spotube.plugin_interfaces.plugin_apis.device_control.DeviceControlException
import org.spotube.plugin_interfaces.plugin_apis.device_control.RemoteDevice
import org.spotube.plugin_interfaces.plugin_apis.device_control.RemotePlaybackState
import org.spotube.spotube.modules.plugin.DeviceControlPluginSource

@Serializable
data class SpotifyDevice(
    val id: String? = null,
    val name: String = "",
    val type: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("volume_percent") val volumePercent: Int? = null,
)

@Serializable
data class SpotifyPlayback(
    val device: SpotifyDevice? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long? = null,
    val item: SpotifyPlaybackItem? = null,
)

@Serializable
data class SpotifyPlaybackItem(
    val id: String? = null,
    val uri: String? = null,
    val name: String = "",
    @SerialName("duration_ms") val durationMs: Long = 0L,
    val artists: List<SpotifyArtistRef> = emptyList(),
    val album: SpotifyAlbumRef? = null,
)

@Serializable
data class SpotifyArtistRef(val name: String = "")

@Serializable
data class SpotifyAlbumRef(val images: List<SpotifyImage> = emptyList())

@Serializable
data class SpotifyImage(val url: String = "", val width: Int? = null)

/** Thrown when the remote service refuses a control call (no login, no device...). */
class SpotifyConnectException(val status: Int, message: String) : Exception(message)

/**
 * Spotify Connect remote-device control, entirely through whatever plugin implements
 * [org.spotube.plugin_interfaces.plugin_apis.device_control.DeviceControlAPI] for this account
 * (normally the Spotify plugin). The app itself never calls Spotify's unofficial "dealer"
 * socket or connect-state endpoints directly - it only talks to this bridge, which talks to
 * the sandboxed plugin, which talks to Spotify.
 */
class SpotifyConnectApi(
    private val deviceControl: DeviceControlPluginSource,
) {
    // The plugin API doesn't push changes out to a flow this bridge can safely collect from
    // outside the plugin's own dispatcher; SpotifyConnectViewModel already polls periodically,
    // so this simply never emits and polling alone keeps things in sync.
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private suspend fun <T> withPlugin(block: suspend org.spotube.spotube.core.zipline.PluginServiceScope.() -> T): T {
        val plugin = deviceControl.selectedDeviceControlPlugin.first()
            ?: throw SpotifyConnectException(404, "no device-control plugin selected")
        return try {
            plugin.use(block)
        } catch (e: DeviceControlException) {
            throw SpotifyConnectException(e.statusCode, e.message ?: "")
        }
    }

    suspend fun isAvailable(): Boolean = runCatching { withPlugin { deviceControlAPI.isAvailable() } }.getOrDefault(false)

    suspend fun devices(): List<SpotifyDevice> = withPlugin { deviceControlAPI.devices() }.map { it.toSpotifyDevice() }

    /** What the active device is playing, as of the last [devices] call. */
    suspend fun playback(): SpotifyPlayback? =
        withPlugin { deviceControlAPI.currentPlayback() }?.toSpotifyPlayback()

    suspend fun playOn(deviceId: String, trackUri: String?, positionMs: Long) =
        withPlugin { deviceControlAPI.playOn(deviceId, trackUri, positionMs) }

    suspend fun playTracks(deviceId: String, trackUris: List<String>, startIndex: Int, positionMs: Long = 0L) =
        withPlugin { deviceControlAPI.playTracks(deviceId, trackUris, startIndex, positionMs) }

    suspend fun transfer(deviceId: String, play: Boolean = true) =
        withPlugin { deviceControlAPI.transfer(deviceId, play) }

    suspend fun resume() = withPlugin { deviceControlAPI.resume() }
    suspend fun pause() = withPlugin { deviceControlAPI.pause() }
    suspend fun next() = withPlugin { deviceControlAPI.next() }
    suspend fun previous() = withPlugin { deviceControlAPI.previous() }
    suspend fun setVolume(percent: Int) = withPlugin { deviceControlAPI.setVolume(percent) }
    suspend fun seek(positionMs: Long) = withPlugin { deviceControlAPI.seek(positionMs) }

    /** Closes any open connection the plugin holds to the remote service. */
    fun disconnect() {
        // Best-effort: the plugin manages its own connection lifecycle; nothing to close here
        // since calls go through the short-lived `use { }` scope rather than a kept connection.
    }

    private fun RemoteDevice.toSpotifyDevice() = SpotifyDevice(
        id = id,
        name = name,
        type = type,
        isActive = isActive,
        volumePercent = volumePercent,
    )

    private fun RemotePlaybackState.toSpotifyPlayback() = SpotifyPlayback(
        device = device?.toSpotifyDevice(),
        isPlaying = isPlaying,
        progressMs = positionMs,
        item = item?.let {
            SpotifyPlaybackItem(
                id = it.id,
                uri = it.uri,
                name = it.title,
                durationMs = it.durationMs,
                artists = it.artistNames.map { name -> SpotifyArtistRef(name) },
                album = it.artworkUrl?.let { url -> SpotifyAlbumRef(listOf(SpotifyImage(url, 640))) },
            )
        },
    )
}
