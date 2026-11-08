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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.composables.MillenniumTextFieldTheme
import org.spotube.millennium.composables.MillenniumTextFieldThemeProperties
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import org.spotube.spotube.resources.iconsax.IconsaxSearch

/**
 * Material 3 Expressive search bar theme: a full pill on the high tonal surface, tall touch
 * target, room on the left for the magnifier and on the right for the clear button.
 */
@Composable
fun expressiveSearchFieldTheme(): MillenniumTextFieldTheme {
    val base = MillenniumTheme.textFieldTheme
    val fill = SolidColor(AppTheme.colors.surfaceContainerHigh)
    val f = { p: MillenniumTextFieldThemeProperties ->
        p.copy(
            shape = RoundedCornerShape(50), background = fill, borderWidth = 0.dp,
            minimumHeight = 52.dp,
            contentPadding = PaddingValues(start = 48.dp, end = 44.dp, top = 14.dp, bottom = 14.dp),
            insetShadeColor = null, innerGlowColor = null, bottomRimColor = null
        )
    }
    val s = base.states
    return MillenniumTextFieldTheme(
        MillenniumControlStateProperty(
            default = f(s.default), hovered = f(s.default), focused = f(s.focused),
            pressed = f(s.default), disabled = f(s.disabled), selected = f(s.selected), error = f(s.error)
        )
    )
}

/** Magnifier on the left and clear button on the right, drawn over a pill search field. */
@Composable
fun ExpressiveSearchDecorations(state: TextFieldState, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.CenterStart) {
        content()
        MillenniumIcon(
            Iconsax.IconsaxSearch, null,
            modifier = Modifier.padding(start = 18.dp).size(20.dp),
            tint = AppTheme.colors.onSurfaceVariant
        )
        if (state.text.isNotEmpty()) Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(36.dp).clip(CircleShape)
                .clickable(role = Role.Button) { state.clearText() },
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(ClearIcon, null, modifier = Modifier.size(18.dp), tint = AppTheme.colors.onSurfaceVariant)
        }
    }
}

/** Pill search field (Material 3 Expressive) used by every search and filter box. */
@Composable
fun ExpressiveSearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        ExpressiveSearchDecorations(state) {
            MillenniumTextField(
                state = state,
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth(),
                theme = expressiveSearchFieldTheme()
            )
        }
    }
}

/** Plain round-capped "x" for the clear button. */
private val ClearIcon: ImageVector by lazy {
    ImageVector.Builder("Clear", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }.build()
}
