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

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.spotube.core.webview.WebViewController
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.settings.components.SettingCardItem
import com.composeunstyled.Text
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_account_logout
import spotube.composeapp.generated.resources.settings_account_signed_out
import spotube.composeapp.generated.resources.settings_account_title

/** Who the metadata plugin (Spotify) is signed in as, with a button to sign out. */
internal fun LazyListScope.accountSection() {
    settingsSectionHeader(Res.string.settings_account_title)
    settingsSectionCard(
        items = listOf<@Composable () -> Unit>({ AccountCard() })
    )
}

@Composable
private fun AccountCard() {
    val pluginManager = koinInject<PluginManager>()
    val libraryRepository = koinInject<LibraryRepository>()
    val webViewController = koinInject<WebViewController>()
    val scope = rememberCoroutineScope()
    val plugin by pluginManager.selectedMetadataPlugin.collectAsState()
    val loggedIn by (plugin?.loggedInFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) })
        .collectAsState()
    var busy by remember { mutableStateOf(false) }

    val user by produceState<org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser?>(
        initialValue = null, loggedIn, plugin
    ) {
        value = if (loggedIn) runCatching { libraryRepository.currentUser() }.getOrNull() else null
    }

    val name = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.username
    SettingCardItem(
        title = if (loggedIn) name ?: "Spotify" else stringResource(Res.string.settings_account_signed_out),
        subtitle = if (loggedIn) user?.username?.takeIf { it != name } else null,
        icon = {
            val avatar = user?.thumbnails?.firstOrNull()?.url
            if (avatar != null) AsyncImage(
                model = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        },
        trailingContent = if (!loggedIn) null else ({
            MillenniumButton(
                onClick = {
                    val current = plugin ?: return@MillenniumButton
                    busy = true
                    scope.launch {
                        pluginManager.launchTask {
                            runCatching { current.use { coreAPI.logout() } }
                            runCatching { webViewController.clearData(current.pluginId) }
                        }.join()
                        busy = false
                    }
                },
                loading = busy,
                style = MillenniumButtonStyle.Secondary
            ) {
                Text(stringResource(Res.string.settings_account_logout))
            }
        })
    )
}
