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
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.spotube.spotube.modules.shell.PlayPauseGlyph
import org.spotube.spotube.modules.shell.PlayerControlButton
import org.spotube.spotube.modules.shell.controlWeight
import org.spotube.spotube.modules.shell.playIsBusy
import org.spotube.spotube.modules.shell.rememberPlayTapState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf

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
import spotube.composeapp.generated.resources.lyrics_tab_lyrics
import spotube.composeapp.generated.resources.lyrics_tab_track
import spotube.composeapp.generated.resources.lyrics_tab_artist
import spotube.composeapp.generated.resources.lyrics_tab_related
import spotube.composeapp.generated.resources.lyrics_track_album
import spotube.composeapp.generated.resources.lyrics_track_release_date
import spotube.composeapp.generated.resources.lyrics_track_credits_header
import spotube.composeapp.generated.resources.lyrics_track_credits_artist
import spotube.composeapp.generated.resources.lyrics_track_no_info
import spotube.composeapp.generated.resources.lyrics_artist_followers
import spotube.composeapp.generated.resources.lyrics_artist_monthly_listeners
import spotube.composeapp.generated.resources.lyrics_artist_world_rank
import spotube.composeapp.generated.resources.lyrics_artist_no_bio
import spotube.composeapp.generated.resources.lyrics_related_empty
import spotube.composeapp.generated.resources.lyrics_translate
import spotube.composeapp.generated.resources.lyrics_show_original
import spotube.composeapp.generated.resources.lyrics_translating
import spotube.composeapp.generated.resources.player_track_details_album_type
import spotube.composeapp.generated.resources.player_track_details_album_genres
import spotube.composeapp.generated.resources.player_track_details_album_track_count
import spotube.composeapp.generated.resources.player_track_details_track_number
import spotube.composeapp.generated.resources.player_track_details_disc_number
import spotube.composeapp.generated.resources.player_track_details_explicit
import spotube.composeapp.generated.resources.player_track_details_isrc
import spotube.composeapp.generated.resources.common_yes
import kotlin.math.roundToInt

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onClose: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsState()
    // Expressive lyrics: the whole page takes the cover's palette, like the full player.
    val pal = rememberArtworkColors(uiState.coverUrl.takeIf { it.isNotBlank() })
        .animated(themeArtworkColors())
    val audioPlayer = koinInject<AudioPlayerInterface>()
    val audioPlayerQueue = koinInject<AudioPlayerQueue>()
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val scope = rememberCoroutineScope()

    // The four carousel pages: lyrics, track info, artist info, related tracks.
    // Seeded from initialPage (the compact mini-carousel's own current page when it was
    // expanded), so opening from e.g. "Artista" there lands on "Artista" here too, instead
    // of always snapping back to "Testo". rememberSaveable (not remember) also keeps
    // whichever tab the person manually switches to within this same screen instance.
    var lastSelectedPage by rememberSaveable { mutableStateOf(initialPage) }
    val pagerState = rememberPagerState(initialPage = lastSelectedPage, pageCount = { 4 })
    LaunchedEffect(pagerState.currentPage) {
        lastSelectedPage = pagerState.currentPage
    }
    var autoPageChosen by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading, uiState.syncedLyrics, uiState.plainLyrics) {
        // Only auto-redirect when the person hadn't already picked a tab themselves -
        // otherwise re-expanding on a lyrics-less track would snap "Autore" back to
        // "Brano" even though they'd deliberately navigated there.
        if (!autoPageChosen && !uiState.isLoading) {
            autoPageChosen = true
            if (lastSelectedPage == 0) {
                val hasLyrics = !uiState.syncedLyrics.isNullOrEmpty() || !uiState.plainLyrics.isNullOrBlank()
                // No lyrics for this track: open straight on "Brano" instead of an empty page.
                if (!hasLyrics) pagerState.scrollToPage(1)
            }
        }
    }

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
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            val tabLabels = listOf(
                stringResource(Res.string.lyrics_tab_lyrics),
                stringResource(Res.string.lyrics_tab_track),
                stringResource(Res.string.lyrics_tab_artist),
                stringResource(Res.string.lyrics_tab_related)
            )
            ExpressiveToggle(
                options = tabLabels,
                selectedIndex = pagerState.currentPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                // Follow the pager's own continuous position (drag or programmatic scroll
                // alike): a single pill slides smoothly instead of flashing through every
                // tab it passes over.
                progress = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                container = pal.tonal,
                accent = pal.accent,
                onAccent = pal.onAccent,
                content = pal.onContainer
            )

            val pagerContent: @Composable ColumnScope.() -> Unit = {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    Column(Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> LyricsBody(uiState, viewModel)
                            1 -> TrackInfoTab(uiState)
                            2 -> ArtistInfoTab(uiState, viewModel::toggleBioTranslation)
                            else -> RelatedTracksTab(uiState)
                        }
                    }
                }
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
                    Column(Modifier.weight(0.62f).fillMaxHeight(), content = pagerContent)
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
                        .padding(horizontal = 8.dp),
                    content = pagerContent
                )
            }
            val playTap = rememberPlayTapState(playerUiState.isPlaying)
            LyricsControls(
                isPlaying = playerUiState.isPlaying,
                busy = playIsBusy(playTap, playerUiState),
                onPrevious = { scope.launch { audioPlayer.skipToPrevious() } },
                onPlayPause = {
                    playTap.pending = true
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
    if (!uiState.isLoading && uiState.error == null && uiState.lyricsTranslationAvailable) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = Alignment.CenterEnd) {
            TranslatePill(
                translating = uiState.lyricsTranslating,
                showingTranslation = uiState.showTranslatedLyrics,
                onClick = viewModel::toggleLyricsTranslation
            )
        }
    }
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
            syncedLyrics = (if (uiState.showTranslatedLyrics) uiState.translatedSyncedLyrics else null)
                ?: uiState.syncedLyrics ?: emptyList(),
            currentIndex = uiState.currentIndex,
            onLineClick = viewModel::seekToLine
        )

        uiState.mode == LyricType.STATIC -> PlainLyricsContent(
            plainLyrics = (if (uiState.showTranslatedLyrics) uiState.translatedPlainLyrics else null)
                ?: uiState.plainLyrics ?: ""
        )
    }
}

