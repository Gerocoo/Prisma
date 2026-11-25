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


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumTooltip
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import com.composeunstyled.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.koin.compose.koinInject
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ExpressiveLoadingIndicator
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.TrackList
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.dragScrollable
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.modules.library.playlist.AddToPlaylistPicker
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxUserRemove
import org.spotube.spotube.resources.iconsax.User
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun ArtistScreen(
    viewModel: ArtistViewModel,
    audioPlayerQueue: AudioPlayerQueue,
    audioPlayer: AudioPlayerInterface,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playbackUiState by viewModel.playbackUiState.collectAsStateWithLifecycle()
    val jamRoomService: JamRoomService = koinInject()
    val jamActive by jamRoomService.role.map { it != null }
        .collectAsStateWithLifecycle(initialValue = false)
    val isJamGuest by jamRoomService.role.map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val currentQueueEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val savedArtistIds by viewModel.savedArtistIds.collectAsStateWithLifecycle()
    val blacklistedArtistIds by viewModel.blacklistedArtistIds.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val trackOptionsContext by viewModel.trackOptionsContext.collectAsStateWithLifecycle()
    val showAddToPlaylistPicker by viewModel.showAddToPlaylistPicker.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MillenniumTheme.colors.background)
    ) {
        ApplicationMainBar()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val currentState = state) {
                is ArtistScreenState.Loading -> {
                    ArtistLoadingContent()
                }

                is ArtistScreenState.Error -> {
                    ArtistErrorContent(
                        message = currentState.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is ArtistScreenState.Loaded -> {
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        // Leaves room for the mini player and the navigation bar under the last row.
                        contentPadding = PaddingValues(bottom = LocalAppShellBottomInset.current + 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            ArtistHeaderCard(
                                artist = currentState.artist,
                                isSaved = currentState.isArtistSaved || savedArtistIds.contains(currentState.artist.id),
                                isSaving = currentState.isSaving,
                                onFollowClick = viewModel::toggleSavedArtist,
                                isBlacklisted = currentState.artist.id in blacklistedArtistIds,
                                onBlacklistClick = { viewModel.toggleArtistBlacklist(currentState.artist) })
                        }

                        item {
                            TopTracksHeader(
                                onPlay = viewModel::playTopTracks,
                                onAddToQueue = viewModel::addTopTracksToQueue,
                                isPlayLoading = playbackUiState.isPlayLoading,
                                isAddToQueueLoading = playbackUiState.isAddToQueueLoading
                            )
                        }

                        item {
                            TrackList(
                                tracks = currentState.topTracks,
                                error = null,
                                hasMore = false,
                                isLoading = false,
                                isLoadingNextPage = false,
                                currentTrackId = (currentQueueEntry as? QueueEntry.StreamingTrack)?.track?.id,
                                isCurrentTrackPlaying = playerState == PlayerState.PLAYING,
                                onTrackClick = viewModel::playTopTracksFromTrack,
                                onTrackOptionsAction = viewModel::handleTrackOptionsAction,
                                trackOptionsState = trackOptionsContext::stateFor,
                                onArtistClick = { trackArtist ->
                                    navigationCommands.navigateTo(
                                        Routes.Artist(
                                            trackArtist.id
                                        )
                                    )
                                },
                                onAlbumClick = { album ->
                                    navigationCommands.navigateTo(
                                        Routes.Album(
                                            album.id
                                        )
                                    )
                                },
                                onLoadNextPage = {},
                                simplified = true,
                                scrollable = false,
                                onBulkAddToQueue = viewModel::addTracksToQueue,
                                onBulkPlayNext = viewModel::playTracksNext,
                                onBulkAddToPlaylist = viewModel::showAddToPlaylistPicker,
                                onBulkAddToJam = viewModel::addTracksToJam,
                                isInJam = jamActive,
                                isJamGuest = isJamGuest
                            )
                        }

                        if (currentState.albums.isNotEmpty() || currentState.albumsNextPagination != null) {
                            item(key = "albums-section") {
                                AlbumsSection(
                                    albums = currentState.albums,
                                    expanded = currentState.showFullDiscography,
                                    onToggleExpanded = viewModel::toggleFullDiscography,
                                    onLoadMore = { viewModel.loadMoreAlbums() })
                            }
                            if (currentState.showFullDiscography) {
                                items(currentState.albums, key = { "discography-" + it.id }) { album ->
                                    AlbumCard(
                                        album = album,
                                        listMode = true,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    )
                                }
                                if (currentState.isLoadingAlbums) {
                                    item(key = "discography-loading") {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) { ExpressiveLoadingIndicator() }
                                    }
                                }
                            }
                        }

                        if (currentState.relatedArtists.isNotEmpty() || currentState.relatedArtistsNextPagination != null) {
                            item {
                                RelatedArtistsSection(
                                    artists = currentState.relatedArtists,
                                    onArtistClick = { artist ->
                                        navigationCommands.navigateTo(
                                            Routes.Artist(
                                                artist.id
                                            )
                                        )
                                    },
                                    onLoadMore = { viewModel.loadMoreRelatedArtists() })
                            }
                        }

                        if (currentState.featuredPlaylists.isNotEmpty() || currentState.featuredPlaylistsNextPagination != null) {
                            item {
                                FeaturedPlaylistsSection(
                                    playlists = currentState.featuredPlaylists,
                                    onPlaylistClick = { playlist ->
                                        navigationCommands.navigateTo(
                                            Routes.Playlist(
                                                playlist.id
                                            )
                                        )
                                    },
                                    onLoadMore = { viewModel.loadMoreFeaturedPlaylists() })
                            }
                        }
                    }
                }
            }
        }

        AddToPlaylistPicker(
            visible = showAddToPlaylistPicker,
            currentUserId = currentUserId,
            onDismiss = viewModel::dismissAddToPlaylistPicker,
            onPlaylistSelected = viewModel::addTracksToPlaylist
        )
    }
}

