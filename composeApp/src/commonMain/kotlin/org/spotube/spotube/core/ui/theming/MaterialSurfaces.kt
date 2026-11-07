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
package org.spotube.spotube.core.ui.theming

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.spotube.millennium.theme.MillenniumColors

/**
 * Material 3 surface: clipped to [shape] and filled flat. Drop-in replacement for the glossy
 * millenniumSurface; border, glow and shadow arguments are accepted but deliberately ignored,
 * because Material separates surfaces by tone, not by outlines or bevels.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.materialSurface(
    background: Brush,
    shape: Shape,
    borderColor: Color? = null,
    borderWidth: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    innerGlowColor: Color? = null,
): Modifier = clip(shape).background(background, shape)

/** Flat tonal fill used instead of Millennium's glossy panel gradient. */
fun materialPanelBrush(colors: MillenniumColors): Brush = SolidColor(colors.surface)