/** Small pill shown above the lyrics/bio: switches between the native text and its translation. */
@Composable
private fun TranslatePill(
    translating: Boolean,
    showingTranslation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val pal = LocalLyricsColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(pal?.tonal ?: MillenniumTheme.colors.raised)
            .clickable(role = Role.Button, enabled = !translating, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (translating) {
            ExpressiveLoadingIndicator(size = 14.dp)
        }
        Text(
            text = stringResource(
                when {
                    translating -> Res.string.lyrics_translating
                    showingTranslation -> Res.string.lyrics_show_original
                    else -> Res.string.lyrics_translate
                }
            ),
            style = AppTheme.typography.labelLarge,
            color = lyricsText()
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
/** Same Expressive group as the player: the pressed button widens and the others give way. */
@Composable
private fun LyricsControls(
    isPlaying: Boolean,
    busy: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalLyricsColors.current ?: return
    var pulsing by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pulsing) {
        if (pulsing >= 0) {
            delay(260)
            pulsing = -1
        }
    }
    Row(
        modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerControlButton(
            onClick = { pulsing = 0; onPrevious() },
            background = pal.tonal,
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.weight(controlWeight(0, 1f, pulsing)),
            height = 68.dp,
            onPress = { pulsing = 0 }
        ) {
            MillenniumIcon(
                Iconsax.IconsaxPrevious, stringResource(Res.string.player_previous_content_description),
                tint = pal.onContainer, modifier = Modifier.size(26.dp)
            )
        }
        PlayerControlButton(
            onClick = { pulsing = 1; onPlayPause() },
            background = pal.accent,
            shape = RoundedCornerShape(if (isPlaying) 24.dp else 36.dp),
            modifier = Modifier.weight(controlWeight(1, 1.25f, pulsing)),
            height = 68.dp,
            onPress = { pulsing = 1 }
        ) {
            PlayPauseGlyph(isPlaying = isPlaying, busy = busy, tint = pal.onAccent, size = 30.dp)
        }
        PlayerControlButton(
            onClick = { pulsing = 2; onNext() },
            background = pal.tonal,
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.weight(controlWeight(2, 1f, pulsing)),
            height = 68.dp,
            onPress = { pulsing = 2 }
        ) {
            MillenniumIcon(
                Iconsax.IconsaxNext, stringResource(Res.string.player_next_content_description),
                tint = pal.onContainer, modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** "Brano" tab: album, release date, duration and other metadata for the current track. */
@Composable
private fun TrackInfoTab(uiState: LyricsUiState) {
    val track = uiState.track
    if (track == null) {
        CenteredLyricsMessage { Text(text = stringResource(Res.string.lyrics_track_no_info), color = lyricsMuted()) }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Grouped like Spotify's own "Riconoscimenti" (Credits) panel: the release
                // this track belongs to, when it came out, and who's behind it. The metadata
                // source only gives us the performing artist(s) - not per-role songwriting,
                // production or engineering credits - so only "Artista" is listed here.
                Text(
                    text = stringResource(Res.string.lyrics_track_credits_header),
                    style = AppTheme.typography.titleMedium,
                    color = lyricsText()
                )
                track.album?.title?.takeIf { it.isNotBlank() }?.let {
                    LyricsInfoRow(stringResource(Res.string.lyrics_track_album), it)
                }
                track.album?.releaseDate?.takeIf { it.isNotBlank() }?.let {
                    LyricsInfoRow(stringResource(Res.string.lyrics_track_release_date), it)
                }
                if (track.artists.isNotEmpty()) {
                    LyricsInfoRow(
                        stringResource(Res.string.lyrics_track_credits_artist),
                        track.artists.joinToString(", ") { it.name }
                    )
                }
                track.album?.albumType?.let {
                    LyricsInfoRow(stringResource(Res.string.player_track_details_album_type), it.name)
                }
                // Track/disc position within the album, Spotify-credits style.
                if (track.trackNumber != null || track.discNumber != null) {
                    track.trackNumber?.let {
                        LyricsInfoRow(stringResource(Res.string.player_track_details_track_number), it.toString())
                    }
                    track.discNumber?.let {
                        LyricsInfoRow(stringResource(Res.string.player_track_details_disc_number), it.toString())
                    }
                }
                track.album?.trackCount?.takeIf { it > 0 }?.let {
                    LyricsInfoRow(stringResource(Res.string.player_track_details_album_track_count), it.toString())
                }
                val genres = track.album?.genres.orEmpty()
                if (genres.isNotEmpty()) {
                    LyricsInfoRow(stringResource(Res.string.player_track_details_album_genres), genres.joinToString(", "))
                }
                if (track.explicit == true) {
                    LyricsInfoRow(stringResource(Res.string.player_track_details_explicit), stringResource(Res.string.common_yes))
                }
                track.isrcCode?.takeIf { it.isNotBlank() }?.let {
                    LyricsInfoRow(stringResource(Res.string.player_track_details_isrc), it)
                }
            }
        }
    }
}

/** "Artista" tab: biography and basics for the track's main artist. */
@Composable
private fun ArtistInfoTab(uiState: LyricsUiState, onToggleBioTranslation: () -> Unit) {
    val overview = uiState.artistOverview
    when {
        uiState.artistOverviewLoading -> CenteredLyricsMessage { ExpressiveLoadingIndicator(size = 40.dp) }
        overview == null -> CenteredLyricsMessage {
            Text(text = stringResource(Res.string.lyrics_artist_no_bio), color = lyricsMuted())
        }
        else -> {
            val artist = overview.artist
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(LocalLyricsColors.current?.tonal ?: MillenniumTheme.colors.raised)
                        ) {
                            AsyncImage(
                                model = artist.thumbnails.firstOrNull()?.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column {
                            Text(text = artist.name, style = AppTheme.typography.titleLarge, color = lyricsText())
                            artist.followersCount?.let {
                                Text(
                                    text = stringResource(Res.string.lyrics_artist_followers, formatLyricsCount(it)),
                                    style = AppTheme.typography.bodyMedium,
                                    color = lyricsMuted()
                                )
                            }
                            artist.monthlyListeners?.let {
                                Text(
                                    text = stringResource(Res.string.lyrics_artist_monthly_listeners, formatLyricsCount(it)),
                                    style = AppTheme.typography.bodyMedium,
                                    color = lyricsMuted()
                                )
                            }
                            artist.worldRank?.let {
                                Text(
                                    text = stringResource(Res.string.lyrics_artist_world_rank, it),
                                    style = AppTheme.typography.bodyMedium,
                                    color = lyricsMuted()
                                )
                            }
                        }
                    }
                    if (artist.genres.isNotEmpty()) {
                        Text(
                            text = artist.genres.joinToString(" · "),
                            style = AppTheme.typography.bodyMedium,
                            color = lyricsMuted(),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    artist.biography?.takeIf { it.isNotBlank() }?.let { bio ->
                        if (uiState.bioTranslationAvailable) {
                            TranslatePill(
                                translating = uiState.bioTranslating,
                                showingTranslation = uiState.showTranslatedBio,
                                onClick = onToggleBioTranslation,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                        Text(
                            text = (if (uiState.showTranslatedBio) uiState.translatedBio else null) ?: bio,
                            style = AppTheme.typography.bodyLarge,
                            color = lyricsText()
                        )
                    }
                }
            }
        }
    }
}

/** "Correlate" tab: tracks recommended from the one currently playing. */
@Composable
private fun RelatedTracksTab(uiState: LyricsUiState) {
    val tracks = uiState.relatedTracks
    when {
        uiState.relatedTracksLoading -> CenteredLyricsMessage { ExpressiveLoadingIndicator(size = 40.dp) }
        tracks.isNullOrEmpty() -> CenteredLyricsMessage {
            Text(text = stringResource(Res.string.lyrics_related_empty), color = lyricsMuted())
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                            .background(LocalLyricsColors.current?.tonal ?: MillenniumTheme.colors.raised)
                    ) {
                        AsyncImage(
                            model = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = AppTheme.typography.titleMedium,
                            color = lyricsText(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artists.joinToString(", ") { it.name },
                            style = AppTheme.typography.bodyMedium,
                            color = lyricsMuted(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsInfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = AppTheme.typography.labelLarge, color = lyricsMuted())
        Text(text = value, style = AppTheme.typography.bodyLarge, color = lyricsText())
    }
}

@Composable
private fun CenteredLyricsMessage(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** 1_234_567 -> "1.2M", 12_345 -> "12.3K". Below 1000 the raw number is shown. */
private fun formatLyricsCount(count: Int): String {
    fun scaled(divisor: Int, suffix: String): String {
        val tenths = (count / divisor.toDouble() * 10).roundToInt()
        val whole = tenths / 10
        val fraction = tenths % 10
        return "$whole.$fraction$suffix"
    }
    return when {
        count >= 1_000_000 -> scaled(1_000_000, "M")
        count >= 1_000 -> scaled(1_000, "K")
        else -> count.toString()
    }
}
