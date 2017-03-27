package com.grosner.kpoet

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun method(methodSpec: MethodSpec.Builder,
           vararg parameters: ParameterSpec.Builder,
           methodSpecFunction: MethodSpec.Builder.() -> MethodSpec.Builder = { this })
        = methodSpecFunction(methodSpec).addParameters(parameters.map { it.build() }.toList())

fun overrideMethod(methodSpec: MethodSpec.Builder,
                   vararg parameters: ParameterSpec.Builder,
                   methodSpecFunction: MethodSpec.Builder.() -> MethodSpec.Builder = { this })
        = methodSpecFunction(methodSpec).addParameters(parameters.map { it.build() }.toList())
        .addAnnotation(Override::class.java)!!

infix fun List<Modifier>.methodNamed(name: String) = MethodSpec.methodBuilder(name).addModifiers(this)!!

infix fun MethodSpec.Builder.returns(typeName: TypeName) = returns(typeName)!!

infix fun MethodSpec.Builder.returns(typeName: KClass<*>) = returns(typeName.java)!!

fun MethodSpec.Builder.code(codeMethod: CodeBlock.Builder.() -> CodeBlock.Builder) = addCode(codeMethod(CodeBlock.builder()).build())!!

fun MethodSpec.Builder.statement(codeMethod: CodeBlock.Builder.() -> CodeBlock.Builder)
        = addStatement("\$L", codeMethod(CodeBlock.builder()).build().toString())!!

fun MethodSpec.Builder.statement(arg: Args) = addStatement(arg.code, *arg.args)!!

fun MethodSpec.Builder.statement(code: String, vararg args: Any?) = addStatement(code, *args)!!

fun MethodSpec.Builder.comment(comment: String) = addComment(comment)!!


// control flow extensions
fun MethodSpec.Builder.`if`(statement: String, vararg args: Any?,
                            function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("if", statement = statement, args = *args, function = function)

fun MethodSpec.Builder.`do`(statement: String, vararg args: Any?,
                            function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("do", statement = statement, args = *args, function = function)

fun MethodSpec.Builder.`while`(statement: String, vararg args: Any?) = endControl("while", statement = statement, args = *args)

fun MethodSpec.Builder.`while`(arg: Args) = endControl("while", arg = arg)

infix fun MethodSpec.Builder.`else`(function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = nextControl("else", function = function)

fun MethodSpec.Builder.`else if`(statement: String, vararg args: Any?,
                                 function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = nextControl("else if", statement = statement, args = *args, function = function)

fun MethodSpec.Builder.`for`(statement: String, vararg args: Any?,
                             function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("for", statement = statement, args = *args, function = function).endControlFlow()!!

fun MethodSpec.Builder.`switch`(statement: String, vararg args: Any?,
                                function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControl("switch", statement = statement, args = *args, function = function).endControlFlow()!!

fun MethodSpec.Builder.`return`(statement: String, vararg args: Any?) = addStatement("return $statement", *args)!!

fun MethodSpec.Builder.`return`(arg: Args) = addStatement("return ${arg.code}", *arg.args)!!

fun MethodSpec.Builder.`break`() = addStatement("break")!!

fun MethodSpec.Builder.`continue`() = addStatement("continue")!!

private fun MethodSpec.Builder.nextControl(name: String, statement: String = "", vararg args: Any?,
                                           function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = nextControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .addCode(function(CodeBlock.builder()).build())!!

private fun MethodSpec.Builder.beginControl(name: String, statement: String = "", vararg args: Any?,
                                            function: CodeBlock.Builder.() -> CodeBlock.Builder)
        = beginControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .addCode(function(CodeBlock.builder()).build())!!

private fun MethodSpec.Builder.endControl(name: String, statement: String = "", vararg args: Any?)
        = endControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)!!

private fun MethodSpec.Builder.endControl(name: String, arg: Args)
        = endControlFlow("$name${if (arg.code.isNullOrEmpty()) "" else " (${arg.code})"}", *arg.args)!!