package io.github.parseworks.taker;

import java.util.Objects;
import java.util.function.Function;

final class TakerRecovery {

    private TakerRecovery() {
    }

    static <A, B> Taker<B> recover(Taker<A> parser, Taker<B> recovery) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(recovery, "recovery");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply(input);
        });
    }

    static <A, B> Taker<B> recoverWith(Taker<A> parser, Function<Failure<A>, Result<B>> recovery) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(recovery, "recovery");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply((Failure<A>) result);
        });
    }
}
