package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;
import io.github.parseworks.taker.impl.result.PartialMatch;
import io.github.parseworks.taker.parsers.Combinators;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Core parser class for consuming input and producing results of type {@code A}.
 * <pre>{@code
 * Taker<Integer> parser = Numeric.integer;
 * }</pre>
 *
 * @param <A> result type
 */
public class Taker<A> implements Function<Input, Result<A>>{



    /**
     * Replaces the result of this parser with a constant value on success.
     * <pre>{@code
     * Lexical.string("true").as(true).value(); // true
     * }</pre>
     *
     * @param value constant value to return
     * @param <R>   result type
     * @return a parser returning the constant value
     */
    public <R> Taker<R> as(R value) {
        return new Taker<>(in -> {
            Result<A> result = this.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            return new Match<>(value, result.input());
        });
    }

    /** Matches an expression enclosed by brackets. */
    public Taker<A> between(char bracket) {
        return Combinators.between(bracket, this);
    }

    public Taker<A> between(char open, char close) {
        return Combinators.between(open, this, close);
    }

    /** Applies sequential parsers and returns the first result. */
    public <B> Taker<A> thenSkip(Taker<B> pb) {
        return Combinators.between(null, this, pb);
    }


    /** Applies sequential parsers and returns the second result. */
    public <B> Taker<B> skipThen(Taker<B> pb) {
        return Combinators.between(this, pb, null);
    }


    /** Returns an ApplyBuilder to combine sequential results. */
    public <B> ApplyBuilder<A, B> then(Taker<B> next) {
        return ApplyBuilder.of(this, next);
    }

    /** Matches an expression between brackets. */
    public <B> Taker<A> between(Taker<B> bracket) {
        return Combinators.between(bracket, this);
    }

    public <B, C> Taker<A> between(Taker<B> open, Taker<C> close) {
        return Combinators.between(open, this, close);
    }

    /** Matches a left-associative chain of operators, defaulting if none. */
    public Taker<A> chainLeftZeroOrMore(Taker<BinaryOperator<A>> op, A a) {
        return Combinators.chainLeft(this, op, a);
    }

    /** Matches a left-associative chain of operators. */
    public Taker<A> chainLeftOneOrMore(Taker<BinaryOperator<A>> op) {
        return Combinators.chainLeft(this, op);
    }

    /** Matches a right-associative chain of operators, defaulting if none. */
    public Taker<A> chainRightZeroOrMore(Taker<BinaryOperator<A>> op, A a) {
        return Combinators.chainRight(this, op, a);
    }

    /**
     * Tries this parser first, and if it fails, tries the alternative parser.
     * <pre>{@code
     * Taker<Integer> p = Numeric.integer.or(Combinators.pure(0));
     * p.parse("42").value(); // 42
     * p.parse("abc").value(); // 0
     * }</pre>
     *
     * @param other alternative parser
     * @return a choice parser
     */
    public Taker<A> or(Taker<A> other) {
        return Combinators.oneOf(this, other);
    }

    /** Matches a right-associative chain of operators. */
    public Taker<A> chainRightOneOrMore(Taker<BinaryOperator<A>> op) {
        return Combinators.chainRight(this, op);
    }

    /**
     * Collects zero or more matches into a list.
     * <pre>{@code
     * Lexical.chr('a').zeroOrMore().parse("aaa").value(); // ['a', 'a', 'a']
     * Lexical.chr('a').zeroOrMore().parse("bbb").value(); // []
     * }</pre>
     *
     * @return a list parser
     */
    public Taker<List<A>> zeroOrMore() {
        return repeat(0, Integer.MAX_VALUE);
    }

    /**
     * Collects one or more matches into a list.
     * <pre>{@code
     * Lexical.chr('a').oneOrMore().parse("aaa").value(); // ['a', 'a', 'a']
     * Lexical.chr('a').oneOrMore().parse("bbb").matches(); // false
     * }</pre>
     *
     * @return a list parser
     */
    public Taker<List<A>> oneOrMore() {
        return repeat(1, Integer.MAX_VALUE);
    }

