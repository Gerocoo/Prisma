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


import org.spotube.spotube.core.ui.component.ExpressivePullToRefreshBox
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.TrackCard
import org.spotube.spotube.core.ui.component.UserCard
import org.spotube.spotube.core.ui.component.FeaturedCarousel
import org.spotube.spotube.core.ui.component.VerticalScrollbar
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.dragScrollable
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.shell.ConnectStatusBanner
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.support.LocalSupportPrompt
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import org.spotube.spotube.modules.recap.RecapSchedule
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import spotube.composeapp.generated.resources.recap_title
import spotube.composeapp.generated.resources.recap_entry_subtitle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.spotube.resources.iconsax.User
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import spotube.composeapp.generated.resources.saved_tracks_title
import spotube.composeapp.generated.resources.settings_screen_title
import org.spotube.spotube.resources.iconsax.IconsaxSetting2
import spotube.composeapp.generated.resources.support_button_label
import spotube.composeapp.generated.resources.devices_title
import spotube.composeapp.generated.resources.home_title_browse
import spotube.composeapp.generated.resources.jam_screen_title
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.Font
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import spotube.composeapp.generated.resources.cookie_regular
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.home_greeting_afternoon
import spotube.composeapp.generated.resources.home_greeting_evening
import spotube.composeapp.generated.resources.home_greeting_morning

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel) {
    val navigationCommands = koinInject<NavigationCommands>()
    val openSupport = LocalSupportPrompt.current
    val isSmallScreen = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val selectedGenreId = when (val s = state) {
        is HomeScreenState.Data -> s.selectedGenreId
        else -> null
    }

    LaunchedEffect(selectedGenreId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (
                    lastVisibleIndex != null &&
                    totalItems > 0 &&
                    lastVisibleIndex >= totalItems - 2
                ) {
                    viewModel.loadMoreData()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MillenniumTheme.colors.background)
    ) {
        val quickAccess by viewModel.quickAccess.collectAsStateWithLifecycle()
        val data = state as? HomeScreenState.Data
        if (isSmallScreen) {
            // Phone header: round profile button (opens Settings) followed by the genre chips.
            CompactHomeHeader(
                genres = data?.genres.orEmpty(),
                selectedGenreId = data?.selectedGenreId,
                onGenreSelected = { viewModel.selectGenre(it) },
                onProfileClick = { navigationCommands.navigateTo(Routes.Settings) }
            )
        } else {
            ApplicationMainBar(
                backButton = false,
                title = {
                    if (isSmallScreen) {
                        val dayPart = remember { currentDayPart() }
                        Text(
                            stringResource(dayPart.greeting),
                            // Big expressive heading, like a magazine cover title.
                            style = AppTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 44.sp),
                            color = AppTheme.colors.onSurface
                        )
                    } else {
                        Text(stringResource(Res.string.home_title_browse))
                    }

                },
                actions = {
                    if (isSmallScreen) {
                        // Settings left the bottom bar: reach them from the Home header.
                        MillenniumIconButton(
                            onClick = { navigationCommands.navigateTo(Routes.Settings) },
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxSetting2,
                                contentDescription = stringResource(Res.string.settings_screen_title)
                            )
                        }
                        MillenniumIconButton(
                            onClick = { navigationCommands.navigateTo(Routes.Devices) },
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxMirrorScreenRegular,
                                contentDescription = stringResource(Res.string.devices_title)
                            )
                        }
                        MillenniumIconButton(
                            onClick = { navigationCommands.navigateTo(Routes.Jam) },
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxSoundTwotone,
                                contentDescription = stringResource(Res.string.jam_screen_title)
                            )
                        }
                        val supportLabel = stringResource(Res.string.support_button_label)
                        MillenniumIconButton(
                            onClick = openSupport,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxHeart2,
                                contentDescription = supportLabel
                            )
                        }
                    }
                })
        }
        if (isSmallScreen) {
            ConnectStatusBanner(
                onOpenRemote = { navigationCommands.navigateTo(Routes.RemoteControl) },
                onOpenJam = { navigationCommands.navigateTo(Routes.Jam) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (!isSmallScreen) {
                HomeContent(
                    listState = listState,
                    state = state,
                    quickAccess = quickAccess,
                    compact = isSmallScreen,
                    onRetry = { viewModel.refresh() },
                    onGenreSelected = { viewModel.selectGenre(it) })
            } else {
                ExpressivePullToRefreshBox(
                    isRefreshing = state is HomeScreenState.Loading,
                    onRefresh = {
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeContent(
                        listState = listState,
                        state = state,
                        quickAccess = quickAccess,
                        compact = isSmallScreen,
                        onRetry = { viewModel.refresh() },
                        onGenreSelected = { viewModel.selectGenre(it) })
                }
            }

            VerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun HomeContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    state: HomeScreenState,
    quickAccess: List<MetadataPlaylist> = emptyList(),
    compact: Boolean = false,
    onRetry: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (state) {
            is HomeScreenState.Loading -> {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(3) {
                            SkeletonTree(true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .padding(horizontal = 16.dp)
                                    )
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
            }

            is HomeScreenState.Error -> {
                item {
                    ErrorDisplay(
                        errorMessage = state.message,
                        onRetry = onRetry
                    )
                }
            }

            is HomeScreenState.Data -> {
                if (compact) {
                    item { QuickAccessGrid(playlists = quickAccess) }
                }
                // Shown only during the recap season (from early December to the end of January).
                RecapSchedule.activeYear()?.let { year -> item { RecapBanner(year) } }
                if (state.featuredItems.isNotEmpty()) {
                    item {
                        FeaturedCarousel(items = state.featuredItems)
                    }
                }

                if (!compact && state.genres.isNotEmpty()) {
                    item {
                        GenreTabs(
                            genres = state.genres,
                            selectedGenreId = state.selectedGenreId,
                            onGenreSelected = onGenreSelected
                        )
                    }
                }

                val currentSections = state.selectedGenreId?.let { state.browseSections[it] }

                if (currentSections == null) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp)
                                )
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
                } else {
                    items(currentSections) { section ->
                        HomeSection(
                            title = section.title,
                            subtitle = section.description,
                            items = section.items
                        )
                    }
                }

                if (state is HomeScreenState.Data.LoadingMore) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp)
                                )
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
        }

    }
}

