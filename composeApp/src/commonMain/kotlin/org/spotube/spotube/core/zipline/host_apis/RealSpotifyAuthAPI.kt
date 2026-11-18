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

package org.spotube.spotube.core.zipline.host_apis

import org.spotube.plugin_interfaces.host_apis.SpotifyAuthAPI
import org.spotube.spotube.core.spotify.SpotifyPluginAuth

/**
 * Host-side implementation of [SpotifyAuthAPI]: just forwards to [SpotifyPluginAuth], which
 * already reads the Spotify metadata plugin's token from the host's own storage. The token
 * never passes through this plugin's JS sandbox except as this one string.
 */
class RealSpotifyAuthAPI(private val auth: SpotifyPluginAuth) : SpotifyAuthAPI {
    override suspend fun accessToken(): String? = auth.accessToken()
}
