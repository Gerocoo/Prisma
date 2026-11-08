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

package org.spotube.spotube.modules.shell.player_queue


import androidx.compose.ui.graphics.lerp
import spotube.composeapp.generated.resources.queue_find_current
import spotube.composeapp.generated.resources.queue_filter_chip
import spotube.composeapp.generated.resources.queue_tracks_count
import spotube.composeapp.generated.resources.queue_up_next
import org.spotube.spotube.resources.iconsax.InconsaxClock
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxLocation
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.modules.shell.rememberPlayerUiState
import org.spotube.spotube.modules.shell.PlayerOptionsViewModel
import org.spotube.spotube.core.ui.component.PlayingBars
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import org.spotube.spotube.core.ui.component.ExpressiveSearchField
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.spotube.spotube.core.jam.JamParticipant
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxDragHandle
import org.spotube.spotube.resources.iconsax.IconsaxMusicSquareRemove
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import org.spotube.spotube.resources.iconsax.IconsaxUserRemove
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.component.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.composables.MillenniumTooltip
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.queue_clear_queue
import spotube.composeapp.generated.resources.queue_filter_queue
import spotube.composeapp.generated.resources.queue_no_queue_entries
import spotube.composeapp.generated.resources.queue_title
import spotube.composeapp.generated.resources.jam_participants_ban
import spotube.composeapp.generated.resources.jam_participants_host
import spotube.composeapp.generated.resources.player_more_options_content_description
import spotube.composeapp.generated.resources.queue_item_subtitle_added_by
import spotube.composeapp.generated.resources.queue_participant_ban_description
import spotube.composeapp.generated.resources.queue_participant_kick
import spotube.composeapp.generated.resources.queue_participant_kick_description
import spotube.composeapp.generated.resources.queue_participant_remove_suggestions
import spotube.composeapp.generated.resources.queue_participant_remove_suggestions_description
import spotube.composeapp.generated.resources.queue_remove_from_queue
import spotube.composeapp.generated.resources.queue_reorder_content_description

