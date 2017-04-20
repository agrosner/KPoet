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
import com.grosner.kpoet.core.Util.checkState
import com.grosner.kpoet.core.Util.hasDefaultModifier
import com.grosner.kpoet.core.Util.requireExactlyOneOf
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import java.util.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

/**
 * A generated class, interface, or enum declaration.
 */
class TypeSpec private constructor(builder: TypeSpec.Builder) {
    val kind = builder.kind
    val name = builder.name
    val anonymousTypeArguments = builder.anonymousTypeArguments
    val javadoc = builder.javadoc.build()
    val annotations = builder.annotations.toList()
    val modifiers = builder.modifiers.toSet()
    val typeVariables = builder.typeVariables.toList()
    val superclass = builder.superclass
    val superinterfaces = builder.superinterfaces.toList()
    val enumConstants = builder.enumConstants.toMap()
    val fieldSpecs = builder.fieldSpecs.toList()
    val staticBlock = builder.staticBlock.build()
    val initializerBlock = builder.initializerBlock.build()
    val methodSpecs = builder.methodSpecs.toList()
    val typeSpecs = builder.typeSpecs.toList()
    val originatingElements: List<Element>

    init {
        val originatingElementsMutable = arrayListOf<Element>()
        originatingElementsMutable.addAll(builder.originatingElements)
        for (typeSpec in builder.typeSpecs) {
            originatingElementsMutable.addAll(typeSpec.originatingElements)
        }
        this.originatingElements = originatingElementsMutable
    }

    fun hasModifier(modifier: Modifier) = modifier in modifiers

    fun toBuilder(): Builder {
        val builder = Builder(kind, name, anonymousTypeArguments)
        builder.javadoc.add(javadoc)
        builder.annotations.addAll(annotations)
        builder.modifiers.addAll(modifiers)
        builder.typeVariables.addAll(typeVariables)
        builder.superclass = superclass
        builder.superinterfaces.addAll(superinterfaces)
        builder.enumConstants.putAll(enumConstants)
        builder.fieldSpecs.addAll(fieldSpecs)
        builder.methodSpecs.addAll(methodSpecs)
        builder.typeSpecs.addAll(typeSpecs)
        builder.initializerBlock.add(initializerBlock)
        builder.staticBlock.add(staticBlock)
        return builder
    }