@Composable
private fun ArtistLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SkeletonTree(true) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceContainerHighest)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(4) {
                            PlayableCard(
                                title = "Item Title",
                                subtitle = "Subtitle",
                                imageURL = "https://placehold.co/600x400"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                color = AppTheme.colors.error,
                style = AppTheme.typography.bodyLarge
            )
            MillenniumButton(onClick = onRetry) {
                Text(stringResource(Res.string.common_retry))
            }
        }
    }
}

@Composable
private fun ArtistHeaderCard(
    artist: MetadataArtist.Detailed,
    isSaved: Boolean,
    isSaving: Boolean,
    onFollowClick: () -> Unit,
    isBlacklisted: Boolean,
    onBlacklistClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ArtistPhotoCarousel(artist = artist)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArtistMeta(artist = artist)

            ArtistHeaderActions(
                isSaved = isSaved,
                isSaving = isSaving,
                onFollowClick = onFollowClick,
                isBlacklisted = isBlacklisted,
                onBlacklistClick = onBlacklistClick
            )
        }
    }
}

/**
 * Horizontally scrollable strip of artist photos, matching Spotify's artist-page hero.
 * The plugin only supplies a single avatar image today, so this renders one large photo;
 * it already behaves like a real carousel, so a future photo gallery from the plugin needs
 * no change here, only more items in [MetadataArtist.Detailed.thumbnails].
 */
