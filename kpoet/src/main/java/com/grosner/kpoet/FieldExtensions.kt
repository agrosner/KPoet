package com.grosner.kpoet

import com.grosner.kpoet.core.*
import kotlin.reflect.KClass

fun field(typeName: TypeName, name: String, fieldMethod: FieldMethod = {})
        = FieldSpec.builder(typeName, name).apply(fieldMethod)

fun field(kClass: KClass<*>, name: String, fieldMethod: FieldMethod = { })
        = FieldSpec.builder(kClass, name).apply(fieldMethod)

fun field(annotationSpec: AnnotationSpec.Builder, typeName: TypeName, name: String, fieldMethod: FieldMethod = { })
        = FieldSpec.builder(typeName, name).addAnnotation(annotationSpec.build()).apply(fieldMethod)

fun field(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>, name: String, fieldMethod: FieldMethod = { })
        = FieldSpec.builder(kClass, name).addAnnotation(annotationSpec.build()).apply(fieldMethod)

fun field(annotationFunction: FieldMethod, kClass: KClass<*>, name: String, fieldMethod: FieldMethod = { })
        = FieldSpec.builder(kClass, name).apply(annotationFunction).apply(fieldMethod)

fun field(annotationFunction: FieldMethod, className: ClassName, name: String, fieldMethod: FieldMethod = { })
        = FieldSpec.builder(className, name).apply(annotationFunction).apply(fieldMethod)

infix inline fun FieldSpec.Builder.`=`(codeFunc: CodeMethod)
        = initializer(CodeBlock.builder().apply(codeFunc).build())

fun FieldSpec.Builder.`=`(code: String, vararg args: Any?)
        = initializer(CodeBlock.builder().add(code, *args).build())

fun FieldSpec.Builder.`=`(codeFunc: CodeMethod,
                          fieldMethod: FieldMethod)
        = initializer(CodeBlock.builder().apply(codeFunc).build()).apply(fieldMethod)

fun FieldSpec.Builder.`=`(code: String, vararg args: Any?, fieldMethod: FieldMethod)
        = initializer(CodeBlock.builder().add(code, *args).build()).fieldMethod()

fun FieldSpec.Builder.`@`(kClass: KClass<*>, annotationMethod: AnnotationMethod = { })
        = addAnnotation(AnnotationSpec.builder(kClass).apply(annotationMethod).build())

fun FieldSpec.Builder.`@`(className: ClassName, annotationMethod: AnnotationMethod = { })
        = addAnnotation(AnnotationSpec.builder(className).apply(annotationMethod).build())

fun FieldSpec.Builder.javadoc(doc: String, vararg args: Any?) = addJavadoc(doc, args)

fun FieldSpec.Builder.javadoc(codeBlock: CodeBlock) = addJavadoc(codeBlock)