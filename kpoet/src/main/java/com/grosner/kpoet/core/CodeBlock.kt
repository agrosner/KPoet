/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grosner.kpoet.core

import com.grosner.kpoet.core.Util.checkArgument
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * A fragment of a .java file, potentially containing declarations, statements, and documentation.
 * Code blocks are not necessarily well-formed Java code, and are not validated. This class assumes
 * javac will check correctness later!

 *
 * Code blocks support placeholders like [java.text.Format]. Where [String.format]
 * uses percent `%` to reference target values, this class uses dollar sign `$` and has
 * its own set of permitted placeholders:

 *
 *  * `$L` emits a *literal* value with no escaping. Arguments for literals may be
 * strings, primitives, [type declarations][TypeSpec], [       annotations][AnnotationSpec] and even other code blocks.
 *  * `$N` emits a *name*, using name collision avoidance where necessary. Arguments
 * for names may be strings (actually any [character sequence][CharSequence]),
 * [parameters][ParameterSpec], [fields][FieldSpec], [       ], and [types][TypeSpec].
 *  * `$S` escapes the value as a *string*, wraps it with double quotes, and emits
 * that. For example, `6" sandwich` is emitted `"6\" sandwich"`.
 *  * `$T` emits a *type* reference. Types will be imported if possible. Arguments
 * for types may be [classes][Class], [,*       type mirrors][TypeMirror], and [elements][Element].
 *  * `$$` emits a dollar sign.
 *  * `$W` emits a space or a newline, depending on its position on the line. This prefers
 * to wrap lines before 100 columns.
 *  * `$>` increases the indentation level.
 *  * `$<` decreases the indentation level.
 *  * `$[` begins a statement. For multiline statements, every line after the first line
 * is double-indented.
 *  * `$]` ends a statement.
 *
 */
class CodeBlock private constructor(builder: CodeBlock.Builder) {

    /** A heterogeneous list containing string literals and value placeholders.  */
    internal val formatParts = builder.formatParts.toList()
    internal val args = builder.args.toList()

