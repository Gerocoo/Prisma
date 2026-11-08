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


import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.height
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPause

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetails(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
    ownerName: String,
    ownerImageURL: String?,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean = false,
    /** Guest in a jam session: play/shuffle are replaced by Add to Jam. */
    isJamGuest: Boolean = false,
    onAddToJam: (() -> Unit)? = null,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    isPlayLoading: Boolean = false,
    isShuffleLoading: Boolean = false,
    isAddToQueueLoading: Boolean = false,
    onFollowClick: () -> Unit = { },
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    sharedElementKey: String? = null) {
    Box(modifier = modifier.fillMaxWidth()) {
        val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium

        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

        val playPauseButton = @Composable {
            if (isJamGuest) {
                if (onAddToJam != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MillenniumButton(
                            modifier = if (isCompact) Modifier.weight(1f) else Modifier,
                            onClick = onAddToJam!!) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxAddSquare,
                                contentDescription = stringResource(Res.string.track_options_add_to_jam))
                            TextWithShimmer(text = stringResource(Res.string.track_options_add_to_jam))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val modifier = if (isCompact) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }

                    MillenniumButton(
                        modifier = modifier,
                        onClick = onPlay,
                        ) {
                        MillenniumIcon(
                            imageVector = if (isPlaying) Iconsax.IconsaxPauseCircle else Iconsax.IconsaxPlayCircle2,
                            contentDescription = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description))
                        TextWithShimmer(text = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description))
                    }
                    MillenniumButton(
                        modifier = modifier,
                        onClick = onShufflePlay,
                        loading = isShuffleLoading,
                        style = MillenniumButtonStyle.Secondary
                    ) {
                        MillenniumIcon(imageVector = Iconsax.IconsaxShuffle, contentDescription = stringResource(Res.string.collection_details_shuffle_play))
                        TextWithShimmer(text = stringResource(Res.string.collection_details_shuffle))
                    }
                }
            }
        }

        val actions = @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MillenniumIconButton(
                    onClick = onAddToQueue,
                    loading = isAddToQueueLoading,
                    style = MillenniumIconButtonStyle.Quiet) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxAddSquare,
                        contentDescription = stringResource(Res.string.player_add_to_queue_content_description)
                    )
                }
                MillenniumSeparator(modifier = Modifier.height(24.dp), vertical = true)
                if (showFollowButton) {
                    if (isFollowing) {
                        MillenniumIconButton(
                            onClick = onFollowClick,
                            loading = isFollowLoading,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxHeart2,
                                contentDescription = stringResource(Res.string.collection_details_unfollow))
                        }
                    } else {
                        MillenniumIconButton(
                            onClick = onFollowClick,
                            loading = isFollowLoading,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxHeart,
                                contentDescription = stringResource(Res.string.collection_details_follow),
                                tint = AppTheme.colors.onSurfaceVariant)
                        }
                    }
                }
                if (onEdit != null) {
                    MillenniumSeparator(modifier = Modifier.height(24.dp), vertical = true)
                    MillenniumIconButton(
                        onClick = onEdit,
                        style = MillenniumIconButtonStyle.Quiet) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxEdit,
                            contentDescription = stringResource(Res.string.collection_details_edit_playlist))
                    }
                }
            }
        }
        val ownerInfo = @Composable {
            Row(
                modifier = Modifier.clickable(onClick = onOwnerClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ownerImageURL != null) {
                    AsyncImage(
                        model = ownerImageURL,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .shimmerApply(),
                        contentScale = ContentScale.Crop)
                } else {
                    MillenniumIcon(
                        imageVector = Iconsax.User,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                AppTheme.colors.primaryContainer,
                                CircleShape
                            )
                            .padding(6.dp),
                        tint = AppTheme.colors.onPrimaryContainer)
                }
                TextWithShimmer(
                    text = stringResource(Res.string.collection_details_by_owner, ownerName),
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.onSurface)
            }
        }

        // Shuffle + large round play, aligned to the end of the actions row.
        val roundPlayControls = @Composable {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MillenniumIconButton(
                    onClick = onShufflePlay,
                    loading = isShuffleLoading,
                    style = MillenniumIconButtonStyle.Quiet) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxShuffle,
                        contentDescription = stringResource(Res.string.collection_details_shuffle_play),
                        modifier = Modifier.size(26.dp),
                        tint = AppTheme.colors.onSurfaceVariant)
                }
                MorphingPlayButton(
                    isPlaying = isPlaying,
                    onClick = onPlay,
                    loading = isPlayLoading,
                    size = 56.dp,
                    contentDescription = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description))
            }
        }

        val artwork = @Composable {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 232.dp else 220.dp)
                    .shadow(16.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .sharedElementOrNone(
                        key = sharedElementKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope)
                    .shimmerApply(),
                contentAlignment = Alignment.Center) {
                if (imageURL.isNotBlank()) {
                    AsyncImage(
                        model = imageURL,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else if (imageResource != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(imageResource),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else {
                    Text(
                        text = title.take(1).ifBlank { "?" }.uppercase(),
                        style = AppTheme.typography.displaySmall,
                        color = AppTheme.colors.onSurfaceVariant)
                }
            }
        }

        val metadata = @Composable {
            Column {
                TextWithShimmer(
                    text = title,
                    style = if (isCompact) AppTheme.typography.headlineMedium else AppTheme.typography.displaySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)

                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Flat header straight on the page, no card chrome.
        if (isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    artwork()
                }
                metadata()
                ownerInfo()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    actions()
                    Spacer(modifier = Modifier.weight(1f))
                    if (!isJamGuest) roundPlayControls()
                }
                if (isJamGuest) playPauseButton()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Bottom) {
                artwork()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metadata()
                    ownerInfo()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isJamGuest) playPauseButton() else roundPlayControls()
                        Spacer(modifier = Modifier.width(8.dp))
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun CollectionDetailsPreview() {
    CollectionDetails(
        title = "My Playlist",
        description = "A collection of my favorite songs.",
        imageURL = "https://example.com/playlist.jpg",
        ownerName = "John Doe",
        ownerImageURL = "https://example.com/john.jpg",
        onOwnerClick = {},
        onPlay = {},
        onShufflePlay = {},
        onAddToQueue = {})
}
