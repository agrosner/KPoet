/*
 * Copyright (C) 2014 Google, Inc.
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
import java.util.*
import java.util.Map
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.NestingKind.MEMBER
import javax.lang.model.element.NestingKind.TOP_LEVEL
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

/**
 * A fully-qualified class name for top-level and member classes.
 */
class ClassName private constructor(
        /**
         * From top to bottom. This will be ["java.util", "Map", "Entry"] for [Map.Entry].
         */
        internal val names: List<String>, annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>())
    : TypeName(annotations), Comparable<ClassName> {

    internal val canonicalName: String

    init {
        for (i in 1..names.size - 1) {
            checkArgument(SourceVersion.isName(names[i]), "part '%s' is keyword", names[i])
        }
        this.canonicalName = when {
            names[0].isEmpty() -> names.subList(1, names.size).joinToString(separator = ".")
            else -> names.joinToString(separator = ".")
        }
    }

    override fun annotated(annotations: List<AnnotationSpec>) = ClassName(names, concatAnnotations(annotations))

    override fun withoutAnnotations() = ClassName(names)

    /**
     * Returns the package name, like `"java.util"` for `Map.Entry`.
     */
    fun packageName() = names[0]

    /**
     * Returns the enclosing class, like [Map] for `Map.Entry`. Returns null if this class
     * is not nested in another class.
     */
    fun enclosingClassName() = if (names.size == 2) null else ClassName(names.subList(0, names.size - 1))

    /**
     * Returns the top class in this nesting group. Equivalent to chained calls to [ ][.enclosingClassName] until the result's enclosing class is null.
     */
    fun topLevelClassName() = ClassName(names.subList(0, 2))

    fun reflectionName(): String {
        // trivial case: no nested names
        if (names.size == 2) {
            val packageName = packageName()
            return when {
                packageName.isEmpty() -> names[1]
                else -> "$packageName.${names[1]}"
            }
        }
        // concat top level class name and nested names
        return buildString {
            append(topLevelClassName())
            for (name in simpleNames().subList(1, simpleNames().size)) {
                append('$').append(name)
            }
        }
    }

    /**
     * Returns a new [ClassName] instance for the specified `name` as nested inside this
     * class.
     */
    fun nestedClass(name: String) = ClassName(ArrayList<String>(names.size + 1).apply {
        addAll(names)
        add(name)
    })

    fun simpleNames() = names.subList(1, names.size)

    /**
     * Returns a class that shares the same enclosing package or class. If this class is enclosed by
     * another class, this is equivalent to `enclosingClassName().nestedClass(name)`. Otherwise
     * it is equivalent to `get(packageName(), name)`.
     */
    fun peerClass(name: String) = ClassName(names.toMutableList().apply { this[size - 1] = name })

    /**
     * Returns the simple name of this class, like `"Entry"` for [Map.Entry].
     */
    fun simpleName() = names[names.size - 1]

    override fun compareTo(o: ClassName) = canonicalName.compareTo(o.canonicalName)

    @Throws(IOException::class)
    internal override fun emit(out: CodeWriter) = out.emitAndIndent(out.lookupName(this))

    companion object {
        val OBJECT = ClassName[Any::class]

        operator fun get(clazz: KClass<*>): ClassName {
            var javaClass = clazz.java
            checkArgument(!javaClass.isPrimitive, "primitive types cannot be represented as a ClassName")
            checkArgument(Void.TYPE != javaClass, "'void' type cannot be represented as a ClassName")
            checkArgument(!javaClass.isArray, "array types cannot be represented as a ClassName")
            val names = ArrayList<String>()
            while (true) {
                names.add(javaClass.simpleName)
                val enclosing = javaClass.enclosingClass ?: break
                javaClass = enclosing
            }
            // Avoid unreliable Class.getPackage(). https://github.com/square/javapoet/issues/295
            val lastDot = javaClass.name.lastIndexOf('.')
            if (lastDot != -1) names.add(javaClass.name.substring(0, lastDot))
            return ClassName(names.reversed())
        }

        /**
         * Returns a new [ClassName] instance for the given fully-qualified class name string. This
         * method assumes that the input is ASCII and follows typical Java style (lowercase package
         * names, UpperCamelCase class names) and may produce incorrect results or throw
         * [IllegalArgumentException] otherwise. For that reason, [.get] and
         * [.get] should be preferred as they can correctly create [ClassName]
         * instances without such restrictions.
         */
        fun bestGuess(classNameString: String): ClassName {
            val names = ArrayList<String>()

            // Add the package name, like "java.util.concurrent", or "" for no package.
            var p = 0
            while (p < classNameString.length && Character.isLowerCase(classNameString.codePointAt(p))) {
                p = classNameString.indexOf('.', p) + 1
                checkArgument(p != 0, "couldn't make a guess for %s", classNameString)
            }
            names.add(if (p != 0) classNameString.substring(0, p - 1) else "")

            // Add the class names, like "Map" and "Entry".
            for (part in classNameString.substring(p).split("\\.".toRegex()).toTypedArray()) {
                checkArgument(!part.isEmpty() && Character.isUpperCase(part.codePointAt(0)),
                        "couldn't make a guess for %s", classNameString)
                names.add(part)
            }

            checkArgument(names.size >= 2, "couldn't make a guess for %s", classNameString)
            return ClassName(names)
        }

        /**
         * Returns a class name created from the given parts. For example, calling this with package name
         * `"java.util"` and simple names `"Map"`, `"Entry"` yields [Map.Entry].
         */
        operator fun get(packageName: String, simpleName: String, vararg simpleNames: String)
                = ClassName(arrayListOf(packageName, simpleName, *simpleNames))

        /**
         * Returns the class name for `element`.
         */
        operator fun get(element: TypeElement): ClassName {
            val names = ArrayList<String>()
            var e: Element = element
            while (isClassOrInterface(e)) {
                checkArgument(element.nestingKind == TOP_LEVEL || element.nestingKind == MEMBER,
                        "unexpected type testing")
                names.add(e.simpleName.toString())
                e = e.enclosingElement
            }
            names.add(getPackage(element).qualifiedName.toString())
            return ClassName(names.reversed())
        }

        private fun isClassOrInterface(e: Element) = e.kind.isClass || e.kind.isInterface

        private fun getPackage(type: Element): PackageElement {
            var packageType = type
            while (packageType.kind != ElementKind.PACKAGE) {
                packageType = packageType.enclosingElement
            }
            return packageType as PackageElement
        }
    }
}
