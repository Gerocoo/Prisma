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

package org.spotube.spotube.core.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ML Kit-backed [TranslationService]. Runs fully on-device: language identification ships
 * with the app, and the translation model for a given language pair is downloaded once
 * (over Wi-Fi by default) the first time it's needed, then reused offline from then on.
 */
class AndroidTranslationService : TranslationService {

    private val languageIdentifier = LanguageIdentification.getClient()

    override suspend fun detectLanguage(text: String): String? {
        if (text.isBlank()) return null
        return try {
            suspendCancellableCoroutine { cont ->
                languageIdentifier.identifyLanguage(text)
                    .addOnSuccessListener { tag ->
                        cont.resume(if (tag.isBlank() || tag == "und") null else tag)
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun translate(text: String, targetLanguageTag: String): String? {
        if (text.isBlank()) return null
        val sourceTag = detectLanguage(text) ?: return null

        val sourceCode = TranslateLanguage.fromLanguageTag(sourceTag) ?: return null
        val targetCode = TranslateLanguage.fromLanguageTag(targetLanguageTag) ?: return null
        if (sourceCode == targetCode) return null

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
            .setTargetLanguage(targetCode)
            .build()
        val translator = Translation.getClient(options)
        return try {
            val downloaded = suspendCancellableCoroutine { cont ->
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                    .addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener { cont.resume(false) }
            }
            if (!downloaded) return null

            suspendCancellableCoroutine { cont ->
                translator.translate(text)
                    .addOnSuccessListener { translated -> cont.resume(translated) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (e: Exception) {
            null
        } finally {
            translator.close()
        }
    }
}