    val isEmpty: Boolean
        get() = formatParts.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        return toString() == other.toString()
    }

    override fun hashCode() = toString().hashCode()

    override fun toString(): String {
        val out = StringWriter()
        try {
            com.grosner.kpoet.core.CodeWriter(out).emit(this)
            return out.toString()
        } catch (e: IOException) {
            throw AssertionError()
        }

    }

    fun toBuilder() = Builder().apply {
        this.formatParts.addAll(formatParts)
        this.args.addAll(args)
    }

    class Builder internal constructor() : CodeAble<Builder> {
        internal val formatParts: MutableList<String> = ArrayList()
        internal val args: MutableList<Any?> = ArrayList()

        /**
         * Adds code using named arguments.

         *
         * Named arguments specify their name after the '$' followed by : and the corresponding type
         * character. Argument names consist of characters in `a-z, A-Z, 0-9, and _` and must
         * start with a lowercase character.

         *
         * For example, to refer to the type [Integer] with the argument name `clazz` use a format string containing `$clazz:T` and include the key `clazz` with
         * value `java.lang.Integer.class` in the argument map.
         */
        override fun addNamed(format: String, args: Map<String, *>): Builder {
            var p = 0

            for (argument in args.keys) {
                checkArgument(LOWERCASE.matcher(argument).matches(),
                        "argument '%s' must start with a lowercase character", argument)
            }

            while (p < format.length) {
                val nextP = format.indexOf("$", p)
                if (nextP == -1) {
                    formatParts.add(format.substring(p, format.length))
                    break
                }

                if (p != nextP) {
                    formatParts.add(format.substring(p, nextP))
                    p = nextP
                }

                var matcher: Matcher? = null
                val colon = format.indexOf(':', p)
                if (colon != -1) {
                    val endIndex = Math.min(colon + 2, format.length)
                    matcher = NAMED_ARGUMENT.matcher(format.substring(p, endIndex))
                }
                if (matcher != null && matcher.lookingAt()) {
                    val argumentName = matcher.group("argumentName")
                    checkArgument(args.containsKey(argumentName), "Missing named argument for $%s",
                            argumentName)
                    val formatChar = matcher.group("typeChar")[0]
                    addArgument(format, formatChar, args[argumentName] ?: "Expecting argument, found null.")
                    formatParts.add("$" + formatChar)
                    p += matcher.regionEnd()
                } else {
                    checkArgument(p < format.length - 1, "dangling $ at end")
                    checkArgument(isNoArgPlaceholder(format[p + 1]),
                            "unknown format $%s at %s in '%s'", format[p + 1], p + 1, format)
                    formatParts.add(format.substring(p, p + 2))
                    p += 2
                }
            }

            return this
        }

        /**
         * Add code with positional or relative arguments.

         *
         * Relative arguments map 1:1 with the placeholders in the format string.

         *
         * Positional arguments use an index after the placeholder to identify which argument index
         * to use. For example, for a literal to reference the 3rd argument: "$3L" (1 based index)

         *
         * Mixing relative and positional arguments in a call to add is invalid and will result in an
         * error.
         */
        override fun add(format: String, vararg args: Any?): Builder {
            var hasRelative = false
            var hasIndexed = false

            var relativeParameterCount = 0
            val indexedParameterCount = IntArray(args.size)

            var p = 0
            while (p < format.length) {
                if (format[p] != '$') {
                    var nextP = format.indexOf('$', p + 1)
                    if (nextP == -1) nextP = format.length
                    formatParts.add(format.substring(p, nextP))
                    p = nextP
                    continue
                }

                p++ // '$'.

                // Consume zero or more digits, leaving 'c' as the first non-digit char after the '$'.
                val indexStart = p
                var c: Char
                do {
                    checkArgument(p < format.length, "dangling format characters in '%s'", format)
                    c = format[p++]
                } while (c in '0'..'9')
                val indexEnd = p - 1

                // If 'c' doesn't take an argument, we're done.
                if (isNoArgPlaceholder(c)) {
                    checkArgument(indexStart == indexEnd, "$$, $>, $<, $[, $], and \$W may not have an index")
                    formatParts.add("$" + c)
                    continue
                }

                // Find either the indexed argument, or the relative argument. (0-based).
                val index: Int
                if (indexStart < indexEnd) {
                    index = Integer.parseInt(format.substring(indexStart, indexEnd)) - 1
                    hasIndexed = true
                    if (args.isNotEmpty()) {
                        indexedParameterCount[index % args.size]++ // modulo is needed, checked below anyway
                    }
                } else {
                    index = relativeParameterCount
                    hasRelative = true
                    relativeParameterCount++
                }

                checkArgument(index >= 0 && index < args.size,
                        "index %d for '%s' not in range (received %s arguments)",
                        index + 1, format.substring(indexStart - 1, indexEnd + 1), args.size)
                checkArgument(!hasIndexed || !hasRelative, "cannot mix indexed and positional parameters")

                addArgument(format, c, args[index])

                formatParts.add("$" + c)
            }

            if (hasRelative) {
                checkArgument(relativeParameterCount >= args.size,
                        "unused arguments: expected %s, received %s", relativeParameterCount, args.size)
            }
            if (hasIndexed) {
                val unused = ArrayList<String>()
                for (i in args.indices) {
                    if (indexedParameterCount[i] == 0) {
                        unused.add("$" + (i + 1))
                    }
                }
                val s = if (unused.size == 1) "" else "s"
                checkArgument(unused.isEmpty(), "unused argument%s: %s", s, Util.join(", ", unused))
            }
            return this
        }

        private fun isNoArgPlaceholder(c: Char): Boolean {
            return c == '$' || c == '>' || c == '<' || c == '[' || c == ']' || c == 'W'
        }

        private fun addArgument(format: String, c: Char, arg: Any?) {
            when (c) {
                'N' -> this.args.add(argToName(arg))
                'L' -> this.args.add(argToLiteral(arg))
                'S' -> this.args.add(argToString(arg))
                'T' -> this.args.add(argToType(arg))
                else -> throw IllegalArgumentException(
                        String.format("invalid format string: '%s'", format))
            }
        }

        private fun argToName(o: Any?): String {
            if (o is CharSequence) return o.toString()
            if (o is ParameterSpec) return o.name
            if (o is FieldSpec) return o.name
            if (o is MethodSpec) return o.name
            if (o is TypeSpec) return o.name
            throw IllegalArgumentException("expected name but was " + o)
        }

        private fun argToLiteral(o: Any?) = o

        private fun argToString(o: Any?) = o?.toString()

        private fun argToType(o: Any?): TypeName {
            if (o is TypeName) return o
            if (o is TypeMirror) return TypeName[o]
            if (o is Element) return TypeName[o.asType()]
            if (o is Type) return TypeName[o]
            throw IllegalArgumentException("expected type but was " + o)
        }

        /**
         * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
         * * Shouldn't contain braces or newline characters.
         */
        override fun beginControlFlow(controlFlow: String, vararg args: Any?) = apply {
            add(controlFlow + " {\n", *args)
            indent()
        }

        /**
         * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
         * *     Shouldn't contain braces or newline characters.
         */
        override fun nextControlFlow(controlFlow: String, vararg args: Any?) = apply {
            unindent()
            add("} $controlFlow {\n", *args)
            indent()
        }

        override fun endControlFlow() = apply {
            unindent()
            add("}\n")
        }

        /**
         * @param controlFlow the optional control flow construct and its code, such as
         * *     "while(foo == 20)". Only used for "do/while" control flows.
         */
        override fun endControlFlow(controlFlow: String, vararg args: Any?) = apply {
            unindent()
            add("} $controlFlow;\n", *args)
        }

        override fun addStatement(format: String, vararg args: Any?) = apply {
            add("$[")
            add(format, *args)
            add(";\n$]")
        }

        override fun add(codeBlock: CodeBlock) = apply {
            formatParts.addAll(codeBlock.formatParts)
            args.addAll(codeBlock.args)
        }

        override fun addComment(format: String, vararg args: Any?) = add("// " + format + "\n", *args)

        fun indent() = apply { this.formatParts.add("$>") }

        fun unindent() = apply { this.formatParts.add("$<") }

        fun build() = CodeBlock(this)
    }

    companion object {
        private val NAMED_ARGUMENT = Pattern.compile("\\$(?<argumentName>[\\w_]+):(?<typeChar>[\\w]).*")
        private val LOWERCASE = Pattern.compile("[a-z]+[\\w_]*")

        fun of(format: String, vararg args: Any?) = Builder().add(format, *args).build()

        fun builder() = Builder()
    }
}
