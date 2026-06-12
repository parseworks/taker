package io.github.parseworks.taker;

import java.util.BitSet;
import java.util.Objects;

/**
 * A functional interface for character predicates.
 */
@FunctionalInterface
public interface CharPredicate {

    /**
     * Tests whether the given character satisfies this predicate.
     *
     * @param c the character to test
     * @return {@code true} if the character satisfies the predicate, {@code false} otherwise
     */
    boolean test(char c);

    /**
     * Describes the characters this predicate accepts for parser error messages.
     *
     * @return a human-readable expectation
     */
    default String expected() {
        return "character matching predicate";
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND
     * of this predicate and another.
     *
     * @param other the other predicate to combine with this one
     * @return a composed predicate that is {@code true} only if both predicates are {@code true}
     */
    default CharPredicate and(CharPredicate other) {
        Objects.requireNonNull(other, "other");
        return named(expected() + " and " + other.expected(), c -> test(c) && other.test(c));
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR
     * of this predicate and another.
     *
     * @param other the other predicate to combine with this one
     * @return a composed predicate that is {@code true} if either predicate is {@code true}
     */
    default CharPredicate or(CharPredicate other) {
        Objects.requireNonNull(other, "other");
        return named(expected() + " or " + other.expected(), c -> test(c) || other.test(c));
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a negated predicate
     */
    default CharPredicate negate() {
        return named("not " + expected(), c -> !test(c));
    }

    /**
     * Returns a predicate with a custom expectation label.
     *
     * @param expected human-readable expectation
     * @param predicate predicate implementation
     * @return a named predicate
     */
    static CharPredicate named(String expected, CharPredicate predicate) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(predicate, "predicate");
        return new NamedCharPredicate(expected, predicate);
    }

    /**
     * Returns a predicate that represents the logical negation of another predicate.
     *
     * @param predicate predicate to negate
     * @return a negated predicate
     */
    static CharPredicate not(CharPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.negate();
    }

    /**
     * Returns a predicate that tests if a character is equal to the target character.
     *
     * @param target the character to compare against
     * @return a predicate that is {@code true} if the character matches the target
     */
    static CharPredicate is(char target) {
        return named("'" + display(target) + "'", c -> c == target);
    }

    /**
     * Returns a predicate that tests if a character equals the target character,
     * ignoring case with {@link Character#toLowerCase(char)}.
     *
     * @param target the character to compare against
     * @return a predicate that is {@code true} when the character matches the target ignoring case
     */
    static CharPredicate isIgnoreCase(char target) {
        char lower = Character.toLowerCase(target);
        char upper = Character.toUpperCase(target);
        return named("'" + display(target) + "' ignoring case",
                c -> c == target || Character.toLowerCase(c) == lower || Character.toUpperCase(c) == upper);
    }

    /**
     * Returns a predicate that tests if a character is not equal to the target character.
     *
     * @param target the character to compare against
     * @return a predicate that is {@code true} if the character does not match the target
     */
    static CharPredicate isNot(char target) {
        return named("not '" + display(target) + "'", c -> c != target);
    }

    /**
     * Returns a predicate that tests if a character is within the specified range (inclusive).
     *
     * @param start the start of the range (inclusive)
     * @param end   the end of the range (inclusive)
     * @return a predicate that is {@code true} if the character is within the range
     */
    static CharPredicate range(char start, char end) {
        return named("'" + display(start) + "'..'" + display(end) + "'", c -> c >= start && c <= end);
    }

    /**
     * Alias for {@link #range(char, char)}.
     */
    static CharPredicate between(char start, char end) {
        return range(start, end);
    }

    /**
     * Returns a predicate that tests if a character is outside the specified range.
     *
     * @param start the start of the range (inclusive)
     * @param end   the end of the range (inclusive)
     * @return a predicate that is {@code true} if the character is outside the range
     */
    static CharPredicate outside(char start, char end) {
        return named("outside '" + display(start) + "'..'" + display(end) + "'", c -> c < start || c > end);
    }

    /**
     * Returns a predicate that tests if a character is present in the given string.
     *
     * @param chars a string containing the characters to match
     * @return a predicate that is {@code true} if the character is in the string
     */
    static CharPredicate anyOf(String chars) {
        Objects.requireNonNull(chars, "chars");
        return named("one of \"" + display(chars) + "\"", charSet(chars));
    }

    /**
     * Returns a predicate that tests if a character is present in the given
     * string, ignoring case.
     *
     * @param chars a string containing the characters to match
     * @return a predicate that is {@code true} if the character matches any supplied character ignoring case
     */
    static CharPredicate anyOfIgnoreCase(String chars) {
        Objects.requireNonNull(chars, "chars");
        return named("one of \"" + display(chars) + "\" ignoring case", ignoreCaseCharSet(chars));
    }

    /**
     * Returns a predicate that tests if any of the predicates accepts a character.
     *
     * @param predicates predicates to combine
     * @return a predicate that is {@code true} if any predicate accepts the character
     */
    static CharPredicate anyOf(CharPredicate... predicates) {
        Objects.requireNonNull(predicates, "predicates");
        CharPredicate[] copy = predicates.clone();
        for (CharPredicate predicate : copy) {
            Objects.requireNonNull(predicate, "predicate");
        }
        return named("any of predicates", c -> {
            for (CharPredicate predicate : copy) {
                if (predicate.test(c)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Returns a predicate that tests if a character is not present in the given string.
     *
     * @param chars a string containing the characters to avoid
     * @return a predicate that is {@code true} if the character is not in the string
     */
    static CharPredicate noneOf(String chars) {
        Objects.requireNonNull(chars, "chars");
        CharPredicate included = charSet(chars);
        return named("none of \"" + display(chars) + "\"", c -> !included.test(c));
    }

    /**
     * Returns a predicate that tests if a character is not present in the given
     * string, ignoring case.
     *
     * @param chars a string containing the characters to avoid
     * @return a predicate that is {@code true} if the character does not match any supplied character ignoring case
     */
    static CharPredicate noneOfIgnoreCase(String chars) {
        Objects.requireNonNull(chars, "chars");
        CharPredicate included = ignoreCaseCharSet(chars);
        return named("none of \"" + display(chars) + "\" ignoring case", c -> !included.test(c));
    }

    /**
     * Returns a predicate that tests if all predicates accept a character.
     *
     * @param predicates predicates to combine
     * @return a predicate that is {@code true} only if every predicate accepts the character
     */
    static CharPredicate allOf(CharPredicate... predicates) {
        Objects.requireNonNull(predicates, "predicates");
        CharPredicate[] copy = predicates.clone();
        for (CharPredicate predicate : copy) {
            Objects.requireNonNull(predicate, "predicate");
        }
        return named("all predicates", c -> {
            for (CharPredicate predicate : copy) {
                if (!predicate.test(c)) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * A predicate that tests if a character is a digit.
     */
    CharPredicate digit = named("digit", Character::isDigit);

    /**
     * A predicate that tests if a character is a letter.
     */
    CharPredicate letter = named("letter", Character::isLetter);

    /**
     * A predicate that tests if a character is a letter or digit.
     */
    CharPredicate letterOrDigit = named("letter or digit", Character::isLetterOrDigit);

    /**
     * A predicate that tests if a character is whitespace.
     */
    CharPredicate whitespace = named("Java whitespace", Character::isWhitespace);

    /**
     * A predicate that tests if a character is upper case.
     */
    CharPredicate upperCase = named("uppercase character", Character::isUpperCase);

    /**
     * A predicate that tests if a character is lower case.
     */
    CharPredicate lowerCase = named("lowercase character", Character::isLowerCase);

    /**
     * A predicate that tests if a character is an ASCII digit (0-9).
     */
    CharPredicate asciiDigit = named("ASCII digit", c -> c >= '0' && c <= '9');

    /**
     * A predicate that tests if a character is an ASCII letter (a-z, A-Z).
     */
    CharPredicate asciiLetter = named("ASCII letter", c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));

    /**
     * A predicate that tests if a character is an ASCII lowercase letter (a-z).
     */
    CharPredicate asciiLowerCase = named("ASCII lowercase letter", c -> c >= 'a' && c <= 'z');

    /**
     * A predicate that tests if a character is an ASCII uppercase letter (A-Z).
     */
    CharPredicate asciiUpperCase = named("ASCII uppercase letter", c -> c >= 'A' && c <= 'Z');

    /**
     * A predicate that tests if a character is an ASCII letter or digit.
     */
    CharPredicate asciiLetterOrDigit = named("ASCII letter or digit", asciiLetter.or(asciiDigit));

    /**
     * A predicate that tests if a character is common ASCII whitespace.
     * <p>
     * This includes space, tab, line feed, carriage return, and form feed.
     */
    CharPredicate asciiWhitespace = named("ASCII whitespace", anyOf(" \t\n\r\f"));

    /**
     * A predicate that tests if a character is horizontal whitespace.
     * <p>
     * This includes space and tab, but intentionally excludes line breaks.
     */
    CharPredicate horizontalWhitespace = named("horizontal whitespace", anyOf(" \t"));

    /**
     * A predicate that tests if a character is a line break.
     * <p>
     * This matches line feed and carriage return. Use a parser such as
     * {@code oneOf(string("\r\n"), string("\n"), string("\r"))} when CRLF
     * should be consumed as a single line ending token.
     */
    CharPredicate lineBreak = named("line break", anyOf("\n\r"));

    record NamedCharPredicate(String expected, CharPredicate predicate) implements CharPredicate {
        public NamedCharPredicate {
            Objects.requireNonNull(expected, "expected");
            Objects.requireNonNull(predicate, "predicate");
        }

        @Override
        public boolean test(char c) {
            return predicate.test(c);
        }
    }

    private static CharPredicate charSet(String chars) {
        int length = chars.length();
        if (length == 0) {
            return c -> false;
        }
        if (length == 1) {
            char c0 = chars.charAt(0);
            return c -> c == c0;
        }
        if (length == 2) {
            char c0 = chars.charAt(0);
            char c1 = chars.charAt(1);
            return c -> c == c0 || c == c1;
        }
        if (length < 10) {
            return c -> chars.indexOf(c) >= 0;
        }

        BitSet set = new BitSet(Character.MAX_VALUE + 1);
        for (int i = 0; i < length; i++) {
            set.set(chars.charAt(i));
        }
        return set::get;
    }

    private static CharPredicate ignoreCaseCharSet(String chars) {
        int length = chars.length();
        if (length == 0) {
            return c -> false;
        }
        if (length == 1) {
            return isIgnoreCase(chars.charAt(0));
        }
        if (length < 10) {
            return c -> {
                char lower = Character.toLowerCase(c);
                char upper = Character.toUpperCase(c);
                for (int i = 0; i < length; i++) {
                    char item = chars.charAt(i);
                    if (c == item || lower == Character.toLowerCase(item) || upper == Character.toUpperCase(item)) {
                        return true;
                    }
                }
                return false;
            };
        }

        BitSet set = new BitSet(Character.MAX_VALUE + 1);
        for (int i = 0; i < length; i++) {
            char item = chars.charAt(i);
            set.set(item);
            set.set(Character.toLowerCase(item));
            set.set(Character.toUpperCase(item));
        }
        return c -> set.get(c) || set.get(Character.toLowerCase(c)) || set.get(Character.toUpperCase(c));
    }

    private static String display(String chars) {
        StringBuilder builder = new StringBuilder(chars.length());
        for (int i = 0; i < chars.length(); i++) {
            builder.append(display(chars.charAt(i)));
        }
        return builder.toString();
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
