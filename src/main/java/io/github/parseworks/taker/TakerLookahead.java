package io.github.parseworks.taker;

import io.github.parseworks.taker.results.NoMatch;

import java.util.Objects;

final class TakerLookahead {

    private TakerLookahead() {
    }

    static <A, B> Taker<A> onlyIf(Taker<A> parser, Taker<B> validation) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(validation, "validation");
        return new Taker<>(input -> {
            Result<B> validationResult = validation.apply(input);
            if (!validationResult.matches()) {
                return validationResult.cast();
            }
            return parser.apply(input);
        });
    }

    static <A> Taker<A> onlyIf(Taker<A> parser, CharPredicate validation) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(validation, "validation");
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new NoMatch<>(input, "Expected Character at " + input.position());
            }
            if (!validation.test(input.current())) {
                return new NoMatch<>(input, "Predicate failed");
            }
            return parser.apply(input);
        });
    }

    static <A, B> Taker<A> peek(Taker<A> parser, Taker<B> lookahead) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(lookahead, "lookahead");
        return new Taker<>(input -> {
            Result<A> result = parser.apply(input);
            if (!result.matches()) {
                return result;
            }
            Result<B> peek = lookahead.apply(result.input());
            if (!peek.matches()) {
                return new NoMatch<>(input, "Expected 'peek' to succeed", (NoMatch<?>) peek);
            }
            return result;
        });
    }
}
