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

package org.spotube.spotube.modules.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.remote.CollectionPlaybackUiState
import org.spotube.spotube.core.remote.RemoteCollectionType
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.core.share.ShareService
import org.spotube.spotube.core.ui.component.TrackOptionsAction
import org.spotube.spotube.core.ui.component.TrackOptionsContext
import org.spotube.spotube.modules.blacklist.BlacklistRepository
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.*

sealed interface ArtistScreenState {
    data object Loading : ArtistScreenState

    data class Loaded(
        val artist: MetadataArtist.Detailed,
        val isArtistSaved: Boolean,
        val topTracks: List<MetadataTrack>,
        val albums: List<MetadataAlbum.Detailed>,
        val albumsNextPagination: PaginationStrategy?,
        val relatedArtists: List<MetadataArtist.Basic>,
        val relatedArtistsNextPagination: PaginationStrategy?,
        val featuredPlaylists: List<MetadataPlaylist>,
        val featuredPlaylistsNextPagination: PaginationStrategy?,
        val isSaving: Boolean = false,
        /** The albums row is expanded into the full discography list. */
        val showFullDiscography: Boolean = false,
        val isLoadingAlbums: Boolean = false) : ArtistScreenState

    data class Error(val message: String) : ArtistScreenState
}

@OptIn(ExperimentalStdlibApi::class)
class ArtistViewModel(
    private val artistId: String,
    private val repository: ArtistRepository,
    private val libraryRepository: LibraryRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository,
    private val shareService: ShareService,
    private val remotePlaybackController: RemotePlaybackController) : ViewModel(), KoinComponent {
    private val logger by injectLogger<ArtistViewModel>()

    private val _state = MutableStateFlow<ArtistScreenState>(ArtistScreenState.Loading)
    val state: StateFlow<ArtistScreenState> = _state.asStateFlow()

    /** Loading flags for the top-tracks play / add-to-queue buttons. */
    val playbackUiState: StateFlow<CollectionPlaybackUiState> = remotePlaybackController
        .collectionPlaybackUiState(RemoteCollectionType.ArtistTopTracks, artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionPlaybackUiState())

    val savedArtistIds
        get() = libraryRepository.savedArtistIdsFlow

    val savedTrackIds
        get() = savedTracksRepository.savedTracksIdsFlow

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _blacklistedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val blacklistedTrackIds: StateFlow<Set<String>> = _blacklistedTrackIds.asStateFlow()

    private val _blacklistedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    val blacklistedArtistIds: StateFlow<Set<String>> = _blacklistedArtistIds.asStateFlow()

    private val _tracksToAddToPlaylist = MutableStateFlow<List<MetadataTrack>>(emptyList())
    private val _showAddToPlaylistPicker = MutableStateFlow(false)
    val showAddToPlaylistPicker: StateFlow<Boolean> = _showAddToPlaylistPicker.asStateFlow()

    val trackOptionsContext: StateFlow<TrackOptionsContext> = combine(
        audioPlayerQueue.queueFlow,
        audioPlayerQueue.currentQueueEntryFlow,
        savedTracksRepository.savedTracksIdsFlow,
        _blacklistedTrackIds,
        _blacklistedArtistIds) { queue, currentEntry, savedIds, blacklistedTracks, blacklistedArtists ->
        TrackOptionsContext(
            currentTrackId = (currentEntry as? QueueEntry.StreamingTrack)?.track?.id,
            queueTrackIds = queue.mapNotNull { entry ->
                (entry as? QueueEntry.StreamingTrack)?.track?.id
            }.toSet(),
            savedTrackIds = savedIds,
            blacklistedTrackIds = blacklistedTracks,
            blacklistedArtistIds = blacklistedArtists)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackOptionsContext())

    init {
        viewModelScope.launch {
            libraryRepository.savedArtistIdsFlow.collect {
                val currentState = _state.value
                if (currentState is ArtistScreenState.Loaded) {
                    _state.value = currentState.copy(isArtistSaved = it.contains(artistId))
                }
            }
        }
        blacklistRepository.blacklistedTracks
            .onEach { tracks -> _blacklistedTrackIds.value = tracks.map { it.id }.toSet() }
            .launchIn(viewModelScope)
        blacklistRepository.blacklistedArtists
            .onEach { artists -> _blacklistedArtistIds.value = artists.map { it.id }.toSet() }
            .launchIn(viewModelScope)
        loadCurrentUser()
        loadOverview()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            runCatching {
                _currentUserId.value = libraryRepository.currentUser()?.id
            }.onFailure { e ->
                logger.e(e) { "Failed to load current user" }
            }
        }
    }

    fun refresh() {
        repository.invalidateCaches()
        loadOverview()
    }

    fun toggleSavedArtist() {
        val loaded = _state.value as? ArtistScreenState.Loaded ?: return
        if (loaded.isSaving) return
        val follow = !loaded.isArtistSaved
        // Show the new state right away ("Segui" -> "Segui già"); undo it if Spotify refuses.
        _state.update { state ->
            if (state is ArtistScreenState.Loaded) state.copy(isSaving = true, isArtistSaved = follow) else state
        }
        viewModelScope.launch {
            runCatching {
                if (follow) {
                    libraryRepository.saveArtists(listOf(artistId))
                } else {
                    libraryRepository.removeSavedArtists(listOf(artistId))
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to toggle saved artist" }
                _state.update { state ->
                    if (state is ArtistScreenState.Loaded) state.copy(isArtistSaved = !follow) else state
                }
            }
            setSaving(false)
        }
    }

    private fun setSaving(isSaving: Boolean) {
        _state.update { state ->
            if (state is ArtistScreenState.Loaded) state.copy(isSaving = isSaving) else state
        }
    }

    fun isArtistBlacklisted(artistId: String): Boolean {
        return _blacklistedArtistIds.value.contains(artistId)
    }

    fun toggleArtistBlacklist(artist: MetadataArtist) {
        viewModelScope.launch {
            blacklistRepository.toggleArtist(artist)
        }
    }

    fun isTrackBlacklisted(track: MetadataTrack): Boolean {
        val trackIds = _blacklistedTrackIds.value
        val artistIds = _blacklistedArtistIds.value
        return track.id in trackIds || track.artists.any { it.id in artistIds }
    }

    fun toggleTrackBlacklist(track: MetadataTrack) {
        viewModelScope.launch {
            blacklistRepository.toggleTrack(track)
        }
    }

    fun loadMoreAlbums() {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded || currentState.isLoadingAlbums) return
        val pagination = currentState.albumsNextPagination ?: return
        setLoadingAlbums(true)

        viewModelScope.launch {
            runCatching {
                repository.albums(artistId, pagination)
            }.onSuccess { result ->
                _state.update { state ->
                    if (state !is ArtistScreenState.Loaded) return@update state
                    if (result == null) return@update state.copy(albumsNextPagination = null)
                    // The same release can come back on two pages: a repeated id would crash the row.
                    val merged = (state.albums + result.items).distinctBy { it.id }
                    state.copy(
                        albums = merged,
                        // No new album on this page: stop asking, or the row would load forever.
                        albumsNextPagination = result.nextPagination
                            ?.takeIf { merged.size > state.albums.size && it != pagination })
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more albums" }
            }
            setLoadingAlbums(false)
        }
    }

    private fun setLoadingAlbums(loading: Boolean) {
        _state.update { state ->
            if (state is ArtistScreenState.Loaded) state.copy(isLoadingAlbums = loading) else state
        }
    }

    /** Opens or closes the full discography; opening it loads every remaining page. */
    fun toggleFullDiscography() {
        val currentState = _state.value as? ArtistScreenState.Loaded ?: return
        val show = !currentState.showFullDiscography
        _state.update { state ->
            if (state is ArtistScreenState.Loaded) state.copy(showFullDiscography = show) else state
        }
        if (!show) return
        viewModelScope.launch {
            var guard = 0
            while (guard++ < 40) {
                val state = _state.value as? ArtistScreenState.Loaded ?: return@launch
                if (!state.showFullDiscography) return@launch
                val pagination = state.albumsNextPagination ?: return@launch
                if (state.isLoadingAlbums) { kotlinx.coroutines.delay(150); continue }
                setLoadingAlbums(true)
                val result = runCatching { repository.albums(artistId, pagination) }
                    .onFailure { e -> logger.e(e) { "Failed to load the discography" } }
                    .getOrNull()
                _state.update { current ->
                    if (current !is ArtistScreenState.Loaded) return@update current
                    if (result == null) return@update current.copy(albumsNextPagination = null, isLoadingAlbums = false)
                    val merged = (current.albums + result.items).distinctBy { it.id }
                    current.copy(
                        albums = merged,
                        albumsNextPagination = result.nextPagination
                            ?.takeIf { merged.size > current.albums.size && it != pagination },
                        isLoadingAlbums = false)
                }
            }
        }
    }

    fun loadMoreRelatedArtists() {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return
        val pagination = currentState.relatedArtistsNextPagination ?: return

        viewModelScope.launch {
            runCatching {
                repository.relatedArtists(artistId, pagination)
            }.onSuccess { result ->
                if (result != null) {
                    _state.update { state ->
                        if (state !is ArtistScreenState.Loaded) return@update state
                        val merged = (state.relatedArtists + result.items).distinctBy { it.id }
                        state.copy(
                            relatedArtists = merged,
                            relatedArtistsNextPagination = result.nextPagination
                                ?.takeIf { merged.size > state.relatedArtists.size && it != pagination })
                    }
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more related artists" }
            }
        }
    }

    fun loadMoreFeaturedPlaylists() {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return
        val pagination = currentState.featuredPlaylistsNextPagination ?: return

        viewModelScope.launch {
            runCatching {
                repository.featuredPlaylists(artistId, pagination)
            }.onSuccess { result ->
                if (result != null) {
                    _state.update { state ->
                        if (state !is ArtistScreenState.Loaded) return@update state
                        val merged = (state.featuredPlaylists + result.items).distinctBy { it.id }
                        state.copy(
                            featuredPlaylists = merged,
                            featuredPlaylistsNextPagination = result.nextPagination
                                ?.takeIf { merged.size > state.featuredPlaylists.size && it != pagination })
                    }
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more featured playlists" }
            }
        }
    }

    fun addTopTracksToQueue() {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestCollectionAddToQueue(
            RemoteCollectionType.ArtistTopTracks,
            artistId,
            artistName)
    }

    fun playTopTracks() {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestCollectionPlay(
            RemoteCollectionType.ArtistTopTracks,
            artistId,
            artistName)
    }

    fun playTopTracksFromTrack(track: MetadataTrack) {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestCollectionPlay(
            type = RemoteCollectionType.ArtistTopTracks,
            id = artistId,
            title = artistName,
            startTrack = track)
    }
    fun addTracksToJam(tracks: List<MetadataTrack>) {
        remotePlaybackController.addTracksToJam(tracks)
    }

    fun addTracksToQueue(tracks: List<MetadataTrack>) {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestTracksAddToQueue(tracks, artistName)
    }

    fun playTracksNext(tracks: List<MetadataTrack>) {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestTracksPlayNext(tracks, artistName)
    }

    fun handleTrackOptionsAction(track: MetadataTrack, action: TrackOptionsAction) {
        viewModelScope.launch {
            when (action) {
                is TrackOptionsAction.StartRadio -> {}
                is TrackOptionsAction.PlayNext -> {
                    val queue = audioPlayerQueue.getQueue()
                    val existing = queue.find { entry ->
                        (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
                    }
                    if (existing != null) {
                        // Already in the local queue: move it to the next position
                        audioPlayerQueue.removeFromQueue(existing)
                        audioPlayerQueue.addAllAfterCurrent(listOf(QueueEntry.StreamingTrack(track = track, url = "")))
                    } else {
                        remotePlaybackController.requestTrackPlayNext(track)
                    }
                }
                is TrackOptionsAction.AddToQueue -> {
                    remotePlaybackController.requestTrackAddToQueue(track)
                }

                is TrackOptionsAction.AddToJam -> {
                    remotePlaybackController.addTrackToJam(track)
                }
                is TrackOptionsAction.RemoveFromQueue -> {
                    val queue = audioPlayerQueue.getQueue()
                    queue.find { entry ->
                        (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
                    }?.let { audioPlayerQueue.removeFromQueue(it) }
                }
                is TrackOptionsAction.ToggleFavorite -> {
                    val savedIds = savedTrackIds.value
                    if (savedIds.contains(track.id)) {
                        savedTracksRepository.removeSavedTracks(listOf(track.id))
                    } else {
                        savedTracksRepository.saveTracks(listOf(track.id))
                    }
                }
                is TrackOptionsAction.ToggleBlacklist -> toggleTrackBlacklist(track)
                is TrackOptionsAction.Share -> {
                    val uri = track.externalUri?.takeIf { it.isNotBlank() }
                    if (uri != null) {
                        shareService.share(uri, track.title)
                    }
                }
                is TrackOptionsAction.AddToPlaylist -> {
                    _tracksToAddToPlaylist.value = listOf(track)
                    _showAddToPlaylistPicker.value = true
                }
            }
        }
    }

    fun addTracksToPlaylist(playlistId: String) {
        viewModelScope.launch {
            runCatching {
                libraryRepository.addTracksToPlaylist(
                    playlistId,
                    _tracksToAddToPlaylist.value.map { it.id })
            }.onFailure { e ->
                logger.e(e) { "Failed to add tracks to playlist" }
            }.onSuccess {
                _showAddToPlaylistPicker.value = false
            }
        }
    }

    fun showAddToPlaylistPicker(tracks: List<MetadataTrack>) {
        _tracksToAddToPlaylist.value = tracks
        _showAddToPlaylistPicker.value = true
    }

    fun dismissAddToPlaylistPicker() {
        _showAddToPlaylistPicker.value = false
    }

    private fun loadOverview() {
        _state.value = ArtistScreenState.Loading
        viewModelScope.launch {
            runCatching {
                repository.artistOverview(artistId)
            }.onSuccess { overview ->
                if (overview != null) {
                    savedTracksRepository.isSavedTracks(overview.top10Tracks.map { it.id })
                    _state.value = ArtistScreenState.Loaded(
                        artist = overview.artist,
                        isArtistSaved = libraryRepository.savedArtistIdsFlow.value.contains(artistId),
                        topTracks = overview.top10Tracks,
                        albums = overview.albums.items.distinctBy { it.id },
                        albumsNextPagination = overview.albums.nextPagination,
                        relatedArtists = overview.relatedArtists.items.distinctBy { it.id },
                        relatedArtistsNextPagination = overview.relatedArtists.nextPagination,
                        featuredPlaylists = overview.featuredPlaylists.items.distinctBy { it.id },
                        featuredPlaylistsNextPagination = overview.featuredPlaylists.nextPagination)
                    // Ask whether the artist is already followed, so the button starts in the right state.
                    val followed = runCatching {
                        libraryRepository.isSavedArtists(listOf(artistId)).firstOrNull() == true
                    }.getOrDefault(false)
                    if (followed) {
                        _state.update { state ->
                            if (state is ArtistScreenState.Loaded) state.copy(isArtistSaved = true) else state
                        }
                    }
                } else {
                    _state.value = ArtistScreenState.Error(getString(Res.string.artist_error_load_failed))
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load artist overview" }
                _state.value = ArtistScreenState.Error(e.message ?: getString(Res.string.common_unknown_error))
            }
        }
    }

    private suspend fun resolveTopTrackEntries(): List<QueueEntry> {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return emptyList()
        
        val blacklistedTracks = blacklistRepository.getTracksSnapshot()
        val blacklistedArtists = blacklistRepository.getArtistsSnapshot()
        val blacklistedTrackIds = blacklistedTracks.map { it.id }.toSet()
        val blacklistedArtistIds = blacklistedArtists.map { it.id }.toSet()
        
        return currentState.topTracks
            .filter { track ->
                track.id !in blacklistedTrackIds && 
                track.artists.none { it.id in blacklistedArtistIds }
            }
            .map { track ->
                QueueEntry.StreamingTrack(track = track, url = "")
            }
    }

    private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
        if (id.isNotBlank() && other.id.isNotBlank()) {
            return id == other.id
        }
        return title == other.title &&
                durationMs == other.durationMs &&
                album?.id == other.album?.id &&
                artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
    }
}
