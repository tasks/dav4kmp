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
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.buffered

actual open class XmlPullParserException actual constructor(msg: String?) : Exception(msg)

actual interface XmlSerializer {
    actual fun setOutput(writer: Writer)
    actual fun startDocument(encoding: String?, standalone: Boolean?)
    actual fun endDocument()
    actual fun setPrefix(prefix: String, namespace: String)
    actual fun startTag(namespace: String?, name: String): XmlSerializer
    actual fun attribute(namespace: String?, name: String, value: String): XmlSerializer
    actual fun text(text: String): XmlSerializer
    actual fun endTag(namespace: String?, name: String): XmlSerializer
}

actual class StringReader actual constructor(s: String) {
    internal val string = s
}

actual abstract class Writer {
    abstract fun write(str: String)
}

actual class StringWriter actual constructor() : Writer() {
    private val buffer = StringBuilder()

    override fun write(str: String) {
        buffer.append(str)
    }

    override fun toString() = buffer.toString()
}

actual abstract class InputStream : AutoCloseable {
    /**
     * Reads up to [length] bytes into [buffer] at [offset].
     *
     * @return number of bytes read, or -1 at the end of the stream
     */
    abstract fun read(buffer: ByteArray, offset: Int, length: Int): Int

    actual override fun close() {}
}

internal class SourceInputStream(
    private val source: Source
) : InputStream() {
    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        try {
            source.readAtMostTo(buffer, offset, offset + length)
        } catch (_: IOException) {
            -1
        }

    override fun close() = source.close()
}

actual fun ByteReadChannel.toInputStream(): InputStream =
    SourceInputStream(asSource().buffered())

internal actual fun newPlatformPullParser(): XmlPullParser = LibXmlPullParser()

internal actual fun newPlatformSerializer(): XmlSerializer = KotlinXmlSerializer()
