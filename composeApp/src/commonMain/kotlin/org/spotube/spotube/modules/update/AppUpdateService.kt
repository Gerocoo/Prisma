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

import org.spotube.spotube.core.network.HttpClientFactory
import co.touchlab.kermit.Logger
import org.spotube.spotube.AppBuildInfo
import org.spotube.spotube.modules.settings.SettingsRepository
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.swiftzer.semver.SemVer
import org.koin.core.component.KoinComponent

data class AvailableAppUpdate(
    val tag: String,
    val releaseNotesMarkdown: String,
    /** APK for this device (Android), null when the update must be fetched from [pageUrl]. */
    val downloadUrl: String? = null,
    val pageUrl: String = AppBuildInfo.REPOSITORY_URL + "/releases/latest")

/** Progress of an in-app update: null when idle. */
sealed interface AppUpdateProgress {
    data class Downloading(val fraction: Float) : AppUpdateProgress
    data object Installing : AppUpdateProgress
    data class Failed(val message: String) : AppUpdateProgress
}

/**
 * Checks GitHub for a newer app release once on startup and exposes it for the UI to
 * surface via [org.spotube.spotube.modules.update.AppUpdateDialog]. This is an
 * app-scoped singleton (not a ViewModel) since the check runs once for the whole
 * process, independent of any screen's lifecycle.
 */
class AppUpdateService(
    private val settingsRepository: SettingsRepository,
    httpClientFactory: HttpClientFactory) : KoinComponent {
    private val logger = Logger.withTag("AppUpdateService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient = httpClientFactory.json

    private val _availableUpdate = MutableStateFlow<AvailableAppUpdate?>(null)
    val availableUpdate: StateFlow<AvailableAppUpdate?> = _availableUpdate.asStateFlow()

    private val _progress = MutableStateFlow<AppUpdateProgress?>(null)
    val progress: StateFlow<AppUpdateProgress?> = _progress.asStateFlow()

    /** Result of the last manual check, for the settings button: true = up to date. */
    private val _upToDate = MutableStateFlow<Boolean?>(null)
    val upToDate: StateFlow<Boolean?> = _upToDate.asStateFlow()

    init {
        scope.launch {
            checkForUpdate()
        }
    }

    /** Manual check from Settings: ignores the auto-check switch and the "ignored" version. */
    fun checkNow() {
        _upToDate.value = null
        scope.launch { checkForUpdate(manual = true) }
    }

    private suspend fun checkForUpdate(manual: Boolean = false) {
        try {
            // Read directly from DataStore so the check honors persisted preferences,
            // rather than the repository StateFlow's default value during startup.
            val settings = settingsRepository.getCurrentSettings()
            if (!manual && !settings.autoCheckForUpdates) return

            val release = httpClient.get(AppBuildInfo.LATEST_RELEASE_API_URL) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, "Tubify/${AppBuildInfo.VERSION}")
            }.body<GitHubRelease>()

            // Tubify releases are tagged build-<CI run>, the same number as this app's build.
            val latestBuild = release.tagName.removePrefix("build-").toIntOrNull()
                ?: return logger.w { "Ignoring release with unexpected tag: ${release.tagName}" }
            val newer = latestBuild > AppBuildInfo.BUILD_NUMBER
            val ignored = !manual && release.tagName == settings.ignoredAppUpdateVersion
            if (newer && !ignored) {
                _availableUpdate.value = AvailableAppUpdate(
                    tag = release.tagName,
                    releaseNotesMarkdown = release.body.orEmpty(),
                    downloadUrl = pickApk(release.assets),
                    pageUrl = release.htmlUrl ?: (AppBuildInfo.REPOSITORY_URL + "/releases/latest"))
            }
            if (manual) _upToDate.value = !newer
        } catch (e: Exception) {
            logger.w(e) { "Failed to check for app updates" }
            if (manual) _upToDate.value = null
        }
    }

    /** The APK built for this device's ABI, else the universal one. */
    private fun pickApk(assets: List<GitHubAsset>): String? {
        if (!AppUpdateInstaller.canInstallInApp) return null
        val apks = assets.filter { it.name.endsWith(".apk") }
        AppUpdateInstaller.preferredAbis().forEach { abi ->
            apks.firstOrNull { it.name.contains(abi) }?.let { return it.downloadUrl }
        }
        return apks.firstOrNull { it.name.contains("universal") }?.downloadUrl ?: apks.firstOrNull()?.downloadUrl
    }

    /** Downloads and installs the update in the app, or opens the release page. */
    fun startUpdate(openPage: (String) -> Unit) {
        val update = _availableUpdate.value ?: return
        val url = update.downloadUrl
        if (url == null) {
            openPage(update.pageUrl)
            dismissUpdate()
            return
        }
        if (_progress.value is AppUpdateProgress.Downloading) return
        _progress.value = AppUpdateProgress.Downloading(0f)
        downloadJob = scope.launch {
            try {
                AppUpdateInstaller.downloadAndInstall(url) { f ->
                    _progress.value = AppUpdateProgress.Downloading(f)
                }
                _progress.value = AppUpdateProgress.Installing
            } catch (e: kotlinx.coroutines.CancellationException) {
                _progress.value = null
                throw e
            } catch (e: Exception) {
                logger.e(e) { "In-app update failed" }
                _progress.value = AppUpdateProgress.Failed(e.message ?: "")
            }
        }
    }

    fun ignoreUpdate() {
        val update = _availableUpdate.value ?: return
        _availableUpdate.value = null
        scope.launch {
            runCatching {
                val settings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(settings.copy(ignoredAppUpdateVersion = update.tag))
            }.onFailure { e ->
                logger.e(e) { "Failed to persist ignored app version" }
            }
        }
    }

    private var downloadJob: kotlinx.coroutines.Job? = null

    /** Stops a download in progress; the dialog goes back to the release notes. */
    fun cancelUpdate() {
        downloadJob?.cancel()
        downloadJob = null
        _progress.value = null
    }

    fun dismissUpdate() {
        downloadJob?.cancel()
        downloadJob = null
        _availableUpdate.value = null
        _progress.value = null
    }

    private fun parseVersion(version: String): SemVer? = runCatching {
        SemVer.parse(version)
    }.getOrNull()
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val assets: List<GitHubAsset> = emptyList())

@Serializable
private data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String)
