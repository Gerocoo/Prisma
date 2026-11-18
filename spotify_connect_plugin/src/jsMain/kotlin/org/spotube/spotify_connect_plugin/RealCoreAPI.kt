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

package org.spotube.spotify_connect_plugin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.swiftzer.semver.SemVer
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.PluginUpdateInfo

/**
 * This plugin has no login of its own: it borrows the Spotify metadata plugin's session via
 * SpotifyAuthAPI, so there is nothing for the user to log in or out of here.
 */
class RealCoreAPI : CoreAPI {
    override suspend fun checkPluginUpdates(currentVersion: SemVer): PluginUpdateInfo? = null

    override fun supportMarkdownText(currentVersion: SemVer): String =
        "Controls Spotify Connect devices using the Spotify plugin's own login."

    override val requiresAuthentication = false

    private val stateFlow = MutableStateFlow(true)
    override val loggedInFlow: StateFlow<Boolean> = stateFlow.asStateFlow()

    override suspend fun login() {}
    override suspend fun logout() {}
}
