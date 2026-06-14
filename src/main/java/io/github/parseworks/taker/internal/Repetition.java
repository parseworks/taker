package io.github.parseworks.taker.internal;

import io.github.parseworks.taker.*;

import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.results.PartialMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class Repetition {

    private Repetition() {
    }

    public static <A> Taker<List<A>> repeat(Taker<A> parser, int min, int max, Taker<?> until) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("The number of repetitions cannot be negative");
        }
        if (min > max) {
            throw new IllegalArgumentException("The minimum number of repetitions cannot be greater than the maximum");
        }
        return new Taker<>(in -> {
            List<A> buffer = new ArrayList<>();
            Input current = in;
            int count = 0;

            while (true) {
                if (until != null) {
                    Result<?> termRes = until.apply(current);
                    if (termRes.matches()) {
                        if (count < min) {
                            return new NoMatch<>(
                                current,
                                "expected at least " + min + " items (found only " + count + " before terminator)");
                        }
                        return new Match<>(Collections.unmodifiableList(buffer), termRes.input());
                    }
                }
                if (current.isEof() || count >= max) {
                    if (count >= min && until == null) {
                        return new Match<>(Collections.unmodifiableList(buffer), current);
                    }
                    String reason = current.isEof() ? "end of input reached" : "maximum repetitions reached";
                    return new NoMatch<>(current, min + " repetitions (" + reason + ")");
                }
                Result<A> res = parser.apply(current);
                if (!res.matches()) {
                    if (res.type() == ResultType.PARTIAL) {
                        return res.cast();
                    }
                    if (until != null) {
                        return res.cast();
                    }
                    if (count >= min) {
                        return new Match<>(Collections.unmodifiableList(buffer), current);
                    }
                    return new NoMatch<>(
                        current,
                        "at least " + min + " repetition(s)",
                        (NoMatch<?>) res
                    );
                }
                if (current.position() == res.input().position()) {
                    return new NoMatch<>(
                        current,
                        "parser to consume input during repetition"
                    );
                }
                buffer.add(res.value());
                current = res.input();
                count++;
            }
        });
    }

    public static <A, SEP> Taker<List<A>> separatedBy(Taker<A> parser, Taker<SEP> sep, int min) {
        Objects.requireNonNull(sep, "sep");
        return new Taker<>(in -> {
            List<A> values = new ArrayList<>();
            Result<A> first = parser.apply(in);
            if (!first.matches()) {
                if (first.type() == ResultType.PARTIAL) {
                    return first.cast();
                }
                if (min == 0) {
                    return new Match<>(Collections.emptyList(), in);
                }
                return first.cast();
            }

            values.add(first.value());
            Input current = first.input();

            while (true) {
                Result<SEP> sepResult = sep.apply(current);
                if (!sepResult.matches()) {
                    if (sepResult.type() == ResultType.PARTIAL) {
                        return sepResult.cast();
                    }
                    return new Match<>(Collections.unmodifiableList(values), current);
                }

                Result<A> next = parser.apply(sepResult.input());
                if (!next.matches()) {
                    if (next.type() == ResultType.PARTIAL) {
                        return next.cast();
                    }
                    if (next.input().position() > current.position() || sepResult.input().position() > current.position()) {
                        return new PartialMatch<>(next.input(), (Failure<A>) next).cast();
                    }
                    return new Match<>(Collections.unmodifiableList(values), current);
                }

                if (current.position() == next.input().position()) {
                    return new NoMatch<>(current, "separator and parser to consume input during separated repetition");
                }

                values.add(next.value());
                current = next.input();
            }
        });
    }

    public static <A, B> Taker<B> foldRepeated(
        Taker<A> parser,
        int min,
        Supplier<? extends B> identitySupplier,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        Objects.requireNonNull(identitySupplier, "identitySupplier");
        Objects.requireNonNull(accumulator, "accumulator");
        return new Taker<>(in -> {
            B accumulated = identitySupplier.get();
            Input current = in;
            int count = 0;

            while (true) {
                Result<A> parsed = parser.apply(current);
                if (!parsed.matches()) {
                    if (parsed.type() == ResultType.PARTIAL) {
                        return parsed.cast();
                    }
                    if (count >= min) {
                        return new Match<>(accumulated, current);
                    }
                    return new NoMatch<>(current, "at least " + min + " repetition(s)", (Failure<?>) parsed);
                }
                if (current.position() == parsed.input().position()) {
                    return new NoMatch<>(current, "parser to consume input during folded repetition");
                }
                accumulated = accumulator.apply(accumulated, parsed.value());
                current = parsed.input();
                count++;
            }
        });
    }

    public static <A, SEP, B> Taker<B> foldSeparatedBy(
        Taker<A> parser,
        Taker<SEP> sep,
        int min,
        Supplier<? extends B> identitySupplier,
        BiFunction<? super B, ? super A, ? extends B> accumulator
    ) {
        Objects.requireNonNull(sep, "sep");
        Objects.requireNonNull(identitySupplier, "identitySupplier");
        Objects.requireNonNull(accumulator, "accumulator");
        return new Taker<>(in -> {
            B accumulated = identitySupplier.get();
            Result<A> first = parser.apply(in);
            if (!first.matches()) {
                if (first.type() == ResultType.PARTIAL) {
                    return first.cast();
                }
                if (min == 0) {
                    return new Match<>(accumulated, in);
                }
                return first.cast();
            }

            accumulated = accumulator.apply(accumulated, first.value());
            Input current = first.input();

            while (true) {
                Result<SEP> sepResult = sep.apply(current);
                if (!sepResult.matches()) {
                    if (sepResult.type() == ResultType.PARTIAL) {
                        return sepResult.cast();
                    }
                    return new Match<>(accumulated, current);
                }

                Result<A> next = parser.apply(sepResult.input());
                if (!next.matches()) {
                    if (next.type() == ResultType.PARTIAL) {
                        return next.cast();
                    }
                    if (next.input().position() > current.position() || sepResult.input().position() > current.position()) {
                        return new PartialMatch<>(next.input(), (Failure<A>) next).cast();
                    }
                    return new Match<>(accumulated, current);
                }

                if (current.position() == next.input().position()) {
                    return new NoMatch<>(current, "separator and parser to consume input during folded separated repetition");
                }

                accumulated = accumulator.apply(accumulated, next.value());
                current = next.input();
            }
        });
    }
}
