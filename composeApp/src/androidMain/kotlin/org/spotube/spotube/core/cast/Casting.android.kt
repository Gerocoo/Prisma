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
package org.spotube.spotube.core.cast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.media3.cast.Cast
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.util.UnstableApi
import org.spotube.spotube.core.ui.theming.AppTheme

actual fun supportsCasting(): Boolean = true

/**
 * Local network protection: apps targeting Android 17 need ACCESS_LOCAL_NETWORK to discover
 * Cast devices (Android 16 used NEARBY_WIFI_DEVICES for this); older versions need nothing.
 */
private fun localNetworkPermission(): String? = when {
    Build.VERSION.SDK_INT >= 37 -> "android.permission.ACCESS_LOCAL_NETWORK"
    Build.VERSION.SDK_INT >= 36 -> Manifest.permission.NEARBY_WIFI_DEVICES
    else -> null
}

@OptIn(UnstableApi::class)
@Composable
actual fun CastButton(modifier: Modifier) {
    // Cast is initialized in MyApplication; without Google Play services it never becomes
    // ready, so the button stays hidden instead of crashing.
    val context = androidx.compose.ui.platform.LocalContext.current
    // Ask once, the first time the player shows the Cast button, so device search can work.
    val permission = localNetworkPermission()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(permission) {
        if (permission != null &&
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(permission)
        }
    }
    val cast = runCatching { Cast.getSingletonInstance(context) }.getOrNull() ?: return
    if (cast.needsInitialization() || cast.castContextLoadFailure != null) return
    CompositionLocalProvider(LocalContentColor provides AppTheme.colors.onSurfaceVariant) {
        MediaRouteButton(modifier = modifier)
    }
}
