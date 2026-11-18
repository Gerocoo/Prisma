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

package org.spotube.spotify_connect_plugin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod
import org.spotube.plugin_interfaces.host_apis.SpotifyAuthAPI
import org.spotube.plugin_interfaces.host_apis.WebSocketAPI
import org.spotube.plugin_interfaces.host_apis.WebSocketEvent
import org.spotube.plugin_interfaces.plugin_apis.device_control.DeviceControlAPI
import org.spotube.plugin_interfaces.plugin_apis.device_control.DeviceControlException
import org.spotube.plugin_interfaces.plugin_apis.device_control.RemoteDevice
import org.spotube.plugin_interfaces.plugin_apis.device_control.RemotePlaybackItem
import org.spotube.plugin_interfaces.plugin_apis.device_control.RemotePlaybackState
import kotlin.random.Random

/**
 * Spotify Connect the way Spotify's own web player does it, using the Spotify metadata plugin's
 * login (via [SpotifyAuthAPI]) and the host's [HttpClientAPI]/[WebSocketAPI] instead of talking
 * to the network directly - this code runs inside the plugin sandbox.
 *
 * Ported 1:1 from the app's own (now removed) native SpotifyConnectApi: a "dealer" WebSocket
 * gives a connection id, a hidden controller device registers with it, and the connect-state
 * service then returns every device of the account with what it is playing, and accepts
 * commands for them.
 */
