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

import at.bitfire.dav4jvm.property.webdav.GetETag
import nl.adaptivity.xmlutil.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyTest {

    @Test
    fun testParse_InvalidProperty() {
        val parser = XmlUtils.newReader("<multistatus xmlns='DAV:'><getetag/></multistatus>")
        do {
            parser.next()
        } while (parser.eventType != EventType.START_ELEMENT && parser.localName != "multistatus")

        // we're now at the start of <multistatus>
        assertEquals(EventType.START_ELEMENT, parser.eventType)
        assertEquals("multistatus", parser.localName)

        // parse invalid DAV:getetag
        Property.Companion.parse(parser).let {
            assertEquals(1, it.size)
            assertEquals(GetETag(null), it[0])
        }

        // we're now at the end of <multistatus>
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        assertEquals("multistatus", parser.localName)
    }

    @Test
    fun testParse_ValidProperty() {
        val parser = XmlUtils.newReader("<multistatus xmlns='DAV:'><getetag>12345</getetag></multistatus>")
        do {
            parser.next()
        } while (parser.eventType != EventType.START_ELEMENT && parser.localName != "multistatus")

        // we're now at the start of <multistatus>
        assertEquals(EventType.START_ELEMENT, parser.eventType)
        assertEquals("multistatus", parser.localName)

        val etag = Property.Companion.parse(parser).first()
        assertEquals(GetETag("12345"), etag)

        // we're now at the end of <multistatus>
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        assertEquals("multistatus", parser.localName)
    }

}