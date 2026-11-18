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

package org.spotube.plugin_interfaces.plugin_apis.device_control

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

const val DeviceControlAPI_SERVICE_NAME = "DeviceControlAPI"

@Serializable
data class RemoteDevice(
    val id: String,
    val name: String,
    val type: String,
    val isActive: Boolean,
    val volumePercent: Int? = null
)

@Serializable
data class RemotePlaybackItem(
    val id: String?,
    val uri: String?,
    val title: String,
    val durationMs: Long,
    val artistNames: List<String> = emptyList(),
    val artworkUrl: String? = null
)

@Serializable
data class RemotePlaybackState(
    val device: RemoteDevice?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val item: RemotePlaybackItem?
)

/** Thrown when the remote service refuses a control call (no session, no active device...). */
class DeviceControlException(val statusCode: Int, message: String) : Exception(message)

/**
 * Controls playback on other devices signed into the same streaming account - "Spotify Connect"
 * and the like. A plugin that implements this talks to whatever remote-control service the
 * backend exposes, through [org.spotube.plugin_interfaces.host_apis.HttpClientAPI] and
 * [org.spotube.plugin_interfaces.host_apis.WebSocketAPI]; the app itself never speaks that
 * protocol directly, and ships with none of it built in.
 */
interface DeviceControlAPI : ZiplineService {
    suspend fun isAvailable(): Boolean

    /** The account's devices, most recently active first. Also refreshes [currentPlayback]. */
    suspend fun devices(): List<RemoteDevice>

    /** What the active device was last known to be playing, as of the last [devices] call. */
    suspend fun currentPlayback(): RemotePlaybackState?

    /** Fires whenever the remote service pushes a change (another device started, paused...). */
    fun changes(): Flow<Unit>

    /** Plays [trackUri] from [positionMs] on [deviceId]; with no uri, just transfers playback there. */
    suspend fun playOn(deviceId: String, trackUri: String?, positionMs: Long)

    /** Plays [trackUris] on [deviceId] starting at [startIndex]. */
    suspend fun playTracks(deviceId: String, trackUris: List<String>, startIndex: Int, positionMs: Long = 0L)

    /** Moves whatever is playing to [deviceId]. */
    suspend fun transfer(deviceId: String, play: Boolean = true)

    suspend fun resume()
    suspend fun pause()
    suspend fun next()
    suspend fun previous()
    suspend fun seek(positionMs: Long)
    suspend fun setVolume(percent: Int)

    /** Closes any open connection to the remote service; the next call reconnects. */
    fun disconnect()
}
