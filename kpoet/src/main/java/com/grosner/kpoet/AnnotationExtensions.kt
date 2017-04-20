package com.grosner.kpoet

import com.grosner.kpoet.core.AnnotationSpec
import com.grosner.kpoet.core.ClassName
import com.grosner.kpoet.core.CodeBlock
import kotlin.reflect.KClass

fun AnnotationSpec.Builder.member(name: String, format: String, vararg args: Any?) = addMember(name, format, *args)

fun AnnotationSpec.Builder.member(name: String, codeBlock: CodeBlock) = addMember(name, codeBlock)

fun AnnotationSpec.Builder.member(name: String, codeBlockFunc: CodeBlock.Builder.() -> CodeBlock.Builder)
        = addMember(name, CodeBlock.builder().codeBlockFunc().build())


fun `@`(kClass: KClass<*>, mapFunc: MutableMap<String, String>.() -> Unit = { })
        = AnnotationSpec.builder(kClass)
        .apply {
            mutableMapOf<String, String>().apply { mapFunc(this) }
                    .forEach { key, value -> addMember(key, value) }
        }


fun `@`(className: ClassName, mapFunc: MutableMap<String, String>.() -> Unit = {})
        = AnnotationSpec.builder(className)
        .apply {
            mutableMapOf<String, String>().apply { mapFunc(this) }
                    .forEach { key, value -> addMember(key, value) }
        }