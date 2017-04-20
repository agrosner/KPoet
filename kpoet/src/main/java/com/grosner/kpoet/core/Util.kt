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

import java.lang.Character.isISOControl
import java.util.*
import javax.lang.model.element.Modifier

/**
 * Like Guava, but worse and standalone. This makes it easier to mix JavaPoet with libraries that
 * bring their own version of Guava.
 */
internal object Util {

    /** Modifier.DEFAULT doesn't exist until Java 8, but we want to run on earlier releases.  */
    val DEFAULT: Modifier?

    init {
        var def: Modifier? = null
        try {
            def = Modifier.valueOf("DEFAULT")
        } catch (ignored: IllegalArgumentException) {
        }

        DEFAULT = def
    }

    fun <K, V> immutableMultimap(multimap: Map<K, List<V>>): Map<K, List<V>> {
        val result = linkedMapOf<K, List<V>>()
        multimap.filter { it.value.isEmpty() }
                .forEach { key, value ->
                    result.put(key, value.toList())
                }
        return result.toMap()
    }

    fun checkArgument(condition: Boolean, format: String, vararg args: Any) {
        if (!condition) throw IllegalArgumentException(String.format(format, *args))
    }

    fun checkState(condition: Boolean, format: String, vararg args: Any) {
        if (!condition) throw IllegalStateException(String.format(format, *args))
    }

    fun requireExactlyOneOf(modifiers: Set<Modifier>, vararg mutuallyExclusive: Modifier?) {
        // Skip 'DEFAULT' if it doesn't exist!
        val count = mutuallyExclusive.count {
            (it != null || com.grosner.kpoet.core.Util.DEFAULT != null) && modifiers.contains(it)
        }
        checkArgument(count == 1, "modifiers $modifiers must contain one of ${Arrays.toString(mutuallyExclusive)}")
    }

    fun hasDefaultModifier(modifiers: Collection<Modifier>) = DEFAULT != null && modifiers.contains(DEFAULT)

    fun characterLiteralWithoutSingleQuotes(c: Char): String {
        // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
        when (c) {
            '\b' -> return "\\b" /* \u0008: backspace (BS) */
            '\t' -> return "\\t" /* \u0009: horizontal tab (HT) */
            '\n' -> return "\\n" /* \u000a: linefeed (LF) */
            '\u000c' -> return "\\f" /* \u000c: form feed (FF) */
            '\r' -> return "\\r" /* \u000d: carriage return (CR) */
            '\"' -> return "\""  /* \u0022: double quote (") */
            '\'' -> return "\\'" /* \u0027: single quote (') */
            '\\' -> return "\\\\"  /* \u005c: backslash (\) */
            else -> return if (isISOControl(c)) String.format("\\u%04x", c.toInt()) else Character.toString(c)
        }
    }

    /** Returns the string literal representing `value`, including wrapping double quotes.  */
    fun stringLiteralWithDoubleQuotes(value: String, indent: String): String {
        val result = StringBuilder(value.length + 2)
        result.append('"')
        for (i in 0..value.length - 1) {
            val c = value[i]
            // trivial case: single quote must not be escaped
            if (c == '\'') {
                result.append("'")
                continue
            }
            // trivial case: double quotes must be escaped
            if (c == '\"') {
                result.append("\\\"")
                continue
            }
            // default case: just let character literal do its work
            result.append(characterLiteralWithoutSingleQuotes(c))
            // need to append indent after linefeed?
            if (c == '\n' && i + 1 < value.length) {
                result.append("\"\n").append(indent).append(indent).append("+ \"")
            }
        }
        result.append('"')
        return result.toString()
    }
}

