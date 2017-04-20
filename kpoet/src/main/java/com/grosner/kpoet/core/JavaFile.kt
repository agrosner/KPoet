/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grosner.kpoet.core

import com.grosner.kpoet.core.Util.checkArgument
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject
import kotlin.reflect.KClass

/** A Java file containing a single top level class.  */
class JavaFile private constructor(builder: JavaFile.Builder) {

    val fileComment = builder.fileComment.build()
    val packageName = builder.packageName
    val typeSpec = builder.typeSpec
    val skipJavaLangImports = builder.skipJavaLangImports
    private val staticImports = builder.staticImports.toSet()
    private val indent = builder.indent

    @Throws(IOException::class)
    fun writeTo(out: Appendable) {
        // First pass: emit the entire class, just to collect the types we'll need to import.
        val importsCollector = CodeWriter(NULL_APPENDABLE, indent, staticImports)
        emit(importsCollector)
        val suggestedImports = importsCollector.suggestedImports()

        // Second pass: write the code, taking advantage of the imports.
        val codeWriter = CodeWriter(out, indent, suggestedImports, staticImports)
        emit(codeWriter)
    }

    /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
    @Throws(IOException::class)
    fun writeTo(directory: Path) {
        checkArgument(Files.notExists(directory) || Files.isDirectory(directory),
                "path %s exists but is not a directory.", directory)
        var outputDirectory = directory
        if (!packageName.isEmpty()) {
            for (packageComponent in packageName.split("\\.".toRegex())
                    .dropLastWhile(String::isEmpty).toTypedArray()) {
                outputDirectory = outputDirectory.resolve(packageComponent)
            }
            Files.createDirectories(outputDirectory)
        }

        val outputPath = outputDirectory.resolve("${typeSpec.name}.java")
        OutputStreamWriter(Files.newOutputStream(outputPath), UTF_8).use { writer -> writeTo(writer) }
    }

    /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
    @Throws(IOException::class)
    fun writeTo(directory: File) = writeTo(directory.toPath())

    /** Writes this to `filer`.  */
    @Throws(IOException::class)
    fun writeTo(filer: Filer) {
        val fileName = if (packageName.isEmpty()) typeSpec.name else "$packageName.${typeSpec.name}"
        val originatingElements = typeSpec.originatingElements
        val filerSourceFile = filer.createSourceFile(fileName,
                *originatingElements.toTypedArray())
        try {
            filerSourceFile.openWriter().use { writer -> writeTo(writer) }
        } catch (e: Exception) {
            try {
                filerSourceFile.delete()
            } catch (ignored: Exception) {
            }

            throw e
        }

    }

    @Throws(IOException::class)
    private fun emit(codeWriter: CodeWriter) {
        codeWriter.pushPackage(packageName)

        if (!fileComment.isEmpty) {
            codeWriter.emitComment(fileComment)
        }

        if (!packageName.isEmpty()) {
            codeWriter.emit("package \$L;\n", packageName)
            codeWriter.emit("\n")
        }

        if (!staticImports.isEmpty()) {
            for (signature in staticImports) {
                codeWriter.emit("import static \$L;\n", signature)
            }
            codeWriter.emit("\n")
        }

        var importedTypesCount = 0
        for (className in TreeSet(codeWriter.importedTypes.values)) {
            if (skipJavaLangImports && className.packageName() == "java.lang") continue
            codeWriter.emit("import \$L;\n", className)
            importedTypesCount++
        }

        if (importedTypesCount > 0) {
            codeWriter.emit("\n")
        }

        typeSpec.emit(codeWriter, null, emptySet<Modifier>())

        codeWriter.popPackage()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        return toString() == other.toString()
    }

    override fun hashCode() = toString().hashCode()

    override fun toString(): String {
        try {
            return buildString { writeTo(this) }
        } catch (e: IOException) {
            throw AssertionError()
        }
    }

    fun toJavaFileObject(): JavaFileObject {
        val uri = URI.create((if (packageName.isEmpty())
            typeSpec.name
        else
            packageName.replace('.', '/') + '/' + typeSpec.name) + Kind.SOURCE.extension)

        return object : SimpleJavaFileObject(uri, Kind.SOURCE) {
            private val lastModified = System.currentTimeMillis()
            override fun getCharContent(ignoreEncodingErrors: Boolean) = this@JavaFile.toString()

            @Throws(IOException::class)
            override fun openInputStream() = ByteArrayInputStream(getCharContent(true).toByteArray(UTF_8))

            override fun getLastModified() = lastModified
        }
    }

    fun toBuilder(): Builder {
        val builder = Builder(packageName, typeSpec)
        builder.fileComment.add(fileComment)
        builder.skipJavaLangImports = skipJavaLangImports
        builder.indent = indent
        return builder
    }

    class Builder internal constructor(internal val packageName: String,
                                       internal val typeSpec: TypeSpec) {
        internal val fileComment = CodeBlock.builder()
        internal val staticImports = TreeSet<String>()
        internal var skipJavaLangImports: Boolean = false
        internal var indent = "  "

        fun addFileComment(format: String, vararg args: Any) = apply { this.fileComment.add(format, *args) }

        fun <T : Enum<T>> addStaticImport(constant: Enum<T>)
                = addStaticImport(ClassName[constant.declaringClass.kotlin], constant.name)

        fun addStaticImport(clazz: KClass<*>, vararg names: String)
                = addStaticImport(ClassName[clazz], *names)

        fun addStaticImport(className: ClassName, vararg names: String) = apply {
            checkArgument(names.isNotEmpty(), "names array is empty")
            names.mapTo(staticImports) { className.canonicalName + "." + it }
        }

        /**
         * Call this to omit imports for classes in `java.lang`, such as `java.lang.String`.
         *
         * By default, JavaPoet explicitly imports types in `java.lang` to defend against
         * naming conflicts. Suppose an (ill-advised) class is named `com.example.String`. When
         * `java.lang` imports are skipped, generated code in `com.example` that references
         * `java.lang.String` will get `com.example.String` instead.
         */
        fun skipJavaLangImports(skipJavaLangImports: Boolean) = apply { this.skipJavaLangImports = skipJavaLangImports }

        fun indent(indent: String) = apply { this.indent = indent }

        fun build() = JavaFile(this)
    }

    companion object {
        private val NULL_APPENDABLE = object : Appendable {
            override fun append(charSequence: CharSequence) = this

            override fun append(charSequence: CharSequence, start: Int, end: Int) = this

            override fun append(c: Char) = this
        }

        fun builder(packageName: String, typeSpec: TypeSpec) = Builder(packageName, typeSpec)
    }
}
