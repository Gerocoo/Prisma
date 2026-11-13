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

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package org.spotube.spotube.modules.shell


import androidx.compose.ui.graphics.Brush
import spotube.composeapp.generated.resources.spotify_connect_button
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.spotube.spotube.modules.shell.spotify_connect.SpotifyConnectSheet
import org.spotube.spotube.modules.shell.spotify_connect.SpotifyConnectViewModel
import androidx.compose.animation.core.tween
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.core.spotify.rememberSpotifyCanvasUrl
import org.spotube.spotube.core.spotify.CanvasVideo
import org.spotube.spotube.core.spotify.CanvasBackdrop
import org.spotube.spotube.core.spotify.LocalCanvasBackdrop
import org.spotube.spotube.core.spotify.canvasBlur
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.basicMarquee
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.AnimatedContent
import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import org.spotube.spotube.core.ui.component.WavySlider
import org.spotube.spotube.core.ui.theming.ArtworkColors
import org.spotube.spotube.core.ui.theming.rememberArtworkColors
import org.spotube.spotube.core.ui.theming.animated
import org.spotube.spotube.core.ui.theming.themeArtworkColors
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import org.spotube.spotube.core.cast.CastButton
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.millennium.composables.MillenniumSnackbarHost
import org.spotube.millennium.composables.MillenniumSnackbarHostState
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumModalBottomSheet
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.materialPanelBrush
import org.spotube.spotube.core.ui.theming.materialSurface
import com.composeunstyled.SheetDetent
import com.composeunstyled.rememberModalBottomSheetState

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.lyrics.LyricsViewModel
import org.spotube.spotube.modules.lyrics.SyncedLyricsList
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.spotube.spotube.resources.iconsax.IconsaxArrowSquareUp
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxRepeatMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.InconsaxClock
import org.spotube.spotube.resources.iconsax.SwapHorizontal2
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_alternative_source
import spotube.composeapp.generated.resources.player_collapse_content_description
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_expand_lyrics_content_description
import spotube.composeapp.generated.resources.player_go_to_album
import spotube.composeapp.generated.resources.player_loop_mode_all
import spotube.composeapp.generated.resources.player_loop_mode_none
import spotube.composeapp.generated.resources.player_loop_mode_one
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_now_playing
import spotube.composeapp.generated.resources.player_options_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.player_sleep_timer
import spotube.composeapp.generated.resources.player_track_details
import spotube.composeapp.generated.resources.queue_title

// The expanded player on small screens
// on the top left it has a angle/chevron down that can be used to collapse the player back to the
// floating player
// on the top right it has a three dots for options
// The top bar can be used to swipe down and close the player back to the floating player. It should
// follow the user's finger while swiping down and should have a nice animation when collapsing back
// to the floating player
// On the middle the album art stays rounded, centered and takes up most of the space
// Below the album art, on the left the song title and artists are shown and on the right the like
// button is shown
// below that we have the playback controls and the progress bar, which should be similar to the
// large player but with a different layout to fit the smaller screen
// And at the very end we will have the volume slider
/** False while the full player is folded into the mini player: its canvas video must not run then. */
val LocalExpandedPlayerVisible = androidx.compose.runtime.compositionLocalOf { true }

