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

package org.spotube.plugin_interfaces.host_apis

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

const val WebSocketAPI_SERVICE_NAME = "WebSocketAPI"

@Serializable
sealed class WebSocketEvent {
    @Serializable
    data class Text(val text: String) : WebSocketEvent()

    /** The host closed the connection, locally or because the server closed it. */
    @Serializable
    data class Closed(val code: Int, val reason: String) : WebSocketEvent()

    /** The connection broke outside a normal close (network drop, handshake failure once open...). */
    @Serializable
    data class Failed(val message: String) : WebSocketEvent()
}

/**
 * A single websocket connection owned by the host, driven and observed by the plugin.
 *
 * `HttpClientAPI` only carries plain request/response calls; some real-time protocols (Spotify
 * Connect's "dealer" socket, for instance) need a persistent socket instead. One connection is
 * live per plugin at a time: calling [connect] again while one is open replaces it.
 */
interface WebSocketAPI : ZiplineService {
    /** Opens the socket to [url]; throws if the handshake fails. */
    suspend fun connect(url: String, headers: Map<String, String>? = null)

    suspend fun send(text: String)

    suspend fun close(code: Int = 1000, reason: String = "")

    /** Incoming frames, closes and failures for whichever connection is currently open. */
    fun events(): Flow<WebSocketEvent>
}
