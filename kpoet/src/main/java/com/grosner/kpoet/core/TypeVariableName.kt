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
import java.util.*
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeVariable

class TypeVariableName private constructor(val name: String, val bounds: List<TypeName>, annotations: List<AnnotationSpec> = ArrayList<AnnotationSpec>())
    : TypeName(annotations = annotations) {
    init {
        for (bound in this.bounds) {
            checkArgument(!bound.isPrimitive && bound !== TypeName.Companion.VOID, "invalid bound: %s", bound)
        }
    }

    override fun annotated(annotations: List<AnnotationSpec>) = TypeVariableName(name, bounds, annotations)

    override fun withoutAnnotations() = TypeVariableName(name, bounds)

    fun withBounds(vararg bounds: Type) = withBounds(TypeName.list(bounds.toList().toTypedArray()))

    fun withBounds(vararg bounds: TypeName) = withBounds(bounds.toList())

    fun withBounds(bounds: List<TypeName>) = TypeVariableName(name, arrayListOf<TypeName>().apply {
        addAll(this@TypeVariableName.bounds)
        addAll(bounds)
    }, annotations)

    @Throws(IOException::class)
    override fun emit(out: CodeWriter) = out.emitAndIndent(name)

    companion object {

        private fun of(name: String, bounds: List<TypeName>) = TypeVariableName(name, bounds.toMutableList().apply {
            // Strip java.lang.Object from bounds if it is present.
            remove(TypeName.OBJECT)
        })

        /**
         * Returns type variable named `name` without bounds.
         */
        operator fun get(name: String) = TypeVariableName.of(name, emptyList<TypeName>())

        /**
         * Returns type variable named `name` with `bounds`.
         */
        operator fun get(name: String, vararg bounds: TypeName) = TypeVariableName.of(name, bounds.toList())

        /**
         * Returns type variable named `name` with `bounds`.
         */
        operator fun get(name: String, vararg bounds: Type) = TypeVariableName.of(name, TypeName.list(bounds.toList().toTypedArray()))

        /**
         * Returns type variable equivalent to `mirror`.
         */
        operator fun get(mirror: TypeVariable) = get(mirror.asElement() as TypeParameterElement)

        /**
         * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
         * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
         * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
         * map before looking up the bounds. Then if we encounter this TypeVariable again while
         * constructing the bounds, we can just return it from the map. And, the code that put the entry
         * in `variables` will make sure that the bounds are filled in before returning.
         */
        internal operator fun get(
                mirror: TypeVariable, typeVariables: MutableMap<TypeParameterElement, TypeVariableName>): TypeVariableName {
            val element = mirror.asElement() as TypeParameterElement
            var typeVariableName: TypeVariableName? = typeVariables[element]
            if (typeVariableName == null) {
                // Since the bounds field is public, we need to make it an unmodifiableList. But we control
                // the List that that wraps, which means we can change it before returning.
                val bounds = ArrayList<TypeName>()
                val visibleBounds = Collections.unmodifiableList(bounds)
                typeVariableName = TypeVariableName(element.simpleName.toString(), visibleBounds)
                typeVariables.put(element, typeVariableName)
                element.bounds.mapTo(bounds) { TypeName[it, typeVariables] }
                bounds.remove(TypeName.Companion.OBJECT)
            }
            return typeVariableName
        }

        /**
         * Returns type variable equivalent to `element`.
         */
        operator fun get(element: TypeParameterElement)
                = TypeVariableName.of(element.simpleName.toString(),
                element.bounds.mapTo(ArrayList<TypeName>()) { TypeName[it] })

        /**
         * Returns type variable equivalent to `type`.
         */
        operator fun get(type: java.lang.reflect.TypeVariable<*>) = get(type, linkedMapOf<Type, TypeVariableName>())

        /**
         * @see .get
         */
        internal operator fun get(type: java.lang.reflect.TypeVariable<*>,
                                  map: MutableMap<Type, TypeVariableName>): TypeVariableName {
            var result: TypeVariableName? = map[type]
            if (result == null) {
                val bounds = ArrayList<TypeName>()
                val visibleBounds = Collections.unmodifiableList(bounds)
                result = TypeVariableName(type.name, visibleBounds)
                map.put(type, result)
                type.bounds.mapTo(bounds) { TypeName[it, map] }
                bounds.remove(TypeName.Companion.OBJECT)
            }
            return result
        }
    }
}
