package com.grosner.kpoet

import com.grosner.kpoet.gen.ClassName
import com.grosner.kpoet.gen.JavaFile
import com.grosner.kpoet.gen.TypeSpec
import kotlin.reflect.KClass

fun javaFile(packageName: String, imports: JavaFile.Builder.() -> JavaFile.Builder = { this },
             function: () -> TypeSpec) = JavaFile.builder(packageName, function()).imports().build()!!

fun JavaFile.Builder.`import static`(kClass: KClass<*>, vararg names: String) = addStaticImport(kClass.java, *names)

fun JavaFile.Builder.`import static`(className: ClassName, vararg names: String) = addStaticImport(className, *names)

fun JavaFile.Builder.`import static`(enum: Enum<*>) = addStaticImport(enum)


