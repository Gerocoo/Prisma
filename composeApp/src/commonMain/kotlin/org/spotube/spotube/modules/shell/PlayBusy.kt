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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.ui.component.ExpressiveLoadingIndicator
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description

/** Remembers a play/pause tap until the player actually reacts (or 15 s pass). */
@Stable
internal class PlayTapState {
    var pending by mutableStateOf(false)
}

@Composable
internal fun rememberPlayTapState(isPlaying: Boolean): PlayTapState {
    val tap = remember { PlayTapState() }
    LaunchedEffect(isPlaying) { tap.pending = false }
    LaunchedEffect(tap.pending) {
        if (tap.pending) {
            delay(15_000)
            tap.pending = false
        }
    }
    return tap
}

/** True while the tap is waiting for the server or the player is buffering. */
internal fun playIsBusy(tap: PlayTapState, state: PlayerUiState): Boolean =
    tap.pending || state.playerState == PlayerState.BUFFERING

/**
 * Play/pause icon that turns into the Expressive loading shape while the stream is on its way,
 * so a slow server never looks like an ignored tap.
 */
@Composable
internal fun PlayPauseGlyph(isPlaying: Boolean, busy: Boolean, tint: Color, size: Dp) {
    Crossfade(targetState = busy, label = "playGlyph") { loading ->
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            if (loading) {
                ExpressiveLoadingIndicator(size = size, color = tint)
            } else {
                MillenniumIcon(
                    if (isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.player_pause_content_description
                        else Res.string.player_play_content_description
                    ),
                    tint = tint,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}
