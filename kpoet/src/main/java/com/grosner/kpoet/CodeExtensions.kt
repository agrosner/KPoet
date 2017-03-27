package com.grosner.kpoet

import com.squareup.javapoet.CodeBlock

fun CodeBlock.Builder.case(statement: String, vararg args: Any, function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControlFlow("case $statement:", args).function().endControlFlow()!!

fun CodeBlock.Builder.case(arg: Args, function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControlFlow("case ${arg.code}:", *arg.args).function().endControlFlow()!!

fun CodeBlock.Builder.default(function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControlFlow("default:").function().endControlFlow()!!

fun CodeBlock.Builder.code(codeMethod: CodeBlock.Builder.() -> CodeBlock.Builder) = add(codeMethod(CodeBlock.builder())
        .build())!!

fun CodeBlock.Builder.statement(codeMethod: CodeBlock.Builder.() -> CodeBlock.Builder)
        = addStatement("\$L", codeMethod(CodeBlock.builder()).build().toString())!!

fun CodeBlock.Builder.statement(arg: Args) = addStatement(arg.code, *arg.args)!!

fun CodeBlock.Builder.statement(code: String, vararg args: Any?) = addStatement(code, *args)!!

// control flow extensions
fun CodeBlock.Builder.`if`(statement: String, vararg args: Any?,
                           function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("if", statement = statement, args = *args, function = function)

fun CodeBlock.Builder.`do`(statement: String, vararg args: Any?,
                           function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("do", statement = statement, args = *args, function = function)

fun CodeBlock.Builder.`while`(statement: String, vararg args: Any?) = endControl("while", statement = statement, args = *args)

fun CodeBlock.Builder.`while`(arg: Args) = endControl("while", arg = arg)

infix fun CodeBlock.Builder.`else`(function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = nextControl("else", function = function)

fun CodeBlock.Builder.`else if`(statement: String, vararg args: Any?,
                                function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = nextControl("else if", statement = statement, args = *args, function = function)

fun CodeBlock.Builder.`for`(statement: String, vararg args: Any?,
                            function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("for", statement = statement, args = *args, function = function).endControlFlow()!!

fun CodeBlock.Builder.`switch`(statement: String, vararg args: Any?,
                               function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("switch", statement = statement, args = *args, function = function).endControlFlow()!!

fun CodeBlock.Builder.`return`(statement: String, vararg args: Any?) = addStatement("return $statement", *args)!!

fun CodeBlock.Builder.`return`(arg: Args) = addStatement("return ${arg.code}", *arg.args)!!

fun CodeBlock.Builder.`break`() = addStatement("break")!!

fun CodeBlock.Builder.`continue`() = addStatement("continue")!!

private fun CodeBlock.Builder.nextControl(name: String, statement: String = "", vararg args: Any?,
                                          function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = nextControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .add(function(CodeBlock.builder()).build())!!

private fun CodeBlock.Builder.beginControl(name: String, statement: String = "", vararg args: Any?,
                                           function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .add(function(CodeBlock.builder()).build())!!

private fun CodeBlock.Builder.endControl(name: String, statement: String = "", vararg args: Any?)
        = endControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)!!

private fun CodeBlock.Builder.endControl(name: String, arg: Args)
        = endControlFlow("$name${if (arg.code.isNullOrEmpty()) "" else " (${arg.code})"}", *arg.args)!!