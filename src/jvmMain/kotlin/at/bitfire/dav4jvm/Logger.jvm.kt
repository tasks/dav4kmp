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

internal actual fun platformLog(loggerName: String, level: Level, msg: String, thrown: Throwable?) {
    val logger = java.util.logging.Logger.getLogger(loggerName)
    val julLevel = when (level) {
        Level.SEVERE -> java.util.logging.Level.SEVERE
        Level.WARNING -> java.util.logging.Level.WARNING
        Level.INFO -> java.util.logging.Level.INFO
        Level.CONFIG -> java.util.logging.Level.CONFIG
        Level.FINE -> java.util.logging.Level.FINE
        Level.FINER -> java.util.logging.Level.FINER
        else -> java.util.logging.Level.FINEST
    }
    if (logger.isLoggable(julLevel))
        logger.logp(julLevel, loggerName, null, msg, thrown)
}
