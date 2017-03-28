package com.grosner.kpoet

import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import kotlin.reflect.KClass

infix fun TypeName.paramNamed(name: String) = ParameterSpec.builder(this, name)!!

infix fun <T : Any> KClass<T>.paramNamed(name: String) = ParameterSpec.builder(this.java, name)!!

fun param(kClass: KClass<*>, name: String) = ParameterSpec.builder(kClass.java, name)!!

fun param(typeName: TypeName, name: String) = ParameterSpec.builder(typeName, name)!!