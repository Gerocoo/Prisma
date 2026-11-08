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


import androidx.compose.foundation.layout.statusBarsPadding
import org.spotube.spotube.core.ui.component.ExpressiveLoadingIndicator
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.core.ui.component.ExpressiveToggle
import org.spotube.spotube.core.ui.theming.themeArtworkColors
import org.spotube.spotube.core.ui.theming.animated
import org.spotube.spotube.core.ui.theming.rememberArtworkColors
import org.spotube.spotube.core.ui.theming.ArtworkColors
import org.spotube.spotube.modules.shell.rememberPlayerUiState
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import org.spotube.spotube.core.ui.theming.materialPanelBrush
import org.spotube.spotube.core.ui.theming.materialSurface
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.ui.component.ApplicationBackButton
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error
import spotube.composeapp.generated.resources.lyrics_mode_plain
import spotube.composeapp.generated.resources.lyrics_mode_synced
import spotube.composeapp.generated.resources.lyrics_no_plain
import spotube.composeapp.generated.resources.lyrics_no_synced
import spotube.composeapp.generated.resources.common_close

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsState()
    // Expressive lyrics: the whole page takes the cover's palette, like the full player.
    val pal = rememberArtworkColors(uiState.coverUrl.takeIf { it.isNotBlank() })
        .animated(themeArtworkColors())
    val audioPlayer = koinInject<AudioPlayerInterface>()
    val audioPlayerQueue = koinInject<AudioPlayerQueue>()
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLyricsColors provides pal) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // One flat cover-tinted color edge to edge, status bar included: no gradient band.
            .background(pal.container)
    ) {
        val isWide = maxWidth >= 840.dp

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
                    if (onClose != null) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(pal.tonal)
                                .clickable(role = Role.Button, onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxArrowDown4,
                                contentDescription = stringResource(Res.string.common_close),
                                tint = pal.onContainer
                            )
                        }
                    } else {
                        ApplicationBackButton()
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ExpressiveToggle(
                        options = listOf(
                            stringResource(Res.string.lyrics_mode_synced),
                            stringResource(Res.string.lyrics_mode_plain)
                        ),
                        selectedIndex = if (uiState.mode == LyricType.SYNCED) 0 else 1,
                        onSelect = { viewModel.setMode(if (it == 0) LyricType.SYNCED else LyricType.STATIC) },
                        modifier = Modifier.widthIn(max = 280.dp),
                        container = pal.tonal,
                        accent = pal.accent,
                        onAccent = pal.onAccent,
                        content = pal.onContainer
                    )
                }
                Spacer(Modifier.width(48.dp))
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackPane(uiState, Modifier.weight(0.38f))
                    Column(Modifier.weight(0.62f).fillMaxHeight()) {
                        LyricsBody(uiState, viewModel)
                    }
                }
            } else {
                CompactTrackHeader(
                    uiState,
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    LyricsBody(uiState, viewModel)
                }
            }
            LyricsControls(
                isPlaying = playerUiState.isPlaying,
                onPrevious = { scope.launch { audioPlayer.skipToPrevious() } },
                onPlayPause = {
                    scope.launch { if (playerUiState.isPlaying) audioPlayer.pause() else audioPlayer.play() }
                },
                onNext = { scope.launch { audioPlayer.skipToNext() } },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 20.dp)
            )
        }
    }
    }
}

@Composable
private fun ColumnScope.LyricsBody(uiState: LyricsUiState, viewModel: LyricsViewModel) {
    when {
        uiState.isLoading -> CenteredMessage {
            ExpressiveLoadingIndicator(size = 48.dp)
        }

        uiState.error != null -> CenteredMessage {
            Text(
                text = uiState.error ?: stringResource(Res.string.common_unknown_error),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.danger,
                textAlign = TextAlign.Center
            )
        }

        uiState.mode == LyricType.SYNCED -> SyncedLyricsContent(
            syncedLyrics = uiState.syncedLyrics ?: emptyList(),
            currentIndex = uiState.currentIndex,
            onLineClick = viewModel::seekToLine
        )

        uiState.mode == LyricType.STATIC -> PlainLyricsContent(
            plainLyrics = uiState.plainLyrics ?: ""
        )
    }
}

