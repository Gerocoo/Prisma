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

import android.content.Context
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.spotube.millennium.theme.MillenniumColors

actual fun supportsSystemDynamicColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun systemDynamicColors(dark: Boolean): MillenniumColors? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    // Re-read when the configuration changes (Android recreates it after a wallpaper/palette change).
    val configuration = LocalConfiguration.current
    return remember(context, configuration, dark) { monetColors(context, dark) }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun monetColors(context: Context, dark: Boolean): MillenniumColors {
    fun c(@ColorRes id: Int) = Color(context.getColor(id))
    val accent = if (dark) c(android.R.color.system_accent1_200) else c(android.R.color.system_accent1_600)
    val onAccent = if (dark) c(android.R.color.system_accent1_800) else c(android.R.color.system_accent1_0)
    return if (dark) {
        val ground = c(android.R.color.system_neutral1_900)
        val raised = c(android.R.color.system_neutral2_800)
        MillenniumColors(
            background = ground,
            surface = lerp(ground, raised, 0.35f),
            raised = lerp(ground, raised, 0.7f),
            inset = lerp(ground, Color.Black, 0.3f),
            text = c(android.R.color.system_neutral1_50),
            muted = c(android.R.color.system_neutral2_200),
            border = c(android.R.color.system_neutral2_700),
            highlight = c(android.R.color.system_neutral2_600),
            accent = accent, accentTop = accent, accentBottom = accent, onAccent = onAccent,
            success = Color(0xFF86EFAC), danger = Color(0xFFFF8A80), scrim = Color(0xCC050505)
        )
    } else {
        MillenniumColors(
            background = c(android.R.color.system_neutral1_10),
            surface = c(android.R.color.system_neutral1_50),
            raised = Color.White,
            inset = c(android.R.color.system_neutral1_100),
            text = c(android.R.color.system_neutral1_900),
            muted = c(android.R.color.system_neutral2_700),
            border = c(android.R.color.system_neutral2_300),
            highlight = Color.White,
            accent = accent, accentTop = accent, accentBottom = accent, onAccent = onAccent,
            success = Color(0xFF166534), danger = Color(0xFFB42332), scrim = Color(0x99101418)
        )
    }
}