@Composable
fun AppExpandedPlayer(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onCollapse: () -> Unit = {},
    onQueue: () -> Unit = {},
    onAlternativeSource: () -> Unit = {},
    onExpandLyrics: () -> Unit = {},
    onGoToAlbum: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    playerOptionsViewModel: PlayerOptionsViewModel = koinViewModel(),
    spotifyConnectViewModel: SpotifyConnectViewModel = koinInject(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val jamRoomService: JamRoomService = koinInject()
    val isJamGuest by jamRoomService.role
        .map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val navigationCommands: NavigationCommands = koinInject()
    val currentEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val playerOptionsUiState by playerOptionsViewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack = remember(currentEntry) {
        (currentEntry as? QueueEntry.StreamingTrack)?.track
    }
    val snackbarHostState = remember { MillenniumSnackbarHostState() }
    val albumArtModifier =
        rememberSharedAlbumArtModifier(sharedTransitionScope, animatedVisibilityScope)
    val coverModel = playerUiState.coverModel
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(playerUiState.progress) }
    val moreOptionsSheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded
    )

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    LaunchedEffect(playerUiState.progress, isSeeking) {
        if (!isSeeking) {
            seekProgress = playerUiState.progress
        }
    }

    // Which way the cover carousel moves on the next track change (1 = next, -1 = previous),
    // and which control is doing its Expressive "grow on press" pulse.
    var skipDirection by remember { mutableIntStateOf(1) }
    var pulsingControl by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pulsingControl) {
        if (pulsingControl >= 0) {
            delay(260)
            pulsingControl = -1
        }
    }

    val playTap = rememberPlayTapState(playerUiState.isPlaying)
    val playBusy = playIsBusy(playTap, playerUiState)

    fun onPlayPause() {
        pulsingControl = 1
        playTap.pending = true
        scope.launch {
            if (playerUiState.isPlaying) {
                audioPlayer.pause()
            } else {
                audioPlayer.play()
            }
        }
    }


    fun onSkipPrevious() {
        if (isJamGuest) return
        skipDirection = -1
        pulsingControl = 0
        scope.launch { audioPlayer.skipToPrevious() }
    }

    fun onSkipNext() {
        if (isJamGuest) return
        skipDirection = 1
        pulsingControl = 2
        scope.launch { audioPlayer.skipToNext() }
    }

    fun onShuffleToggle() {
        if (isJamGuest) return
        scope.launch { audioPlayer.shuffle(!playerUiState.isShuffling) }
    }

    fun onLoopToggle() {
        if (isJamGuest) return
        scope.launch { audioPlayer.loop(playerUiState.loopState.next()) }
    }

    fun onSeekFinished() {
        val durationMillis = playerUiState.seekDuration.inWholeMilliseconds
        if (durationMillis <= 0L) return
        scope.launch {
            audioPlayer.seekTo(
                (durationMillis * seekProgress.coerceIn(
                    0f,
                    1f
                )).toLong().milliseconds
            )
        }
    }

    LaunchedEffect(moreOptionsSheetState.bottomSheetState.currentDetent) {
        if (moreOptionsSheetState.bottomSheetState.currentDetent == SheetDetent.Hidden) {
            playerOptionsViewModel.dismissMoreOptionsSheet()
        }
    }

    LaunchedEffect(playerOptionsUiState.isMoreOptionsSheetOpen) {
        if (playerOptionsUiState.isMoreOptionsSheetOpen) {
            moreOptionsSheetState.animateTo(SheetDetent.FullyExpanded)
        }
    }

    // Warm wash of the accent over the ground instead of a flat page.
    // Whole screen tinted with the cover's colors, cross-fading on track change.
    val coverArt = rememberArtworkColors(coverModel).animated(themeArtworkColors())

    // Spotify Canvas (with the Spotify plugin): like Spotify, the looping video fills the whole
    // player behind the controls, the cover hides and the controls switch to light-on-video colors.
    val settingsRepository = koinInject<SettingsRepository>()
    val userSettings by settingsRepository.userSettings.collectAsStateWithLifecycle(UserSettings())
    val canvasUrl = rememberSpotifyCanvasUrl(playerUiState.currentQueueEntry, userSettings.showSpotifyCanvas)
    var canvasShowing by remember(canvasUrl) { mutableStateOf(false) }
    // As soon as a track is known to have a canvas the player switches to the video look (cover
    // hidden, light controls on a dark ground); the video itself fades in on its first frame.
    val canvasProgress by animateFloatAsState(
        if (canvasUrl != null) 1f else 0f, tween(220), label = "canvas"
    )
    val videoAlpha by animateFloatAsState(
        if (canvasUrl != null && canvasShowing) 1f else 0f, tween(260), label = "canvasVideo"
    )
    val art = if (canvasProgress > 0f) coverArt.overVideo(canvasProgress) else coverArt
    val canvasBackdrop = remember { CanvasBackdrop() }
    SideEffect { canvasBackdrop.amount = videoAlpha }
    LaunchedEffect(canvasUrl) { if (canvasUrl == null) canvasBackdrop.image = null }

    CompositionLocalProvider(LocalCanvasBackdrop provides canvasBackdrop) {
    Box(
        modifier = modifier.fillMaxSize().background(coverArt.container)
            .onGloballyPositioned { canvasBackdrop.container = it }
    ) {
        if (canvasUrl != null && LocalExpandedPlayerVisible.current) {
            // Dark ground under the video, so nothing of the cover's colors flashes before it.
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = canvasProgress }.background(Color.Black))
            CanvasVideo(
                url = canvasUrl,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = videoAlpha },
                onFirstFrame = { canvasShowing = true },
                onBackdrop = { canvasBackdrop.image = it }
            )
            // Like Spotify: a light shade under the top bar, and a shadow that deepens from the
            // middle of the video down to the title and the controls, so they stay readable.
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = canvasProgress }.background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.40f),
                        0.18f to Color.Transparent,
                        0.36f to Color.Transparent,
                        0.52f to Color.Black.copy(alpha = 0.50f),
                        0.72f to Color.Black.copy(alpha = 0.78f),
                        1f to Color.Black.copy(alpha = 0.94f)
                    )
                )
            )
        }
        if (playerOptionsUiState.isMoreOptionsSheetOpen) {
            MillenniumModalBottomSheet(
                state = moreOptionsSheetState,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                contentSpacing = 4.dp
            ) {
                // Grid of Options
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OptionTile(
                            icon = Iconsax.SwapHorizontal2,
                            label = stringResource(Res.string.player_alternative_source),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                onAlternativeSource()
                            })
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.InconsaxClock,
                            label = stringResource(Res.string.player_sleep_timer),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                playerOptionsViewModel.showSleepTimerDialog()
                                onSleepTimer()
                            })
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.IconsaxCd,
                            label = stringResource(Res.string.player_go_to_album),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                val albumId = currentTrack?.album?.id
                                if (albumId != null) {
                                    navigationCommands.navigateTo(Routes.Album(albumId))
                                    onCollapse()
                                }
                                onGoToAlbum()
                            })
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.Iconsax3DotsMore,
                            label = stringResource(Res.string.player_track_details),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                playerOptionsViewModel.showTrackDetails()
                            })
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            // Top bar: collapse, title, then Cast / queue / options as tonal round buttons.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerRoundButton(onClick = onCollapse, background = art.containerHigh) {
                    MillenniumIcon(
                        Iconsax.IconsaxArrowDown4,
                        contentDescription = stringResource(Res.string.player_collapse_content_description),
                        tint = art.onContainer
                    )
                }
                Text(
                    text = stringResource(Res.string.player_now_playing),
                    style = AppTheme.typography.titleMedium,
                    color = art.onContainer,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    maxLines = 1
                )
                CastButton(background = art.containerHigh, tint = art.onContainer)
                // Spotify Connect: hand the track to the Spotify desktop app (or any Spotify device).
                val connectState by spotifyConnectViewModel.uiState.collectAsStateWithLifecycle()
                PlayerRoundButton(
                    onClick = spotifyConnectViewModel::open,
                    background = if (connectState.isRemoteActive) art.accent else art.containerHigh
                ) {
                    MillenniumIcon(
                        Iconsax.IconsaxMirroringScreen,
                        contentDescription = stringResource(Res.string.spotify_connect_button),
                        tint = if (connectState.isRemoteActive) art.onAccent else art.onContainer
                    )
                }
                PlayerRoundButton(onClick = onQueue, background = art.containerHigh) {
                    MillenniumIcon(
                        Iconsax.IconsaxMusicFilter,
                        contentDescription = stringResource(Res.string.queue_title),
                        tint = art.onContainer
                    )
                }
                PlayerRoundButton(onClick = playerOptionsViewModel::showMoreOptionsSheet, background = art.containerHigh) {
                    MillenniumIcon(
                        Iconsax.Iconsax3DotsMore,
                        contentDescription = stringResource(Res.string.player_options_content_description),
                        tint = art.onContainer
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Cover carousel: the cover follows a horizontal swipe; letting go past a third of
                // the width skips, and the new cover slides in from that side with a small gap,
                // as on Pixel. Buttons drive the same slide.
                val dragOffset = remember { Animatable(0f) }
                var coverWidth by remember { mutableIntStateOf(1) }
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .then(albumArtModifier)
                        .graphicsLayer { alpha = 1f - canvasProgress }
                        .onSizeChanged { coverWidth = it.width.coerceAtLeast(1) }
                        .pointerInput(isJamGuest) {
                            if (isJamGuest) return@pointerInput
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val v = dragOffset.value
                                    scope.launch {
                                        when {
                                            v < -coverWidth / 3f -> { onSkipNext(); dragOffset.snapTo(0f) }
                                            v > coverWidth / 3f -> { onSkipPrevious(); dragOffset.snapTo(0f) }
                                            else -> dragOffset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
                                        }
                                    }
                                },
                                onDragCancel = { scope.launch { dragOffset.animateTo(0f) } }
                            ) { change, amount ->
                                change.consume()
                                scope.launch { dragOffset.snapTo(dragOffset.value + amount) }
                            }
                        }
                        // The cover's rounded square is the mask: covers slide inside it.
                        .clip(RoundedCornerShape(32.dp))
                        .background(art.containerHigh)
                ) {
                    // Keyed on the track, not just the image: tracks of one album share a cover
                    // and would otherwise skip the slide.
                    AnimatedContent(
                        targetState = playerUiState.currentQueueEntry to coverModel,
                        transitionSpec = {
                            val dir = skipDirection
                            val slide = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 340f)
                            (slideInHorizontally(slide) { w -> dir * (w + w / 20) }) togetherWith
                                (slideOutHorizontally(slide) { w -> -dir * (w + w / 20) })
                        },
                        modifier = Modifier.fillMaxSize().graphicsLayer { translationX = dragOffset.value },
                        label = "coverCarousel"
                    ) { (_, model) ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp))
                                .background(art.containerHigh)
                        ) {
                            AsyncImage(
                                model = model,
                                contentDescription = playerUiState.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Long titles scroll instead of being cut, like on Pixel.
            Text(
                text = playerUiState.title,
                style = AppTheme.typography.headlineSmall,
                color = art.onContainer,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1500)
            )
            PlayerArtistsText(
                artists = playerUiState.artistList,
                onArtistClick = { artistId ->
                    navigationCommands.navigateTo(Routes.Artist(artistId))
                    // The page opens beneath the player, so get out of the way
                    onCollapse()
                },
                style = AppTheme.typography.bodyLarge,
                color = art.onContainerMuted,
                textAlign = TextAlign.Center
            )

            Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
                WavySlider(
                    value = seekProgress,
                    onValueChange = {
                        seekProgress = it
                        isSeeking = true
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        onSeekFinished()
                    },
                    activeColor = art.accent,
                    inactiveColor = art.tonal,
                    enabled = playerUiState.seekDuration.inWholeMilliseconds > 0L,
                    playing = playerUiState.isPlaying,
                    label = stringResource(Res.string.player_seek_label)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlayerTime(playerUiState.position),
                        style = AppTheme.typography.labelMedium,
                        color = art.onContainerMuted
                    )
                    Text(
                        text = formatPlayerTime(playerUiState.displayDuration),
                        style = AppTheme.typography.labelMedium,
                        color = art.onContainerMuted
                    )
                }

                // Main controls: three big rounded buttons, play in the middle on the accent.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerControlButton(
                        onClick = ::onSkipPrevious,
                        enabled = !isJamGuest,
                        background = art.tonal,
                        shape = RoundedCornerShape(40.dp),
                        modifier = Modifier.weight(controlWeight(0, 1f, pulsingControl)),
                        onPress = { pulsingControl = 0 }
                    ) {
                        MillenniumIcon(
                            Iconsax.IconsaxPrevious,
                            contentDescription = stringResource(Res.string.player_previous_content_description),
                            modifier = Modifier.size(30.dp),
                            tint = art.onContainer
                        )
                    }
                    PlayerControlButton(
                        onClick = ::onPlayPause,
                        background = art.accent,
                        shape = RoundedCornerShape(if (playerUiState.isPlaying) 28.dp else 40.dp),
                        modifier = Modifier.weight(controlWeight(1, 1.25f, pulsingControl)),
                        onPress = { pulsingControl = 1 }
                    ) {
                        PlayPauseGlyph(
                            isPlaying = playerUiState.isPlaying,
                            busy = playBusy,
                            tint = art.onAccent,
                            size = 34.dp
                        )
                    }
                    PlayerControlButton(
                        onClick = ::onSkipNext,
                        enabled = !isJamGuest,
                        background = art.tonal,
                        shape = RoundedCornerShape(40.dp),
                        modifier = Modifier.weight(controlWeight(2, 1f, pulsingControl)),
                        onPress = { pulsingControl = 2 }
                    ) {
                        MillenniumIcon(
                            Iconsax.IconsaxNext,
                            contentDescription = stringResource(Res.string.player_next_content_description),
                            modifier = Modifier.size(30.dp),
                            tint = art.onContainer
                        )
                    }
                }

                // Secondary toggles in one pill: shuffle, repeat, like.
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .canvasBlur()
                        .background(art.containerHigh)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerToggleSegment(
                        onClick = ::onShuffleToggle,
                        enabled = !isJamGuest,
                        active = playerUiState.isShuffling,
                        colors = art
                    ) {
                        MillenniumIcon(
                            Iconsax.IconsaxShuffle,
                            contentDescription = stringResource(if (playerUiState.isShuffling) Res.string.player_disable_shuffle_content_description else Res.string.player_enable_shuffle_content_description),
                            tint = if (playerUiState.isShuffling) art.onAccent else art.onContainer
                        )
                    }
                    PlayerToggleSegment(
                        onClick = ::onLoopToggle,
                        enabled = !isJamGuest,
                        active = playerUiState.loopState != LoopState.NONE,
                        colors = art
                    ) {
                        MillenniumIcon(
                            imageVector = when (playerUiState.loopState) {
                                LoopState.NONE -> Iconsax.IconsaxRepeateMusic
                                LoopState.ONE -> Iconsax.IconsaxRepeateOne
                                LoopState.ALL -> Iconsax.IconsaxRepeatMusic
                            },
                            contentDescription = stringResource(when (playerUiState.loopState) { LoopState.NONE -> Res.string.player_loop_mode_none; LoopState.ONE -> Res.string.player_loop_mode_one; LoopState.ALL -> Res.string.player_loop_mode_all }),
                            tint = if (playerUiState.loopState != LoopState.NONE) art.onAccent else art.onContainer
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(art.tonal),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayerHeartButton(
                            audioPlayerQueue = audioPlayerQueue,
                            savedTracksViewModel = savedTracksViewModel,
                            likedTint = art.accent,
                            tint = art.onContainer
                        )
                    }
                }
            }

            LyricsPreviewCard(
                colors = art,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                onExpand = onExpandLyrics
            )
        }

        SpotifyConnectSheet(spotifyConnectViewModel)

        MillenniumSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberSharedAlbumArtModifier(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier

    with(sharedTransitionScope) {
        return Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "player_album_art"),
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@Composable
private fun LyricsPreviewCard(
    colors: ArtworkColors,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    viewModel: LyricsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncedLyrics = uiState.syncedLyrics ?: emptyList()
    val isLoading = uiState.isLoading
    // The slot keeps its height even without lyrics, so the player never jumps between tracks.
    if (syncedLyrics.isEmpty() || isLoading) {
        Spacer(modifier = modifier.height(PreviewCardHeight))
        return
    }

    // Same cover-tinted colors as the player and the full lyrics page.
    Box(
        modifier = modifier
            .height(PreviewCardHeight)
            .clip(RoundedCornerShape(28.dp))
            .canvasBlur()
            .background(colors.containerHigh)
            .clickable(onClick = onExpand)
    ) {
        // Passive preview: no onLineClick, so lines never seek playback.
        SyncedLyricsList(
            lines = syncedLyrics,
            currentIndex = uiState.currentIndex,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 52.dp, top = 8.dp, bottom = 8.dp),
            textColor = colors.onContainerMuted,
            activeColor = colors.onContainer
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.tonal)
                .clickable(role = Role.Button, onClick = onExpand),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                Iconsax.IconsaxArrowSquareUp,
                contentDescription = stringResource(Res.string.player_expand_lyrics_content_description),
                tint = colors.onContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private val PreviewCardHeight = 150.dp

@Composable
private fun OptionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MillenniumCard(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(12.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MillenniumIcon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = AppTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

/** Small tonal circle used in the player's top bar. */
@Composable
private fun PlayerRoundButton(
    onClick: () -> Unit,
    background: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .canvasBlur()
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Big rounded control (previous / play / next); the shape morphs smoothly between states. */
@Composable
internal fun PlayerControlButton(
    onClick: () -> Unit,
    background: Color,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 80.dp,
    onPress: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) onPress() }
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .canvasBlur()
            .background(if (enabled) background else background.copy(alpha = 0.5f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Material 3 Expressive button group: the pressed control widens and its neighbours give way,
 * then everything springs back.
 */
@Composable
internal fun controlWeight(index: Int, base: Float, pulsing: Int): Float {
    val target = when {
        pulsing == index -> base * 1.55f
        pulsing >= 0 -> base * 0.85f
        else -> base
    }
    val weight by animateFloatAsState(
        target,
        spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "controlWeight$index"
    )
    return weight
}

/** One segment of the shuffle / repeat / like pill: filled with the accent while active. */
@Composable
private fun PlayerToggleSegment(
    onClick: () -> Unit,
    active: Boolean,
    colors: ArtworkColors,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) colors.accent else colors.tonal)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}


/** Player palette over a Canvas video: white text and translucent white controls, like Spotify. */
private fun ArtworkColors.overVideo(amount: Float): ArtworkColors {
    fun mix(a: Color, b: Color) = androidx.compose.ui.graphics.lerp(a, b, amount)
    return ArtworkColors(
        container = mix(container, Color.Transparent),
        containerHigh = mix(containerHigh, Color.White.copy(alpha = 0.16f)),
        tonal = mix(tonal, Color.White.copy(alpha = 0.22f)),
        accent = mix(accent, Color.White),
        onAccent = mix(onAccent, Color.Black),
        onContainer = mix(onContainer, Color.White),
        onContainerMuted = mix(onContainerMuted, Color.White.copy(alpha = 0.75f)),
    )
}