@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val rowState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!subtitle.isNullOrEmpty()) Text(
            text = subtitle,
            style = AppTheme.typography.labelMedium.copy(color = AppTheme.colors.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 16.dp).offset(y = 8.dp)
        )
        Text(
            text = title,
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            state = rowState,
            modifier = Modifier.dragScrollable(rowState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(items) { browseItem ->
                when (browseItem) {
                    is MetadataBrowseItem.Album -> AlbumCard(album = browseItem.data)
                    is MetadataBrowseItem.Artist -> ArtistCard(artist = browseItem.data)
                    is MetadataBrowseItem.Playlist -> PlaylistCard(playlist = browseItem.data)
                    is MetadataBrowseItem.Track -> TrackCard(track = browseItem.data)
                    is MetadataBrowseItem.User -> UserCard(user = browseItem.data)
                }
            }
        }
    }
}

@Composable
private fun GenreTabs(
    genres: List<MetadataBrowseGenre>,
    selectedGenreId: String?,
    onGenreSelected: (String) -> Unit
) {
    val rowState = rememberLazyListState()

    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            GenreChip(
                label = genre.name,
                selected = selectedGenreId == genre.id,
                onClick = { onGenreSelected(genre.id) }
            )
        }
    }
}

/** Pill-shaped filter chip: accent fill when selected, raised neutral otherwise. */
@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) AppTheme.colors.primary else AppTheme.colors.surfaceContainerHigh)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelLarge,
            color = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.onSurface
        )
    }
}

private enum class DayPart(val greeting: StringResource) {
    Morning(Res.string.home_greeting_morning),
    Afternoon(Res.string.home_greeting_afternoon),
    Evening(Res.string.home_greeting_evening)
}

private fun currentDayPart(): DayPart {
    val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return when (hour) {
        in 5..11 -> DayPart.Morning
        in 12..17 -> DayPart.Afternoon
        else -> DayPart.Evening
    }
}

/** Phone Home header: profile circle (Settings) + scrolling genre chips on one row. */
@Composable
private fun CompactHomeHeader(
    genres: List<MetadataBrowseGenre>,
    selectedGenreId: String?,
    onGenreSelected: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val rowState = rememberLazyListState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val settingsLabel = stringResource(Res.string.settings_screen_title)
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary)
                .clickable(role = Role.Button, onClickLabel = settingsLabel, onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                imageVector = Iconsax.User,
                contentDescription = settingsLabel,
                tint = AppTheme.colors.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        LazyRow(
            state = rowState,
            modifier = Modifier.weight(1f).dragScrollable(rowState),
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                GenreChip(
                    label = genre.name,
                    selected = selectedGenreId == genre.id,
                    onClick = { onGenreSelected(genre.id) }
                )
            }
        }
    }
}

/**
 * Two-column grid of shortcuts to the user's own music: "Liked songs" first, then up to seven
 * of their playlists. Each tile is cover + title on a rounded tonal surface.
 */
@Composable
private fun QuickAccessGrid(
    playlists: List<MetadataPlaylist>,
    navigationCommands: NavigationCommands = koinInject()
) {
    val likedTitle = stringResource(Res.string.saved_tracks_title)
    val tiles: List<@Composable (Modifier) -> Unit> = buildList {
        add { m ->
            QuickAccessTile(
                title = likedTitle,
                imageUrl = null,
                onClick = { navigationCommands.navigateTo(Routes.SavedTracks) },
                modifier = m,
                placeholder = {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxHeart2,
                        contentDescription = null,
                        tint = AppTheme.colors.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )
        }
        playlists.forEach { playlist ->
            add { m ->
                QuickAccessTile(
                    title = playlist.title,
                    imageUrl = playlist.thumbnails.firstOrNull()?.url,
                    onClick = { navigationCommands.navigateTo(Routes.Playlist(playlist.id)) },
                    modifier = m
                )
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile -> tile(Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = {
        MillenniumIcon(Iconsax.IconsaxMusic, contentDescription = null, tint = AppTheme.colors.onSurfaceVariant)
    }
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(if (imageUrl == null) AppTheme.colors.primary else AppTheme.colors.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) else placeholder()
        }
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

/** Entry to the yearly listening recap ("Your <year> in music"). */
@Composable
private fun RecapBanner(year: Int, navigationCommands: NavigationCommands = koinInject()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFB224), Color(0xFFFF5A36), Color(0xFF6C5CE7))))
            .clickable(role = Role.Button) { navigationCommands.navigateTo(Routes.Recap) }
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(Res.string.recap_title, year),
            style = AppTheme.typography.headlineSmall,
            color = Color.White
        )
        Text(
            text = stringResource(Res.string.recap_entry_subtitle),
            style = AppTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

