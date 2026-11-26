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

package org.spotube.spotube.core.zipline.host_apis

import org.koin.core.component.inject
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod
import org.spotube.plugin_interfaces.host_apis.HttpResponse
import org.spotube.spotube.core.di.injectLogger
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import org.koin.core.component.KoinComponent
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Header convention (not part of the shared [HttpClientAPI] contract): a plugin that needs to
 * send or receive raw bytes over this text-only bridge (protobuf, for instance) sets this
 * request header to "base64". The request [HttpResponse.body] is then treated as Base64 of the
 * actual bytes to send, and the response body is returned as Base64 of the actual bytes received,
 * with the same header echoed back on the response headers so the caller knows to decode it.
 */
private const val BINARY_BODY_HEADER = "X-Zipline-Binary-Body"
private const val BINARY_BODY_VALUE = "base64"

class RealHttpClientAPI : HttpClientAPI, KoinComponent {
    val logger by injectLogger<RealHttpClientAPI>()
    private val httpClientFactory: HttpClientFactory by inject()
    private val httpClient get() = httpClientFactory.default

    override suspend fun request(
        method: HttpMethod,
        url: String,
        requestHeaders: Map<String, String>?,
        body: String?
    ): HttpResponse {
        // Spotify's token for the plugin is asked in "transport" mode; the web player's own
        // ("init") gives the full account session. Tried first, the original request is the fallback.
        if (url.contains("open.spotify.com/api/token") && url.contains("reason=transport")) {
            val init = runCatching {
                send(method, url.replace("reason=transport", "reason=init"), requestHeaders, body)
            }.getOrNull()
            if (init != null && init.statusCode == 200 && init.body?.contains("accessToken") == true) {
                SpotifyWebClientHints.tokenMode = "init"
                return init
            }
            SpotifyWebClientHints.tokenMode = "transport (init non riuscito)"
        }
        return send(method, url, requestHeaders, body)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun send(
        method: HttpMethod,
        url: String,
        requestHeaders: Map<String, String>?,
        body: String?
    ): HttpResponse {
        // Binary convention: see BINARY_BODY_HEADER doc above. Not applied to the Spotify-token
        // special-casing path, which is always text (JSON).
        val isBinary = requestHeaders?.entries
            ?.any { (k, v) -> k.equals(BINARY_BODY_HEADER, ignoreCase = true) && v.equals(BINARY_BODY_VALUE, ignoreCase = true) } == true
        val sendHeaders = requestHeaders?.filterKeys { !it.equals(BINARY_BODY_HEADER, ignoreCase = true) }

        val (adaptedHeaders, adaptedBody) = if (isBinary) sendHeaders to body
            else SpotifyWebClientHints.adapt(url, sendHeaders, body)

        logger.i {
            buildString {
                append("[${method.name.uppercase()}] $url\n")
                append("Headers:\n")
                adaptedHeaders?.forEach { (key, value) ->
                    append("  $key: $value\n")
                }
                if (!isBinary) adaptedBody?.let { append("Body: $it\n") }
            }
        }
        val res = httpClient.request {
            this.method = when (method) {
                HttpMethod.Get -> io.ktor.http.HttpMethod.Get
                HttpMethod.Post -> io.ktor.http.HttpMethod.Post
                HttpMethod.Put -> io.ktor.http.HttpMethod.Put
                HttpMethod.Delete -> io.ktor.http.HttpMethod.Delete
                HttpMethod.Patch -> io.ktor.http.HttpMethod.Patch
                HttpMethod.Head -> io.ktor.http.HttpMethod.Head
                HttpMethod.Options -> io.ktor.http.HttpMethod.Options
            }
            this.url {
                takeFrom(url)
            }
            adaptedHeaders?.forEach { (key, value) ->
                if (!key.equals(HttpHeaders.ContentType, ignoreCase = true) || !isBinary) {
                    headers.append(key, value)
                }
            }
            if (isBinary) {
                val bytes = adaptedBody?.let { Base64.decode(it) } ?: ByteArray(0)
                val contentType = adaptedHeaders?.entries
                    ?.firstOrNull { it.key.equals(HttpHeaders.ContentType, ignoreCase = true) }
                    ?.value?.let { ContentType.parse(it) } ?: ContentType.Application.OctetStream
                setBody(ByteArrayContent(bytes, contentType))
            } else {
                adaptedBody?.let { setBody(it) }
            }
        }

        return if (isBinary) {
            val responseBytes = res.bodyAsBytes()
            HttpResponse(
                statusCode = res.status.value,
                headers = res.headers.entries().associate { it.key to it.value.joinToString(",") } +
                    (BINARY_BODY_HEADER to BINARY_BODY_VALUE),
                body = Base64.encode(responseBytes)
            )
        } else {
            val responseText = res.bodyAsText()
            SpotifyWebClientHints.inspect(url, adaptedBody, responseText)
            HttpResponse(
                statusCode = res.status.value,
                headers = res.headers.entries().associate { it.key to it.value.joinToString(",") },
                body = responseText
            )
        }
    }
}