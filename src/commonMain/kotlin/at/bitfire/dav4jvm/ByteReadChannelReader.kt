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

import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asSource
import kotlinx.io.Source
import kotlinx.io.buffered
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader

private val logger: Logger = KtorSimpleLogger("at.bitfire.dav4jvm.ByteReadChannelReader")

@OptIn(XmlUtilInternal::class)
internal class ByteReadChannelReader(
    channel: ByteReadChannel
): Reader() {

    private val source: Source = channel.asSource().buffered()

    private val decoder: Decoder

    private val bytes = ByteArray(CHUNK_SIZE)

    private var carry = 0

    private var chars = ""
    private var pos = 0

    private var eof = false

    init {
        val head = peekBytes(4)

        val bomLength: Int
        val bomEncoding: String?
        when {
            head.startsWith(0xEF, 0xBB, 0xBF) -> { bomLength = 3; bomEncoding = UTF_8 }
            head.startsWith(0xFE, 0xFF) -> { bomLength = 2; bomEncoding = UTF_16BE }
            head.startsWith(0xFF, 0xFE) -> { bomLength = 2; bomEncoding = UTF_16LE }
            else -> { bomLength = 0; bomEncoding = null }
        }

        fun detectWithoutBom(): String? = when {
            head.startsWith(0x00, 0x3C, 0x00, 0x3F) -> UTF_16BE
            head.startsWith(0x3C, 0x00, 0x3F, 0x00) -> UTF_16LE
            head.startsWith(0x3C, 0x3F, 0x78, 0x6D) -> encodingFromXmlDeclaration()
            else -> null
        }

        val name = bomEncoding
            ?: detectWithoutBom()
            ?: UTF_8

        decoder = Decoder.forName(name) ?: run {
            logger.warn("Unsupported encoding: $name, reading document as UTF-8")
            Decoder.UTF_8
        }

        source.skip(bomLength.toLong())
    }


    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (len <= 0)
            return 0

        while (pos >= chars.length)
            if (!fill())
                return -1

        val count = minOf(len, chars.length - pos)
        chars.toCharArray(buf, offset, pos, pos + count)
        pos += count
        return count
    }

    override fun close() {}


    private fun fill(): Boolean {
        if (eof)
            return false
        pos = 0

        val read = source.readAtMostTo(bytes, carry, bytes.size)
        if (read == -1) {
            eof = true
            chars = decode(carry, endOfInput = true)
            return chars.isNotEmpty()
        }

        chars = decode(carry + read, endOfInput = false)
        return true
    }

    private fun decode(end: Int, endOfInput: Boolean): String {
        val (result, consumed) = decoder.decode(bytes, end, endOfInput)

        bytes.copyInto(bytes, 0, consumed, end)
        carry = end - consumed
        return result
    }


    private fun encodingFromXmlDeclaration(): String? {
        val head = peekBytes(MAX_XML_DECLARATION_SIZE)
        val end = head.indexOfDeclarationEnd(head.size)
        if (end == -1)
            return null

        val declaration = CharArray(end) { Char(head[it].toInt() and 0xFF) }.concatToString()
        return ENCODING_PSEUDO_ATTRIBUTE.find(declaration)?.groupValues?.get(1)
    }

    private fun ByteArray.indexOfDeclarationEnd(length: Int): Int {
        for (i in 0 until length - 1)
            if (this[i] == '?'.code.toByte() && this[i + 1] == '>'.code.toByte())
                return i
        return -1
    }

    private fun peekBytes(count: Int): ByteArray {
        val peek = source.peek()
        val result = ByteArray(count)
        var read = 0
        while (read < count) {
            val n = peek.readAtMostTo(result, read, count)
            if (n <= 0)
                break
            read += n
        }
        return result.copyOf(read)
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
        if (size < prefix.size)
            return false
        for (i in prefix.indices)
            if (this[i].toInt() and 0xFF != prefix[i])
                return false
        return true
    }


    companion object {

        private const val CHUNK_SIZE = 8*1024

        private const val MAX_XML_DECLARATION_SIZE = 256

        private const val UTF_8 = "UTF-8"
        private const val UTF_16BE = "UTF-16BE"
        private const val UTF_16LE = "UTF-16LE"

        private val ENCODING_PSEUDO_ATTRIBUTE = Regex("""encoding\s*=\s*["']([A-Za-z][A-Za-z0-9._-]*)["']""")

    }

}


private enum class Decoder {
    UTF_8,
    UTF_16BE,
    UTF_16LE,
    SINGLE_BYTE;

    fun decode(bytes: ByteArray, end: Int, endOfInput: Boolean): Pair<String, Int> {
        val consumed = if (endOfInput) end else endOfCompleteCharacters(bytes, end)
        return decode(bytes, 0, consumed) to consumed
    }

    private fun endOfCompleteCharacters(bytes: ByteArray, end: Int): Int = when (this) {
        SINGLE_BYTE ->
            end

        UTF_16BE, UTF_16LE ->
            end - end % 2

        UTF_8 -> {
            var start = end - 1
            val limit = maxOf(0, end - 4)
            while (start >= limit && bytes[start].toInt() and 0xC0 == 0x80)
                start--

            if (start < limit)
                end
            else {
                val lead = bytes[start].toInt() and 0xFF
                val length = when {
                    lead < 0x80 -> 1
                    lead and 0xE0 == 0xC0 -> 2
                    lead and 0xF0 == 0xE0 -> 3
                    lead and 0xF8 == 0xF0 -> 4
                    else -> 1
                }
                if (end - start >= length) end else start
            }
        }
    }

    private fun utf16Unit(bytes: ByteArray, index: Int): Int {
        val b0 = bytes[index].toInt() and 0xFF
        val b1 = bytes[index + 1].toInt() and 0xFF
        return if (this == UTF_16BE)
            (b0 shl 8) or b1
        else
            (b1 shl 8) or b0
    }

    private fun decode(bytes: ByteArray, start: Int, end: Int): String = when (this) {
        UTF_8 ->
            bytes.decodeToString(start, end)

        UTF_16BE, UTF_16LE -> {
            val length = end - start
            val result = CharArray(length / 2) { utf16Unit(bytes, start + 2*it).toChar() }.concatToString()
            if (length % 2 == 0)
                result
            else
                result + REPLACEMENT_CHARACTER
        }

        SINGLE_BYTE ->
            CharArray(end - start) { Char(bytes[start + it].toInt() and 0xFF) }.concatToString()
    }


    companion object {

        private const val REPLACEMENT_CHARACTER = '\uFFFD'

        fun forName(name: String): Decoder? = when (canonicalName(name)) {
            "UTF8" -> UTF_8
            "UTF16", "UTF16BE" -> UTF_16BE
            "UTF16LE" -> UTF_16LE
            "ISO88591", "LATIN1", "USASCII", "ASCII" -> SINGLE_BYTE
            else -> null
        }

        private fun canonicalName(name: String) =
            name.uppercase().replace("-", "").replace("_", "")

    }

}
