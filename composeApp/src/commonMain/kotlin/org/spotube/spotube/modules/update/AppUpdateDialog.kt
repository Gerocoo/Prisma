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

import spotube.composeapp.generated.resources.app_update_whats_new
import org.spotube.spotube.resources.iconsax.IconsaxDocumentDownload
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumDialog
import com.mikepenz.markdown.m3.markdownColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.app_update_failed
import spotube.composeapp.generated.resources.app_update_installing
import spotube.composeapp.generated.resources.app_update_downloading
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import org.spotube.spotube.core.ui.component.WavyLinearProgress
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import com.composeunstyled.Text
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.app_update_dialog_no_notes
import spotube.composeapp.generated.resources.app_update_dialog_title
import spotube.composeapp.generated.resources.update_action_ignore
import spotube.composeapp.generated.resources.update_action_update

/**
 * Update dialog: what is new in the release, then download and install inside the app.
 * Flat Material 3 layout with readable colors in both themes and no close cap: [onIgnore]
 * silences this release, [onCancel] stops a download in progress, back or a tap outside closes.
 */
@Composable
fun AppUpdateDialog(
    update: AvailableAppUpdate,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit,
    onUpdate: () -> Unit,
    progress: AppUpdateProgress? = null,
    onCancel: () -> Unit = {}) {
    val title = stringResource(Res.string.app_update_dialog_title, update.tag)
    val colors = AppTheme.colors
    MillenniumDialog(
        visible = true,
        onDismissRequest = onDismiss,
        paneTitle = title
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(Iconsax.IconsaxDocumentDownload, null, tint = colors.primary, modifier = Modifier.size(26.dp))
            }
            Text(title, style = AppTheme.typography.headlineSmall, color = colors.onSurface)

            when (progress) {
                is AppUpdateProgress.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(Res.string.app_update_downloading, (progress.fraction * 100).toInt()),
                        style = AppTheme.typography.bodyLarge,
                        color = colors.onSurface
                    )
                    WavyLinearProgress(modifier = Modifier.fillMaxWidth())
                }
                AppUpdateProgress.Installing -> Text(
                    stringResource(Res.string.app_update_installing),
                    style = AppTheme.typography.bodyLarge, color = colors.onSurface
                )
                else -> {
                    if (progress is AppUpdateProgress.Failed) Text(
                        stringResource(Res.string.app_update_failed),
                        style = AppTheme.typography.bodyMedium, color = colors.error
                    )
                    Text(
                        stringResource(Res.string.app_update_whats_new),
                        style = AppTheme.typography.titleSmall, color = colors.primary
                    )
                    // Release notes in a scrolling tonal card with the app's own text colors.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceContainerHigh)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (update.releaseNotesMarkdown.isNotBlank()) {
                            SelectionContainer {
                                Markdown(
                                    content = update.releaseNotesMarkdown,
                                    colors = markdownColor(text = colors.onSurface)
                                )
                            }
                        } else {
                            Text(
                                stringResource(Res.string.app_update_dialog_no_notes),
                                style = AppTheme.typography.bodyMedium, color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (progress is AppUpdateProgress.Downloading) {
                    MillenniumButton(onClick = onCancel, style = MillenniumButtonStyle.Secondary) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                } else {
                    MillenniumButton(onClick = onIgnore, style = MillenniumButtonStyle.Secondary) {
                        Text(stringResource(Res.string.update_action_ignore))
                    }
                    if (progress != AppUpdateProgress.Installing) MillenniumButton(onClick = onUpdate) {
                        Text(stringResource(Res.string.update_action_update))
                    }
                }
            }
        }
    }
}
