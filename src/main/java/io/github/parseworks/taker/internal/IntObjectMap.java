package io.github.parseworks.taker.internal;

import java.util.Arrays;

public final class IntObjectMap<V> {
    private static final int DEFAULT_CAPACITY = 16;
    private int[] keys = new int[DEFAULT_CAPACITY];
    private Object[] values = new Object[DEFAULT_CAPACITY];
    private int size;

    public void put(int key, V value) {
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
    public V get(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return (V) values[i];
            }
        }
        return null;
    }

    public void remove(int key) {
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
