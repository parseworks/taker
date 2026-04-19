package io.github.parseworks.taker.impl.result;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.ResultType;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Input;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 */
public record NoMatch<A>(
        Input input,
        String expected,
        Failure<?> cause,
        List<Failure<A>> combinedFailures
) implements Failure<A> {

    public NoMatch(Input input, String expected) {
        this(input, expected, null, null);
    }

    public NoMatch(Input input, String expected, Failure<?> cause) {
        this(input, expected, cause, null);
    }

    public NoMatch(List<Failure<A>> failures) {
        this(failures.isEmpty() ? null : failures.get(0).input(), null, null, failures);
    }

    @Override
    public ResultType type() { return ResultType.NO_MATCH; }

    @Override
    public boolean matches() { return false; }

    @Override
    public A value() { throw new RuntimeException(error()); }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> cast() { return (Result<B>) this; }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> map(Function<A, B> mapper) { return (Result<B>) this; }

    @Override
    public <B> B handle(Function<Result<A>, B> success, Function<Result<A>, B> failure) {
        return failure.apply(this);
    }
}
