# Taker API Contract

This document defines the intended public surface and parser semantics for Taker.
It is the compatibility guide for future releases: if behavior listed here changes,
that change should be deliberate, tested, and called out in release notes.

## Audience

Taker is a parser-combinator library for Java 21+. The API favors readable names
and fluent composition over dense parser-combinator terminology. Method names such
as `zeroOrMore`, `oneOrMore`, `thenSkip`, and `skipThen` are intentionally more
descriptive than traditional names like `many`, `many1`, `left`, or `right`.

## Public Packages

The JPMS module exports these packages:

- `io.github.parseworks.taker`
- `io.github.parseworks.taker.parsers`
- `io.github.parseworks.taker.results`

Classes under `io.github.parseworks.taker.impl` are internal implementation
details. They must not be exported from the module and should not be referenced
by user code.

## Stable Public API

The following types are intended to be stable public API:

- `Taker<A>`: the core parser type.
- `Input`: immutable input cursor abstraction.
- `TextInput`: input extension that can report line, column, and snippets.
- `Result<A>`: parser result abstraction.
- `Failure<A>`: result subtype for failures.
- `Located<A>`: value wrapper with zero-based source offsets.
- `ResultType`: result category, currently `MATCH`, `NO_MATCH`, and `PARTIAL`.
- `CharPredicate`: character predicate helper type.
- `ApplyBuilder`: fluent sequence builder returned by `Taker.then`.
- `ApplyBuilder.Func3` through `ApplyBuilder.Func8`: arity-specific function
  interfaces used by `ApplyBuilder` map overloads.
- `io.github.parseworks.taker.parsers.*`: built-in parser libraries.
- `io.github.parseworks.taker.results.*`: concrete result records for low-level
  parser authors.

The following types are visible in source form but should be treated as
provisional until the API is tightened:

- Concrete input implementations under `impl.inputs`.
- `Pair` under `impl`.

## Input Contract

`Input` represents a position in a character sequence.

- `Input.of(CharSequence)` creates a random-access input.
- `Input` instances are logically immutable. Calling `next()` or `skip(int)`
  returns a new cursor and does not mutate the original cursor.
- `position()` is a zero-based absolute character offset.
- `current()` returns the current character and is only valid when `isEof()` is
  false.
- `next()` advances by one character and is only valid when `isEof()` is false.
- `skip(offset)` advances by `offset` characters.
- `hasMore()` is equivalent to `!isEof()`.
- `Input.lowercase(input)` and `Input.uppercase(input)` return wrapper inputs
  that transform character access without changing positions.

`TextInput` additionally exposes line/column and snippet formatting. Error
messages may use this information when available. Line and column reporting is
user-facing and should remain one-based.

## Result Contract

A parser returns a `Result<A>`.

- A successful parse returns `matches() == true`, `type() == MATCH`, a `value()`,
  and the next `input()` cursor.
- A failed parse returns `matches() == false`.
- Calling `value()` on a failure throws an exception containing the formatted
  error message.
- `map` transforms only successful values and preserves failures.
- `cast` is a type-level convenience for preserving failures across generic
  parser boundaries.
- `handle(success, failure)` dispatches according to `matches()`.
- `toOptional()` returns the successful value or `Optional.empty()`.
- `errorOptional()` returns the formatted error for failures or
  `Optional.empty()` for success.

`Failure<A>` exposes structured failure information:

- `expected()` is the current expected label.
- `cause()` is an optional nested failure.
- `combinedFailures()` is an optional list of failures from alternative parsers.
- `error()` formats a user-facing diagnostic lazily.

## Failure Types

Taker distinguishes two failure categories:

- `NO_MATCH`: the parser did not match. This failure allows alternatives to be
  tried by choice combinators.
- `PARTIAL`: a committed failure. This indicates that the parser had progressed
  far enough that alternatives should not be tried.

