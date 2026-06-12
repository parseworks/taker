package io.github.parseworks.taker;

import java.util.Optional;
import java.util.function.Function;

/**
 * The outcome of applying a parser to an input.
 * <p>
 * A result is either a successful match or a failure. Most code should branch
 * with {@link #matches()}, {@link #handle(Function, Function)}, or the optional
 * convenience methods rather than depending on a concrete implementation class.
 *
 * @param <A> result type
 */
public interface Result<A> {

    /**
     * Returns the type of this result.
     *
     * @return the result type
     */
    ResultType type();

    /** Returns {@code true} when the parser succeeded. */
    boolean matches();

    /** Returns the parsed value. Throws when this result is a failure. */
    A value();

    /** Returns the input cursor reported by this result. */
    Input input();

    /**
     * Casts this result to a result of a different type.
     *
     * @param <B> the new type of the parsed value
     * @return this result cast to the new type
     */
    <B> Result<B> cast();

    /** Transforms a successful value and propagates failures unchanged. */
    <B> Result<B> map(java.util.function.Function<A, B> mapper);

    /** Returns a formatted error message for failures, or an empty string for success. */
    String error();

    /**
     * Returns the parsed value when this result succeeded.
     *
     * @return the parsed value, or an empty optional for failures
     */
    default Optional<A> toOptional() {
        return matches() ? Optional.of(value()) : Optional.empty();
    }

    /**
     * Returns the formatted error message when this result failed.
     *
     * @return the formatted error message, or an empty optional for success
     */
    default Optional<String> errorOptional() {
        return !matches() ? Optional.of(error()) : Optional.empty();
    }

    /**
     * Applies one of two functions based on whether this result succeeded.
     *
     * @param success   function applied to successful results
     * @param failure   function applied to failed results
     * @param <B>       the function return type
     * @return          the result of applying either function
     */
    <B> B handle(Function<Result<A>, B> success, Function<Result<A>, B> failure);
}