@Composable
private fun CoverArt(url: String, size: Dp, radius: Dp, elevation: Dp, modifier: Modifier = Modifier) {
    val colors = MillenniumTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(LocalLyricsColors.current?.tonal ?: colors.raised)
    ) {
        AsyncImage(
            model = url.takeIf { it.isNotBlank() },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun TrackPane(uiState: LyricsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoverArt(
            url = uiState.coverUrl,
            size = 300.dp,
            radius = 32.dp,
            elevation = 14.dp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = uiState.trackTitle,
            style = AppTheme.typography.headlineSmall,
            color = lyricsText(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiState.trackArtists,
            style = AppTheme.typography.bodyLarge,
            color = lyricsMuted(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactTrackHeader(uiState: LyricsUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = uiState.coverUrl, size = 56.dp, radius = 16.dp, elevation = 0.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = uiState.trackTitle,
                style = AppTheme.typography.titleMedium,
                color = lyricsText(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.trackArtists,
                style = AppTheme.typography.bodyMedium,
                color = lyricsMuted(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ColumnScope.CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ColumnScope.SyncedLyricsContent(
    syncedLyrics: List<LyricsLine>,
    currentIndex: Int,
    onLineClick: (LyricsLine) -> Unit) {
    if (syncedLyrics.isEmpty()) {
        CenteredMessage {
            Text(
                text = stringResource(Res.string.lyrics_no_synced),
                style = AppTheme.typography.bodyLarge,
                color = lyricsMuted(),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    SyncedLyricsList(
        lines = syncedLyrics,
        currentIndex = currentIndex,
        onLineClick = onLineClick,
        textColor = lyricsText(),
        activeColor = LocalLyricsColors.current?.accent ?: MillenniumTheme.colors.accent,
        modifier = Modifier.weight(1f).fillMaxWidth()
    )
}

@Composable
private fun ColumnScope.PlainLyricsContent(
    plainLyrics: String) {
    if (plainLyrics.isBlank()) {
        CenteredMessage {
            Text(
                text = stringResource(Res.string.lyrics_no_plain),
                style = AppTheme.typography.bodyLarge,
                color = lyricsMuted(),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
    ) {
        item {
            Text(
                text = plainLyrics,
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 34.sp),
                color = lyricsText(),
                textAlign = TextAlign.Start
            )
        }
    }
}

private val LocalLyricsColors = staticCompositionLocalOf<ArtworkColors?> { null }

@Composable
private fun lyricsText(): Color = LocalLyricsColors.current?.onContainer ?: MillenniumTheme.colors.text

@Composable
private fun lyricsMuted(): Color = LocalLyricsColors.current?.onContainerMuted ?: MillenniumTheme.colors.muted

/** Previous / play-pause / next under the lyrics, in the player's round Expressive style. */
@Composable
private fun LyricsControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalLyricsColors.current ?: return
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(pal.tonal)
                .clickable(role = Role.Button, onClick = onPrevious),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                Iconsax.IconsaxPrevious, stringResource(Res.string.player_previous_content_description),
                tint = pal.onContainer, modifier = Modifier.size(24.dp)
            )
        }
        Box(
            Modifier.width(96.dp).height(64.dp).clip(RoundedCornerShape(50)).background(pal.accent)
                .clickable(role = Role.Button, onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                if (isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                stringResource(if (isPlaying) Res.string.player_pause_content_description else Res.string.player_play_content_description),
                tint = pal.onAccent, modifier = Modifier.size(28.dp)
            )
        }
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(pal.tonal)
                .clickable(role = Role.Button, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                Iconsax.IconsaxNext, stringResource(Res.string.player_next_content_description),
                tint = pal.onContainer, modifier = Modifier.size(24.dp)
            )
        }
    }
}
