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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class RecapTrack(val title: String, val artists: String, val cover: String?, val plays: Int)
data class RecapArtist(val name: String, val cover: String?, val minutes: Long)

sealed interface RecapUiState {
    data object Loading : RecapUiState
    data class Empty(val year: Int) : RecapUiState
    data class Ready(
        val year: Int,
        val totalMinutes: Long,
        val distinctTracks: Int,
        val distinctArtists: Int,
        val topTracks: List<RecapTrack>,
        val topArtists: List<RecapArtist>,
        /** 1..12 */
        val topMonth: Int,
        val topMonthMinutes: Long,
    ) : RecapUiState
}

/** Builds this year's recap from the local listening log. */
class RecapViewModel(private val history: ListeningHistoryRepository) : ViewModel() {
    private val state = MutableStateFlow<RecapUiState>(RecapUiState.Loading)
    val uiState: StateFlow<RecapUiState> = state.asStateFlow()

    init {
        viewModelScope.launch { state.value = compute() }
    }

    private suspend fun compute(): RecapUiState = withContext(Dispatchers.Default) {
        val zone = TimeZone.currentSystemDefault()
        val year = Clock.System.now().toLocalDateTime(zone).year
        val events = history.events().filter {
            Instant.fromEpochMilliseconds(it.at).toLocalDateTime(zone).year == year
        }
        if (events.isEmpty()) return@withContext RecapUiState.Empty(year)

        val topTracks = events.groupBy { it.id }
            .map { (_, plays) ->
                val first = plays.last()
                RecapTrack(first.title, first.artists.joinToString(", "), first.cover, plays.size)
            }
            .sortedByDescending { it.plays }
            .take(5)

        val artistMinutes = mutableMapOf<String, Long>()
        val artistCover = mutableMapOf<String, String?>()
        events.forEach { event ->
            event.artists.forEach { name ->
                artistMinutes[name] = (artistMinutes[name] ?: 0) + event.ms
                if (artistCover[name] == null) artistCover[name] = event.cover
            }
        }
        val topArtists = artistMinutes.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { RecapArtist(it.key, artistCover[it.key], it.value / 60_000) }

        val byMonth = events.groupBy { Instant.fromEpochMilliseconds(it.at).toLocalDateTime(zone).month.ordinal + 1 }
            .mapValues { (_, list) -> list.sumOf { it.ms } }
        val topMonth = byMonth.maxBy { it.value }

        RecapUiState.Ready(
            year = year,
            totalMinutes = events.sumOf { it.ms } / 60_000,
            distinctTracks = events.map { it.id }.distinct().size,
            distinctArtists = artistMinutes.size,
            topTracks = topTracks,
            topArtists = topArtists,
            topMonth = topMonth.key,
            topMonthMinutes = topMonth.value / 60_000,
        )
    }
}
