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

import com.grosner.kpoet.L
import com.grosner.kpoet.S
import com.grosner.kpoet.core.Util.characterLiteralWithoutSingleQuotes
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Array
import java.util.*
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor7
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod

/**
 * A generated annotation on a declaration.
 */
class AnnotationSpec private constructor(builder: AnnotationSpec.Builder) {

    val type = builder.type
    val members = Util.immutableMultimap(builder.members)

    @Throws(IOException::class)
    internal fun emit(codeWriter: CodeWriter, inline: Boolean) {
        val whitespace = if (inline) "" else "\n"
        val memberSeparator = if (inline) ", " else ",\n"
        if (members.isEmpty()) {
            // @Singleton
            codeWriter.emit("@\$T", type)
        } else if (members.size == 1 && members.containsKey("value")) {
            // @Named("foo")
            codeWriter.emit("@\$T(", type)
            emitAnnotationValues(codeWriter, whitespace, memberSeparator, members["value"]!!)
            codeWriter.emit(")")
        } else {
            // Inline:
            //   @Column(name = "updated_at", nullable = false)
            //
            // Not inline:
            //   @Column(
            //       name = "updated_at",
            //       nullable = false
            //   )
            codeWriter.emit("@\$T($whitespace", type)
            codeWriter.indent(2)
            val i = members.entries.iterator()
            while (i.hasNext()) {
                val entry = i.next()
                codeWriter.emit("\$L = ", entry.key)
                emitAnnotationValues(codeWriter, whitespace, memberSeparator, entry.value)
                if (i.hasNext()) codeWriter.emit(memberSeparator)
            }
            codeWriter.unindent(2)
            codeWriter.emit(whitespace + ")")
        }
    }

    @Throws(IOException::class)
    private fun emitAnnotationValues(codeWriter: CodeWriter, whitespace: String,
                                     memberSeparator: String, values: List<CodeBlock>) {
        if (values.size == 1) {
            codeWriter.indent(2)
            codeWriter.emit(values[0])
            codeWriter.unindent(2)
            return
        }

        codeWriter.emit("{" + whitespace)
        codeWriter.indent(2)
        var first = true
        for (codeBlock in values) {
            if (!first) codeWriter.emit(memberSeparator)
            codeWriter.emit(codeBlock)
            first = false
        }
        codeWriter.unindent(2)
        codeWriter.emit(whitespace + "}")
    }

    fun toBuilder(): Builder {
        val builder = Builder(type)
        for ((key, value) in members) {
            builder.members.put(key, ArrayList(value))
        }
        return builder
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null) return false
        if (javaClass != o.javaClass) return false
        return toString() == o.toString()
    }

    override fun hashCode() = toString().hashCode()

    override fun toString(): String {
        val out = StringWriter()
        try {
            val codeWriter = CodeWriter(out)
            codeWriter.emit(this.L)
            return out.toString()
        } catch (e: IOException) {
            throw AssertionError()
        }

    }

    class Builder internal constructor(internal val type: TypeName) {

        internal val members = linkedMapOf<String, MutableList<CodeBlock>>()

        fun addMember(name: String, format: String, vararg args: Any?)
                = addMember(name, CodeBlock.of(format, *args))

        fun addMember(name: String, codeBlock: CodeBlock): Builder {
            var values: MutableList<CodeBlock>? = members[name]
            if (values == null) {
                values = ArrayList<CodeBlock>()
                members.put(name, values)
            }
            values.add(codeBlock)
            return this
        }

        /**
         * Delegates to [.addMember], with parameter `format`
         * depending on the given `value` object. Falls back to `"$L"` literal format if
         * the class of the given `value` object is not supported.
         */
        internal fun addMemberForValue(memberName: String, value: Any?): Builder {
            if (value is KClass<*> || value is Class<*>) {
                return addMember(memberName, "\$T.class", value)
            }
            if (value is Enum<*>) {
                return addMember(memberName, "\$T.${value.name}", value.javaClass)
            }
            if (value is String) {
                return addMember(memberName, value.S)
            }
            if (value is Float) {
                return addMember(memberName, "${value}f")
            }
            if (value is Char) {
                return addMember(memberName, "'${characterLiteralWithoutSingleQuotes(value)}'")
            }
            return addMember(memberName, value.L)
        }

        fun build() = AnnotationSpec(this)
    }

    /**
     * Annotation value visitor adding members to the given builder instance.
     */
    private class Visitor internal constructor(internal val builder: Builder)
        : SimpleAnnotationValueVisitor7<Builder, String>(builder) {

        override fun defaultAction(o: Any?, name: String) = builder.addMemberForValue(name, o)

        override fun visitAnnotation(a: AnnotationMirror, name: String) = builder.addMember(name, get(a).L)

        override fun visitEnumConstant(c: VariableElement, name: String) = builder.addMember(name, "\$T.${c.simpleName}", c.asType())

        override fun visitType(t: TypeMirror, name: String): Builder {
            return builder.addMember(name, "\$T.class", t)
        }

        override fun visitArray(values: List<AnnotationValue>, name: String) = builder.apply {
            values.forEach { value -> value.accept(this@Visitor, name) }
        }
    }

    companion object {

        operator fun get(annotation: Annotation, includeDefaultValues: Boolean = false): AnnotationSpec {
            val builder = builder(annotation.annotationClass)
            try {
                val methods = annotation.annotationClass.declaredFunctions
                        .sortedWith(Comparator<KFunction<*>> { m1, m2 -> m1.name.compareTo(m2.name) })
                for (method in methods) {
                    val value = method.call(annotation)
                    if (!includeDefaultValues) {
                        if (Objects.deepEquals(value, method.javaMethod?.defaultValue)) {
                            continue
                        }
                    }
                    if (value != null && value.javaClass.isArray) {
                        for (i in 0..Array.getLength(value) - 1) {
                            builder.addMemberForValue(method.name, Array.get(value, i))
                        }
                        continue
                    }
                    if (value is Annotation) {
                        builder.addMember(method.name, get(value).L)
                        continue
                    }
                    builder.addMemberForValue(method.name, value)
                }
            } catch (e: Exception) {
                throw RuntimeException("Reflecting $annotation failed!", e)
            }

            return builder.build()
        }

        operator fun get(annotation: AnnotationMirror): AnnotationSpec {
            val element = annotation.annotationType.asElement() as TypeElement
            val builder = AnnotationSpec.builder(ClassName[element])
            val visitor = Visitor(builder)
            for (executableElement in annotation.elementValues.keys) {
                val name = executableElement.simpleName.toString()
                val value = annotation.elementValues[executableElement]
                value?.accept(visitor, name)
            }
            return builder.build()
        }

        fun builder(type: ClassName) = Builder(type)

        fun builder(type: KClass<*>) = builder(ClassName[type])
    }
}
