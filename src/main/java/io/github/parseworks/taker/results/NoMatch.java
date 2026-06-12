package io.github.parseworks.taker.results;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.ResultType;

import java.util.List;
import java.util.function.Function;

/**
 * Recoverable parser failure.
 * <p>
 * Choice combinators may try later alternatives after a {@code NoMatch}. Use
 * {@link PartialMatch} when a branch should be treated as committed.
 *
 * @param input input cursor where the failure should be reported
 * @param expected human-readable expectation
 * @param cause optional underlying cause
 * @param combinedFailures tied failures from alternative parsers
 * @param <A> the type of the parsed value
 */
public record NoMatch<A>(
        Input input,
        String expected,
        Failure<?> cause,
        List<Failure<A>> combinedFailures
) implements Failure<A> {

    /**
     * Constructs a recoverable failure at {@code input}.
     */
    public NoMatch(Input input, String expected) {
        this(input, expected, null, null);
    }

    /**
     * Constructs a recoverable failure with an underlying cause.
     */
    public NoMatch(Input input, String expected, Failure<?> cause) {
        this(input, expected, cause, null);
    }

    /**
     * Constructs a recoverable failure from tied alternative failures.
     */
    public NoMatch(List<Failure<A>> failures) {
        this(failures.isEmpty() ? null : failures.getFirst().input(), null, null, failures);
    }

    @Override
    public ResultType type() {
        return ResultType.NO_MATCH;
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
        return failure.apply(this);
    }

}
