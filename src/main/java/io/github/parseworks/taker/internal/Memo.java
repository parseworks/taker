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

import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;

import java.util.Arrays;

/**
 * Internal packrat memo table keyed by parser identity and input position.
 */
public final class Memo {

    private static final int EMPTY = -1;
    private static final double LOAD_FACTOR = 0.5;
    private static final int MAX_CAPACITY = 1 << 20;

    private int[] positions;
    private Taker<?>[] takers;
    private Result<?>[] results;
    private int count;
    private int threshold;
    private boolean atLimit;

    /** Creates an empty memo table. */
    public Memo() {
        this.positions = new int[16];
        this.takers = new Taker<?>[16];
        this.results = new Result<?>[16];
        this.threshold = (int) (16 * LOAD_FACTOR);
        fillEmpty();
    }

    private void fillEmpty() {
        Arrays.fill(positions, EMPTY);
    }

    /**
     * Returns a cached result for {@code taker} at {@code position}, or {@code null} on miss.
     *
     * @param position input position
     * @param taker parser identity
     * @param <A> cached result type
     * @return cached result, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <A> Result<A> get(int position, Taker<?> taker) {
        int len = positions.length;
        int i = hash(position, taker) & (len - 1);
        int[] p = positions;
        Taker<?>[] t = takers;
        Result<?>[] r = results;

        while (p[i] != EMPTY) {
            if (p[i] == position && t[i] == taker) {
                @SuppressWarnings("unchecked") Result<A> hit = (Result<A>) r[i];
                return hit;
            }
            i = (i + 1) & (len - 1);
        }
        return null;
    }

    /**
     * Stores a result for {@code taker} at {@code position}.
     *
     * @param position input position
     * @param taker parser identity
     * @param result result to store
     */
    public void put(int position, Taker<?> taker, Result<?> result) {
        int len = positions.length;
        int i = hash(position, taker) & (len - 1);
        int[] p = positions;
        Taker<?>[] t = takers;
        Result<?>[] r = results;

        while (p[i] != EMPTY) {
            if (p[i] == position && t[i] == taker) {
                r[i] = result;
                return;
            }
            i = (i + 1) & (len - 1);
        }
        p[i] = position;
        t[i] = taker;
        r[i] = result;
        if (!atLimit) {
            count++;
            if (count > threshold) {
                resize();
            }
        }
    }

    private static int hash(int position, Taker<?> taker) {
        int h = position * 0x9E3779B9 ^ System.identityHashCode(taker);
        h ^= h >>> 16;
        return h & Integer.MAX_VALUE;
    }

    private void resize() {
        if (positions.length >= MAX_CAPACITY) {
            atLimit = true;
            return;
        }

        int oldLen = positions.length;
        int newLen = oldLen << 1;
        int[] oldPositions = positions;
        Taker<?>[] oldTakers = takers;
        Result<?>[] oldResults = results;

        positions = new int[newLen];
        takers = new Taker<?>[newLen];
        results = new Result<?>[newLen];
        threshold = (int) (newLen * LOAD_FACTOR);
        fillEmpty();

        for (int i = 0; i < oldLen; i++) {
            int pos = oldPositions[i];
            if (pos != EMPTY) {
                insertRehash(pos, oldTakers[i], oldResults[i]);
            }
        }
    }

    private void insertRehash(int position, Taker<?> taker, Result<?> result) {
        int len = positions.length;
        int i = hash(position, taker) & (len - 1);
        while (positions[i] != EMPTY) {
            i = (i + 1) & (len - 1);
        }
        positions[i] = position;
        takers[i] = taker;
        results[i] = result;
    }
}
