package com.grosner.kpoet

import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.*

val packagePrivate
    get() = listOf<Modifier>()

val publicFinal
    get() = listOf(PUBLIC, FINAL)

val publicStatic
    get() = listOf(PUBLIC, STATIC)

val publicStaticFinal
    get() = listOf(PUBLIC, STATIC, FINAL)

val `public`
    get() = listOf(PUBLIC)

val `private`
    get() = listOf(PRIVATE)

val `privateFinal`
    get() = listOf(PRIVATE, FINAL)

val `privateStaticFinal`
    get() = listOf(PRIVATE, STATIC, FINAL)

val `protected`
    get() = listOf(PROTECTED)