class RealDeviceControlAPI(
    private val http: HttpClientAPI,
    private val socket: WebSocketAPI,
    private val auth: SpotifyAuthAPI,
) : DeviceControlAPI {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Id of this plugin's hidden controller device, stable while the plugin runs. */
    private val controllerId = randomHex(40)

    private val connectionLock = Mutex()
    private var connectionId: String? = null
    private var dealerJob: Job? = null

    private var devices: List<RemoteDevice> = emptyList()
    private var playback: RemotePlaybackState? = null
    private var activeDeviceId: String? = null

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun isAvailable(): Boolean = auth.accessToken() != null

    override fun changes(): Flow<Unit> = _changes.asSharedFlow()

    private suspend fun token(): String =
        auth.accessToken() ?: throw DeviceControlException(401, "not logged in")

    private suspend fun authHeaders(extra: Map<String, String> = emptyMap()): Map<String, String> =
        mapOf(
            "Authorization" to "Bearer ${token()}",
            "Content-Type" to "application/json",
        ) + extra

    private fun checkOk(statusCode: Int, body: String?) {
        if (statusCode !in 200..299) throw DeviceControlException(statusCode, body.orEmpty())
    }

    /** Opens the dealer socket if needed and returns its connection id. */
    private suspend fun connection(): String = connectionLock.withLock {
        connectionId?.takeIf { dealerJob?.isActive == true }?.let { return@withLock it }
        dealerJob?.cancel()
        val token = token()
        val ready = CompletableDeferred<String>()
        dealerJob = scope.launch {
            try {
                socket.connect("wss://dealer.spotify.com/?access_token=$token")
                val ping = launch {
                    while (isActive) {
                        delay(30_000)
                        runCatching { socket.send("{\"type\":\"ping\"}") }
                    }
                }
                try {
                    socket.events().collect { event ->
                        when (event) {
                            is WebSocketEvent.Text -> {
                                val message = runCatching {
                                    json.parseToJsonElement(event.text).jsonObject
                                }.getOrNull() ?: return@collect
                                val id = (message["headers"] as? JsonObject)?.string("Spotify-Connection-Id")
                                if (id != null && !ready.isCompleted) {
                                    ready.complete(id)
                                } else if (message.string("uri")?.contains("connect-state") == true) {
                                    _changes.tryEmit(Unit)
                                }
                            }

                            is WebSocketEvent.Closed, is WebSocketEvent.Failed -> {
                                if (!ready.isCompleted) {
                                    ready.completeExceptionally(DeviceControlException(0, "dealer closed"))
                                }
                                connectionId = null
                            }
                        }
                    }
                } finally {
                    ping.cancel()
                }
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
        val body = buildJsonObject {
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
        }.toString()
        http.request(HttpMethod.Post, "$SPCLIENT/track-playback/v1/devices", authHeaders(), body)
    }

    /** Asks connect-state for the account's devices and what is playing; fills the caches. */
    private suspend fun refresh() {
        val connection = connection()
        val body = buildJsonObject {
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
        }.toString()
        val response = http.request(
            HttpMethod.Put,
            "$SPCLIENT/connect-state/v1/devices/hobs_$controllerId",
            authHeaders(mapOf("X-Spotify-Connection-Id" to connection)),
            body,
        )
        if (response.statusCode !in 200..299) {
            // A stale connection id is refused: drop it so the next call reconnects.
            dealerJob?.cancel()
            connectionId = null
            throw DeviceControlException(response.statusCode, response.body.orEmpty())
        }
        val cluster = json.parseToJsonElement(response.body.orEmpty()).jsonObject
        val active = cluster.string("active_device_id")?.takeIf { it.isNotBlank() }
        activeDeviceId = active

        devices = (cluster["devices"] as? JsonObject).orEmpty().mapNotNull { (id, value) ->
            val device = value as? JsonObject ?: return@mapNotNull null
            if (id == controllerId || device.boolean("can_play") == false) return@mapNotNull null
            RemoteDevice(
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
            val metadata = track.get("metadata") as? JsonObject
            val playing = state.boolean("is_playing") == true && state.boolean("is_paused") != true
            val basePosition = state.long("position_as_of_timestamp") ?: 0L
            val since = state.long("timestamp")
            val serverNow = cluster.long("server_timestamp_ms") ?: 0L
            val duration = state.long("duration") ?: 0L
            val position = if (playing && since != null) basePosition + (serverNow - since) else basePosition
            val image = (metadata?.string("image_large_url") ?: metadata?.string("image_url"))
                ?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
            RemotePlaybackState(
                device = devices.firstOrNull { it.id == active },
                isPlaying = playing,
                positionMs = if (duration > 0) position.coerceIn(0L, duration) else position.coerceAtLeast(0L),
                item = RemotePlaybackItem(
                    id = trackUri.substringAfterLast(':'),
                    uri = trackUri,
                    title = metadata?.string("title").orEmpty(),
                    durationMs = duration,
                    artistNames = listOfNotNull(metadata?.string("artist_name")),
                    artworkUrl = image?.let { "https://i.scdn.co/image/$it" },
                ),
            )
        }
    }

    override suspend fun devices(): List<RemoteDevice> {
        refresh()
        return devices
    }

    override suspend fun currentPlayback(): RemotePlaybackState? = playback

    private suspend fun command(target: String, build: JsonObjectBuilder.() -> Unit) {
        connection()
        val body = buildJsonObject {
            putJsonObject("command") {
                build()
                putJsonObject("logging_params") { put("command_id", randomHex(32)) }
            }
        }.toString()
        val response = http.request(
            HttpMethod.Post,
            "$SPCLIENT/connect-state/v1/player/command/from/$controllerId/to/$target",
            authHeaders(),
            body,
        )
        checkOk(response.statusCode, response.body)
    }

    private fun activeDevice(): String =
        activeDeviceId ?: throw DeviceControlException(404, "no active device")

    override suspend fun playOn(deviceId: String, trackUri: String?, positionMs: Long) {
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

    override suspend fun playTracks(deviceId: String, trackUris: List<String>, startIndex: Int, positionMs: Long) {
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

    override suspend fun transfer(deviceId: String, play: Boolean) {
        connection()
        val body = buildJsonObject {
            putJsonObject("transfer_options") { put("restore_paused", if (play) "resume" else "restore") }
        }.toString()
        val response = http.request(
            HttpMethod.Post,
            "$SPCLIENT/connect-state/v1/connect/transfer/from/$controllerId/to/$deviceId",
            authHeaders(),
            body,
        )
        checkOk(response.statusCode, response.body)
        activeDeviceId = deviceId
    }

    override suspend fun resume() = command(activeDevice()) { put("endpoint", "resume") }
    override suspend fun pause() = command(activeDevice()) { put("endpoint", "pause") }
    override suspend fun next() = command(activeDevice()) { put("endpoint", "skip_next") }
    override suspend fun previous() = command(activeDevice()) { put("endpoint", "skip_prev") }

    override suspend fun setVolume(percent: Int) {
        val target = activeDevice()
        connection()
        val body = buildJsonObject { put("volume", percent.coerceIn(0, 100) * 65535 / 100) }.toString()
        val response = http.request(
            HttpMethod.Put,
            "$SPCLIENT/connect-state/v1/connect/volume/from/$controllerId/to/$target",
            authHeaders(),
            body,
        )
        checkOk(response.statusCode, response.body)
    }

    override suspend fun seek(positionMs: Long) = command(activeDevice()) {
        put("value", positionMs.coerceAtLeast(0L))
        put("endpoint", "seek_to")
    }

    override fun disconnect() {
        dealerJob?.cancel()
        dealerJob = null
        connectionId = null
        scope.launch { runCatching { socket.close() } }
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
