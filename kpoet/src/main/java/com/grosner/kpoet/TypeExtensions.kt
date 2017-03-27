package com.grosner.kpoet

import com.squareup.javapoet.*
import java.lang.reflect.Type
import javax.lang.model.element.Modifier

fun TypeSpec.Builder.extends(typeName: TypeName) = superclass(typeName)

fun TypeSpec.Builder.extends(type: Type) = superclass(type)

fun TypeSpec.Builder.implements(vararg typeName: TypeName) = addSuperinterfaces(typeName.toList())

fun TypeSpec.Builder.implements(vararg type: Type) = apply { type.forEach { addSuperinterface(it) } }

fun TypeSpec.Builder.modifiers(vararg modifier: Modifier) = addModifiers(*modifier)!!

fun TypeSpec.Builder.modifiers(modifiers: Collection<Modifier>) = addModifiers(*modifiers.toTypedArray())!!

fun `class`(className: String, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.classBuilder(className)).build()!!

fun JavaFile.Builder.`class`(className: ClassName, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.classBuilder(className)).build()!!

fun `interface`(className: String, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.interfaceBuilder(className)).build()!!

fun JavaFile.Builder.`interface`(className: ClassName, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.interfaceBuilder(className)).build()!!

fun `abstract class`(className: String, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.classBuilder(className)).addModifiers(Modifier.ABSTRACT).build()!!

fun JavaFile.Builder.`abstract class`(className: ClassName, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.classBuilder(className).addModifiers(Modifier.ABSTRACT)).build()!!

fun `final class`(className: String, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.classBuilder(className)).addModifiers(Modifier.FINAL).build()!!

fun JavaFile.Builder.`final class`(className: ClassName, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.classBuilder(className).addModifiers(Modifier.FINAL)).build()!!

fun `enum`(className: String, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.enumBuilder(className)).build()!!

fun JavaFile.Builder.`enum`(className: ClassName, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.enumBuilder(className)).build()!!

fun JavaFile.Builder.`anonymous class`(typeArgumentsFormat: String, vararg args: Any?,
                                       typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.anonymousClassBuilder(typeArgumentsFormat, *args)).build()!!

fun `@interface`(className: String, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.annotationBuilder(className)).build()!!

fun JavaFile.Builder.`@interface`(className: ClassName, typeSpecFunc: TypeSpec.Builder.() -> TypeSpec.Builder)
        = typeSpecFunc(TypeSpec.annotationBuilder(className)).build()!!

fun TypeSpec.Builder.method(methodSpec: MethodSpec.Builder,
                            vararg parameters: ParameterSpec.Builder,
                            methodSpecFunction: MethodSpec.Builder.() -> MethodSpec.Builder = { this })
        = addMethod(methodSpecFunction(methodSpec).addParameters(parameters.map { it.build() }.toList()).build())!!

fun TypeSpec.Builder.overrideMethod(methodSpec: MethodSpec.Builder,
                                    vararg parameters: ParameterSpec.Builder,
                                    methodSpecFunction: MethodSpec.Builder.() -> MethodSpec.Builder = { this })
        = addMethod(methodSpecFunction(methodSpec).addParameters(parameters.map { it.build() }.toList())
        .addAnnotation(Override::class.java)
        .build())!!

fun TypeSpec.Builder.constructor(vararg parameters: ParameterSpec.Builder,
                                 methodSpecFunction: MethodSpec.Builder.() -> MethodSpec.Builder = { this })
        = addMethod(methodSpecFunction(MethodSpec.constructorBuilder()).addParameters(parameters.map { it.build() }
        .toMutableList()).build())!!

fun TypeSpec.Builder.field(fieldSpec: FieldSpec.Builder,
                           fieldSpecFunction: FieldSpec.Builder.() -> FieldSpec.Builder = { this })
        = addField(fieldSpecFunction(fieldSpec).build())!!

