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
package org.spotube.spotube.core.ui.theming

import android.graphics.Bitmap
import coil3.Image
import coil3.toBitmap

internal actual fun Image.samplePixels(maxSize: Int): IntArray? = runCatching {
    // Hardware bitmaps can't be read directly; copy to a software one first.
    val source = toBitmap().let { if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it }
    val scaled = Bitmap.createScaledBitmap(source, maxSize, maxSize, true)
    IntArray(maxSize * maxSize).also { scaled.getPixels(it, 0, maxSize, 0, 0, maxSize, maxSize) }
}.getOrNull()
