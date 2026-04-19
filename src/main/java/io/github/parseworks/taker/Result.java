package io.github.parseworks.taker;

import io.github.parseworks.taker.Input;

import java.util.Optional;
import java.util.function.Function;

/**
 * The outcome of applying a parser to an input.
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

    /** Returns true if the parser matched. */
    boolean matches();

    /** Returns the parsed value. Throws if this is a NoMatch. */
     A value();

    /** Returns the input position after parsing. */
     Input input();

    /**
     * Casts this result to a result of a different type.
     *
     * @param <B> the new type of the parsed value
     * @return this result cast to the new type
     */
     <B> Result<B> cast();

    /** Transforms the result value using the given function. */
     <B> Result<B> map(Function<A, B> mapper);

    /** Returns the error message if this is a NoMatch. */
     String error();

    /**
     * Returns an Optional containing the parsed value if this result is a Match.
     * If this result is a NoMatch, returns an empty Optional.
     *
     * @return an Optional containing the parsed value, or an empty Optional if this result is a NoMatch
     */
    default Optional<A> toOptional() {
        return matches() ? Optional.of(value()) : Optional.empty();
    }

    /**
     * Returns an Optional containing the error message if this result is a NoMatch.
     * If this result is a Match, returns an empty Optional.
     *
     * @return an Optional containing the error message, or an empty Optional if this result is a Match
     */
    default Optional<String> errorOptional() {
        return !matches() ? Optional.of(error()) : Optional.empty();
    }

    /**
     * Apply one of two functions to this value.
     * @param success   the function to be applied to a Match result
     * @param failure   the function to be applied to a NoMatch result
     * @param <B>       the function return type
     * @return          the result of applying either function
     */
    <B> B handle(Function<Result<A>, B> success, Function<Result<A>, B> failure);
}
