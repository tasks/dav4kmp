/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

repositories {
    mavenCentral()
}

/** Checks whether this string is a valid semver: `X.Y.Z` with an optional pre-release suffix like `-alpha01` */
fun String.isSemVer(): Boolean = "\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z.]+)?".toRegex().matches(this)

group = "org.tasks"

val releaseVersion: String? = System.getenv("DAV4JVM_VERSION")
if (releaseVersion != null)
    require(releaseVersion.isSemVer()) { "DAV4JVM_VERSION must be X.Y.Z or X.Y.Z-suffix, not \"$releaseVersion\"" }
version = releaseVersion ?: "SNAPSHOT"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)

    alias(libs.plugins.dokka)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.coroutines.core)
            api(libs.ktor.client.core)
            api(libs.xmlutil.core)

            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.encoding)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlin.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(group.toString(), "dav4kmp", version.toString())

    pom {
        name.set("dav4kmp")
        description.set("WebDAV/CalDAV/CardDAV library for Kotlin Multiplatform (JVM/Android and iOS)")
        url.set("https://github.com/tasks/dav4kmp")
        inceptionYear.set("2015")

        licenses {
            license {
                name.set("Mozilla Public License, Version 2.0")
                url.set("https://mozilla.org/MPL/2.0/")
            }
        }

        developers {
            developer {
                id.set("bitfireAT")
                name.set("bitfire web engineering GmbH")
                url.set("https://www.bitfire.at")
            }
            developer {
                id.set("abaker")
                name.set("Alex Baker")
                url.set("https://tasks.org")
            }
        }

        scm {
            url.set("https://github.com/tasks/dav4kmp")
            connection.set("scm:git:https://github.com/tasks/dav4kmp.git")
            developerConnection.set("scm:git:git@github.com:tasks/dav4kmp.git")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "dav4kmp"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("dav4jvm")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URI("https://github.com/bitfireAT/dav4jvm/tree/main/src/main/kotlin/").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}