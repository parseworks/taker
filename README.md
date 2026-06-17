# Taker: A Fluent Parser Combinator Library for Java

Taker is a lean, powerful, and developer-friendly parser combinator library for Java 21+. It strikes a balance between the purity of functional combinators and the pragmatic performance needs of real-world tooling.

## Key Features

- **Fluent API**: Highly ergonomic DSL for Java using a functional approach.
- **N-ary Composition**: Use `.then()` and `ApplyBuilder` to cleanly map multiple values without deep nesting.
- **Performance-Oriented**: Includes scanner primitives like `Chars.takeWhile`, `collectChars`, `skipWhile`, `countWhile`, `takeUntil`, and `Lexical.regex` for efficient lexing.
- **Robust Diagnostics**: Source-mapped error snippets with line/column info and "expected vs found" messaging.
- **Recursion Support**: Easy definition of recursive grammars using `Taker.ref()` and `.set()`.
- **Commit/Cut**: Prevent unnecessary backtracking and improve error locality with `commit()`.

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.parseworks</groupId>
    <artifactId>taker</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Parsing

```java
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.parsers.Numeric;

public class Main {
    public static void main(String[] args) {
        Taker<Integer> parser = Numeric.integer;
        Result<Integer> result = parser.parse("-123");
        
        if (result.matches()) {
            System.out.println("Parsed: " + result.value());
        }
    }
}
```

### Building a Custom Parser

Using combinators to parse a simple "Key: Value" pair:

```java
import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Taker;

import static io.github.parseworks.taker.parsers.Chars.*;
import static io.github.parseworks.taker.parsers.Lexical.trim;

Taker<String> identifier = collectChars(CharPredicate.asciiLetterOrDigit);
Taker<String> kvPair = identifier
    .thenSkip(trim(chr(':')))
    .then(collectChars(CharPredicate.lineBreak.negate()))
    .map((key, value) -> key + " => " + value);

String output = kvPair.parse("User: Bob").value();
// Output: User => Bob
```

`Lexical.trim(parser)` is intentionally space-only: it skips ASCII `' '` around
a token, but not tabs or newlines. Use `trimWhitespace(parser)` when Java
whitespace, including line breaks, should be ignored, or `lexeme(parser,
ignored)` when the grammar needs a custom whitespace/comment policy.

For long character runs, prefer scanner primitives over repeated character
parsers:

```java
// Preferred: direct scanner, low allocation
Taker<String> word = collectChars(CharPredicate.asciiLetter);
Taker<Void> spaces = skipWhile(CharPredicate.horizontalWhitespace);

// More flexible, but much more expensive for long runs
Taker<String> slower = chr(CharPredicate.asciiLetter).collectString();
```

### Recursive Grammars

Handle nested structures with ease:

```java
import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Taker;

import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Chars.chr;

Taker<String> nested = Taker.ref();
Taker<String> atom = chr(CharPredicate.asciiLetter).collectString();
Taker<String> grouped = chr('(')
    .skipThen(nested)
    .thenSkip(chr(')'));

nested.set(oneOf(atom, grouped));

String value = nested.parse("((hello))").value();
```

## Error Handling

Taker provides detailed error reports when parsing fails:

```text
Error: line 5 position 12
    <div id="main">
               ^
Reasons at this location:
- expected '"' found >
- expected escaped string
```

## API Contract

The documentation set is mapped in [docs/README.md](docs/README.md).

The intended public API and parser semantics are documented in
[docs/api-contract.md](docs/api-contract.md). Compatibility expectations and
the release checklist are documented in
[docs/release-policy.md](docs/release-policy.md).

## Realistic Examples

Executable examples live in `src/test/java/io/github/parseworks/taker/examples`.
They include a sectioned config parser and a recursive JSON-like value parser
that demonstrate scanner primitives, whitespace policy, separated values, and
recursive grammars against realistic input. The suite also includes a practical
TOML parser covering tables, dotted keys, typed values, arrays, inline tables,
comments, and duplicate-key validation.

## Benchmarks

JMH benchmarks live under `src/jmh/java` and are opt-in so normal test runs stay
fast and deterministic.

```bash
mvn -Pjmh test-compile exec:exec
```

Common JMH options are exposed as Maven properties:

```bash
mvn -Pjmh test-compile exec:exec "-Djmh.include=parseNumber" "-Djmh.warmupIterations=5" "-Djmh.measurementIterations=5" "-Djmh.forks=2"
```

Use `jmh.prof` to enable a JMH profiler such as GC allocation reporting:

```bash
mvn -Pjmh test-compile exec:exec "-Djmh.prof=gc"
```

Representative benchmark records are kept in
[docs/benchmarks.md](docs/benchmarks.md).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
