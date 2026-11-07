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

import androidx.compose.runtime.Composable
import org.spotube.millennium.theme.MillenniumColors

/** Whether the platform offers wallpaper-based system colors (Android 12+ Material You / Monet). */
expect fun supportsSystemDynamicColors(): Boolean

/**
 * The system's wallpaper-based palette mapped onto Millennium roles, or null where the platform
 * has none. [dark] picks the dark or light tone set.
 */
@Composable
expect fun systemDynamicColors(dark: Boolean): MillenniumColors?
