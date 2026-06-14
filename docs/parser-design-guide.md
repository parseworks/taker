# Parser Design and Implementation Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Understanding Parser Concepts](#understanding-parser-concepts)
3. [The Parser Design Process](#the-parser-design-process)
4. [Implementing Parsers with Taker](#implementing-parsers-with-Taker)
5. [Design Patterns for Parsers](#design-patterns-for-parsers)
6. [Testing and Debugging Parsers](#testing-and-debugging-parsers)
7. [Performance Considerations](#performance-considerations)
8. [Additional Design Notes](#additional-design-notes)

## Introduction

Use this guide to design and implement parsers with Taker. Apply the steps and patterns here to build parsers for configuration files, domain-specific languages, and full programming languages in a consistent, repeatable way.

## Understanding Parser Concepts

Before diving into parser design, it's important to understand some fundamental concepts:

### What is a Parser?

A parser is a program that takes input (usually text) and transforms it into a structured representation. This process typically involves:

1. **Lexical Analysis**: Breaking the input into tokens (lexemes)
2. **Syntactic Analysis**: Organizing tokens according to a grammar
3. **Semantic Analysis**: Interpreting the meaning of the organized tokens

### Types of Parsers

There are several types of parsers, each with its own strengths and weaknesses:

- **Recursive Descent Parsers**: Top-down parsers that use a set of recursive procedures
- **LL Parsers**: Left-to-right, Leftmost derivation parsers
- **LR Parsers**: Left-to-right, Rightmost derivation parsers
- **Parser Combinators**: Composable parsers built from smaller parsers

Taker is a parser combinator library, which means it allows you to build complex parsers by combining simpler ones.

### Grammar Notation

Parsers are often defined using formal grammar notations:

```
expr   ::= term ('+' term | '-' term)*
term   ::= factor ('*' factor | '/' factor)*
factor ::= number | '(' expr ')'
number ::= [0-9]+
```

This notation describes the structure of valid inputs and how they should be parsed.

## The Parser Design Process

Designing a parser involves several steps:

### 1. Define the Problem

Clearly define what you're trying to parse:

- What is the format of the input?
- What is the expected output?
- What are the edge cases?

### 2. Define the Grammar

Express the syntax of your language using a formal grammar:

- Identify the terminal symbols (tokens)
- Define the non-terminal symbols (grammar rules)
- Specify the production rules

### 3. Plan the Parser Structure

Decide how to structure your parser:

- Will you separate lexical analysis from syntactic analysis?
- How will you handle operator precedence?
- How will you represent the parsed data?

### 4. Implement the Parser

Implement the parser using your chosen approach (in this case, parser combinators with Taker).

### 5. Test and Refine

Test your parser with various inputs, including edge cases, and refine it as needed.

## Implementing Parsers with Taker

Taker provides a rich set of tools for implementing parsers:

### Basic Parsers

Start with basic parsers for simple elements:

```java
// Taker for a single digit
Taker<Integer> digit = chr(Character::isDigit)
    .map(c -> Character.getNumericValue(c));

// Taker for a specific string
Taker<String> keyword = string("if");

// Taker for a regular expression
Taker<String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
```

### Combining Parsers

Combine basic parsers to create more complex ones:

```java
// Taker for a number (one or more digits)
Taker<Integer> number = digit.oneOrMore()
    .map(digits -> {
        int result = 0;
        for (int d : digits) {
            result = result * 10 + d;
        }
        return result;
    });

// Taker for a key-value pair
Taker<KeyValue> keyValueParser = identifier
    .thenSkip(chr('='))
    .then(identifier)
    .map(key -> value -> new KeyValue(key, value));
```

### Recursive Parsers

Use recursive parsers for nested structures:

```java
// Create references for recursive parsers
Taker<Expression> expr = Taker.ref();
Taker<Expression> term = Taker.ref();
Taker<Expression> factor = Taker.ref();

// Define the factor parser (numbers or parenthesized expressions)
factor.set(
    number.map(n -> (Expression) new NumberExpression(n))
        .or(chr('(').skipThen(expr).thenSkip(chr(')')))
);

// Define the term parser (factors with multiplication/division)
term.set(
    factor.chainLeftOneOrMore(
        oneOf(
            chr('*').as((a, b) -> new BinaryExpression(a, Operator.MULTIPLY, b)),
            chr('/').as((a, b) -> new BinaryExpression(a, Operator.DIVIDE, b))
        )
    )
);

// Define the expression parser (terms with addition/subtraction)
expr.set(
    term.chainLeftOneOrMore(
        oneOf(
            chr('+').as((a, b) -> new BinaryExpression(a, Operator.ADD, b)),
            chr('-').as((a, b) -> new BinaryExpression(a, Operator.SUBTRACT, b))
        )
    )
);
```

## Design Patterns for Parsers

Several design patterns are commonly used in parser implementation:

### The Visitor Pattern

The Visitor pattern is useful for operations on parsed data:

```java
interface Expression {
    <T> T accept(ExpressionVisitor<T> visitor);
}

interface ExpressionVisitor<T> {
    T visit(NumberExpression expr);
    T visit(BinaryExpression expr);
}

class EvaluationVisitor implements ExpressionVisitor<Integer> {
    @Override
    public Integer visit(NumberExpression expr) {
        return expr.getValue();
    }

    @Override
    public Integer visit(BinaryExpression expr) {
        int left = expr.getLeft().accept(this);
        int right = expr.getRight().accept(this);
        switch (expr.getOperator()) {
            case ADD: return left + right;
            case SUBTRACT: return left - right;
            case MULTIPLY: return left * right;
            case DIVIDE: return left / right;
            default: throw new IllegalStateException("Unknown operator");
        }
    }
}
```

### The Builder Pattern

The Builder pattern can be used to construct complex objects during parsing:

```java
class JsonObjectBuilder {
    private final Map<String, Object> map = new HashMap<>();

    public JsonObjectBuilder add(String key, Object value) {
        map.put(key, value);
        return this;
    }

    public Map<String, Object> build() {
        return new HashMap<>(map);
    }
}

Taker<Map<String, Object>> jsonObject = chr('{')
    .skipThen(
        jsonString
            .thenSkip(chr(':'))
            .then(jsonValue)
            .map(key -> value -> (Map.Entry<String, Object>) new AbstractMap.SimpleEntry<>(key, value))
            .zeroOrMoreSeparatedBy(chr(','))
    )
    .thenSkip(chr('}'))
    .map(entries -> {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        for (Map.Entry<String, Object> entry : entries) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    });
```

### The Factory Pattern

The Factory pattern can be used to create parsers for different types:

```java
class ParserFactory {
    public static Taker<Expression> createExpressionParser() {
        // Implementation
    }

    public static Taker<Statement> createStatementParser() {
        // Implementation
    }

    public static Taker<Program> createProgramParser() {
        // Implementation
    }
}
```

## Testing and Debugging Parsers

Testing and debugging parsers is crucial for ensuring correctness:

### Unit Testing

Write unit tests for each parser component:

```java
@Test
public void testNumberParser() {
    Taker<Integer> parser = Numeric.integer;
    
    // Test valid inputs
    assertEquals(42, parser.parse(Input.of("42")).value());
    assertEquals(0, parser.parse(Input.of("0")).value());
    
    // Test invalid inputs
    assertFalse(parser.parse(Input.of("abc")).matches());
}
```

### Integration Testing

Test the integration of parser components:

```java
@Test
public void testExpressionParser() {
    Taker<Expression> parser = expr;
    
    // Test simple expressions
    Expression result1 = parser.parse(Input.of("1+2")).value();
    assertEquals(3, result1.evaluate());
    
    // Test complex expressions
    Expression result2 = parser.parse(Input.of("1+2*3")).value();
    assertEquals(7, result2.evaluate());
}
```

### Debugging Techniques

Use these techniques for debugging parsers:

1. **Add tracing**: Add logging to see what's happening during parsing
2. **Use custom error messages**: Provide meaningful error messages for failures
3. **Test incrementally**: Build and test your parser incrementally
4. **Label failures**: Use `label(...)` for grammar rules and
   `expecting(...)` for specific expected tokens or values.
5. **Visualize the parse tree**: Create a visualization of the parse tree for complex inputs

## Performance Considerations

This guide focuses on parser design. For exact parser semantics, use
[api-contract.md](api-contract.md); for recorded performance numbers, use
[benchmarks.md](benchmarks.md).

Design choices that usually matter most:

1. Tokenize long raw character spans with scanner primitives such as
   `collectChars`, `skipWhile`, `countWhile`, and `takeUntil`.
2. Decide whitespace policy early. Use `TokensParser` or explicit token helpers
   when most grammar rules should ignore the same whitespace or comments.
3. Use `label(...)` on grammar rules and `expecting(...)` on specific tokens so
   errors point to both the rule and the low-level cause.
4. Order alternatives from most specific to least specific when they overlap.
5. Use `commit(...)` after a branch becomes unambiguous.

## Additional Design Notes

Keep large examples executable where possible. The normal test suite includes
realistic examples under
`src/test/java/io/github/parseworks/taker/examples`, including a sectioned
config parser, a recursive JSON-like value parser, and a practical TOML parser.
The user guide links to those examples; this document explains how to design
parsers like them.
