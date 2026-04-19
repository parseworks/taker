package io.github.parseworks.taker;

import java.util.*;

/**
 * Utility functions for working with {@link List} in a functional style.
 */
public final class Lists {

    private Lists() {}

    public static <T> List<T> prepend(T head, List<T> tail) {
        List<T> list = new ArrayList<>(tail.size() + 1);
        list.add(head);
        list.addAll(tail);
        return List.copyOf(list);
    }

    public static <T> List<T> append(List<T> list, T element) {
        List<T> result = new ArrayList<>(list.size() + 1);
        result.addAll(list);
        result.add(element);
        return List.copyOf(result);
    }

    public static <T, B> B foldLeft(List<T> list, B identity, java.util.function.BiFunction<B, ? super T, B> folder) {
        B result = identity;
        for (T t : list) result = folder.apply(result, t);
        return result;
    }

    public static <T, B> B foldRight(List<T> list, B identity, java.util.function.BiFunction<? super T, B, B> folder) {
        B result = identity;
        ListIterator<T> it = list.listIterator(list.size());
        while (it.hasPrevious()) result = folder.apply(it.previous(), result);
        return result;
    }

    public static String join(List<?> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object o : list) sb.append(o);
        return sb.toString();
    }
}
