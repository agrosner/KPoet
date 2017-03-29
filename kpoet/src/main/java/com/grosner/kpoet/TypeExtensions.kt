package com.grosner.kpoet

import com.squareup.javapoet.*
import java.lang.reflect.Type
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun TypeSpec.Builder.extends(typeName: TypeName) = superclass(typeName)!!

fun TypeSpec.Builder.extends(type: Type) = superclass(type)!!

fun TypeSpec.Builder.implements(vararg typeName: TypeName) = addSuperinterfaces(typeName.toList())!!

fun TypeSpec.Builder.implements(vararg type: Type) = apply { type.forEach { addSuperinterface(it) } }

fun TypeSpec.Builder.modifiers(vararg modifier: Modifier) = addModifiers(*modifier)!!

fun TypeSpec.Builder.modifiers(modifiers: Collection<Modifier>) = addModifiers(*modifiers.toTypedArray())!!

infix fun TypeSpec.Builder.annotation(type: KClass<*>) = addAnnotation(type.java)!!

infix fun TypeSpec.Builder.annotation(type: ClassName) = addAnnotation(type)!!

inline fun TypeSpec.Builder.annotation(className: ClassName,
                                       function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className).function().build())!!

inline fun TypeSpec.Builder.annotation(className: KClass<*>,
                                       function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className.java).function().build())!!

inline fun `class`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.classBuilder(className)).build()!!

inline fun `interface`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.interfaceBuilder(className)).build()!!

inline fun `abstract class`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.classBuilder(className)).addModifiers(Modifier.ABSTRACT).build()!!

inline fun `public class`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.classBuilder(className)).addModifiers(Modifier.PUBLIC).build()!!

inline fun `final class`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.classBuilder(className)).addModifiers(Modifier.FINAL).build()!!

inline fun `enum`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.enumBuilder(className)).build()!!

inline fun `anonymous class`(typeArgumentsFormat: String, vararg args: Any?,
                             typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.anonymousClassBuilder(typeArgumentsFormat, *args)).build()!!

inline fun `@interface`(className: String, typeSpecFunc: TypeMethod)
        = typeSpecFunc(TypeSpec.annotationBuilder(className)).build()!!

fun TypeSpec.Builder.constructor(vararg parameters: ParameterSpec.Builder,
                                 methodSpecFunction: MethodMethod = { this })
        = addMethod(methodSpecFunction(MethodSpec.constructorBuilder()).addParameters(parameters.map { it.build() }
        .toMutableList()).build())!!

fun TypeSpec.Builder.abstract(returnClass: ClassName, name: String,
                              vararg parameters: ParameterSpec.Builder,
                              methodSpecFunction: MethodMethod = { this })
        = addMethod(methodSpecFunction(MethodSpec.methodBuilder(name))
        .addModifiers(Modifier.ABSTRACT)
        .addParameters(parameters.map { it.build() }
                .toMutableList())
        .returns(returnClass)
        .build())!!

fun TypeSpec.Builder.abstract(returnType: KClass<*>, name: String,
                              vararg parameters: ParameterSpec.Builder,
                              methodSpecFunction: MethodMethod = { this })
        = addMethod(methodSpecFunction(MethodSpec.methodBuilder(name))
        .addModifiers(Modifier.ABSTRACT)
        .addParameters(parameters.map { it.build() }
                .toMutableList())
        .returns(returnType)
        .build())!!

fun TypeSpec.Builder.case(name: String) = addEnumConstant(name)!!

fun TypeSpec.Builder.case(name: String, function: TypeMethod) = addEnumConstant(name, function().build())!!

fun TypeSpec.Builder.case(name: String, parameter: String, vararg args: Any?, function: TypeMethod = { this })
        = addEnumConstant(name, TypeSpec.anonymousClassBuilder(parameter, *args).function().build())!!

