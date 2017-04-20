package com.grosner.kpoet

import com.grosner.kpoet.core.AnnotationSpec
import com.grosner.kpoet.core.ClassName
import com.grosner.kpoet.core.ParameterSpec
import com.grosner.kpoet.core.TypeName
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun param(annotationFunction: ParamMethod, kClass: KClass<*>, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(kClass, name).annotationFunction().paramMethod()

fun param(annotationFunction: ParamMethod, className: ClassName, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(className, name).annotationFunction().paramMethod()

fun param(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(kClass, name).addAnnotation(annotationSpec.build()).paramMethod()

fun param(annotationSpec: AnnotationSpec.Builder, className: ClassName, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(className, name).addAnnotation(annotationSpec.build()).paramMethod()

fun param(kClass: KClass<*>, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(kClass, name).paramMethod()

fun param(typeName: TypeName, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(typeName, name).paramMethod()

fun `final param`(annotationFunction: ParamMethod, kClass: KClass<*>, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(kClass, name).addModifiers(Modifier.FINAL).annotationFunction().paramMethod()

fun `final param`(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>,
                  name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(kClass, name).addModifiers(Modifier.FINAL)
        .addAnnotation(annotationSpec.build()).paramMethod()

fun `final param`(kClass: KClass<*>, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(kClass, name).addModifiers(Modifier.FINAL).paramMethod()

fun `final param`(typeName: TypeName, name: String, paramMethod: ParamMethod = { this })
        = ParameterSpec.builder(typeName, name).addModifiers(Modifier.FINAL).paramMethod()

fun ParameterSpec.Builder.`@`(kClass: KClass<*>, annotationMethod: AnnotationMethod = { this })
        = addAnnotation(AnnotationSpec.builder(kClass).annotationMethod().build())!!

fun ParameterSpec.Builder.`@`(className: ClassName, annotationMethod: AnnotationMethod = { this })
        = addAnnotation(AnnotationSpec.builder(className).annotationMethod().build())!!




