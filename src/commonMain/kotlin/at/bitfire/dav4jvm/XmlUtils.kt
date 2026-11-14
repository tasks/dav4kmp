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

object XmlUtils {

    /**
     * Creates a new [XmlPullParser].
     *
     * On the JVM, it first tries to create a namespace-aware parser that supports relaxed parsing. If that
     * fails, it falls back to a namespace-aware parser without relaxed parsing. On iOS, libxml2 is used.
     *
     * @throws XmlPullParserException when no parser could be created
     */
    fun newPullParser(): XmlPullParser = newPlatformPullParser()

    fun newSerializer(): XmlSerializer = newPlatformSerializer()


    fun XmlSerializer.insertTag(name: Property.Name, contentGenerator: XmlSerializer.() -> Unit = {}) {
        startTag(name.namespace, name.name)
        contentGenerator(this)
        endTag(name.namespace, name.name)
    }

    fun XmlPullParser.propertyName(): Property.Name {
        val propNs = namespace
        val propName = name
        if (propNs == null || propName == null)
            throw IllegalStateException("Current event must be START_TAG or END_TAG")
        return Property.Name(propNs, propName)
    }

}