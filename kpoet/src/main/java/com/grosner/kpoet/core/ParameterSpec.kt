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
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

/** A generated parameter declaration.  */
class ParameterSpec private constructor(builder: ParameterSpec.Builder) {

    val name = builder.name
    val annotations = builder.annotations.toList()
    val modifiers = builder.modifiers.toSet()
    val type = builder.type

    fun hasModifier(modifier: Modifier) = modifier in modifiers

    @Throws(IOException::class)
    internal fun emit(codeWriter: CodeWriter, varargs: Boolean) {
        codeWriter.emitAnnotations(annotations, true)
        codeWriter.emitModifiers(modifiers)
        if (varargs) {
            codeWriter.emit("\$T... $name", TypeName.arrayComponent(type))
        } else {
            codeWriter.emit("\$T $name", type)
        }
    }

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
            emit(CodeWriter(out), false)
            return out.toString()
        } catch (e: IOException) {
            throw AssertionError()
        }

    }

    fun toBuilder() = toBuilder(type, name)

    internal fun toBuilder(type: TypeName, name: String) = Builder(type, name).apply {
        this.annotations.addAll(this@ParameterSpec.annotations)
        this.modifiers.addAll(this@ParameterSpec.modifiers)
    }

    class Builder internal constructor(internal val type: TypeName,
                                       internal val name: String) {

        internal val annotations = mutableListOf<AnnotationSpec>()
        internal val modifiers = mutableListOf<Modifier>()

        fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
            for (annotationSpec in annotationSpecs) {
                this.annotations.add(annotationSpec)
            }
        }

        fun addAnnotation(annotationSpec: AnnotationSpec) = apply {
            this.annotations.add(annotationSpec)
        }

        fun addAnnotation(annotation: ClassName) = apply {
            this.annotations.add(AnnotationSpec.builder(annotation).build())
        }

        fun addAnnotation(annotation: KClass<*>) = addAnnotation(ClassName[annotation])

        fun addModifiers(vararg modifiers: Modifier) = apply {
            this.modifiers.addAll(modifiers.toList())
        }

        fun addModifiers(modifiers: Iterable<Modifier>) = apply {
            for (modifier in modifiers) {
                this.modifiers.add(modifier)
            }
        }

        fun build() = ParameterSpec(this)
    }

    companion object {

        operator fun get(element: VariableElement)
                = ParameterSpec.builder(type = TypeName[element.asType()], name = element.simpleName.toString())
                .addModifiers(element.modifiers)
                .build()

        internal fun parametersOf(method: ExecutableElement) = method.parameters.map { ParameterSpec[it] }

        fun builder(type: TypeName, name: String, vararg modifiers: Modifier): Builder {
            checkArgument(SourceVersion.isName(name), "not a valid name: %s", name)
            return Builder(type, name)
                    .addModifiers(*modifiers)
        }

        fun builder(type: Type, name: String, vararg modifiers: Modifier): Builder {
            return builder(TypeName[type], name, *modifiers)
        }
    }
}
