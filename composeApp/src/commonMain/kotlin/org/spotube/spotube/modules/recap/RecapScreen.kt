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
package org.spotube.spotube.modules.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import spotube.composeapp.generated.resources.*

/** Story-style colors for the recap pages: our own warm/cool pairs, not tied to any brand. */
private val pagePalettes = listOf(
    Color(0xFFFFB224) to Color(0xFFFF5A36),
    Color(0xFF6C5CE7) to Color(0xFF2D1B69),
    Color(0xFF00C2A8) to Color(0xFF0B4F6C),
    Color(0xFFFF6FA5) to Color(0xFF7A1F5C),
    Color(0xFF9BE15D) to Color(0xFF1D6B3A),
    Color(0xFF4FACFE) to Color(0xFF1A2A6C),
)

@Composable
fun RecapScreen(viewModel: RecapViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101010))) {
        when (val s = state) {
            RecapUiState.Loading -> Unit
            is RecapUiState.Empty -> RecapPage(0) {
                BigTitle(stringResource(Res.string.recap_title, s.year))
                Spacer(Modifier.height(16.dp))
                Body(stringResource(Res.string.recap_empty_title), bold = true)
                Spacer(Modifier.height(8.dp))
                Body(stringResource(Res.string.recap_empty_body))
            }
            is RecapUiState.Ready -> RecapPager(s)
        }
        // Close button over every page.
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(Iconsax.IconsaxArrowDown4, stringResource(Res.string.recap_close), tint = Color.White)
        }
    }
}

@Composable
private fun RecapPager(s: RecapUiState.Ready) {
    val pages: List<@Composable () -> Unit> = buildList {
        add {
            Label(stringResource(Res.string.recap_intro_label))
            BigTitle(stringResource(Res.string.recap_title, s.year))
            Spacer(Modifier.height(12.dp))
            Body(stringResource(Res.string.recap_swipe_hint))
        }
        add {
            Label(stringResource(Res.string.recap_minutes_intro))
            HugeNumber(s.totalMinutes.toString())
            Body(stringResource(Res.string.recap_minutes_label), bold = true)
            Spacer(Modifier.height(16.dp))
            Body(stringResource(Res.string.recap_minutes_caption, s.distinctTracks, s.distinctArtists))
        }
        s.topArtists.firstOrNull()?.let { top ->
            add {
                Label(stringResource(Res.string.recap_top_artist_label))
                Cover(top.cover, circle = true)
                Spacer(Modifier.height(20.dp))
                BigTitle(top.name)
                Body(stringResource(Res.string.recap_minutes_short, top.minutes))
            }
            add {
                Label(stringResource(Res.string.recap_top_artists))
                s.topArtists.forEachIndexed { i, a ->
                    RankRow(i + 1, a.name, stringResource(Res.string.recap_minutes_short, a.minutes), a.cover, circle = true)
                }
            }
        }
        s.topTracks.firstOrNull()?.let { top ->
            add {
                Label(stringResource(Res.string.recap_top_track_label))
                Cover(top.cover)
                Spacer(Modifier.height(20.dp))
                BigTitle(top.title)
                Body(top.artists)
                Spacer(Modifier.height(8.dp))
                Body(pluralStringResource(Res.plurals.recap_plays, top.plays, top.plays), bold = true)
            }
            add {
                Label(stringResource(Res.string.recap_top_tracks))
                s.topTracks.forEachIndexed { i, t ->
                    RankRow(i + 1, t.title, t.artists, t.cover)
                }
            }
        }
        add {
            Label(stringResource(Res.string.recap_top_month_label))
            BigTitle(monthName(s.topMonth))
            Body(stringResource(Res.string.recap_minutes_short, s.topMonthMinutes))
        }
        add {
            Label(stringResource(Res.string.recap_summary))
            BigTitle(stringResource(Res.string.recap_title, s.year))
            Spacer(Modifier.height(16.dp))
            SummaryLine(stringResource(Res.string.recap_minutes_label), s.totalMinutes.toString())
            s.topArtists.firstOrNull()?.let { SummaryLine(stringResource(Res.string.recap_top_artist_label), it.name) }
            s.topTracks.firstOrNull()?.let { SummaryLine(stringResource(Res.string.recap_top_track_label), it.title) }
            SummaryLine(stringResource(Res.string.recap_top_month_label), monthName(s.topMonth))
        }
    }
    val pagerState = rememberPagerState { pages.size }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            RecapPage(page) { pages[page]() }
        }
        // Story progress dashes at the top.
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 68.dp, end = 16.dp, top = 30.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = if (i <= pagerState.currentPage) 0.95f else 0.3f))
                )
            }
        }
    }
}

@Composable
private fun RecapPage(index: Int, content: @Composable () -> Unit) {
    val (top, bottom) = pagePalettes[index % pagePalettes.size]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 72.dp),
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun Label(text: String) = Text(
    text.uppercase(),
    style = AppTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
    color = Color.White.copy(alpha = 0.85f),
    modifier = Modifier.padding(bottom = 14.dp)
)

@Composable
private fun BigTitle(text: String) = Text(
    text,
    style = AppTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 44.sp),
    color = Color.White,
    maxLines = 3,
    overflow = TextOverflow.Ellipsis
)

@Composable
private fun HugeNumber(text: String) = Text(
    text,
    style = AppTheme.typography.displaySmall.copy(fontSize = 88.sp, lineHeight = 92.sp, fontWeight = FontWeight.ExtraBold),
    color = Color.White
)

@Composable
private fun Body(text: String, bold: Boolean = false) = Text(
    text,
    style = (if (bold) AppTheme.typography.titleLarge else AppTheme.typography.bodyLarge),
    color = Color.White.copy(alpha = if (bold) 1f else 0.9f)
)

@Composable
private fun Cover(url: String?, circle: Boolean = false) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(if (circle) CircleShape else RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String, cover: String?, circle: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString(),
            style = AppTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.width(36.dp)
        )
        Box(
            Modifier
                .size(52.dp)
                .clip(if (circle) CircleShape else RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            if (cover != null) AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, style = AppTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = AppTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = AppTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
        Text(value, style = AppTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun monthName(month: Int): String = stringResource(
    when (month) {
        1 -> Res.string.recap_month_1; 2 -> Res.string.recap_month_2; 3 -> Res.string.recap_month_3
        4 -> Res.string.recap_month_4; 5 -> Res.string.recap_month_5; 6 -> Res.string.recap_month_6
        7 -> Res.string.recap_month_7; 8 -> Res.string.recap_month_8; 9 -> Res.string.recap_month_9
        10 -> Res.string.recap_month_10; 11 -> Res.string.recap_month_11; else -> Res.string.recap_month_12
    }
)
