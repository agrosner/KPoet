package com.grosner.kpoet

import com.grosner.kpoet.core.*
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

typealias CodeAbleMethod = CodeAble<*>.() -> Unit
typealias CodeMethod = CodeBlock.Builder.() -> Unit
typealias MethodMethod = MethodSpec.Builder.() -> Unit
typealias FieldMethod = FieldSpec.Builder.() -> Unit
typealias ParamMethod = ParameterSpec.Builder.() -> Unit
typealias AnnotationMethod = AnnotationSpec.Builder.() -> Unit
typealias TypeMethod = TypeSpec.Builder.() -> Unit


