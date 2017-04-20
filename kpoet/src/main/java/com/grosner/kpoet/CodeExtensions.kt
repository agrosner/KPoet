package com.grosner.kpoet

import com.grosner.kpoet.core.CodeBlock

inline fun code(codeMethod: CodeMethod) = CodeBlock.builder().apply(codeMethod).build()
