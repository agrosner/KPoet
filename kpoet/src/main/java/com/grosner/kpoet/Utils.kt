package com.grosner.kpoet

fun cargs(code: String, vararg args: Any?) = Args(code, args)

fun str(code: String) = cargs("\$S", code)

fun lit(arg: Any?) = cargs("\$L", arg)

class Args(val code: String, val args: Array<out Any?>)