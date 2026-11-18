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

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.spotube.plugin_interfaces.host_apis.WebSocketAPI
import org.spotube.plugin_interfaces.host_apis.WebSocketEvent
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.network.HttpClientFactory

/**
 * One websocket at a time per plugin instance, reusing the app's shared Ktor engine. See
 * [WebSocketAPI] for why a plugin would need this instead of [org.spotube.plugin_interfaces.host_apis.HttpClientAPI].
 */
class RealWebSocketAPI(
    private val scope: CoroutineScope,
    httpClientFactory: HttpClientFactory
) : WebSocketAPI, KoinComponent {
    private val logger by injectLogger<RealWebSocketAPI>()
    private val client = httpClientFactory.create { install(WebSockets) }

    private val lock = Mutex()
    private var job: Job? = null
    private var sendFrame: (suspend (Frame) -> Unit)? = null

    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 32)

    override fun events(): Flow<WebSocketEvent> = _events.asSharedFlow()

    override suspend fun connect(url: String, headers: Map<String, String>?) = lock.withLock {
        job?.cancel()
        sendFrame = null
        val opened = CompletableDeferred<Unit>()
        job = scope.launch {
            try {
                client.webSocket(urlString = url, request = {
                    headers?.forEach { (key, value) -> this.headers.append(key, value) }
                }) {
                    sendFrame = { frame -> outgoing.send(frame) }
                    if (!opened.isCompleted) opened.complete(Unit)
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                _events.tryEmit(WebSocketEvent.Text(frame.readText()))
                            }
                        }
                        val reason = closeReason.await()
                        _events.tryEmit(
                            WebSocketEvent.Closed(reason?.code?.toInt() ?: 1000, reason?.message.orEmpty())
                        )
                    } finally {
                        sendFrame = null
                    }
                }
            } catch (e: Exception) {
                if (!opened.isCompleted) {
                    opened.completeExceptionally(e)
                } else {
                    logger.w(e) { "Websocket to $url failed" }
                    _events.tryEmit(WebSocketEvent.Failed(e.message ?: e.toString()))
                }
            }
        }
        opened.await()
    }

    override suspend fun send(text: String) {
        val send = sendFrame ?: throw IllegalStateException("WebSocketAPI: not connected")
        send(Frame.Text(text))
    }

    override suspend fun close(code: Int, reason: String) {
        runCatching { sendFrame?.invoke(Frame.Close(CloseReason(code.toShort(), reason))) }
        job?.cancel()
        job = null
        sendFrame = null
    }
}
