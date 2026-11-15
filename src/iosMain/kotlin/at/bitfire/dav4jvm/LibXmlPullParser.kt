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

import at.bitfire.dav4jvm.libxml2.XML_PARSE_NONET
import at.bitfire.dav4jvm.libxml2.XML_PARSE_NOWARNING
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_CDATA
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_DOCUMENT_TYPE
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_ELEMENT
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_END_ELEMENT
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_SIGNIFICANT_WHITESPACE
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_TEXT
import at.bitfire.dav4jvm.libxml2.XML_READER_TYPE_WHITESPACE
import at.bitfire.dav4jvm.libxml2.xmlCharVar
import at.bitfire.dav4jvm.libxml2.xmlError
import at.bitfire.dav4jvm.libxml2.xmlFreeTextReader
import at.bitfire.dav4jvm.libxml2.xmlInitParser
import at.bitfire.dav4jvm.libxml2.xmlReaderForIO
import at.bitfire.dav4jvm.libxml2.xmlTextReaderConstLocalName
import at.bitfire.dav4jvm.libxml2.xmlTextReaderConstNamespaceUri
import at.bitfire.dav4jvm.libxml2.xmlTextReaderConstValue
import at.bitfire.dav4jvm.libxml2.xmlTextReaderDepth
import at.bitfire.dav4jvm.libxml2.xmlTextReaderIsEmptyElement
import at.bitfire.dav4jvm.libxml2.xmlTextReaderMoveToElement
import at.bitfire.dav4jvm.libxml2.xmlTextReaderMoveToNextAttribute
import at.bitfire.dav4jvm.libxml2.xmlTextReaderNodeType
import at.bitfire.dav4jvm.libxml2.xmlTextReaderPtr
import at.bitfire.dav4jvm.libxml2.xmlTextReaderRead
import at.bitfire.dav4jvm.libxml2.xmlTextReaderSetStructuredErrorHandler
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

@OptIn(ExperimentalForeignApi::class)
private val libXmlInitialized: Unit by lazy {
    xmlInitParser()
}

