/*
 * Copyright (c) 2026 jason bailey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.parseworks.taker;

import io.github.parseworks.taker.internal.*;

import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.results.PartialMatch;
import io.github.parseworks.taker.parsers.Combinators;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Parser that consumes {@link Input} and produces a {@link Result}.
 * <p>
 * A {@code Taker<A>} is immutable after construction, except for parser
 * references created with {@link #ref()} and initialized with {@link #set(Taker)}
 * or {@link #set(Function)}. Parser composition is usually expressed with the
 * fluent instance methods on this type.
 *
 * @param <A> result type
 */
public class Taker<A> implements Function<Input, Result<A>>{



    /**
     * Replaces the result of this parser with a constant value on success.
     *
     * @param value constant value to return
     * @param <R>   result type
     * @return a parser returning the constant value
     */
    public <R> Taker<R> as(R value) {
        return Transforms.as(this, value);
    }

    /**
     * Parses this parser between two occurrences of {@code bracket}.
     *
     * @param bracket opening and closing delimiter
     * @return a parser that returns this parser's value
     */
    public Taker<A> between(char bracket) {
        return Combinators.between(bracket, this);
    }

    /**
     * Parses this parser between {@code open} and {@code close}.
     *
     * @param open opening delimiter
     * @param close closing delimiter
     * @return a parser that returns this parser's value
     */
    public Taker<A> between(char open, char close) {
        return Combinators.between(open, this, close);
    }

    /**
     * Parses this parser followed by {@code pb}, returning this parser's value.
     *
     * @param pb parser to apply after this parser
     * @param <B> ignored parser result type
     * @return a parser returning this parser's value
     */
    public <B> Taker<A> thenSkip(Taker<B> pb) {
        return Combinators.between(null, this, pb);
    }


    /**
     * Parses this parser followed by {@code pb}, returning {@code pb}'s value.
     *
     * @param pb parser to apply after this parser
     * @param <B> returned parser result type
     * @return a parser returning {@code pb}'s value
     */
    public <B> Taker<B> skipThen(Taker<B> pb) {
        return Combinators.between(this, pb, null);
    }


    /**
     * Parses this parser followed by {@code next} and returns a sequence builder.
     *
     * @param next parser to apply after this parser
     * @param <B> next parser result type
     * @return a builder for mapping both parsed values
     */
    public <B> ApplyBuilder<A, B> then(Taker<B> next) {
        return ApplyBuilder.of(this, next);
    }

    /**
     * Parses this parser between two occurrences of {@code bracket}.
     *
     * @param bracket opening and closing parser
     * @param <B> delimiter parser result type
     * @return a parser that returns this parser's value
     */
    public <B> Taker<A> between(Taker<B> bracket) {
        return Combinators.between(bracket, this);
    }

    /**
     * Parses this parser between {@code open} and {@code close}.
     *
     * @param open opening parser
     * @param close closing parser
     * @param <B> opening parser result type
     * @param <C> closing parser result type
     * @return a parser that returns this parser's value
     */
    public <B, C> Taker<A> between(Taker<B> open, Taker<C> close) {
        return Combinators.between(open, this, close);
    }

    /**
     * Parses a left-associative operator chain, returning {@code a} when this parser does not match.
     *
     * @param op operator parser
     * @param a identity value
     * @return a chain parser
     */
    public Taker<A> chainLeftZeroOrMore(Taker<BinaryOperator<A>> op, A a) {
        return Combinators.chainLeft(this, op, a);
    }

    /**
     * Parses a left-associative operator chain requiring at least one value.
     *
     * @param op operator parser
     * @return a chain parser
     */
    public Taker<A> chainLeftOneOrMore(Taker<BinaryOperator<A>> op) {
        return Combinators.chainLeft(this, op);
    }

    /**
     * Parses a right-associative operator chain, returning {@code a} when this parser does not match.
     *
     * @param op operator parser
     * @param a identity value
     * @return a chain parser
     */
    public Taker<A> chainRightZeroOrMore(Taker<BinaryOperator<A>> op, A a) {
        return Combinators.chainRight(this, op, a);
    }

    /**
     * Tries this parser first, and if it fails, tries the alternative parser.
     *
     * @param other alternative parser
     * @return a choice parser
     */
    public Taker<A> or(Taker<A> other) {
        return Combinators.oneOf(this, other);
    }

    /**
     * Parses a right-associative operator chain requiring at least one value.
     *
     * @param op operator parser
     * @return a chain parser
     */
    public Taker<A> chainRightOneOrMore(Taker<BinaryOperator<A>> op) {
        return Combinators.chainRight(this, op);
    }

