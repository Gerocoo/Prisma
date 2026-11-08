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
package org.spotube.spotube.core.spotify

import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.spotube.spotube.core.db.Database
import kotlin.time.Clock

/**
 * Access token of the Spotify metadata plugin, read from the storage the plugin keeps refreshed
 * ("<plugin id>:credentials"). Null when the plugin is not installed or not logged in.
 */
class SpotifyPluginAuth(private val database: Database) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class PluginCredentials(val accessToken: String, val expiration: Long = 0L)

    suspend fun accessToken(): String? {
        val prefs = database.pluginsDataStore.data.first()
        val now = Clock.System.now().toEpochMilliseconds()
        return prefs.asMap().entries
            .filter { it.key.name.endsWith(":credentials") }
            .mapNotNull { runCatching { json.decodeFromString<PluginCredentials>(it.value.toString()) }.getOrNull() }
            .firstOrNull { it.accessToken.isNotBlank() && (it.expiration == 0L || it.expiration > now) }
            ?.accessToken
    }
}