/**
 * [XmlPullParser] that uses the libxml2 `xmlTextReader` and behaves like `org.xmlpull.v1.XmlPullParser`:
 * consecutive text, CDATA sections and resolved references are reported as a single [XmlPullParser.TEXT]
 * event; comments, processing instructions and whitespace outside of the root element are skipped.
 *
 * Document type declarations are skipped; network access is disabled.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal class LibXmlPullParser : XmlPullParser {

    private class Node(
        val type: Int,
        val depth: Int,
        val name: String? = null,
        val namespace: String? = null,
        val text: String? = null,
        val attributes: List<Attribute> = emptyList()
    )

    private class Attribute(
        val namespace: String,
        val name: String,
        val value: String
    )

    private class Input(
        val stream: InputStream
    ) {
        var failure: Throwable? = null
        var errorMessage: String? = null
    }

    private class Handle {
        var reader: xmlTextReaderPtr? = null
        var inputRef: StableRef<Input>? = null

        fun free() {
            reader?.let { xmlFreeTextReader(it) }
            reader = null
            inputRef?.dispose()
            inputRef = null
        }
    }

    private val handle = Handle()

    @Suppress("unused")
    private val cleaner: Cleaner = createCleaner(handle) { it.free() }

    private var input: Input? = null
    private val pending = ArrayDeque<Node>()
    private var current = Node(XmlPullParser.START_DOCUMENT, 0)

    override val eventType get() = current.type
    override val depth get() = current.depth
    override val name get() = current.name
    override val namespace get() = current.namespace
    override val text get() = current.text

    override fun setInput(reader: StringReader) {
        open(SourceInputStream(kotlinx.io.Buffer().apply { write(reader.string.encodeToByteArray()) }), "UTF-8")
    }

    override fun setInput(inputStream: InputStream, inputEncoding: String?) {
        open(inputStream, inputEncoding)
    }

    private fun open(stream: InputStream, encoding: String?) {
        libXmlInitialized
        handle.free()
        pending.clear()
        current = Node(XmlPullParser.START_DOCUMENT, 0)

        val input = Input(stream)
        this.input = input
        val inputRef = StableRef.create(input)
        val reader = xmlReaderForIO(
            readCallback,
            closeCallback,
            inputRef.asCPointer(),
            null,
            encoding,
            (XML_PARSE_NONET or XML_PARSE_NOWARNING).toInt()
        )
        if (reader == null) {
            inputRef.dispose()
            throw input.failure ?: XmlPullParserException("Couldn't create XML parser")
        }
        xmlTextReaderSetStructuredErrorHandler(reader, errorCallback, inputRef.asCPointer())
        handle.reader = reader
        handle.inputRef = inputRef
    }

    override fun getAttributeValue(namespace: String?, name: String): String? {
        if (current.type != XmlPullParser.START_TAG)
            throw IndexOutOfBoundsException("Current event must be START_TAG")
        val ns = namespace.orEmpty()
        return current.attributes.firstOrNull { it.namespace == ns && it.name == name }?.value
    }

    override fun next(): Int {
        if (current.type == XmlPullParser.END_DOCUMENT)
            return XmlPullParser.END_DOCUMENT

        var text: StringBuilder? = null
        var textDepth = 0
        while (true) {
            val node = pending.removeFirstOrNull() ?: readNode()
            if (node.type == XmlPullParser.TEXT) {
                if (node.depth == 0)
                    continue
                if (text == null) {
                    text = StringBuilder()
                    textDepth = node.depth
                }
                text.append(node.text)
                continue
            }
            if (text != null) {
                pending.addFirst(node)
                current = Node(XmlPullParser.TEXT, textDepth, text = text.toString())
            } else
                current = node
            return current.type
        }
    }

    override fun nextTag(): Int {
        var eventType = next()
        if (eventType == XmlPullParser.TEXT && current.text.orEmpty().all { it == ' ' || it == '\t' || it == '\n' || it == '\r' })
            eventType = next()
        if (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_TAG)
            throw XmlPullParserException("expected start or end tag")
        return eventType
    }

    override fun nextText(): String {
        if (current.type != XmlPullParser.START_TAG)
            throw XmlPullParserException("parser must be on START_TAG to read next text")
        var eventType = next()
        return when (eventType) {
            XmlPullParser.TEXT -> {
                val result = current.text.orEmpty()
                eventType = next()
                if (eventType != XmlPullParser.END_TAG)
                    throw XmlPullParserException("event TEXT must be immediately followed by END_TAG")
                result
            }
            XmlPullParser.END_TAG -> ""
            else -> throw XmlPullParserException("parser must be on START_TAG or TEXT to read text")
        }
    }

    /**
     * Reads the next relevant node from libxml2. Comments and processing instructions are skipped.
     * An empty element is returned as start tag, and an end tag is queued in [pending].
     */
    private fun readNode(): Node {
        val reader = handle.reader ?: throw XmlPullParserException("No input")
        while (true) {
            when (xmlTextReaderRead(reader)) {
                1 -> {}
                0 -> {
                    handle.free()
                    return Node(XmlPullParser.END_DOCUMENT, 0)
                }
                else -> {
                    val failure = input?.failure
                    val message = input?.errorMessage
                    handle.free()
                    throw failure ?: XmlPullParserException(message ?: "Couldn't parse XML")
                }
            }

            val nodeDepth = xmlTextReaderDepth(reader)
            when (xmlTextReaderNodeType(reader)) {
                XML_READER_TYPE_ELEMENT.toInt() -> {
                    val name = xmlTextReaderConstLocalName(reader).toKStringOrEmpty()
                    val namespace = xmlTextReaderConstNamespaceUri(reader).toKStringOrEmpty()
                    val empty = xmlTextReaderIsEmptyElement(reader) == 1
                    val attributes = mutableListOf<Attribute>()
                    while (xmlTextReaderMoveToNextAttribute(reader) == 1) {
                        val attrNamespace = xmlTextReaderConstNamespaceUri(reader).toKStringOrEmpty()
                        if (attrNamespace != NS_XMLNS)
                            attributes += Attribute(
                                attrNamespace,
                                xmlTextReaderConstLocalName(reader).toKStringOrEmpty(),
                                xmlTextReaderConstValue(reader).toKStringOrEmpty()
                            )
                    }
                    xmlTextReaderMoveToElement(reader)
                    if (empty)
                        pending.addLast(Node(XmlPullParser.END_TAG, nodeDepth + 1, name, namespace))
                    return Node(XmlPullParser.START_TAG, nodeDepth + 1, name, namespace, attributes = attributes)
                }
                XML_READER_TYPE_END_ELEMENT.toInt() ->
                    return Node(
                        XmlPullParser.END_TAG,
                        nodeDepth + 1,
                        xmlTextReaderConstLocalName(reader).toKStringOrEmpty(),
                        xmlTextReaderConstNamespaceUri(reader).toKStringOrEmpty()
                    )
                XML_READER_TYPE_TEXT.toInt(),
                XML_READER_TYPE_CDATA.toInt(),
                XML_READER_TYPE_WHITESPACE.toInt(),
                XML_READER_TYPE_SIGNIFICANT_WHITESPACE.toInt() ->
                    return Node(XmlPullParser.TEXT, nodeDepth, text = xmlTextReaderConstValue(reader).toKStringOrEmpty())
                XML_READER_TYPE_DOCUMENT_TYPE.toInt() -> {}
            }
        }
    }


    private companion object {

        const val NS_XMLNS = "http://www.w3.org/2000/xmlns/"

        val readCallback = staticCFunction { context: COpaquePointer?, buffer: CPointer<ByteVar>?, len: Int ->
            val input = context!!.asStableRef<Input>().get()
            try {
                val bytes = ByteArray(len)
                val count = input.stream.read(bytes, 0, len)
                if (count <= 0)
                    0
                else {
                    bytes.usePinned { pinned ->
                        memcpy(buffer, pinned.addressOf(0), count.convert())
                    }
                    count
                }
            } catch (e: Throwable) {
                input.failure = e
                -1
            }
        }

        val closeCallback = staticCFunction { _: COpaquePointer? -> 0 }

        val errorCallback = staticCFunction { context: COpaquePointer?, error: CPointer<xmlError>? ->
            val input = context!!.asStableRef<Input>().get()
            val message = error?.pointed?.message?.toKString()?.trim()
            if (message != null)
                input.errorMessage = message
        }

        fun CPointer<xmlCharVar>?.toKStringOrEmpty(): String =
            this?.reinterpret<ByteVar>()?.toKString().orEmpty()

    }

}
