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

import java.util.Arrays;

/**
 * Packrat memo table using open-addressing hash table.
 * <p>
 * Parallel {@code int[]} and {@code Result<?>[]} arrays with linear probing.
 * {@code -1} is the empty sentinel (input positions are always &ge; 0).
 * No boxing, no allocation on cache hits, load factor &le; 0.5.
 * <p>
 * Created once per parse by {@link io.github.parseworks.taker.Taker#memoize()}.
 * All cursors in that parse share the same instance via the {@link Context}
 * chain, so the memo is scoped to a single parse and cleared naturally
 * when the wrapper is discarded.
 */
public final class Memo {

    private static final int EMPTY = -1;
    private static final double LOAD_FACTOR = 0.5;
    private static final int MAX_CAPACITY = 1 << 20; // ~1M entries, ~16MB total

    private int[] positions;
    private Result<?>[] results;
    private int count;
    private int threshold;
    private boolean atLimit;

    /** Creates an empty memo table. */
    public Memo() {
        this.positions = new int[16];
        this.results = new Result<?>[16];
        this.threshold = (int) (16 * LOAD_FACTOR);
        fillEmpty();
    }

    /** Fill the positions array with EMPTY sentinel. */
    private void fillEmpty() {
        Arrays.fill(positions, EMPTY);
    }

    /**
     * Returns a cached result for {@code position}, or {@code null} on miss.
     *
     * @param position input position
     * @param <A> cached result type
     * @return cached result, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <A> Result<A> get(int position) {
        int len = positions.length;
        int i = hash(position) & (len - 1);
        int[] p = positions;
        Result<?>[] r = results;

        while (p[i] != EMPTY) {
            if (p[i] == position) {
                @SuppressWarnings("unchecked") Result<A> hit = (Result<A>) r[i];
                return hit;
            }
            i = (i + 1) & (len - 1);
        }
        return null;
    }

    /**
     * Stores a result for {@code position}. Silently ignores if already present.
     * When at capacity limit, overwrites the probed slot (effectively evicts).
     *
     * @param position input position
     * @param result result to store
     */
    public void put(int position, Result<?> result) {
        int len = positions.length;
        int i = hash(position) & (len - 1);
        int[] p = positions;
        Result<?>[] r = results;

        while (p[i] != EMPTY) {
            if (p[i] == position) {
                r[i] = result;
                return;
            }
            i = (i + 1) & (len - 1);
        }
        p[i] = position;
        r[i] = result;
        if (!atLimit) {
            count++;
            if (count > threshold) {
                resize();
            }
        }
    }

    /** Fast hash: good enough distribution for sequential parser positions. */
    private static int hash(int value) {
        int h = value * 0x9E3779B9;
        return h & Integer.MAX_VALUE;
    }

    /** Double the table size and rehash all entries. Stops at MAX_CAPACITY. */
    private void resize() {
        if (positions.length >= MAX_CAPACITY) {
            atLimit = true;
            return;
        }
        int oldLen = positions.length;
        int newLen = oldLen << 1;
        int[] newPos = new int[newLen];
        Result<?>[] newRes = new Result<?>[newLen];
        fillEmptyImpl(newPos);

        int[] oldPos = positions;
        Result<?>[] oldRes = results;

        for (int i = 0; i < oldLen; i++) {
            int key = oldPos[i];
            if (key != EMPTY) {
                int j = hash(key) & (newLen - 1);
                while (newPos[j] != EMPTY) {
                    j = (j + 1) & (newLen - 1);
                }
                newPos[j] = key;
                newRes[j] = oldRes[i];
            }
        }

        positions = newPos;
        results = newRes;
        threshold = (int) (newLen * LOAD_FACTOR);
    }

    private void fillEmptyImpl(int[] arr) {
        Arrays.fill(arr, EMPTY);
    }
}
