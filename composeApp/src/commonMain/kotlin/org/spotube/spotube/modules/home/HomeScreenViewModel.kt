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

package org.spotube.spotube.modules.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.plugin.PluginManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.spotube.spotube.core.zipline.settledLoggedInFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import spotube.composeapp.generated.resources.home_section_your_artists
import spotube.composeapp.generated.resources.home_section_your_albums
import spotube.composeapp.generated.resources.home_section_your_playlists
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error
import spotube.composeapp.generated.resources.home_chip_all
import spotube.composeapp.generated.resources.home_chip_audiobooks
import spotube.composeapp.generated.resources.home_chip_music
import spotube.composeapp.generated.resources.home_chip_podcasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.spotube.spotube.core.paths.Paths

sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    sealed interface Data : HomeScreenState {
        val featuredItems: List<MetadataBrowseItem>
        val genres: List<MetadataBrowseGenre>
        val selectedGenreId: String?
        val browseSections: Map<String, List<MetadataBrowseSection>?>
        val paginationStrategies: Map<String, PaginationStrategy?>

        data class Loaded(
            override val featuredItems: List<MetadataBrowseItem>,
            override val genres: List<MetadataBrowseGenre>,
            override val selectedGenreId: String?,
            override val browseSections: Map<String, List<MetadataBrowseSection>?>,
            override val paginationStrategies: Map<String, PaginationStrategy?>) : Data {
            fun toLoadingMore(): LoadingMore = LoadingMore(
                featuredItems = featuredItems,
                genres = genres,
                selectedGenreId = selectedGenreId,
                browseSections = browseSections,
                paginationStrategies = paginationStrategies)
        }

        data class LoadingMore(
            override val featuredItems: List<MetadataBrowseItem>,
            override val genres: List<MetadataBrowseGenre>,
            override val selectedGenreId: String?,
            override val browseSections: Map<String, List<MetadataBrowseSection>?>,
            override val paginationStrategies: Map<String, PaginationStrategy?>) : Data
    }

    data class Error(val message: String) : HomeScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModel(
    private val repository: HomeScreenRepository, private val pluginManager: PluginManager
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<HomeScreenViewModel>()

    private val state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val uiState = state.asStateFlow()

    private val libraryRepository: LibraryRepository by inject()

    /** The user's own playlists for the shortcut grid at the top of Home (empty if logged out). */
    private val _quickAccess = MutableStateFlow<List<MetadataPlaylist>>(emptyList())
    val quickAccess: StateFlow<List<MetadataPlaylist>> = _quickAccess.asStateFlow()

    private fun loadQuickAccess() {
        viewModelScope.launch {
            _quickAccess.value = runCatching {
                libraryRepository.savedPlaylists()?.items?.take(7)
            }.getOrNull() ?: emptyList()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)

    /** True only while the user pulls to refresh: the first load never shows the spinner. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val snapshotPath by lazy { "${Paths().getApplicationCacheDirPath()}/home_snapshot.json".toPath() }

    /** Last home the user saw, kept on disk so the next start shows it instantly. */
    @Serializable
    private data class HomeSnapshot(
        val genres: List<MetadataBrowseGenre> = emptyList(),
        val sections: List<MetadataBrowseSection> = emptyList(),
        val quickAccess: List<MetadataPlaylist> = emptyList(),
    )

    private suspend fun restoreSnapshot() {
        val snapshot = withContext(Dispatchers.Default) {
            runCatching {
                if (!FileSystem.SYSTEM.exists(snapshotPath)) return@runCatching null
                json.decodeFromString<HomeSnapshot>(FileSystem.SYSTEM.read(snapshotPath) { readUtf8() })
            }.getOrNull()
        } ?: return
        val homeId = snapshot.genres.firstOrNull()?.id ?: return
        if (state.value !is HomeScreenState.Loading || snapshot.sections.isEmpty()) return
        if (_quickAccess.value.isEmpty()) _quickAccess.value = snapshot.quickAccess
        state.value = HomeScreenState.Data.Loaded(
            featuredItems = emptyList(),
            genres = snapshot.genres,
            selectedGenreId = homeId,
            browseSections = mapOf(homeId to snapshot.sections),
            paginationStrategies = emptyMap())
    }

    private suspend fun saveSnapshot(genres: List<MetadataBrowseGenre>, sections: List<MetadataBrowseSection>) {
        val homeId = genres.firstOrNull()?.id ?: return
        if (sections.isEmpty()) return
        withContext(Dispatchers.Default) {
            runCatching {
                val snapshot = HomeSnapshot(genres, sections, _quickAccess.value)
                FileSystem.SYSTEM.write(snapshotPath) { writeUtf8(json.encodeToString(snapshot)) }
            }.onFailure { e -> logger.e(e) { "Failed to save home snapshot for $homeId" } }
        }
    }

    /**
     * Personal rows built from the user's library, shown above Spotify's feed: Spotify's home
     * request does not always return the personalized sections, the library always is the user's own.
     */
    private suspend fun personalSections(): List<MetadataBrowseSection> {
        val artists = runCatching { libraryRepository.savedArtists()?.items }.getOrNull().orEmpty()
            .take(20).map { a ->
                MetadataBrowseItem.Artist(
                    MetadataArtist.Basic(a.id, a.name, a.thumbnails, a.externalUri))
            }
        val albums = runCatching { libraryRepository.savedAlbums()?.items }.getOrNull().orEmpty()
            .take(20).map { a ->
                MetadataBrowseItem.Album(
                    MetadataAlbum.Basic(
                        a.id, a.title, a.description, a.thumbnails, a.albumType, a.artists, a.externalUri))
            }
        val playlists = runCatching { libraryRepository.savedPlaylists()?.items }.getOrNull().orEmpty()
            .drop(7).take(20).map { MetadataBrowseItem.Playlist(it) }
        return listOfNotNull(
            albums.takeIf { it.isNotEmpty() }?.let {
                MetadataBrowseSection(getString(Res.string.home_section_your_albums), null, it)
            },
            artists.takeIf { it.isNotEmpty() }?.let {
                MetadataBrowseSection(getString(Res.string.home_section_your_artists), null, it)
            },
            playlists.takeIf { it.isNotEmpty() }?.let {
                MetadataBrowseSection(getString(Res.string.home_section_your_playlists), null, it)
            },
        )
    }

    init {
        viewModelScope.launch { restoreSnapshot() }
        viewModelScope.launch {
            pluginManager.selectedMetadataPlugin
                .filterNotNull()
                // Waits for the plugin to restore its session, so the home is not loaded (and
                // refused) as a signed-out user at every start.
                .flatMapLatest { it.settledLoggedInFlow() }
                .distinctUntilChanged().collect {
                    loadInitialData()
                }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        // With a saved home already on screen the update happens silently behind it.
        val hasContent = state.value is HomeScreenState.Data
        if (!hasContent) state.value = HomeScreenState.Loading
        loadQuickAccess()
        val genres = (repository.genres() ?: emptyList()).shortList()
        val firstGenreId = genres.firstOrNull()?.id
        val browseSections = mutableMapOf<String, List<MetadataBrowseSection>?>()
        val paginationStrategies = mutableMapOf<String, PaginationStrategy?>()
        if (firstGenreId != null) {
            val result = repository.list(firstGenreId)
            browseSections[firstGenreId] =
                (personalSections() + (result?.items ?: emptyList())).cleaned()
            paginationStrategies[firstGenreId] = result?.nextPagination
        }
        // Spotify's home is only the personal feed: no generic "featured" carousel on top of it.
        state.value = HomeScreenState.Data.Loaded(
            featuredItems = emptyList(),
            genres = genres,
            selectedGenreId = firstGenreId,
            browseSections = browseSections,
            paginationStrategies = paginationStrategies)
        if (firstGenreId != null) saveSnapshot(genres, browseSections[firstGenreId].orEmpty())
    }.onFailure { e ->
        logger.e(e) { "Failed to load home screen data" }
        // A failed update must not replace the saved home with an error page.
        if (state.value !is HomeScreenState.Data) {
            state.value = HomeScreenState.Error(e.message ?: getString(Res.string.common_unknown_error))
        }
    }

    fun selectGenre(genreId: String) {
        val currentState = state.value
        if (currentState is HomeScreenState.Data) {
            state.value = when (currentState) {
                is HomeScreenState.Data.Loaded -> currentState.copy(selectedGenreId = genreId)
                is HomeScreenState.Data.LoadingMore -> currentState.copy(selectedGenreId = genreId)
            }
            if (currentState.browseSections[genreId] == null) {
                viewModelScope.launch {
                    // A category that fails to load shows up empty instead of breaking the whole home.
                    val result = runCatching { repository.list(genreId) }
                        .onFailure { e -> logger.e(e) { "Failed to load home category $genreId" } }
                        .getOrNull()
                    val current = state.value
                    if (current is HomeScreenState.Data) {
                        state.value = HomeScreenState.Data.Loaded(
                            featuredItems = current.featuredItems,
                            genres = current.genres,
                            selectedGenreId = genreId,
                            browseSections = current.browseSections + (genreId to (result?.items ?: emptyList()).cleaned()),
                            paginationStrategies = current.paginationStrategies + (genreId to result?.nextPagination))
                    }
                }
            }
        }
    }

    suspend fun loadMoreData() = runCatching {
        val currentState = state.value
        if (currentState is HomeScreenState.Data.Loaded) {
            val genreId = currentState.selectedGenreId ?: return@runCatching
            val pagination = currentState.paginationStrategies[genreId] ?: return@runCatching
            state.value = currentState.toLoadingMore()
            val result = repository.list(genreId, pagination)
            val existingSections = currentState.browseSections[genreId] ?: emptyList()
            state.value = HomeScreenState.Data.Loaded(
                featuredItems = currentState.featuredItems,
                genres = currentState.genres,
                selectedGenreId = genreId,
                browseSections = currentState.browseSections + (genreId to (existingSections + (result?.items ?: emptyList()).cleaned())),
                paginationStrategies = currentState.paginationStrategies + (genreId to result?.nextPagination))
        }
    }.onFailure { e ->
        logger.e(e) { "Failed to load more home screen data" }
        state.value = HomeScreenState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.invalidateCaches()
            loadInitialData()
            _isRefreshing.value = false
        }
    }
}

/**
 * Like Spotify's home, the chip row is only "All / Music / Podcasts / Audiobooks": the home feed
 * first, then the browse pages with those names (matched in English and Italian). Chips the
 * account has no page for are left out.
 */
private val ChipPages: List<Pair<StringResource, List<String>>> = listOf(
    Res.string.home_chip_music to listOf("music", "musica"),
    Res.string.home_chip_podcasts to listOf("podcast", "podcasts"),
    Res.string.home_chip_audiobooks to listOf("audiobook", "audiobooks", "audiolibro", "audiolibri"),
)

/** Fallback when the browse pages above are not found: the three main Spotify pages. */
private val MainBrowsePageIds = listOf(
    "0JQ5DAt0tbjZptfcdMSKl3", // Made For You
    "0JQ5DAqbMKFz6FAsUtgAab", // New Releases
    "0JQ5DAudkNjCgYMM0TZXDw", // Charts
)

private suspend fun List<MetadataBrowseGenre>.shortList(): List<MetadataBrowseGenre> {
    val home = firstOrNull() ?: return emptyList()
    val rest = drop(1)
    val all = MetadataBrowseGenre(home.id, getString(Res.string.home_chip_all))
    val named = ChipPages.mapNotNull { (label, names) ->
        rest.firstOrNull { it.name.trim().lowercase() in names }
            ?.let { MetadataBrowseGenre(it.id, getString(label)) }
    }
    if (named.isNotEmpty()) return listOf(all) + named
    val main = MainBrowsePageIds.mapNotNull { pageId -> rest.firstOrNull { it.id.endsWith(pageId) } }
    return listOf(all) + main
}

/** Drops cards with no id or no title (they open a broken page) and the sections left empty. */
private fun List<MetadataBrowseSection>.cleaned(): List<MetadataBrowseSection> = mapNotNull { section ->
    val items = section.items.filter { item ->
        when (item) {
            is MetadataBrowseItem.Album -> item.data.id.isNotBlank() && item.data.title.isNotBlank()
            is MetadataBrowseItem.Artist -> item.data.id.isNotBlank() && item.data.name.isNotBlank()
            is MetadataBrowseItem.Playlist -> item.data.id.isNotBlank() && item.data.title.isNotBlank()
            is MetadataBrowseItem.Track -> item.data.id.isNotBlank() && item.data.title.isNotBlank()
            is MetadataBrowseItem.User -> item.data.id.isNotBlank()
        }
    }
    if (items.isEmpty()) null else section.copy(items = items)
}