Failure type is about backtracking control, not whether the underlying `Input`
object was mutated. Inputs are immutable; a failed result may still report an
advanced error cursor.

## Parsing Entry Points

`parse(Input)` and `parse(CharSequence)` apply a parser without requiring the
entire input to be consumed.

`parseAll(Input)` and `parseAll(CharSequence)` require a successful parser to end
at EOF. If a parser succeeds but leaves trailing input, `parseAll` returns a
`PARTIAL` failure expecting end of input.

`stream(Input)` and `iterateParse(Input)` scan through input and yield each
successful parse. If the parser fails at the current position, scanning advances
by one character and tries again. Parsers used with these methods must consume
input on success, or callers risk non-terminating iteration.

## Core Parser Semantics

### `pure`

`Combinators.pure(value)` always succeeds without consuming input.

### `take`

`Lexical.take(predicate)` matches exactly one character when the predicate succeeds.
It fails at EOF or when the predicate is false.

### `takeWhile`

`Lexical.takeWhile(predicate)` greedily consumes one or more matching characters.
It fails if no characters match.

Use `.orElse("")` when a zero-length match is desired.

### `collectChars`

`Lexical.collectChars(predicate)` is an explicit alias for `takeWhile(predicate)`.
It greedily consumes one or more matching input characters and returns the
matched text.

Prefer `collectChars(predicate)` or `takeWhile(predicate)` over
`chr(predicate).collectString()` when the grammar is simply accumulating
consecutive raw input characters. The scanner form avoids per-character parser
result allocation.

### `skipWhile`

`Lexical.skipWhile(predicate)` greedily consumes zero or more matching characters
and returns `null`.

It always succeeds and does not allocate a matched string. Use it for ignored
input such as whitespace or comments when the skipped text is not needed.

### `countWhile`

`Lexical.countWhile(predicate)` greedily consumes zero or more matching characters
and returns the number of consumed characters.

It always succeeds and does not allocate a matched string.

### `takeUntil`

`Lexical.takeUntil(predicate)` and `Lexical.takeUntil(String)` consume characters
until a terminator is found. The terminator is not consumed. If no terminator is
found, these parsers consume to EOF and succeed.

An empty string delimiter succeeds with the empty string and consumes no input.

### `map`

`parser.map(f)` applies `f` only to a successful parser value. Failures are
propagated unchanged.

### `located`

`parser.located()` applies `parser` and wraps a successful value in
`Located<A>`. The recorded `start` offset is the parser's starting
`Input.position()`. The recorded `end` offset is the successful result input's
`position()`. Offsets are zero-based, with `start` inclusive and `end` exclusive.

`located()` is opt-in and does not change input consumption. Failures are
propagated unchanged and do not allocate a `Located` value.

### `flatMap`

`parser.flatMap(f)` applies `parser`; on success, it calls `f` with the parsed
value and applies the returned parser at the new input position. If `f` returns
null, parsing fails with an internal parser expectation.

### `then`

`a.then(b)` parses `a` followed by `b` and returns an `ApplyBuilder` for mapping
both values. Additional `.then(...)` calls extend the sequence.

Sequential parsers propagate failures from the first failed component. They do
not automatically convert failures into `PARTIAL`.

### `thenSkip`

`a.thenSkip(b)` parses `a` followed by `b` and returns `a`'s value.

### `skipThen`

`a.skipThen(b)` parses `a` followed by `b` and returns `b`'s value.

### `between`

`parser.between(open, close)` parses the opening parser or character, then the
main parser, then the closing parser or character, returning the main value.

### `or` and `oneOf`

`a.or(b)` is equivalent to a two-branch `oneOf`.

`Combinators.oneOf(...)` tries alternatives from left to right:

- the first successful alternative wins;
- a `PARTIAL` failure stops choice immediately and is returned;
- if all alternatives fail with `NO_MATCH`, only failures at the farthest
  reported input position are kept;
