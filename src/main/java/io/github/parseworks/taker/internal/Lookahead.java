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

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.NoMatch;

import java.util.Objects;

public final class Lookahead {

    private Lookahead() {
    }

    public static <A, B> Taker<A> onlyIf(Taker<A> parser, Taker<B> validation) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(validation, "validation");
        return new Taker<>(input -> {
            Result<B> validationResult = validation.apply(input);
            if (!validationResult.matches()) {
                return validationResult.cast();
            }
            return parser.apply(input);
        });
    }

    public static <A> Taker<A> onlyIf(Taker<A> parser, CharPredicate validation) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(validation, "validation");
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new NoMatch<>(input, "Expected Character at " + input.position());
            }
            if (!validation.test(input.current())) {
                return new NoMatch<>(input, "Predicate failed");
            }
            return parser.apply(input);
        });
    }

    public static <A, B> Taker<A> peek(Taker<A> parser, Taker<B> lookahead) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(lookahead, "lookahead");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
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
}
