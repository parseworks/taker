# Taker Advanced User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Advanced Parser Combinators](#advanced-parser-combinators)
   1. [Chain Operations](#chain-operations)
   2. [Recursive Parsers](#recursive-parsers)
   3. [Repetition Nuances](#repetition-nuances)
   4. [`takeWhile` and `zeroOrMoreUntil`](#takewhile-and-zeroormoreuntil)
   5. [Negation and Validation](#negation-and-validation)
3. [Error Handling Strategies](#error-handling-strategies)
   1. [Custom Error Messages](#custom-error-messages)
   2. [Error Types](#error-types)
   3. [Recovery Strategies](#recovery-strategies)
4. [Performance Tips](#performance-tips)
5. [Working with Complex Grammars](#working-with-complex-grammars)
   1. [Operator Precedence](#operator-precedence)
   2. [Left vs. Right Recursion](#left-vs-right-recursion)
   3. [Handling Ambiguity](#handling-ambiguity)
6. [Advanced Examples](#advanced-examples)
   1. [JSON Parser](#json-parser)
   2. [Expression Evaluator with Variables](#expression-evaluator-with-variables)
   3. [Custom DSL Parser](#custom-dsl-parser)
7. [Integration Patterns](#integration-patterns)
   1. [Combining with Other Libraries](#combining-with-other-libraries)
   2. [Testing Strategies](#testing-strategies)
   3. [Modular Parser Design](#modular-parser-design)
8. [Troubleshooting](#troubleshooting)
   1. [Common Pitfalls](#common-pitfalls)
   2. [Debugging Techniques](#debugging-techniques)

## Introduction

Build on the concepts from the [user guide](user-guide.md) and explore advanced features and techniques in Taker. Use this guide when you already understand the fundamentals of parser combinators and want to apply the full power of the library in complex scenarios.

## Advanced Parser Combinators

### Chain Operations

Handling operator precedence and associativity is common in expression parsing. `chainLeftZeroOrMore` and `chainRightZeroOrMore` simplify this by applying a binary operator between parsed elements.

#### left-associativity with `chainLeftZeroOrMore`
Useful for addition, subtraction, etc., where `1+2+3` should be `(1+2)+3`.

```java
// Define a number parser
Taker<Integer> number = Numeric.integer;

Taker<Integer> addition = number.chainLeftZeroOrMore(
    chr('+').as((a, b) -> a + b),
    0
);
```

#### right-associativity with `chainRightZeroOrMore`
Useful for exponentiation, where `2^3^2` should be `2^(3^2)`.

```java
Taker<Integer> power = number.chainRightZeroOrMore(
    chr('^').as((a, b) -> (int)Math.pow(a, b)),
    1
);
```

### Recursive Parsers

For nested structures (JSON, expressions), use `Taker.ref()`. This creates a placeholder that you `set()` later, allowing the parser to refer to itself.

```java
Taker<Integer> number = Numeric.integer;
Taker<Expr> expr = Taker.ref();

// Parens: ( expr )
Taker<Expr> parens = chr('(')
    .skipThen(expr)
    .thenSkip(chr(')'));

// A factor is a number OR a parenthesized expression
Taker<Expr> factor = number.map(Literal::new).or(parens);

// Close the loop
expr.set(factor); 
```

### Repetition Nuances

While `zeroOrMore` and `oneOrMore` are common, you often need stricter bounds.

- `repeat(n)`: Exactly `n` times.
- `repeat(min, max)`: Between `min` and `max` times.
- `repeatAtLeast(n)`: At least `n` times.
- `repeatAtMost(n)`: At most `n` times.
- `zeroOrMoreUntil(terminator)`: Consume items until the `terminator` parser matches.

### `takeWhile` and `zeroOrMoreUntil`

`takeWhile` consumes one or more consecutive input characters while a
`CharPredicate` succeeds. It is a scanner primitive: use it when the grammar is
matching a raw character span rather than a repeated structured parser.

```java
Taker<String> word = takeWhile(CharPredicate.asciiLetter);

// Equivalent name when the intent is text collection:
Taker<String> word2 = collectChars(CharPredicate.asciiLetter);

// For ignored text, skip without creating a String:
Taker<Void> spaces = skipWhile(CharPredicate.horizontalWhitespace);
```

Avoid `chr(predicate).oneOrMore()` or `chr(predicate).collectString()` for long
raw character runs. Those forms are useful when each character parser is part of
a larger composition, but they allocate per parser step.

`zeroOrMoreUntil` is useful for consuming items until a specific terminator is reached:

```java
// Parse everything up to, but not including, a semicolon
Taker<String> content = takeUntil(CharPredicate.is(';'));
```

### Negation and Validation

#### not

The `not` method creates a parser that succeeds if the provided parser fails, and fails if it succeeds. It does **not** consume any input (it's a zero-width assertion).

```java
// Parse any character that is not a digit
// We use not() to check the condition, then any() to actually consume the character.
Taker<Character> notDigit = not(chr(CharPredicate.asciiDigit)).skipThen(any());
```

#### isNot

The `isNot` method creates a parser that succeeds if the current input item is not equal to the provided value:

```java
// Parse any character except 'x'
Taker<Character> notX = isNot('x');
```

#### Validation Errors

For custom validation:

```java
// Parse a positive number
Taker<Integer> positiveNumber = number.flatMap(n -> {
    if (n > 0) {
        return pure(n);
    } else {
        return fail("positive number");
    }
});
```

## Error Handling Strategies

### Custom Error Messages

Taker provides several ways to create custom error messages:

#### Using fail

```java
// Create a parser that fails with a custom error message
Taker<String> customError = fail("Expected a specific pattern");
```

#### Using expecting

```java
// Label a parser with a domain-specific expectation.
Taker<Integer> numberOrError = number.expecting("number");
```

### Error Types

Taker provides different ways to label or produce failures:

```java
// Syntax error (input doesn't match expected pattern)
Taker<String> syntaxError = fail("Expected a valid syntax");

// Validation error (input parsed but failed validation)
Taker<String> validationError = fail("Expected a valid value");

// Generic error
Taker<String> genericError = fail("Something went wrong");
```

### Recovery Strategies

#### Using or

The `or` method provides a way to try an alternative parser if the first one fails:

```java
// Try to parse a number, or a string if that fails
Taker<Object> numberOrString = number.map(n -> (Object)n)
    .or(string("null").map(s -> (Object)null));
```

#### Using orElse

The `orElse` method provides a way to return a default value if the parser fails:

```java
// Try to parse a number, or return 0 if that fails
Taker<Integer> numberOrZero = number.orElse(0);
```

#### Using optional

The `optional` method creates a parser that optionally applies the parser. It returns an `Optional<A>`:

```java
// Parse an optional sign followed by a number
// then() returns an ApplyBuilder, which allows combining results
Taker<Integer> signedNumber = chr('-').optional()
    .then(number)
    .map(optSign -> num -> optSign.isPresent() ? -num : num);
```

## Performance Tips

1. **Static Reuse**: Define common parsers (keywords, delimiters) as `static final` fields. Creating parsers on-the-fly inside a loop is a major performance killer.
2. **Scanner Primitives for Character Runs**: Use `collectChars`/`takeWhile` for matched text, `skipWhile` for ignored text, and `countWhile` when only the length matters. These avoid the per-character allocation cost of repeated `chr(predicate)` parsers.
3. **Deterministic Choices**: In `oneOf`, ensure your alternatives are as distinct as possible. Overlapping of terms can result in early termination, as the parser will assume that the partial parse indicates an error.
4. **Be careful with `trim()`**: It's convenient but adds parser-result allocation. Apply it at the token level rather than around every single character parser.
5. **Regular Expressions**: `Lexical.regex` is backed by standard Java `Pattern`. It's fast for tokenization but avoid complex, nested groups if a scanner primitive would suffice.

## Working with Complex Grammars

### Operator Precedence

Operator precedence can be handled by creating separate parsers for each precedence level:

```java
// Expression grammar with operator precedence:
// expr   ::= term ('+' term | '-' term)*
// term   ::= factor ('*' factor | '/' factor)*
// factor ::= number | '(' expr ')'

// Create references for recursive parsers
Taker<Integer> expr = Taker.ref();
Taker<Integer> term = Taker.ref();
Taker<Integer> factor = Taker.ref();

// Factor can be a number or an expression in parentheses
Taker<Integer> numberFactor = number;
Taker<Integer> parenFactor = chr('(')
    .skipThen(expr)
    .thenSkip(chr(')'));

factor.set(
    oneOf(numberFactor, parenFactor)
);

// Term handles multiplication and division (higher precedence)
Taker<BinaryOperator<Integer>> mulOp = chr('*')
    .as((a, b) -> a * b);
Taker<BinaryOperator<Integer>> divOp = chr('/')
    .as((a, b) -> a / b);

term.set(
    factor.chainLeftZeroOrMore(oneOf(mulOp, divOp), 0)
);

// Expression handles addition and subtraction (lower precedence)
Taker<BinaryOperator<Integer>> addOp = chr('+')
    .as((a, b) -> a + b);
Taker<BinaryOperator<Integer>> subOp = chr('-')
    .as((a, b) -> a - b);

expr.set(
    term.chainLeftZeroOrMore(oneOf(addOp, subOp), 0)
);
```

### Left vs. Right Recursion

As mentioned earlier, left recursion can cause infinite loops. Here's a more detailed example of how to convert left recursion to right recursion:

```java
// Left-recursive grammar (problematic):
// expr ::= expr '+' term | term

// Right-recursive equivalent:
// expr ::= term ('+' expr)?

// Implementation with right recursion:
Taker<Integer> expr = Taker.ref();
Taker<Integer> term = number;

expr.set(
    term.then(
        chr('+').skipThen(expr).optional()
    ).map(t -> rest -> rest.map(value -> t + value).orElse(t))
);
```

### Handling Ambiguity

Ambiguity in grammars can lead to unexpected parsing results. Taker uses ordered choice (via `or` or `oneOf`), which means the first matching parser wins:

```java
// This parser will always choose the first matching alternative
Taker<String> ambiguous = oneOf(string("if"), string("ifelse"));

// For input "ifelse", it will parse "if" and leave "else" unparsed
```

## Advanced Examples

### JSON Parser

Here's a simplified example of a JSON parser:

```java
// Create references for recursive parsers
Taker<Object> jsonValue = Taker.ref();
Taker<Map<String, Object>> jsonObject = Taker.ref();
Taker<List<Object>> jsonArray = Taker.ref();

// Taker for JSON strings
Taker<String> jsonString = escapedString(
    '"',
    '\\',
    Map.of('"', '"', '\\', '\\', '/', '/', 'b', '\b', 'f', '\f', 'n', '\n', 'r', '\r', 't', '\t')
);

// Taker for JSON numbers
Taker<Double> jsonNumber = regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")
    .map(Double::parseDouble);

// Taker for JSON booleans
Taker<Boolean> jsonBoolean = string("true").as(true)
    .or(string("false").as(false));

// Taker for JSON null
Taker<Object> jsonNull = string("null").as(null);

// Taker for JSON arrays
jsonArray.set(
    chr('[')
        .skipThen(jsonValue.zeroOrMoreSeparatedBy(chr(',')))
        .thenSkip(chr(']'))
        .map(values -> (List<Object>) new ArrayList<>(values))
);

// Taker for JSON objects
jsonObject.set(
    chr('{')
        .skipThen(
            jsonString
                .thenSkip(chr(':'))
                .then(jsonValue)
                .map(key -> value -> (Map.Entry<String, Object>) new AbstractMap.SimpleEntry<>(key, value))
                .zeroOrMoreSeparatedBy(chr(','))
        )
        .thenSkip(chr('}'))
        .map(entries -> {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : entries) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        })
);

// Set the JSON value parser to handle all JSON value types
jsonValue.set(
    oneOf(
        jsonString.map(s -> (Object) s),
        jsonNumber.map(n -> (Object) n),
        jsonBoolean.map(b -> (Object) b),
        jsonNull,
        jsonArray.map(a -> (Object) a),
        jsonObject.map(o -> (Object) o)
    )
);
```

### Expression Evaluator with Variables

Here's an example of an expression evaluator that can handle variables:

```java
enum BinOp {
    ADD { BinaryOperator<Integer> op() { return Integer::sum; } },
    SUB { BinaryOperator<Integer> op() { return (a, b) -> a - b; } },
    MUL { BinaryOperator<Integer> op() { return (a, b) -> a * b; } },
    DIV { BinaryOperator<Integer> op() { return (a, b) -> a / b; } };
    abstract BinaryOperator<Integer> op();
}

// Create a parser for expressions with variables
Taker<UnaryOperator<Integer>> expr = Taker.ref();

// Taker for variables (x)
Taker<UnaryOperator<Integer>> var = chr('x')
    .map(x -> v -> v);  // Identity function that returns the variable value

// Taker for numbers (constants)
Taker<UnaryOperator<Integer>> num = regex("\\d+")
    .map(Integer::parseInt)
    .map(i -> v -> i);  // Constant function that ignores the variable value

// Taker for binary operators
Taker<BinOp> binOp = oneOf(
    chr('+').as(BinOp.ADD),
    chr('-').as(BinOp.SUB),
    chr('*').as(BinOp.MUL),
    chr('/').as(BinOp.DIV)
);

// Taker for binary expressions
Taker<UnaryOperator<Integer>> binExpr = chr('(')
    .skipThen(expr)
    .then(binOp)
    .then(expr.thenSkip(chr(')')))
    .map(left -> op -> right -> (Integer x) -> op.op().apply(left.apply(x), right.apply(x)));

// Set the expression parser to handle variables, numbers, and binary expressions
expr.set(oneOf(var, num, binExpr));

// Use the parser to evaluate expressions with variables
UnaryOperator<Integer> evaluator = expr.parse(Input.of("(x*(x+1))")).value();
int result = evaluator.apply(5);  // Evaluates the expression with x = 5
System.out.println(result);  // Output: 30
```

### Custom DSL Parser

Here's an example of a parser for a simple domain-specific language (DSL):

```java
// Define the AST classes
interface Statement {}

class PrintStatement implements Statement {
    private final String message;
    
    public PrintStatement(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}

class AssignStatement implements Statement {
    private final String variable;
    private final int value;
    
    public AssignStatement(String variable, int value) {
        this.variable = variable;
        this.value = value;
    }
    
    public String getVariable() {
        return variable;
    }
    
    public int getValue() {
        return value;
    }
}

class IfStatement implements Statement {
    private final String condition;
    private final List<Statement> thenBranch;
    private final List<Statement> elseBranch;
    
    public IfStatement(String condition, List<Statement> thenBranch, List<Statement> elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public List<Statement> getThenBranch() {
        return thenBranch;
    }
    
    public List<Statement> getElseBranch() {
        return elseBranch;
    }
}

// Create the parsers
Taker<Statement> statement = Taker.ref();
Taker<List<Statement>> statements = Taker.ref();

// Taker for identifiers
Taker<String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");

// Taker for numbers
Taker<Integer> number = regex("\\d+").map(Integer::parseInt);

// Taker for strings
Taker<String> stringLiteral = escapedString('"', '\\', Map.of('"', '"', '\\', '\\'));

// Taker for print statements
Taker<Statement> printStatement = string("print")
    .skipThen(stringLiteral)
    .thenSkip(chr(';'))
    .map(PrintStatement::new);

// Taker for assignment statements
Taker<Statement> assignStatement = identifier
    .thenSkip(string("="))
    .then(number)
    .thenSkip(chr(';'))
    .map(var -> val -> new AssignStatement(var, val));

// Taker for if statements
Taker<Statement> ifStatement = string("if")
    .skipThen(chr('('))
    .skipThen(identifier)
    .thenSkip(chr(')'))
    .thenSkip(chr('{'))
    .then(statements)
    .thenSkip(chr('}'))
    .then(
        string("else")
            .skipThen(chr('{'))
            .skipThen(statements)
            .thenSkip(chr('}'))
            .optional()
    )
    .map(cond -> thenStmts -> (Optional<List<Statement>> elseStmts) -> 
        new IfStatement(cond, thenStmts, elseStmts.orElse(Collections.emptyList()))
    );

// Set the statement parser
statement.set(
    oneOf(
        printStatement,
        assignStatement,
        ifStatement
    )
);

// Set the statements parser
statements.set(
    statement.zeroOrMore().map(stmts -> {
        List<Statement> result = new ArrayList<>();
        for (Statement stmt : stmts) {
            result.add(stmt);
        }
        return result;
    })
);

// Parse a simple program
String program = "x = 5; if (x) { print \"x is true\"; } else { print \"x is false\"; }";
List<Statement> ast = statements.parse(Input.of(program)).value();
```

## Integration Patterns

### Combining with Other Libraries

Taker can be combined with other libraries to create more powerful parsing solutions:

```java
// Combine with Jackson for JSON processing
Taker<JsonNode> jsonParser = /* JSON parser implementation */;
ObjectMapper mapper = new ObjectMapper();

Taker<MyObject> myObjectParser = jsonParser.map(jsonNode -> {
    try {
        return mapper.treeToValue(jsonNode, MyObject.class);
    } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
    }
});
```

### Testing Strategies

Here are some strategies for testing parsers:

```java
// Test a parser with various inputs
@Test
public void testNumberParser() {
    Taker<Integer> parser = number;
    
    // Test valid inputs
    assertEquals(42, parser.parse(Input.of("42")).value());
    assertEquals(0, parser.parse(Input.of("0")).value());
    assertEquals(123456789, parser.parse(Input.of("123456789")).value());
    
    // Test invalid inputs
    assertTrue(!parser.parse(Input.of("abc")).matches());
    assertTrue(!parser.parse(Input.of("")).matches());
    assertTrue(parser.parse(Input.of("42a")).matches());  // Parses "42", leaves "a"
    
    // Test with parseAll (consumes all input)
    assertTrue(parser.parseAll(Input.of("42")).matches());
    assertTrue(!parser.parseAll(Input.of("42a")).matches());  // Fails because "a" is left
}
```

### Modular Parser Design

For complex parsers, it's often beneficial to use a modular design:

```java
// Define a parser module interface
interface ParserModule<T> {
    Taker<T> getParser();
}

// Implement modules for different parts of the grammar
class ExpressionModule implements ParserModule<Expression> {
    private final TermModule termModule;
    
    public ExpressionModule(TermModule termModule) {
        this.termModule = termModule;
    }
    
    @Override
    public Taker<Expression> getParser() {
        return termModule.getParser().chainLeftZeroOrMore(
            oneOf(
                chr('+').as((a, b) -> new BinaryExpression(a, Operator.ADD, b)),
                chr('-').as((a, b) -> new BinaryExpression(a, Operator.SUBTRACT, b))
            ),
            null
        );
    }
}

class TermModule implements ParserModule<Expression> {
    private final FactorModule factorModule;
    
    public TermModule(FactorModule factorModule) {
        this.factorModule = factorModule;
    }
    
    @Override
    public Taker<Expression> getParser() {
        return factorModule.getParser().chainLeftZeroOrMore(
            oneOf(
                chr('*').as((a, b) -> new BinaryExpression(a, Operator.MULTIPLY, b)),
                chr('/').as((a, b) -> new BinaryExpression(a, Operator.DIVIDE, b))
            ),
            null
        );
    }
}

// Use dependency injection to wire up the modules
FactorModule factorModule = new FactorModule();
TermModule termModule = new TermModule(factorModule);
ExpressionModule expressionModule = new ExpressionModule(termModule);
Taker<Expression> expressionParser = expressionModule.getParser();
```

## Troubleshooting

### Common Pitfalls

#### Infinite Recursion

One common pitfall is infinite recursion, which can happen when a parser refers to itself directly:

```java
// This will cause infinite recursion in most parser libraries
// Taker will detect and mitigate this specific example
Taker<String> badRecursion = Taker.ref();
badRecursion.set(badRecursion.or(string("x")));

// Fix: Ensure the recursive parser makes progress before recursing
Taker<String> goodRecursion = Taker.ref();
goodRecursion.set(string("x").then(goodRecursion).optional().map(opt -> "x" + opt.orElse("")));
```

#### Greedy Matching

Another common pitfall is greedy matching, which can consume more input than intended:

```java
// This will greedily consume all characters until the end of input
Taker<String> greedy = regex(".*");

// Fix: Use a non-greedy quantifier or be more specific
Taker<String> nonGreedy = regex(".*?");
Taker<String> specific = regex("[a-zA-Z]+");
```

### Avoiding Regex for Common Patterns

While `regex()` is powerful, it carries overhead and can be less readable for simple patterns. Taker provides efficient alternatives:

- **Identifiers**: Use a named parser for identifiers instead of repeating `regex("[a-zA-Z_][a-zA-Z0-9_]*")`.
- **Hexadecimal**: Use `Numeric.hex` instead of `regex("[0-9a-fA-F]+")`.
- **Custom character sets**: Use `satisfy()` with predicates.

```java
// Instead of regex:
Taker<String> regexId = regex("[a-zA-Z_][a-zA-Z0-9_]*");

// Use scanner primitives for simple character runs:
CharPredicate idStart = CharPredicate.asciiLetter.or(CharPredicate.is('_'));
CharPredicate idPart = CharPredicate.asciiLetterOrDigit.or(CharPredicate.is('_'));
Taker<String> id = chr(idStart)
    .then(collectChars(idPart).orElse(""))
    .map((first, rest) -> first + rest);

// Or build your own:
Taker<String> myPattern = chr(idStart)
    .then(collectChars(idPart).orElse(""))
    .map((first, rest) -> first + rest);
```

### Order of Alternatives

The order of alternatives in `oneOf` (and `or`) can affect the parsing result:

```java
// This will always parse "if" and never "ifelse"
Taker<String> badOrder = oneOf(string("if"), string("ifelse"));

// Fix: Order from most specific to least specific
Taker<String> goodOrder = oneOf(string("ifelse"), string("if"));
```

### Debugging Techniques

#### Tracing

You can add tracing to your parsers to see what's happening during parsing:

```java
// Add tracing to a parser
Taker<Integer> tracedParser = number.map(n -> {
    System.out.println("Parsed number: " + n);
    return n;
});
```
#### Log to output

The `systemOut` method allows you to log the parsing process to the system output, which is invaluable for debugging complex combinators:

```java
Taker<String> myParser = string("foo").or(string("bar")).systemOut();
```

#### Use `expecting` to redefine error messages

The `expecting` method allows you to provide a more user-friendly name for a parser, which will be used in error messages when the parser fails to match.

```java
Taker<String> identifier = regex("[a-zA-Z_][a-zA-Z0-9_]*")
    .expecting("an identifier");

// If this fails, the error message will say "Expected an identifier" 
// instead of a generic message or the underlying pattern.
```


#### Custom Error Messages

Custom error messages can help identify where parsing is failing:

```java
// Add custom error messages
Taker<Integer> withErrorMessage = number.expecting("number");
```

#### Incremental Development

Build your parser incrementally, testing each part as you go:

```java
// Start with simple parsers
Taker<String> identifier = regex("[a-zA-Z_][a-zA-Z0-9_]*");
assertTrue(identifier.parse(Input.of("abc123")).matches());

// Add more complex parsers
Taker<Statement> assignStatement = identifier
    .thenSkip(string("="))
    .then(number)
    .thenSkip(chr(';'))
    .map(var -> val -> new AssignStatement(var, val));
assertTrue(assignStatement.parse(Input.of("abc123=42;")).matches());
```

By following these advanced techniques and patterns, you can leverage the full power of Taker to create sophisticated parsers for a wide range of applications.
