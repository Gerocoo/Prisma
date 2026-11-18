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

import app.cash.zipline.Zipline
import org.spotube.plugin_interfaces.core.runPluginInitialized
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.SpotifyAuthAPI
import org.spotube.plugin_interfaces.host_apis.SpotifyAuthAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.WebSocketAPI
import org.spotube.plugin_interfaces.host_apis.WebSocketAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.device_control.DeviceControlAPI
import org.spotube.plugin_interfaces.plugin_apis.device_control.DeviceControlAPI_SERVICE_NAME

private val zipline by lazy { Zipline.get() }

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    // Binds the Initializer service the host waits for before consuming any plugin API; the
    // host already bound HttpClientAPI/WebSocketAPI/SpotifyAuthAPI by the time this block runs,
    // since it calls bindHostServices() before initializer.initialize().
    runPluginInitialized {
        val httpClientAPI = zipline.take<HttpClientAPI>(HttpClientAPI_SERVICE_NAME)
        val webSocketAPI = zipline.take<WebSocketAPI>(WebSocketAPI_SERVICE_NAME)
        val spotifyAuthAPI = zipline.take<SpotifyAuthAPI>(SpotifyAuthAPI_SERVICE_NAME)

        zipline.bind<CoreAPI>(CoreAPI_SERVICE_NAME, RealCoreAPI())
        zipline.bind<DeviceControlAPI>(
            DeviceControlAPI_SERVICE_NAME,
            RealDeviceControlAPI(httpClientAPI, webSocketAPI, spotifyAuthAPI),
        )
    }
}
