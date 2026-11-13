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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
        // Android 12+: the launch screen follows the night mode set for this app.
        if (Build.VERSION.SDK_INT >= 31) {
            val mode = when (theme) {
                Theme.LIGHT -> UiModeManager.MODE_NIGHT_NO
                Theme.DARK -> UiModeManager.MODE_NIGHT_YES
                Theme.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            }
            runCatching {
                val manager = context.getSystemService(UiModeManager::class.java)
                manager.setApplicationNightMode(mode)
            }
        }
    }
}
