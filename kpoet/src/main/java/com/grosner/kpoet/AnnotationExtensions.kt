package com.grosner.kpoet

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock

fun AnnotationSpec.Builder.member(name: String, format: String, vararg args: Any?) = addMember(name, format, *args)

fun AnnotationSpec.Builder.member(name: String, codeBlock: CodeBlock) = addMember(name, codeBlock)

fun AnnotationSpec.Builder.member(name: String, codeBlockFunc: CodeBlock.Builder.() -> CodeBlock.Builder)
        = addMember(name, CodeBlock.builder().codeBlockFunc().build())