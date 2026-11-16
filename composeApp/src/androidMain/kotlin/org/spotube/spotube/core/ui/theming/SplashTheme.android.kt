package org.spotube.spotube.core.ui.theming

import androidx.compose.runtime.Composable
import org.spotube.spotube.modules.settings.Theme

/**
 * Disattivato: chiamare setApplicationNightMode() ad ogni avvio forzava Android a ricreare
 * l'Activity (documentato), causando il lampeggio chiaro/scuro riportato dagli utenti. La
 * splash screen torna a seguire semplicemente il tema di sistema, com'era prima e come
 * funzionava correttamente.
 */
@Composable
actual fun SplashThemeSync(theme: Theme) = Unit
