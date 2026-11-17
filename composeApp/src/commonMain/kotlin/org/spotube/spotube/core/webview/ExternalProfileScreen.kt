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

package org.spotube.spotube.core.webview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.composeunstyled.Text
import dev.nucleusframework.webview.web.WebContent
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewState
import dev.nucleusframework.webview.web.rememberWebViewNavigator
import org.spotube.spotube.core.tools.user_agents.UserAgents
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * Read-only in-app page for viewing an external profile (e.g. a playlist creator's Spotify
 * profile) without leaving Prisma for the Spotify app. No JS bridge, no cookie/plugin
 * plumbing: this is a plain page view, not a plugin login/auth flow like [PlatformWebViewScreen].
 */
@Composable
fun ExternalProfileScreen(url: String, title: String) {
    val state = remember(url) {
        WebViewState(
            WebContent.Url(
                url = url,
                additionalHttpHeaders = mapOf("User-Agent" to UserAgents.random())
            )
        )
    }
    val navigator = rememberWebViewNavigator()

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationMainBar(
            title = {
                Text(
                    text = title,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            backButton = true
        )
        WebView(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            navigator = navigator
        )
    }
}
