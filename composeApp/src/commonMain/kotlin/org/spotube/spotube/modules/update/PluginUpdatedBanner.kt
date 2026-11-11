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

package org.spotube.spotube.modules.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.plugin_updated_banner

/**
 * Pill that drops in from the top after a plugin updated itself ("Spotify aggiornato alla 1.1.0")
 * and leaves after a few seconds, or on tap.
 */
@Composable
fun PluginUpdatedBanner(
    notice: PluginUpdatedNotice?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keeps the last text while the pill slides out.
    var shown by remember { mutableStateOf<PluginUpdatedNotice?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            shown = notice
            delay(5_000)
            onDismiss()
        }
    }
    Box(
        modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notice != null,
            enter = slideInVertically { -it * 2 } + fadeIn(),
            exit = slideOutVertically { -it * 2 } + fadeOut()
        ) {
            val current = shown ?: return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppTheme.colors.primary)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxCheckCircle,
                    contentDescription = null,
                    tint = AppTheme.colors.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.plugin_updated_banner, current.pluginName, current.version),
                    style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
