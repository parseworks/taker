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
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;

import java.util.Objects;
import java.util.function.Function;

public final class Recovery {

    private Recovery() {
    }

    public static <A, B> Taker<B> recover(Taker<A> parser, Taker<B> recovery) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(recovery, "recovery");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply(input);
        });
    }

    public static <A, B> Taker<B> recoverWith(Taker<A> parser, Function<Failure<A>, Result<B>> recovery) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(recovery, "recovery");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply((Failure<A>) result);
        });
    }
}
