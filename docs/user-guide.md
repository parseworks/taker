# Taker User Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Basic Concepts](#basic-concepts)
4. [Quick Start](#quick-start)
   1. [Basic String Match](#basic-string-match)
   2. [Composition with `then`](#composition-with-then)
   3. [Structured Data](#structured-data)
   4. [Error Handling](#error-handling)
   5. [Calculator Parser](#calculator-parser)
5. [Advanced Usage](#advanced-usage)
   1. [Recursive Parsers](#recursive-parsers)
   2. [Scanner Primitives](#scanner-primitives)
   3. [Performance Optimization](#performance-optimization)
6. [Where Next](#where-next)
7. [Realistic Examples](#realistic-examples)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

## Introduction

Taker is a parser-combinator library for Java 21+. Instead of using external
grammar files or code generation, you define grammars directly in Java by
combining small parsers into larger ones.

The API favors readable method names such as `thenSkip`, `skipThen`,
`zeroOrMore`, and `oneOrMoreSeparatedBy`. It also provides scanner primitives
such as `collectChars`, `skipWhile`, and `countWhile` for efficient character
runs.

### Key Features

- **Composable combinators**: build complex grammars from small parsers.
- **Detailed diagnostics**: failures report expected input and source context.
- **Recursive grammars**: use `Taker.ref()` and `set(...)`.
- **Source spans**: use `located()` when AST nodes need start/end offsets.
- **Scanner primitives**: handle whitespace, identifiers, and raw text without
  per-character parser allocation.
- **Java 21 baseline**: the Maven build uses `maven.compiler.release=21`.

## Installation

### Maven

```xml
<dependency>
  <groupId>io.github.parseworks</groupId>
  <artifactId>taker</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.parseworks:taker:1.0-SNAPSHOT'
```

## Basic Concepts

### `Taker<A>`

A `Taker<A>` is a parser that consumes an `Input` and returns a `Result<A>`.
Successful results contain a parsed value and the next input position.

### `Input`

`Input` wraps a character sequence and tracks the current zero-based position.
Inputs are immutable: advancing returns a new cursor.

### `Result<A>`

A result is either:

- **Match**: parsing succeeded and produced a value.
- **Failure**: parsing failed with structured diagnostic information.
- **Partial failure**: parsing committed far enough that alternatives should not
  be tried.

Use `matches()`, `value()`, `input()`, and `error()` to inspect results.

## Quick Start

### Basic String Match

```java
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;

import static io.github.parseworks.taker.parsers.Lexical.string;

Taker<String> hello = string("hello");
Result<String> result = hello.parse("hello world");

if (result.matches()) {
    System.out.println("Got: " + result.value());
}
```

### Composition with `then`

Use `then`, `thenSkip`, and `skipThen` to sequence parsers.

```java
import io.github.parseworks.taker.Taker;

import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.string;

Taker<String> helloWorld = string("hello")
    .thenSkip(chr(' '))
    .then(string("world"))
    .map((left, right) -> left + " " + right);

System.out.println(helloWorld.parse("hello world").value());
```

### Structured Data

For anything more complex than strings, use `map` to build domain objects.

```java
import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Taker;

import static io.github.parseworks.taker.parsers.Lexical.collectChars;
import static io.github.parseworks.taker.parsers.Lexical.chr;

record KV(String key, String value) {}

Taker<String> identifier = collectChars(CharPredicate.asciiLetterOrDigit)
    .expecting("identifier");
Taker<String> lineValue = collectChars(CharPredicate.lineBreak.negate())
    .expecting("value");

Taker<KV> kvParser = identifier
    .thenSkip(chr('='))
    .then(lineValue)
    .map(KV::new);

Taker<java.util.List<KV>> configParser = kvParser.oneOrMoreSeparatedBy(chr('\n'));
```

### Error Handling

Use `expecting(...)` to relabel a specific expected token and `label(...)` to
add larger grammar context without losing the underlying cause.

```java
Taker<String> identifier = collectChars(CharPredicate.asciiLetterOrDigit)
    .expecting("identifier");

Taker<KV> kvParser = identifier
    .thenSkip(chr('='))
    .then(lineValue)
    .map(KV::new)
    .label("key-value pair");

Result<String> result = identifier.parse("=value");
if (!result.matches()) {
    System.err.println(result.error());
}
```

Use `flatMap` with `Combinators.pure(...)` and `Combinators.fail(...)` for semantic
validation after syntax has parsed.

```java
import static io.github.parseworks.taker.parsers.Combinators.fail;
import static io.github.parseworks.taker.parsers.Combinators.pure;
import static io.github.parseworks.taker.parsers.Numeric.integer;

Taker<Integer> positive = integer.flatMap(n ->
    n > 0 ? pure(n) : fail("positive integer")
);
```

### Calculator Parser

```java
import io.github.parseworks.taker.Taker;

import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static io.github.parseworks.taker.parsers.Numeric.integer;

Taker<Integer> expr = Taker.ref();
Taker<Integer> term = Taker.ref();
Taker<Integer> factor = Taker.ref();

Taker<Integer> parenFactor = chr('(')
    .skipThen(trim(expr))
    .thenSkip(chr(')'));

factor.set(trim(integer.or(parenFactor)));

Taker<BinaryOperator<Integer>> mulOp = trim(chr('*')).as((a, b) -> a * b);
Taker<BinaryOperator<Integer>> divOp = trim(chr('/')).as((a, b) -> a / b);
term.set(factor.chainLeftOneOrMore(oneOf(mulOp, divOp)).or(factor));

Taker<BinaryOperator<Integer>> addOp = trim(chr('+')).as(Integer::sum);
Taker<BinaryOperator<Integer>> subOp = trim(chr('-')).as((a, b) -> a - b);
expr.set(term.chainLeftOneOrMore(oneOf(addOp, subOp)).or(term));

System.out.println(expr.parseAll("(2 + 3) * 4").value());
```

## Advanced Usage

### Recursive Parsers

Recursive parsers are essential for nested grammars such as expressions, JSON,
HTML, and programming languages. Create a placeholder with `Taker.ref()`, build
parsers that refer to it, then initialize it with `set(...)`.

```java
Taker<String> nested = Taker.ref();
Taker<String> parens = chr('(')
    .skipThen(nested)
    .thenSkip(chr(')'));

nested.set(collectChars(CharPredicate.asciiLetter).or(parens));
```

### Scanner Primitives

Use scanner primitives for consecutive raw input characters:

```java
Taker<String> word = Lexical.collectChars(CharPredicate.asciiLetter);
Taker<Void> spaces = Lexical.skipWhile(CharPredicate.horizontalWhitespace);
Taker<Integer> indentWidth = Lexical.countWhile(CharPredicate.is(' '));
```

Prefer these over `chr(predicate).oneOrMore()` or
`chr(predicate).collectString()` for long character runs. Repeated character
parsers are more flexible, but allocate per parser step.

### Performance Optimization

1. **Reuse parsers**: define common parsers once, often as `static final`.
2. **Use scanner primitives for character runs**: prefer `collectChars`,
   `takeWhile`, `skipWhile`, and `countWhile`.
3. **Use `trim` at token boundaries**: avoid wrapping every single character
   parser.
4. **Order alternatives carefully**: put longer or more specific `oneOf`
   alternatives first.
5. **Use `commit` intentionally**: once a grammar branch is chosen, committing
   can improve error locality and avoid confusing backtracking.

## Where Next

- Use [api-contract.md](api-contract.md) as the precise reference for public API
  and parser semantics.
- Use [advanced-user-guide.md](advanced-user-guide.md) for expression parsing,
  recursion, ambiguity, diagnostics, and performance-sensitive parser choices.
- Use [parser-design-guide.md](parser-design-guide.md) when designing a grammar,
  AST, or parser module structure.
- Use [benchmarks.md](benchmarks.md) when checking performance or allocation
  changes.

## Realistic Examples

The executable examples under
`src/test/java/io/github/parseworks/taker/examples`
show larger parsers that are still small enough to study:

- **Sectioned config parser**: parses INI-style sections and key/value pairs
  while preserving line breaks as meaningful grammar.
- **Recursive JSON-like value parser**: parses objects, arrays, strings,
  numbers, booleans, and null using `Taker.ref()`, separated values, and
  whitespace-insensitive tokens.
- **Practical TOML parser**: parses tables, dotted keys, typed scalar values,
  arrays, inline tables, comments, and duplicate-key errors while keeping the
  parser executable as documentation.

These examples are run by the normal Maven test suite, so they double as
documentation and compatibility checks.

## Troubleshooting

### Parser Does Not Consume All Input

Use `parseAll(...)` when trailing input should be rejected:

```java
Result<Integer> result = integer.parseAll("42x");
assert !result.matches();
```

Use `parse(...)` when a prefix match is acceptable.

### Left Recursion

Left recursion can cause infinite loops in parser combinator libraries. Taker
has recursion protection, but grammar rules should still consume input before
recursing.

```java
// Problematic:
expr.set(expr.then(chr('+')).then(term).map(...));

// Safer:
expr.set(term.then(chr('+').skipThen(expr).optional()).map(...));
```

### Performance Issues

If a parser allocates heavily or parses slowly:

1. Replace raw character repetitions with scanner primitives.
2. Break complex parsers into reusable tokens.
3. Reduce overlapping alternatives in `oneOf`.
4. Use built-in parsers such as `Numeric.integer` and `Numeric.doubleValue`.
5. Run the JMH profile described in the README.

## Best Practices

1. **Keep parsers small and named**: named parsers are easier to test and debug.
2. **Label grammar boundaries**: use `label(...)` for grammar rules and
   `expecting(...)` for specific expected tokens.
3. **Prefer built-ins**: use `Numeric`, `Lexical`, and `Combinators` before
   writing custom logic.
4. **Be explicit about whitespace**: choose `trimSpaces`, `trimWhitespace`,
   `skipWhile`, or `lexeme` according to the grammar.
5. **Test semantic contracts**: empty input, partial input, trailing input, and
   invalid alternatives are where parser behavior matters most.
