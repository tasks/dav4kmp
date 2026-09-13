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

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter

object XmlUtils {

    const val XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

    /**
     * Requests the parser to be as lenient as possible when parsing invalid XML
     * (equivalent of `FEATURE_RELAXED` of Android's `XmlPullParser`).
     */
    private const val RELAXED = true

    private const val EXPAND_ENTITIES = false

    /**
     * Creates a new [XmlReader] for the given XML text, positioned at [EventType.START_DOCUMENT].
     *
     * Malformed or truncated XML causes an [XmlException] from [XmlReader.next].
     */
    @OptIn(ExperimentalXmlUtilApi::class)
    fun newReader(xml: String): XmlReader =
        SafeXmlReader(KtXmlReader(xml, expandEntities = EXPAND_ENTITIES, relaxed = RELAXED)).apply { next() }

    /**
     * Creates a new [XmlReader] that parses the XML document from [channel] while it is being
     * received, positioned at [EventType.START_DOCUMENT]. Reading from [channel] blocks the calling
     * thread until data is available (like `ByteReadChannel.asSource()`).
     *
     * The parser reads the document in blocks (a few KB), so an element becomes available once the
     * block it ends in has been received – or the document has ended. The pull parsers that
     * dav4jvm 4.x used (XPP3's `MXParser`, Android's `KXmlParser`) processed whatever a single
     * `Reader.read()` returned; the difference doesn't matter for the sizes of Multi-Status
     * responses.
     *
     * Malformed or truncated XML causes an [XmlException] from [XmlReader.next].
     *
     * The encoding is detected from the document itself (byte order mark or XML declaration,
     * XML 1.0 Appendix F), UTF-8 otherwise – like the pull parsers used by dav4jvm 4.x did. An
     * out-of-band charset (like the one from the HTTP `Content-Type`) is not taken into account,
     * see [ByteReadChannelReader].
     *
     * @param channel   channel to read the XML document from
     *
     * @throws XmlException if [channel] doesn't contain any data
     */
    @OptIn(ExperimentalXmlUtilApi::class, XmlUtilInternal::class)
    fun newReader(channel: ByteReadChannel): XmlReader {
        val xmlReader = try {
            val reader = ByteReadChannelReader(channel)
            KtXmlReader(reader, relaxed = RELAXED, expandEntities = EXPAND_ENTITIES)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            throw XmlException("Couldn't parse XML document: ${e.message}", e)
        }
        return SafeXmlReader(xmlReader).apply { next() }
    }

    fun newWriter(output: Appendable): XmlWriter =
        KtXmlWriter(output, isRepairNamespaces = true, xmlDeclMode = XmlDeclMode.None)

    fun buildDocument(
        namespaces: List<Pair<String, String>>,
        root: Property.Name,
        content: XmlWriter.() -> Unit
    ): String {
        val output = StringBuilder(XML_DECLARATION)
        val writer = newWriter(output)
        for ((prefix, namespace) in namespaces)
            writer.setPrefix(prefix, namespace)
        writer.startTag(root.namespace, root.name, null)
        for ((prefix, namespace) in namespaces)
            writer.namespaceAttr(prefix, namespace)
        writer.content()
        writer.endTag(root.namespace, root.name, null)
        writer.endDocument()
        return output.toString()
    }


    fun XmlWriter.insertTag(name: Property.Name, contentGenerator: XmlWriter.() -> Unit = {}) {
        startTag(name.namespace, name.name, null)
        contentGenerator(this)
        endTag(name.namespace, name.name, null)
    }

    fun XmlReader.propertyName(): Property.Name {
        if (eventType != EventType.START_ELEMENT && eventType != EventType.END_ELEMENT)
            throw IllegalStateException("Current event must be START_ELEMENT or END_ELEMENT")
        return Property.Name(namespaceURI, localName)
    }


    private class SafeXmlReader(delegate: XmlReader) : XmlDelegatingReader(delegate) {

        private var failure: XmlException? = null

        override fun next(): EventType {
            failure?.let { throw it }
            try {
                return checkedNext()
            } catch (e: XmlException) {
                failure = e
                throw e
            }
        }

        private fun checkedNext(): EventType {
            val eventType = try {
                delegate.next()
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                throw XmlException("Couldn't parse XML document: ${e.message}", e)
            }

            if (eventType == EventType.DOCDECL)
                throw XmlException("Document type declarations are not supported")

            if (eventType == EventType.END_DOCUMENT && depth > 0)
                throw XmlException("Unexpected end of XML document ($depth unclosed element(s))")

            return eventType
        }

    }

}
