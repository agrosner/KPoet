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
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass

class ParameterizedTypeName private constructor(private val enclosingType: ParameterizedTypeName?,
                                                val rawType: ClassName,
                                                val typeArguments: List<TypeName>,
                                                annotations: List<AnnotationSpec>)
    : TypeName(annotations = annotations) {
    internal constructor(enclosingType: ParameterizedTypeName?, rawType: ClassName,
                         typeArguments: List<TypeName>)
            : this(enclosingType, rawType, typeArguments, ArrayList<AnnotationSpec>())

    init {
        checkArgument(!this.typeArguments.isEmpty() || enclosingType != null,
                "no type arguments: %s", rawType)
        for (typeArgument in this.typeArguments) {
            checkArgument(!typeArgument.isPrimitive && typeArgument !== TypeName.Companion.VOID,
                    "invalid type parameter: %s", typeArgument)
        }
    }

    override fun annotated(annotations: List<AnnotationSpec>): ParameterizedTypeName {
        return ParameterizedTypeName(enclosingType, rawType, typeArguments, concatAnnotations(annotations))
    }

    override fun withoutAnnotations(): TypeName {
        return ParameterizedTypeName(enclosingType, rawType, typeArguments, arrayListOf<AnnotationSpec>())
    }

    @Throws(IOException::class)
    override fun emit(out: CodeWriter): CodeWriter {
        if (enclosingType != null) {
            enclosingType.emitAnnotations(out)
            enclosingType.emit(out)
            out.emit("." + rawType.simpleName())
        } else {
            rawType.emitAnnotations(out)
            rawType.emit(out)
        }
        if (!typeArguments.isEmpty()) {
            out.emitAndIndent("<")
            var firstParameter = true
            for (parameter in typeArguments) {
                if (!firstParameter) out.emitAndIndent(", ")
                parameter.emitAnnotations(out)
                parameter.emit(out)
                firstParameter = false
            }
            out.emitAndIndent(">")
        }
        return out
    }

    /**
     * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested
     * inside this class.
     */
    fun nestedClass(name: String): ParameterizedTypeName {
        return ParameterizedTypeName(this, rawType.nestedClass(name), listOf<TypeName>(),
                listOf<AnnotationSpec>())
    }

    /**
     * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested
     * inside this class, with the specified `typeArguments`.
     */
    fun nestedClass(name: String, typeArguments: List<TypeName>): ParameterizedTypeName {
        return ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments,
                listOf<AnnotationSpec>())
    }

    companion object {

        /**
         * Returns a parameterized type, applying `typeArguments` to `rawType`.
         */
        operator fun get(rawType: ClassName, vararg typeArguments: TypeName): ParameterizedTypeName {
            return ParameterizedTypeName(null, rawType, Arrays.asList(*typeArguments))
        }

        /**
         * Returns a parameterized type, applying `typeArguments` to `rawType`.
         */
        operator fun get(rawType: KClass<*>, vararg typeArguments: KClass<*>): ParameterizedTypeName {
            return ParameterizedTypeName(null, ClassName[rawType],
                    list(typeArguments.toList().map { it.java }.toTypedArray()))
        }

        /**
         * Returns a parameterized type equivalent to `type`.
         */
        operator fun get(type: ParameterizedType): ParameterizedTypeName {
            return get(type, LinkedHashMap<Type, TypeVariableName>())
        }

        /**
         * Returns a parameterized type equivalent to `type`.
         */
        internal operator fun get(type: ParameterizedType, map: MutableMap<Type, TypeVariableName>): ParameterizedTypeName {
            val rawType = ClassName[type.rawType as KClass<*>]
            val ownerType = if (type.ownerType is ParameterizedType && !Modifier.isStatic((type.rawType as Class<*>).modifiers))
                type.ownerType as ParameterizedType
            else
                null
            val typeArguments = TypeName.list(type.actualTypeArguments, map)
            return if (ownerType != null)
                get(ownerType, map).nestedClass(rawType.simpleName(), typeArguments)
            else
                ParameterizedTypeName(null, rawType, typeArguments)
        }
    }
}