@Composable
private fun ArtistPhotoCarousel(artist: MetadataArtist.Detailed) {
    val photos = artist.thumbnails.distinctBy { it.url }.take(1)

    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(AppTheme.colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                imageVector = Iconsax.User,
                contentDescription = artist.name,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(96.dp)
            )
        }
        return
    }

    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos) { photo ->
            AsyncImage(
                model = photo.url,
                contentDescription = artist.name,
                modifier = Modifier
                    .fillParentMaxWidth(if (photos.size > 1) 0.82f else 1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ArtistMeta(artist: MetadataArtist.Detailed) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = artist.name,
            style = AppTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        val monthlyListenersText = artist.monthlyListeners?.let { formatMonthlyListeners(it) }
        val worldRankText = artist.worldRank?.let { stringResource(Res.string.artist_world_rank, it) }
        if (monthlyListenersText != null || worldRankText != null) {
            Text(
                text = listOfNotNull(monthlyListenersText, worldRankText).joinToString(" • "),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.primary,
                textAlign = TextAlign.Start
            )
        }

        Text(
            text = buildString {
                append(formatFollowers(artist.followersCount))
                if (artist.genres.isNotEmpty()) {
                    append(" • ")
                    append(artist.genres.joinToString(", "))
                }
            },
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Start
        )

        artist.biography?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ArtistHeaderActions(
    isSaved: Boolean,
    isSaving: Boolean,
    onFollowClick: () -> Unit,
    isBlacklisted: Boolean,
    onBlacklistClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSaved) {
            MillenniumButton(
                onClick = onFollowClick,
                loading = isSaving,
                style = MillenniumButtonStyle.Secondary
            ) {
                Text(
                    stringResource(Res.string.artist_following),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            MillenniumButton(onClick = onFollowClick, loading = isSaving) {
                Text(stringResource(Res.string.collection_details_follow), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
            }
        }

        MillenniumTooltip(
            text = if (isBlacklisted) stringResource(Res.string.blacklist_remove) else stringResource(Res.string.blacklist_add)
        ) {
            if (isBlacklisted) {
                MillenniumIconButton(
                    onClick = onBlacklistClick,
                    style = MillenniumIconButtonStyle.Secondary
                ) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxUserRemove,
                        contentDescription = stringResource(Res.string.blacklist_remove),
                        tint = AppTheme.colors.error
                    )
                }
            } else {
                MillenniumIconButton(
                    onClick = onBlacklistClick,
                    style = MillenniumIconButtonStyle.Secondary
                ) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxUserRemove,
                        contentDescription = stringResource(Res.string.blacklist_add)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopTracksHeader(
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlayLoading: Boolean = false,
    isAddToQueueLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(Res.string.artist_top_tracks),
            style = AppTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MillenniumIconButton(
                onClick = onPlay,
                loading = isPlayLoading,
                style = MillenniumIconButtonStyle.Primary
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxPlay,
                    contentDescription = stringResource(Res.string.artist_play_top_tracks),
                    tint = LocalTextStyle.current.color
                )
            }
            MillenniumIconButton(
                onClick = onAddToQueue,
                loading = isAddToQueueLoading,
                style = MillenniumIconButtonStyle.Secondary
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxAddSquare,
                    contentDescription = stringResource(Res.string.artist_add_top_tracks_to_queue)
                )
            }
        }
    }
}

@Composable
private fun AlbumsSection(
    albums: List<MetadataAlbum.Detailed>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onLoadMore: () -> Unit
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState, expanded) {
        if (expanded) return@LaunchedEffect
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.library_tab_albums),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            MillenniumButton(onClick = onToggleExpanded, style = MillenniumButtonStyle.Secondary) {
                Text(
                    stringResource(
                        if (expanded) Res.string.artist_discography_hide else Res.string.artist_discography_show
                    ),
                    maxLines = 1
                )
            }
        }
        if (!expanded) {
            Spacer(modifier = Modifier.size(8.dp))
            LazyRow(
                state = rowState,
                modifier = Modifier.dragScrollable(rowState),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(album = album)
                }
            }
        }
    }
}

@Composable
private fun RelatedArtistsSection(
    artists: List<MetadataArtist.Basic>,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onLoadMore: () -> Unit
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = stringResource(Res.string.artist_related_artists))
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        items(artists, key = { it.id }) { artist ->
            ArtistCard(artist = artist)
        }
    }
}

@Composable
private fun FeaturedPlaylistsSection(
    playlists: List<MetadataPlaylist>,
    onPlaylistClick: (MetadataPlaylist) -> Unit,
    onLoadMore: () -> Unit
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = stringResource(Res.string.artist_featured_playlists))
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(playlist = playlist)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun formatFollowers(count: Int?): String {
    if (count == null) return stringResource(Res.string.artist_followers_unavailable)
    return when {
        count >= 1_000_000_000 -> stringResource(Res.string.artist_followers_billions, formatAbbreviatedCount(count, 1_000_000_000))
        count >= 1_000_000 -> stringResource(Res.string.artist_followers_millions, formatAbbreviatedCount(count, 1_000_000))
        count >= 1_000 -> stringResource(Res.string.artist_followers_thousands, formatAbbreviatedCount(count, 1_000))
        else -> pluralStringResource(Res.plurals.artist_followers_count, count, count)
    }
}

@Composable
private fun formatMonthlyListeners(count: Int): String = when {
    count >= 1_000_000_000 -> stringResource(Res.string.artist_monthly_listeners_billions, formatAbbreviatedCount(count, 1_000_000_000))
    count >= 1_000_000 -> stringResource(Res.string.artist_monthly_listeners_millions, formatAbbreviatedCount(count, 1_000_000))
    count >= 1_000 -> stringResource(Res.string.artist_monthly_listeners_thousands, formatAbbreviatedCount(count, 1_000))
    else -> pluralStringResource(Res.plurals.artist_monthly_listeners_count, count, count)
}

private fun formatAbbreviatedCount(count: Int, divisor: Int): String {
    val scaled = count / divisor.toDouble()
    val rounded = (scaled * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
