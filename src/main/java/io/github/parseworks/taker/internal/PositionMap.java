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
