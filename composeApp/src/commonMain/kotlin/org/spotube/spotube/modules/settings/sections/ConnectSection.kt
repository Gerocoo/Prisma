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

import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.foundation.lazy.LazyListScope
import org.spotube.spotube.core.ui.theming.AppTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_screen_title
import spotube.composeapp.generated.resources.nav_devices
import spotube.composeapp.generated.resources.nav_group_connect

/** Devices and Jam: on phones they left the Home header, so they live here. */
internal fun LazyListScope.connectSection(navigationCommands: NavigationCommands) {
    settingsSectionHeader(Res.string.nav_group_connect)
    settingsSectionCard(
        items = listOf(
            {
                val title = stringResource(Res.string.nav_devices)
                SettingCardItem(
                    title = title,
                    icon = { SettingsItemIcon(Iconsax.IconsaxMirrorScreenRegular, title) },
                    trailingContent = {
                        MillenniumIcon(FeatherIcons.ChevronRight, title, tint = AppTheme.colors.onSurfaceVariant)
                    },
                    onClick = { navigationCommands.navigateTo(Routes.Devices) })
            },
            {
                val title = stringResource(Res.string.jam_screen_title)
                SettingCardItem(
                    title = title,
                    icon = { SettingsItemIcon(Iconsax.IconsaxSoundTwotone, title) },
                    trailingContent = {
                        MillenniumIcon(FeatherIcons.ChevronRight, title, tint = AppTheme.colors.onSurfaceVariant)
                    },
                    onClick = { navigationCommands.navigateTo(Routes.Jam) })
            })
    )
}
