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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.spotube.millennium.composables.MillenniumListTileTheme
import org.spotube.millennium.theme.MillenniumTheme

private val NoWash: Brush = SolidColor(Color.Transparent)

/**
 * Millennium's list row with the app's flat look. The library ignores the theme background and
 * paints its own accent gradient on focus, hover and press (the "glass" left on a row after a
 * dropdown closes), so unless a background is given, a transparent one is always passed.
 * Selected rows keep the theme's selected fill.
 */
@Composable
fun MillenniumListTile(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRightClick: ((Offset) -> Unit)? = null,
    titleStyle: TextStyle? = null,
    subtitleStyle: TextStyle? = null,
    titleForeground: Color? = null,
    subtitleForeground: Color? = null,
    selectedTitleForeground: Color? = null,
    selectedSubtitleForeground: Color? = null,
    background: Brush? = null,
    selectedBackground: Brush? = null,
    selectedGelStrength: Float? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    leadingSpacing: Dp? = null,
    trailingSpacing: Dp? = null,
    titleSpacing: Dp? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    innerGlowColor: Color? = null,
    alpha: Float? = null,
    stateAnimationMillis: Int? = null,
    theme: MillenniumListTileTheme = MillenniumTheme.listTileTheme,
) = org.spotube.millennium.composables.MillenniumListTile(
    title = title, modifier = modifier, leading = leading, subtitle = subtitle, trailing = trailing,
    selected = selected, enabled = enabled, onClick = onClick, onRightClick = onRightClick,
    titleStyle = titleStyle, subtitleStyle = subtitleStyle, titleForeground = titleForeground,
    subtitleForeground = subtitleForeground, selectedTitleForeground = selectedTitleForeground,
    selectedSubtitleForeground = selectedSubtitleForeground,
    background = background ?: if (selected) null else NoWash,
    selectedBackground = selectedBackground, selectedGelStrength = selectedGelStrength, shape = shape,
    contentPadding = contentPadding, minimumHeight = minimumHeight, leadingSpacing = leadingSpacing,
    trailingSpacing = trailingSpacing, titleSpacing = titleSpacing,
    borderWidth = borderWidth ?: Dp(0f), borderColor = borderColor ?: Color.Transparent,
    innerGlowColor = innerGlowColor ?: Color.Transparent, alpha = alpha,
    stateAnimationMillis = stateAnimationMillis, theme = theme
)
