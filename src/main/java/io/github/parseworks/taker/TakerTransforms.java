package io.github.parseworks.taker;

import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.util.Optional;
import java.util.function.Function;

final class TakerTransforms {

    private TakerTransforms() {
    }

    static <A, R> Taker<R> as(Taker<A> parser, R value) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            return new Match<>(value, result.input());
        });
    }

    static <A> Taker<Optional<A>> optional(Taker<A> parser) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return new Match<>(Optional.empty(), in);
            }
            return new Match<>(Optional.of(result.value()), result.input());
        });
    }

    static <A> Taker<A> orElse(Taker<A> parser, A other) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return new Match<>(other, in);
            }
            return result;
        });
    }

    static <A, R> Taker<R> map(Taker<A> parser, Function<A, R> mapper) {
        return new Taker<>(in -> parser.apply(in).map(mapper));
    }

    static <A> Taker<Located<A>> located(Taker<A> parser) {
        return new Taker<>(in -> {
            int start = in.position();
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            return new Match<>(new Located<>(result.value(), start, result.input().position()), result.input());
        });
    }

    static <A> Taker<A> expecting(Taker<A> parser, String label) {
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result;
            }
            return new NoMatch<>(result.input(), label, (Failure<?>) result);
        });
    }

    static <A> Taker<A> label(Taker<A> parser, String label) {
        return expecting(parser, label);
    }

    static <A, B> Taker<B> flatMap(Taker<A> parser, Function<A, Taker<B>> f) {
        if (f == null) {
            throw new IllegalArgumentException("flatMap function cannot be null");
        }
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) {
                return result.cast();
            }
            Taker<B> next = f.apply(result.value());
            if (next == null) {
                return new NoMatch<B>(result.input(), "parser to function correctly").cast();
            }
            return next.apply(result.input());
        });
    }
}
