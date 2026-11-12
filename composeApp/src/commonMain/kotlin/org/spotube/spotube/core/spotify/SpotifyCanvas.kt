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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import kotlin.math.roundToInt
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

    /** Canvas already looked up for [trackId]: known at once, with no request. */
    fun cachedCanvasUrl(trackId: String?): String? = trackId?.let { cache[it] }

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
    val queue = koinInject<AudioPlayerQueue>()
    val trackId = (entry as? QueueEntry.StreamingTrack)?.track?.id
    // A canvas looked up in advance is there from the first frame: no cover flash before it.
    val url by produceState(
        if (enabled) repository.cachedCanvasUrl(trackId) else null, trackId, enabled
    ) {
        value = if (enabled) repository.cachedCanvasUrl(trackId) else null
        if (enabled && trackId != null) value = repository.canvasUrl(trackId)
    }
    // Looks up the canvases of the next tracks while this one plays.
    LaunchedEffect(trackId, enabled) {
        if (!enabled || trackId == null) return@LaunchedEffect
        val entries = queue.queueFlow.value
        val index = entries.indexOfFirst { (it as? QueueEntry.StreamingTrack)?.track?.id == trackId }
        if (index < 0) return@LaunchedEffect
        entries.drop(index + 1).take(2).forEach { next ->
            (next as? QueueEntry.StreamingTrack)?.track?.id?.let { repository.canvasUrl(it) }
        }
    }
    return url
}

/**
 * Plays a canvas muted and looping, cropped to fill [modifier]. Calls [onFirstFrame] once the
 * video is actually showing. [onBackdrop] receives a tiny copy of the current frame a few times
 * a second: stretched back to full size it is the blurred picture drawn behind the controls.
 * Android only; elsewhere nothing is drawn and the cover stays.
 */
@Composable
expect fun CanvasVideo(
    url: String,
    modifier: Modifier = Modifier,
    onFirstFrame: () -> Unit = {},
    onBackdrop: (ImageBitmap) -> Unit = {},
)

/** The blurred copy of the canvas video shared with the controls drawn over it. */
@Stable
class CanvasBackdrop {
    var image by mutableStateOf<ImageBitmap?>(null)
    var container by mutableStateOf<LayoutCoordinates?>(null)

    /** 0 = no canvas (nothing is drawn), 1 = canvas fully shown. */
    var amount by mutableFloatStateOf(0f)
}

val LocalCanvasBackdrop = staticCompositionLocalOf<CanvasBackdrop?> { null }

/**
 * Frosted-glass fill for a control drawn over a canvas video: paints the part of the blurred
 * video that lies behind the control, then a dark veil so icons and text stay readable. Put it
 * after `clip(...)` and before the control's own `background(...)`. Does nothing without a canvas.
 */
@Composable
fun Modifier.canvasBlur(): Modifier {
    val backdrop = LocalCanvasBackdrop.current ?: return this
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { coordinates ->
            val container = backdrop.container
            if (container != null && container.isAttached && coordinates.isAttached) {
                origin = container.localPositionOf(coordinates, Offset.Zero)
            }
        }
        .drawBehind {
            val image = backdrop.image ?: return@drawBehind
            val amount = backdrop.amount
            val container = backdrop.container
            if (amount <= 0f || container == null || !container.isAttached) return@drawBehind
            val containerWidth = container.size.width.toFloat()
            val containerHeight = container.size.height.toFloat()
            // The video is cropped to fill the screen: place the small frame the same way.
            val scale = maxOf(containerWidth / image.width, containerHeight / image.height)
            val width = image.width * scale
            val height = image.height * scale
            drawImage(
                image = image,
                dstOffset = IntOffset(
                    ((containerWidth - width) / 2f - origin.x).roundToInt(),
                    ((containerHeight - height) / 2f - origin.y).roundToInt()
                ),
                dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                alpha = amount,
                filterQuality = FilterQuality.High
            )
            drawRect(Color.Black.copy(alpha = 0.34f * amount))
        }
}
