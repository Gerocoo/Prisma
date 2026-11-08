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
package org.spotube.spotube.modules.shell.spotify_connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.spotify.SpotifyDevice
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.component.ExpressiveLoadingIndicator
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxVolumeHigh
import org.spotube.spotube.resources.iconsax.IconsaxVolumeLow
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.spotify_connect_active
import spotube.composeapp.generated.resources.spotify_connect_bring_back
import spotube.composeapp.generated.resources.spotify_connect_devices
import spotube.composeapp.generated.resources.spotify_connect_error_failed
import spotube.composeapp.generated.resources.spotify_connect_error_no_devices
import spotube.composeapp.generated.resources.spotify_connect_error_not_logged_in
import spotube.composeapp.generated.resources.spotify_connect_error_premium
import spotube.composeapp.generated.resources.spotify_connect_playing_on
import spotube.composeapp.generated.resources.spotify_connect_title

/** Spotify Connect: devices of the account and remote controls for the one playing. */
@Composable
fun SpotifyConnectSheet(viewModel: SpotifyConnectViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (!state.isOpen) return
    val colors = AppTheme.colors
    AdaptiveDialogBottomSheet(
        onDismiss = viewModel::dismiss,
        dialogTitle = stringResource(Res.string.spotify_connect_title),
        title = { Text(stringResource(Res.string.spotify_connect_title), style = AppTheme.typography.titleLarge, color = colors.onSurface) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicator(size = 40.dp)
                }
            }
            state.error?.let { error ->
                Text(
                    stringResource(
                        when (error) {
                            SpotifyConnectError.NotLoggedIn -> Res.string.spotify_connect_error_not_logged_in
                            SpotifyConnectError.PremiumRequired -> Res.string.spotify_connect_error_premium
                            SpotifyConnectError.NoDevices -> Res.string.spotify_connect_error_no_devices
                            SpotifyConnectError.Failed -> Res.string.spotify_connect_error_failed
                        }
                    ),
                    style = AppTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surfaceContainerHigh)
                        .padding(16.dp)
                )
            }

            // Now playing on Spotify, with remote controls.
            val playback = state.playback
            val item = playback?.item
            if (item != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AsyncImage(
                            model = item.album?.images?.maxByOrNull { it.width ?: 0 }?.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = AppTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.artists.joinToString { it.name }, style = AppTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            playback.device?.name?.let {
                                Text(
                                    stringResource(Res.string.spotify_connect_playing_on, it),
                                    style = AppTheme.typography.labelMedium, color = colors.primary
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundControl(Iconsax.IconsaxVolumeLow) {
                            viewModel.setVolume((playback.device?.volumePercent ?: 50) - 10)
                        }
                        RoundControl(Iconsax.IconsaxPrevious, onClick = viewModel::previous)
                        Box(
                            Modifier.size(width = 76.dp, height = 56.dp).clip(RoundedCornerShape(50))
                                .background(colors.primary).clickable(role = Role.Button, onClick = viewModel::togglePlay),
                            contentAlignment = Alignment.Center
                        ) {
                            MillenniumIcon(
                                if (playback.isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay, null,
                                tint = colors.onPrimary, modifier = Modifier.size(26.dp)
                            )
                        }
                        RoundControl(Iconsax.IconsaxNext, onClick = viewModel::next)
                        RoundControl(Iconsax.IconsaxVolumeHigh) {
                            viewModel.setVolume((playback.device?.volumePercent ?: 50) + 10)
                        }
                    }
                    if (state.isRemoteActive) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(colors.primary.copy(alpha = 0.18f))
                                .clickable(role = Role.Button, onClick = viewModel::bringBack).padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(Res.string.spotify_connect_bring_back), style = AppTheme.typography.labelLarge, color = colors.primary)
                        }
                    }
                }
            }

            if (state.devices.isNotEmpty()) {
                Text(stringResource(Res.string.spotify_connect_devices), style = AppTheme.typography.titleSmall, color = colors.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.devices.forEachIndexed { index, device ->
                        DeviceRow(
                            device = device,
                            first = index == 0,
                            last = index == state.devices.lastIndex,
                            onClick = { viewModel.playOn(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundControl(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(AppTheme.colors.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(icon, null, tint = AppTheme.colors.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DeviceRow(device: SpotifyDevice, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val large = 24.dp
    val small = 6.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = if (first) large else small, topEnd = if (first) large else small,
                    bottomStart = if (last) large else small, bottomEnd = if (last) large else small
                )
            )
            .background(if (device.isActive) colors.primary.copy(alpha = 0.16f) else colors.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                if (device.type.equals("Computer", ignoreCase = true) || device.type.equals("TV", ignoreCase = true))
                    Iconsax.IconsaxMirroringScreen else Iconsax.IconsaxMusic,
                null, tint = colors.primary, modifier = Modifier.size(20.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                device.name, style = AppTheme.typography.bodyLarge.copy(fontWeight = if (device.isActive) FontWeight.Bold else FontWeight.Normal),
                color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(device.type, style = AppTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (device.isActive) {
            Text(stringResource(Res.string.spotify_connect_active), style = AppTheme.typography.labelMedium, color = colors.primary)
        }
    }
}
