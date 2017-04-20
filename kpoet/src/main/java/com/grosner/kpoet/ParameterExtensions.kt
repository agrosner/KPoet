package com.grosner.kpoet

import com.grosner.kpoet.core.AnnotationSpec
import com.grosner.kpoet.core.ClassName
import com.grosner.kpoet.core.ParameterSpec
import com.grosner.kpoet.core.TypeName
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun param(annotationFunction: ParamMethod, kClass: KClass<*>,
          name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(kClass, name).apply(annotationFunction).apply(paramMethod)

fun param(annotationFunction: ParamMethod, className: ClassName,
          name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(className, name).apply(annotationFunction).apply(paramMethod)

fun param(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>,
          name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(kClass, name).addAnnotation(annotationSpec.build()).apply(paramMethod)

fun param(annotationSpec: AnnotationSpec.Builder, className: ClassName,
          name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(className, name).addAnnotation(annotationSpec.build()).apply(paramMethod)

fun param(kClass: KClass<*>, name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(kClass, name).apply(paramMethod)

fun param(typeName: TypeName, name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(typeName, name).apply(paramMethod)

fun `final param`(annotationFunction: ParamMethod, kClass: KClass<*>,
                  name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(kClass, name).addModifiers(Modifier.FINAL)
        .apply(annotationFunction).apply(paramMethod)

fun `final param`(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>,
                  name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(kClass, name).addModifiers(Modifier.FINAL)
        .addAnnotation(annotationSpec.build()).apply(paramMethod)

fun `final param`(kClass: KClass<*>, name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(kClass, name).addModifiers(Modifier.FINAL).apply(paramMethod)

fun `final param`(typeName: TypeName, name: String, paramMethod: ParamMethod = { })
        = ParameterSpec.builder(typeName, name).addModifiers(Modifier.FINAL).apply(paramMethod)

fun ParameterSpec.Builder.`@`(kClass: KClass<*>, annotationMethod: AnnotationMethod = { })
        = addAnnotation(AnnotationSpec.builder(kClass).apply(annotationMethod).build())

fun ParameterSpec.Builder.`@`(className: ClassName, annotationMethod: AnnotationMethod = { })
        = addAnnotation(AnnotationSpec.builder(className).apply(annotationMethod).build())




