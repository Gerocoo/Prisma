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

package org.spotube.spotube.modules.settings.sections


import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import org.spotube.spotube.core.server.isContainerPlayableOn
import org.spotube.spotube.core.server.currentPlatform
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.materialPanelBrush
import org.spotube.spotube.core.ui.theming.materialSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioFormat
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioQuality
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_audio_format_label
import spotube.composeapp.generated.resources.settings_audio_quality_lossless
import spotube.composeapp.generated.resources.settings_audio_quality_lossy

private val standardLossyQualities = listOf(
    AudioQuality.Lossy(bitrate = 44_000),
    AudioQuality.Lossy(bitrate = 96_000),
    AudioQuality.Lossy(bitrate = 128_000),
    AudioQuality.Lossy(bitrate = 256_000))

// Formats the platform's player can't open aren't offered (iOS: no WebM/Ogg).
internal val streamingFormatPresets = listOf(
    AudioFormat(
        codec = "opus",
        container = "webm",
        qualities = standardLossyQualities),
    AudioFormat(
        codec = "aac",
        container = "mp4",
        qualities = standardLossyQualities),
    AudioFormat(
        codec = "vorbis",
        container = "ogg",
        qualities = standardLossyQualities)
).filter { isContainerPlayableOn(it.container, currentPlatform) }


internal fun LazyListScope.settingsSectionHeader(title: StringResource) {
    item {
        // Material 3: section titles in the accent color, aligned with the cards' content.
        Text(
            text = stringResource(title),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
        )
    }
}

/**
 * A page-level title for a settings destination (used by the large-screen sidebar
 * layout, where each section is its own page rather than one entry in a long list).
 */
@Composable
internal fun SettingsPageTitle(title: StringResource) {
    Text(
        text = stringResource(title),
        style = AppTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
internal fun SettingsItemIcon(
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color = AppTheme.colors.primary) {
    // Icon in a tonal circle, as in Material 3 settings lists.
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center) {
        MillenniumIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp))
    }
}

internal fun availableAudioFormats(current: AudioFormat, presets: List<AudioFormat>): List<AudioFormat> {
    val options = mutableListOf(current)
    presets.forEach { preset ->
        val alreadyIncluded = options.any {
            it.codec.equals(preset.codec, ignoreCase = true) &&
                it.container.equals(preset.container, ignoreCase = true)
        }
        if (!alreadyIncluded) {
            options += preset
        }
    }
    return options
}

internal fun availableAudioQualities(format: AudioFormat, current: AudioQuality): List<AudioQuality> {
    val options = format.qualities.toMutableList()
    if (options.none { it == current }) {
        options += current
    }
    return options
}

@Composable
internal fun AudioFormat.displayLabel(): String {
    return stringResource(Res.string.settings_audio_format_label, codec.uppercase(), container.uppercase())
}

internal fun AudioFormat.preferredQuality(): AudioQuality {
    return qualities.lastOrNull() ?: AudioQuality.Lossy(bitrate = 256_000)
}

internal fun AudioFormat.resolveQuality(preferred: AudioQuality): AudioQuality {
    return qualities.firstOrNull { it == preferred } ?: preferredQuality()
}

@Composable
internal fun AudioQuality.displayLabel(): String {
    return when (this) {
        is AudioQuality.Lossy -> stringResource(Res.string.settings_audio_quality_lossy, bitrate / 1000)
        is AudioQuality.Lossless -> {
            val sampleRateLabel = if (sampleRate % 1000 == 0) {
                "${sampleRate / 1000}"
            } else {
                "${sampleRate / 1000.0}"
            }
            stringResource(Res.string.settings_audio_quality_lossless, sampleRateLabel, channels)
        }
    }
}

internal fun normalizePath(path: String): String {
    return path.trim().trimEnd('/', '\\')
}

internal fun LazyListScope.settingsSectionCard(
    items: List<@Composable () -> Unit>) {
    item {
        // Material 3 Expressive grouped list: each setting is its own tonal tile, separated by
        // a small gap; the group's outer corners are large, the inner ones small.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, content ->
                val large = 24.dp
                val small = 4.dp
                val shape = RoundedCornerShape(
                    topStart = if (index == 0) large else small,
                    topEnd = if (index == 0) large else small,
                    bottomStart = if (index == items.lastIndex) large else small,
                    bottomEnd = if (index == items.lastIndex) large else small
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(AppTheme.colors.surfaceContainer)
                ) { content() }
            }
        }
    }
}

