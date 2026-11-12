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

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.spotube.spotube.core.paths.Paths
import kotlin.random.Random

/**
 * Makes the home requests of the Spotify plugin look like the ones of Spotify's own web player,
 * so the home feed is the personal one, in the user's language:
 *  - `Accept-Language` follows the device language (without it Spotify answers in English);
 *  - `app-platform: WebPlayer` like the web player;
 *  - `sp_t`, the web player's client identifier, which the plugin reads from the login cookies
 *    but is often missing there, leaves Spotify treating the client as unknown (generic home).
 *    When it is missing a stable one is created once and used for the cookie and the query.
 * Only requests to the Spotify GraphQL host that carry an `sp_t` variable are touched.
 */
internal object SpotifyWebClientHints {
    private const val GQL_HOST = "api-partner.spotify.com"
    private val emptySpT = Regex("\"sp_t\"\\s*:\\s*\"\"")
    private val cookieSpT = Regex("(^|;\\s*)sp_t=[^;]+")

    private val clientId: String by lazy {
        val path = "${Paths().getApplicationDataDirPath()}/spotify_sp_t".toPath()
        runCatching {
            if (FileSystem.SYSTEM.exists(path)) FileSystem.SYSTEM.read(path) { readUtf8() }.trim()
            else null
        }.getOrNull()?.takeIf { it.length == 32 } ?: newId().also { id ->
            runCatching { FileSystem.SYSTEM.write(path) { writeUtf8(id) } }
        }
    }

    private fun newId(): String {
        val hex = "0123456789abcdef"
        return buildString { repeat(32) { append(hex[Random.nextInt(16)]) } }
    }

    fun adapt(url: String, headers: Map<String, String>?, body: String?): Pair<Map<String, String>?, String?> {
        if (body == null || !url.contains(GQL_HOST) || !body.contains("\"sp_t\"")) return headers to body
        return runCatching {
            val result = (headers ?: emptyMap()).toMutableMap()
            fun has(name: String) = result.keys.any { it.equals(name, ignoreCase = true) }
            if (!has("Accept-Language")) {
                result["Accept-Language"] = RealSystemInformationAPI().getLocale() + ",en;q=0.8"
            }
            if (!has("app-platform")) result["app-platform"] = "WebPlayer"
            var newBody: String = body
            val cookieKey = result.keys.firstOrNull { it.equals("Cookie", ignoreCase = true) } ?: "Cookie"
            val cookie = result[cookieKey].orEmpty()
            if (!cookieSpT.containsMatchIn(cookie)) {
                val id = clientId
                result[cookieKey] = if (cookie.isBlank()) "sp_t=$id" else "$cookie; sp_t=$id"
                newBody = emptySpT.replace(body, "\"sp_t\":\"$id\"")
            }
            result to newBody
        }.getOrDefault(headers to body)
    }
}