- failures tied at that farthest position are combined so the formatted
  diagnostic can report multiple expectations.

`oneOf` requires at least one parser.

### `commit`

`Combinators.commit(parser)` applies `parser`. If `parser` fails and reports an input
position greater than the starting position, the failure is converted to
`PARTIAL`. Choice combinators do not try later alternatives after a `PARTIAL`
failure.

Use `commit` when a grammar branch has become specific enough that continuing to
other alternatives would produce a worse error.

### `expecting`

`parser.expecting(label)` relabels a failure while preserving the original
failure as its cause. It does not change successful results or input consumption.

### `optional`

`parser.optional()` always succeeds. It returns `Optional.of(value)` when the
parser succeeds and `Optional.empty()` without consuming input when the parser
fails.

### `orElse`

`parser.orElse(value)` succeeds with `value` without consuming input when
`parser` fails. It returns the original result when `parser` succeeds.

### `repeat`

`repeat(n)` parses exactly `n` items.

`repeat(min, max)` parses between `min` and `max` items.

`repeatAtLeast(n)` parses at least `n` items.

`repeatAtMost(n)` parses up to `n` items.

Repetition fails when:

- `min` or `max` is negative;
- `min > max`;
- fewer than `min` items match;
- the repeated parser succeeds without advancing input.

Successful repetition returns an unmodifiable list.

### `zeroOrMore` and `oneOrMore`

`zeroOrMore()` is `repeat(0, Integer.MAX_VALUE)`.

`oneOrMore()` is `repeat(1, Integer.MAX_VALUE)`.

### Allocation-conscious repetition

`foldZeroOrMore(identity, accumulator)` and
`foldOneOrMore(identity, accumulator)` repeat this parser and fold each value
into an accumulator instead of allocating an
intermediate list.

`foldZeroOrMoreFrom(identitySupplier, accumulator)` and
`foldOneOrMoreFrom(identitySupplier, accumulator)` are the mutable-accumulator forms.
The supplier is called once per parse to avoid sharing mutable state across
parse calls.

`skipZeroOrMore()` and `skipOneOrMore()` repeat this parser and discard parsed
values.

`collectString()` applies this parser one or more times and concatenates parsed
values with `String.valueOf(value)`. It is the allocation-conscious equivalent
of collecting a list with `oneOrMore()` and joining it afterward.

For raw input characters, prefer the scanner-level
`Lexical.collectChars(predicate)` / `Lexical.takeWhile(predicate)` APIs. Use
`collectString()` when the repeated parser produces values that are not simply
consecutive characters from the input.

### `zeroOrMoreUntil` and `oneOrMoreUntil`

These parsers repeat until the terminator parser succeeds. The terminator is
consumed when found. If the terminator appears before the minimum count, parsing
fails.

### `oneOrMoreSeparatedBy` and `zeroOrMoreSeparatedBy`

Separated parsers parse values with a separator parser between them.

`oneOrMoreSeparatedBy(sep)` requires at least one value.

`zeroOrMoreSeparatedBy(sep)` returns an empty list when the first value is absent.

`foldSeparatedBy(sep, identity, accumulator)` and
`foldZeroOrMoreSeparatedBy(sep, identity, accumulator)` are allocation-conscious
alternatives that fold separated values without allocating an intermediate list.

`foldSeparatedByFrom(sep, identitySupplier, accumulator)` and
`foldZeroOrMoreSeparatedByFrom(sep, identitySupplier, accumulator)` are the
mutable-accumulator forms.

### `onlyIf`

`parser.onlyIf(validationParser)` succeeds only when `validationParser` succeeds
at the same starting input position. The validation parser is lookahead; the main
parser is applied at the original input position.

`parser.onlyIf(CharPredicate)` checks the current character before applying the
main parser.

### `peek`

`parser.peek(lookahead)` first applies `parser`. If it succeeds, `lookahead` must
also succeed at the result input position. The returned value and input position
are from `parser`; the lookahead result is not consumed.

