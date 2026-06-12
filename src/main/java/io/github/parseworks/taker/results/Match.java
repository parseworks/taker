package io.github.parseworks.taker.results;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.ResultType;

import java.util.function.Function;

/**
 * Successful parser result.
 *
 * @param value parsed value
 * @param input input cursor after the successful parse
 *
 * @param <A> the type of the parsed value
 */
public record Match<A>(
        A value,
        Input input
) implements Result<A> {

    @Override
    public ResultType type() {
        return ResultType.MATCH;
    }

    @Override
    public boolean matches() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<B> cast() {
        return (Result<B>) this;
    }

    @Override
    public <B> Result<B> map(Function<A, B> mapper) {
        return new Match<>(mapper.apply(value), input);
    }

    @Override
    public String error() {
        return "";
    }

    @Override
    public <B> B handle(Function<Result<A>, B> success, Function<Result<A>, B> failure) {
        return success.apply(this);
    }

    public String toString() {
        return "Match(" + value + ")";
    }
}
