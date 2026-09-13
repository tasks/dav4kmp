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

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.readSimpleElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlUtilsTest {

    @Test
    fun newReader() {
        assertNotNull(XmlUtils.newReader("<test/>"))
    }

    @Test
    fun newWriter() {
        assertNotNull(XmlUtils.newWriter(StringBuilder()))
    }


    @Test
    fun `newReader - truncated between elements throws XmlException`() {
        val reader = XmlUtils.newReader("<multistatus xmlns='DAV:'><response><href>/x</href>")
        assertThrows(XmlException::class.java) {
            reader.drain()
        }
    }

    @Test
    fun `newReader - truncated inside tag throws XmlException`() {
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader("<").drain()
        }
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader("<multistatus xmlns='DAV:'><resp").drain()
        }
    }

    @Test
    fun `newReader - keeps failing after truncation`() {
        val reader = XmlUtils.newReader("<multistatus xmlns='DAV:'><response><href>/x</href><propstat><prop><displayname>abc")
        assertThrows(XmlException::class.java) {
            reader.drain()
        }
        assertThrows(XmlException::class.java) {
            reader.next()
        }
        assertThrows(XmlException::class.java) {
            reader.nextTag()
        }
    }

    @Test
    fun `newReader - not XML throws XmlException`() {
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader("Some error occurred").drain()
        }
    }

    @Test
    fun `newReader - malformed character reference throws XmlException`() {
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader("<test>&#xZZ;</test>").rootText()
        }
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader(ByteReadChannel("<test>&#;</test>")).rootText()
        }
    }

    @Test
    fun `newReader - document type declaration throws XmlException`() {
        val xml = "<!DOCTYPE test [<!ENTITY lol \"lol\"><!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">]>" +
                "<test>&lol1;</test>"
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader(xml).rootText()
        }
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader(ByteReadChannel(xml)).rootText()
        }
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader("<!DOCTYPE test><test/>").drain()
        }
    }

    @Test
    fun `newReader - empty document ends without elements`() {
        val reader = XmlUtils.newReader("")
        assertEquals(EventType.START_DOCUMENT, reader.eventType)
        assertEquals(EventType.END_DOCUMENT, reader.next())
    }

    @Test
    fun `newReader - relaxed mode tolerates unquoted attribute values`() {
        val reader = XmlUtils.newReader("<test attr=value>text</test>")
        while (reader.eventType != EventType.START_ELEMENT)
            reader.next()
        assertEquals("value", reader.getAttributeValue(null, "attr"))
        assertEquals("text", reader.readSimpleElement())
    }


    @Test
    fun `newReader channel - truncated throws XmlException`() {
        val reader = XmlUtils.newReader(ByteReadChannel("<multistatus xmlns='DAV:'><response><href>/x</href>"))
        assertThrows(XmlException::class.java) {
            reader.drain()
        }
    }

    @Test
    fun `newReader channel - empty throws XmlException`() {
        assertThrows(XmlException::class.java) {
            XmlUtils.newReader(ByteReadChannel.Empty)
        }
    }

    @Test
    fun `newReader channel - UTF-8 without declaration`() {
        val reader = XmlUtils.newReader(ByteReadChannel("<test>café ☕</test>".encodeToByteArray()))
        assertEquals("café ☕", reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-8 with BOM`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "<test>café</test>".encodeToByteArray()
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - encoding from XML declaration`() {
        val bytes = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><test>café</test>".toLatin1()
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - encoding from XML declaration with single quotes and whitespace`() {
        val bytes = "<?xml version='1.0'   encoding = 'iso-8859-1' ?><test>café</test>".toLatin1()
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-8 is used when document doesn't indicate encoding`() {
        val bytes = "<?xml version=\"1.0\"?><test>café</test>".encodeToByteArray()
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-16BE with BOM`() {
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + "<test>café 😀</test>".toUtf16(bigEndian = true)
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café 😀", reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-16LE with BOM`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "<test>café</test>".toUtf16(bigEndian = false)
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-16BE without BOM`() {
        val bytes = "<?xml version=\"1.0\"?><test>café</test>".toUtf16(bigEndian = true)
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-16LE without BOM`() {
        val bytes = "<?xml version=\"1.0\"?><test>café</test>".toUtf16(bigEndian = false)
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - multi-byte UTF-8 characters across chunk boundaries`() {
        val text = buildString {
            while (length < 40*1024)
                append("aé☕😀")
        }
        val reader = XmlUtils.newReader(ByteReadChannel("<test>$text</test>".encodeToByteArray()))
        assertEquals(text, reader.rootText())
    }

    @Test
    fun `newReader channel - UTF-16 surrogate pairs across chunk boundaries`() {
        val text = buildString {
            while (length < 40*1024)
                append("a😀")
        }
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + "<test>$text</test>".toUtf16(bigEndian = true)
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals(text, reader.rootText())
    }

    @Test
    fun `newReader channel - large single-byte document`() {
        val text = "café ".repeat(20*1024)
        val reader = XmlUtils.newReader(ByteReadChannel("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><test>$text</test>".toLatin1()))
        assertEquals(text, reader.rootText())
    }

    @Test
    fun `newReader channel - encoding aliases`() {
        for (alias in listOf("ISO8859-1", "ISO_8859-1", "latin1", "US-ASCII", "ascii")) {
            val bytes = "<?xml version=\"1.0\" encoding=\"$alias\"?><test>café</test>".toLatin1()
            val reader = XmlUtils.newReader(ByteReadChannel(bytes))
            assertEquals(alias, "café", reader.rootText())
        }
        for (alias in listOf("utf8", "UTF-8")) {
            val bytes = "<?xml version=\"1.0\" encoding=\"$alias\"?><test>café ☕</test>".encodeToByteArray()
            val reader = XmlUtils.newReader(ByteReadChannel(bytes))
            assertEquals(alias, "café ☕", reader.rootText())
        }
    }

    @Test
    fun `newReader channel - unsupported encoding falls back to UTF-8`() {
        val bytes = "<?xml version=\"1.0\" encoding=\"x-no-such-charset\"?><test>café</test>".encodeToByteArray()
        val reader = XmlUtils.newReader(ByteReadChannel(bytes))
        assertEquals("café", reader.rootText())
    }

    @Test
    fun `newReader channel - parses while receiving`() = runTest {
        val channel = ByteChannel()
        val firstElementSeen = CompletableDeferred<Unit>()

        launch(Dispatchers.Default) {
            try {
                channel.writeFully("<test><first/>$parserBlocks".encodeToByteArray())
                channel.flush()

                withTimeout(10_000) { firstElementSeen.await() }

                channel.writeFully("<second/></test>".encodeToByteArray())
                channel.flushAndClose()
            } catch (e: Exception) {
                channel.close(e)
            }
        }

        val reader = XmlUtils.newReader(channel)
        val elements = mutableListOf<String>()
        while (reader.next() != EventType.END_DOCUMENT)
            if (reader.eventType == EventType.START_ELEMENT) {
                elements += reader.localName
                if (reader.localName == "first")
                    firstElementSeen.complete(Unit)
            }
        assertEquals(listOf("test", "first", "second"), elements)
    }

    @Test
    fun `newReader channel - I-O error is propagated`() = runTest {
        val channel = ByteChannel()
        channel.writeFully("<test><first/>".encodeToByteArray())
        channel.close(IOException("Connection reset"))

        val e = assertThrows(IOException::class.java) {
            XmlUtils.newReader(channel).drain()
        }
        assertEquals("Connection reset", e.message)
    }


    private val parserBlocks = " ".repeat(64 * 1024)

    private fun XmlReader.drain() {
        while (next() != EventType.END_DOCUMENT) {}
    }

    private fun XmlReader.rootText(): String {
        while (eventType != EventType.START_ELEMENT)
            next()
        val text = readSimpleElement()
        assertTrue(next() == EventType.END_DOCUMENT)
        return text
    }

}
