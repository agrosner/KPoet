package com.grosner.kpoet

import com.grosner.kpoet.core.*
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

typealias CodeAbleMethod = CodeAble<*>.() -> CodeAble<*>
typealias CodeMethod = CodeBlock.Builder.() -> CodeBlock.Builder
typealias MethodMethod = MethodSpec.Builder.() -> MethodSpec.Builder
typealias FieldMethod = FieldSpec.Builder.() -> FieldSpec.Builder
typealias ParamMethod = ParameterSpec.Builder.() -> ParameterSpec.Builder
typealias AnnotationMethod = AnnotationSpec.Builder.() -> AnnotationSpec.Builder
typealias TypeMethod = TypeSpec.Builder.() -> TypeSpec.Builder


