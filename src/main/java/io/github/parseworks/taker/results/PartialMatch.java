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
 * Committed parser failure.
 * <p>
 * Choice combinators do not try later alternatives after a {@code PartialMatch}.
 * This is used for committed branches and for successful parses that fail a
 * full-input requirement such as {@link io.github.parseworks.taker.Taker#parseAll(CharSequence)}.
 *
 * @param input input cursor where the failure should be reported
 * @param cause underlying failure cause
 * @param <A> the type of the parsed value
 */
public record PartialMatch<A>(
        Input input,
        Failure<A> cause
) implements Failure<A> {

    @Override
    public Input input() {
        return cause.input();
    }

    @Override
    public Failure<A> cause() {
        return cause;
    }

    @Override
    public String expected() {
        return cause.expected();
    }

    @Override
    public List<Failure<A>> combinedFailures() {
        return cause.combinedFailures();
    }

    @Override
    public ResultType type() {
        return ResultType.PARTIAL;
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

    public String toString() {
        return "PartialMatch(" + cause + ")";
    }
}
