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

import java.util.Arrays;

/**
 * Simple open-addressing int-to-object map used by recursion guards.
 * Not thread-safe callers must manage their own concurrency.
 */
final class PositionMap<V> {
    private static final int DEFAULT_CAPACITY = 16;

    private int[] keys = new int[DEFAULT_CAPACITY];
    private Object[] values = new Object[DEFAULT_CAPACITY];
    private int size;

    void put(int key, V value) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                values[i] = value;
                return;
            }
        }

        if (size == keys.length) {
            grow();
        }

        keys[size] = key;
        values[size] = value;
        size++;
    }

    @SuppressWarnings("unchecked")
    V get(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return (V) values[i];
            }
        }
        return null;
    }

    void remove(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                if (i < size - 1) {
                    System.arraycopy(keys, i + 1, keys, i, size - i - 1);
                    System.arraycopy(values, i + 1, values, i, size - i - 1);
                }
                values[size - 1] = null;
                size--;
                return;
            }
        }
    }

    private void grow() {
        int newCapacity = keys.length * 2;
        keys = Arrays.copyOf(keys, newCapacity);
        values = Arrays.copyOf(values, newCapacity);
    }
}
