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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * Material 3 switch with icons, as in Android's own settings: outlined track and a grey thumb
 * with an "x" when off, accent track and a light thumb with a check mark when on.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val bouncy = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.6f, stiffness = 500f)
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 4.dp, bouncy, label = "switchThumb")
    val track by animateColorAsState(if (checked) colors.primary else colors.surfaceContainerHighest, label = "switchTrack")
    val outline by animateColorAsState(if (checked) colors.primary else colors.onSurfaceVariant, label = "switchOutline")
    val thumb by animateColorAsState(if (checked) colors.onPrimary else colors.onSurfaceVariant, label = "switchThumbColor")
    val iconTint by animateColorAsState(if (checked) colors.primary else colors.surfaceContainerHighest, label = "switchIcon")
    val toggle = if (onCheckedChange != null) Modifier.toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Switch,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onValueChange = onCheckedChange
    ) else Modifier
    Box(
        modifier
            .then(toggle)
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .border(2.dp, outline, RoundedCornerShape(50)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier.offset(x = thumbOffset).size(24.dp).clip(CircleShape).background(thumb),
            contentAlignment = Alignment.Center
        ) {
            Image(
                if (checked) CheckGlyph else CrossGlyph, null,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(iconTint)
            )
        }
    }
}

private fun glyph(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2.6f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block
        )
    }.build()

private val CheckGlyph: ImageVector by lazy { glyph("SwitchCheck") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) } }
private val CrossGlyph: ImageVector by lazy { glyph("SwitchCross") { moveTo(7f, 7f); lineTo(17f, 17f); moveTo(17f, 7f); lineTo(7f, 17f) } }
