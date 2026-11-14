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

/**
 * The part of the [XmlPullParser API](https://www.xmlpull.org/) that is used by dav4jvm.
 *
 * On the JVM, it's implemented by `org.xmlpull.v1.XmlPullParser` (the system parser on Android,
 * XPP3 otherwise); on iOS, by libxml2.
 *
 * Like `org.xmlpull.v1.XmlPullParser.next`, [next] only reports [START_DOCUMENT], [END_DOCUMENT],
 * [START_TAG], [END_TAG] and [TEXT].
 */
interface XmlPullParser {

    val eventType: Int
    val depth: Int
    val name: String?
    val namespace: String?
    val text: String?

    fun setInput(reader: StringReader)
    fun setInput(inputStream: InputStream, inputEncoding: String?)

    fun next(): Int
    fun nextTag(): Int
    fun nextText(): String

    fun getAttributeValue(namespace: String?, name: String): String?

    companion object {
        const val START_DOCUMENT = 0
        const val END_DOCUMENT = 1
        const val START_TAG = 2
        const val END_TAG = 3
        const val TEXT = 4
    }

}

expect open class XmlPullParserException(msg: String?) : Exception

/**
 * The part of the [XmlSerializer API](https://www.xmlpull.org/) that is used by dav4jvm.
 */
expect interface XmlSerializer {
    fun setOutput(writer: Writer)
    fun startDocument(encoding: String?, standalone: Boolean?)
    fun endDocument()
    fun setPrefix(prefix: String, namespace: String)
    fun startTag(namespace: String?, name: String): XmlSerializer
    fun attribute(namespace: String?, name: String, value: String): XmlSerializer
    fun text(text: String): XmlSerializer
    fun endTag(namespace: String?, name: String): XmlSerializer
}

expect class StringReader(s: String)

expect abstract class Writer

expect class StringWriter() : Writer

expect abstract class InputStream : AutoCloseable {
    override fun close()
}

/**
 * Creates an [InputStream] that reads from the channel. Reading blocks the calling thread until
 * data is available.
 */
expect fun ByteReadChannel.toInputStream(): InputStream

internal expect fun newPlatformPullParser(): XmlPullParser

internal expect fun newPlatformSerializer(): XmlSerializer