@Composable
fun PlayerQueueContent(
    viewModel: PlayerQueueContentViewModel = koinViewModel<PlayerQueueContentViewModel>(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.queueContentUiState.collectAsState()
    val displayItems = state.displayItems
    val filterQuery = state.filterQuery
    val isFiltered = state.isFiltered
    val isReadOnly = state.isReadOnly
    var selectedParticipant by remember { mutableStateOf<JamParticipant?>(null) }
    var showFilter by remember { mutableStateOf(filterQuery.isNotEmpty()) }
    val scope = rememberCoroutineScope()

    val audioPlayer = koinInject<AudioPlayerInterface>()
    val audioPlayerQueue = koinInject<AudioPlayerQueue>()
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val playerOptionsViewModel: PlayerOptionsViewModel = koinViewModel()

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState,
        onMove = { from, to ->
            if (isFiltered || isReadOnly) return@rememberReorderableLazyListState
            viewModel.onMove(from.index, to.index)
        })

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: big "Up next" title, track count, and a chip that shows the filter field.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.queue_up_next),
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.queue_tracks_count, displayItems.size),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (showFilter) AppTheme.colors.primary else AppTheme.colors.surfaceContainerHigh)
                        .clickable(role = Role.Button) {
                            showFilter = !showFilter
                            if (!showFilter) viewModel.setQueueFilter("")
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chipColor = if (showFilter) AppTheme.colors.onPrimary else AppTheme.colors.onSurface
                    MillenniumIcon(Iconsax.IconsaxSearch, null, modifier = Modifier.size(16.dp), tint = chipColor)
                    Text(stringResource(Res.string.queue_filter_chip), style = AppTheme.typography.labelLarge, color = chipColor)
                }
            }

            AnimatedVisibility(visible = showFilter) {
                val filterState = rememberTextFieldState(filterQuery)
                LaunchedEffect(filterState.text) {
                    viewModel.setQueueFilter(filterState.text.toString())
                }
                ExpressiveSearchField(
                    state = filterState,
                    placeholder = stringResource(Res.string.queue_filter_queue),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (displayItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.queue_no_queue_entries),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Room for the floating toolbar at the bottom.
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(displayItems, key = { it.id }) { item ->
                        ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                            // The dragged row lifts: it grows a little and casts a shadow, while
                            // the rows it passes slide out of the way.
                            val lift by animateFloatAsState(
                                if (isDragging) 1f else 0f,
                                spring(dampingRatio = 0.6f, stiffness = 500f),
                                label = "queueLift"
                            )
                            Box(
                                Modifier
                                    .graphicsLayer {
                                        val scale = 1f + 0.04f * lift
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .shadow((10 * lift).dp, RoundedCornerShape(28.dp), clip = false)
                            ) {
                                QueueItemRow(
                                    item = item,
                                    isPlaying = playerUiState.isPlaying,
                                    dragging = isDragging,
                                    reorderScope = if (isFiltered || isReadOnly) null else this@ReorderableItem,
                                    onPlayClick = { viewModel.playQueueItem(item.originalIndex) },
                                    onRemoveClick = { viewModel.removeQueueItem(item.originalIndex) },
                                    onDragStarted = { viewModel.onDragStart() },
                                    onDragStopped = { viewModel.onDragStop() },
                                    showOptions = !isReadOnly,
                                    enabled = !isReadOnly,
                                    onParticipantClick = { selectedParticipant = it })
                            }
                        }
                    }
                }
            }

            selectedParticipant?.let { participant ->
                ParticipantDialog(
                    participant = participant,
                    isJamHost = state.isJamHost,
                    onDismiss = { selectedParticipant = null },
                    onKick = { viewModel.kickParticipant(participant.id) },
                    onBan = { viewModel.banParticipant(participant.id) },
                    onRemoveSuggestions = { viewModel.removeParticipantTracks(participant.id) })
            }
        }

        // Floating Expressive toolbar: shuffle / repeat / sleep timer in a pill, with a separate
        // button that unfolds the queue actions above it.
        QueueToolbar(
            isShuffling = playerUiState.isShuffling,
            loopState = playerUiState.loopState,
            enabled = !isReadOnly,
            onShuffle = { scope.launch { audioPlayer.shuffle(!playerUiState.isShuffling) } },
            onLoop = { scope.launch { audioPlayer.loop(playerUiState.loopState.next()) } },
            onSleepTimer = playerOptionsViewModel::showSleepTimerDialog,
            onFindCurrent = {
                val index = displayItems.indexOfFirst { it.isCurrent }
                if (index >= 0) scope.launch { lazyListState.animateScrollToItem(index) }
            },
            onClear = viewModel::clearQueue,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun QueueToolbar(
    isShuffling: Boolean,
    loopState: LoopState,
    enabled: Boolean,
    onShuffle: () -> Unit,
    onLoop: () -> Unit,
    onSleepTimer: () -> Unit,
    onFindCurrent: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Actions unfold from the button as a stack of pills.
        AnimatedVisibility(
            visible = menuOpen,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) +
                scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(1f, 1f)),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) +
                scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(1f, 1f))
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QueueMenuPill(Iconsax.IconsaxLocation, stringResource(Res.string.queue_find_current)) {
                    menuOpen = false; onFindCurrent()
                }
                if (enabled) QueueMenuPill(Iconsax.IconsaxTrash, stringResource(Res.string.queue_clear_queue), danger = true) {
                    menuOpen = false; onClear()
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(50), clip = false)
                    .clip(RoundedCornerShape(50))
                    .background(AppTheme.colors.surfaceContainerHigh)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarToggle(Iconsax.IconsaxShuffle, isShuffling, enabled, onShuffle)
                ToolbarToggle(
                    if (loopState == LoopState.ONE) Iconsax.IconsaxRepeateOne else Iconsax.IconsaxRepeateMusic,
                    loopState != LoopState.NONE, enabled, onLoop
                )
                ToolbarToggle(Iconsax.InconsaxClock, false, true, onSleepTimer)
            }
            val fabCorner by animateDpAsState(if (menuOpen) 28.dp else 18.dp, label = "fabCorner")
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 56.dp)
                    .shadow(6.dp, RoundedCornerShape(fabCorner), clip = false)
                    .clip(RoundedCornerShape(fabCorner))
                    .background(if (menuOpen) AppTheme.colors.primary else accentTonal())
                    .clickable(role = Role.Button) { menuOpen = !menuOpen },
                contentAlignment = Alignment.Center
            ) {
                val rotation by animateFloatAsState(if (menuOpen) 90f else 0f, label = "fabRotation")
                MillenniumIcon(
                    Iconsax.Iconsax3DotsMore,
                    contentDescription = stringResource(Res.string.player_more_options_content_description),
                    tint = if (menuOpen) AppTheme.colors.onPrimary else AppTheme.colors.onSurface,
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

@Composable
private fun ToolbarToggle(icon: ImageVector, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) AppTheme.colors.primary else Color.Transparent, label = "toggleBg")
    val fg by animateColorAsState(if (active) AppTheme.colors.onPrimary else AppTheme.colors.onSurface, label = "toggleFg")
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(icon, null, modifier = Modifier.size(20.dp), tint = fg)
    }
}

