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

package org.spotube.spotube

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import org.koin.core.context.GlobalContext
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.plugin.PluginManagerStates
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.spotube.spotube.core.deeplink.ExternalUriHandler
import org.spotube.spotube.core.newpipe.NewPipeDownloader
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.media.PlaybackService
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        NewPipeDownloader.init(Paths())
        
        // Plain start from the foreground; Media3 promotes PlaybackService itself once playback starts
        startService(Intent(this, PlaybackService::class.java))
        
        intent?.dataString?.let(ExternalUriHandler::onNewUri)
        setContent {
            App()
        }
        holdSplashUntilReady()
    }

    /**
     * Keeps the launch screen up until the plugins are loaded (2.5 s at most), so the app opens
     * on its real interface instead of a half-loaded one.
     */
    private fun holdSplashUntilReady() {
        val pluginManager = runCatching { GlobalContext.get().get<PluginManager>() }.getOrNull() ?: return
        val content = findViewById<View>(android.R.id.content)
        val deadline = SystemClock.uptimeMillis() + 2_500
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = pluginManager.state.value !is PluginManagerStates.Loading ||
                    SystemClock.uptimeMillis() >= deadline
                if (ready) {
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                } else {
                    // Ask for another frame: nothing else would redraw while we hold the first one.
                    content.postDelayed({ content.invalidate() }, 50)
                }
                return ready
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let(ExternalUriHandler::onNewUri)
    }
}