package com.grosner.kpoet.core

/**
 * Description:
 */
interface CodeAble<out Builder : CodeAble<Builder>> {

    fun beginControlFlow(controlFlow: String, vararg args: Any?): Builder

    fun nextControlFlow(controlFlow: String, vararg args: Any?): Builder

    fun endControlFlow(controlFlow: String, vararg args: Any?): Builder

    fun endControlFlow(): Builder

    fun addStatement(format: String, vararg args: Any?): Builder

    fun add(codeBlock: CodeBlock): Builder

    fun add(format: String, vararg args: Any?): Builder

    fun addNamed(format: String, args: Map<String, *>): Builder

    fun addComment(format: String, vararg args: Any?): Builder
}
