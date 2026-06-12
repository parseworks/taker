package io.github.parseworks.taker.results;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.ResultType;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a partial match result in a parser combinator.
 * This indicates that the parser matched, but not all of the input was consumed
 * or it's a success that should be distinguished from a full match.
 *
 * @param <A> the type of the parsed value
 */
public record PartialMatch<A>(
        Input input,
        Failure<A> cause
) implements Failure<A> {

    @Override
    public Input input() {
        return cause.input();
    }

    @Override
    public Failure<A> cause() {
        return cause;
    }

    @Override
    public String expected() {
        return cause.expected();
    }

    @Override
    public List<Failure<A>> combinedFailures() {
        return cause.combinedFailures();
    }

    @Override
    public ResultType type() {
        return ResultType.PARTIAL;
    }

    @Override
    public boolean matches() {
        return false;
    }


    @Override
    public A value() {
        throw new RuntimeException(error());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> cast() {
        return (Result<B>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> map(Function<A, B> mapper) {
        return (Result<B>) this;
    }

    @Override
    public <B> B handle(Function<Result<A>, B> success, Function<Result<A>, B> failure) {
        // Since matches() is false, we should probably call failure.
        // But wait, the previous implementation called success.apply(this).
        // If it's a failure (matches() == false), standard practice is to call the failure handler.
        return failure.apply(this);
    }

    public String toString() {
        return "PartialMatch(" + cause + ")";
    }
}
