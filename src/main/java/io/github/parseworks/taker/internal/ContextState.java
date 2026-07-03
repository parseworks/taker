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
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;

/**
 * Internal parse context state for recursion detection and packrat memoization.
 */
public final class ContextState extends Context {

    private static final ContextState EMPTY = new ContextState(-1, null, null, null);

    /** Lookup result: memo hit, recursion, or proceed. */
    public abstract static class Find {
        /** Creates a lookup result marker. */
        protected Find() {
        }

        /** A cached result was found in the memo table. */
        public static final class Memo extends Find {
            /** Cached parser result. */
            public final Result<?> result;
            Memo(Result<?> result) { this.result = result; }
        }

        /** The same parser and position is already on the recursion stack. */
        public static final class Recursion extends Find {
            static final Recursion INSTANCE = new Recursion();
            private Recursion() {}
        }

        /** No cache hit and no recursion; proceed with normal parsing. */
        public static final class Proceed extends Find {
            static final Proceed INSTANCE = new Proceed();
            private Proceed() {}
        }
    }

    private final int pos;
    private final Taker<?> taker;
    private final ContextState next;
    private final Memo memo;

    private ContextState(int pos, Taker<?> taker, ContextState next, Memo memo) {
        this.pos = pos;
        this.taker = taker;
        this.next = next;
        this.memo = memo;
    }

    /**
     * Returns an empty context state.
     *
     * @return an empty context state
     */
    public static ContextState empty() {
        return EMPTY;
    }

    /**
     * Pushes a new frame onto the context.
     *
     * @param context current context
     * @param pos input position
     * @param taker parser for the new frame
     * @return context with the new frame
     */
    public static ContextState push(Context context, int pos, Taker<?> taker) {
        ContextState current = asState(context);
        return new ContextState(pos, taker, current, current.memo);
    }

    /**
     * Attaches a memo table as the chain root.
     *
     * @param context current context
     * @param memo memo table to attach
     * @return context with memo support
     */
    public static ContextState withMemo(Context context, Memo memo) {
        return new ContextState(-1, null, asState(context), memo);
    }

    /**
     * Memo lookup and recursion guard.
     *
     * @param context current context
     * @param pos input position
     * @param taker parser to find
     * @param <A> parser result type
     * @return lookup result
     */
    public static <A> Find find(Context context, int pos, Taker<A> taker) {
        ContextState current = asState(context);
        Memo memo = current.memo;
        if (memo != null) {
            Result<A> cached = memo.get(pos, taker);
            if (cached != null) return new Find.Memo(cached);
        }

        while (current != EMPTY) {
            if (current.pos == pos && current.taker == taker) {
                return Find.Recursion.INSTANCE;
            }
            current = current.next;
        }

        return Find.Proceed.INSTANCE;
    }

    /**
     * Stores a result in the memo table if one is attached to the chain.
     *
     * @param context current context
     * @param pos input position
     * @param taker parser identity
     * @param result result to cache
     */
    public static void store(Context context, int pos, Taker<?> taker, Result<?> result) {
        Memo memo = asState(context).memo;
        if (memo != null) memo.put(pos, taker, result);
    }

    private static ContextState asState(Context context) {
        if (context instanceof ContextState state) {
            return state;
        }
        return EMPTY;
    }
}
