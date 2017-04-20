package com.grosner.kpoet

import com.grosner.kpoet.core.ClassName
import com.grosner.kpoet.core.CodeAble
import com.grosner.kpoet.core.CodeBlock
import kotlin.reflect.KClass

inline fun CodeAble<*>.code(codeMethod: CodeMethod) = add(codeMethod(CodeBlock
        .builder()).build())

inline fun CodeAble<*>.case(statement: String, vararg args: Any, function: CodeAbleMethod)
        = beginControlFlow("case $statement:", args).function().endControlFlow()

inline fun CodeAble<*>.default(function: CodeAbleMethod)
        = beginControlFlow("default:").function().endControlFlow()

inline fun CodeAble<*>.statement(codeMethod: CodeMethod)
        = addStatement(CodeBlock.builder().codeMethod().build().L)

fun CodeAble<*>.statement(code: String, vararg args: Any?) = addStatement(code, *args)

fun CodeAble<*>.comment(format: String, vararg args: Any?) = addComment(format, args)

// control flow extensions
inline fun CodeAble<*>.`if`(statement: String, vararg args: Any?,
                            function: CodeMethod)
        = beginControl("if", statement = statement, args = *args, function = function)

inline fun CodeAble<*>.`do`(function: CodeMethod)
        = beginControl("do", function = function)

fun CodeAble<*>.`while`(statement: String, vararg args: Any?)
        = endControl("while", statement = statement, args = *args)

inline infix fun CodeAble<*>.`else`(function: CodeMethod)
        = nextControl("else", function = function).end()

inline fun CodeAble<*>.`else if`(statement: String, vararg args: Any?,
                                 function: CodeMethod)
        = nextControl("else if", statement = statement, args = *args, function = function)

inline fun CodeAble<*>.`for`(statement: String, vararg args: Any?,
                             function: CodeMethod)
        = beginControl("for", statement = statement, args = *args, function = function).endControlFlow()!!

inline fun CodeAble<*>.`switch`(statement: String, vararg args: Any?,
                                function: CodeMethod)
        = beginControl("switch", statement = statement, args = *args, function = function).endControlFlow()!!

fun CodeAble<*>.`return`(statement: String, vararg args: Any?) = addStatement("return $statement", *args)!!

fun CodeAble<*>.`break`() = addStatement("break")

fun CodeAble<*>.`continue`() = addStatement("continue")

fun CodeAble<*>.`throw new`(type: KClass<*>, statement: String, vararg arg: Any?)
        = addStatement("throw new \$T(\"$statement\")", type, *arg)

fun CodeAble<*>.`throw new`(type: ClassName, statement: String, vararg arg: Any?)
        = addStatement("throw new \$T(\"$statement\")", type, *arg)

inline fun CodeAble<*>.nextControl(name: String, statement: String = "", vararg args: Any?,
                                   function: CodeMethod)
        = nextControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .add(function(CodeBlock.builder()).build())

inline fun CodeAble<*>.beginControl(name: String, statement: String = "", vararg args: Any?,
                                    function: CodeMethod)
        = beginControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .add(function(CodeBlock.builder()).build())

inline fun CodeAble<*>.endControl(name: String, statement: String = "", vararg args: Any?)
        = endControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)

fun CodeAble<*>.end(statement: String = "", vararg args: Any?)
        = (if (statement.isNullOrBlank().not()) endControlFlow(statement, *args) else endControlFlow())