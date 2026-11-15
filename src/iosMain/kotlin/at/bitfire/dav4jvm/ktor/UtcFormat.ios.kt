/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.ktor

import io.ktor.util.date.GMTDate
import kotlin.time.Instant

internal actual fun formatUtc(instant: Instant): String {
    val date = GMTDate(instant.toEpochMilliseconds())
    fun Int.pad(length: Int) = toString().padStart(length, '0')
    return date.year.pad(4) + (date.month.ordinal + 1).pad(2) + date.dayOfMonth.pad(2) +
            "T" + date.hours.pad(2) + date.minutes.pad(2) + date.seconds.pad(2) + "Z"
}
