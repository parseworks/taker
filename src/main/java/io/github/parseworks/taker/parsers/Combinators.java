package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;

import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.ResultType;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.*;
import io.github.parseworks.taker.impl.inputs.LowercaseInput;
import io.github.parseworks.taker.impl.inputs.UppercaseInput;
import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Core parser combinators for choice, sequence, and filtering.
 */
public class Combinators {

    /**
     * Succeeds if the input is at the end of the file (EOF).
     */
    public static final Taker<Void> eof = new Taker<>(input -> {
        if (input.isEof()) {
            return new Match<>(null, input);
        } else {
            return new NoMatch<>(input, "end of input");
        }
    });

    /**
     * Succeeds on any input other than EOF
     */
    public static Taker<Character> any = new Taker<>(in -> {
        if (in.isEof()) {
            return new NoMatch<>(in, Character.class.descriptorString()).cast();
        } else {
            return new Match<>(in.current(), in.next());
        }
    });

    private Combinators() {
    }

    /**
     * Unconditionally throws an exception from the supplier.
     * <pre>{@code
     * Taker< Object> critical = throwError(() -> new IllegalStateException("Fail"));
     * }</pre>
     *
     * @param supplier exception supplier
     * @return a parser that always throws
     * @see #fail()
     */
    public static Taker<? super Object> throwError(Supplier<? extends Exception> supplier) {
        return new Taker<>(in -> {
            throw sneakyThrow(supplier.get());
        });
    }

    /**
     * Utility method to bypass checked exception requirements.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    /**
     * Succeeds if the input is at the end of the file (EOF).
     *
     * @deprecated use {@link #eof} instead
     */
    @Deprecated
    public static <I> Taker<Void> eof() {
        return eof;
    }

    /**
     * Unconditionally fails, consuming no input.
     */
    public static <A> Taker<A> fail() {
        return new Taker<>(in -> new NoMatch<>(in, "parser explicitly set to fail"));
    }

    /**
     * Fails with a specific error message.
     */
    public static <A> Taker<A> fail(String expected) {
        return new Taker<>(in -> new NoMatch<>(in, expected));
    }