@Composable
private fun QueueMenuPill(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val bg = if (danger) AppTheme.colors.error.copy(alpha = 0.18f) else accentTonal()
    val fg = if (danger) AppTheme.colors.error else AppTheme.colors.onSurface
    Row(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillenniumIcon(icon, null, modifier = Modifier.size(18.dp), tint = fg)
        Text(label, style = AppTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun ParticipantDialog(
    participant: JamParticipant,
    isJamHost: Boolean,
    onDismiss: () -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit,
    onRemoveSuggestions: () -> Unit
) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { Text(participant.displayName, style = AppTheme.typography.titleLarge) }) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                ParticipantAvatar(participant, size = 40)
                Text(
                    text = participant.displayName,
                    style = AppTheme.typography.bodyLarge
                )
                if (participant.isHost) {
                    Text(
                        text = stringResource(Res.string.jam_participants_host),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.primary
                    )
                }
            }

            if (isJamHost && !participant.isHost) {
                MillenniumSeparator(
                    modifier = Modifier.fillMaxWidth()
                )
                MillenniumListTile(
                    onClick = {
                        onKick()
                        onDismiss()
                    },
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxCloseSquare,
                            contentDescription = null,
                            tint = AppTheme.colors.error
                        )
                    },
                    title = { Text(stringResource(Res.string.queue_participant_kick)) },
                    subtitle = { Text(stringResource(Res.string.queue_participant_kick_description)) })
                MillenniumListTile(
                    onClick = {
                        onBan()
                        onDismiss()
                    },
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxUserRemove,
                            contentDescription = null,
                            tint = AppTheme.colors.error
                        )
                    },
                    title = { Text(stringResource(Res.string.jam_participants_ban)) },
                    subtitle = { Text(stringResource(Res.string.queue_participant_ban_description)) })
                MillenniumListTile(
                    onClick = {
                        onRemoveSuggestions()
                        onDismiss()
                    },
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxMusicSquareRemove,
                            contentDescription = null
                        )
                    },
                    title = { Text(stringResource(Res.string.queue_participant_remove_suggestions)) },
                    subtitle = { Text(stringResource(Res.string.queue_participant_remove_suggestions_description)) })
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(participant: JamParticipant, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = participant.displayName.firstOrNull()?.uppercase()?.take(1) ?: "?",
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onPrimaryContainer
        )
    }
}

@Composable
private fun QueueItemRow(
    item: QueueItemUi,
    isPlaying: Boolean,
    dragging: Boolean,
    reorderScope: sh.calvin.reorderable.ReorderableCollectionItemScope?,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    showOptions: Boolean = true,
    enabled: Boolean = true,
    onParticipantClick: (JamParticipant) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowColor by animateColorAsState(
        when {
            dragging -> accentTonal()
            else -> AppTheme.colors.surfaceContainerHigh
        },
        label = "queueRow"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(rowColor)
            .clickable(enabled = enabled, onClick = onPlayClick)
            .padding(start = if (item.isCurrent) 10.dp else 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // The current track has no handle and a round cover; queued ones a handle and a squircle.
        if (!item.isCurrent) {
            MillenniumIcon(
                Iconsax.IconsaxDragHandle,
                contentDescription = if (reorderScope != null) stringResource(Res.string.queue_reorder_content_description) else null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(6.dp)
                    .then(
                        if (reorderScope != null) {
                            with(reorderScope) {
                                Modifier.draggableHandle(
                                    onDragStarted = { onDragStarted() },
                                    onDragStopped = onDragStopped
                                )
                            }
                        } else Modifier
                    ),
                tint = AppTheme.colors.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (item.isCurrent) CircleShape else RoundedCornerShape(12.dp))
                .background(AppTheme.colors.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (item.artwork != null) {
                AsyncImage(
                    model = item.artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "${item.originalIndex + 1}",
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = AppTheme.typography.bodyLarge.copy(
                    fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.isCurrent) AppTheme.colors.primary else AppTheme.colors.onSurface
            )
            Text(
                text = item.addedByParticipant?.let { stringResource(Res.string.queue_item_subtitle_added_by, item.subtitle, it.displayName) } ?: item.subtitle,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.isCurrent && isPlaying) {
            PlayingBars(
                playing = true,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.size(width = 16.dp, height = 14.dp)
            )
        }
        item.addedByParticipant?.let { participant ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.surfaceContainer)
                    .clickable { onParticipantClick(participant) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.displayName.firstOrNull()?.uppercase()?.take(1) ?: "?",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }
        if (showOptions) {
            MillenniumDropdownMenu(
                expanded = showMenu,
                onExpandedChange = { showMenu = it },
                anchor = {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (item.isCurrent) accentTonal() else AppTheme.colors.surfaceContainer
                            )
                            .clickable(role = Role.Button) { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        MillenniumIcon(
                            Iconsax.Iconsax3DotsMore,
                            contentDescription = stringResource(Res.string.player_more_options_content_description),
                            modifier = Modifier.size(18.dp),
                            tint = AppTheme.colors.onSurface
                        )
                    }
                }) {
                MillenniumDropdownMenuItem(
                    label = stringResource(Res.string.queue_remove_from_queue),
                    icon = Iconsax.IconsaxMusicSquareRemove,
                    destructive = true,
                    onClick = {
                        onRemoveClick()
                        showMenu = false
                    })
            }
        }
    }
}

/** Tonal accent fill (Material "primary container"): the surface tinted with the accent. */
@Composable
private fun accentTonal(): Color = lerp(AppTheme.colors.surfaceContainerHigh, AppTheme.colors.primary, 0.3f)
