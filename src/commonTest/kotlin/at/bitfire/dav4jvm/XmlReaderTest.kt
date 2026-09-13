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

import nl.adaptivity.xmlutil.EventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class XmlReaderTest {

    @Test
    fun testProcessTag_Root() {
        val parser = XmlUtils.newReader("<test></test>")
        // now on START_DOCUMENT [0]

        var processed = false
        parser.processTag(Property.Name("", "test")) {
            processed = true
        }
        assertTrue(processed)
    }

    @Test
    fun testProcessTag_Depth1() {
        val parser = XmlUtils.newReader("<root><test></test></root>")
        parser.next()       // now on START_TAG <root>

        var processed = false
        parser.processTag(Property.Name("", "test")) {
            processed = true
        }
        assertTrue(processed)
    }


    @Test
    fun testReadText() {
        val parser = XmlUtils.newReader("<root><test>Test 1</test><test><garbage/>Test 2</test></root>")
        parser.next()
        parser.next()       // now on START_TAG <test>
        val reader = parser

        assertEquals("Test 1", reader.readText())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        parser.next()

        assertEquals("Test 2", reader.readText())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
    }

    @Test
    fun testReadText_CDATA() {
        val parser = XmlUtils.newReader("<test><![CDATA[Test 1</test><test><garbage/>Test 2]]></test>")
        parser.next()       // now on START_TAG <test>

        assertEquals("Test 1</test><test><garbage/>Test 2", parser.readText())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
    }

    @Test
    fun testReadText_Entities() {
        val parser = XmlUtils.newReader("<test>A&amp;B &lt;C&gt; &#x41;&#65; &quot;q&apos;</test>")
        parser.next()       // now on START_TAG <test>

        assertEquals("A&B <C> AA \"q'", parser.readText())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
    }

    @Test
    fun testReadText_UnknownEntity() {
        val parser = XmlUtils.newReader("<test>Team&nbsp;Calendar &amp; Contacts</test>")
        parser.next()       // now on START_TAG <test>

        assertEquals("TeamCalendar & Contacts", parser.readText())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
    }

    @Test
    fun testReadTextProperty_Entities() {
        val parser = XmlUtils.newReader("<root><entry>A&amp;B</entry><entry>C&nbsp;D</entry></root>")
        parser.next()        // now on START_TAG <root>

        assertEquals("A&B", parser.readTextProperty(Property.Name("", "entry")))
    }

    @Test
    fun testReadTextPropertyList_Entities() {
        val parser = XmlUtils.newReader("<root><entry>A&amp;B</entry><entry>C&nbsp;D</entry></root>")
        parser.next()        // now on START_TAG <root>

        val entries = mutableListOf<String>()
        parser.readTextPropertyList(Property.Name("", "entry"), entries)
        assertEquals(listOf("A&B", "CD"), entries)
    }

    @Test
    fun testReadText_PropertyRoot() {
        val parser = XmlUtils.newReader("<root><entry>Test 1</entry><entry>Test 2</entry></root>")
        parser.next()        // now on START_TAG <root>

        val entries = mutableListOf<String>()
        parser.readTextPropertyList(Property.Name("", "entry"), entries)
        assertEquals("Test 1", entries[0])
        assertEquals("Test 2", entries[1])

        parser.next()       // END_TAG </root>
        assertEquals(EventType.END_DOCUMENT, parser.eventType)
    }


    @Test
    fun testReadTextPropertyList_Depth1() {
        val parser = XmlUtils.newReader("<test><entry>Test 1</entry><entry>Test 2</entry></test>")
        parser.next()       // now on START_TAG <test> [1]

        val entries = mutableListOf<String>()
        parser.readTextPropertyList(Property.Name("", "entry"), entries)
        assertEquals("Test 1", entries[0])
        assertEquals("Test 2", entries[1])
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        assertEquals("test", parser.localName)
    }


    @Test
    fun testReadLong() {
        val parser = XmlUtils.newReader("<root><test>1</test><test><garbage/>2</test><test><garbage/>a</test></root>")
        parser.next()
        parser.next()       // now on START_TAG <test>
        val reader = parser

        assertEquals(1L, reader.readLong())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        parser.next()

        assertEquals(2L, reader.readLong())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        parser.next()

        assertNull(reader.readLong())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
    }


    @Test
    fun testReadHttpDate() {
        val parser = XmlUtils.newReader("<root><test>Sun, 06 Nov 1994 08:49:37 GMT</test><test><garbage/>Sun, 06 Nov 1994 08:49:37 GMT</test><test><garbage/>invalid</test></root>")
        parser.next()
        parser.next()       // now on START_TAG <test>
        val reader = parser

        assertEquals(Instant.fromEpochSeconds(784111777), reader.readHttpDate())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        parser.next()

        assertEquals(Instant.fromEpochSeconds(784111777), reader.readHttpDate())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
        parser.next()

        assertNull(reader.readHttpDate())
        assertEquals(EventType.END_ELEMENT, parser.eventType)
    }


    @Test
    fun testReadContentTypes() {
        val parser = XmlUtils.newReader("<root><test content-type=\"text/plain\">text</test><test content-type=\"application/json\">{}</test></root>")
        parser.next()
        val reader = parser

        val types = mutableListOf<String>()
        reader.readContentTypes(Property.Name("", "test"), types::add)
        assertEquals(2, types.size)
        assertEquals("text/plain", types[0])
        assertEquals("application/json", types[1])
    }

}