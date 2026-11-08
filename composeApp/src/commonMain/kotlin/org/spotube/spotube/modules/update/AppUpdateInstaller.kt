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
package org.spotube.spotube.modules.update

/**
 * Platform side of the in-app updater. On Android the APK for this device is downloaded into the
 * app cache and handed to the system installer; elsewhere [canInstallInApp] is false and the
 * release page is opened instead.
 */
expect object AppUpdateInstaller {
    val canInstallInApp: Boolean

    /** ABIs of this device, best first (e.g. arm64-v8a, armeabi-v7a). */
    fun preferredAbis(): List<String>

    /** Downloads [url] reporting progress 0..1, then opens the system installer. */
    suspend fun downloadAndInstall(url: String, onProgress: (Float) -> Unit)
}
