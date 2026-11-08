/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalTextStyle
import coil3.compose.AsyncImage
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.component.MillenniumListTile
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.materialSurface
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayableListTile(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedElementKey: String? = null,
    playLoading: Boolean = false,
    addToQueueLoading: Boolean = false,
    imageModel: Any? = imageURL,
    imageShape: Shape = RoundedCornerShape(14.dp),
    placeholderIcon: ImageVector = Iconsax.IconsaxMusic,
    artwork: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isBlacklisted: Boolean = false,
    compact: Boolean = false
) {
    val colors = MillenniumTheme.colors
    MillenniumListTile(
        modifier = modifier,
        onClick = onClick ?: onPlay,
        selected = isPlaying,
        background = SolidColor(
            if (isPlaying) AppTheme.colors.primary.copy(alpha = 0.16f)
            else AppTheme.colors.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp),
        borderColor = if (isBlacklisted) colors.danger.copy(alpha = 0.5f) else null,
        titleForeground = if (isBlacklisted) colors.danger else null,
        borderWidth = if (isBlacklisted) 1.dp else 0.dp,
        contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 8.dp),
        minimumHeight = if (compact) 52.dp else 76.dp,
        leading = {
            MediaArtwork(
                imageModel, title,
                Modifier.size(if (compact) 36.dp else 52.dp).sharedElementOrNone(
                    sharedElementKey,
                    LocalSharedTransitionScope.current,
                    LocalAnimatedVisibilityScope.current
                ),
                imageShape, placeholderIcon, artwork
            )
        },
        title = {
            TextWithShimmer(
                title,
                style = AppTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        subtitle = subtitle?.let { text ->
            {
                TextWithShimmer(
                    text,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailing = if (onPlay != null || onAddToQueue != null || trailingContent != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingContent?.invoke()
                    if (onAddToQueue != null && !isPlaying) {
                        MillenniumIconButton(
                            onClick = onAddToQueue,
                            loading = addToQueueLoading,
                            modifier = Modifier.size(if (compact) 32.dp else 40.dp).clip(CircleShape),
                            style = MillenniumIconButtonStyle.Secondary
                        ) {
                            MillenniumIcon(
                                Iconsax.IconsaxAddSquare,
                                stringResource(Res.string.player_add_to_queue_content_description)
                            )
                        }
                    }
                    if (onPlay != null) {
                        MillenniumIconButton(
                            onClick = onPlay,
                            modifier = Modifier.size(if (compact) 32.dp else 40.dp).clip(CircleShape),
                            style = MillenniumIconButtonStyle.Primary
                        ) {
                            MillenniumIcon(
                                if (isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                                stringResource(if (isPlaying) Res.string.player_pause_content_description else Res.string.player_play_content_description),
                                tint = LocalTextStyle.current.color
                            )
                        }
                    }
                }
            }
        } else null
    )
}

@Composable
internal fun MediaArtwork(
    model: Any?,
    title: String,
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    placeholderIcon: ImageVector = Iconsax.IconsaxMusic,
    artwork: (@Composable () -> Unit)? = null
) {
    Box(
        // Flat tonal placeholder behind the cover, fully rounded, no outline.
        modifier.materialSurface(
            background = SolidColor(MillenniumTheme.colors.raised),
            shape = shape
        ).clip(shape).shimmerApply(),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) artwork()
        else if (model != null) AsyncImage(
            model,
            title,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        else MillenniumIcon(
            placeholderIcon,
            null,
            modifier = Modifier.size(24.dp),
            tint = AppTheme.colors.onSurfaceVariant
        )
    }
}
