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

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

private val timeFormatUTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssVV", Locale.US)

internal actual fun formatUtc(instant: Instant): String =
    timeFormatUTC.format(
        ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(instant.epochSeconds, instant.nanosecondsOfSecond.toLong()),
            ZoneOffset.UTC
        )
    )