    /**
     * Collects one or more matches until the terminator succeeds.
     * <pre>{@code
     * Lexical.chr('a').oneOrMoreUntil(Lexical.chr(';')).parse("aaa;").value(); // ['a', 'a', 'a']
     * }</pre>
     *
     * @param until terminator parser
     * @return a list parser
     */
    public Taker<List<A>> oneOrMoreUntil(Taker<?> until) {
        return TakerRepetition.repeat(this, 1, Integer.MAX_VALUE, until);
    }


    /**
     * Succeeds only if validation succeeds without consuming input.
     * <pre>{@code
     * Taker<Integer> p = Numeric.integer.onlyIf(Lexical.chr('+'));
     * p.parse("+123").value(); // 123
     * p.parse("-123").matches(); // false
     * }</pre>
     *
     * @param validation validation parser
     * @param <B>        validation result type
     * @return a conditional parser
     */
    public <B> Taker<A> onlyIf(Taker<B> validation) {
        return new Taker<>(input -> {
            Result<B> validationResult = validation.apply(input);
            if (!validationResult.matches()) {
                return validationResult.cast();
            }
            return this.apply(input);
        });
    }

    public Taker<A> onlyIf(CharPredicate validation) {
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new NoMatch<>(input, "Expected Character at " + input.position());
            }
            var result = validation.test(input.current());
            if (!result) {
                return new NoMatch<>(input, "Predicate failed" );
            }
            return this.apply(input);
        });
    }

    /**
     * Succeeds if followed by lookahead without consuming lookahead input.
     * <pre>{@code
     * Taker<String> p = Lexical.word.peek(Lexical.chr('='));
     * p.parse("id=42").value(); // "id"
     * }</pre>
     *
     * @param lookahead lookahead parser
     * @param <B>       lookahead result type
     * @return a lookahead parser
     */
    public <B> Taker<A> peek(Taker<B> lookahead) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (!result.matches()) {
                return result;
            }
            Result<B> peek = lookahead.apply(result.input());
            if (!peek.matches()) {
                return new NoMatch<>(input, "Expected 'peek' to succeed", (NoMatch<?>) peek);
            }
            return result;
        });
    }

    /**
     * Creates a parser that logs its progress and results to standard output with a custom label.
     *
     * @param label a descriptive name for this parser to include in logs
     * @return a new parser that logs its progress
     */
    public Taker<A> systemOut(String label) {
        return TakerDebug.systemOut(this, label);
    }

    /**
     * Creates a parser that always succeeds, optionally containing this parser's result.
     * <p>
     * The {@code optional} method creates a parser that attempts to apply this parser, but always
     * succeeds regardless of the result. The parsing process works as follows:
     * <ol>
     *   <li>First attempts to apply this parser to the input</li>
     *   <li>If this parser succeeds, its result is wrapped in a non-empty {@link Optional}</li>
     *   <li>If this parser fails, an empty {@link Optional} is returned without consuming any input</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing optional elements in a grammar, such as optional
     * parameters, modifiers, or any syntax structures that may or may not be present. It provides
     * a convenient way to handle the presence or absence of elements without disrupting the overall
     * parsing flow.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines {@link #map(Function)} with {@link #orElse(Object)} to achieve the optional behavior</li>
     *   <li>Always succeeds, never causing parsing failure</li>
     *   <li>No input is consumed when the parser fails</li>
     *   <li>The result type is transformed from {@code A} to {@code Optional<A>}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse signed numbers
     * Taker<Integer> number = Numeric.integer;
     * Taker<Integer> signedNumber = Lexical.chr('-').optional().then(number)
     *     .map((sign, num) -> sign.isPresent() ? -num : num);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with -42 for input "-42"
     * // Fails for input "abc" (no number found)
     * }</pre>
     *
     * @return a parser that always succeeds, returning either an Optional containing this
     * parser's result or an empty Optional
     * @see #orElse(Object) for providing a default value instead of an Optional
     * @see #zeroOrMore() for collecting zero or more occurrences of a pattern
     */
    public Taker<Optional<A>> optional() {
        return new Taker<>(in -> {
            Result<A> result = this.apply(in);
            if (!result.matches()) {
                return new Match<>(Optional.empty(), in);
            }
            return new Match<>(Optional.of(result.value()), result.input());
        });
    }

    /**
     * Creates a parser that provides a default value if this parser fails.
     * <p>
     * The {@code orElse} method provides a way to handle parser failures by substituting a default value
     * rather than propagating the failure. This is different from the {@code or} method which tries an
     * alternative parsing strategy.
     * <p>
     * When applied to input:
     * <ol>
     *   <li>First attempts to apply this parser to the input</li>
     *   <li>If this parser succeeds, its result is returned</li>
     *   <li>If this parser fails, a success result containing the default value is returned</li>
     * </ol>
     * <p>
     * Important: When returning the default value, the input position remains unchanged from the original position.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse an integer, or use 0 as default if parsing fails
     * Taker<Integer> parser = Numeric.integer.orElse(0);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with 0 for input "abc"
     * }</pre>
     *
     * @param other the default value to return if this parser fails
     * @return a parser that returns either the successful parse result or the default value
     */
    public Taker<A> orElse(A other) {
        return new Taker<>(in -> {
            Result<A> result = this.apply(in);
            if (!result.matches()) {
                return new Match<>(other, in);
            }
            return result;
        });
    }

    /**
     * Parses the input without requiring that the entire input is consumed.
     *
     * @param in the input to parse
     * @return the result of parsing the input
     */
    public Result<A> parse(Input in) {
        return parse(in, false);
    }

    /**
     * Creates an iterator that incrementally parses the input, allowing for streaming processing of parse results.
     * <p>
     * The {@code iterateParse} method provides a way to parse input incrementally by returning an iterator
     * that processes the input one element at a time. This is particularly useful when:
     * <ul>
     *   <li>Processing large inputs that shouldn't be parsed all at once</li>
     *   <li>Implementing streaming or lazy parsing scenarios</li>
     *   <li>Searching for multiple occurrences of a pattern in the input</li>
     * </ul>
     * <p>
     * The iterator works as follows:
     * <ol>
     *   <li>Attempts to parse the input at the current position using this parser</li>
     *   <li>If parsing succeeds, returns the parsed value and advances the input position</li>
     *   <li>If parsing fails, skips one character and tries again at the next position</li>
     *   <li>Continues until the end of input is reached</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The iterator maintains its own input position state</li>
     *   <li>Results are computed lazily when {@code hasNext()} or {@code next()} is called</li>
     *   <li>Failed parse attempts are skipped automatically</li>
     *   <li>The iterator follows the standard Java Iterator contract</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that matches integers
     * Taker<Integer> intParser = integer;
     *
     * // Create an input from a string containing mixed content
     * Input input = Input.of("123 abc 456 def 789");
     *
     * // Iterate over all integers in the input
     * Iterator<Integer> numbers = intParser.iterateParse(input);
     * while (numbers.hasNext()) {
     *     Integer number = numbers.next();
     *     System.out.println(number); // Prints: 123, 456, 789
     * }
     * }</pre>
     *
     * @param input the input to parse
     * @return an iterator that yields parse results one at a time
     * @throws IllegalArgumentException if the input is null
     * @see Input for input handling
     * @see Result for parse result handling
     */
    public Iterator<A> iterateParse(Input input) {
        final Taker<A> parser = this;
        return new Iterator<>() {
            private Input currentInput = input;
            private Result<A> nextResult = null;


            @Override
            public boolean hasNext() {
                if (currentInput.isEof()) {
                    return false;
                }

                if (nextResult != null) {
                    return true;
                }

                // Try to find the next valid result
                while (!currentInput.isEof()) {
                    Result<A> result = parser.parse(currentInput, false);
                    if (result.matches()) {
                        nextResult = result;
                        return true;
                    }
                    // Skip one character and try again
                    currentInput = currentInput.next();
                }

                return false;
            }

            @Override
            public A next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements to parse");
                }

                // We already have the next result from hasNext()
                A value = nextResult.value();
                currentInput = nextResult.input();
                nextResult = null;
                return value;
            }
        };
    }


    public Stream<A> stream(Input input) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterateParse(input),
                Spliterator.ORDERED | Spliterator.NONNULL
            ),
            false
        );
    }


    public Result<A> parse(Input in, boolean consumeAll) {
        Result<A> result = this.apply(in);
        if (consumeAll && result.matches()) {
            if (!result.input().isEof()) {
                return new PartialMatch<>(result.input(), new NoMatch<>(result.input(), "end of input"));
            }
        }
        return result;
    }

    /**
     * Applies the specified input to the handler and returns the result.
     *
     * @param in the input to process of type {@code Input<I>}
     * @return the result of processing the input, encapsulated in a {@code Result<A>}
     */
    public Result<A> apply(Input in) {
        return applyHandler.apply(in);
    }

    /**
     * Parses the input string without requiring that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    public Result<A> parse(CharSequence input) {
        return parse(Input.of(input), false);
    }

    /**
     * Parses the input and ensures that the entire input is consumed.
     * <p>
     * The {@code parseAll} method applies this parser to the given input and verifies
     * that no unconsumed input remains. It is useful for scenarios where the input
     * must be fully parsed without leaving any trailing characters.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>Applies this parser to the provided input</li>
     *   <li>If the parser succeeds, checks whether the input has been fully consumed</li>
     *   <li>If any unconsumed input remains, the method returns a failure result</li>
     *   <li>If the parser fails or unconsumed input exists, an error is returned</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a complete integer from the input
     * Taker<Integer> intParser = Numeric.integer;
     * Input input = Input.of("123");
     * Result<Integer> result = intParser.parseAll(input);
     *
     * if (result.matches()) {
     *     Integer value = result.value(); // Successfully parsed value
     * } else {
     *     String error = result.error(); // Error message for failure
     * }
     * }</pre>
     *
     * @param in the input to parse
     * @return the result of parsing the input, ensuring the entire input is consumed
     * @see #parse(Input) for parsing without requiring complete input consumption
     * @see #parse(Input, boolean) for parsing with an explicit consumeAll flag
     */
    public Result<A> parseAll(Input in) {
        return parse(in, true);
    }

    /**
     * Parses the input string and ensures that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    public Result<A> parseAll(CharSequence input) {
        return parse( Input.of(input), true);
    }

    /**
     * Creates a parser that applies this parser exactly the specified number of times,
     * collecting all results into a list.
     * <p>
     * The {@code repeat} method creates a parser that matches this parser's pattern an exact
     * number of times in sequence. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser exactly {@code target} times in sequence</li>
     *   <li>If successful for all iterations, returns all results collected in a {@code List}</li>
     *   <li>If this parser fails before reaching the target count, the entire parser fails</li>
     * </ol>
     * <p>
     * This method is useful when parsing structures with a known, fixed number of elements,
     * such as fixed-length records, tuples, or specific syntax patterns with exact repetition
     * requirements.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>All parse results are collected in order of occurrence</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>The input position is advanced after each successful application</li>
     *   <li>Unlike {@link #oneOrMore()}, this parser requires exactly the specified number of matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse exactly 3 digits
     * Taker<Character> digit = Lexical.chr(CharPredicate.asciiDigit);
     * Taker<String> threeDigits = digit.repeat(3);
     *
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2,3] for input "123abc" (consuming only "123")
     * // Fails for input "12" (not enough digits)
     * // Fails for input "ab12" (first element not a digit)
     * }</pre>
     *
     * @param target the exact number of times to apply this parser
     * @return a parser that applies this parser exactly the specified number of times
     * @throws IllegalArgumentException if the target count is negative
     * @see #repeat(int, int) for a version with minimum and maximum repetition counts
     * @see #repeatAtLeast(int) for a version with only a minimum count
     * @see #oneOrMore() for matching one or more occurrences without an upper limit
     * @see #zeroOrMore() for matching zero or more occurrences
     */
    public Taker<List<A>> repeat(int target) {
        return TakerRepetition.repeat(this, target, target, null);
    }

    /**
     * Creates a parser that applies this parser between a minimum and maximum number of times,
     * collecting all results into a list.
     * <p>
     * The {@code repeat} method with min and max arguments creates a parser that matches this parser's pattern
     * a variable number of times within the specified range. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser repeatedly</li>
     *   <li>Requires at least {@code min} successful applications to succeed</li>
     *   <li>Will not attempt more than {@code max} applications, even if more matches are possible</li>
     *   <li>Returns all results collected in a {@code List}</li>
     *   <li>If this parser fails before reaching the minimum count, the entire parser fails</li>
     * </ol>
     * <p>
     * This method generalizes several other repetition parsers, allowing explicit control over both
     * the lower and upper bounds of repetition. It is particularly useful for parsing structures with
     * flexible but constrained repetition patterns, such as optional sections with limits, or required
     * elements with additional optional elements.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>All parse results are collected in order of occurrence</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>The input position is advanced after each successful application</li>
     *   <li>Collection stops either when the maximum count is reached or when this parser fails</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse between 2 and 4 digits
     * Taker<Character> digit = Lexical.chr(CharPredicate.asciiDigit);
     * Taker<String> digits = digit.repeat(2, 4);
     *
     * // Succeeds with [1,2,3,4] for input "1234"
     * // Succeeds with [1,2,3,4] for input "12345" (consuming only "1234")
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2] for input "12"
     * // Fails for input "1" (not enough digits)
     * // Fails for input "abc" (no digits found)
     * }</pre>
     *
     * @param min the minimum number of times to apply this parser
     * @param max the maximum number of times to apply this parser
     * @return a parser that applies this parser between min and max times
     * @throws IllegalArgumentException if min or max is negative, or if min is greater than max
     * @see #repeat(int) for a version with an exact count
     * @see #repeatAtLeast(int) for a version with only a minimum count
     * @see #repeatAtMost(int) for a version with only a maximum count
     * @see #oneOrMore() for matching one or more occurrences without an upper limit
     * @see #zeroOrMore() for matching zero or more occurrences without an upper limit
     */
    public Taker<List<A>> repeat(int min, int max) {
        return TakerRepetition.repeat(this, min, max, null);
    }

    public Taker<List<A>> repeatAtLeast(int target) {
        return TakerRepetition.repeat(this, target, Integer.MAX_VALUE, null);
    }

    public Taker<List<A>> repeatAtMost(int max) {
        return TakerRepetition.repeat(this, 0, max, null);
    }

    /** Parses zero or more elements separated by a delimiter. */
    public <SEP> Taker<List<A>> zeroOrMoreSeparatedBy(Taker<SEP> sep) {
        return TakerRepetition.separatedBy(this, sep, 0);
    }

    /**
     * Transforms the result of this parser using the given function.
     * <pre>{@code
     * Taker<Integer> p = Lexical.chr('5').map(Character::getNumericValue);
     * p.parse("5").value(); // 5
     * }</pre>
     *
     * @param func transformation function
     * @param <R>  transformed result type
     * @return a transformed parser
     */
    public <R> Taker<R> map(Function<A, R> func) {
        return new Taker<>(in -> apply(in).map(func));
    }

    /**
     * Wraps this parser's successful value with the consumed source offsets.
     * <pre>{@code
     * Located<String> id = Lexical.word.located().parse("name = value").value();
     * id.value(); // "name"
     * id.start(); // 0
     * id.end();   // 4
     * }</pre>
     *
     * @return a parser returning the value and zero-based start/end offsets
     */
    public Taker<Located<A>> located() {
        return new Taker<>(in -> {
            int start = in.position();
            Result<A> result = this.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            return new Match<>(new Located<>(result.value(), start, result.input().position()), result.input());
        });
    }

    /** Parses one or more elements separated by a delimiter. */
    public <SEP> Taker<List<A>> oneOrMoreSeparatedBy(Taker<SEP> sep) {
        return TakerRepetition.separatedBy(this, sep, 1);
    }

    /**
     * Repeatedly applies this parser and folds each value into an accumulator.
     * <p>
     * Unlike {@link #zeroOrMore()}, this does not allocate an intermediate
     * {@link List}. Use {@link #foldZeroOrMoreFrom(Supplier, BiFunction)} when
     * the accumulator is mutable.
     *
     * @param identity initial accumulator value
     * @param accumulator combines the current accumulator and parsed value
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <B> Taker<B> foldZeroOrMore(B identity, BiFunction<? super B, ? super A, ? extends B> accumulator) {
        return foldZeroOrMoreFrom(() -> identity, accumulator);
    }

    /**
     * Repeatedly applies this parser and folds each value into a fresh accumulator.
     * <p>
     * This succeeds even when this parser matches zero times.
     *
     * @param identitySupplier supplies a fresh accumulator for each parse
     * @param accumulator combines the current accumulator and parsed value
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <B> Taker<B> foldZeroOrMoreFrom(
        Supplier<? extends B> identitySupplier,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        return TakerRepetition.foldRepeated(this, 0, identitySupplier, accumulator);
    }

    /**
     * Applies this parser one or more times and folds each value into an accumulator.
     * <p>
     * Unlike {@link #oneOrMore()}, this does not allocate an intermediate
     * {@link List}. Use {@link #foldOneOrMoreFrom(Supplier, BiFunction)} when
     * the accumulator is mutable.
     *
     * @param identity initial accumulator value
     * @param accumulator combines the current accumulator and parsed value
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <B> Taker<B> foldOneOrMore(B identity, BiFunction<? super B, ? super A, ? extends B> accumulator) {
        return foldOneOrMoreFrom(() -> identity, accumulator);
    }

    /**
     * Applies this parser one or more times and folds each value into a fresh accumulator.
     *
     * @param identitySupplier supplies a fresh accumulator for each parse
     * @param accumulator combines the current accumulator and parsed value
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <B> Taker<B> foldOneOrMoreFrom(
        Supplier<? extends B> identitySupplier,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        return TakerRepetition.foldRepeated(this, 1, identitySupplier, accumulator);
    }

    /**
     * Applies this parser one or more times separated by {@code sep}, folding
     * values into an accumulator without allocating an intermediate list.
     *
     * @param sep separator parser
     * @param identity initial accumulator value
     * @param accumulator combines the current accumulator and parsed value
     * @param <SEP> separator result type
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <SEP, B> Taker<B> foldSeparatedBy(
        Taker<SEP> sep,
        B identity,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        return foldSeparatedByFrom(sep, () -> identity, accumulator);
    }

    /**
     * Applies this parser one or more times separated by {@code sep}, folding
     * values into a fresh accumulator without allocating an intermediate list.
     *
     * @param sep separator parser
     * @param identitySupplier supplies a fresh accumulator for each parse
     * @param accumulator combines the current accumulator and parsed value
     * @param <SEP> separator result type
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <SEP, B> Taker<B> foldSeparatedByFrom(
        Taker<SEP> sep,
        Supplier<? extends B> identitySupplier,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        return TakerRepetition.foldSeparatedBy(this, sep, 1, identitySupplier, accumulator);
    }

    /**
     * Applies this parser zero or more times separated by {@code sep}, folding
     * values into an accumulator without allocating an intermediate list.
     *
     * @param sep separator parser
     * @param identity initial accumulator value
     * @param accumulator combines the current accumulator and parsed value
     * @param <SEP> separator result type
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <SEP, B> Taker<B> foldZeroOrMoreSeparatedBy(
        Taker<SEP> sep,
        B identity,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        return foldZeroOrMoreSeparatedByFrom(sep, () -> identity, accumulator);
    }

    /**
     * Applies this parser zero or more times separated by {@code sep}, folding
     * values into a fresh accumulator without allocating an intermediate list.
     *
     * @param sep separator parser
     * @param identitySupplier supplies a fresh accumulator for each parse
     * @param accumulator combines the current accumulator and parsed value
     * @param <SEP> separator result type
     * @param <B> accumulator/result type
     * @return a parser returning the folded accumulator
     */
    public <SEP, B> Taker<B> foldZeroOrMoreSeparatedByFrom(
        Taker<SEP> sep,
        Supplier<? extends B> identitySupplier,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        return TakerRepetition.foldSeparatedBy(this, sep, 0, identitySupplier, accumulator);
    }

    /**
     * Repeatedly applies this parser and discards all parsed values.
     * <p>
     * This is useful for skipping comments, whitespace, or delimiters without
     * allocating a {@link List}.
     *
     * @return a parser returning {@code null} after consuming zero or more matches
     */
    public Taker<Void> skipZeroOrMore() {
        return skipRepeated(0);
    }

    /**
     * Applies this parser one or more times and discards all parsed values.
     *
     * @return a parser returning {@code null} after consuming one or more matches
     */
    public Taker<Void> skipOneOrMore() {
        return skipRepeated(1);
    }

    private Taker<Void> skipRepeated(int min) {
        return TakerRepetition.foldRepeated(this, min, () -> null, (ignored, value) -> null);
    }

    /**
     * Applies this parser one or more times and concatenates parsed values using
     * {@link String#valueOf(Object)}.
     * <p>
     * This is an allocation-conscious replacement for
     * collecting a list with {@code oneOrMore()} and joining it afterward.
     *
     * @return a parser returning the concatenated parsed values
     */
    public Taker<String> collectString() {
        return foldOneOrMoreFrom(StringBuilder::new, (builder, value) -> builder.append(value))
            .map(StringBuilder::toString);
    }

    /** Initializes a parser reference with another parser's behavior. */
    public synchronized void set(Taker<A> parser) {
        if (parser == null) {
            throw new IllegalArgumentException("parser cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Taker already has an applyHandler");
        }
        this.applyHandler = parser.applyHandler;
    }

    /** Initializes a parser reference with a custom apply handler. */
    public synchronized void set(Function<Input, Result<A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Taker already has an applyHandler");
        }
        this.applyHandler = applyHandler;
    }


    /** Parses zero or more times until a terminator succeeds. */
    public Taker<List<A>> zeroOrMoreUntil(Taker<?> terminator) {
        return TakerRepetition.repeat(this, 0, Integer.MAX_VALUE, terminator);
    }

    /**
     * A function that defines how this parser applies to an input.
     * <p>
     * This function is the core of the parser, defining how it processes input and produces results.
     * It takes an {@link Input} object representing the current parsing state and returns a {@link Result}
     * object containing either a successful parse result or an error.
     */
    protected Function<Input, Result<A>> applyHandler;
    /**
     * A default apply handler that throws an exception if the parser is not initialized.
     */
    private Function<Input, Result<A>> defaultApplyHandler;


    /**
     * Private constructor to create a parser reference that can be initialized later.
     */
    protected Taker() {
        this.applyHandler = defaultApplyHandler = in -> {
            throw new IllegalStateException("Taker not initialized");
        };
    }

    /** Creates a parser with the specified apply handler. */
    public Taker(Function<Input, Result<A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        this.applyHandler = applyHandler;
    }

    /** Creates a parser reference for recursive grammar definitions. */
    public static <A> Taker<A> ref() {
        return new CheckParser<>();
    }

    private static class CheckParser<A> extends Taker<A> {

        private final ThreadLocal<IntObjectMap<ArrayDeque<Taker<?>>>> contextLocal =
            ThreadLocal.withInitial(IntObjectMap::new);

        @Override
        public Result<A> apply(Input in) {
            int pos = in.position();
            IntObjectMap<ArrayDeque<Taker<?>>> ctx = contextLocal.get();

            ArrayDeque<Taker<?>> stack = ctx.get(pos);
            if (stack == null) {
                stack = new ArrayDeque<>();
                ctx.put(pos, stack);
            }

            for (Taker<?> p : stack) {
                if (p == this) {
                    return new NoMatch<>(in, "no infinite recursion");
                }
            }

            stack.push(this);
            try {
                return applyHandler.apply(in);
            } catch (RuntimeException e) {
                if (e instanceof IllegalStateException && "Taker not initialized".equals(e.getMessage())) {
                    throw e;
                }
                return new NoMatch<>(in, "parser to function correctly");
            } finally {
                stack.pop();
                if (stack.isEmpty()) {
                    ctx.remove(pos);
                }
            }
        }
    }

    private static final class IntObjectMap<V> {
        private static final int DEFAULT_CAPACITY = 16;
        private int[] keys = new int[DEFAULT_CAPACITY];
        private Object[] values = new Object[DEFAULT_CAPACITY];
        private int size;

        void put(int key, V value) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    values[i] = value;
                    return;
                }
            }

            if (size == keys.length) {
                grow();
            }

            keys[size] = key;
            values[size] = value;
            size++;
        }

        @SuppressWarnings("unchecked")
        V get(int key) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    return (V) values[i];
                }
            }
            return null;
        }

        void remove(int key) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    if (i < size - 1) {
                        System.arraycopy(keys, i + 1, keys, i, size - i - 1);
                        System.arraycopy(values, i + 1, values, i, size - i - 1);
                    }
                    values[size - 1] = null;
                    size--;
                    return;
                }
            }
        }

        private void grow() {
            int newCapacity = keys.length * 2;
            keys = Arrays.copyOf(keys, newCapacity);
            values = Arrays.copyOf(values, newCapacity);
        }
    }

    /**
     * Creates a parser that attempts to recover from errors by trying an alternative parser.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the recovery parser is applied
     * to the same input position.
     * <p>
     * This is useful for error recovery in situations where there are multiple valid alternatives,
     * and you want to try them in sequence.
     *
     * @param recovery the parser to try if this parser fails
     * @param <B> the result type of the recovery parser
     * @return a new parser that tries the recovery parser if this parser fails
     */
    public <B> Taker<B> recover(Taker<B> recovery) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply(input);
        });
    }

    /**
     * Creates a parser that attempts to recover from errors by applying a function
     * to the failure result.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the recovery function is applied
     * to the failure result to produce a new result.
     * <p>
     * This is useful for custom error recovery strategies, such as:
     * <ul>
     *   <li>Transforming errors into default values</li>
     *   <li>Implementing domain-specific error recovery logic</li>
     * </ul>
     *
     * @param recovery the function to apply to the failure result
     * @param <B> the result type of the recovery function
     * @return a new parser that applies the recovery function if this parser fails
     */
    public <B> Taker<B> recoverWith(Function<Failure<A>, Result<B>> recovery) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply((Failure<A>) result);
        });
    }

    /**
     * Labels this parser with a human-readable expectation for error messages.
     * <pre>{@code
     * Taker<String> p = Lexical.word.expecting("identifier");
     * p.parse("123").matches(); // false, error: "Expected identifier"
     * }</pre>
     *
     * @param label descriptive label
     * @return a labeled parser
     */
    public  Taker<A> expecting(String label) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (result.matches()) return result;
            return new NoMatch<>(result.input(), label, (Failure<?>) result);
        });
    }

    /**
     * This parser applies a function to the result of this parser, returning a new parser.
     * This is a monadic operation that allows chaining parsers based on the result of the current parser.
     * The following example shows where we want to parse a number of characters exactly, based on the initial number
     * provided.
     * This allows for dynamic parsing based on the result of the current parser.
     * <pre>{@code
     * Taker<String> p = Numeric.unsignedInteger.flatMap(n ->
     *     Lexical.chr(',').skipThen(Lexical.chr('a').repeat(n))
     *         .map(chars -> chars.stream()
     *             .map(String::valueOf)
     *             .collect(java.util.stream.Collectors.joining()))
     * );
     * p.parse("3,aaa").value(); // "aaa"
     * }</pre>
     *
     * @param f   function returning the next parser
     * @param <B> next result type
     * @return a monadic parser
     */
    public <B> Taker<B> flatMap(Function<A, Taker<B>> f) {
        if (f == null) {
            throw new IllegalArgumentException("flatMap function cannot be null");
        }
        return new Taker<>(in -> {
            Result<A> r = this.apply(in);
            if (!r.matches()) {
                // propagate the original failure
                return r.cast();
            }
            Taker<B> next = f.apply(r.value());
            if (next == null) {
                // be defensive to help users diagnose nulls
                return new NoMatch<B>(r.input(), "parser to function correctly").cast();
            }
            return next.apply(r.input());
        });
    }

}
