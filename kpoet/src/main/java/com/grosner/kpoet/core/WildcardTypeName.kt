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
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.*
import javax.lang.model.element.TypeParameterElement

class WildcardTypeName private constructor(val upperBounds: List<TypeName>,
                                           val lowerBounds: List<TypeName>,
                                           annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>())
    : TypeName(annotations = annotations) {
    init {
        checkArgument(this.upperBounds.size == 1, "unexpected extends bounds: %s", upperBounds)
        for (upperBound in this.upperBounds) {
            checkArgument(!upperBound.isPrimitive && upperBound !== TypeName.Companion.VOID,
                    "invalid upper bound: %s", upperBound)
        }
        for (lowerBound in this.lowerBounds) {
            checkArgument(!lowerBound.isPrimitive && lowerBound !== TypeName.Companion.VOID,
                    "invalid lower bound: %s", lowerBound)
        }
    }

    override fun annotated(annotations: List<AnnotationSpec>): WildcardTypeName {
        return WildcardTypeName(upperBounds, lowerBounds, concatAnnotations(annotations))
    }

    override fun withoutAnnotations(): TypeName {
        return WildcardTypeName(upperBounds, lowerBounds)
    }

    @Throws(IOException::class)
    override fun emit(out: CodeWriter): CodeWriter {
        if (lowerBounds.size == 1) {
            return out.emit("? super \$T", lowerBounds[0])
        }
        return if (upperBounds[0] == TypeName.OBJECT)
            out.emit("?")
        else
            out.emit("? extends \$T", upperBounds[0])
    }

    companion object {

        /**
         * Returns a type that represents an unknown type that extends `bound`. For example, if
         * `bound` is `CharSequence.class`, this returns `? extends CharSequence`. If
         * `bound` is `Object.class`, this returns `?`, which is shorthand for `? extends Object`.
         */
        fun subtypeOf(upperBound: TypeName) = WildcardTypeName(arrayListOf(upperBound), emptyList<TypeName>())

        fun subtypeOf(upperBound: Type) = subtypeOf(TypeName[upperBound])

        /**
         * Returns a type that represents an unknown supertype of `bound`. For example, if `bound` is `String.class`, this returns `? super String`.
         */
        fun supertypeOf(lowerBound: TypeName)
                = WildcardTypeName(arrayListOf(TypeName.Companion.OBJECT), arrayListOf(lowerBound))

        fun supertypeOf(lowerBound: Type) = supertypeOf(TypeName[lowerBound])

        operator fun get(mirror: javax.lang.model.type.WildcardType): TypeName {
            return get(mirror, linkedMapOf<TypeParameterElement, TypeVariableName>())
        }

        internal operator fun get(
                mirror: javax.lang.model.type.WildcardType,
                typeVariables: MutableMap<TypeParameterElement, TypeVariableName>): TypeName {
            val extendsBound = mirror.extendsBound
            return if (extendsBound == null) {
                val superBound = mirror.superBound
                if (superBound == null) {
                    subtypeOf(Any::class.java)
                } else {
                    supertypeOf(TypeName[superBound, typeVariables])
                }
            } else {
                subtypeOf(TypeName[extendsBound, typeVariables])
            }
        }

        operator fun get(wildcardName: WildcardType) = get(wildcardName, linkedMapOf<Type, TypeVariableName>())

        internal operator fun get(wildcardName: WildcardType, map: MutableMap<Type, TypeVariableName>): TypeName {
            return WildcardTypeName(TypeName.list(wildcardName.upperBounds, map),
                    TypeName.list(wildcardName.lowerBounds, map))
        }
    }
}
