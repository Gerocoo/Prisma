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

package org.spotube.spotube.core.zipline

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioAPI
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import org.spotube.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

class PluginServiceScope(private val registry: Map<KClass<*>, ZiplineService>) {
    val coreAPI: CoreAPI get() = getService()
    val metadataUserAPI: MetadataUserAPI get() = getService()
    val metadataTrackAPI: MetadataTrackAPI get() = getService()
    val metadataAlbumAPI: MetadataAlbumAPI get() = getService()
    val metadataArtistAPI: MetadataArtistAPI get() = getService()
    val metadataPlaylistAPI: MetadataPlaylistAPI get() = getService()
    val metadataBrowseAPI: MetadataBrowseAPI get() = getService()
    val metadataSearchAPI: MetadataSearchAPI get() = getService()
    val audioAPI: AudioAPI get() = getService()
    val lyricsAPI: LyricsAPI get() = getService()
    val scrobbleAPI: ScrobbleAPI get() = getService()

    private inline fun <reified T : ZiplineService> getService(): T {
        return registry[T::class] as? T
            ?: error("${T::class.simpleName} is not initialized or loaded.")
    }
}

interface PluginService {
    val pluginId: String
    val loggedInFlow: StateFlow<Boolean>

    suspend fun start()
    suspend fun stop()
    suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T
}

/** How long a plugin gets, after it starts, to restore its saved session before "signed out" counts. */
private const val SessionRestoreGraceMillis = 4_000L

/**
 * [PluginService.loggedInFlow] without the false "signed out" every start begins with: a plugin
 * that needs an account reports signed out until it has restored its session, a moment later.
 * Here "signed out" is only reported once it has lasted [SessionRestoreGraceMillis]; "signed in"
 * is reported at once. Plugins that need no account are passed through unchanged.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun PluginService.settledLoggedInFlow(alreadyKnownToRequireAuth: Boolean = false): kotlinx.coroutines.flow.Flow<Boolean> =
    kotlinx.coroutines.flow.flow {
        val requiresAuth = alreadyKnownToRequireAuth || try {
            use { coreAPI.requiresAuthentication }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
        if (!requiresAuth) {
            emitAll(loggedInFlow)
        } else {
            emitAll(loggedInFlow.transformLatest { loggedIn ->
                if (!loggedIn) kotlinx.coroutines.delay(SessionRestoreGraceMillis)
                emit(loggedIn)
            })
        }
    }
