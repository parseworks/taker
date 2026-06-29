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

import io.github.parseworks.taker.Taker;

/**
 * A linked list-based linear map with no side effects.
 */
public final class LinearMap {
    private final int pos;
    private final Taker<?> taker;
    private final LinearMap next;

    private LinearMap(int pos, Taker<?> taker, LinearMap next) {
        this.pos = pos;
        this.taker = taker;
        this.next = next;
    }

    public static LinearMap empty() {
        return null;
    }

    public static LinearMap push(LinearMap map, int pos, Taker<?> taker) {
        return new LinearMap(pos, taker, map);
    }

    public static boolean contains(LinearMap map, int pos, Taker<?> taker) {
        LinearMap current = map;
        while (current != null) {
            if (current.pos == pos && current.taker == taker) {
                return true;
            }
            current = current.next;
        }
        return false;
    }
}
