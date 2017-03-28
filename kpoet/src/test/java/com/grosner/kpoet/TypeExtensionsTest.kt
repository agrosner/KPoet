package com.grosner.kpoet

import com.squareup.javapoet.TypeName
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.assertEquals
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.io.Serializable

/**
 * Description:
 */
@RunWith(JUnitPlatform::class)
class TypeExtensionsTest : Spek({
    describe("type extensions") {
        on("can create a class with a method that sums two numbers with if else branches") {
            val typeSpec = `class`("TestClass") {
                modifiers(publicFinal)

                `public`(String::class, "doGood") {
                    statement("\$T a = 1", TypeName.INT)
                    statement("\$T b = 2", TypeName.INT)
                    statement("\$T sum = a + b", TypeName.INT)

                    `if`("sum > 3") {
                        `return`(str("Large Sum"))
                    }.`else if`("sum < 3") {
                        `return`(str("Small Sum"))
                    } `else` {
                        `return`(str("Three is the Sum"))
                    }
                }
            }

            it("should generate proper class file") {
                assertEquals("public final class TestClass {\n" +
                        "  public java.lang.String doGood() {\n" +
                        "    int a = 1;\n" +
                        "    int b = 2;\n" +
                        "    int sum = a + b;\n" +
                        "    if (sum > 3) {\n" +
                        "      return \"Large Sum\";\n" +
                        "    } else if (sum < 3) {\n" +
                        "      return \"Small Sum\";\n" +
                        "    } else {\n" +
                        "      return \"Three is the Sum\";\n" +
                        "    }\n" +
                        "  }\n", typeSpec.toString())
            }
        }

        on("can create a class with fields") {
            val isReady = "isReady"
            val typeSpec = `abstract class`("TestClass") {
                modifiers(public)
                `package private field`(TypeName.BOOLEAN, isReady, { init(lit(false)) })
                `package private field`(String::class, isReady, { init(str("SomeName")) })

                constructor(param(TypeName.BOOLEAN, isReady)) {
                    statement("this.\$1L = \$1L", isReady)
                }
            }

            it("should generate proper class file") {
                assertEquals("public abstract class TestClass {\n" +
                        "  boolean isReady = false;\n\n" +
                        "  java.lang.String isReady = \"SomeName\";\n\n" +
                        "  TestClass(boolean isReady) {\n" +
                        "    this.isReady = isReady;\n" +
                        "  }\n" +
                        "}\n", typeSpec.toString())
            }
        }

        on("can create subclass with overridden methods") {
            val typeSpec = `class`("TestClass") {
                extends(parameterized<String>(List::class))
                implements(parameterized<String>(Comparable::class), Serializable::class.typeName)

                `public`(Int::class, "compareTo", param(String::class, "other")) {
                    annotation(Override::class)
                    `return`(lit(0))
                }
            }

            it("should generate proper class file") {
                assertEquals("class TestClass extends java.util.List<java.lang.String> implements java.lang" +
                        ".Comparable<java.lang.String>, java.io.Serializable {\n" +
                        "  @java.lang.Override\n" +
                        "  public int compareTo(java.lang.String other) {\n" +
                        "    return 0;\n" +
                        "  }\n" +
                        "}\n", typeSpec.toString())
            }
        }
    }
})

