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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme

private val IndicatorHeight = 40.dp

/**
 * Material 3 Expressive connected toggle: a tonal pill track with a single accent pill that
 * slides to the selected option, same stretch-and-settle feel as [org.spotube.spotube.modules.shell.AppBottombar]'s
 * indicator (fast leading edge, slow trailing edge).
 *
 * When [progress] is supplied (e.g. a pager's `currentPage + currentPageOffsetFraction`), the
 * indicator tracks it directly instead of animating on its own — the caller's own continuous
 * motion (a drag, or an already-animated scroll) is smooth by itself, and layering a spring on
 * top of it is what caused multiple pills to flash at once while scrolling past intermediate
 * tabs.
 */
@Composable
fun ExpressiveToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    container: Color = AppTheme.colors.surfaceContainerHigh,
    accent: Color = AppTheme.colors.primary,
    onAccent: Color = AppTheme.colors.onPrimary,
    content: Color = AppTheme.colors.onSurface,
) {
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(50)).background(container).padding(4.dp)
    ) {
        val itemCount = options.size
        val itemWidth = maxWidth / itemCount
        val rawTarget = (progress ?: selectedIndex.toFloat()).coerceIn(0f, (itemCount - 1).toFloat())
        val restLeft = itemWidth * rawTarget

        // Discrete tap target: reproduce the nav bar's liquid-pill stretch (fast leading edge,
        // slow trailing edge). Driven externally by a pager instead: snap straight to it every
        // recomposition, so the indicator tracks the drag/scroll 1:1 with no extra spring on top.
        val last = remember { floatArrayOf(rawTarget, 1f) }
        if (rawTarget != last[0]) {
            last[1] = if (rawTarget > last[0]) 1f else -1f
            last[0] = rawTarget
        }
        val goingRight = last[1] > 0
        val fast = spring<Dp>(dampingRatio = 0.72f, stiffness = 520f)
        val slow = spring<Dp>(dampingRatio = 0.62f, stiffness = 220f)
        val snapSpec = snap<Dp>()
        val left by animateDpAsState(
            restLeft,
            if (progress != null) snapSpec else if (goingRight) slow else fast,
            label = "toggleLeft"
        )
        val right by animateDpAsState(
            restLeft + itemWidth,
            if (progress != null) snapSpec else if (goingRight) fast else slow,
            label = "toggleRight"
        )

        Box(
            Modifier
                .offset(x = left)
                .width((right - left).coerceAtLeast(0.dp))
                .height(IndicatorHeight)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )

        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val fg by animateColorAsState(if (selected) onAccent else content)
                Box(
                    Modifier.weight(1f).heightIn(min = 40.dp)
                        .clickable(role = Role.Tab) { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = AppTheme.typography.labelLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
