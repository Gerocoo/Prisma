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
import org.spotube.millennium.composables.MillenniumAlertDialog
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.app_update_dialog_no_notes
import spotube.composeapp.generated.resources.app_update_dialog_title
import spotube.composeapp.generated.resources.update_action_ignore
import spotube.composeapp.generated.resources.update_action_update

/**
 * Prompts the user that a newer Spotube release is available on GitHub. [onUpdate]
 * should take the user to the downloads page; [onIgnore] persistently silences this
 * specific release tag so the check moves on to the next newer one.
 */
@Composable
fun AppUpdateDialog(
    update: AvailableAppUpdate,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit,
    onUpdate: () -> Unit,
    progress: AppUpdateProgress? = null) {
    MillenniumAlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.app_update_dialog_title, update.tag)) },
        content = {
            when (progress) {
                is AppUpdateProgress.Downloading -> {
                    Text(stringResource(Res.string.app_update_downloading, (progress.fraction * 100).toInt()))
                    WavyLinearProgress(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                }
                AppUpdateProgress.Installing -> Text(stringResource(Res.string.app_update_installing))
                is AppUpdateProgress.Failed -> Text(stringResource(Res.string.app_update_failed))
                null -> {}
            }
            val busy = progress is AppUpdateProgress.Downloading || progress == AppUpdateProgress.Installing
            if (busy) {
                // Progress only while the update is on its way.
            } else if (update.releaseNotesMarkdown.isNotBlank()) {
                SelectionContainer {
                    Markdown(content = update.releaseNotesMarkdown)
                }
            } else {
                Text(stringResource(Res.string.app_update_dialog_no_notes))
            }
        }) {
        MillenniumButton(
            onClick = onIgnore,
            style = MillenniumButtonStyle.Secondary) {
            Text(stringResource(Res.string.update_action_ignore))
        }
        if (progress !is AppUpdateProgress.Downloading) MillenniumButton(onClick = onUpdate) {
            Text(stringResource(Res.string.update_action_update))
        }
    }
}
