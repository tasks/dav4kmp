/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.property.webdav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.processTag
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

data class SupportedReportSet(
    val reports: Set<Property.Name> = emptySet()
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAV.SupportedReportSet

        override fun create(parser: XmlReader): SupportedReportSet {
            /* <!ELEMENT supported-report-set (supported-report*)>
               <!ELEMENT supported-report report>
               <!ELEMENT report ANY>
            */

            val reports = mutableSetOf<Property.Name>()

            parser.processTag(WebDAV.SupportedReport) {
                processTag(WebDAV.Report) {
                    parser.nextTag()
                    if (parser.eventType == EventType.START_ELEMENT)
                        reports += Property.Name(parser.namespaceURI, parser.localName)
                }
            }
            return SupportedReportSet(reports)
        }

    }

}