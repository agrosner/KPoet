package com.grosner.kpoet

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec

inline fun javaFile(packageName: String, function: () -> TypeSpec) = JavaFile.builder(packageName, function()).build()!!