    /**
     * Collects zero or more matches into a list.
     *
     * @return a list parser
     */
    public Taker<List<A>> zeroOrMore() {
        return repeat(0, Integer.MAX_VALUE);
    }

    /**
     * Collects one or more matches into a list.
     *
     * @return a list parser
     */
    public Taker<List<A>> oneOrMore() {
        return repeat(1, Integer.MAX_VALUE);
    }

    /**
     * Collects one or more matches until the terminator succeeds.
     *
     * @param until terminator parser
     * @return a list parser
     */
    public Taker<List<A>> oneOrMoreUntil(Taker<?> until) {
        return Repetition.repeat(this, 1, Integer.MAX_VALUE, until);
    }


    /**
     * Succeeds only if validation succeeds without consuming input.
     *
     * @param validation validation parser
     * @param <B>        validation result type
     * @return a conditional parser
     */
    public <B> Taker<A> onlyIf(Taker<B> validation) {
        return Lookahead.onlyIf(this, validation);
    }

    /**
     * Applies this parser only when the current character satisfies
     * {@code validation}. The validation check does not consume input.
     *
     * @param validation character predicate checked at the current input
     * @return a conditional parser
     */
    public Taker<A> onlyIf(CharPredicate validation) {
        return Lookahead.onlyIf(this, validation);
    }

    /**
     * Succeeds if followed by lookahead without consuming lookahead input.
     *
     * @param lookahead lookahead parser
     * @param <B>       lookahead result type
     * @return a lookahead parser
     */
    public <B> Taker<A> peek(Taker<B> lookahead) {
        return Lookahead.peek(this, lookahead);
    }

    /**
     * Creates a parser that logs its progress and results to standard output with a custom label.
     *
     * @param label a descriptive name for this parser to include in logs
     * @return a new parser that logs its progress
     */
    public Taker<A> systemOut(String label) {
        return Debug.systemOut(this, label);
    }

    /**
     * Creates a parser that always succeeds, optionally containing this parser's result.
     * <p>
     * The {@code optional} method creates a parser that attempts to apply this parser, but always
     * succeeds. The parsing process works as follows:
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
     *
     * @return a parser that always succeeds, returning either an Optional containing this
     * parser's result or an empty Optional
     * @see #orElse(Object) for providing a default value instead of an Optional
     * @see #zeroOrMore() for collecting zero or more occurrences of a pattern
     */
    public Taker<Optional<A>> optional() {
        return Transforms.optional(this);
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
     *
     * @param other the default value to return if this parser fails
     * @return a parser that returns either the successful parse result or the default value
     */
    public Taker<A> orElse(A other) {
        return Transforms.orElse(this, other);
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
     * Creates an iterator that scans the input for repeated matches of this parser.
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
     *
     * @param input the input to parse
     * @return an iterator that yields parse results one at a time
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


    /**
     * Parses the input with optional full-input enforcement.
     *
     * @param in input to parse
     * @param consumeAll when true, a successful parse must end at EOF
     * @return parse result
     */
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
     * @param in the input to process
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
        return Repetition.repeat(this, target, target, null);
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
        return Repetition.repeat(this, min, max, null);
    }

    /**
     * Applies this parser at least {@code target} times and collects the values
     * into an unmodifiable list.
     *
     * @param target minimum number of matches
     * @return a parser that requires at least {@code target} matches
     */
    public Taker<List<A>> repeatAtLeast(int target) {
        return Repetition.repeat(this, target, Integer.MAX_VALUE, null);
    }

    /**
     * Applies this parser up to {@code max} times and collects the values into
     * an unmodifiable list.
     *
     * @param max maximum number of matches
     * @return a parser that accepts at most {@code max} matches
     */
    public Taker<List<A>> repeatAtMost(int max) {
        return Repetition.repeat(this, 0, max, null);
    }

    /**
     * Parses zero or more elements separated by {@code sep}.
     *
     * @param sep separator parser
     * @param <SEP> separator result type
     * @return a parser returning parsed values
     */
    public <SEP> Taker<List<A>> zeroOrMoreSeparatedBy(Taker<SEP> sep) {
        return Repetition.separatedBy(this, sep, 0);
    }

    /**
     * Transforms the result of this parser using the given function.
     *
     * @param func transformation function
     * @param <R>  transformed result type
     * @return a transformed parser
     */
    public <R> Taker<R> map(Function<A, R> func) {
        return Transforms.map(this, func);
    }

    /**
     * Wraps this parser's successful value with the consumed source offsets.
     *
     * @return a parser returning the value and zero-based start/end offsets
     */
    public Taker<Located<A>> located() {
        return Transforms.located(this);
    }

    /**
     * Parses one or more elements separated by {@code sep}.
     *
     * @param sep separator parser
     * @param <SEP> separator result type
     * @return a parser returning parsed values
     */
    public <SEP> Taker<List<A>> oneOrMoreSeparatedBy(Taker<SEP> sep) {
        return Repetition.separatedBy(this, sep, 1);
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
        return Repetition.foldRepeated(this, 0, identitySupplier, accumulator);
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
        return Repetition.foldRepeated(this, 1, identitySupplier, accumulator);
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
        return Repetition.foldSeparatedBy(this, sep, 1, identitySupplier, accumulator);
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
        return Repetition.foldSeparatedBy(this, sep, 0, identitySupplier, accumulator);
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
        return Repetition.foldRepeated(this, min, () -> null, (ignored, value) -> null);
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
        return foldOneOrMoreFrom(StringBuilder::new, StringBuilder::append)
            .map(StringBuilder::toString);
    }

    /**
     * Initializes a parser reference with another parser's behavior.
     *
     * @param parser parser used by this reference
     */
    public synchronized void set(Taker<A> parser) {
        if (parser == null) {
            throw new IllegalArgumentException("parser cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Taker already has an applyHandler");
        }
        this.applyHandler = parser.applyHandler;
    }

    /**
     * Initializes a parser reference with a custom apply handler.
     *
     * @param applyHandler parser implementation used by this reference
     */
    public synchronized void set(Function<Input, Result<A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Taker already has an applyHandler");
        }
        this.applyHandler = applyHandler;
    }


