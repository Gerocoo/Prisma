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

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import org.spotube.spotube.core.network.HttpClientFactory

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

@Serializable
private data class DevicesResponse(val devices: List<SpotifyDevice> = emptyList())

/** Thrown when Spotify refuses a control call (no Premium, no login, no device...). */
class SpotifyConnectException(val status: Int, message: String) : Exception(message)

/**
 * Spotify Connect through the official Web API player endpoints, with the Spotify plugin's
 * login. Controlling playback requires a Spotify Premium account.
 */
class SpotifyConnectApi(
    httpClientFactory: HttpClientFactory,
    private val auth: SpotifyPluginAuth,
) {
    private val http = httpClientFactory.default
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun isAvailable(): Boolean = auth.accessToken() != null

    private suspend fun HttpRequestBuilder.authorize() {
        val token = auth.accessToken() ?: throw SpotifyConnectException(401, "not logged in")
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun HttpResponse.check(): HttpResponse {
        if (!status.isSuccess()) throw SpotifyConnectException(status.value, bodyAsText())
        return this
    }

    suspend fun devices(): List<SpotifyDevice> {
        val text = http.get("$BASE/me/player/devices") { authorize() }.check().bodyAsText()
        return json.decodeFromString<DevicesResponse>(text).devices
    }

    suspend fun playback(): SpotifyPlayback? {
        val response = http.get("$BASE/me/player") { authorize() }.check()
        if (response.status == HttpStatusCode.NoContent) return null
        val text = response.bodyAsText()
        return if (text.isBlank()) null else json.decodeFromString<SpotifyPlayback>(text)
    }

    /** Plays [trackUri] from [positionMs] on [deviceId]. */
    suspend fun playOn(deviceId: String, trackUri: String?, positionMs: Long) {
        http.put("$BASE/me/player/play") {
            authorize()
            parameter("device_id", deviceId)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                if (trackUri != null) putJsonArray("uris") { add(trackUri) }
                put("position_ms", positionMs)
            }.toString())
        }.check()
    }

    /** Moves whatever Spotify is playing to [deviceId]. */
    suspend fun transfer(deviceId: String, play: Boolean = true) {
        http.put("$BASE/me/player") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("device_ids") { add(deviceId) }
                put("play", play)
            }.toString())
        }.check()
    }

    suspend fun resume() { http.put("$BASE/me/player/play") { authorize() }.check() }
    suspend fun pause() { http.put("$BASE/me/player/pause") { authorize() }.check() }
    suspend fun next() { http.post("$BASE/me/player/next") { authorize() }.check() }
    suspend fun previous() { http.post("$BASE/me/player/previous") { authorize() }.check() }

    suspend fun setVolume(percent: Int) {
        http.put("$BASE/me/player/volume") {
            authorize()
            parameter("volume_percent", percent.coerceIn(0, 100))
        }.check()
    }

    suspend fun seek(positionMs: Long) {
        http.put("$BASE/me/player/seek") {
            authorize()
            parameter("position_ms", positionMs)
        }.check()
    }

    private companion object {
        const val BASE = "https://api.spotify.com/v1"
    }
}
