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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * Material 3 Expressive connected toggle: a tonal pill track holding one pill per option; the
 * selected option is filled with the accent.
 */
@Composable
fun ExpressiveToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    container: Color = AppTheme.colors.surfaceContainerHigh,
    accent: Color = AppTheme.colors.primary,
    onAccent: Color = AppTheme.colors.onPrimary,
    content: Color = AppTheme.colors.onSurface,
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(container).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(if (selected) accent else Color.Transparent)
            val fg by animateColorAsState(if (selected) onAccent else content)
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(50)).background(bg)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = AppTheme.typography.labelLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
