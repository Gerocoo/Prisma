package org.spotube.spotube.core.ui.theming

import androidx.compose.runtime.Composable
import org.spotube.spotube.modules.settings.Theme

/**
 * Remembers the app's own theme choice so the launch screen (drawn before any code runs) can
 * follow it instead of the system's dark mode. [Theme.SYSTEM] keeps following the system.
 */
@Composable
expect fun SplashThemeSync(theme: Theme)
