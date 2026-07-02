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

/**
 * Immutable parse context: a linked chain of (position, parser) frames
 * used for infinite-recursion detection and packrat memoization.
 * <p>
 * The chain root carries a shared {@link Memo} reference. Each frame
 * caches the memo so {@link #push} and {@link #find} are O(1) for
 * memo access, with only the recursion guard requiring an O(depth) walk.
 * <p>
 * {@code null} represents an empty context.
 */
public final class Context {

    // ── lookup outcome hierarchy ──────────────────────────────────

    /** Result of {@link Context#find} — memo hit, recursion, or proceed. */
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
        /** The same (parser, position) is already on the recursion stack. */
        public static final class Recursion extends Find {
            static final Recursion INSTANCE = new Recursion();
            private Recursion() {}
        }

        /** No cache hit, no recursion — proceed with normal parsing. */
        public static final class Proceed extends Find {
            static final Proceed INSTANCE = new Proceed();
            private Proceed() {}
        }
    }

    // ── chain node ────────────────────────────────────────────────

    private final int pos;
    private final Taker<?> taker;
    private final Context next;
    /** Shared memo table — set on the root, inherited by pushes. */
    private final Memo memo;

    private Context(int pos, Taker<?> taker, Context next, Memo memo) {
        this.pos = pos;
        this.taker = taker;
        this.next = next;
        this.memo = memo;
    }

    /**
     * Returns an empty context.
     *
     * @return an empty context
     */
    public static Context empty() {
        return null;
    }

    /**
     * Pushes a new frame onto the context, inheriting the memo from the head frame.
     * Each frame caches the memo reference so subsequent pushes are O(1).
     *
     * @param context current context
     * @param pos input position
     * @param taker parser for the new frame
     * @return context with the new frame
     */
    public static Context push(Context context, int pos, Taker<?> taker) {
        Memo memo = (context != null) ? context.memo : null;
        return new Context(pos, taker, context, memo);
    }

    /**
     * Attaches a memo table as the chain root.
     *
     * @param context current context
     * @param memo memo table to attach
     * @return context with memo support
     */
    public static Context withMemo(Context context, Memo memo) {
        return new Context(-1, null, context, memo);
    }

    /**
     * Memo lookup and recursion guard.
     * <p>
     * Memo is O(1) on the head frame. Recursion guard walks the stack.
     * Returns a {@link Find} indicating the outcome.
     *
     * @param context current context
     * @param pos input position
     * @param taker parser to find
     * @param <A> parser result type
     * @return lookup result
     */
    public static <A> Find find(Context context, int pos, Taker<A> taker) {
        // Memo is cached on every frame — O(1) head lookup
        Memo memo = (context != null) ? context.memo : null;
        if (memo != null) {
            Result<A> cached = memo.get(pos);
            if (cached != null) return new Find.Memo(cached);
        }

        // Walk the recursion stack
        Context current = context;
        while (current != null) {
            if (current.pos == pos && current.taker == taker) {
                return Find.Recursion.INSTANCE;
            }
            current = current.next;
        }

        return Find.Proceed.INSTANCE;
    }

    /** Returns the memo table cached on this frame, or {@code null}. */
    static Memo memo(Context context) {
        return (context != null) ? context.memo : null;
    }

    /**
     * Stores a result in the memo table if one is attached to the chain.
     * No-op when no memo is active.
     *
     * @param context current context
     * @param pos input position
     * @param result result to cache
     */
    public static void store(Context context, int pos, Result<?> result) {
        Memo memo = memo(context);
        if (memo != null) memo.put(pos, result);
    }
}