    @Throws(IOException::class)
    internal fun emit(codeWriter: CodeWriter, enumName: String?, implicitModifiers: Set<Modifier>) {
        // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
        // it back afterwards when this type is complete.
        val previousStatementLine = codeWriter.statementLine
        codeWriter.statementLine = -1

        try {
            if (enumName != null) {
                codeWriter.emitJavadoc(javadoc)
                codeWriter.emitAnnotations(annotations, false)
                codeWriter.emit("\$L", enumName)
                if (!anonymousTypeArguments!!.formatParts.isEmpty()) {
                    codeWriter.emit("(")
                    codeWriter.emit(anonymousTypeArguments)
                    codeWriter.emit(")")
                }
                if (fieldSpecs.isEmpty() && methodSpecs.isEmpty() && typeSpecs.isEmpty()) {
                    return  // Avoid unnecessary braces "{}".
                }
                codeWriter.emit(" {\n")
            } else if (anonymousTypeArguments != null) {
                val supertype = if (!superinterfaces.isEmpty()) superinterfaces[0] else superclass
                codeWriter.emit("new \$T(", supertype)
                codeWriter.emit(anonymousTypeArguments)
                codeWriter.emit(") {\n")
            } else {
                codeWriter.emitJavadoc(javadoc)
                codeWriter.emitAnnotations(annotations, false)
                codeWriter.emitModifiers(modifiers, implicitModifiers.union(kind.asMemberModifiers))
                if (kind == Kind.ANNOTATION) {
                    codeWriter.emit("\$L \$L", "@interface", name)
                } else {
                    codeWriter.emit("\$L \$L", kind.name.toLowerCase(Locale.US), name)
                }
                codeWriter.emitTypeVariables(typeVariables)

                val extendsTypes: List<TypeName>
                val implementsTypes: List<TypeName>
                if (kind == Kind.INTERFACE) {
                    extendsTypes = superinterfaces
                    implementsTypes = emptyList<TypeName>()
                } else {
                    extendsTypes = if (superclass == ClassName.OBJECT)
                        emptyList<TypeName>()
                    else
                        listOf(superclass)
                    implementsTypes = superinterfaces
                }

                if (!extendsTypes.isEmpty()) {
                    codeWriter.emit(" extends")
                    var firstType = true
                    for (type in extendsTypes) {
                        if (!firstType) codeWriter.emit(",")
                        codeWriter.emit(" \$T", type)
                        firstType = false
                    }
                }

                if (!implementsTypes.isEmpty()) {
                    codeWriter.emit(" implements")
                    var firstType = true
                    for (type in implementsTypes) {
                        if (!firstType) codeWriter.emit(",")
                        codeWriter.emit(" \$T", type)
                        firstType = false
                    }
                }

                codeWriter.emit(" {\n")
            }

            codeWriter.pushType(this)
            codeWriter.indent()
            var firstMember = true
            val i = enumConstants.entries.iterator()
            while (i.hasNext()) {
                val enumConstant = i.next()
                if (!firstMember) codeWriter.emit("\n")
                enumConstant.value
                        .emit(codeWriter, enumConstant.key, emptySet<Modifier>())
                firstMember = false
                if (i.hasNext()) {
                    codeWriter.emit(",\n")
                } else if (!fieldSpecs.isEmpty() || !methodSpecs.isEmpty() || !typeSpecs.isEmpty()) {
                    codeWriter.emit(";\n")
                } else {
                    codeWriter.emit("\n")
                }
            }

            // Static fields.
            for (fieldSpec in fieldSpecs) {
                if (!fieldSpec.hasModifier(Modifier.STATIC)) continue
                if (!firstMember) codeWriter.emit("\n")
                fieldSpec.emit(codeWriter, kind.implicitFieldModifiers)
                firstMember = false
            }

            if (!staticBlock.isEmpty) {
                if (!firstMember) codeWriter.emit("\n")
                codeWriter.emit(staticBlock)
                firstMember = false
            }

            // Non-static fields.
            for (fieldSpec in fieldSpecs) {
                if (fieldSpec.hasModifier(Modifier.STATIC)) continue
                if (!firstMember) codeWriter.emit("\n")
                fieldSpec.emit(codeWriter, kind.implicitFieldModifiers)
                firstMember = false
            }

            // Initializer block.
            if (!initializerBlock.isEmpty) {
                if (!firstMember) codeWriter.emit("\n")
                codeWriter.emit(initializerBlock)
                firstMember = false
            }

            // Constructors.
            for (methodSpec in methodSpecs) {
                if (!methodSpec.isConstructor) continue
                if (!firstMember) codeWriter.emit("\n")
                methodSpec.emit(codeWriter, name!!, kind.implicitMethodModifiers)
                firstMember = false
            }

            // Methods (static and non-static).
            for (methodSpec in methodSpecs) {
                if (methodSpec.isConstructor) continue
                if (!firstMember) codeWriter.emit("\n")
                methodSpec.emit(codeWriter, name!!, kind.implicitMethodModifiers)
                firstMember = false
            }

            // Types.
            for (typeSpec in typeSpecs) {
                if (!firstMember) codeWriter.emit("\n")
                typeSpec.emit(codeWriter, null, kind.implicitTypeModifiers)
                firstMember = false
            }

            codeWriter.unindent()
            codeWriter.popType()

            codeWriter.emit("}")
            if (enumName == null && anonymousTypeArguments == null) {
                codeWriter.emit("\n") // If this type isn't also a value, include a trailing newline.
            }
        } finally {
            codeWriter.statementLine = previousStatementLine
        }
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
            emit(CodeWriter(out), null, emptySet<Modifier>())
            return out.toString()
        } catch (e: IOException) {
            throw AssertionError()
        }

    }

    enum class Kind constructor(internal val implicitFieldModifiers: Set<Modifier>,
                                internal val implicitMethodModifiers: Set<Modifier>,
                                internal val implicitTypeModifiers: Set<Modifier>,
                                internal val asMemberModifiers: Set<Modifier>) {
        CLASS(emptySet<Modifier>(),
                emptySet<Modifier>(),
                emptySet<Modifier>(),
                emptySet<Modifier>()),

        INTERFACE(setOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
                setOf(Modifier.PUBLIC, Modifier.ABSTRACT),
                setOf(Modifier.PUBLIC, Modifier.STATIC),
                setOf(Modifier.STATIC)),

        ENUM(emptySet<Modifier>(),
                emptySet<Modifier>(),
                emptySet<Modifier>(),
                setOf(Modifier.STATIC)),

        ANNOTATION(setOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
                setOf(Modifier.PUBLIC, Modifier.ABSTRACT),
                setOf(Modifier.PUBLIC, Modifier.STATIC),
                setOf(Modifier.STATIC))
    }

    class Builder constructor(internal val kind: Kind, internal val name: String?,
                              internal val anonymousTypeArguments: CodeBlock?) : Typeable<Builder> {

        internal val javadoc = CodeBlock.builder()
        internal val annotations = arrayListOf<AnnotationSpec>()
        internal val modifiers = arrayListOf<Modifier>()
        internal val typeVariables = arrayListOf<TypeVariableName>()
        internal var superclass: TypeName = ClassName.OBJECT
        internal val superinterfaces = arrayListOf<TypeName>()
        internal val enumConstants = linkedMapOf<String, TypeSpec>()
        internal val fieldSpecs = arrayListOf<FieldSpec>()
        internal val staticBlock = CodeBlock.builder()
        internal val initializerBlock = CodeBlock.builder()
        internal val methodSpecs = arrayListOf<MethodSpec>()
        internal val typeSpecs = arrayListOf<TypeSpec>()
        internal val originatingElements = arrayListOf<Element>()

        init {
            checkArgument(name == null || SourceVersion.isName(name), "not a valid name: $name")
        }

        override fun addJavadoc(format: String, vararg args: Any) = apply { javadoc.add(format, *args) }

        override fun addJavadoc(block: CodeBlock) = apply { javadoc.add(block) }

        override fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
            for (annotationSpec in annotationSpecs) {
                this.annotations.add(annotationSpec)
            }
        }

        override fun addAnnotation(annotationSpec: AnnotationSpec) = apply { this.annotations.add(annotationSpec) }

        override fun addAnnotation(className: ClassName)
                = addAnnotation(AnnotationSpec.builder(className).build())

        override fun addAnnotation(annotation: KClass<*>) = addAnnotation(ClassName[annotation])

        override fun addModifiers(vararg modifiers: Modifier) = apply {
            checkState(anonymousTypeArguments == null, "forbidden on anonymous types.")
            for (modifier in modifiers) {
                this.modifiers.add(modifier)
            }
        }

        override fun addModifiers(modifiers: Iterable<Modifier>) = apply {
            checkState(anonymousTypeArguments == null, "forbidden on anonymous types.")
            for (modifier in modifiers) {
                this.modifiers.add(modifier)
            }
        }


        override fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
            checkState(anonymousTypeArguments == null, "forbidden on anonymous types.")
            for (typeVariable in typeVariables) {
                this.typeVariables.add(typeVariable)
            }
        }

        override fun addTypeVariable(typeVariable: TypeVariableName) = apply {
            checkState(anonymousTypeArguments == null, "forbidden on anonymous types.")
            typeVariables.add(typeVariable)
        }

        fun superclass(superclass: TypeName) = apply {
            checkState(this.kind == Kind.CLASS, "only classes have super classes, not " + this.kind)
            checkState(this.superclass === ClassName.OBJECT,
                    "superclass already set to " + this.superclass)
            checkArgument(!superclass.isPrimitive, "superclass may not be a primitive")
            this.superclass = superclass
        }

        fun superclass(superclass: Type) = superclass(TypeName[superclass])

        fun addSuperinterfaces(superinterfaces: Iterable<TypeName>) = apply {
            for (superinterface in superinterfaces) {
                addSuperinterface(superinterface)
            }
        }

        fun addSuperinterface(superinterface: TypeName) = apply { this.superinterfaces.add(superinterface) }

        fun addSuperinterface(superinterface: Type) = addSuperinterface(TypeName[superinterface])

        @JvmOverloads fun addEnumConstant(name: String,
                                          typeSpec: TypeSpec = anonymousClassBuilder("").build())
                = apply {
            checkState(kind == Kind.ENUM, "${this.name} is not enum")
            checkArgument(typeSpec.anonymousTypeArguments != null,
                    "enum constants must have anonymous type arguments")
            checkArgument(SourceVersion.isName(name), "not a valid enum constant: $name")
            enumConstants.put(name, typeSpec)
        }

        fun addFields(fieldSpecs: Iterable<FieldSpec>) = apply {
            for (fieldSpec in fieldSpecs) {
                addField(fieldSpec)
            }
        }

        fun addField(fieldSpec: FieldSpec) = apply {
            if (kind == Kind.INTERFACE || kind == Kind.ANNOTATION) {
                requireExactlyOneOf(fieldSpec.modifiers, Modifier.PUBLIC, Modifier.PRIVATE)
                val check = EnumSet.of(Modifier.STATIC, Modifier.FINAL)
                checkState(fieldSpec.modifiers.containsAll(check),
                        "$kind $name.${fieldSpec.name} requires modifiers $check")
            }
            fieldSpecs.add(fieldSpec)
        }

        fun addField(type: TypeName, name: String, vararg modifiers: Modifier): Builder {
            return addField(FieldSpec.builder(type, name, *modifiers).build())
        }

        fun addField(type: Type, name: String, vararg modifiers: Modifier): Builder {
            return addField(TypeName[type], name, *modifiers)
        }

        fun addStaticBlock(block: CodeBlock) = apply {
            staticBlock.beginControlFlow("static").add(block).endControlFlow()
        }

        fun addInitializerBlock(block: CodeBlock) = apply {
            if (kind != Kind.CLASS && kind != Kind.ENUM) {
                throw UnsupportedOperationException(kind.toString() + " can't have initializer blocks")
            }
            initializerBlock.add("{\n")
                    .indent()
                    .add(block)
                    .unindent()
                    .add("}\n")
        }

        fun addMethods(methodSpecs: Iterable<MethodSpec>) = apply {
            for (methodSpec in methodSpecs!!) {
                addMethod(methodSpec)
            }
        }

        fun addMethod(methodSpec: MethodSpec) = apply {
            if (kind == Kind.INTERFACE) {
                requireExactlyOneOf(methodSpec.modifiers, Modifier.ABSTRACT, Modifier.STATIC, Util.DEFAULT)
                requireExactlyOneOf(methodSpec.modifiers, Modifier.PUBLIC, Modifier.PRIVATE)
            } else if (kind == Kind.ANNOTATION) {
                checkState(methodSpec.modifiers == kind.implicitMethodModifiers,
                        "$kind $name.${methodSpec.name} requires modifiers ${kind.implicitMethodModifiers}")
            }
            if (kind != Kind.ANNOTATION) {
                checkState(methodSpec.defaultValue == null, "$kind $name.${methodSpec.name} cannot have a default value")
            }
            if (kind != Kind.INTERFACE) {
                checkState(!hasDefaultModifier(methodSpec.modifiers),
                        "$kind $name.${methodSpec.name} cannot be default")
            }
            methodSpecs.add(methodSpec)
        }

        fun addTypes(typeSpecs: Iterable<TypeSpec>) = apply {
            for (typeSpec in typeSpecs) {
                addType(typeSpec)
            }
        }

        fun addType(typeSpec: TypeSpec) = apply {
            checkArgument(typeSpec.modifiers.containsAll(kind.implicitTypeModifiers),
                    "$kind $name.${typeSpec.name} requires modifiers ${kind.implicitTypeModifiers}")
            typeSpecs.add(typeSpec)
        }

        fun addOriginatingElement(originatingElement: Element) = apply { originatingElements.add(originatingElement) }

        fun build(): TypeSpec {
            checkArgument(kind != Kind.ENUM || !enumConstants.isEmpty(),
                    "at least one enum constant is required for $name")

            val isAbstract = modifiers.contains(Modifier.ABSTRACT) || kind != Kind.CLASS
            for (methodSpec in methodSpecs) {
                checkArgument(isAbstract || !methodSpec.hasModifier(Modifier.ABSTRACT),
                        "non-abstract type $name cannot declare abstract method ${methodSpec.name}")
            }

            val superclassIsObject = superclass == ClassName.OBJECT
            val interestingSupertypeCount = (if (superclassIsObject) 0 else 1) + superinterfaces.size
            checkArgument(anonymousTypeArguments == null || interestingSupertypeCount <= 1,
                    "anonymous type has too many supertypes")

            return TypeSpec(this)
        }
    }

    companion object {

        fun classBuilder(name: String) = Builder(Kind.CLASS, name, null)

        fun classBuilder(className: ClassName) = classBuilder(className.simpleName())

        fun interfaceBuilder(name: String) = Builder(Kind.INTERFACE, name, null)

        fun interfaceBuilder(className: ClassName) = interfaceBuilder(className.simpleName())

        fun enumBuilder(name: String) = Builder(Kind.ENUM, name, null)

        fun enumBuilder(className: ClassName) = enumBuilder(className.simpleName())

        fun anonymousClassBuilder(typeArgumentsFormat: String, vararg args: Any): Builder {
            return Builder(Kind.CLASS, null, CodeBlock.builder()
                    .add(typeArgumentsFormat, *args)
                    .build())
        }

        fun annotationBuilder(name: String) = Builder(Kind.ANNOTATION, name, null)

        fun annotationBuilder(className: ClassName) = annotationBuilder(className.simpleName())
    }
}
