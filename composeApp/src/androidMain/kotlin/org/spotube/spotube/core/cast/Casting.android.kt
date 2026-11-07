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
package org.spotube.spotube.core.cast

import androidx.annotation.OptIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.media3.cast.Cast
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.util.UnstableApi
import org.spotube.spotube.core.ui.theming.AppTheme

actual fun supportsCasting(): Boolean = true

@OptIn(UnstableApi::class)
@Composable
actual fun CastButton(modifier: Modifier) {
    // Cast is initialized in MyApplication; without Google Play services it never becomes
    // ready, so the button stays hidden instead of crashing.
    val context = androidx.compose.ui.platform.LocalContext.current
    val cast = runCatching { Cast.getSingletonInstance(context) }.getOrNull() ?: return
    if (cast.needsInitialization() || cast.castContextLoadFailure != null) return
    CompositionLocalProvider(LocalContentColor provides AppTheme.colors.onSurfaceVariant) {
        MediaRouteButton(modifier = modifier)
    }
}