    /**
     * Parses zero or more values until {@code terminator} succeeds.
     *
     * @param terminator parser that stops repetition
     * @return a parser returning values parsed before the terminator
     */
    public Taker<List<A>> zeroOrMoreUntil(Taker<?> terminator) {
        return Repetition.repeat(this, 0, Integer.MAX_VALUE, terminator);
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
     * Constructor used by parser references that are initialized later.
     */
    protected Taker() {
        this.applyHandler = defaultApplyHandler = in -> {
            throw new IllegalStateException("Taker not initialized");
        };
    }

    /**
     * Creates a parser with the specified apply handler.
     *
     * @param applyHandler parser implementation
     */
    public Taker(Function<Input, Result<A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        this.applyHandler = applyHandler;
    }

    /**
     * Creates an uninitialized parser reference for recursive grammar definitions.
     *
     * @param <A> parser result type
     * @return a parser reference to initialize with {@link #set(Taker)}
     */
    public static <A> Taker<A> ref() {
        return new CheckParser<>();
    }

    /**
     * Creates a parser that attempts to recover by trying an alternative parser.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the
     * recovery parser is applied to the same starting input.
     * <p>
     * This is useful for error recovery in situations where there are multiple valid alternatives,
     * and you want to try them in sequence.
     *
     * @param recovery the parser to try if this parser fails
     * @param <B> the result type of the recovery parser
     * @return a new parser that tries the recovery parser if this parser fails
     */
    public <B> Taker<B> recover(Taker<B> recovery) {
        return Recovery.recover(this, recovery);
    }

    /**
     * Creates a parser that recovers by applying a function to the failure.
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
        return Recovery.recoverWith(this, recovery);
    }

    /**
     * Labels this parser with a human-readable expectation for error messages.
     *
     * @param label descriptive label
     * @return a labeled parser
     */
    public Taker<A> expecting(String label) {
        return Transforms.expecting(this, label);
    }

    /**
     * Adds a grammar label to this parser's failures.
     * <p>
     * This is useful for naming larger grammar rules while preserving the
     * lower-level failure as a cause. Use {@link #expecting(String)} when you
     * want to relabel a specific expected token; use {@code label(...)} when
     * you want the error path to include a rule such as {@code expression},
     * {@code statement}, or {@code object literal}.
     *
     * @param label grammar label
     * @return a parser that adds the label to failed diagnostics
     */
    public Taker<A> label(String label) {
        return Transforms.label(this, label);
    }

    /**
     * Uses this parser's successful value to choose the next parser.
     * <p>
     * This is useful when later grammar depends on an earlier parsed value.
     *
     * @param f   function returning the next parser
     * @param <B> next result type
     * @return a monadic parser
     */
    public <B> Taker<B> flatMap(Function<A, Taker<B>> f) {
        return Transforms.flatMap(this, f);
    }

}
