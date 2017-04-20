package com.grosner.kpoet

import com.grosner.kpoet.core.ParameterizedTypeName
import com.grosner.kpoet.core.TypeName
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

val <T : Any> KClass<T>.typeName
    get() = TypeName[this.java]

val TypeMirror.typeName
    get() = TypeName[this]

inline fun <reified T : Any> parameterized(kClass: KClass<*>) = ParameterizedTypeName[kClass, T::class]

inline fun <reified T1 : Any, reified T2 : Any> parameterized2(kClass: KClass<*>)
        = ParameterizedTypeName[kClass, T1::class, T2::class]

inline fun <reified T1 : Any, reified T2 : Any, reified T3 : Any> parameterized3(kClass: KClass<*>)
        = ParameterizedTypeName[kClass, T1::class, T2::class, T3::class]!!

