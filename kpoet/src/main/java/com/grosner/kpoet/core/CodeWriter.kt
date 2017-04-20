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

import com.grosner.kpoet.core.Util.*
import java.io.IOException
import java.util.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier

/**
 * Converts a [JavaFile] to a string suitable to both human- and javac-consumption. This
 * honors imports, indentation, and deferred variable names.
 */
internal class CodeWriter(out: Appendable, private val indent: String,
                          val importedTypes: Map<String, ClassName>,
                          private val staticImports: Set<String>) {

    private val out = LineWrapper(out, indent, 100)
    private var indentLevel: Int = 0

    private var javadoc = false
    private var comment = false
    private var packageName = NO_PACKAGE
    private val typeSpecStack = ArrayList<TypeSpec>()
    private val staticImportClassNames = linkedSetOf<String>()
    private val importableTypes = LinkedHashMap<String, ClassName>()
    private val referencedNames = LinkedHashSet<String>()
    private var trailingNewline: Boolean = false

    /**
     * When emitting a statement, this is the line of the statement currently being written. The first
     * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
     * is -1 when the currently-written line isn't part of a statement.
     */
    var statementLine = -1

    @JvmOverloads constructor(out: Appendable, indent: String = "  ", staticImports: Set<String> = emptySet<String>()) : this(out, indent, emptyMap<String, ClassName>(), staticImports) {}

    init {
        staticImports.mapTo(staticImportClassNames) { it.substring(0, it.lastIndexOf('.')) }
    }

    fun indent(levels: Int = 1) = apply { indentLevel += levels }

    fun unindent(levels: Int = 1) = apply {
        checkArgument(indentLevel - levels >= 0, "cannot unindent %s from %s", levels, indentLevel)
        indentLevel -= levels
    }

    fun pushPackage(packageName: String) = apply {
        checkState(this.packageName === NO_PACKAGE, "package already set: %s", this.packageName)
        this.packageName = packageName
    }

    fun popPackage() = apply {
        checkState(this.packageName !== NO_PACKAGE, "package already set: %s", this.packageName)
        this.packageName = NO_PACKAGE
    }

    fun pushType(type: TypeSpec) = apply { this.typeSpecStack.add(type) }

    fun popType() = apply { this.typeSpecStack.removeAt(typeSpecStack.size - 1) }

    @Throws(IOException::class)
    fun emitComment(codeBlock: CodeBlock) {
        trailingNewline = true // Force the '//' prefix for the comment.
        comment = true
        try {
            emit(codeBlock)
            emit("\n")
        } finally {
            comment = false
        }
    }

    @Throws(IOException::class)
    fun emitJavadoc(javadocCodeBlock: CodeBlock) {
        if (javadocCodeBlock.isEmpty) return

        emit("/**\n")
        javadoc = true
        try {
            emit(javadocCodeBlock)
        } finally {
            javadoc = false
        }
        emit(" */\n")
    }

    @Throws(IOException::class)
    fun emitAnnotations(annotations: List<AnnotationSpec>, inline: Boolean) {
        for (annotationSpec in annotations) {
            annotationSpec.emit(this, inline)
            emit(if (inline) " " else "\n")
        }
    }

    /**
     * Emits `modifiers` in the standard order. Modifiers in `implicitModifiers` will not
     * be emitted.
     */
    @Throws(IOException::class)
    @JvmOverloads fun emitModifiers(modifiers: Set<Modifier>, implicitModifiers: Set<Modifier> = emptySet<Modifier>()) {
        if (modifiers.isEmpty()) return
        for (modifier in EnumSet.copyOf(modifiers)) {
            if (implicitModifiers.contains(modifier)) continue
            emitAndIndent(modifier.name.toLowerCase(Locale.US))
            emitAndIndent(" ")
        }
    }

    /**
     * Emit type variables with their bounds. This should only be used when declaring type variables;
     * everywhere else bounds are omitted.
     */
    @Throws(IOException::class)
    fun emitTypeVariables(typeVariables: List<TypeVariableName>) {
        if (typeVariables.isEmpty()) return

        emit("<")
        var firstTypeVariable = true
        for (typeVariable in typeVariables) {
            if (!firstTypeVariable) emit(", ")
            emit("\$L", typeVariable.name)
            var firstBound = true
            for (bound in typeVariable.bounds) {
                emit(if (firstBound) " extends \$T" else " & \$T", bound)
                firstBound = false
            }
            firstTypeVariable = false
        }
        emit(">")
    }

    @Throws(IOException::class)
    fun emit(s: String) = emitAndIndent(s)

    @Throws(IOException::class)
    fun emit(format: String, vararg args: Any?) = emit(CodeBlock.of(format, *args))

    @Throws(IOException::class)
    fun emit(codeBlock: CodeBlock): CodeWriter {
        var a = 0
        var deferredTypeName: ClassName? = null // used by "import static" logic
        val partIterator = codeBlock.formatParts.listIterator()
        while (partIterator.hasNext()) {
            val part = partIterator.next()
            when (part) {
                "\$L" -> emitLiteral(codeBlock.args[a++])

                "\$N" -> emitAndIndent(codeBlock.args[a++] as String)

                "\$S" -> {
                    val string = codeBlock.args[a++] as String?
                    // Emit null as a literal null: no quotes.
                    emitAndIndent(if (string != null)
                        stringLiteralWithDoubleQuotes(string, indent)
                    else
                        "null")
                }

                "\$T" -> {
                    var typeName = codeBlock.args[a++] as TypeName
                    if (typeName.isAnnotated) {
                        typeName.emitAnnotations(this)
                        typeName = typeName.withoutAnnotations()
                    }
                    // defer "typeName.emit(this)" if next format part will be handled by the default case
                    var defer = false
                    if (typeName is ClassName && partIterator.hasNext()) {
                        if (!codeBlock.formatParts[partIterator.nextIndex()].startsWith("$")) {
                            val candidate = typeName
                            if (staticImportClassNames.contains(candidate.canonicalName)) {
                                checkState(deferredTypeName == null, "pending type for static import?!")
                                deferredTypeName = candidate
                                defer = true
                            }
                        }
                    }
                    if (!defer) typeName.emit(this)
                }

                "$$" -> emitAndIndent("$")

                "$>" -> indent()

                "$<" -> unindent()

                "$[" -> {
                    checkState(statementLine == -1, "statement enter $[ followed by statement enter $[")
                    statementLine = 0
                }

                "$]" -> {
                    checkState(statementLine != -1, "statement exit $] has no matching statement enter $[")
                    if (statementLine > 0) {
                        unindent(2) // End a multi-line statement. Decrease the indentation level.
                    }
                    statementLine = -1
                }

                "\$W" -> out.wrappingSpace(indentLevel + 2)

                else -> {
                    // handle deferred type
                    var defer = false
                    if (deferredTypeName != null) {
                        if (part.startsWith(".")) {
                            if (emitStaticImportMember(deferredTypeName.canonicalName, part)) {
                                // okay, static import hit and all was emitted, so clean-up and jump to next part
                                deferredTypeName = null
                                defer = true
                            }
                        }
                        if (!defer && deferredTypeName != null) {
                            deferredTypeName.emit(this)
                            deferredTypeName = null
                        }
                    }
                    if (!defer) emitAndIndent(part)
                }
            }
        }
        return this
    }

    @Throws(IOException::class)
    fun emitWrappingSpace(): CodeWriter {
        out.wrappingSpace(indentLevel + 2)
        return this
    }

    @Throws(IOException::class)
    private fun emitStaticImportMember(canonical: String, part: String): Boolean {
        val partWithoutLeadingDot = part.substring(1)
        if (partWithoutLeadingDot.isEmpty()) return false
        val first = partWithoutLeadingDot[0]
        if (!Character.isJavaIdentifierStart(first)) return false
        val explicit = canonical + "." + extractMemberName(partWithoutLeadingDot)
        val wildcard = canonical + ".*"
        if (staticImports.contains(explicit) || staticImports.contains(wildcard)) {
            emitAndIndent(partWithoutLeadingDot)
            return true
        }
        return false
    }

    @Throws(IOException::class)
    private fun emitLiteral(o: Any?) {
        (o as? TypeSpec)?.emit(this, null, emptySet<Modifier>())
                ?: ((o as? AnnotationSpec)?.emit(this, true)
                ?: if (o is CodeBlock) {
            emit(o)
        } else {
            emitAndIndent(o.toString())
        })
    }

    /**
     * Returns the best name to identify `className` with in the current context. This uses the
     * available imports and the current scope to find the shortest name available. It does not honor
     * names visible due to inheritance.
     */
    fun lookupName(className: ClassName): String {
        // Find the shortest suffix of className that resolves to className. This uses both local type
        // names (so `Entry` in `Map` refers to `Map.Entry`). Also uses imports.
        var nameResolved = false
        var c: ClassName? = className
        while (c != null) {
            val resolved = resolve(c.simpleName())
            nameResolved = resolved != null

            if (resolved == c) {
                val suffixOffset = c.simpleNames().size - 1
                return join(".", className.simpleNames().subList(
                        suffixOffset, className.simpleNames().size))
            }
            c = c.enclosingClassName()
        }

        // If the name resolved but wasn't a match, we're stuck with the fully qualified name.
        if (nameResolved) {
            return className.canonicalName
        }

        // If the class is in the same package, we're done.
        if (packageName == className.packageName()) {
            referencedNames.add(className.topLevelClassName().simpleName())
            return join(".", className.simpleNames())
        }

        // We'll have to use the fully-qualified name. Mark the type as importable for a future pass.
        if (!javadoc) {
            importableType(className)
        }

        return className.canonicalName
    }

    private fun importableType(className: ClassName) {
        if (className.packageName().isEmpty()) {
            return
        }
        val topLevelClassName = className.topLevelClassName()
        val simpleName = topLevelClassName.simpleName()
        val replaced = importableTypes.put(simpleName, topLevelClassName)
        if (replaced != null) {
            importableTypes.put(simpleName, replaced) // On collision, prefer the first inserted.
        }
    }

    /**
     * Returns the class referenced by `simpleName`, using the current nesting context and
     * imports.
     */
    // TODO(jwilson): also honor superclass members when resolving names.
    private fun resolve(simpleName: String): ClassName? {
        // Match a child of the current (potentially nested) class.
        for (i in typeSpecStack.indices.reversed()) {
            val typeSpec = typeSpecStack[i]
            for (visibleChild in typeSpec.typeSpecs) {
                if (visibleChild.name == simpleName) {
                    return stackClassName(i, simpleName)
                }
            }
        }

        // Match the top-level class.
        if (typeSpecStack.size > 0 && typeSpecStack[0].name == simpleName) {
            return ClassName[packageName, simpleName]
        }

        // Match an imported type.
        val importedType = importedTypes[simpleName]
        if (importedType != null) return importedType

        // No match.
        return null
    }

    /** Returns the class named `simpleName` when nested in the class at `stackDepth`.  */
    private fun stackClassName(stackDepth: Int, simpleName: String): ClassName {
        var className = ClassName[packageName, typeSpecStack[0].name]
        for (i in 1..stackDepth) {
            className = className.nestedClass(typeSpecStack[i].name)
        }
        return className.nestedClass(simpleName)
    }

    /**
     * Emits `s` with indentation as required. It's important that all code that writes to
     * [.out] does it through here, since we emit indentation lazily in order to avoid
     * unnecessary trailing whitespace.
     */
    @Throws(IOException::class)
    fun emitAndIndent(s: String): CodeWriter {
        var first = true
        for (line in s.split("\n".toRegex()).toTypedArray()) {
            // Emit a newline character. Make sure blank lines in Javadoc & comments look good.
            if (!first) {
                if ((javadoc || comment) && trailingNewline) {
                    emitIndentation()
                    out.append(if (javadoc) " *" else "//")
                }
                out.append("\n")
                trailingNewline = true
                if (statementLine != -1) {
                    if (statementLine == 0) {
                        indent(2) // Begin multiple-line statement. Increase the indentation level.
                    }
                    statementLine++
                }
            }

            first = false
            if (line.isEmpty()) continue // Don't indent empty lines.

            // Emit indentation and comment prefix if necessary.
            if (trailingNewline) {
                emitIndentation()
                if (javadoc) {
                    out.append(" * ")
                } else if (comment) {
                    out.append("// ")
                }
            }

            out.append(line)
            trailingNewline = false
        }
        return this
    }

    @Throws(IOException::class)
    private fun emitIndentation() {
        for (j in 0..indentLevel - 1) {
            out.append(indent)
        }
    }

    /**
     * Returns the types that should have been imported for this code. If there were any simple name
     * collisions, that type's first use is imported.
     */
    fun suggestedImports(): Map<String, ClassName> {
        val result = LinkedHashMap(importableTypes)
        result.keys.removeAll(referencedNames)
        return result
    }

    companion object {
        /** Sentinel value that indicates that no user-provided package has been set.  */
        private val NO_PACKAGE = String()

        private fun extractMemberName(part: String): String {
            checkArgument(Character.isJavaIdentifierStart(part[0]), "not an identifier: %s", part)
            for (i in 1..part.length) {
                if (!SourceVersion.isIdentifier(part.substring(0, i))) {
                    return part.substring(0, i - 1)
                }
            }
            return part
        }
    }
}
