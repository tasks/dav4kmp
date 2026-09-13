/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.property.carddav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.XmlReader

data class AddressData(
    val card: String?
): Property {

    companion object {

        // attributes
        const val CONTENT_TYPE = "content-type"
        const val VERSION = "version"

    }


    object Factory: PropertyFactory {

        override fun getName() = CardDAV.AddressData

        override fun create(parser: XmlReader) =
            // <!ELEMENT address-data (#PCDATA)>
            AddressData(parser.readText())

    }

}
