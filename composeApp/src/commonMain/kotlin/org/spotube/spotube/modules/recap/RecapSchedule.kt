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
package org.spotube.spotube.modules.recap

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * When the yearly recap is available, aligned with the usual season for annual music recaps:
 * from the first Wednesday on or after 29 November (2026: 2 December) until 31 January, when
 * the previous year's recap is still shown. Outside that window there is no recap to open.
 */
object RecapSchedule {
    fun releaseDate(year: Int): LocalDate {
        var date = LocalDate(year, 11, 29)
        while (date.dayOfWeek != DayOfWeek.WEDNESDAY) date = date.plus(1, DateTimeUnit.DAY)
        return date
    }

    /** The year whose recap is available today, or null outside the recap season. */
    fun activeYear(today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date): Int? = when {
        today >= releaseDate(today.year) -> today.year
        today.month.ordinal == 0 -> today.year - 1 // January: last year's recap is still on
        else -> null
    }
}
