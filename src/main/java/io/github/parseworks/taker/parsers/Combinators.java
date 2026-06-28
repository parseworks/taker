package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.*;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.results.PartialMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Core parser combinators for choice, sequence, and filtering.
 */
public class Combinators {

    private Combinators() {
    }

    /** Always succeeds without consuming input. */
    public static <A> Taker<A> pure(A value) {
        return new Taker<>(input -> new Match<>(value, input));
    }

    /**
     * Commits the parser. If the parser fails and has consumed input, it returns
     * a PartialMatch.
     */
    public static <A> Taker<A> commit(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (!result.matches() && result.input().position() > in.position()) {
                return new PartialMatch<>(result.input(), (Failure<A>) result);
            }
            return result;
        });
    }

    /** Matches any single character. */
    public static Taker<Character> any() {
        return new Taker<>(input -> {
            if (input.isEof()) {
                return new NoMatch<Character>(input, "any character").cast();
            } else {
                return new Match<>(input.current(), input.next());
            }
        });
    }

    /** Unconditionally throws an exception. */
    public static Taker<? super Object> throwError(Supplier<? extends Exception> supplier) {
        Objects.requireNonNull(supplier, "supplier");
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


    /** Matches any of the given characters. */
    public static Taker<Character> oneOf(char... items) {
        Objects.requireNonNull(items, "items");
        if (items.length == 0) {
            throw new IllegalArgumentException("There must be at least one character defined");
        }
        return Chars.oneOf(new String(items));
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
        return new Taker<>(in -> new NoMatch<>(in, "parser explicitly set to fail"));
    }

    /**
     * Fails with a specific error message.
     */
    public static <A> Taker<A> fail(String expected) {
        Objects.requireNonNull(expected, "expected");
        return new Taker<>(in -> new NoMatch<>(in, expected));
    }

    /**
     * Succeeds without consuming input if the provided parser fails.
     * <p>
     * Use {@code not(parser).skipThen(any())} when the grammar should consume
     * the character that was validated by negative lookahead.
     */
    public static <A> Taker<Void> not(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Result<A> result = parser.apply(in);
            if (result.matches()) {
                return new NoMatch<>(in, "parser not to match");
            }
            return new Match<>(null, in);
        });
    }

    /** Matches anything except the given character. */
    public static Taker<Character> isNot(char value) {
        String expected = "any character except " + expectedChar(value);
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expected);
            }
            char item = in.current();
            if (item == value) {
                return new NoMatch<>(in, expected);
            } else {
                return new Match<>(item, in.next());
            }
        });
    }


    /** Matches the first succeeding parser in the list. */
    public static <A> Taker<A> oneOf(List<Taker<A>> parsers) {
        Objects.requireNonNull(parsers, "parsers");
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one parser defined");
        }
        for (Taker<A> parser : parsers) {
            Objects.requireNonNull(parser, "parser");
        }
        return new Taker<>(in -> {
            List<Failure<A>> failures = null;
            int farthestFailurePosition = -1;

            for (Taker<A> parser : parsers) {
                Result<A> result = parser.apply(in);
                if (result.matches()) {
                    return result;
                }
                
                // If it's a hard failure (consumed input), stop and return it
                if (result.type() == ResultType.PARTIAL) {
                    return result;
                }

                int failurePosition = result.input() == null ? -1 : result.input().position();
                if (failurePosition < farthestFailurePosition) {
                    continue;
                }
                if (failurePosition > farthestFailurePosition) {
                    farthestFailurePosition = failurePosition;
                    failures = null;
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

    /** Matches the first succeeding parser. */
    @SafeVarargs
    public static <A> Taker<A> oneOf(Taker<A>... parsers) {
        Objects.requireNonNull(parsers, "parsers");
        return oneOf(Arrays.asList(parsers));
    }

    /** Applies multiple parsers in sequence. */
    public static <A> Taker<List<A>> sequence(List<Taker<A>> parsers) {
        Objects.requireNonNull(parsers, "parsers");
        for (Taker<A> parser : parsers) {
            Objects.requireNonNull(parser, "parser");
        }
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
        Objects.requireNonNull(parserA, "parserA");
        Objects.requireNonNull(parserB, "parserB");
        return parserA.then(parserB);
    }

    /**
     * Applies three parsers in sequence and returns an ApplyBuilder3.
     */
    public static <A> ApplyBuilder<A, A>.ApplyBuilder3<A> sequence(Taker<A> parserA, Taker<A> parserB, Taker<A> parserC) {
        Objects.requireNonNull(parserA, "parserA");
        Objects.requireNonNull(parserB, "parserB");
        Objects.requireNonNull(parserC, "parserC");
        return parserA.then(parserB).then(parserC);
    }

    /** Matches a character between open and close parsers. */
    public static <A, B, C> Taker<A> between(Taker<B> open, Taker<A> parser, Taker<C> close) {
        Objects.requireNonNull(parser, "parser");
        return new Taker<>(in -> {
            Input current = in;
            if (open != null) {
                Result<B> resOpen = open.apply(current);
                if (!resOpen.matches()) return resOpen.cast();
                current = resOpen.input();
            }
            Result<A> resParser = parser.apply(current);
            if (!resParser.matches()) return resParser;
            current = resParser.input();
            if (close != null) {
                Result<C> resClose = close.apply(current);
                if (!resClose.matches()) return resClose.cast();
                current = resClose.input();
            }
            return new Match<>(resParser.value(), current);
        });
    }

    /** Matches a character between bracket parsers. */
    public static <A, B> Taker<A> between(Taker<B> bracket, Taker<A> parser) {
        return between(bracket, parser, bracket);
    }

    /** Matches a character between open and close characters. */
    public static <A> Taker<A> between(char open, Taker<A> parser, char close) {
        return between(Chars.chr(open), parser, Chars.chr(close));
    }

    /** Matches a character between bracket characters. */
    public static <A> Taker<A> between(char bracket, Taker<A> parser) {
        return between(bracket, parser, bracket);
    }

    /** Matches a character satisfying the predicate. */
    public static Taker<Character> satisfy(String expectedType, CharPredicate predicate) {
        Objects.requireNonNull(expectedType, "expectedType");
        Objects.requireNonNull(predicate, "predicate");
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expectedType);
            }
            var item = in.current();
            if (predicate.test(item)) {
                return new Match<>(item, in.next());
            } else {
                return new NoMatch<>(in, expectedType);
            }
        });
    }

    /** Matches the given value. */
    public static <A> Taker<A> is(A equivalence) {
        Objects.requireNonNull(equivalence, "equivalence");
        String expected = expectedValue(equivalence);
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expected);
            }
            char item = in.current();
            if (Objects.equals(item, equivalence)) {
                return new Match<>(equivalence, in.next());
            } else {
                return new NoMatch<>(in, expected);
            }
        });
    }

    /**
     * Chains a parser left-associatively.
     */
    public static <A> Taker<A> chainLeft(Taker<A> parser, Taker<java.util.function.BinaryOperator<A>> op, A identity) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(op, "op");
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
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(op, "op");
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
     * Chains a parser right-associatively, returning {@code identity} when
     * this parser does not match.
     *
     * <pre>{@code
     * BinaryOperator<Long> power = (a, b) -> (long) Math.pow(a, b);
     * var p = number.chainRightZeroOrMore(chr('^').as(power), 1L);
     * // "2^3^2" = 2^(3^2) = 512
     * }</pre>
     *
     * @param elem element parser
     * @param op operator parser producing a {@link java.util.function.BinaryOperator}
     * @param identity value returned when {@code elem} matches zero times
     * @return a chain parser (right-associative)
     */
    public static <A> Taker<A> chainRight(Taker<A> elem, Taker<java.util.function.BinaryOperator<A>> op, A identity) {
        Objects.requireNonNull(elem, "elem");
        Objects.requireNonNull(op, "op");
        return new Taker<>(in -> {
            Result<A> first = elem.apply(in);
            if (!first.matches()) return new Match<>(identity, in);

            // Collect operator+value pairs left-to-right without recursion
            List<java.util.AbstractMap.SimpleEntry<java.util.function.BinaryOperator<A>, A>> opsAndValues = null;
            Input current = first.input();
            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> next = elem.apply(opResult.input());
                if (!next.matches()) break;

                if (opsAndValues == null) {
                    opsAndValues = new ArrayList<>();
                }
                opsAndValues.add(new java.util.AbstractMap.SimpleEntry<>(opResult.value(), next.value()));
                current = next.input();
            }

            // Fold right-to-left to preserve right-associativity.
            if (opsAndValues == null) {
                return new Match<>(first.value(), current);
            }

            A inner = opsAndValues.get(opsAndValues.size() - 1).getValue();
            for (int i = opsAndValues.size() - 2; i >= 0; i--) {
                var pair = opsAndValues.get(i);
                inner = pair.getKey().apply(pair.getValue(), inner);
            }
            A acc = opsAndValues.get(opsAndValues.size() - 1).getKey().apply(first.value(), inner);
            return new Match<>(acc, current);
        });
    }

    /**
     * Chains a parser right-associatively, requiring at least one match.
     *
     * <pre>{@code
     * BinaryOperator<Long> power = (a, b) -> (long) Math.pow(a, b);
     * var p = number.chainRightOneOrMore(chr('^').as(power));
     * // "2^3^2" = 2^(3^2) = 512
     * }</pre>

     * }
     *
     * @param elem element parser
     * @param op operator parser producing a {@link java.util.function.BinaryOperator}
     * @return a chain parser (right-associative)
     */
    public static <A> Taker<A> chainRight(Taker<A> elem, Taker<java.util.function.BinaryOperator<A>> op) {
        Objects.requireNonNull(elem, "elem");
        Objects.requireNonNull(op, "op");
        return new Taker<>(in -> {
            Result<A> first = elem.apply(in);
            if (!first.matches()) return first;

            // Collect operator+value pairs left-to-right without recursion
            List<java.util.AbstractMap.SimpleEntry<java.util.function.BinaryOperator<A>, A>> opsAndValues = null;
            Input current = first.input();
            while (true) {
                Result<java.util.function.BinaryOperator<A>> opResult = op.apply(current);
                if (!opResult.matches()) break;

                Result<A> next = elem.apply(opResult.input());
                if (!next.matches()) break;

                if (opsAndValues == null) {
                    opsAndValues = new ArrayList<>();
                }
                opsAndValues.add(new java.util.AbstractMap.SimpleEntry<>(opResult.value(), next.value()));
                current = next.input();
            }

            // Fold right-to-left to preserve right-associativity.
            A acc = first.value();
            if (opsAndValues != null && opsAndValues.size() == 1) {
                var pair = opsAndValues.get(0);
                acc = pair.getKey().apply(acc, pair.getValue());
            } else if (opsAndValues != null) {
                A inner = opsAndValues.get(opsAndValues.size() - 1).getValue();
                for (int i = opsAndValues.size() - 2; i >= 0; i--) {
                    var pair = opsAndValues.get(i);
                    inner = pair.getKey().apply(pair.getValue(), inner);
                }
                acc = opsAndValues.get(opsAndValues.size() - 1).getKey().apply(first.value(), inner);
            }
            return new Match<>(acc, current);
        });
    }

    private static String expectedValue(Object equivalence) {
        if (equivalence instanceof Character c) {
            return expectedChar(c);
        }
        return String.valueOf(equivalence);
    }

    private static String expectedChar(char c) {
        return "'" + display(c) + "'";
    }

    private static String display(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\f' -> "\\f";
            case '\b' -> "\\b";
            default -> Character.isISOControl(c) ? "\\u%04x".formatted((int) c) : String.valueOf(c);
        };
    }

}
