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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class XmlUtilsTest {

    @Test
    fun newPullParser() {
        assertNotNull(XmlUtils.newPullParser())
    }

    @Test
    fun newSerializer() {
        assertNotNull(XmlUtils.newSerializer())
    }


    @Test
    fun `getAttributeValue finds an unqualified attribute`() {
        val parser = parserAt("<C:calendar-data xmlns:C='urn:ietf:params:xml:ns:caldav' " +
                "content-type='text/calendar' version='2.0'/>")

        assertEquals("text/calendar", parser.getAttributeValue(null, "content-type"))
        assertEquals("2.0", parser.getAttributeValue("", "version"))
    }

    @Test
    fun `a document type declaration is skipped`() {
        val parser = parserAt("<!DOCTYPE multistatus><multistatus xmlns='DAV:'/>")

        assertEquals("multistatus", parser.name)
        assertEquals("DAV:", parser.namespace)
    }


    private fun parserAt(xml: String): XmlPullParser {
        val parser = XmlUtils.newPullParser()
        parser.setInput(StringReader(xml))
        while (parser.eventType != XmlPullParser.START_TAG)
            parser.next()
        return parser
    }

}