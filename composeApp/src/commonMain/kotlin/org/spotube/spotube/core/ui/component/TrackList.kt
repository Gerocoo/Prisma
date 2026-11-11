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

package org.spotube.spotube.core.ui.component



import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.state.ToggleableState
import org.spotube.millennium.composables.MillenniumCheckbox
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.component.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.composables.MillenniumTriStateCheckbox

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.getPlatform
import org.spotube.spotube.isDesktop
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxFilterSearch
import org.spotube.spotube.resources.iconsax.IconsaxMusicPlaylist
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxSort
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

private val CompactTrackListBreakpoint = 600.dp
private val LargeTrackListBreakpoint = 840.dp
private const val ShimmerRowCount = 6

enum class TrackSortOption(val label: StringResource) {
    None(Res.string.track_list_sort_original),
    Title(Res.string.track_list_sort_title),
    Artist(Res.string.track_list_sort_artist),
    Album(Res.string.track_list_sort_album),
    Duration(Res.string.track_list_sort_duration),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackList(
    tracks: List<MetadataTrack> = emptyList(),
    error: String? = null,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
    isLoadingNextPage: Boolean = false,
    showEmptyMessage: Boolean = true,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    onLoadNextPage: () -> Unit = {},
    onTrackClick: (MetadataTrack) -> Unit = {},
    onTrackOptionsAction: (MetadataTrack, TrackOptionsAction) -> Unit = { _, _ -> },
    onArtistClick: (MetadataArtist.Basic) -> Unit = {},
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit = {},
    onArtistsOverflowClick: (MetadataTrack) -> Unit = {},
    onBulkDownload: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToQueue: (List<MetadataTrack>) -> Unit = {},
    onBulkPlayNext: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToPlaylist: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToJam: (List<MetadataTrack>) -> Unit = {},
    isInJam: Boolean = false,
    isJamGuest: Boolean = false,
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    trackOptionsState: (MetadataTrack) -> TrackOptionsState = { TrackOptionsState() },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = LocalAppShellBottomInset.current),
    simplified: Boolean = false,
    scrollable: Boolean = true,
    state: LazyListState? = null) {
    val filterState = rememberTextFieldState()
    val filterQuery = filterState.text.toString()
    var sortBy by rememberSaveable { mutableStateOf(TrackSortOption.None) }
    var selectedTrackForOptions by remember { mutableStateOf<MetadataTrack?>(null) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTrackIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }

    val normalizedQuery = remember(filterQuery) { filterQuery.trim().lowercase() }
    val visibleTracks = remember(tracks, normalizedQuery, sortBy) {
        val filtered = if (normalizedQuery.isBlank()) {
            tracks
        } else {
            tracks.filter { track ->
                val searchableText = buildString {
                    append(track.title).append(' ')
                    append(track.album?.title.orEmpty()).append(' ')
                    append(track.artists.joinToString(" ") { it.name })
                }.lowercase()
                searchableText.contains(normalizedQuery)
            }
        }

        when (sortBy) {
            TrackSortOption.None -> filtered
            TrackSortOption.Title -> filtered.sortedBy { it.title.lowercase() }
            TrackSortOption.Artist -> filtered.sortedBy {
                it.artists.firstOrNull()?.name?.lowercase().orEmpty()
            }

            TrackSortOption.Album -> filtered.sortedBy { it.album?.title?.lowercase() }
            TrackSortOption.Duration -> filtered.sortedBy { it.durationMs }
        }
    }

    val listState = state ?: rememberLazyListState()
    val density = LocalDensity.current
    val shouldLoadMore = remember(density) {
        derivedStateOf {
            val loadMoreThreshold = with(density) { 200.dp.toPx() }
            val cardItem = listState.layoutInfo.visibleItemsInfo.find { it.key == "track-card" }
            // Trigger when the bottom of the track card is close to/inside the viewport,
            // which means the user has scrolled through (or is about to scroll through)
            // all loaded tracks.
            cardItem != null && cardItem.offset + cardItem.size <= listState.layoutInfo.viewportEndOffset + loadMoreThreshold
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMore && !isLoading && !isLoadingNextPage) {
            onLoadNextPage()
        }
    }

    val widthBreakpoint = currentWindowWidthBreakpoint()
    val isCompact = widthBreakpoint isBelow WindowBreakpoints.Medium
    val isDesktop = remember { getPlatform().isDesktop() }
    val showIndex = widthBreakpoint isAtLeast WindowBreakpoints.Expanded
    val showAlbum = !isCompact
    val useDropdownForOptions = isDesktop && !isCompact

    val trackCardContent: @Composable () -> Unit = {
        // Each track is its own rounded tonal card, separated by a small gap (Expressive lists).
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visibleTracks.forEachIndexed { displayedIndex, track ->
                    TrackListRow(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(playingRowColor(track.id == currentTrackId)),
                        index = displayedIndex + 1,
                        track = track,
                        showIndex = showIndex,
                        showAlbum = showAlbum,
                        useDropdownForOptions = useDropdownForOptions,
                        isCurrentTrack = track.id == currentTrackId,
                        isCurrentTrackPlaying = isCurrentTrackPlaying,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedTrackIds.contains(track.id),
                        onTrackClick = {
                            if (isSelectionMode) {
                                selectedTrackIds =
                                    if (selectedTrackIds.contains(track.id)) {
                                        selectedTrackIds - track.id
                                    } else {
                                        selectedTrackIds + track.id
                                    }
                            } else if (isJamGuest) {
                                onTrackOptionsAction(track, TrackOptionsAction.AddToJam)
                            } else {
                                onTrackClick(track)
                            }
                        },
                        onLongClick = {
                            // Holding a row opens the same menu as its three dots.
                            if (!useDropdownForOptions && !isSelectionMode) {
                                selectedTrackForOptions = track
                            }
                        },
                        onSelectionToggle = { checked ->
                            isSelectionMode = true
                            selectedTrackIds = if (checked) {
                                selectedTrackIds + track.id
                            } else {
                                selectedTrackIds - track.id
                            }
                        },
                        onTrackOptionsAction = { action ->
                            onTrackOptionsAction(
                                track,
                                action
                            )
                        },
                        trackOptionsState = trackOptionsState(track),
                        isInJam = isInJam,
                        isJamGuest = isJamGuest,
                        onShowOptionsClick = { selectedTrackForOptions = track },
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                        onArtistsOverflowClick = { onArtistsOverflowClick(track) })
                }

                if (isLoading && tracks.isEmpty()) {
                    repeat(ShimmerRowCount) { shimmerIndex ->
                        if (visibleTracks.isNotEmpty() || shimmerIndex > 0) {
                            MillenniumSeparator(
                                modifier = Modifier.padding(horizontal = 8.dp))
                        }
                        ShimmerTrackListRow(
                            index = shimmerIndex + 1,
                            showIndex = showIndex,
                            showAlbum = showAlbum,
                            useDropdownForOptions = useDropdownForOptions)
                    }
                }

                if (error != null) {
                    if (visibleTracks.isNotEmpty()) {
                        MillenniumSeparator(
                            modifier = Modifier.padding(horizontal = 8.dp))
                    }
                    TrackListFeedbackRow(
                        message = error,
                        isError = true)
                }

                if (showEmptyMessage && !isLoading && visibleTracks.isEmpty() && error == null) {
                    TrackListFeedbackRow(stringResource(Res.string.track_list_no_tracks))
                }

                if (isLoadingNextPage) {
                    if (visibleTracks.isNotEmpty()) {
                        MillenniumSeparator(
                            modifier = Modifier.padding(horizontal = 8.dp))
                    }
                    ShimmerTrackListRow(
                        index = visibleTracks.size + 1,
                        showIndex = showIndex,
                        showAlbum = showAlbum,
                        useDropdownForOptions = useDropdownForOptions)
                }
            }
        }
    }

    val filterSortRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .heightIn(max = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
            if (showIndex || isSelectionMode) {
                val allSelected =
                    visibleTracks.isNotEmpty() && selectedTrackIds.size == visibleTracks.size
                val anySelected = selectedTrackIds.isNotEmpty()
                val headerState = when {
                    allSelected -> ToggleableState.On
                    anySelected -> ToggleableState.Indeterminate
                    else -> ToggleableState.Off
                }
                MillenniumTriStateCheckbox(
                    value = headerState,
                    onClick = {
                        if (allSelected) {
                            isSelectionMode = false
                            selectedTrackIds = emptySet()
                        } else {
                            isSelectionMode = true
                            selectedTrackIds = visibleTracks.map { it.id }.toSet()
                        }
                    },
                    label = "")
            }

            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                ExpressiveSearchField(
                    state = filterState,
                    placeholder = stringResource(Res.string.track_list_filter_placeholder),
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .align(Alignment.CenterEnd))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center) {
                    AdaptiveDropdownBottomSheet(
                        items = TrackSortOption.entries.map { option ->
                            AdaptiveMenuItem(
                                label = stringResource(option.label),
                                onClick = { sortBy = option },
                                selected = sortBy == option)
                        },
                        trigger = { onClick ->
                            MillenniumIconButton(
                                onClick = onClick,
                                style = MillenniumIconButtonStyle.Quiet) {
                                MillenniumIcon(
                                    imageVector = Iconsax.IconsaxSort,
                                    contentDescription = stringResource(Res.string.track_list_sort))
                            }
                        })
                }
                MillenniumSeparator(modifier = Modifier.height(24.dp), vertical = true)
                Box(
                    contentAlignment = Alignment.Center) {
                    val targetTracks = if (selectedTrackIds.isNotEmpty()) {
                        visibleTracks.filter { selectedTrackIds.contains(it.id) }
                    } else {
                        visibleTracks
                    }
                    val trackCount = targetTracks.size
                    val isAll =
                        selectedTrackIds.isEmpty() || trackCount == visibleTracks.size
                    AdaptiveDropdownBottomSheet(
                        items = buildList {
                            add(
                                AdaptiveMenuItem(
                                    icon = Iconsax.IconsaxDirectboxReceive,
                                    label = if (isAll) stringResource(Res.string.track_list_download_all) else stringResource(Res.string.track_list_download_count, trackCount),
                                    onClick = { onBulkDownload(targetTracks) }))
                            if (!isJamGuest) {
                                add(
                                    AdaptiveMenuItem(
                                        icon = Iconsax.IconsaxAddSquare,
                                        label = if (isAll) stringResource(Res.string.track_list_add_all_to_queue) else stringResource(Res.string.track_list_add_count_to_queue, trackCount),
                                        onClick = { onBulkAddToQueue(targetTracks) }))
                                add(
                                    AdaptiveMenuItem(
                                        icon = Iconsax.IconsaxNext,
                                        label = if (isAll) stringResource(Res.string.track_list_play_all_next) else stringResource(Res.string.track_list_play_count_next, trackCount),
                                        onClick = { onBulkPlayNext(targetTracks) }))
                            }
                            add(
                                AdaptiveMenuItem(
                                    icon = Iconsax.IconsaxMusicPlaylist,
                                    label = if (isAll) stringResource(Res.string.track_list_add_all_to_playlist) else stringResource(Res.string.track_list_add_count_to_playlist, trackCount),
                                    onClick = { onBulkAddToPlaylist(targetTracks) }))
                        } + if (isInJam) {
                            listOf(
                                AdaptiveMenuItem(
                                    icon = Iconsax.IconsaxAddSquare,
                                    label = if (isAll) stringResource(Res.string.track_list_add_all_to_jam) else stringResource(Res.string.track_list_add_count_to_jam, trackCount),
                                    onClick = { onBulkAddToJam(targetTracks) }))
                        } else {
                            emptyList()
                        },
                        trigger = { onClick ->
                            MillenniumIconButton(
                                onClick = onClick,
                                style = MillenniumIconButtonStyle.Quiet) {
                                MillenniumIcon(
                                    imageVector = Iconsax.Iconsax3DotsMore,
                                    contentDescription = stringResource(Res.string.track_list_bulk_actions))
                            }
                        })
                }
            }
        }
    }

    if (scrollable) {
        Box(modifier = modifier.fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 1280.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = if (isCompact) 6.dp else 16.dp, vertical = 8.dp),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (headerContent != null) {
                    item {
                        headerContent()
                    }
                }
                if (!simplified) {
                    item { filterSortRow() }
                }
                item(key = "track-card") { trackCardContent() }
                if (footerContent != null) {
                    item {
                        footerContent()
                    }
                }
            }

            if (!useDropdownForOptions) {
                selectedTrackForOptions?.let { track ->
                    TrackOptionsBottomSheet(
                        track = track,
                        state = trackOptionsState(track),
                        onDismiss = { selectedTrackForOptions = null },
                        onAction = { action ->
                            onTrackOptionsAction(track, action)
                            selectedTrackForOptions = null
                        },
                        onAlbumClick = { track.album?.let { onAlbumClick(it) } },
                        onArtistClick = onArtistClick,
                        onSelect = {
                            isSelectionMode = true
                            selectedTrackIds = setOf(track.id)
                            selectedTrackForOptions = null
                        },
                        isInJam = isInJam,
                        isJamGuest = isJamGuest)
                }
            }
            VerticalScrollbar(listState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 6.dp else 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (headerContent != null) {
                headerContent()
            }
            if (!simplified) {
                filterSortRow()
            }
            trackCardContent()
            if (footerContent != null) {
                footerContent()
            }
        }

        if (!useDropdownForOptions) {
            selectedTrackForOptions?.let { track ->
                TrackOptionsBottomSheet(
                    track = track,
                    state = trackOptionsState(track),
                    onDismiss = { selectedTrackForOptions = null },
                    onAction = { action ->
                        onTrackOptionsAction(track, action)
                        selectedTrackForOptions = null
                    },
                    onAlbumClick = { track.album?.let { onAlbumClick(it) } },
                    onArtistClick = onArtistClick,
                    onSelect = {
                        isSelectionMode = true
                        selectedTrackIds = setOf(track.id)
                        selectedTrackForOptions = null
                    },
                    isInJam = isInJam,
                    isJamGuest = isJamGuest)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackListRow(
    index: Int,
    track: MetadataTrack,
    showIndex: Boolean,
    showAlbum: Boolean,
    useDropdownForOptions: Boolean,
    isCurrentTrack: Boolean,
    isCurrentTrackPlaying: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTrackClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectionToggle: (Boolean) -> Unit,
    onTrackOptionsAction: (TrackOptionsAction) -> Unit,
    trackOptionsState: TrackOptionsState,
    isInJam: Boolean,
    isJamGuest: Boolean,
    onShowOptionsClick: () -> Unit,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit,
    onArtistsOverflowClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowHovered by rowInteractionSource.collectIsHoveredAsState()
    val rowBackgroundColor = when {
        isSelected -> AppTheme.colors.secondary.copy(alpha = 0.12f)
        // The current track is marked by its accent title, not by a filled row.
        trackOptionsState.isBlacklisted -> AppTheme.colors.error.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val haptics = LocalHapticFeedback.current
    MillenniumListTile(
        modifier = modifier.longPressMenu {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        },
        selected = isSelected,
        onClick = onTrackClick,
        title = {
            TextWithShimmer(
                text = track.title,
                style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = AppTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        },
        subtitle = {
            // Plain text: a tap anywhere on the row plays the track. The artist page is reached
            // from the row's menu and from the player.
            TextWithShimmer(
                text = track.artists.joinToString(", ") { it.name },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        },
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isSelectionMode) {
                    MillenniumCheckbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionToggle(it) },
                        label = "",
                        modifier = Modifier.shimmerApply())
                } else if (showIndex) {
                    Box(
                        modifier = Modifier.width(30.dp),
                        contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isRowHovered,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "index-checkbox") { hovered ->
                            if (hovered) {
                                MillenniumCheckbox(
                                    checked = isSelected,
                                    onCheckedChange = { onSelectionToggle(it) },
                                    label = "",
                                    modifier = Modifier.shimmerApply())
                            } else {
                                TextWithShimmer(
                                    text = index.toString(),
                                    style = AppTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    color = AppTheme.colors.onSurfaceVariant)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AppTheme.shapes.small)
                        .hoverable(interactionSource = artworkInteractionSource),
                    contentAlignment = Alignment.Center) {
                    val imageUrl = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url
                    val platformContext = LocalPlatformContext.current
                    val imageRequest = remember(imageUrl) {
                        ImageRequest.Builder(platformContext)
                            .data(imageUrl)
                            .size(128)
                            .crossfade(false)
                            .build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().shimmerApply())

                    if (isArtworkHovered || isCurrentTrack) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center) {
                            MillenniumIcon(
                                imageVector = if (isCurrentTrack && isCurrentTrackPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                                contentDescription = if (isCurrentTrack && isCurrentTrackPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp).shimmerApply())
                        }
                    }
                }
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showAlbum && track.album != null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val colorScheme = AppTheme.colors

                    TextWithShimmer(
                        text = track.album!!.title,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .hoverable(interactionSource)
                            .clickable(
                                indication = null,
                                interactionSource = interactionSource) { onAlbumClick(track.album!!) }
                            .then(
                                if (isHovered || isPressed) {
                                    Modifier.drawWithCache {
                                        val underlineHeight = 1.dp.toPx()
                                        onDrawWithContent {
                                            drawContent()
                                            drawLine(
                                                color = colorScheme.primary,
                                                start = Offset(0f, size.height - underlineHeight),
                                                end = Offset(size.width, size.height - underlineHeight),
                                                strokeWidth = underlineHeight)
                                        }
                                    }
                                } else {
                                    Modifier
                                }))
                }

                TextWithShimmer(
                    text = track.durationMs.toDurationString(),
                    style = AppTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.width(56.dp))

                if (useDropdownForOptions) {
                    TrackOptions(
                        track = track,
                        state = trackOptionsState,
                        onAction = onTrackOptionsAction,
                        onAlbumClick = { track.album?.let { onAlbumClick(it) } },
                        onArtistClick = onArtistClick,
                        isInJam = isInJam,
                        isJamGuest = isJamGuest)
                } else {
                    // Playing marker: animated bars next to a filled options button.
                    AnimatedVisibility(
                        // Only while actually playing: on pause the marker goes away.
                        visible = isCurrentTrack && isCurrentTrackPlaying,
                        enter = fadeIn() + scaleIn(initialScale = 0.6f),
                        exit = fadeOut() + scaleOut(targetScale = 0.6f)
                    ) {
                        PlayingBars(
                            playing = isCurrentTrackPlaying,
                            color = AppTheme.colors.onSurface,
                            modifier = Modifier.size(width = 18.dp, height = 16.dp)
                        )
                    }
                    val optionsBg by animateColorAsState(
                        if (isCurrentTrack) AppTheme.colors.onSurface else Color.Transparent, label = "optionsBg"
                    )
                    val optionsFg by animateColorAsState(
                        if (isCurrentTrack) AppTheme.colors.surface else AppTheme.colors.onSurface, label = "optionsFg"
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(optionsBg)
                            .clickable(onClick = onShowOptionsClick),
                        contentAlignment = Alignment.Center
                    ) {
                        MillenniumIcon(
                            imageVector = Iconsax.Iconsax3DotsMore,
                            contentDescription = stringResource(Res.string.track_options_title),
                            tint = optionsFg,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        })
}

/**
 * Long press that works on top of a clickable row: it watches the gesture before the row does and,
 * once the press has lasted long enough, swallows the rest so the row's own click does not fire.
 */
private fun Modifier.longPressMenu(onLongPress: () -> Unit): Modifier = pointerInput(onLongPress) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var ended = false
        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation(PointerEventPass.Initial)
            ended = true
        }
        if (!ended) {
            onLongPress()
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

@Composable
private fun ShimmerTrackListRow(
    index: Int,
    showIndex: Boolean,
    showAlbum: Boolean,
    useDropdownForOptions: Boolean) {
    val dummyTrack = remember(index) { dummyTrackForShimmer(index) }
    SkeletonTree(true) {
        TrackListRow(
            index = index,
            track = dummyTrack,
            showIndex = showIndex,
            showAlbum = showAlbum,
            useDropdownForOptions = useDropdownForOptions,
            isCurrentTrack = false,
            isCurrentTrackPlaying = false,
            isSelectionMode = false,
            isSelected = false,
            onTrackClick = {},
            onLongClick = {},
            onSelectionToggle = {},
            onTrackOptionsAction = {},
            trackOptionsState = TrackOptionsState(),
            isInJam = false,
            isJamGuest = false,
            onShowOptionsClick = {},
            onArtistClick = {},
            onAlbumClick = {},
            onArtistsOverflowClick = {})
    }
}

private fun dummyTrackForShimmer(index: Int): MetadataTrack {
    val artist = MetadataArtist.Basic(
        id = "shimmer_artist_$index",
        name = "Artist Name",
        thumbnails = emptyList(),
        externalUri = null)
    return MetadataTrack(
        id = "shimmer_track_$index",
        title = "Track Title Here",
        durationMs = 210_000,
        trackNumber = index,
        discNumber = 1,
        artists = listOf(
            artist,
            artist.copy(id = "shimmer_artist_${index}_2", name = "Another Artist")
        ),
        album = MetadataAlbum.Detailed(
            releaseDate = null,
            genres = emptyList(),
            trackCount = 10,
            id = "shimmer_album_$index",
            title = "Album Name",
            description = null,
            thumbnails = emptyList(),
            albumType = MetadataAlbumType.Album,
            artists = listOf(artist),
            externalUri = null),
        thumbnails = null,
        explicit = false,
        popularity = 0,
        isrcCode = null,
        externalUri = null)
}

@Composable
private fun TrackListFeedbackRow(
    message: String,
    isError: Boolean = false) {
    TextWithShimmer(
        text = message,
        color = if (isError) AppTheme.colors.error else AppTheme.colors.onSurfaceVariant,
        style = AppTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp))
}

private fun Long.toDurationString(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun previewTracks(): List<MetadataTrack> {
    val artists = listOf(
        MetadataArtist.Basic(
            id = "artist_1",
            name = "Aria Vale",
            thumbnails = emptyList(),
            externalUri = null),
        MetadataArtist.Basic(
            id = "artist_2",
            name = "Neon Harbor",
            thumbnails = emptyList(),
            externalUri = null),
        MetadataArtist.Basic(
            id = "artist_3",
            name = "Echo Drift",
            thumbnails = emptyList(),
            externalUri = null))

    val album = MetadataAlbum.Detailed(
        releaseDate = null,
        genres = emptyList(),
        trackCount = 10,
        id = "album_1",
        title = "Night Drive Archives",
        description = null,
        thumbnails = listOf(
            Thumbnail(
                url = "https://picsum.photos/120",
                width = 120,
                height = 120)
        ),
        albumType = MetadataAlbumType.Album,
        artists = artists.take(2),
        externalUri = null)

    return listOf(
        MetadataTrack(
            id = "track_1",
            title = "Signal Bloom",
            durationMs = 198_000,
            trackNumber = 1,
            discNumber = 1,
            artists = artists,
            album = album,
            thumbnails = null,
            explicit = false,
            popularity = 92,
            isrcCode = null,
            externalUri = null),
        MetadataTrack(
            id = "track_2",
            title = "Static Horizon",
            durationMs = 224_000,
            trackNumber = 2,
            discNumber = 1,
            artists = artists.take(2),
            album = album.copy(id = "album_2", title = "City Pulse"),
            explicit = false,
            popularity = 80,
            isrcCode = null,
            externalUri = null,
            thumbnails = null),
        MetadataTrack(
            id = "track_3",
            title = "Last Train Echo",
            durationMs = 246_000,
            trackNumber = 3,
            discNumber = 1,
            artists = artists.take(1),
            album = album.copy(id = "album_3", title = "Afterlight"),
            explicit = false,
            popularity = 77,
            isrcCode = null,
            externalUri = null,
            thumbnails = null))
}

@Composable
@Preview
private fun TrackListPreview() {
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        TrackList(
            tracks = previewTracks(),
            hasMore = true,
            isLoadingNextPage = true)
    }
}

/** Row fill: the playing track glows in the accent tint, the others stay on the tonal surface. */
@Composable
private fun playingRowColor(isCurrent: Boolean): Color {
    val base = AppTheme.colors.surfaceContainer
    val color by animateColorAsState(
        if (isCurrent) lerp(base, AppTheme.colors.primary, 0.32f) else base,
        tween(350),
        label = "playingRow"
    )
    return color
}

/** Three equalizer bars bouncing while the track plays, resting low when paused. */
@Composable
internal fun PlayingBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "playingBars")
    val phases = listOf(0, 180, 360).map { delay ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(520, delayMillis = delay, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "bar$delay"
        )
    }
    Canvas(modifier) {
        val barWidth = size.width / 5f
        phases.forEachIndexed { i, phase ->
            val h = size.height * (if (playing) phase.value else 0.3f)
            val x = barWidth * (i * 2) + barWidth / 2f
            drawLine(
                color = color,
                start = Offset(x, size.height),
                end = Offset(x, size.height - h),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
