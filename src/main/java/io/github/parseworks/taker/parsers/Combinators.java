package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.*;
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

    private Combinators() {
    }

    /**
     * Matches any single input element of the specified type.
     * <pre>{@code
     * any(Character.class).parse("abc").value(); // 'a'
     * }</pre>
     *
     * @return a parser that matches any single input element
     */
    public static Taker<Character> any() {
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new NoMatch<Character>(input, "any character").cast();
            } else {
                return new Match<>(input.current(), input.next());
            }
        });
    }

    /**
     * Unconditionally throws an exception from the supplier.
     * <pre>{@code
     * Taker<Object> critical = throwError(() -> new IllegalStateException("Fail"));
     * }</pre>
     *
     * @param supplier exception supplier
     * @param <I>      input type
     * @return a parser that always throws
     * @see #fail()
     */
    public static <I> Taker<? super Object> throwError(Supplier<? extends Exception> supplier) {
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
     * Matches the current input element against a set of possible values.
     * <pre>{@code
     * oneOf('1', '2', '3').parse("1").value(); // '1'
     * }</pre>
     *
     * @param items values to match against
     * @return a parser matching any of the items
     * @see #is(Object)
     */
    public static Taker<Character> oneOf(char... items) {
        if (items == null || items.length == 0) {
            return fail("any character in empty set");
        }
        return Lexical.oneOf(new String(items));
    }

    /**
     * Succeeds if the input is at the end of the file (EOF).
     */
    public static Taker<Void> eof() {
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new Match<>(null, input);
            } else {
                return new NoMatch<>(input, "end of input");
            }
        });
    }

    /**
     * Unconditionally fails, consuming no input.
     */
    public static <A> Taker<A> fail() {
        return new Taker<>(in -> new NoMatch<A>(in, "parser explicitly set to fail"));
    }

    /**
     * Fails with a specific error message.
     */
    public static <A> Taker<A> fail(String expected) {
        return new Taker<>(in -> new NoMatch<A>(in, expected));
    }

    /**
     * Succeeds if the provided parser fails, returning the current input element.
     * <pre>{@code
     * not(Lexical.chr(Character::isDigit)).parse("a").value(); // 'a'
     * }</pre>
     *
     * @param parser parser to negate
     */
    public static <A> Taker<Character> not(Taker<A> parser) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (result.matches() || !result.input().hasMore()) {
                // Provide more context about what was found that shouldn't have matched
                String found = result.input().hasMore() ? "expected parser to fail" : "end of input";
                return new NoMatch<Character>(in, found);
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
     * @see #is(Object)
     */
    public static Taker<Character> isNot(char value) {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<Character>(in, "any value except " + value);
            }
            char item = in.current();
            if (item == value) {
                return new NoMatch<Character>(in, "any value except " + value);
            } else {
                return new Match<>(item, in.next());
            }
        });
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
            List<Failure<A>> failures = null;

            for (Taker<A> parser : parsers) {
                Result<A> result = parser.apply(in);
                if (result.matches()) {
                    return result;
                }
                
                // If it's a hard failure (consumed input), stop and return it
                if (result.type() == ResultType.PARTIAL) {
                    return result;
                }

                if (failures == null){
                    failures = new ArrayList<>();
                }
                failures.add((Failure<A>) result);
            }
            assert failures != null;
            return new NoMatch<>(failures);
        });
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
     * Matches any character between the given opening and closing parsers.
     */
    public static <A, B, C> Taker<A> between(Taker<B> open, Taker<A> parser, Taker<C> close) {
        return open.skipThen(parser).thenSkip(close);
    }

    /**
     * Matches any character between the given bracket parser.
     */
    public static <A, B> Taker<A> between(Taker<B> bracket, Taker<A> parser) {
        return between(bracket, parser, bracket);
    }

    /**
     * Matches any character between the given opening and closing characters.
     */
    public static <A> Taker<A> between(char open, Taker<A> parser, char close) {
        return between(Lexical.chr(open), parser, Lexical.chr(close));
    }

    /**
     * Matches any character between the given bracket character.
     */
    public static <A> Taker<A> between(char bracket, Taker<A> parser) {
        return between(bracket, parser, bracket);
    }

    /**
     * Parses a single character that satisfies the given predicate.
     *
     * @param expectedType error message if not satisfied
     * @param predicate    condition to satisfy
     * @return a satisfy parser
     */
    public static Taker<Character> satisfy(String expectedType, CharPredicate predicate) {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<Character>(in, expectedType);
            }
            var item = in.current();
            if (predicate.test(item)) {
                return new Match<>(item, in.next());
            } else {
                return new NoMatch<Character>(in, expectedType);
            }
        });
    }

    /**
     * Matches the current input item against the provided value.
     */
    public static <A> Taker<A> is(A equivalence) {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<A>(in, String.valueOf(equivalence));
            }
            char item = in.current();
            if (Objects.equals(item, equivalence)) {
                return new Match<A>((A) (Character) item, in.next());
            } else {
                return new NoMatch<A>(in, String.valueOf(equivalence));
            }
        });
    }

    /**
     * Chains a parser left-associatively.
     */
    public static <A> Taker<A> chainLeft(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op, A identity) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) return new Match<>(identity, in);
            A value = result.value();
            Input current = result.input();

            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> nextResult = parser.apply(opResult.input());
                if (!nextResult.matches()) break;

                value = opResult.value().apply(value, nextResult.value());
                current = nextResult.input();
            }
            return new Match<>(value, current);
        });
    }

    /**
     * Chains a parser left-associatively, requiring at least one match.
     */
    public static <A> Taker<A> chainLeft(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) return result;
            A value = result.value();
            Input current = result.input();

            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> nextResult = parser.apply(opResult.input());
                if (!nextResult.matches()) break;

                value = opResult.value().apply(value, nextResult.value());
                current = nextResult.input();
            }
            return new Match<>(value, current);
        });
    }

    /**
     * Chains a parser right-associatively.
     */
    public static <A> Taker<A> chainRight(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op, A identity) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) return new Match<>(identity, in);

            Result<java.util.function.BinaryOperator<A>> opResult = op.apply(result.input());
            if (!opResult.matches()) return result;

            Result<A> restResult = chainRight(parser, op, identity).apply(opResult.input());
            return new Match<>(opResult.value().apply(result.value(), restResult.value()), restResult.input());
        });
    }

    /**
     * Chains a parser right-associatively, requiring at least one match.
     */
    public static <A> Taker<A> chainRight(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op) {
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches()) return result;

            Result<java.util.function.BinaryOperator<A>> opResult = op.apply(result.input());
            if (!opResult.matches()) return result;

            Result<A> restResult = chainRight(parser, op).apply(opResult.input());
            if (!restResult.matches()) return result;

            return new Match<>(opResult.value().apply(result.value(), restResult.value()), restResult.input());
        });
    }

    /**
     * Chains a parser according to specified associativity.
     */
    public static <A> Taker<A> chain(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op, Chains.Associativity associativity) {
        if (associativity == Chains.Associativity.LEFT) {
            return chainLeft(parser, op);
        } else {
            return chainRight(parser, op);
        }
    }



}
