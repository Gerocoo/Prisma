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
package org.spotube.plugin_interfaces.host_apis

import app.cash.zipline.ZiplineService

const val SpotifyAuthAPI_SERVICE_NAME = "SpotifyAuthAPI"

/**
 * Narrow, purpose-built host API: hands a device-control plugin the Spotify metadata plugin's
 * own access token, so it can authenticate Spotify Connect calls without ever implementing
 * Spotify login itself. The host reads the token from wherever the metadata plugin keeps it
 * refreshed; the plugin only ever sees the current bearer token string.
 */
interface SpotifyAuthAPI : ZiplineService {
    /** Current access token of the logged-in Spotify metadata plugin, or null if not logged in. */
    suspend fun accessToken(): String?
}
