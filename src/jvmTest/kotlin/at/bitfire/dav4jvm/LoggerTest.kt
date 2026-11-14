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

import java.util.logging.Handler
import java.util.logging.LogRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LoggerTest {

    private class RecordingHandler : Handler() {
        val records = mutableListOf<LogRecord>()
        override fun publish(record: LogRecord) { records += record }
        override fun flush() {}
        override fun close() {}
    }

    private fun recordJul(loggerName: String, block: () -> Unit): List<LogRecord> {
        val julLogger = java.util.logging.Logger.getLogger(loggerName)
        val handler = RecordingHandler()
        julLogger.addHandler(handler)
        try {
            block()
        } finally {
            julLogger.removeHandler(handler)
        }
        return handler.records
    }

    @Test
    fun `logs to java util logging`() {
        val records = recordJul("at.bitfire.dav4jvm.HttpUtils") {
            HttpUtils.parseDate("not a date")
        }
        val record = records.single()
        assertEquals(java.util.logging.Level.WARNING, record.level)
        assertEquals("Couldn't parse HTTP date: not a date, ignoring", record.message)
        assertEquals("at.bitfire.dav4jvm.HttpUtils", record.loggerName)
    }

    @Test
    fun `passes level and throwable`() {
        val e = IllegalStateException("test")
        val records = recordJul("at.bitfire.dav4jvm.LoggerTest") {
            Logger.getLogger("at.bitfire.dav4jvm.LoggerTest").log(Level.SEVERE, "message", e)
        }
        val record = records.single()
        assertEquals(java.util.logging.Level.SEVERE, record.level)
        assertSame(e, record.thrown)
    }

}
