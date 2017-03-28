package com.grosner.kpoet

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

fun param(annotationFunction: ParamMethod, kClass: KClass<*>, name: String)
        = ParameterSpec.builder(kClass.java, name)!!.annotationFunction()

fun param(annotationFunction: ParamMethod, className: ClassName, name: String)
        = ParameterSpec.builder(className, name)!!.annotationFunction()

fun param(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>, name: String)
        = ParameterSpec.builder(kClass.java, name).addAnnotation(annotationSpec.build())!!

fun param(annotationSpec: AnnotationSpec.Builder, className: ClassName, name: String)
        = ParameterSpec.builder(className, name).addAnnotation(annotationSpec.build())!!

fun param(kClass: KClass<*>, name: String) = ParameterSpec.builder(kClass.java, name)!!

fun param(typeName: TypeName, name: String) = ParameterSpec.builder(typeName, name)!!

fun `final param`(annotationFunction: ParamMethod, kClass: KClass<*>, name: String)
        = ParameterSpec.builder(kClass.java, name).addModifiers(Modifier.FINAL)!!.annotationFunction()

fun `final param`(annotationSpec: AnnotationSpec.Builder, kClass: KClass<*>, name: String)
        = ParameterSpec.builder(kClass.java, name).addModifiers(Modifier.FINAL)
        .addAnnotation(annotationSpec.build())!!

fun `final param`(kClass: KClass<*>, name: String)
        = ParameterSpec.builder(kClass.java, name).addModifiers(Modifier.FINAL)!!

fun `final param`(typeName: TypeName, name: String)
        = ParameterSpec.builder(typeName, name).addModifiers(Modifier.FINAL)!!

fun ParameterSpec.Builder.`@`(kClass: KClass<*>, annotationMethod: AnnotationMethod = { this })
        = addAnnotation(AnnotationSpec.builder(kClass.java).annotationMethod().build())!!

fun ParameterSpec.Builder.`@`(className: ClassName, annotationMethod: AnnotationMethod = { this })
        = addAnnotation(AnnotationSpec.builder(className).annotationMethod().build())!!




