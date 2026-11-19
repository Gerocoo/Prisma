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

import androidx.compose.foundation.lazy.LazyListScope
import spotube.composeapp.generated.resources.*
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import org.jetbrains.compose.resources.stringResource

/**
 * Android only: lets this phone itself become a Spotify Connect target (via the native
 * librespot receiver), instead of only controlling other devices like [connectSection] does.
 */
internal fun LazyListScope.connectReceiverSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel) {
    settingsSectionCard(
        items = listOf(
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_connect_receiver_title),
                    subtitle = stringResource(Res.string.settings_connect_receiver_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxMirrorScreenRegular,
                            stringResource(Res.string.settings_connect_receiver_title)
                        )
                    },
                    checked = settings.spotifyConnectReceiverEnabled,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(spotifyConnectReceiverEnabled = enabled)
                        }
                    }
                )
            })
    )
}
