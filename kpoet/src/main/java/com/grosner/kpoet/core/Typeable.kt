package com.grosner.kpoet.core

import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

interface Typeable<out Builder : Typeable<Builder>> {

    fun addJavadoc(format: String, vararg args: Any): Builder

    fun addJavadoc(block: CodeBlock): Builder

    fun addAnnotation(annotationSpec: AnnotationSpec): Builder

    fun addAnnotation(className: ClassName): Builder

    fun addAnnotation(annotation: KClass<*>): Builder

    fun modifiers(vararg modifiers: Modifier): Builder

    fun modifiers(modifiers: Iterable<Modifier>): Builder

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder

    fun addTypeVariable(typeVariable: TypeVariableName): Builder
}
