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

package org.spotube.spotube.modules.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.spotube.spotube.core.db.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

/** Narrow read-only view over the current [UserSettings], for consumers (like
 * [org.spotube.spotube.core.server.StreamingUrlRepository]) that only need to read the
 * user's streaming preferences without depending on the full [SettingsRepository]. */
interface UserSettingsSource {
    val userSettings: StateFlow<UserSettings>
}

class SettingsRepository(private val database: Database) : UserSettingsSource {
    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("user_settings")
        private val COUNTRY_AUTO_SET_KEY = stringPreferencesKey("country_auto_set")

        /**
         * Lenient so settings written by older builds (which stored `accentColor` /
         * `surfaceTheme` instead of the Millennium `palette`) still decode, and so a
         * missing field falls back to its default.
         */
        private val settingsJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // First launch: use the country of the device instead of the built-in default.
        scope.launch {
            runCatching {
                database.settingsDataStore.edit { prefs ->
                    if (prefs[COUNTRY_AUTO_SET_KEY] != null) return@edit
                    prefs[COUNTRY_AUTO_SET_KEY] = "1"
                    val deviceCountry = androidx.compose.ui.text.intl.Locale.current.region
                        .let { CountryCode.fromCode(it) } ?: return@edit
                    val json = prefs[SETTINGS_KEY]
                    val current = if (json != null) {
                        settingsJson.decodeFromString<UserSettings>(json as String)
                    } else UserSettings()
                    // Only replace the untouched default.
                    if (current.country == UserSettings().country) {
                        prefs[SETTINGS_KEY] =
                            settingsJson.encodeToString(current.copy(country = deviceCountry))
                    }
                }
            }
        }
    }

    override val userSettings: StateFlow<UserSettings> = database.settingsDataStore.data.map { prefs ->
        val json = prefs[SETTINGS_KEY]
        if (json != null) {
            settingsJson.decodeFromString<UserSettings>(json as String).withPlayableStreamingFormat()
        } else {
            UserSettings() // Default value
        }
    }.stateIn(
        scope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings() // Default value
    )

    suspend fun updateSettings(newSettings: UserSettings) {
        database.settingsDataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = settingsJson.encodeToString(newSettings)
        }
    }

    suspend fun getCurrentSettings(): UserSettings {
        val preferences = database.settingsDataStore.data.first()
        val storedJson = preferences[SETTINGS_KEY] ?: return UserSettings()
        return settingsJson.decodeFromString<UserSettings>(storedJson).withPlayableStreamingFormat()
    }
}
