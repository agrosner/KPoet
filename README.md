# KPoet

KPoet is a Kotlin extensions library on top of [JavaPoet](https://github.com/square/javapoet) that helps you write code generators / annotation processors that _feel_ like actually writing Java code directly. It provides a Kotlin DSL syntax that resembles real java code as much as possible.
Also it attempts to make the code generator writing clear as writing native java code itself.

Here's a (boring) `HelloWorld` class:

```java
package com.example.helloworld;

public final class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, JavaPoet!");
  }
}

```

Is represented in JavaPoet like:

```java

MethodSpec main = MethodSpec.methodBuilder("main")
    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
    .returns(void.class)
    .addParameter(String[].class, "args")
    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(main)
    .build();

JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();

javaFile.writeTo(System.out);

```

While JavaPoet provides a very nice library that makes it easier to write code that writes Java code, there are a few problems with vanilla JavaPoet code:
1. The code is declared in reverse order. You define constructs that are nested inside larger ones (fields, methods, etc) first, then work your way up to the `JavaFile`.
2. You have to think about how the code will look a lot (especially when it gets more complex), decreasing maintainability.
3. No order enforced where you declare the spec properties, potentially leading to mistakes and can reduce readibility.

`KPoet` attempts to solve these issues by:
1. Providing Kotlin DSL extension builders that map closely to native Java code.
2. Have the code you write for the generator resemble the Java output code closely and in the order you expect, providing both readibility and maintainability.
3. Also, KPoet provides more concise methods and constructs that will reduce lines of code.

So using `KPoet` from the previous example:

```kotlin

javaFile("com.example.helloworld") {
  `class`("HelloWorld") {  modifiers(public, final)

    `public static`(TypeName.VOID, "main",
        param(Array<String>::class, "args")) {
      statement("\$T.out.println(${"Hello, JavaPoet!".S})", System::class.java)
    }
  }
}.writeTo(System.out)

```

As you can see, KPoet takes JavaPoet code and turns it into an expressive DSL that tries to map to regular java as much as possible.

if we want to output a method such as this:

```java

public boolean handleAction(String action) {
  switch(action) {
    case "bonus": {
      this.name = "BONUS";
      break;
    }
    default: {
      this.name = "NO BONUS";
      break;
    }
  }

  if (this.name == "BONUS") {
    return true
  } else if (this.name  == "NO BONUS") {
    return false
  }

  throw new IllegalStateException("Did not process proper action")
}

```

We represent it as:

```kotlin

`public`(TypeName.BOOLEAN, "handleAction",
       param(String::class, "action")) {
  switch("action") {
    case("bonus".S) {
      statement("this.name = ${"BONUS".S}") // .S wraps it in quotes
      `break`()
    }
    default {
      statement("this.name = ${"NO BONUS".S}")
      `break`()
    }
  }

  `if`("this.name == ${"BONUS".S}") {
    `return`(true.L) // string literal representation with .L
  }.`else if`("this.name == ${"NO_BONUS".S}") {
    `return`(false.L)
  }.end() // end required for `if` and `else if`.

  `throw new`(IllegalStateException::class, "Did not process proper action")
}


```

## Download

Including in your project:

```gradle

allProjects {
  repositories {
    // required to find the project's artifacts
    maven { url "https://www.jitpack.io" }
  }
}
```

```gradle
compile 'com.squareup:javapoet:1.8.0' // version of JavaPoet currently
compile 'com.github.agrosner:KPoet:1.0.0' // version of KPoet
```

The next few sections we attempt to mirror the JavaPoet readme, but converted syntax for KPoet, to give you an idea of what the library provides.


### Code & Control Flow

JavaPoet offers APIs to make code generation easier.

We want to write:

```java
void main() {
  int total = 0;
  for (int i = 0; i < 10; i++) {
    total += i;
  }
}

```

And so JavaPoet has us write this `MethodSpec`:

```java

MethodSpec main = MethodSpec.methodBuilder("main")
    .addStatement("int total = 0")
    .beginControlFlow("for (int i = 0; i < 10; i++)")
    .addStatement("total += i")
    .endControlFlow()
    .build();

```

 This is a simple example, but you have to think about what the code will look like when it's generated. Also if you forget to provide a corresponding `endControlFlow()` for every `beginControlFlow()`, it will lead you to runtime crashes that can make it very difficult to diagnose.

 With KPoet, you do less thinking about how the code will look:

```kotlin

val main = `fun`(TypeName.VOID, "main") {
  statement("int total = 0")
  `for`("int i = 0; i < 10; i++") {
    statement("total += i")
  }
}

```

do..while:

```kotlin
`do` {
  statement("i++")
}.`while`("sum < 20")
```

### Literals

KPoet has a couple helper methods for cases where we need to pass a literal value to a statement, or code block. The best example is `return`.

```java
addStatement("return \$L", someLiteral)
```

can easily be replaced with:

```kotlin
`return`(someLiteral.L)
```

This simply converts the object to string, but preserving the JavaPoet-like syntax.

### Strings

When using code that includes string literals, JavaPoet uses `$S` to emit a `string`, wrapping quotation marks to escape it.

With the power of Kotlin string interpolation, we barely need to use $S. For cases where we need to convert it to a string for code output, KPoet provides the `Any?.S` property to simply wrap the object's `toString()` value in quotes.

```kotlin

`public`(String::class, "getStatus", param(TypeName.BOOLEAN, "isReady")) {
  `if`("isReady") {
    `return`("BONUS".S) // if we don't use .S, it's outputted as a literal.
  } else {
    `return`("NO BONUS".S)
  }
}

```

which outputs:


```java
public String getStatus(boolean isReady) {
  if (isReady) {
    return "BONUS";
  } else {
    return "NO BONUS";
  }
}

```

### Types

JavaPoet has spectacular handling of reference types by collecting and importing them to make the code much more readable, KPoet does not provide any extension on top of this functionality.

You will still need to pass that `Class` or `TypeName` to JavaPoet:

```kotlin

`abstract class`("TestClass") {  modifiers(public)
    field(TypeName.BOOLEAN, isReady, { `=`(false.L) })
    field(String::class, isReady, { `=`("SomeName".S) })

    `constructor`(param(TypeName.BOOLEAN, isReady)) {
        statement("this.$isReady = $isReady")
    }
}

```

__Be careful__: this library does not convert `KClass<*>` to `Class<*>` in string interpolation with "\$T". However, most of places where `Class` is used in `JavaPoet` we provide the `KClass` version of that.

#### Import Static

`KPoet` supports `import static` pretty easily. When constructing a `JavaFile`,
pass them as the second parameter in the `javaFile` method:
```kotlin

val file = javaFile("com.grosner", {
    `import static`(Collections::class, "*")
    `import static`(ClassName.get(String::class.java), "*")
}) {
    `class`("HelloWorld") {
        this
    }
}


```

#### Methods

KPoet supports all kinds of methods.

You can write `abstract` methods easily:

```kotlin

`abstract class`("HelloWorld") { modifiers(public)
  abstract(TypeName.VOID, "flux") {
    modifiers(protected)
  }
}

```

Which generates:

```kotlin
public abstract class HelloWorld {
  protected abstract void flux();
}
```

#### Constructors

Constructors are fairly easy to write.

```kotlin

`public class`("HelloWorld") {
  `private final field`(String::class, "greeting")

  `constructor`(param(String::class, "greeting")) {
    modifiers(Modifier.PUBLIC)
    statement("this.greeting = greeting")
  }
}

```

#### Parameters

Parameters are done via global methods:

```kotlin

`fun`(TypeName.VOID, "welcomeOverlords",
  `final param`(String::class, "android"),)
  `final param`(String::class, "robot")

```

Which generates:

```java
void welcomeOverlords(final String android, final String robot) {
}
```

To add annotations to parameters, simply call:

```kotlin

`fun`(TypeName.VOID, "welcomeOverlords",
  `final param`(`@`(TestAnnotation::class), String::class, "android"),
  `final param`(`@`(TestAnnotation::class, {
                    this["name"] = "Some Kind of Member".S // we use a map to construct the properties here.
                    this["purpose"] = "Some Purpose we have".S
                }, String::class, "robot")))
```

#### Fields

We easily add fields to our `TypeSpec` definition:


```kotlin

`public class`("HelloWorld") {
  `private final field`(String::class, "robot", { `@`(Nullable::class) }) // can add annotations on fields
  field(`@`(Nullable::class), String::class, "android") { `=`("THE BEST".S)} // or this way
}

```

#### Enums

use `enum()` to construct within a `javaFile`:

```kotlin

`enum`("Roshambo") { modifiers(public)
    case("ROCK","fist".S){
      `public`(String::class, "toString") {
        `@`(Override::class)
        `return`("avalanche!".S)
      }
    }
    case("SCISSORS", "peace".S)
    case("PAPER", "flat".S)

    `private final field`(String::class, "handsign")

    `constructor`(param(String::class, "handsign")) {
      statement("this.handsign = handsign")
    }
}

```

which generates this:

```java
public enum Roshambo {
  ROCK("fist") {
    @Override
    public void toString() {
      return "avalanche!";
    }
  },

  SCISSORS("peace"),

  PAPER("flat");

  private final String handsign;

  Roshambo(String handsign) {
    this.handsign = handsign;
  }
}
```

#### Anonymous Inner Classes

We write a method that contains a class that contains a method:

```kotlin
`fun`(TypeName.VOID, "sortByLength", param(parameterized<String>(List::class), "strings")) {
  statement("\$T.sort(strings, \$L)", Collections::class.java, `anonymous class`("") {
    extends(parameterized<String>(Comparator::class))
    `public`(TypeName.INT, "compare", param(String::class, "a"), param(String::class, "b")) {
      `@`(Override::class)
      `return`("a.length() - b.length()")
    }
  })
}

```

Which generates:

```java

void sortByLength(List<String> strings) {
  Collections.sort(strings, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
      return a.length() - b.length();
    }
  });
}

```

#### Annotations

Simple annotations are easy, just use the "\`@()\`" method within classes, functions, fields or parameters:

On methods:
```Kotlin

`public`(String::class, "toString") {
  `@`(Override::class)
  `return`("Hoverboard".S)
}

```

on Classes:

```kotlin

`public class`("User") {
  `@`(Override::class) // annotations have to be within the class block, otherwise we can't associate it with a `class`

}

```

On fields:

```kotlin

field(TypeName.BOOLEAN, isReady) {
  `@`(Override::class)
  `=`(false.L)
}

```

on parameters:


```kotlin

`private`(TypeName.VOID, "someMethod",
  `final param`(`@`(NonNull::class), String::class, "someParameter"))

```

On more complicated cases, say for a class:
```kotlin
`public class`("User") {
  extends(Object::class)
  `@`(Headers::class, {
      this["accept"] = "application/json; charset=utf-8".S
      this["userAgent"] = "Square Cash".S
    })

}
```

It generates:

```java

@Headers(
    accept = "application/json; charset=utf-8",
    userAgent = "Square Cash"
)
public class User extends Object {
};

```

For nested annotations:

```kotlin

`public`(LogReceipt::class, "recordEvent", param(LogRecord::class, "logRecord")) {
  modifiers(abstract)
  `@`(HeaderList::class) {
    member("value", `@`(Header::class, mapFunc = {
      this["name"] = "Accept".S
      this["value"] = "application/json; charset=utf-8".S
    }).L)
    member("value", `@`(Header::class, mapFunc = {
      this["name"] = "User-Agent".S
      this["value"] = "Square Cash".S
    }).L)
  }
}

```

#### JavaDoc

To add JavaDoc to fields, methods, and types:

```kotlin

`public class`("SomeClass") {
  javadoc("Javadoc goes here")

  `private final field`(String::class, "someField") {
    javadoc("This could be anything you want it to be")
    `=`("SomeValue".S)
  }
}

`public`(TypeName.VOID, "dismiss", param(Message::class, "message")) {
  javadoc("Hides {@code message} from the caller's history. Other\n"
        + "participants in the conversation will continue to see the\n"
        + "message in their own history unless they also delete it.\n")
  javadoc("\n")
  javadoc("<p>Use {@link #delete($T)} to delete the entire\n"
        + "conversation for all participants.\n", Conversation.class)
}



```

## Pull Requests
I welcome and encourage all pull requests. It usually will take me within 24-48 hours to respond to any issue or request. Here are some basic rules to follow to ensure timely addition of your request:
  1. Match coding style (braces, spacing, etc.) This is best achieved using CMD+Option+L (Reformat code) on Mac (not sure for Windows) with Android Studio defaults.
  2. If its a feature, bugfix, or anything please only change code to what you specify.
  3. Please keep PR titles easy to read and descriptive of changes, this will make them easier to merge :)
  4. Pull requests _must_ be made against `develop` branch. Any other branch (unless specified by the maintainers) will get rejected.
  5. Have fun!

## Maintained By
[agrosner](https://github.com/agrosner) ([@agrosner](https://www.twitter.com/agrosner))
