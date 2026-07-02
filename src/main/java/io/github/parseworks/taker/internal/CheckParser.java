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

import io.github.parseworks.taker.Context;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.NoMatch;

/**
 * A parser wrapper that checks for infinite recursion and caches results
 * per input position (packrat memoization).
 * <p>
 * Created by {@link Taker#ref()}. Memoization is opt-in at the parse call
 * site: use {@link Taker#memoize()} to activate the cache.
 * When no memo table is attached, the null-check path adds negligible overhead.
 *
 * @param <A> result type
 */
public class CheckParser<A> extends Taker<A> {

    @Override
    public Result<A> apply(Input in) {
        int pos = in.position();

        // Single walk: memo hit, recursion guard, or proceed
        Context.Find f = Context.find(in.context(), pos, this);
        if (f instanceof Context.Find.Memo mf) {
            @SuppressWarnings("unchecked") Result<A> cached = (Result<A>) mf.result;
            return cached;
        }
        if (f instanceof Context.Find.Recursion) {
            return new NoMatch<>(in, "no infinite recursion");
        }

        Context ctx = in.context();
        Input inWithCtx = in.withContext(Context.push(ctx, pos, this));
        Result<A> result = applyHandler.apply(inWithCtx);
        Context.store(ctx, pos, result);
        return result;
    }
}