### `recover` and `recoverWith`

`parser.recover(recovery)` applies `recovery` at the original input position when
`parser` fails.

`parser.recoverWith(function)` calls `function` with the failure when `parser`
fails. The function is responsible for returning a result.

### `ref` and `set`

`Taker.ref()` creates an uninitialized parser reference for recursive grammars.

Calling `set(parser)` or `set(handler)` initializes the reference exactly once.
Calling `set` again throws an exception.

Applying an uninitialized reference throws an exception.

Recursive references detect direct infinite recursion at the same input position
and return a failure instead of recursing indefinitely.

### `systemOut`

`systemOut()` and `systemOut(label)` wrap a parser with diagnostic logging to
standard output. They are debugging helpers and should not be used in library
code paths that require quiet output.

## Built-In Parser Library Contract

### `Lexical`

`Lexical` contains character, string, regex, and quoted-string parsers.

- `chr(char)` matches exactly one character.
- `chr(CharPredicate)` matches one character satisfying the predicate.
- `string(str)` matches `str` exactly. The empty string succeeds without
  consuming input.
- `regex(pattern, flags)` matches with `Matcher.lookingAt()` from the current
  input position.
- `oneOf(chars)` matches one character from the supplied character set.
- `spaces` matches one or more ASCII spaces (`' '`) and does not match tabs,
  newlines, or other whitespace.
- `whitespace` matches one or more characters accepted by
  `Character.isWhitespace`, including line separators.
- `trim(parser)` skips ASCII spaces around `parser`. It does not skip tabs,
  newlines, or other Unicode whitespace.
- `trimSpaces(parser)` is an explicit alias for `trim(parser)`.
- `trimWhitespace(parser)` skips `Character.isWhitespace` around `parser`,
  including line separators. Use it only when crossing line boundaries is part
  of the grammar.
- `lexeme(parser, ignored)` repeatedly applies caller-defined ignored input
  before and after `parser`.
- `word` matches one or more letters.
- `line` consumes until a newline and does not consume the newline.
- `escapedString(quote, escape, escapes)` parses a quoted string and applies the
  supplied escape replacements.

### `Numeric`

`Numeric` contains digit, integer, long, double, and hex parsers.

- `numeric` matches one decimal digit.
- `nonZeroDigit` matches one decimal digit from `1` to `9`.
- `sign` parses `+`, `-`, or no sign, defaulting to positive.
- `unsignedInteger` and `unsignedLong` parse `0` or a non-zero digit followed by
  digits. Leading-zero input such as `0123` parses only the leading zero unless
  the caller uses `parseAll`.
- `integer` and `longValue` parse optional signs.
- Integer and long overflow should fail instead of saturating or wrapping.
- `longValue` accepts `-9223372036854775808`.
- `doubleValue` parses Java double-compatible decimal forms with optional
  exponent.
- `hex` parses `0x` or `0X` followed by one or more hex digits.

### `Combinators`

`Combinators` exposes static forms of common parser operations:

- `any`, `eof`, `fail`, `not`, `isNot`, `oneOf`, `sequence`, `between`,
  `satisfy`, `is`, `chainLeft`, `chainRight`, and `chain`.
- `throwError` deliberately throws and is primarily a test/debugging helper.

### `Chains`

`Chains` provides chain helpers and the `Associativity` enum for left- and
right-associative expression parsing.

### `Csv` and `IsoDates`

`Csv` and `IsoDates` are convenience parser collections. Their documented edge
cases are covered by focused tests under `src/test/java/.../parsers`.

## Compatibility Rules

Compatibility rules and the release checklist live in
[release-policy.md](release-policy.md). This document remains the source of
truth for parser semantics that should be preserved by compatible releases.

## Open Contract Questions

These should be resolved before a 1.0 release:

- Should concrete input implementations become package-private, or remain
  visible in source form but explicitly unsupported?
