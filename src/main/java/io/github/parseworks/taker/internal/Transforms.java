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

package io.github.parseworks.taker.internal;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Located;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Internal helpers for transforming parser results.
 */
public final class Transforms {

    private Transforms() {
    }

    /**
     * Creates a parser that returns a constant value if the underlying parser
     * succeeds.
     *
     * @param <A> underlying result type
     * @param <R> constant result type
     * @param parser the underlying parser
     * @param value the constant value to return
     * @return a constant-returning parser
     */
    public static <A, R> Taker<R> as(Taker<A> parser, R value) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            return new Match<>(value, result.input());
        });
    }

    /**
     * Creates a parser that returns an {@link Optional} containing the result
     * if the underlying parser succeeds, or {@link Optional#empty()} if it
     * fails.
     *
     * @param <A> result type
     * @param parser the underlying parser
     * @return an optional parser
     */
    public static <A> Taker<Optional<A>> optional(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return new Match<>(Optional.empty(), in);
            }
            return new Match<>(Optional.of(result.value()), result.input());
        });
    }

    /**
     * Creates a parser that returns the specified default value if the
     * underlying parser fails.
     *
     * @param <A> result type
     * @param parser the underlying parser
     * @param other the default value
     * @return a parser with a default value
     */
    public static <A> Taker<A> orElse(Taker<A> parser, A other) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return new Match<>(other, in);
            }
            return result;
        });
    }

    /**
     * Maps the result of a parser using a function.
     *
     * @param <A> input result type
     * @param <R> output result type
     * @param parser the underlying parser
     * @param mapper the mapping function
     * @return a mapped parser
     */
    public static <A, R> Taker<R> map(Taker<A> parser, Function<A, R> mapper) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(mapper, "mapper");
        return new Taker<>(in -> parser.apply(in).map(mapper));
    }

    /**
     * Creates a parser that returns the result wrapped in a {@link Located}
     * object containing start and end positions.
     *
     * @param <A> result type
     * @param parser the underlying parser
     * @return a located parser
     */
    public static <A> Taker<Located<A>> located(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            int start = in.position();
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            return new Match<>(new Located<>(result.value(), start, result.input().position()), result.input());
        });
    }

    /**
     * Assigns an expected label to a parser, used in error messages.
     *
     * @param <A> result type
     * @param parser the underlying parser
     * @param label the expected label
     * @return a parser with an expected label
     */
    public static <A> Taker<A> expecting(Taker<A> parser, String label) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(label, "label");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result;
            }
            return new NoMatch<>(result.input(), label, (Failure<?>) result);
        });
    }

    /**
     * Assigns a name label to a parser, typically used for better debugging.
     *
     * @param <A> result type
     * @param parser the underlying parser
     * @param label the label
     * @return a labeled parser
     */
    public static <A> Taker<A> label(Taker<A> parser, String label) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(label, "label");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result;
            }
            return new NoMatch<>(result.input(), label, (Failure<?>) result, true);
        });
    }

    /**
     * Chains two parsers where the second parser depends on the result of the
     * first.
     *
     * @param <A> first result type
     * @param <B> second result type
     * @param parser the first parser
     * @param f function that returns the second parser based on the first result
     * @return a flat-mapped parser
     */
    public static <A, B> Taker<B> flatMap(Taker<A> parser, Function<A, Taker<B>> f) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(f, "f");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            Taker<B> next = f.apply(result.value());
            if (next == null) {
                return new NoMatch<B>(result.input(), "parser to function correctly").cast();
            }
            return next.apply(result.input());
        });
    }
}
