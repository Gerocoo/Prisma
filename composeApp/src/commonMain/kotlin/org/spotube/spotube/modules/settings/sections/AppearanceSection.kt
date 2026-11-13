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

import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogFooter
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import com.composeunstyled.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.isLightAppearance
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SelectionSettingCard
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.core.ui.theming.supportsSystemDynamicColors
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxColorSwatch
import org.spotube.spotube.resources.iconsax.IconsaxMagic
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumText
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumThemeConfig
import org.spotube.millennium.theme.resolve
import spotube.composeapp.generated.resources.*

internal fun LazyListScope.appearanceSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_appearance)
    }
    val showSystemColors = supportsSystemDynamicColors()
    settingsSectionCard(
        items = buildList<@Composable () -> Unit> {
            add {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_theme_title),
                    subtitle = stringResource(
                        Res.string.settings_theme_subtitle_current,
                        settings.theme.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxColorSwatch, stringResource(Res.string.settings_theme_title))
                    },
                    selectedOption = settings.theme,
                    options = Theme.entries,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { theme ->
                        settingsViewModel.updateSettings {
                            copy(theme = theme)
                        }
                    }
                )
            }
            if (showSystemColors) add {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_system_colors_title),
                    subtitle = stringResource(Res.string.settings_system_colors_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxColorSwatch,
                            stringResource(Res.string.settings_system_colors_title)
                        )
                    },
                    checked = settings.useSystemColors,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(useSystemColors = enabled)
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun Theme.displayLabel(): String {
    return when (this) {
        Theme.LIGHT -> stringResource(Res.string.settings_theme_light)
        Theme.DARK -> stringResource(Res.string.settings_theme_dark)
        Theme.SYSTEM -> stringResource(Res.string.settings_theme_system)
    }
}

/** The accent of a built-in palette, used as the starting colour of the picker. */
@Composable
private fun presetAccent(palette: MillenniumPalette): Color {
    val dark = !isLightAppearance()
    return remember(palette, dark) { MillenniumColorSchemes().resolve(palette, dark = dark).accent }
}

private val AccentPresets = listOf(
    Color(0xFF3F51E0), Color(0xFFB45F00), Color(0xFF00796B), Color(0xFFD81B60),
    Color(0xFF8E24AA), Color(0xFF2E7D32), Color(0xFFE53935), Color(0xFF0288D1)
)

@Composable
private fun AppearanceSettingCard(
    selectedAccent: Color,
    icon: (@Composable () -> Unit)? = null,
    onSaved: (Color) -> Unit) {
    var isSheetOpen by remember { mutableStateOf(false) }

    SettingCardItem(
        title = stringResource(Res.string.settings_appearance_title),
        subtitle = stringResource(Res.string.settings_accent_subtitle),
        icon = icon,
        trailingContent = {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(selectedAccent)
                    .border(1.dp, AppTheme.colors.outlineVariant, CircleShape))
        },
        onClick = { isSheetOpen = true })

    if (isSheetOpen) {
        var draft by remember(selectedAccent) { mutableStateOf(selectedAccent) }
        val title = stringResource(Res.string.settings_appearance_title)
        AdaptiveDialogBottomSheet(
            onDismiss = { isSheetOpen = false },
            dialogTitle = title,
            title = { Text(title, style = AppTheme.typography.headlineSmall) },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    MillenniumButton(
                        onClick = { isSheetOpen = false },
                        style = MillenniumButtonStyle.Secondary) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                    MillenniumButton(onClick = {
                        onSaved(draft)
                        isSheetOpen = false
                    }) {
                        Text(stringResource(Res.string.common_save))
                    }
                }
            }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(Res.string.settings_accent_description),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant)
                AccentPreview(draft)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentPresets.forEach { preset ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(preset)
                                .border(
                                    width = if (preset == draft) 3.dp else 1.dp,
                                    color = if (preset == draft) AppTheme.colors.onSurface else AppTheme.colors.outlineVariant,
                                    shape = CircleShape)
                                .clickable { draft = preset })
                    }
                }
                AccentPicker(draft) { draft = it }
            }
        }
    }
}

@Composable
private fun AccentPreview(color: Color) {
    val onColor = if (color.luminance() > 0.5f) Color(0xFF111111) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppTheme.colors.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(color)
                .padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(stringResource(Res.string.settings_preview_button), color = onColor)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(stringResource(Res.string.settings_preview_secondary), color = AppTheme.colors.onSurface)
        }
    }
}

/** Free colour selection: hue, saturation and brightness bars. */
@Composable
private fun AccentPicker(color: Color, onChange: (Color) -> Unit) {
    val hsv = remember(color) {
        val hsvArray = FloatArray(3)
        rgbToHsv(color.red, color.green, color.blue, hsvArray)
        hsvArray
    }
    val hue = hsv[0]
    val saturation = hsv[1]
    val value = hsv[2]
    ColorBar(
        label = stringResource(Res.string.settings_accent_hue),
        fraction = hue / 360f,
        colors = List(7) { hsvToColor(it * 60f, 1f, 1f) },
        onFraction = { onChange(hsvToColor(it * 360f, maxOf(saturation, 0.35f), maxOf(value, 0.5f))) })
    ColorBar(
        label = stringResource(Res.string.settings_accent_saturation),
        fraction = saturation,
        colors = listOf(hsvToColor(hue, 0f, value), hsvToColor(hue, 1f, value)),
        onFraction = { onChange(hsvToColor(hue, it, value)) })
    ColorBar(
        label = stringResource(Res.string.settings_accent_brightness),
        fraction = value,
        colors = listOf(Color.Black, hsvToColor(hue, saturation, 1f)),
        onFraction = { onChange(hsvToColor(hue, saturation, maxOf(it, 0.15f))) })
}

@Composable
private fun ColorBar(
    label: String,
    fraction: Float,
    colors: List<Color>,
    onFraction: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = AppTheme.typography.labelLarge, color = AppTheme.colors.onSurface)
        var widthPx by remember { mutableStateOf(1f) }
        Box(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(colors))
                .pointerInput(Unit) {
                    detectTapGestures { onFraction((it.x / widthPx).coerceIn(0f, 1f)) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onFraction((change.position.x / widthPx).coerceIn(0f, 1f))
                    }
                }) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(((widthPx - 28.dp.toPx()) * fraction.coerceIn(0f, 1f)).toInt(), 0) }
                    .padding(2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0x33000000), CircleShape))
        }
    }
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun rgbToHsv(r: Float, g: Float, b: Float, out: FloatArray) {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    out[0] = if (h < 0f) h + 360f else h
    out[1] = if (max == 0f) 0f else d / max
    out[2] = max
}
