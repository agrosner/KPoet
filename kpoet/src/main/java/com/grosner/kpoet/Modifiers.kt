package com.grosner.kpoet

import com.squareup.javapoet.*
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.*
import kotlin.reflect.KClass

val packagePrivate
    get() = listOf<Modifier>()

val publicFinal
    get() = listOf(PUBLIC, FINAL)

val publicStatic
    get() = listOf(PUBLIC, STATIC)

val publicStaticFinal
    get() = listOf(PUBLIC, STATIC, FINAL)

val `public`
    get() = listOf(PUBLIC)

val `private`
    get() = listOf(PRIVATE)

val `privateFinal`
    get() = listOf(PRIVATE, FINAL)

val `privateStaticFinal`
    get() = listOf(PRIVATE, STATIC, FINAL)

val `protected`
    get() = listOf(PROTECTED)

fun `package private`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                      codeMethod: MethodMethod = { this })
        = applyParams(packagePrivate, type, name, params = *params, function = codeMethod)

fun `package private`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                      codeMethod: MethodMethod = { this })
        = applyParams(packagePrivate, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`package private`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                                       codeMethod: MethodMethod = { this })
        = addMethod(applyParams(packagePrivate, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`package private`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                                       codeMethod: MethodMethod = { this })
        = addMethod(applyParams(packagePrivate, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`package private field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(packagePrivate, type, name, codeMethod))!!

fun TypeSpec.Builder.`package private field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(packagePrivate, type, name, codeMethod))!!

fun `private`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
              codeMethod: MethodMethod = { this })
        = applyParams(private, type, name, params = *params, function = codeMethod)

fun `private`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
              codeMethod: MethodMethod = { this })
        = applyParams(private, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`private`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                               codeMethod: MethodMethod = { this })
        = addMethod(applyParams(private, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`private`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                               codeMethod: MethodMethod = { this })
        = addMethod(applyParams(private, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`private field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(private, type, name, codeMethod))!!

fun TypeSpec.Builder.`private field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(private, type, name, codeMethod))!!

fun `private final`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                    codeMethod: MethodMethod = { this })
        = applyParams(privateFinal, type, name, params = *params, function = codeMethod)

fun `private final`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                    codeMethod: MethodMethod = { this })
        = applyParams(privateFinal, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`private final`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                                     codeMethod: MethodMethod = { this })
        = addMethod(applyParams(privateFinal, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`private final`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                                     codeMethod: MethodMethod = { this })
        = addMethod(applyParams(privateFinal, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`private final field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(privateFinal, type, name, codeMethod))!!

fun TypeSpec.Builder.`private final field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(privateFinal, type, name, codeMethod))!!

fun `private static final`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                           codeMethod: MethodMethod = { this })
        = applyParams(privateStaticFinal, type, name, params = *params, function = codeMethod)

fun `private static final`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                           codeMethod: MethodMethod = { this })
        = applyParams(privateStaticFinal, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`private static final`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                                            codeMethod: MethodMethod = { this })
        = addMethod(applyParams(privateStaticFinal, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`private static final`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                                            codeMethod: MethodMethod = { this })
        = addMethod(applyParams(privateStaticFinal, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`private static final field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(privateStaticFinal, type, name, codeMethod))!!

fun TypeSpec.Builder.`private static final field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(privateStaticFinal, type, name, codeMethod))!!

fun `public static`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                    codeMethod: MethodMethod = { this })
        = applyParams(publicStatic, type, name, params = *params, function = codeMethod)

fun `public static`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                    codeMethod: MethodMethod = { this })
        = applyParams(publicStatic, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`public static`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                                     codeMethod: MethodMethod = { this })
        = addMethod(applyParams(publicStatic, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`public static`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                                     codeMethod: MethodMethod = { this })
        = addMethod(applyParams(publicStatic, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`public static field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(publicStatic, type, name, codeMethod))!!

fun TypeSpec.Builder.`public static field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(publicStatic, type, name, codeMethod))!!

fun `public static final`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                          codeMethod: MethodMethod = { this })
        = applyParams(publicStaticFinal, type, name, params = *params, function = codeMethod)

fun `public static final`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                          codeMethod: MethodMethod = { this })
        = applyParams(publicStaticFinal, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`public static final`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                                           codeMethod: MethodMethod = { this })
        = addMethod(applyParams(publicStaticFinal, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`public static final`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                                           codeMethod: MethodMethod = { this })
        = addMethod(applyParams(publicStaticFinal, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`public static final field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(publicStaticFinal, type, name, codeMethod))!!

fun TypeSpec.Builder.`public static final field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(publicStaticFinal, type, name, codeMethod))!!

fun `public`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
             codeMethod: MethodMethod = { this })
        = applyParams(public, type, name, params = *params, function = codeMethod)

fun `public`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
             codeMethod: MethodMethod = { this })
        = applyParams(public, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`public`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                              codeMethod: MethodMethod = { this })
        = addMethod(applyParams(public, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`public`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                              codeMethod: MethodMethod = { this })
        = addMethod(applyParams(public, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`public field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(public, type, name, codeMethod))!!

fun TypeSpec.Builder.`public field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(public, type, name, codeMethod))!!

fun `protected`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                codeMethod: MethodMethod = { this })
        = applyParams(protected, type, name, params = *params, function = codeMethod)

fun `protected`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                codeMethod: MethodMethod = { this })
        = applyParams(protected, type, name, params = *params, function = codeMethod)

fun TypeSpec.Builder.`protected`(type: KClass<*>, name: String, vararg params: ParameterSpec.Builder,
                                 codeMethod: MethodMethod = { this })
        = addMethod(applyParams(protected, type, name, params = *params, function = codeMethod))!!

fun TypeSpec.Builder.`protected`(type: TypeName, name: String, vararg params: ParameterSpec.Builder,
                                 codeMethod: MethodMethod = { this })
        = addMethod(applyParams(protected, type, name, params = *params, function = codeMethod))!!


fun TypeSpec.Builder.`protected field`(type: KClass<*>, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(protected, type, name, codeMethod))!!

fun TypeSpec.Builder.`protected field`(type: TypeName, name: String, codeMethod: FieldMethod = { this })
        = addField(applyFieldParams(protected, type, name, codeMethod))!!

private fun applyParams(modifiers: List<Modifier>,
                        type: TypeName,
                        name: String,
                        vararg params: ParameterSpec.Builder,
                        function: MethodMethod = { this })
        = MethodSpec.methodBuilder(name).addModifiers(*modifiers.toTypedArray())
        .returns(type).addParameters(params.map { it.build() }.toList())
        .function().build()!!

private fun applyParams(modifiers: List<Modifier>,
                        kClass: KClass<*>,
                        name: String,
                        vararg params: ParameterSpec.Builder,
                        function: MethodMethod = { this })
        = MethodSpec.methodBuilder(name).addModifiers(*modifiers.toTypedArray())
        .returns(kClass).addParameters(params.map { it.build() }.toList())
        .function().build()!!

private fun applyFieldParams(modifiers: List<Modifier>,
                             type: TypeName,
                             name: String,
                             function: FieldMethod = { this })
        = FieldSpec.builder(type, name).addModifiers(*modifiers.toTypedArray())
        .function().build()!!

private fun applyFieldParams(modifiers: List<Modifier>,
                             kClass: KClass<*>,
                             name: String,
                             function: FieldMethod = { this })
        = FieldSpec.builder(kClass.java, name).addModifiers(*modifiers.toTypedArray())
        .function().build()!!

