package io.github.parseworks.taker.results;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.ResultType;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 * <p>
 * This class provides detailed error information including:
 * <ul>
 *   <li>The input position where the error occurred</li>
 *   <li>What was expected vs. what was found</li>
 *   <li>The type of the error</li>
 *   <li>A custom error message (if provided)</li>
 *   <li>The cause of the error (for nested errors)</li>
 * </ul>
 *
 * @param <A> the type of the parsed value
 */
public record NoMatch<A>(
        Input input,
        String expected,
        Failure<?> cause,
        List<Failure<A>> combinedFailures
) implements Failure<A> {

    /**
     * Constructs a new NoMatch with no custom message.
     */
    public NoMatch(Input input, String expected) {
        this(input, expected, null, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(Input input, String expected, Failure<?> cause) {
        this(input, expected, cause, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(List<Failure<A>> failures) {
        this(failures.isEmpty() ? null : failures.get(0).input(), null, null, failures);
    }



    // No explicit canonical constructor override is needed; the record-generated
    // canonical constructor is used.

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
