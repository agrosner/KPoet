package com.grosner.kpoet

import com.squareup.javapoet.*
import kotlin.reflect.KClass

infix fun TypeName.fieldNamed(name: String) = FieldSpec.builder(this, name)!!

infix fun <T : Any> KClass<T>.fieldNamed(name: String) = FieldSpec.builder(this.java, name)!!

infix inline fun FieldSpec.Builder.init(codeFunc: CodeBlock.Builder.() -> CodeBlock.Builder)
        = initializer(codeFunc(CodeBlock.builder()).build())!!

infix fun FieldSpec.Builder.init(code: String)
        = initializer(CodeBlock.builder().add(code).build())!!

infix fun FieldSpec.Builder.init(code: Args)
        = initializer(CodeBlock.builder().add(code.code, *code.args).build())!!

infix fun FieldSpec.Builder.annotation(type: KClass<*>) = addAnnotation(type.java)!!

infix fun FieldSpec.Builder.annotation(type: ClassName) = addAnnotation(type)!!

inline fun FieldSpec.Builder.annotation(className: ClassName,
                                        function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className).function().build())!!

inline fun FieldSpec.Builder.annotation(className: KClass<*>,
                                        function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className.java).function().build())!!