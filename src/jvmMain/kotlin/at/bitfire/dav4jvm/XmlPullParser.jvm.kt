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
import io.ktor.utils.io.asSource
import kotlinx.io.asInputStream
import kotlinx.io.buffered
import org.xmlpull.v1.XmlPullParserFactory

actual typealias XmlPullParserException = org.xmlpull.v1.XmlPullParserException

actual typealias XmlSerializer = org.xmlpull.v1.XmlSerializer

actual typealias StringReader = java.io.StringReader

actual typealias Writer = java.io.Writer

actual typealias StringWriter = java.io.StringWriter

actual typealias InputStream = java.io.InputStream

actual fun ByteReadChannel.toInputStream(): InputStream =
    asSource().buffered().asInputStream()


/**
 * Requests the parser to be as lenient as possible when parsing invalid XML.
 *
 * See [https://www.xmlpull.org/](xmlpull.org) and specific implementations, for instance
 * [Android XML](https://developer.android.com/reference/android/util/Xml#FEATURE_RELAXED)
 */
private const val FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed"

/** [XmlPullParserFactory] that is namespace-aware and does relaxed parsing */
private val relaxedFactory =
    XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature(FEATURE_RELAXED, true)
    }

/** [XmlPullParserFactory] that is namespace-aware */
private val standardFactory: XmlPullParserFactory =
    XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = true
    }

/**
 * Creates a new [XmlPullParser].
 *
 * First tries to create a namespace-aware parser that supports [FEATURE_RELAXED]. If that
 * fails, it falls back to a namespace-aware parser without relaxed parsing.
 *
 * @throws XmlPullParserException when no parser could be created
 */
internal actual fun newPlatformPullParser(): XmlPullParser =
    JvmXmlPullParser(
        try {
            relaxedFactory.newPullParser()
        } catch (_: XmlPullParserException) {
            // FEATURE_RELAXED may not be supported, try without it
            null
        }
        ?: standardFactory.newPullParser()
    )

internal actual fun newPlatformSerializer(): XmlSerializer = standardFactory.newSerializer()


private class JvmXmlPullParser(
    private val parser: org.xmlpull.v1.XmlPullParser
) : XmlPullParser {

    override val eventType get() = parser.eventType
    override val depth get() = parser.depth
    override val name: String? get() = parser.name
    override val namespace: String? get() = parser.namespace
    override val text: String? get() = parser.text

    override fun setInput(reader: StringReader) = parser.setInput(reader)
    override fun setInput(inputStream: InputStream, inputEncoding: String?) = parser.setInput(inputStream, inputEncoding)

    override fun next() = parser.next()
    override fun nextTag() = parser.nextTag()
    override fun nextText(): String = parser.nextText()

    override fun getAttributeValue(namespace: String?, name: String): String? = parser.getAttributeValue(namespace, name)

}
