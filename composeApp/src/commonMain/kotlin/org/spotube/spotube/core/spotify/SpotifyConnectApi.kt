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

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import org.spotube.spotube.core.network.HttpClientFactory
import kotlin.random.Random
import kotlin.time.Clock

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

/** Thrown when Spotify refuses a control call (no login, no device...). */
class SpotifyConnectException(val status: Int, message: String) : Exception(message)

/**
 * Spotify Connect the way Spotify's own web player does it, with the Spotify plugin's login.
 *
 * The public Web API (`api.spotify.com/v1/me/player`) answers "API rate limit exceeded" to the
 * web player's token, so this talks to the same services the web player uses: a "dealer"
 * WebSocket gives a connection id, a hidden controller device is registered with it, and the
 * connect-state service then returns every device of the account with what it is playing, and
 * accepts commands for them.
 */
class SpotifyConnectApi(
    httpClientFactory: HttpClientFactory,
    private val auth: SpotifyPluginAuth,
) {
    private val http = httpClientFactory.default
    private val socketClient = httpClientFactory.create { install(WebSockets) }
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Id of Prisma's hidden controller device, stable while the app runs. */
    private val controllerId = randomHex(40)

    private val connectionLock = Mutex()
    private var connectionId: String? = null
    private var dealerJob: Job? = null

    private var devices: List<SpotifyDevice> = emptyList()
    private var playback: SpotifyPlayback? = null
    private var activeDeviceId: String? = null

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when Spotify pushes a change (another device started, paused, changed track...). */
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    suspend fun isAvailable(): Boolean = auth.accessToken() != null

    private suspend fun token(): String =
        auth.accessToken() ?: throw SpotifyConnectException(401, "not logged in")

    private suspend fun HttpRequestBuilder.authorize() {
        header(HttpHeaders.Authorization, "Bearer ${token()}")
    }

    private suspend fun HttpResponse.check(): HttpResponse {
        if (!status.isSuccess()) throw SpotifyConnectException(status.value, bodyAsText())
        return this
    }

    /** Opens the dealer socket if needed and returns its connection id. */
    private suspend fun connection(): String = connectionLock.withLock {
        connectionId?.takeIf { dealerJob?.isActive == true }?.let { return@withLock it }
        dealerJob?.cancel()
        val token = token()
        val ready = CompletableDeferred<String>()
        dealerJob = scope.launch {
            try {
                socketClient.webSocket("wss://dealer.spotify.com/?access_token=$token") {
                    val ping = launch {
                        while (isActive) {
                            delay(30_000)
                            send(Frame.Text("{\"type\":\"ping\"}"))
                        }
                    }
                    try {
                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            val message = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: continue
                            val id = (message["headers"] as? JsonObject)?.string("Spotify-Connection-Id")
                            if (id != null && !ready.isCompleted) {
                                ready.complete(id)
                            } else if (message.string("uri")?.contains("connect-state") == true) {
                                _changes.tryEmit(Unit)
                            }
                        }
                    } finally {
                        ping.cancel()
                    }
                }
                if (!ready.isCompleted) ready.completeExceptionally(SpotifyConnectException(0, "dealer closed"))
            } catch (e: Exception) {
                if (!ready.isCompleted) ready.completeExceptionally(e)
            } finally {
                connectionId = null
            }
        }
        val id = withTimeout(12_000) { ready.await() }
        connectionId = id
        // The web player registers itself before asking for the state; not being listed as a
        // playback device is fine, so a refusal here is not an error.
        runCatching { registerController(id) }
        id
    }

    private suspend fun registerController(connection: String) {
        http.post("$SPCLIENT/track-playback/v1/devices") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonObject("device") {
                    put("brand", "spotify")
                    putJsonObject("capabilities") {
                        put("change_volume", true)
                        put("enable_play_token", true)
                        put("supports_file_media_type", true)
                        put("play_token_lost_behavior", "pause")
                        put("disable_connect", true)
                        put("audio_podcasts", true)
                        put("video_playback", false)
                    }
                    put("device_id", controllerId)
                    put("device_type", "computer")
                    putJsonObject("metadata") {}
                    put("model", "web_player")
                    put("name", "Prisma")
                    put("platform_identifier", "web_player linux undefined;chrome 131.0.0.0;desktop")
                    put("is_group", false)
                }
                put("outro_endcontent_snooping", false)
                put("connection_id", connection)
                put("client_version", "harmony:4.43.2-a61ecaf5")
                put("volume", 65535)
            }.toString())
        }
    }

    /** Asks connect-state for the account's devices and what is playing; fills the caches. */
    private suspend fun refresh() {
        val connection = connection()
        val response = http.put("$SPCLIENT/connect-state/v1/devices/hobs_$controllerId") {
            authorize()
            header("X-Spotify-Connection-Id", connection)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("member_type", "CONNECT_STATE")
                putJsonObject("device") {
                    putJsonObject("device_info") {
                        putJsonObject("capabilities") {
                            put("can_be_player", false)
                            put("hidden", true)
                            put("needs_full_player_state", true)
                        }
                    }
                }
            }.toString())
        }
        if (!response.status.isSuccess()) {
            // A stale connection id is refused: drop it so the next call reconnects.
            dealerJob?.cancel()
            connectionId = null
            throw SpotifyConnectException(response.status.value, response.bodyAsText())
        }
        val cluster = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val active = cluster.string("active_device_id")?.takeIf { it.isNotBlank() }
        activeDeviceId = active

        devices = (cluster["devices"] as? JsonObject).orEmpty().mapNotNull { (id, value) ->
            val device = value as? JsonObject ?: return@mapNotNull null
            if (id == controllerId || device.boolean("can_play") == false) return@mapNotNull null
            SpotifyDevice(
                id = id,
                name = device.string("name").orEmpty(),
                type = device.string("device_type").orEmpty().lowercase().replaceFirstChar { it.uppercase() },
                isActive = id == active,
                volumePercent = device.long("volume")?.let { (it * 100 / 65535).toInt() },
            )
        }.sortedByDescending { it.isActive }

        val state = cluster["player_state"] as? JsonObject
        val track = state?.get("track") as? JsonObject
        val trackUri = track?.string("uri")
        playback = if (state == null || active == null || trackUri.isNullOrBlank()) null else {
            val metadata = track?.get("metadata") as? JsonObject
            val playing = state.boolean("is_playing") == true && state.boolean("is_paused") != true
            val basePosition = state.long("position_as_of_timestamp") ?: 0L
            val since = state.long("timestamp")
            val serverNow = cluster.long("server_timestamp_ms") ?: Clock.System.now().toEpochMilliseconds()
            val duration = state.long("duration") ?: 0L
            val position = if (playing && since != null) basePosition + (serverNow - since) else basePosition
            val image = (metadata?.string("image_large_url") ?: metadata?.string("image_url"))
                ?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
            SpotifyPlayback(
                device = devices.firstOrNull { it.id == active },
                isPlaying = playing,
                progressMs = if (duration > 0) position.coerceIn(0L, duration) else position.coerceAtLeast(0L),
                item = SpotifyPlaybackItem(
                    id = trackUri.substringAfterLast(':'),
                    uri = trackUri,
                    name = metadata?.string("title").orEmpty(),
                    durationMs = duration,
                    artists = listOfNotNull(metadata?.string("artist_name")?.let { SpotifyArtistRef(it) }),
                    album = image?.let { SpotifyAlbumRef(listOf(SpotifyImage("https://i.scdn.co/image/$it", 640))) },
                ),
            )
        }
    }

    suspend fun devices(): List<SpotifyDevice> {
        refresh()
        return devices
    }

    /** What the active device is playing, as of the last [devices] call. */
    fun playback(): SpotifyPlayback? = playback

    private suspend fun command(target: String, build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
        connection()
        http.post("$SPCLIENT/connect-state/v1/player/command/from/$controllerId/to/$target") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonObject("command") {
                    build()
                    putJsonObject("logging_params") { put("command_id", randomHex(32)) }
                }
            }.toString())
        }.check()
    }

    private fun activeDevice(): String =
        activeDeviceId ?: throw SpotifyConnectException(404, "no active device")

    /** Plays [trackUri] from [positionMs] on [deviceId]. */
    suspend fun playOn(deviceId: String, trackUri: String?, positionMs: Long) {
        if (trackUri == null) return transfer(deviceId)
        command(deviceId) {
            putJsonObject("context") {
                put("uri", trackUri)
                put("url", "context://$trackUri")
                putJsonObject("metadata") {}
            }
            putJsonObject("play_origin") {
                put("feature_identifier", "harmony")
                put("feature_version", "4.43.2-a61ecaf5")
            }
            putJsonObject("options") {
                put("license", "on-demand")
                putJsonObject("skip_to") { put("track_uri", trackUri) }
                put("seek_to", positionMs.coerceAtLeast(0L))
                putJsonObject("player_options_override") {}
            }
            put("endpoint", "play")
        }
        activeDeviceId = deviceId
    }

    /** Plays the list [trackUris] on [deviceId] starting at [startIndex]. */
    suspend fun playTracks(deviceId: String, trackUris: List<String>, startIndex: Int, positionMs: Long = 0L) {
        val index = startIndex.coerceIn(0, (trackUris.size - 1).coerceAtLeast(0))
        command(deviceId) {
            putJsonObject("context") {
                put("uri", "spotify:internal:prisma")
                put("url", "context://spotify:internal:prisma")
                putJsonObject("metadata") {}
                putJsonArray("pages") {
                    addJsonObject {
                        putJsonArray("tracks") {
                            trackUris.take(MAX_LIST).forEach { uri -> addJsonObject { put("uri", uri) } }
                        }
                    }
                }
            }
            putJsonObject("play_origin") {
                put("feature_identifier", "harmony")
                put("feature_version", "4.43.2-a61ecaf5")
            }
            putJsonObject("options") {
                put("license", "on-demand")
                putJsonObject("skip_to") {
                    put("track_index", index)
                    put("track_uri", trackUris[index])
                }
                put("seek_to", positionMs.coerceAtLeast(0L))
                putJsonObject("player_options_override") {}
            }
            put("endpoint", "play")
        }
        activeDeviceId = deviceId
    }

    /** Moves whatever Spotify is playing to [deviceId]. */
    suspend fun transfer(deviceId: String, play: Boolean = true) {
        connection()
        http.post("$SPCLIENT/connect-state/v1/connect/transfer/from/$controllerId/to/$deviceId") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonObject("transfer_options") { put("restore_paused", if (play) "resume" else "restore") }
            }.toString())
        }.check()
        activeDeviceId = deviceId
    }

    suspend fun resume() = command(activeDevice()) { put("endpoint", "resume") }
    suspend fun pause() = command(activeDevice()) { put("endpoint", "pause") }
    suspend fun next() = command(activeDevice()) { put("endpoint", "skip_next") }
    suspend fun previous() = command(activeDevice()) { put("endpoint", "skip_prev") }

    suspend fun setVolume(percent: Int) {
        val target = activeDevice()
        connection()
        http.put("$SPCLIENT/connect-state/v1/connect/volume/from/$controllerId/to/$target") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("volume", percent.coerceIn(0, 100) * 65535 / 100) }.toString())
        }.check()
    }

    suspend fun seek(positionMs: Long) = command(activeDevice()) {
        put("value", positionMs.coerceAtLeast(0L))
        put("endpoint", "seek_to")
    }

    /** Closes the dealer socket; the next call opens a new one. */
    fun disconnect() {
        dealerJob?.cancel()
        dealerJob = null
        connectionId = null
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    /** connect-state writes 64-bit numbers as strings. */
    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }

    private fun randomHex(length: Int): String {
        val chars = "0123456789abcdef"
        return buildString { repeat(length) { append(chars[Random.nextInt(chars.length)]) } }
    }

    private companion object {
        const val SPCLIENT = "https://spclient.wg.spotify.com"
        const val MAX_LIST = 500
    }
}
