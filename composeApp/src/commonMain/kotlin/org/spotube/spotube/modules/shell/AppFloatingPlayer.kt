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

package org.spotube.spotube.modules.shell


import spotube.composeapp.generated.resources.player_previous_content_description
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import org.spotube.spotube.core.ui.theming.rememberArtworkColors
import org.spotube.spotube.core.ui.theming.animated
import org.spotube.spotube.core.ui.theming.themeArtworkColors
import org.spotube.spotube.resources.iconsax.IconsaxNext
import spotube.composeapp.generated.resources.player_next_content_description

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import spotube.composeapp.generated.resources.devices_remote_playing_on
import org.spotube.spotube.modules.shell.spotify_connect.SpotifyConnectViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_nothing_playing
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface

internal val FloatingPlayerContentHeight = 64.dp
internal val FloatingPlayerBottomBleed = 16.dp

// The floating player on small screens that appears above the floating AppBottombar
//
// on left the album art, song title, artists grouped together
// on right the playback controls
// top border is actually the progress bar
// users can skip to next or previous track by swiping left or right on the player
//
// the player can be expanded to the full screen by swiping up on the player, and can be collapsed
// back to the floating player by swiping down on the player
@Composable
fun AppFloatingPlayer(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    spotifyConnectViewModel: SpotifyConnectViewModel = koinInject(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val scope = rememberCoroutineScope()
    val albumArtModifier =
        rememberSharedAlbumArtModifier(sharedTransitionScope, animatedVisibilityScope)

    fun onPlayPause() {
        scope.launch {
            if (playerUiState.isPlaying) {
                audioPlayer.pause()
            } else {
                audioPlayer.play()
            }
        }
    }

    val connectState by spotifyConnectViewModel.uiState.collectAsStateWithLifecycle()

    FloatingPlayerContent(
        playerUiState = playerUiState,
        connectDeviceName = if (connectState.isRemoteActive) connectState.remoteDevice?.name?.ifBlank { null } else null,
        modifier = modifier,
        albumArtModifier = albumArtModifier,
        onPlayPause = ::onPlayPause,
        onPrevious = { scope.launch { audioPlayer.skipToPrevious() } },
        onNext = { scope.launch { audioPlayer.skipToNext() } },
        heartButton = {
            PlayerHeartButton(
                audioPlayerQueue = audioPlayerQueue,
                savedTracksViewModel = savedTracksViewModel
            )
        }
    )
}

@Composable
internal fun FloatingPlayerContent(
    playerUiState: PlayerUiState,
    modifier: Modifier = Modifier,
    connectDeviceName: String? = null,
    albumArtModifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    heartButton: @Composable () -> Unit
) {
    val playTap = rememberPlayTapState(playerUiState.isPlaying)
    val coverModel = playerUiState.coverModel
    val hasQueue = playerUiState.queue.isNotEmpty()
    // Tonal pill tinted with the current cover's colors, like the full player.
    val colors = rememberArtworkColors(coverModel).animated(themeArtworkColors())
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FloatingPlayerContentHeight + FloatingPlayerBottomBleed)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = FloatingPlayerBottomBleed + 4.dp)
            .clip(cardShape)
            .background(colors.containerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .then(albumArtModifier)
                    .clip(CircleShape)
                    .background(colors.tonal),
                contentAlignment = Alignment.Center
            ) {
                if (coverModel != null) AsyncImage(
                    model = coverModel,
                    contentDescription = playerUiState.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else MillenniumIcon(
                    Iconsax.IconsaxMusic, contentDescription = null,
                    tint = colors.onContainerMuted, modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!hasQueue) stringResource(Res.string.player_nothing_playing) else playerUiState.title,
                    style = AppTheme.typography.titleSmall,
                    color = colors.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasQueue && connectDeviceName != null) Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.accent)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MillenniumIcon(
                        Iconsax.IconsaxMirroringScreen,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(Res.string.devices_remote_playing_on, connectDeviceName),
                        style = AppTheme.typography.labelSmall,
                        color = colors.onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (hasQueue) Text(
                    text = playerUiState.artists,
                    style = AppTheme.typography.bodySmall,
                    color = colors.onContainerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            heartButton()
            // Round previous/next on the tonal color around a round play/pause on the accent.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.tonal)
                    .clickable(enabled = hasQueue, role = Role.Button, onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(
                    Iconsax.IconsaxPrevious,
                    contentDescription = stringResource(Res.string.player_previous_content_description),
                    tint = colors.onContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable(enabled = hasQueue, role = Role.Button) {
                        playTap.pending = true
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                PlayPauseGlyph(
                    isPlaying = playerUiState.isPlaying,
                    busy = playIsBusy(playTap, playerUiState),
                    tint = colors.onAccent,
                    size = 20.dp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.tonal)
                    .clickable(enabled = hasQueue, role = Role.Button, onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(
                    Iconsax.IconsaxNext,
                    contentDescription = stringResource(Res.string.player_next_content_description),
                    tint = colors.onContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
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
