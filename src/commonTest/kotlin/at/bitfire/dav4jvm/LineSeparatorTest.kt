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

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.responses
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.dav4jvm.property.webdav.GetETag
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.withCharset
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * U+0085 (NEL) and U+2028 (LINE SEPARATOR) are ordinary characters in XML 1.0 and must be passed through
 * unchanged (XML 1.1 would normalize them to line feeds).
 */
class LineSeparatorTest {

    @Test
    fun `text keeps NEL and LINE SEPARATOR`() {
        val text = "a\u2028b\u0085c\u2029d\r\u0085e\r\u2028f"
        assertEquals("a\u2028b\u0085c\u2029d\n\u0085e\n\u2028f", rootText(parser("<test>$text</test>")))
    }

    @Test
    fun `CDATA keeps NEL and LINE SEPARATOR`() {
        val text = "a\u2028b]\u2028c]]\u0085d"
        assertEquals(text, rootText(parser("<test><![CDATA[$text]]></test>")))
    }

    @Test
    fun `text and CDATA are reported as one text event`() {
        assertEquals("a\u2028bc\u2028d", rootText(parser("<test>a\u2028b<![CDATA[c\u2028d]]></test>")))
    }

    @Test
    fun `attribute value keeps LINE SEPARATOR`() {
        val parser = parser("<test a='1\u20282'/>")
        assertEquals(XmlPullParser.START_TAG, parser.next())
        assertEquals("1\u20282", parser.getAttributeValue(null, "a"))
    }

    @Test
    fun `stream keeps NEL and LINE SEPARATOR across buffer boundaries`() {
        for (padding in 4080..4110) {
            val text = "x".repeat(padding) + "a\u2028b\u0085c"
            val cdata = "y".repeat(padding) + "d\u2028e"
            val parser = XmlUtils.newPullParser()
            parser.setInput(ByteReadChannel("<root><t>$text</t><c><![CDATA[$cdata]]></c></root>".encodeToByteArray()).toInputStream(), null)
            val texts = mutableListOf<String>()
            while (parser.next() != XmlPullParser.END_DOCUMENT)
                if (parser.eventType == XmlPullParser.START_TAG && parser.depth == 2)
                    texts += parser.nextText()
            assertEquals(listOf(text, cdata), texts, "padding $padding")
        }
    }

    @Test
    fun `multiget keeps line separators in calendar-data`() = runTest {
        val escaped = vtodoWithLineSeparators.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        assertEquals(vtodoWithLineSeparators, multigetCalendarData(escaped))
    }

    @Test
    fun `multiget keeps line separators in CDATA calendar-data`() = runTest {
        assertEquals(vtodoWithLineSeparators, multigetCalendarData("<![CDATA[$vtodoWithLineSeparators]]>"))
    }


    /**
     * Synthetic VTODO shaped like a real-world report: notes pasted with U+2028 (some next to
     * fold points, one with a colon after it) and U+00A0, followed by another property.
     */
    private val vtodoWithLineSeparators =
        "BEGIN:VCALENDAR\n" +
        "VERSION:2.0\n" +
        "PRODID:-//Example//Test//EN\n" +
        "BEGIN:VTODO\n" +
        "UID:line-separator-test\n" +
        "SUMMARY:Garden supplies\n" +
        "DESCRIPTION:Hardware store \u2028Seeds: 12/40 tomato\\, 8/40 basil\u2028Soil\u00a0bag\n" +
        " s\u2028Watering can \u2028Gloves\u2028\n" +
        " Location: shed\u2028Fertilizer\n" +
        "PRIORITY:9\n" +
        "END:VTODO\n" +
        "END:VCALENDAR\n"

    private suspend fun multigetCalendarData(calendarData: String): String? {
        val engine = MockEngine {
            respond(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<d:multistatus xmlns:d=\"DAV:\" xmlns:cal=\"urn:ietf:params:xml:ns:caldav\">" +
                "<d:response><d:href>/dav/task.ics</d:href><d:propstat><d:prop>" +
                "<d:getetag>&quot;abc&quot;</d:getetag>" +
                "<cal:calendar-data>$calendarData</cal:calendar-data>" +
                "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>" +
                "</d:multistatus>",
                HttpStatusCode.MultiStatus,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Xml.withCharset(Charsets.UTF_8).toString())
            )
        }
        val response = DavCalendar(HttpClient(engine), Url("http://127.0.0.1/dav/"))
            .multiget(listOf(Url("http://127.0.0.1/dav/task.ics")))
            .responses().toList().single()
        assertEquals("abc", response[GetETag::class]?.eTag)
        return response[CalendarData::class]?.iCalendar
    }

    private fun parser(xml: String) = XmlUtils.newPullParser().apply {
        setInput(StringReader(xml))
    }

    private fun rootText(parser: XmlPullParser): String {
        assertEquals(XmlPullParser.START_TAG, parser.next())
        return parser.nextText()
    }

}
