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

import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.property.caldav.SupportedCalendarData.Companion.CONTENT_TYPE
import at.bitfire.dav4jvm.property.caldav.SupportedCalendarData.Companion.VERSION
import io.ktor.http.ContentType
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Instant

private val logger: Logger
    get() = Logger.getLogger("at.bitfire.dav4jvm.XmlReader")

private val EventType.isText: Boolean
    get() = this == EventType.TEXT || this == EventType.CDSECT || this == EventType.ENTITY_REF || this == EventType.IGNORABLE_WHITESPACE

internal fun XmlReader.nextText(): String = buildString {
    while (next() != EventType.END_ELEMENT)
        when {
            eventType.isText -> append(text)
            eventType == EventType.COMMENT || eventType == EventType.PROCESSING_INSTRUCTION -> {}
            else -> throw XmlException("Expected text content or end tag, found: $eventType")
        }
}

/**
 * Reads child elements of the current element. Whenever a direct child with the given name is found,
 * [processor] is called for each one.
 */
fun XmlReader.processTag(name: Property.Name, processor: XmlReader.() -> Unit) {
    val startDepth = depth
    var eventType = eventType
    while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && depth == startDepth)) {
        if (eventType == EventType.START_ELEMENT && depth == startDepth + 1 && propertyName() == name)
            processor()
        eventType = next()
    }
}

/**
 * Reads the inline text of the current element.
 *
 * For instance, if the parser is at the beginning of this XML:
 *
 * ```
 * <tag>text</tag>
 * ```
 *
 * this function will return "text".
 *
 * @return text or `null` if no text is found
 */
fun XmlReader.readText(): String? {
    var last: String? = null
    var current: StringBuilder? = null

    val startDepth = depth
    var eventType = eventType
    while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && depth == startDepth)) {
        if (eventType.isText && depth == startDepth)
            current = (current ?: StringBuilder()).append(text)
        else if (eventType == EventType.START_ELEMENT && depth == startDepth + 1) {
            current?.let { last = it.toString() }
            current = null
        }
        eventType = next()
    }

    return current?.toString() ?: last
}

/**
 * Reads child elements of the current element. When a direct child with the given name is found,
 * its text is returned.
 *
 * @param name The name of the tag to read.
 * @return The text inside the tag, or `null` if the tag is not found.
 */
fun XmlReader.readTextProperty(name: Property.Name): String? {
    var result: String? = null

    val startDepth = depth
    var eventType = eventType
    while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && depth == startDepth)) {
        if (eventType == EventType.START_ELEMENT && depth == startDepth + 1 && propertyName() == name && result == null)
            result = nextText()
        eventType = next()
    }
    return result
}

/**
 * Reads child elements of the current element. Whenever a direct child with the given name is
 * found, its text is added to the given list.
 *
 * @param name The name of the tag to read.
 * @param list The list to add the text to.
 */
fun XmlReader.readTextPropertyList(name: Property.Name, list: MutableCollection<String>) {
    val startDepth = depth
    var eventType = eventType
    while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && depth == startDepth)) {
        if (eventType == EventType.START_ELEMENT && depth == startDepth + 1 && propertyName() == name)
            list.add(nextText())
        eventType = next()
    }
}


/**
 * Uses [readText] to read the tag's value (which is expected to be in _HTTP-date_ format), and converts
 * it into an [Instant] using [HttpUtils.parseDate].
 *
 * If the conversion fails for any reason, null is returned, and a message is displayed in log.
 */
fun XmlReader.readHttpDate(): Instant? {
    return readText()?.let { rawDate ->
        val date = HttpUtils.parseDate(rawDate)
        if (date != null)
            date
        else {
            logger.warning("Couldn't parse HTTP-date")
            null
        }
    }
}

/**
 * Uses [readText] to read the tag's value (which is expected to be a number), and converts it
 * into a [Long] with [String.toLong].
 *
 * If the conversion fails for any reason, null is returned, and a message is displayed in log.
 */
fun XmlReader.readLong(): Long? {
    return readText()?.let { valueStr ->
        try {
            valueStr.toLong()
        } catch(e: NumberFormatException) {
            logger.log(Level.WARNING, "Couldn't parse as Long: $valueStr", e)
            null
        }
    }
}

/**
 * Processes all the tags named [tagName], and sends every tag that has the [CONTENT_TYPE]
 * attribute with [onNewType].
 *
 * @param tagName The name of the tag that contains the [CONTENT_TYPE] attribute value.
 * @param onNewType Called every time a new [ContentType] is found.
 */
fun XmlReader.readContentTypes(tagName: Property.Name, onNewType: (String) -> Unit) {
    try {
        processTag(tagName) {
            getAttributeValue(null, CONTENT_TYPE)?.let { contentType ->
                var type = contentType
                getAttributeValue(null, VERSION)?.let { version -> type += "; version=$version" }
                try {
                    onNewType(ContentType.parse(type).toString())
                } catch (_: Exception) { }
            }
        }
    } catch(e: XmlException) {
        logger.log(Level.SEVERE, "Couldn't parse content types", e)
    }
}
