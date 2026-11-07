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
package org.spotube.spotube.modules.recap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.paths.Paths
import kotlin.time.Clock

/** One listen: what played, when it started and how long it was actually heard. */
@Serializable
data class PlayEvent(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val cover: String? = null,
    val at: Long,
    val ms: Long,
)

/**
 * Local listening log behind the yearly recap. Watches the player and, whenever the track
 * changes, appends the previous one to a JSON-lines file in the app's data folder if it was
 * heard for at least [MIN_LISTEN_MS]. Nothing leaves the device.
 */
class ListeningHistoryRepository(
    paths: Paths,
    audioPlayerQueue: AudioPlayerQueue,
    audioPlayer: AudioPlayerInterface,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val file = "${paths.getApplicationDataDirPath()}/tubify_listening_history.jsonl".toPath()
    private val fileMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private class Pending(val key: String, val event: PlayEvent, var heardMs: Long = 0)

    init {
        scope.launch {
            var pending: Pending? = null
            combine(audioPlayerQueue.currentQueueEntryFlow, audioPlayer.positionFlow) { entry, position ->
                entry to position.inWholeMilliseconds
            }.collect { (entry, positionMs) ->
                val key = entry?.url
                if (pending?.key != key) {
                    pending?.let { flush(it) }
                    pending = entry?.toPending(key!!)
                }
                pending?.let { if (positionMs > it.heardMs) it.heardMs = positionMs }
            }
        }
    }

    private fun QueueEntry.toPending(key: String): Pending? {
        val now = Clock.System.now().toEpochMilliseconds()
        val event = when (this) {
            is QueueEntry.StreamingTrack -> PlayEvent(
                id = track.id,
                title = track.title,
                artists = track.artists.map { it.name },
                album = track.album?.title,
                cover = artworkUrl,
                at = now,
                ms = 0,
            )
            is QueueEntry.LocalTrack -> PlayEvent(
                id = url,
                title = name,
                artists = artists,
                album = album,
                cover = null,
                at = now,
                ms = 0,
            )
            else -> return null
        }
        return Pending(key, event)
    }

    private suspend fun flush(pending: Pending) {
        if (pending.heardMs < MIN_LISTEN_MS) return
        val line = json.encodeToString(PlayEvent.serializer(), pending.event.copy(ms = pending.heardMs))
        fileMutex.withLock {
            runCatching {
                FileSystem.SYSTEM.appendingSink(file, mustExist = false).buffer().use { it.writeUtf8(line + "\n") }
            }
        }
    }

    /** Every recorded listen, oldest first. */
    suspend fun events(): List<PlayEvent> = withContext(Dispatchers.Default) {
        fileMutex.withLock {
            runCatching {
                if (!FileSystem.SYSTEM.exists(file)) return@runCatching emptyList()
                FileSystem.SYSTEM.read(file) {
                    generateSequence { readUtf8Line() }
                        .mapNotNull { line -> runCatching { json.decodeFromString(PlayEvent.serializer(), line) }.getOrNull() }
                        .toList()
                }
            }.getOrDefault(emptyList())
        }
    }

    companion object {
        const val MIN_LISTEN_MS = 30_000L
    }
}
