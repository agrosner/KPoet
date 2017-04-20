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

import java.io.IOException
import java.lang.reflect.*
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.*
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.*
import javax.lang.model.util.SimpleTypeVisitor7
import kotlin.reflect.KClass

/**
 * Any type in Java's type system, plus `void`. This class is an identifier for primitive
 * types like `int` and raw reference types like `String` and `List`. It also
 * identifies composite types like `char[]` and `Set<Long>`.

 *
 * Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for `java.lang.List` doesn't know about the `size()` method, the fact that
 * lists are collections, or even that it accepts a single type parameter.

 *
 * Instances of this class are immutable value objects that implement `equals()` and `hashCode()` properly.

 * <h3>Referencing existing types</h3>

 *
 * Primitives and void are constants that you can reference directly: see [.INT], [ ][.DOUBLE], and [.VOID].

 *
 * In an addAnnotation processor you can get a type name instance for a type mirror by calling
 * [.get]. In reflection code, you can use [.get].

 * <h3>Defining new types</h3>

 *
 * Create new reference types like `com.example.HelloWorld` with [ ][ClassName.get]. To build composite types like `char[]` and
 * `Set<Long>`, use the factory methods on [ArrayTypeName], [ ], [TypeVariableName], and [WildcardTypeName].
 */
open class TypeName internal constructor(
        /** The name of this type if it is a keyword, or null.  */
        private val keyword: String? = null, val annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>()) {

    /** Lazily-initialized toString of this type name.  */
    private var cachedString: String? = null

    fun annotated(vararg annotations: AnnotationSpec) = annotated(annotations.toList())

    open fun annotated(annotations: List<AnnotationSpec>) = TypeName(keyword, concatAnnotations(annotations))

    open fun withoutAnnotations() = TypeName(keyword)

    protected fun concatAnnotations(annotations: List<AnnotationSpec>): List<AnnotationSpec> {
        val allAnnotations = ArrayList(this.annotations)
        allAnnotations.addAll(annotations)
        return allAnnotations
    }

    val isAnnotated: Boolean
        get() = !annotations.isEmpty()

    /**
     * Returns true if this is a primitive type like `int`. Returns false for all other types
     * types including boxed primitives and `void`.
     */
    val isPrimitive: Boolean
        get() = keyword != null && this !== TypeName.VOID

    /**
     * Returns true if this is a boxed primitive type like `Integer`. Returns false for all
     * other types types including unboxed primitives and `java.lang.Void`.
     */
    val isBoxedPrimitive: Boolean
        get() = this == BOXED_BOOLEAN
                || this == BOXED_BYTE
                || this == BOXED_SHORT
                || this == BOXED_INT
                || this == BOXED_LONG
                || this == BOXED_CHAR
                || this == BOXED_FLOAT
                || this == BOXED_DOUBLE

    /**
     * Returns a boxed type if this is a primitive type (like `Integer` for `int`) or
     * `void`. Returns this type if boxing doesn't apply.
     */
    fun box(): TypeName {
        if (keyword == null) return this // Doesn't need boxing.
        if (this === VOID) return BOXED_VOID
        if (this === BOOLEAN) return BOXED_BOOLEAN
        if (this === BYTE) return BOXED_BYTE
        if (this === SHORT) return BOXED_SHORT
        if (this === INT) return BOXED_INT
        if (this === LONG) return BOXED_LONG
        if (this === CHAR) return BOXED_CHAR
        if (this === FLOAT) return BOXED_FLOAT
        if (this === DOUBLE) return BOXED_DOUBLE
        throw AssertionError(keyword)
    }

    /**
     * Returns an unboxed type if this is a boxed primitive type (like `int` for `Integer`) or `Void`. Returns this type if it is already unboxed.

     * @throws UnsupportedOperationException if this type isn't eligible for unboxing.
     */
    fun unbox() = when {
        keyword != null -> this // Already unboxed.
        this == BOXED_VOID -> VOID
        this == BOXED_BOOLEAN -> BOOLEAN
        this == BOXED_BYTE -> BYTE
        this == BOXED_SHORT -> SHORT
        this == BOXED_INT -> INT
        this == BOXED_LONG -> LONG
        this == BOXED_CHAR -> CHAR
        this == BOXED_FLOAT -> FLOAT
        this == BOXED_DOUBLE -> DOUBLE
        else -> throw UnsupportedOperationException("cannot unbox " + this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        return toString() == other.toString()
    }

    override fun hashCode() = toString().hashCode()

    override fun toString(): String {
        var result = cachedString
        if (result == null) {
            try {
                val resultBuilder = StringBuilder()
                val codeWriter = CodeWriter(resultBuilder)
                emitAnnotations(codeWriter)
                emit(codeWriter)
                result = resultBuilder.toString()
                cachedString = result
            } catch (e: IOException) {
                throw AssertionError()
            }

        }
        return result
    }

    @Throws(IOException::class)
    internal open fun emit(out: CodeWriter): CodeWriter {
        if (keyword == null) throw AssertionError()
        return out.emitAndIndent(keyword)
    }

    @Throws(IOException::class)
    internal fun emitAnnotations(out: CodeWriter): CodeWriter {
        for (annotation in annotations) {
            annotation.emit(out, true)
            out.emit(" ")
        }
        return out
    }

    companion object {
        val VOID = TypeName("void")
        val BOOLEAN = TypeName("boolean")
        val BYTE = TypeName("byte")
        val SHORT = TypeName("short")
        val INT = TypeName("int")
        val LONG = TypeName("long")
        val CHAR = TypeName("char")
        val FLOAT = TypeName("float")
        val DOUBLE = TypeName("double")
        val OBJECT = ClassName["java.lang", "Object"]

        private val BOXED_VOID = ClassName["java.lang", "Void"]
        private val BOXED_BOOLEAN = ClassName["java.lang", "Boolean"]
        private val BOXED_BYTE = ClassName["java.lang", "Byte"]
        private val BOXED_SHORT = ClassName["java.lang", "Short"]
        private val BOXED_INT = ClassName["java.lang", "Integer"]
        private val BOXED_LONG = ClassName["java.lang", "Long"]
        private val BOXED_CHAR = ClassName["java.lang", "Character"]
        private val BOXED_FLOAT = ClassName["java.lang", "Float"]
        private val BOXED_DOUBLE = ClassName["java.lang", "Double"]

        /** Returns a type name equivalent to `mirror`.  */
        operator fun get(mirror: TypeMirror) = get(mirror, linkedMapOf<TypeParameterElement, TypeVariableName>())

        internal operator fun get(mirror: TypeMirror,
                                  typeVariables: MutableMap<TypeParameterElement, TypeVariableName>): TypeName {
            return mirror.accept(object : SimpleTypeVisitor7<TypeName, Void>() {
                override fun visitPrimitive(t: PrimitiveType, p: Void): TypeName {
                    when (t.kind) {
                        TypeKind.BOOLEAN -> return TypeName.BOOLEAN
                        TypeKind.BYTE -> return TypeName.BYTE
                        TypeKind.SHORT -> return TypeName.SHORT
                        TypeKind.INT -> return TypeName.INT
                        TypeKind.LONG -> return TypeName.LONG
                        TypeKind.CHAR -> return TypeName.CHAR
                        TypeKind.FLOAT -> return TypeName.FLOAT
                        TypeKind.DOUBLE -> return TypeName.DOUBLE
                        else -> throw AssertionError()
                    }
                }

                override fun visitDeclared(t: DeclaredType, p: Void): TypeName {
                    val rawType = ClassName[t.asElement() as TypeElement]
                    val enclosingType = t.enclosingType
                    val enclosing = if (enclosingType.kind != TypeKind.NONE
                            && !t.asElement().modifiers.contains(Modifier.STATIC))
                        enclosingType.accept(this, null)
                    else
                        null
                    if (t.typeArguments.isEmpty() && enclosing !is ParameterizedTypeName) {
                        return rawType
                    }

                    val typeArgumentNames = t.typeArguments.map { get(it, typeVariables) }
                    return (enclosing as? ParameterizedTypeName)?.nestedClass(rawType.simpleName(), typeArgumentNames)
                            ?: ParameterizedTypeName(null, rawType, typeArgumentNames)
                }

                override fun visitError(t: ErrorType, p: Void) = visitDeclared(t, p)

                override fun visitArray(t: ArrayType, p: Void) = ArrayTypeName[t, typeVariables]

                override fun visitTypeVariable(t: javax.lang.model.type.TypeVariable, p: Void): TypeName {
                    return TypeVariableName[t, typeVariables]
                }

                override fun visitWildcard(t: javax.lang.model.type.WildcardType, p: Void): TypeName {
                    return WildcardTypeName[t, typeVariables]
                }

                override fun visitNoType(t: NoType, p: Void)
                        = if (t.kind == TypeKind.VOID) TypeName.VOID else super.visitUnknown(t, p)

                override fun defaultAction(e: TypeMirror?, p: Void?): TypeName {
                    throw IllegalArgumentException("Unexpected type mirror: " + e!!)
                }
            }, null)
        }

        /** Returns a type name equivalent to `type`.  */
        operator fun get(type: Type) = get(type, LinkedHashMap<Type, TypeVariableName>())

        internal operator fun get(type: Type, map: MutableMap<Type, TypeVariableName>): TypeName {
            if (type is KClass<*>) {
                val classType = type.java
                if (type === Void.TYPE) return VOID
                if (type === Boolean::class.javaPrimitiveType) return BOOLEAN
                if (type === Byte::class.javaPrimitiveType) return BYTE
                if (type === Short::class.javaPrimitiveType) return SHORT
                if (type === Int::class.javaPrimitiveType) return INT
                if (type === Long::class.javaPrimitiveType) return LONG
                if (type === Char::class.javaPrimitiveType) return CHAR
                if (type === Float::class.javaPrimitiveType) return FLOAT
                if (type === Double::class.javaPrimitiveType) return DOUBLE
                if (classType.isArray) return ArrayTypeName.of(get(classType.componentType, map))
                return ClassName[type]

            } else if (type is ParameterizedType) {
                return ParameterizedTypeName[type, map]

            } else if (type is WildcardType) {
                return WildcardTypeName[type, map]

            } else if (type is TypeVariable<*>) {
                return TypeVariableName[type, map]

            } else if (type is GenericArrayType) {
                return ArrayTypeName[type, map]

            } else {
                throw IllegalArgumentException("unexpected type: " + type)
            }
        }

        @JvmOverloads internal fun list(types: Array<Type>, map: MutableMap<Type, TypeVariableName>
        = linkedMapOf<Type, TypeVariableName>()): List<TypeName> {
            return ArrayList<TypeName>(types.size).apply { types.mapTo(this) { TypeName[it, map] } }
        }

        /** Returns the array component of `type`, or null if `type` is not an array.  */
        internal fun arrayComponent(type: TypeName) = if (type is ArrayTypeName) type.componentType else null
    }
}
