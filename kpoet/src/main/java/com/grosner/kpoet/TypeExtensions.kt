package com.grosner.kpoet

import com.grosner.kpoet.core.*
import java.lang.reflect.Type
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun TypeSpec.Builder.extends(typeName: TypeName) = superclass(typeName)

fun TypeSpec.Builder.extends(type: Type) = superclass(type)

fun TypeSpec.Builder.implements(vararg typeName: TypeName) = addSuperinterfaces(typeName.toList())

fun TypeSpec.Builder.implements(vararg type: Type) = apply { type.forEach { addSuperinterface(it) } }

inline fun `class`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.classBuilder(className).apply(typeSpecFunc).build()

inline fun `interface`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.interfaceBuilder(className).apply(typeSpecFunc).build()

inline fun `abstract class`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.classBuilder(className).apply(typeSpecFunc).modifiers(abstract).build()

inline fun `public abstract class`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.classBuilder(className).apply(typeSpecFunc).modifiers(public, abstract).build()

inline fun `public class`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.classBuilder(className).apply(typeSpecFunc).modifiers(public).build()

inline fun `public final class`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.classBuilder(className).apply(typeSpecFunc).modifiers(public, final).build()

inline fun `final class`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.classBuilder(className).apply(typeSpecFunc).modifiers(final).build()

inline fun `enum`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.enumBuilder(className).apply(typeSpecFunc).build()

inline fun `public enum`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.enumBuilder(className).apply(typeSpecFunc).modifiers(public).build()

inline fun `anonymous class`(typeArgumentsFormat: String, vararg args: Any?,
                             typeSpecFunc: TypeMethod)
        = TypeSpec.anonymousClassBuilder(typeArgumentsFormat, *args).apply(typeSpecFunc).build()

inline fun `@interface`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.annotationBuilder(className).apply(typeSpecFunc).build()

inline fun `public @interface`(className: String, typeSpecFunc: TypeMethod)
        = TypeSpec.annotationBuilder(className).apply(typeSpecFunc).modifiers(public).build()

fun TypeSpec.Builder.constructor(vararg parameters: ParameterSpec.Builder,
                                 methodSpecFunction: MethodMethod = { })
        = addMethod(MethodSpec.constructorBuilder().apply(methodSpecFunction)
        .addParameters(parameters.map { it.build() }
                .toMutableList()).build())

fun TypeSpec.Builder.abstract(returnClass: ClassName, name: String,
                              vararg parameters: ParameterSpec.Builder,
                              methodSpecFunction: MethodMethod = { })
        = addMethod(MethodSpec.methodBuilder(name).apply(methodSpecFunction)
        .modifiers(Modifier.ABSTRACT)
        .addParameters(parameters.map { it.build() }
                .toMutableList())
        .returns(returnClass)
        .build())

fun TypeSpec.Builder.abstract(returnType: KClass<*>, name: String,
                              vararg parameters: ParameterSpec.Builder,
                              methodSpecFunction: MethodMethod = { })
        = addMethod(MethodSpec.methodBuilder(name).apply(methodSpecFunction)
        .modifiers(Modifier.ABSTRACT)
        .addParameters(parameters.map { it.build() }
                .toMutableList())
        .returns(returnType)
        .build())

fun TypeSpec.Builder.case(name: String) = addEnumConstant(name)

fun TypeSpec.Builder.case(name: String, function: TypeMethod)
        = addEnumConstant(name, apply(function).build())

fun TypeSpec.Builder.case(name: String, parameter: String, vararg args: Any?, function: TypeMethod = { })
        = addEnumConstant(name, TypeSpec.anonymousClassBuilder(parameter, *args).apply(function).build())

fun TypeSpec.Builder.javadoc(doc: String, vararg args: Any?) = addJavadoc(doc, args)

fun TypeSpec.Builder.javadoc(codeBlock: CodeBlock) = addJavadoc(codeBlock)


