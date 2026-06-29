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

package io.github.parseworks.taker.results;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.ResultType;

import java.util.List;
import java.util.function.Function;

/**
 * Recoverable parser failure.
 * <p>
 * Choice combinators may try later alternatives after a {@code NoMatch}. Use
 * {@link PartialMatch} when a branch should be treated as committed.
 *
 * @param input input cursor where the failure should be reported
 * @param expected human-readable expectation
 * @param cause optional underlying cause
 * @param combinedFailures tied failures from alternative parsers
 * @param context whether {@code expected} is a grammar context label
 * @param <A> the type of the parsed value
 */
public record NoMatch<A>(
        Input input,
        String expected,
        Failure<?> cause,
        List<Failure<A>> combinedFailures,
        boolean context
) implements Failure<A> {

    /**
     * Constructs a recoverable failure at {@code input}.
     */
    public NoMatch(Input input, String expected) {
        this(input, expected, null, null, false);
    }

    /**
     * Constructs a recoverable failure with an underlying cause.
     */
    public NoMatch(Input input, String expected, Failure<?> cause) {
        this(input, expected, cause, null, false);
    }

    /**
     * Constructs a recoverable failure with an underlying cause and optional
     * grammar context semantics.
     */
    public NoMatch(Input input, String expected, Failure<?> cause, boolean context) {
        this(input, expected, cause, null, context);
    }

    /**
     * Constructs a recoverable failure from tied alternative failures.
     */
    public NoMatch(List<Failure<A>> failures) {
        this(failures.isEmpty() ? null : failures.getFirst().input(), null, null, failures, false);
    }

    @Override
    public ResultType type() {
        return ResultType.NO_MATCH;
    }

    @Override
    public boolean matches() {
        return false;
    }

    @Override
    public A value() {
        throw new RuntimeException(error());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> cast() {
        return (Result<B>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> map(Function<A, B> mapper) {
        return (Result<B>) this;
    }

    @Override
    public <B> B handle(Function<Result<A>, B> success, Function<Result<A>, B> failure) {
        return failure.apply(this);
    }

}
