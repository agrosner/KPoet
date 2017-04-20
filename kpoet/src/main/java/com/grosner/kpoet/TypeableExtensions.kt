package com.grosner.kpoet

import com.grosner.kpoet.core.AnnotationSpec
import com.grosner.kpoet.core.ClassName
import com.grosner.kpoet.core.CodeBlock
import com.grosner.kpoet.core.Typeable
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun Typeable<*>.modifiers(vararg modifiers: List<Modifier>) = apply { modifiers.forEach { modifiers(it) } }

inline fun Typeable<*>.annotation(className: ClassName,
                                  function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className).function().build())

inline fun Typeable<*>.annotation(className: KClass<*>,
                                  function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className).function().build())

fun Typeable<*>.annotation(annotationSpec: AnnotationSpec) = addAnnotation(annotationSpec)

fun Typeable<*>.annotation(className: ClassName) = addAnnotation(className)

fun Typeable<*>.annotation(annotation: KClass<*>) = addAnnotation(annotation)

fun Typeable<*>.`@`(kClass: KClass<*>, annotationMethod: AnnotationMethod = { this })
        = addAnnotation(AnnotationSpec.builder(kClass).annotationMethod().build())

fun Typeable<*>.`@`(className: ClassName, annotationMethod: AnnotationMethod = { this })
        = addAnnotation(AnnotationSpec.builder(className).annotationMethod().build())

fun Typeable<*>.javadoc(format: String, vararg args: Any?) = addJavadoc(format, args)

fun Typeable<*>.javadoc(codeBlock: CodeBlock) = addJavadoc(codeBlock)