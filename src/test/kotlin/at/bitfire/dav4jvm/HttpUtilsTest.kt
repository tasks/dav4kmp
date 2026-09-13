/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class HttpUtilsTest {

    @Test
    fun formatDate() {
        assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", HttpUtils.formatDate(Instant.parse("1994-11-06T08:49:37Z")))
    }


    @Test
    fun formatDate_timezone_is_GMT() {
        // See https://github.com/bitfireAT/dav4jvm/issues/22
        assertTrue(HttpUtils.formatDate(Instant.fromEpochSeconds(0)).endsWith(" GMT"))
    }

    @Test
    fun parseDate_IMF_FixDate() {
        // RFC 7231 IMF-fixdate (preferred format)
        assertEquals(Instant.fromEpochSeconds(784111777), HttpUtils.parseDate("Sun, 06 Nov 1994 08:49:37 GMT"))
    }

    @Test
    fun parseDate_IMF_FixDate_GMT() {
        // See https://github.com/bitfireAT/dav4jvm/issues/22
        assertEquals(Instant.parse("2026-05-04T22:51:02Z"), HttpUtils.parseDate("Mon, 04 May 2026 22:51:02 GMT"))
    }

    @Test
    fun parseDate_RFC850_1994() {
        // obsolete RFC 850 format – 2-digit year cannot be parsed, returns null
        assertNull(HttpUtils.parseDate("Sun, 06-Nov-94 08:49:37 GMT"))
    }

}