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

import com.grosner.kpoet.core.Util.*
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import java.util.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/**
 * A generated constructor or method declaration.
 */
class MethodSpec private constructor(builder: MethodSpec.Builder) {

    val name = builder.name
    val javadoc = builder.javadoc.build()
    val annotations = builder.annotations.toList()
    val modifiers = builder.modifiers.toSet()
    val typeVariables = builder.typeVariables.toList()
    val returnType = builder.returnType
    val parameters = builder.parameters.toList()
    val varargs = builder.varargs
    val exceptions = builder.exceptions.toList()
    val defaultValue = builder.defaultValue

    val code: CodeBlock

    init {
        val code = builder.code.build()
        checkArgument(code.isEmpty || !builder.modifiers.contains(Modifier.ABSTRACT),
                "abstract method %s cannot have code", builder.name)
        checkArgument(!builder.varargs || lastParameterIsArray(builder.parameters),
                "last parameter of varargs method %s must be an array", builder.name)

        this.code = code
    }

    private fun lastParameterIsArray(parameters: List<ParameterSpec>): Boolean {
        return !parameters.isEmpty()
                && TypeName.arrayComponent(parameters[parameters.size - 1].type) != null
    }


    @Throws(IOException::class)
    internal fun emit(codeWriter: CodeWriter, enclosingName: String, implicitModifiers: Set<Modifier>) {
        codeWriter.emitJavadoc(javadoc)
        codeWriter.emitAnnotations(annotations, false)
        codeWriter.emitModifiers(modifiers, implicitModifiers)

        if (!typeVariables.isEmpty()) {
            codeWriter.emitTypeVariables(typeVariables)
            codeWriter.emit(" ")
        }

        if (isConstructor) {
            codeWriter.emit("$enclosingName(")
        } else {
            codeWriter.emit("\$T $name(", returnType)
        }

        var firstParameter = true
        val i = parameters.iterator()
        while (i.hasNext()) {
            val parameter = i.next()
            if (!firstParameter) codeWriter.emit(",").emitWrappingSpace()
            parameter.emit(codeWriter, !i.hasNext() && varargs)
            firstParameter = false
        }

        codeWriter.emit(")")

        if (defaultValue != null && !defaultValue.isEmpty) {
            codeWriter.emit(" default ")
            codeWriter.emit(defaultValue)
        }

        if (!exceptions.isEmpty()) {
            codeWriter.emitWrappingSpace().emit("throws")
            var firstException = true
            for (exception in exceptions) {
                if (!firstException) codeWriter.emit(",")
                codeWriter.emitWrappingSpace().emit("\$T", exception)
                firstException = false
            }
        }

        if (hasModifier(Modifier.ABSTRACT)) {
            codeWriter.emit(";\n")
        } else if (hasModifier(Modifier.NATIVE)) {
            // Code is allowed to support stuff like GWT JSNI.
            codeWriter.emit(code)
            codeWriter.emit(";\n")
        } else {
            codeWriter.emit(" {\n")

            codeWriter.indent()
            codeWriter.emit(code)
            codeWriter.unindent()

            codeWriter.emit("}\n")
        }
    }

    fun hasModifier(modifier: Modifier) = modifier in modifiers

