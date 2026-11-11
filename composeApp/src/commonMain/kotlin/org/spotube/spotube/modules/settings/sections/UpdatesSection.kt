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

import androidx.compose.runtime.Composable
import spotube.composeapp.generated.resources.settings_updates_up_to_date
import spotube.composeapp.generated.resources.settings_updates_check_now_subtitle
import spotube.composeapp.generated.resources.settings_updates_check_now_title
import org.spotube.spotube.resources.iconsax.IconsaxDocumentDownload
import org.spotube.spotube.modules.settings.components.SettingCardItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import org.spotube.spotube.AppBuildInfo
import org.spotube.spotube.modules.update.AppUpdateService
import org.koin.compose.koinInject
import androidx.compose.foundation.lazy.LazyListScope
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxRefreshArrow2
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_section_updates
import spotube.composeapp.generated.resources.settings_updates_auto_check_subtitle
import spotube.composeapp.generated.resources.settings_updates_auto_check_title
import spotube.composeapp.generated.resources.settings_updates_auto_plugins_subtitle
import spotube.composeapp.generated.resources.settings_updates_auto_plugins_title

internal fun LazyListScope.updatesSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_updates)
    }
    settingsSectionCard(
        items = listOf<@Composable () -> Unit>({
            SwitchSettingCard(
                title = stringResource(Res.string.settings_updates_auto_check_title),
                subtitle = stringResource(Res.string.settings_updates_auto_check_subtitle),
                icon = {
                    SettingsItemIcon(
                        Iconsax.IconsaxRefreshArrow2,
                        stringResource(Res.string.settings_updates_auto_check_title)
                    )
                },
                checked = settings.autoCheckForUpdates,
                onCheckedChange = { enabled ->
                    settingsViewModel.updateSettings {
                        copy(autoCheckForUpdates = enabled)
                    }
                }
            )
        }, {
            SwitchSettingCard(
                title = stringResource(Res.string.settings_updates_auto_plugins_title),
                subtitle = stringResource(Res.string.settings_updates_auto_plugins_subtitle),
                icon = {
                    SettingsItemIcon(
                        Iconsax.IconsaxRefreshArrow2,
                        stringResource(Res.string.settings_updates_auto_plugins_title)
                    )
                },
                checked = settings.autoUpdatePlugins,
                onCheckedChange = { enabled ->
                    settingsViewModel.updateSettings {
                        copy(autoUpdatePlugins = enabled)
                    }
                }
            )
        }, {
            // Manual check: opens the update dialog (download + install inside the app) when a
            // newer build exists, otherwise says so in the subtitle.
            val updateService = koinInject<AppUpdateService>()
            val upToDate by updateService.upToDate.collectAsState()
            SettingCardItem(
                title = stringResource(Res.string.settings_updates_check_now_title),
                subtitle = if (upToDate == true) stringResource(Res.string.settings_updates_up_to_date)
                    else stringResource(Res.string.settings_updates_check_now_subtitle, AppBuildInfo.BUILD_NUMBER),
                icon = {
                    SettingsItemIcon(
                        Iconsax.IconsaxDocumentDownload,
                        stringResource(Res.string.settings_updates_check_now_title)
                    )
                },
                onClick = updateService::checkNow
            )
        })
    )
}

