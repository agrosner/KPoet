package com.grosner.kpoet

import com.grosner.kpoet.core.ClassName
import com.grosner.kpoet.core.JavaFile
import com.grosner.kpoet.core.TypeSpec
import kotlin.reflect.KClass

fun javaFile(packageName: String, imports: JavaFile.Builder.() -> JavaFile.Builder = { this },
             function: () -> TypeSpec) = JavaFile.builder(packageName, function()).imports().build()

fun JavaFile.Builder.`import static`(kClass: KClass<*>, vararg names: String) = addStaticImport(kClass, *names)

fun JavaFile.Builder.`import static`(className: ClassName, vararg names: String) = addStaticImport(className, *names)

fun <T : Enum<T>> JavaFile.Builder.`import static`(enum: Enum<T>) = addStaticImport(enum)


