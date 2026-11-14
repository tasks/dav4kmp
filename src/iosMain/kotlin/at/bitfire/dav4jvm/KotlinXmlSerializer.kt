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
 * [XmlSerializer] that produces the same output as XPP3's `MXSerializer`, which dav4jvm uses on the
 * JVM.
 *
 * Namespaces that haven't been bound with [setPrefix] get a generated prefix (`n1`, `n2`, …) that
 * is declared on the element.
 */
internal class KotlinXmlSerializer : XmlSerializer {

    private class Element(
        val qName: String,
        val bindings: Map<String, String>
    )

    private var writer: Writer? = null
    private val elements = ArrayDeque<Element>()
    private val pendingBindings = mutableListOf<Pair<String, String>>()
    private val generatedDeclarations = mutableListOf<Pair<String, String>>()
    private var startTagOpen = false
    private var generatedPrefixes = 0

    override fun setOutput(writer: Writer) {
        this.writer = writer
    }

    override fun startDocument(encoding: String?, standalone: Boolean?) {
        write("<?xml version=\"1.0\"")
        if (encoding != null)
            write(" encoding=\"$encoding\"")
        if (standalone != null)
            write(" standalone=\"${if (standalone) "yes" else "no"}\"")
        write("?>")
    }

    override fun endDocument() {
        while (elements.isNotEmpty())
            elements.last().let { endTag(null, it.qName) }
    }

    override fun setPrefix(prefix: String, namespace: String) {
        pendingBindings += prefix to namespace
    }

    override fun startTag(namespace: String?, name: String): XmlSerializer {
        closeStartTag()

        val bindings = (elements.lastOrNull()?.bindings ?: emptyMap()).toMutableMap()
        for ((prefix, ns) in pendingBindings)
            bindings[prefix] = ns
        val declarations = pendingBindings.toList()
        pendingBindings.clear()

        val prefix = when {
            namespace == null -> ""
            namespace.isEmpty() -> {
                if (!bindings[""].isNullOrEmpty())
                    throw IllegalStateException("start tag can not be written in empty default namespace as default namespace is currently bound to '${bindings[""]}'")
                ""
            }
            else -> bindings.entries.firstOrNull { it.value == namespace }?.key ?: run {
                val generated = "n${++generatedPrefixes}"
                bindings[generated] = namespace
                generatedDeclarations += generated to namespace
                generated
            }
        }

        val qName = if (prefix.isEmpty()) name else "$prefix:$name"
        elements.addLast(Element(qName, bindings))

        write("<$qName")
        for ((p, ns) in declarations)
            writeAttribute(if (p.isEmpty()) "xmlns" else "xmlns:$p", ns)
        startTagOpen = true
        return this
    }

    override fun attribute(namespace: String?, name: String, value: String): XmlSerializer {
        check(startTagOpen) { "startTag() must be called before attribute()" }
        val qName = if (namespace.isNullOrEmpty())
            name
        else {
            val prefix = elements.last().bindings.entries.firstOrNull { it.key.isNotEmpty() && it.value == namespace }?.key
                ?: throw IllegalStateException("No prefix bound to namespace $namespace")
            "$prefix:$name"
        }
        writeAttribute(qName, value)
        return this
    }

    override fun text(text: String): XmlSerializer {
        closeStartTag()
        val escaped = StringBuilder(text.length)
        for ((i, c) in text.withIndex())
            when (c) {
                '&' -> escaped.append("&amp;")
                '<' -> escaped.append("&lt;")
                '>' -> escaped.append(if (i >= 2 && text[i - 1] == ']' && text[i - 2] == ']') "&gt;" else ">")
                else -> escaped.append(c)
            }
        write(escaped.toString())
        return this
    }

    override fun endTag(namespace: String?, name: String): XmlSerializer {
        val element = elements.removeLastOrNull() ?: throw IllegalStateException("No open element")
        if (startTagOpen) {
            writeGeneratedDeclarations()
            write(" />")
            startTagOpen = false
        } else
            write("</${element.qName}>")
        return this
    }


    private fun closeStartTag() {
        if (startTagOpen) {
            writeGeneratedDeclarations()
            write(">")
            startTagOpen = false
        }
    }

    private fun writeGeneratedDeclarations() {
        for ((prefix, ns) in generatedDeclarations)
            writeAttribute("xmlns:$prefix", ns)
        generatedDeclarations.clear()
    }

    private fun writeAttribute(name: String, value: String) {
        val escaped = StringBuilder(value.length)
        for (c in value)
            when (c) {
                '&' -> escaped.append("&amp;")
                '<' -> escaped.append("&lt;")
                '"' -> escaped.append("&quot;")
                '\t' -> escaped.append("&#9;")
                '\n' -> escaped.append("&#10;")
                '\r' -> escaped.append("&#13;")
                else -> escaped.append(c)
            }
        write(" $name=\"$escaped\"")
    }

    private fun write(s: String) {
        (writer ?: throw IllegalStateException("setOutput() must be called first")).write(s)
    }

}
