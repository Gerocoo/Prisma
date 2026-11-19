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

package org.spotube.spotube.modules.lyrics

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistOverview
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.translation.TranslationService
import org.spotube.spotube.modules.artist.ArtistRepository
import org.spotube.spotube.modules.plugin.PluginManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
enum class LyricType {
    STATIC,
    SYNCED
}

data class LyricsUiState(
    val mode: LyricType = LyricType.SYNCED,
    val syncedLyrics: List<LyricsLine>? = null,
    val plainLyrics: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = -1,
    val trackTitle: String = "",
    val trackArtists: String = "",
    val coverUrl: String = "",
    /** Full metadata for the "info brano" tab (album, duration, popularity...). */
    val track: MetadataTrack? = null,
    val artistOverview: MetadataArtistOverview? = null,
    val artistOverviewLoading: Boolean = false,
    val relatedTracks: List<MetadataTrack>? = null,
    val relatedTracksLoading: Boolean = false,
    // "Traduci" pill (artist bio): only shown once we've confirmed the bio's language
    // differs from the device/app language.
    val bioTranslationAvailable: Boolean = false,
    val bioTranslating: Boolean = false,
    val translatedBio: String? = null,
    val showTranslatedBio: Boolean = false,
    // "Traduci" pill (lyrics, synced or plain): translated text keeps each LyricsLine's
    // original `time` so the synced view stays scrollable/clickable ("dinamico").
    val lyricsTranslationAvailable: Boolean = false,
    val lyricsTranslating: Boolean = false,
    val translatedPlainLyrics: String? = null,
    val translatedSyncedLyrics: List<LyricsLine>? = null,
    val showTranslatedLyrics: Boolean = false)

