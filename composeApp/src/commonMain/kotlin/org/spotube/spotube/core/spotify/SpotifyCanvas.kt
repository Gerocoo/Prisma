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
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.network.HttpClientFactory
import kotlin.time.Clock

/**
 * Spotify Canvas: the short looping video Spotify shows behind a track.
 *
 * Works only with the Spotify metadata plugin: track ids are Spotify ids, and the access token
 * the plugin keeps refreshed in its storage ("<plugin id>:credentials") is reused for the
 * canvas request. Uses Spotify's unofficial canvaz endpoint (protobuf), so it may stop working
 * if Spotify changes it; every failure simply means "no canvas" and the cover stays.
 */
class SpotifyCanvasRepository(
    httpClientFactory: HttpClientFactory,
    private val auth: SpotifyPluginAuth,
) {
    private val logger = Logger.withTag("SpotifyCanvas")
    private val http = httpClientFactory.default
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, String?>()
    private val mutex = Mutex()

    private suspend fun accessToken(): String? = auth.accessToken()

    /** Video URL of the canvas for a Spotify track id, or null when there is none. */
    suspend fun canvasUrl(trackId: String): String? {
        if (!SPOTIFY_ID.matches(trackId)) return null
        mutex.withLock { if (cache.containsKey(trackId)) return cache[trackId] }
        val token = accessToken() ?: return null
        val url = runCatching {
            val response = http.post(CANVAZ_URL) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/protobuf")
                setBody(ByteArrayContent(requestBody("spotify:track:$trackId"), ContentType("application", "x-protobuf")))
            }
            if (!response.status.isSuccess()) null else parseCanvasUrl(response.bodyAsBytes())
        }.onFailure { logger.w(it) { "Canvas request failed" } }.getOrNull()
        mutex.withLock { cache[trackId] = url }
        return url
    }

    private companion object {
        const val CANVAZ_URL = "https://spclient.wg.spotify.com/canvaz-cache/v0/canvases"
        val SPOTIFY_ID = Regex("^[0-9A-Za-z]{22}$")

        // EntityCanvazRequest { repeated Entity entities = 1; }  Entity { string entity_uri = 1; }
        fun requestBody(uri: String): ByteArray {
            val entity = lengthDelimited(1, uri.encodeToByteArray())
            return lengthDelimited(1, entity)
        }

        fun lengthDelimited(field: Int, bytes: ByteArray): ByteArray =
            byteArrayOf((field shl 3 or 2).toByte()) + varint(bytes.size.toLong()) + bytes

        fun varint(value: Long): ByteArray {
            val out = mutableListOf<Byte>()
            var v = value
            while (true) {
                if (v and 0x7FL.inv() == 0L) { out += v.toByte(); return out.toByteArray() }
                out += ((v and 0x7F) or 0x80).toByte()
                v = v ushr 7
            }
        }

        // EntityCanvazResponse { repeated Canvaz canvases = 1; }  Canvaz { string url = 2; Type type = 4; }
        fun parseCanvasUrl(bytes: ByteArray): String? {
            for (canvas in readFields(bytes).filter { it.first == 1 }.mapNotNull { it.second as? ByteArray }) {
                val fields = readFields(canvas)
                val url = (fields.firstOrNull { it.first == 2 }?.second as? ByteArray)?.decodeToString()
                val type = fields.firstOrNull { it.first == 4 }?.second as? Long ?: 0L
                if (url != null && type != 0L && url.endsWith(".mp4")) return url
            }
            return null
        }

        /** Minimal protobuf reader: (field number, Long for varints | ByteArray for bytes). */
        fun readFields(bytes: ByteArray): List<Pair<Int, Any>> {
            val out = mutableListOf<Pair<Int, Any>>()
            var i = 0
            fun readVarint(): Long {
                var shift = 0; var result = 0L
                while (i < bytes.size) {
                    val b = bytes[i++].toInt() and 0xFF
                    result = result or ((b and 0x7F).toLong() shl shift)
                    if (b and 0x80 == 0) break
                    shift += 7
                }
                return result
            }
            while (i < bytes.size) {
                val key = readVarint()
                val field = (key ushr 3).toInt()
                when ((key and 7).toInt()) {
                    0 -> out += field to readVarint()
                    1 -> i += 8
                    2 -> {
                        val len = readVarint().toInt()
                        if (len < 0 || i + len > bytes.size) return out
                        out += field to bytes.copyOfRange(i, i + len)
                        i += len
                    }
                    5 -> i += 4
                    else -> return out
                }
            }
            return out
        }
    }
}

/** Canvas video URL for the playing entry, or null (no Spotify plugin, no canvas, disabled). */
@Composable
fun rememberSpotifyCanvasUrl(entry: QueueEntry?, enabled: Boolean): String? {
    val repository = koinInject<SpotifyCanvasRepository>()
    val trackId = (entry as? QueueEntry.StreamingTrack)?.track?.id
    val url by produceState<String?>(null, trackId, enabled) {
        value = null
        if (enabled && trackId != null) value = repository.canvasUrl(trackId)
    }
    return url
}

/**
 * Plays a canvas muted and looping, cropped to fill [modifier]. Calls [onFirstFrame] once the
 * video is actually showing, so the cover can stay visible until then. Android only; elsewhere
 * nothing is drawn and the cover stays.
 */
@Composable
expect fun CanvasVideo(url: String, modifier: Modifier = Modifier, onFirstFrame: () -> Unit = {})
