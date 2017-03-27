package com.grosner.kpoet

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeName
import kotlin.reflect.KClass

infix fun TypeName.fieldNamed(name: String) = FieldSpec.builder(this, name)!!

infix fun <T : Any> KClass<T>.fieldNamed(name: String) = FieldSpec.builder(this.java, name)!!

infix fun FieldSpec.Builder.init(codeFunc: CodeBlock.Builder.() -> CodeBlock.Builder)
        = initializer(codeFunc(CodeBlock.builder()).build())!!

infix fun FieldSpec.Builder.init(code: String)
        = initializer(CodeBlock.builder().add(code).build())!!

infix fun FieldSpec.Builder.init(code: Args)
        = initializer(CodeBlock.builder().add(code.code, *code.args).build())!!