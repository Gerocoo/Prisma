package org.spotube.spotube.core.ui.theming

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import org.spotube.spotube.modules.settings.Theme

private const val PREFS = "prisma_splash"
private const val KEY = "theme"
private const val KEY_APPLIED_NIGHT_MODE = "applied_night_mode"

/** Theme saved by the last run: "light", "dark" or "system". */
fun savedSplashTheme(context: Context): String =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "system") ?: "system"

@Composable
actual fun SplashThemeSync(theme: Theme) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(theme) {
        val value = when (theme) {
            Theme.LIGHT -> "light"
            Theme.DARK -> "dark"
            Theme.SYSTEM -> "system"
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, value).apply()
        // Android 12+: the launch screen follows the night mode set for this app.
        //
        // setApplicationNightMode() is a per-app override that the OS applies by recreating
        // the current Activity - that recreate is what showed up as the app flashing between
        // light and dark while just using it. It only needs to run once, when the stored mode
        // actually differs from what was last applied; UiModeManager has no public getter for
        // the currently applied mode on every API level, so the last value this app itself set
        // is cached and compared instead of asking the system again.
        if (Build.VERSION.SDK_INT >= 31) {
            val mode = when (theme) {
                Theme.LIGHT -> UiModeManager.MODE_NIGHT_NO
                Theme.DARK -> UiModeManager.MODE_NIGHT_YES
                Theme.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            }
            val lastApplied = prefs.getInt(KEY_APPLIED_NIGHT_MODE, Int.MIN_VALUE)
            if (lastApplied != mode) {
                runCatching {
                    val manager = context.getSystemService(UiModeManager::class.java)
                    manager.setApplicationNightMode(mode)
                }.onSuccess {
                    prefs.edit().putInt(KEY_APPLIED_NIGHT_MODE, mode).apply()
                }
            }
        }
    }
}
