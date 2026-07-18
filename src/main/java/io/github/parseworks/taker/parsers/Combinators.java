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

package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.*;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.results.PartialMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Core parser combinators for choice, sequence, and filtering.
 */
public class Combinators {

    private Combinators() {
    }

    /**
     * Always succeeds without consuming input.
     *
     * @param value value to return
     * @param <A> result type
     * @return a parser returning {@code value}
     */
    public static <A> Taker<A> pure(A value) {
        return new Taker<>(input -> new Match<>(value, input));
    }

    /**
     * Commits the parser. If the parser fails and has consumed input, it returns
     * a PartialMatch.
     *
     * @param parser parser to commit
     * @param <A> result type
     * @return a committed parser
     */
    public static <A> Taker<A> commit(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            Input failureInput = result.input();
            if (!result.matches() && failureInput != null && failureInput.position() > in.position()) {
                return new PartialMatch<>(failureInput, (Failure<A>) result);
            }
            return result;
        });
    }

    /**
     * Wraps a parser to catch a specific exception type and convert it
     * into a {@link NoMatch} with the given expectation message.
     * <p>
     * Only the specified exception type is caught. All other exceptions
     * (NullPointerException, AssertionError, etc.) propagate unchanged
     * so real bugs are never silently swallowed.
     * <p>
     * The failure is reported at the input position where the parser was
     * invoked, not wherever the inner parser advanced to. This means
     * {@code attempt()} plays nicely with {@link #oneOf(Taker...)} because
     * alternatives retry from the correct position.
     *
     * @param <A>             result type
     * @param <E>             exception type (must extend {@link Exception})
     * @param parser          parser that may throw
     * @param exceptionType   the specific exception class to catch
     * @param expecting       error message for the resulting NoMatch
     * @return a parser that converts the expected exception into a NoMatch
     * @throws NullPointerException if {@code parser}, {@code exceptionType}, or {@code expecting} is null
     */
    public static <A, E extends Exception> Taker<A> attempt(
            Taker<A> parser, Class<E> exceptionType, String expecting) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(exceptionType, "exceptionType");
        Objects.requireNonNull(expecting, "expecting");
        return new Taker<>(in -> {
            try {
                return parser.apply(in);
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) {
                    return new NoMatch<>(in, expecting);
                }
                throw e;
            }
        });
    }

    /**
     * Wraps a parser to catch a specific exception type and delegate
     * result production to a custom handler.
     * <p>
     * The handler receives the caught exception and can decide whether
     * to return a {@link NoMatch}, {@link PartialMatch}, or even a
     * {@link Match} (e.g., for recovery scenarios).
     * <p>
     * Only the specified exception type is caught. All other exceptions
     * propagate unchanged.
     *
     * @param <A>             result type
     * @param <E>             exception type (must extend {@link Exception})
     * @param parser          parser that may throw
     * @param exceptionType   the specific exception class to catch
     * @param handler         function that maps the exception to a Result
     * @return a parser that delegates failure handling to the handler
     * @throws NullPointerException if {@code parser}, {@code exceptionType}, or {@code handler} is null
     */
    public static <A, E extends Exception> Taker<A> attempt(
            Taker<A> parser, Class<E> exceptionType,
            java.util.function.Function<E, Result<A>> handler) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(exceptionType, "exceptionType");
        Objects.requireNonNull(handler, "handler");
        return new Taker<>(in -> {
            try {
                return parser.apply(in);
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) {
                    return handler.apply(exceptionType.cast(e));
                }
                throw e;
            }
        });
    }

    /**
     * Matches any single character.
     *
     * @return a parser for one character
     */
    public static Taker<Character> any() {
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new NoMatch<Character>(input, "any character").cast();
            } else {
                return new Match<>(input.current(), input.next());
            }
        });
    }

    /**
     * Unconditionally throws an exception.
     *
     * @param supplier exception supplier
     * @return a parser that throws
     */
    public static Taker<? super Object> throwError(Supplier<? extends Exception> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return new Taker<>(in -> {
            throw sneakyThrow(supplier.get());
        });
    }

    /**
     * Utility method to bypass checked exception requirements.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }


    /**
     * Matches any of the given characters.
     *
     * @param items accepted characters
     * @return a parser for one matching character
     */
    public static Taker<Character> oneOf(char... items) {
        Objects.requireNonNull(items, "items");
        if (items.length == 0) {
            throw new IllegalArgumentException("There must be at least one character defined");
        }
        return Chars.oneOf(new String(items));
    }

    /**
     * Succeeds if the input is at the end of the file (EOF).
     *
     * @return an end-of-input parser
     */
    public static Taker<Void> eof() {
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new Match<>(null, input);
            } else {
                return new NoMatch<>(input, "end of input");
            }
        });
    }

    /**
     * Unconditionally fails, consuming no input.
     *
     * @param <A> result type
     * @return a parser that fails
     */
    public static <A> Taker<A> fail() {
        return new Taker<>(in -> new NoMatch<>(in, "parser explicitly set to fail"));
    }

    /**
     * Fails with a specific error message.
     *
     * @param expected expected label
     * @param <A> result type
     * @return a parser that fails
     */
    public static <A> Taker<A> fail(String expected) {
        Objects.requireNonNull(expected, "expected");
        return new Taker<>(in -> new NoMatch<>(in, expected));
    }

    /**
     * Succeeds without consuming input if the provided parser fails.
     * <p>
     * Use {@code not(parser).skipThen(any())} when the grammar should consume
     * the character that was validated by negative lookahead.
     *
     * @param parser parser to negate
     * @param <A> parser result type
     * @return a negative lookahead parser
     */
    public static <A> Taker<Void> not(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (result.matches()) {
                return new NoMatch<>(in, "parser not to match");
            }
            return new Match<>(null, in);
        });
    }

    /**
     * Matches anything except the given character.
     *
     * @param value rejected character
     * @return a parser for one different character
     */
    public static Taker<Character> isNot(char value) {
        String expected = "any character except " + expectedChar(value);
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expected);
            }
            char item = in.current();
            if (item == value) {
                return new NoMatch<>(in, expected);
            } else {
                return new Match<>(item, in.next());
            }
        });
    }


    /**
     * Matches the first succeeding parser in the list.
     *
     * @param parsers alternative parsers
     * @param <A> result type
     * @return a choice parser
     */
    public static <A> Taker<A> oneOf(List<Taker<A>> parsers) {
        Objects.requireNonNull(parsers, "parsers");
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one parser defined");
        }
        for (Taker<A> parser : parsers) {
            Objects.requireNonNull(parser, "parser");
        }
        return new Taker<>(in -> {
            List<Failure<A>> failures = null;
            int farthestFailurePosition = -1;

            for (Taker<A> parser : parsers) {
                Result<A> result = parser.apply(in);
                if (result.matches()) {
                    return result;
                }
                
                // If it's a hard failure (consumed input), stop and return it
                if (result.type() == ResultType.PARTIAL) {
                    if (failures != null) {
                        return new PartialMatch<>(result.input(), new io.github.parseworks.taker.results.NoMatch<>(failures));
                    }
                    return result;
                }

                int failurePosition = result.input() == null ? -1 : result.input().position();
                if (failurePosition < farthestFailurePosition) {
                    continue;
                }
                if (failurePosition > farthestFailurePosition) {
                    farthestFailurePosition = failurePosition;
                    failures = null;
                }
                if (failures == null){
                    failures = new ArrayList<>();
                }
                failures.add((Failure<A>) result);
            }
            Objects.requireNonNull(failures, "failures");
            return new NoMatch<>(failures);
        });
    }

    /**
     * Matches the first succeeding parser.
     *
     * @param <A>       the result type of all parsers
     * @param parsers   two or more alternative parsers (order matters — first match wins)
     * @return a choice parser that tries each in order
     */
    @SafeVarargs
    public static <A> Taker<A> oneOf(Taker<A>... parsers) {
        Objects.requireNonNull(parsers, "parsers");
        return oneOf(Arrays.asList(parsers));
    }

    /**
     * Applies multiple parsers in sequence, collecting results into a {@link List}.
     *
     * @param <A>       the result type of all parsers
     * @param parsers   ordered list of parsers (must not be empty)
     * @return a parser returning the collected results
     */
    public static <A> Taker<List<A>> sequence(List<Taker<A>> parsers) {
        Objects.requireNonNull(parsers, "parsers");
        for (Taker<A> parser : parsers) {
            Objects.requireNonNull(parser, "parser");
        }
        return new Taker<>(in -> {
            List<A> results = new ArrayList<>();
            Input currentInput = in;
            for (Taker<A> parser : parsers) {
                Result<A> result = parser.apply(currentInput);
                if (!result.matches()) {
                    return result.cast();
                }
                results.add(result.value());
                currentInput = result.input();
            }
            return new Match<>(results, currentInput);
        });
    }

    /**
     * Applies two parsers in sequence and returns an {@link ApplyBuilder}.
     *
     * @param <A>       the result type of both parsers
     * @param parserA   first parser
     * @param parserB   second parser
     * @return a builder for mapping both values
     */
    public static <A> ApplyBuilder<A, A> sequence(Taker<A> parserA, Taker<A> parserB) {
        Objects.requireNonNull(parserA, "parserA");
        Objects.requireNonNull(parserB, "parserB");
        return parserA.then(parserB);
    }

    /**
     * Applies three parsers in sequence and returns an ApplyBuilder3.
     *
     * @param parserA first parser
     * @param parserB second parser
     * @param parserC third parser
     * @param <A> result type of all parsers
     * @return a builder for mapping three values
     */
    public static <A> ApplyBuilder.ApplyBuilder3<A, A, A> sequence(Taker<A> parserA, Taker<A> parserB, Taker<A> parserC) {
        Objects.requireNonNull(parserA, "parserA");
        Objects.requireNonNull(parserB, "parserB");
        Objects.requireNonNull(parserC, "parserC");
        return parserA.then(parserB).then(parserC);
    }

    /**
     * Parses a value between optional opening and closing parsers.
     *
     * @param open opening parser, or {@code null}
     * @param parser value parser
     * @param close closing parser, or {@code null}
     * @param <A> value result type
     * @param <B> opening result type
     * @param <C> closing result type
     * @return a parser returning the value parser result
     */
    public static <A, B, C> Taker<A> between(Taker<B> open, Taker<A> parser, Taker<C> close) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Input current = in;
            if (open != null) {
                Result<B> resOpen = open.apply(current);
                if (!resOpen.matches()) return resOpen.cast();
                current = resOpen.input();
            }
            Result<A> resParser = parser.apply(current);
            if (!resParser.matches()) return resParser;
            current = resParser.input();
            if (close != null) {
                Result<C> resClose = close.apply(current);
                if (!resClose.matches()) return resClose.cast();
                current = resClose.input();
            }
            return new Match<>(resParser.value(), current);
        });
    }

    /**
     * Parses a value between two uses of the same bracket parser.
     *
     * @param bracket opening and closing parser
     * @param parser value parser
     * @param <A> value result type
     * @param <B> bracket result type
     * @return a parser returning the value parser result
     */
    public static <A, B> Taker<A> between(Taker<B> bracket, Taker<A> parser) {
        return between(bracket, parser, bracket);
    }

    /**
     * Parses a value between opening and closing characters.
     *
     * @param open opening character
     * @param parser value parser
     * @param close closing character
     * @param <A> value result type
     * @return a parser returning the value parser result
     */
    public static <A> Taker<A> between(char open, Taker<A> parser, char close) {
        return between(Chars.chr(open), parser, Chars.chr(close));
    }

    /**
     * Parses a value between two uses of the same bracket character.
     *
     * @param bracket opening and closing character
     * @param parser value parser
     * @param <A> value result type
     * @return a parser returning the value parser result
     */
    public static <A> Taker<A> between(char bracket, Taker<A> parser) {
        return between(bracket, parser, bracket);
    }

    /**
     * Matches a character satisfying the predicate.
     *
     * @param expectedType expected label
     * @param predicate predicate to satisfy
     * @return a parser for one matching character
     */
    public static Taker<Character> satisfy(String expectedType, CharPredicate predicate) {
        Objects.requireNonNull(expectedType, "expectedType");
        Objects.requireNonNull(predicate, "predicate");
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expectedType);
            }
            var item = in.current();
            if (predicate.test(item)) {
                return new Match<>(item, in.next());
            } else {
                return new NoMatch<>(in, expectedType);
            }
        });
    }

    /**
     * Matches the given value.
     *
     * @param equivalence value to match
     * @param <A> result type
     * @return a parser for the value
     */
    public static <A> Taker<A> is(A equivalence) {
        Objects.requireNonNull(equivalence, "equivalence");
        String expected = expectedValue(equivalence);
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expected);
            }
            char item = in.current();
            if (Objects.equals(item, equivalence)) {
                return new Match<>(equivalence, in.next());
            } else {
                return new NoMatch<>(in, expected);
            }
        });
    }

    /**
     * Chains a parser left-associatively.
     *
     * @param parser element parser
     * @param op operator parser
     * @param identity value returned when no element matches
     * @param <A> result type
     * @return a left-associative chain parser
     */
    public static <A> Taker<A> chainLeft(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op, A identity) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(op, "op");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) return new Match<>(identity, in);
            A value = result.value();
            Input current = result.input();

            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> nextResult = parser.apply(opResult.input());
                if (!nextResult.matches()) break;

                value = opResult.value().apply(value, nextResult.value());
                current = nextResult.input();
            }
            return new Match<>(value, current);
        });
    }

    /**
     * Chains a parser left-associatively, requiring at least one match.
     *
     * @param parser element parser
     * @param op operator parser
     * @param <A> result type
     * @return a left-associative chain parser
     */
    public static <A> Taker<A> chainLeft(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(op, "op");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) return result;
            A value = result.value();
            Input current = result.input();

            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> nextResult = parser.apply(opResult.input());
                if (!nextResult.matches()) break;

                value = opResult.value().apply(value, nextResult.value());
                current = nextResult.input();
            }
            return new Match<>(value, current);
        });
    }

    /**
     * Chains a parser right-associatively, returning {@code identity} when
     * this parser does not match.
     *
     * @param elem element parser
     * @param op operator parser producing a {@link java.util.function.BinaryOperator}
     * @param identity value returned when {@code elem} matches zero times
     * @param <A> result type
     * @return a chain parser (right-associative)
     */
    public static <A> Taker<A> chainRight(Taker<A> elem, Taker<java.util.function.BinaryOperator<A>> op, A identity) {
        Objects.requireNonNull(elem, "elem");
        Objects.requireNonNull(op, "op");
        return new Taker<>(in -> {
            Result<A> first = elem.apply(in);
            if (!first.matches()) return new Match<>(identity, in);

            // Collect operator+value pairs left-to-right without recursion
            List<java.util.function.BinaryOperator<A>> ops = null;
            List<A> values = null;
            Input current = first.input();
            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> next = elem.apply(opResult.input());
                if (!next.matches()) break;

                if (ops == null) {
                    ops = new ArrayList<>();
                    values = new ArrayList<>();
                }
                ops.add(opResult.value());
                values.add(next.value());
                current = next.input();
            }

            // Fold right-to-left to preserve right-associativity.
            if (ops == null) {
                return new Match<>(first.value(), current);
            }

            A acc = values.getLast();
            for (int i = ops.size() - 1; i > 0; i--) {
                acc = ops.get(i).apply(values.get(i - 1), acc);
            }
            acc = ops.getFirst().apply(first.value(), acc);
            return new Match<>(acc, current);
        });
    }

    /**
     * Chains a parser right-associatively, requiring at least one match.
     *
     * @param elem element parser
     * @param op operator parser producing a {@link java.util.function.BinaryOperator}
     * @param <A> result type
     * @return a chain parser (right-associative)
     */
    public static <A> Taker<A> chainRight(Taker<A> elem, Taker<java.util.function.BinaryOperator<A>> op) {
        Objects.requireNonNull(elem, "elem");
        Objects.requireNonNull(op, "op");
        return new Taker<>(in -> {
            Result<A> first = elem.apply(in);
            if (!first.matches()) return first;

            // Collect operator+value pairs left-to-right without recursion
            List<java.util.function.BinaryOperator<A>> ops = null;
            List<A> values = null;
            Input current = first.input();
            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> next = elem.apply(opResult.input());
                if (!next.matches()) break;

                if (ops == null) {
                    ops = new ArrayList<>();
                    values = new ArrayList<>();
                }
                ops.add(opResult.value());
                values.add(next.value());
                current = next.input();
            }

            // Fold right-to-left to preserve right-associativity.
            A acc = first.value();
            if (ops != null) {
                acc = values.getLast();
                for (int i = ops.size() - 1; i > 0; i--) {
                    acc = ops.get(i).apply(values.get(i - 1), acc);
                }
                acc = ops.getFirst().apply(first.value(), acc);
            }
            return new Match<>(acc, current);
        });
    }

    private static String expectedValue(Object equivalence) {
        if (equivalence instanceof Character c) {
            return expectedChar(c);
        }
        return String.valueOf(equivalence);
    }

    private static String expectedChar(char c) {
        return "'" + CharPredicate.display(c) + "'";
    }

}