    val isConstructor: Boolean
        get() = name == CONSTRUCTOR

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null) return false
        if (javaClass != o.javaClass) return false
        return toString() == o.toString()
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun toString(): String {
        val out = StringWriter()
        try {
            val codeWriter = CodeWriter(out)
            emit(codeWriter, "Constructor", emptySet<Modifier>())
            return out.toString()
        } catch (e: IOException) {
            throw AssertionError()
        }

    }

    fun toBuilder(): Builder {
        val builder = Builder(name)
        builder.javadoc.add(javadoc)
        builder.annotations.addAll(annotations)
        builder.modifiers.addAll(modifiers)
        builder.typeVariables.addAll(typeVariables)
        builder.returnType = returnType
        builder.parameters.addAll(parameters)
        builder.exceptions.addAll(exceptions)
        builder.code.add(code)
        builder.varargs = varargs
        builder.defaultValue = defaultValue
        return builder
    }

    class Builder internal constructor(internal val name: String)
        : Typeable<Builder>, CodeAble<Builder> {

        internal val javadoc = CodeBlock.builder()
        internal val annotations = ArrayList<AnnotationSpec>()
        internal val modifiers = ArrayList<Modifier>()
        internal val typeVariables = ArrayList<TypeVariableName>()
        internal var returnType: TypeName? = null
        internal val parameters = ArrayList<ParameterSpec>()
        internal val exceptions = LinkedHashSet<TypeName>()
        internal val code = CodeBlock.builder()
        internal var varargs: Boolean = false
        internal var defaultValue: CodeBlock? = null

        init {
            checkArgument(name == CONSTRUCTOR || SourceVersion.isName(name),
                    "not a valid name: %s", name)
            this.returnType = if (name == CONSTRUCTOR) null else TypeName.VOID
        }

        override fun addJavadoc(format: String, vararg args: Any) = apply { javadoc.add(format, *args) }

        override fun addJavadoc(block: CodeBlock) = apply { javadoc.add(block) }

        override fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
            for (annotationSpec in annotationSpecs) {
                this.annotations.add(annotationSpec)
            }
        }

        override fun addAnnotation(annotationSpec: AnnotationSpec) = apply { this.annotations.add(annotationSpec) }

        override fun addAnnotation(className: ClassName) = apply {
            this.annotations.add(AnnotationSpec.builder(className).build())
        }

        override fun addAnnotation(annotation: KClass<*>) = addAnnotation(ClassName.get(annotation))

        override fun addModifiers(vararg modifiers: Modifier) = apply { this.modifiers.addAll(modifiers.toList()) }

        override fun addModifiers(modifiers: Iterable<Modifier>) = apply {
            for (modifier in modifiers) {
                this.modifiers.add(modifier)
            }
        }

        override fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
            for (typeVariable in typeVariables) {
                this.typeVariables.add(typeVariable)
            }
        }

        override fun addTypeVariable(typeVariable: TypeVariableName) = apply { typeVariables.add(typeVariable) }

        fun returns(returnType: TypeName) = apply {
            checkState(name != CONSTRUCTOR, "constructor cannot have return type.")
            this.returnType = returnType
        }

        fun returns(returnType: Type) = returns(TypeName[returnType])

        fun addParameters(parameterSpecs: Iterable<ParameterSpec>) = apply {
            for (parameterSpec in parameterSpecs) {
                this.parameters.add(parameterSpec)
            }
        }

        fun addParameter(parameterSpec: ParameterSpec) = apply { this.parameters.add(parameterSpec) }

        fun addParameter(type: TypeName, name: String, vararg modifiers: Modifier)
                = addParameter(ParameterSpec.builder(type, name, *modifiers).build())

        fun addParameter(type: Type, name: String, vararg modifiers: Modifier)
                = addParameter(TypeName[type], name, *modifiers)

        fun varargs(varargs: Boolean = true) = apply { this.varargs = varargs }

        fun addExceptions(exceptions: Iterable<TypeName>) = apply {
            for (exception in exceptions) {
                this.exceptions.add(exception)
            }
        }

        fun addException(exception: TypeName) = apply { this.exceptions.add(exception) }

        fun addException(exception: Type) = addException(TypeName[exception])

        override fun add(format: String, vararg args: Any?) = apply { code.add(format, *args) }

        override fun addNamed(format: String, args: Map<String, *>) = apply { code.addNamed(format, args) }

        override fun add(codeBlock: CodeBlock) = apply { code.add(codeBlock) }

        override fun addComment(format: String, vararg args: Any?) = apply { code.addComment(format, *args) }

        fun defaultValue(format: String, vararg args: Any) = defaultValue(CodeBlock.of(format, *args))

        fun defaultValue(codeBlock: CodeBlock) = apply {
            checkState(this.defaultValue == null, "defaultValue was already set")
            this.defaultValue = codeBlock
        }

        /**
         * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
         * *                    Shouldn't contain braces or newline characters.
         */
        override fun beginControlFlow(controlFlow: String, vararg args: Any?) = apply {
            code.beginControlFlow(controlFlow, *args)
        }

        /**
         * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
         * *                    Shouldn't contain braces or newline characters.
         */
        override fun nextControlFlow(controlFlow: String, vararg args: Any?) = apply {
            code.nextControlFlow(controlFlow, *args)
        }

        override fun endControlFlow() = apply { code.endControlFlow() }

        /**
         * @param controlFlow the optional control flow construct and its code, such as
         * *                    "while(foo == 20)". Only used for "do/while" control flows.
         */
        override fun endControlFlow(controlFlow: String, vararg args: Any?) = apply {
            code.endControlFlow(controlFlow, *args)
        }

        override fun addStatement(format: String, vararg args: Any?) = apply {
            code.addStatement(format, *args)
        }

        fun build(): MethodSpec {
            return MethodSpec(this)
        }
    }

    companion object {
        internal val CONSTRUCTOR = "<init>"

        fun methodBuilder(name: String): Builder {
            return Builder(name)
        }

        fun constructorBuilder(): Builder {
            return Builder(CONSTRUCTOR)
        }

        /**
         * Returns a new method spec builder that overrides `method`.
         *
         *
         *
         * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
         * throws declarations. An [Override] annotation will be added.
         *
         *
         *
         * Note that in JavaPoet 1.2 through 1.7 this method retained annotations from the method and
         * parameters of the overridden method. Since JavaPoet 1.8 annotations must be added separately.
         */
        fun overriding(method: ExecutableElement): Builder {

            var modifiers: MutableSet<Modifier> = method.modifiers
            if (modifiers.contains(Modifier.PRIVATE)
                    || modifiers.contains(Modifier.FINAL)
                    || modifiers.contains(Modifier.STATIC)) {
                throw IllegalArgumentException("cannot override method with modifiers: " + modifiers)
            }

            val methodName = method.simpleName.toString()
            val methodBuilder = com.grosner.kpoet.core.MethodSpec.methodBuilder(methodName)

            methodBuilder.addAnnotation(Override::class)

            modifiers = LinkedHashSet(modifiers)
            modifiers.remove(Modifier.ABSTRACT)
            modifiers.remove(Util.DEFAULT) // LinkedHashSet permits null as element for Java 7
            methodBuilder.addModifiers(modifiers)

            method.typeParameters
                    .map { it.asType() as TypeVariable }
                    .forEach { methodBuilder.addTypeVariable(TypeVariableName[it]) }

            methodBuilder.returns(TypeName[method.returnType])
            methodBuilder.addParameters(ParameterSpec.parametersOf(method))
            methodBuilder.varargs(method.isVarArgs)

            for (thrownType in method.thrownTypes) {
                methodBuilder.addException(TypeName[thrownType])
            }

            return methodBuilder
        }

        /**
         * Returns a new method spec builder that overrides `method` as a member of `enclosing`. This will resolve type parameters: for example overriding [ ][Comparable.compareTo] in a type that implements `Comparable<Movie>`, the `T`
         * parameter will be resolved to `Movie`.
         *
         *
         *
         * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
         * throws declarations. An [Override] annotation will be added.
         *
         *
         *
         * Note that in JavaPoet 1.2 through 1.7 this method retained annotations from the method and
         * parameters of the overridden method. Since JavaPoet 1.8 annotations must be added separately.
         */
        fun overriding(
                method: ExecutableElement, enclosing: DeclaredType, types: Types): Builder {
            val executableType = types.asMemberOf(enclosing, method) as ExecutableType
            val resolvedParameterTypes = executableType.parameterTypes
            val resolvedReturnType = executableType.returnType

            val builder = overriding(method)
            builder.returns(TypeName[resolvedReturnType])
            var i = 0
            val size = builder.parameters.size
            while (i < size) {
                val parameter = builder.parameters[i]
                val type = TypeName[resolvedParameterTypes[i]]
                builder.parameters[i] = parameter.toBuilder(type, parameter.name).build()
                i++
            }

            return builder
        }
    }
}
