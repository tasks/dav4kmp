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

/**
 * The part of `java.util.logging.Logger` that is used by dav4jvm.
 *
 * On the JVM, messages are logged with `java.util.logging`; on iOS, with Ktor's logger.
 */
class Logger private constructor(
    val name: String
) {

    fun severe(msg: String) = log(Level.SEVERE, msg)
    fun warning(msg: String) = log(Level.WARNING, msg)
    fun info(msg: String) = log(Level.INFO, msg)
    fun config(msg: String) = log(Level.CONFIG, msg)
    fun fine(msg: String) = log(Level.FINE, msg)
    fun finer(msg: String) = log(Level.FINER, msg)
    fun finest(msg: String) = log(Level.FINEST, msg)

    fun log(level: Level, msg: String) = log(level, msg, null)

    fun log(level: Level, msg: String, thrown: Throwable?) =
        platformLog(name, level, msg, thrown)

    companion object {
        fun getLogger(name: String) = Logger(name)
    }

}

/**
 * Log levels of `java.util.logging.Level`.
 */
class Level private constructor(
    val name: String,
    val intValue: Int
) {

    override fun toString() = name

    companion object {
        val SEVERE = Level("SEVERE", 1000)
        val WARNING = Level("WARNING", 900)
        val INFO = Level("INFO", 800)
        val CONFIG = Level("CONFIG", 700)
        val FINE = Level("FINE", 500)
        val FINER = Level("FINER", 400)
        val FINEST = Level("FINEST", 300)
    }

}

internal expect fun platformLog(loggerName: String, level: Level, msg: String, thrown: Throwable?)