    /**
     * Succeeds if the provided parser fails, returning the current input element.
     * <pre>{@code
     * not(Lexical.chr(Character::isDigit)).parse("a").value(); // 'a'
     * }</pre>
     *
     * @param parser parser to negate
     * @param <A>    result type
     * @return a negation parser
     * @see #isNot(char)
     */
    public static <A> Taker not(Taker<A> parser) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (result.matches() || !result.input().hasMore()) {
                // Provide more context about what was found that shouldn't have matched
                String found = result.input().hasMore() ? "expected parser to fail" : "end of input";
                return new NoMatch<>(in, found);
            }
            return new Match<>(in.current(), in.next());

        });
    }

    /**
     * Succeeds if the current input element is not equal to the provided value.
     * <pre>{@code
     * isNot(',').parse("a").value(); // 'a'
     * }</pre>
     *
     * @param value value to exclude
     * @return a parser matching anything except the value
     * @see #not(Taker)
     * @see #is(char)
     */
    public static Taker<Character> isNot(char value) {
        return satisfy("any value except " + value, CharPredicate.isNot(value));
    }

    /**
     * Creates a parser that repeatedly applies this parser as long as the condition evaluates to true.
     * <p>
     * This parser will:
     * <ul>
     *   <li>Check if the condition is true for the current input position</li>
     *   <li>If true, apply this parser and collect the result</li>
     *   <li>Continue until either the condition becomes false, parsing fails, or input is exhausted</li>
     *   <li>Return all collected results as an FList</li>
     * </ul>
     * <p>
     * Unlike {@link #oneOf(List)}, this parser uses a separate condition parser to determine
     * when to stop collecting items rather than relying on parse failures. This allows for more
     * flexible parsing based on lookahead or contextual conditions.
     * <p>
     * The implementation includes a check to prevent infinite loops in cases where the parser
     * succeeds but doesn't advance the input position.
     *
     * @param condition a parser that returns a boolean indicating whether to continue collecting
     * @return a parser that collects elements while the condition is true
     * @throws IllegalArgumentException if the condition parser is null
     */
    public static Taker<String> takeWhile(CharPredicate condition) {
        return Lexical.takeWhile(condition);
    }

    /**
     * Collects characters until the condition is met.
     *
     * @param condition the condition that stops the collection
     * @return characters until the condition is met
     */
    public static Taker<String> takeUntil(CharPredicate condition) {
        return Lexical.takeUntil(condition);
    }

    /**
     * Tries multiple parsers in sequence until one succeeds.
     *
     * @param parsers list of parsers to try
     * @param <A>     result type
     * @return a choice parser
     * @see Taker#or(Taker)
     */
    public static <A> Taker<A> oneOf(List<Taker<A>> parsers) {
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one parser defined");
        }
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, "eof before `oneOf` parser");
            }

            int pos = in.position();
            List<Failure<A>> failures = null;

            for (Taker<A> parser : parsers) {
                Result<A> result = parser.apply(in);
                if (result.matches()) {
                    return result;
                }

                // If it's a hard failure (consumed input), stop and return it
                if (result.input().position() > pos) {
                    return result;
                }

                if (failures == null) {
                    failures = new ArrayList<>();
                }
                failures.add((Failure<A>) result);
            }
            assert failures != null;
            return new NoMatch<>(failures);
        });
    }

    /**
     * Predictive version of {@link #oneOf(List)} that attempts to find a match
     * based on common prefixes.
     *
     * @param parsers list of parsers to try
     * @param <A>     result type
     * @return a choice parser
     */
    public static <A> Taker<A> predict(List<Taker<A>> parsers) {
        return oneOf(parsers);
    }

    /**
     * Predictive version of {@link #oneOf(Taker[])} that attempts to find a match
     * based on common prefixes.
     *
     * @param parsers parsers to try
     * @param <A>     result type
     * @return a choice parser
     */
    @SafeVarargs
    public static <A> Taker<A> predict(Taker<A>... parsers) {
        return predict(Arrays.asList(parsers));
    }

    /**
     * Tries each of the provided parsers in order and succeeds with the first match.
     */
    @SafeVarargs
    public static <A> Taker<A> oneOf(Taker<A>... parsers) {
        return oneOf(Arrays.asList(parsers));
    }

    /**
     * Applies multiple parsers in sequence and collects their results in a list.
     * <pre>{@code
     * sequence(Arrays.asList(p1, p2, p3)).parse("123").value(); // [1, 2, 3]
     * }</pre>
     *
     * @param parsers parsers to apply
     * @param <A>     result type
     * @return a sequence parser
     */
    public static <A> Taker<List<A>> sequence(List<Taker<A>> parsers) {
        return new Taker<>(in -> {
            List<A> results = new ArrayList<>();
            Input currentInput = in;
            for (Taker<A> parser : parsers) {
                Result<A> result = parser.apply(currentInput);
                if (!result.matches()) {
                    return result.cast();
                }
                results.add(result.value());
                currentInput = result.input();
            }
            return new Match<>(results, currentInput);
        });
    }

    /**
     * Applies two parsers in sequence and returns an ApplyBuilder.
     */
    public static <A> ApplyBuilder<A, A> sequence(Taker<A> parserA, Taker<A> parserB) {
        return parserA.then(parserB);
    }

    /**
     * Applies three parsers in sequence and returns an ApplyBuilder3.
     */
    public static <A> ApplyBuilder<A, A>.ApplyBuilder3<A> sequence(Taker<A> parserA, Taker<A> parserB, Taker<A> parserC) {
        return parserA.then(parserB).then(parserC);
    }

    /**
     * Parses a single item that satisfies the given predicate.
     *
     * @param expectedType error message if not satisfied
     * @param predicate    condition to satisfy
     * @return a satisfy parser
     */
    public static Taker<Character> satisfy(String expectedType, CharPredicate predicate) {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expectedType);
            }
            char item = in.current();
            if (predicate.test(item)) {
                return new Match<>(item, in.next());
            } else {
                return new NoMatch<>(in, expectedType);
            }
        });
    }

    /**
     * Matches the current input item against the provided value.
     */
    public static Taker<Character> is(char equivalence) {
        return satisfy(String.valueOf(equivalence), CharPredicate.is(equivalence));
    }


    /**
     *
     *
     * @return
     */
    public static Taker<Character> any() {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, Character.class.descriptorString()).cast();
            } else {
                return new Match<>(in.current(), in.next());
            }
        });
    }
}
