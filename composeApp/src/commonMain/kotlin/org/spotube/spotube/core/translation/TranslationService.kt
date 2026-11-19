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

/**
 * On-device text translation for the lyrics/artist-bio "Traduci" pill.
 *
 * Implementations are expected to work fully offline once any needed language model has
 * been downloaded, and to return `null` (never throw) when detection/translation isn't
 * possible, so callers can simply hide the "Traduci" pill in that case.
 */
interface TranslationService {

    /**
     * Best-effort BCP-47 language tag for [text] (e.g. "en", "it"), or `null` if it could not
     * be confidently determined.
     */
    suspend fun detectLanguage(text: String): String?

    /**
     * Translates [text] into [targetLanguageTag] (BCP-47, e.g. "it").
     *
     * Returns `null` if the source language couldn't be detected, if it already matches
     * [targetLanguageTag], if the language pair isn't supported, or if translation otherwise
     * fails - never throws.
     */
    suspend fun translate(text: String, targetLanguageTag: String): String?
}
