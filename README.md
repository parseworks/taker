# Taker: A Fluent Parser Combinator Library for Java

Taker is a lean, powerful, and developer-friendly parser combinator library for Java 21+. It strikes a balance between the purity of functional combinators and the pragmatic performance needs of real-world tooling.

## Key Features

- **Fluent API**: Highly ergonomic DSL for Java using a functional approach.
- **N-ary Composition**: Use `.then()` and `ApplyBuilder` to cleanly map multiple values without deep nesting.
- **Performance-Oriented**: Includes greedy primitives like `takeWhile`, `takeUntil`, and `regex` for efficient lexing.
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
import static io.github.parseworks.taker.Taker.*;
import static io.github.parseworks.taker.parsers.Lexical.*;
import io.github.parseworks.taker.Taker;

Taker<String> identifier = takeWhile(Character::isLetterOrDigit);
Taker<String> kvPair = identifier
    .thenSkip(trim(chr(':')))
    .then(takeWhile(c -> c != '\n'))
    .map((key, value) -> key + " => " + value);

String output = kvPair.parse("User: Bob").value();
// Output: User => Bob
```

### Recursive Grammars

Handle nested structures with ease:

```java
public static final Taker<Element> element = Taker.ref();

static {
    element.set(
        oneOf(
            tagParser,
            textParser
        )
    );
}
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

The intended public API and parser semantics are documented in
[docs/api-contract.md](docs/api-contract.md).

## License

This project does not currently declare a license. Add one before distributing or publishing the library.
