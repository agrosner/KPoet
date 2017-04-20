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
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type
import java.util.*
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.ArrayType

class ArrayTypeName private constructor(val componentType: TypeName,
                                        annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>())
    : TypeName(annotations = annotations) {

    override fun annotated(annotations: List<AnnotationSpec>) = ArrayTypeName(componentType, concatAnnotations(annotations))

    override fun withoutAnnotations() = ArrayTypeName(componentType)

    @Throws(IOException::class)
    override fun emit(out: CodeWriter) = out.emit("\$T[]", componentType)

    companion object {

        /**
         * Returns an array type whose elements are all instances of `componentType`.
         */
        fun of(componentType: TypeName) = ArrayTypeName(componentType)

        /**
         * Returns an array type whose elements are all instances of `componentType`.
         */
        fun of(componentType: Type) = of(TypeName.get(componentType))

        /**
         * Returns an array type equivalent to `mirror`.
         */
        operator fun get(mirror: ArrayType) = get(mirror, LinkedHashMap<TypeParameterElement, TypeVariableName>())

        internal operator fun get(mirror: ArrayType,
                                  typeVariables: MutableMap<TypeParameterElement, TypeVariableName>): ArrayTypeName {
            return ArrayTypeName(TypeName[mirror.componentType, typeVariables])
        }

        /**`
         * Returns an array type equivalent to `type`.
         */
        operator fun get(type: GenericArrayType) = get(type, linkedMapOf<Type, TypeVariableName>())

        internal operator fun get(type: GenericArrayType, map: MutableMap<Type, TypeVariableName>): ArrayTypeName {
            return ArrayTypeName.of(TypeName[type.genericComponentType, map])
        }
    }
}
