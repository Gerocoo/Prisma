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
 * No-op: ML Kit is Android-only and the project currently ships Android builds only.
 * Kept so the commonMain "Traduci" pill code compiles against every platform's DI graph;
 * the pill simply never shows up here since [translate] always returns `null`.
 */
class IosTranslationService : TranslationService {
    override suspend fun detectLanguage(text: String): String? = null
    override suspend fun translate(text: String, targetLanguageTag: String): String? = null
}