class LyricsViewModel(
    private val pluginManager: PluginManager,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val artistRepository: ArtistRepository,
    private val translationService: TranslationService) : ViewModel() {

    /** BCP-47-ish 2-letter tag of the app/device language, e.g. "it", "en". */
    private val targetLanguageTag: String
        get() = Locale.current.language

    private val _uiState = MutableStateFlow(LyricsUiState())

    // Only re-emits when the active line changes, not on every position tick.
    private val currentIndexFlow = combine(
        _uiState.map { it.syncedLyrics }.distinctUntilChanged(),
        audioPlayer.positionFlow
    ) { lyrics, position -> activeLineIndex(lyrics, position.inWholeMilliseconds) }
        .distinctUntilChanged()

    val uiState: StateFlow<LyricsUiState> = combine(_uiState, currentIndexFlow) { state, index ->
        state.copy(currentIndex = index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsUiState())

    private var currentTrackId: String? = null

    fun setMode(mode: LyricType) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun loadLyrics(forceReload: Boolean = false) {
        viewModelScope.launch {
            val entry = audioPlayerQueue.currentQueueEntryFlow.value ?: return@launch
            val track = when (entry) {
                is QueueEntry.StreamingTrack -> entry.track
                is QueueEntry.LocalTrack -> return@launch
            }

            if (!forceReload && currentTrackId == track.id && _uiState.value.syncedLyrics != null) {
                return@launch
            }

            currentTrackId = track.id
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    bioTranslationAvailable = false,
                    translatedBio = null,
                    showTranslatedBio = false,
                    lyricsTranslationAvailable = false,
                    translatedPlainLyrics = null,
                    translatedSyncedLyrics = null,
                    showTranslatedLyrics = false)
            }
            try {
                pluginManager.selectedLyricsPlugin.value?.let { pluginService ->
                    pluginService.use {
                       val lyrics = lyricsAPI.getLyrics(track)

                        _uiState.update {
                            it.copy(
                                syncedLyrics = lyrics?.syncedLyrics,
                                plainLyrics = lyrics?.plainLyrics,
                                isLoading = false)
                        }
                        checkLyricsTranslationAvailable(lyrics?.plainLyrics, lyrics?.syncedLyrics)
                    }
                } ?: run {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Detects whether the lyrics' language differs from [targetLanguageTag] to show the pill. */
    private suspend fun checkLyricsTranslationAvailable(
        plainLyrics: String?,
        syncedLyrics: List<LyricsLine>?) {
        val sample = plainLyrics?.takeIf { it.isNotBlank() }
            ?: syncedLyrics?.joinToString(" ") { it.text }?.takeIf { it.isNotBlank() }
            ?: return
        val detected = translationService.detectLanguage(sample) ?: return
        val available = !detected.startsWith(targetLanguageTag, ignoreCase = true)
        _uiState.update { it.copy(lyricsTranslationAvailable = available) }
    }

    /** Toggles between the original and translated lyrics (plain block or line-by-line). */
    fun toggleLyricsTranslation() {
        val state = _uiState.value
        if (!state.lyricsTranslationAvailable) return

        if (state.showTranslatedLyrics) {
            _uiState.update { it.copy(showTranslatedLyrics = false) }
            return
        }

        if (state.translatedPlainLyrics != null || state.translatedSyncedLyrics != null) {
            _uiState.update { it.copy(showTranslatedLyrics = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(lyricsTranslating = true) }
            val target = targetLanguageTag
            val plain = state.plainLyrics?.takeIf { it.isNotBlank() }
            val synced = state.syncedLyrics

            val translatedPlain = plain?.let { translationService.translate(it, target) }
            // Each line is translated individually, keeping its original `time`, so the
            // translated version stays synced/scrollable/clickable just like the original.
            val translatedSynced = synced?.takeIf { it.isNotEmpty() }?.let { lines ->
                coroutineScope {
                    lines.map { line ->
                        async {
                            val translated = translationService.translate(line.text, target)
                            line.copy(text = translated ?: line.text)
                        }
                    }.awaitAll()
                }
            }

            _uiState.update {
                it.copy(
                    lyricsTranslating = false,
                    translatedPlainLyrics = translatedPlain,
                    translatedSyncedLyrics = translatedSynced,
                    showTranslatedLyrics = translatedPlain != null || translatedSynced != null)
            }
        }
    }

    private var currentArtistId: String? = null

    init {
        viewModelScope.launch {
            audioPlayerQueue.currentQueueEntryFlow.collect { entry ->
                val trackId = when (entry) {
                    is QueueEntry.StreamingTrack -> entry.track.id
                    else -> null
                }
                if (entry is QueueEntry.StreamingTrack) {
                    val track = entry.track
                    _uiState.update {
                        it.copy(
                            trackTitle = track.title,
                            trackArtists = track.artists.joinToString(", ") { artist -> artist.name },
                            coverUrl = (track.album?.thumbnails ?: track.thumbnails)
                                ?.firstOrNull()?.url.orEmpty(),
                            track = track
                        )
                    }
                    if (trackId != null && trackId != currentTrackId) {
                        loadArtistOverview(track.artists.firstOrNull()?.id)
                        loadRelatedTracks(trackId)
                    }
                }
                if (trackId != null && trackId != currentTrackId) {
                    loadLyrics()
                }
            }
        }
    }

    /** "Info artista" tab: biography, genres, related artists, etc. for the track's main artist. */
    private fun loadArtistOverview(artistId: String?) {
        if (artistId == null) {
            _uiState.update { it.copy(artistOverview = null, artistOverviewLoading = false) }
            return
        }
        if (artistId == currentArtistId) return
        currentArtistId = artistId
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    artistOverviewLoading = true,
                    bioTranslationAvailable = false,
                    translatedBio = null,
                    showTranslatedBio = false)
            }
            val overview = runCatching { artistRepository.artistOverview(artistId) }.getOrNull()
            _uiState.update { it.copy(artistOverview = overview, artistOverviewLoading = false) }

            val bio = overview?.artist?.biography?.takeIf { it.isNotBlank() } ?: return@launch
            val detected = translationService.detectLanguage(bio) ?: return@launch
            val available = !detected.startsWith(targetLanguageTag, ignoreCase = true)
            _uiState.update { it.copy(bioTranslationAvailable = available) }
        }
    }

    /** Toggles the artist bio between its original text and the translated version. */
    fun toggleBioTranslation() {
        val state = _uiState.value
        if (!state.bioTranslationAvailable) return

        if (state.showTranslatedBio) {
            _uiState.update { it.copy(showTranslatedBio = false) }
            return
        }

        if (state.translatedBio != null) {
            _uiState.update { it.copy(showTranslatedBio = true) }
            return
        }

        val bio = state.artistOverview?.artist?.biography?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(bioTranslating = true) }
            val translated = translationService.translate(bio, targetLanguageTag)
            _uiState.update {
                it.copy(
                    bioTranslating = false,
                    translatedBio = translated,
                    showTranslatedBio = translated != null)
            }
        }
    }

    /** "Tracce correlate" tab: recommendations seeded by the currently playing track. */
    private fun loadRelatedTracks(trackId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(relatedTracksLoading = true, relatedTracks = null) }
            val plugin = pluginManager.selectedMetadataPlugin.value
            val tracks = plugin?.let {
                runCatching {
                    it.use { metadataTrackAPI.recommendationsBasedOnTracks(listOf(trackId), limit = 15) }
                }.getOrNull()
            }
            _uiState.update { it.copy(relatedTracks = tracks, relatedTracksLoading = false) }
        }
    }

    fun seekToLine(line: LyricsLine) {
        viewModelScope.launch {
            audioPlayer.seekTo(line.time.milliseconds)
        }
    }

    private fun activeLineIndex(lyrics: List<LyricsLine>?, positionMillis: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        val index = lyrics.indexOfLast { it.time <= positionMillis }
        return index.coerceAtLeast(0)
    }
}
