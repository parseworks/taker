package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;

import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.impl.IntObjectMap;
import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;

import java.nio.CharBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.parseworks.taker.parsers.Combinators.satisfy;

/**
 * Common text parsers for characters, strings, and whitespace.
 * <pre>{@code
 * Taker<String> greeting =
 *     Lexical.take("Hello").thenSkip(Lexical.whitespace).then(Lexical.word);
 * }</pre>
 */
public class Lexical {


    /**
     * Matches one of the characters in the given string.
     *
     * @param chars string containing characters to match
     * @return a parser matching any of the characters
     */
    public static Taker<Character> oneOf(String chars) {
        return chr(c -> chars.indexOf(c) >= 0);
    }

    /**
     * Trims whitespace around the given parser.
     * <pre>{@code
     * trim(take("foo")).parse("  foo  ").value(); // "foo"
     * }</pre>
     *
     * @param parser parser to wrap
     * @param <A>    result type
     * @return trimmed parser
     */
    public static <A> Taker<A>  trim(Taker<A> parser) {
        return new Taker<>(in -> {
            Input trimmedInput = skipWhitespace(in);
            Result<A> result = parser.apply(trimmedInput);
            if (result.matches()) {
                trimmedInput = skipWhitespace(result.input());
                return new Match<>(result.value(), trimmedInput);
            }
            return result;
        });
    }

    private static Input skipWhitespace(Input in) {
        while (!in.isEof() && Character.isWhitespace(in.current())) {
            in = in.next();
        }
        return in;
    }


    /**
     *  retrieves a single character that satisfies the given condition.
     * @param condition
     * @return matching char
     */
    public static Taker<Character> take(CharPredicate condition){
        return new Taker<>(input -> {
            var c = input.data().charAt(input.position());
            if (condition.test(c)) {
                return new Match<>(c, input.skip(1));
            }
            return new NoMatch<>(input,"to find char matching predicate");
        });
    }

    /**
     * Creates a parser that repeatedly applies this parser as long as the condition evaluates to true.
     * <p>
     * This parser will:
     * <ul>
     *   <li>Check if the condition is true for the current input position</li>
     *   <li>If true, apply this parser and collect the result</li>
     *   <li>Continue until either the condition becomes false, parsing fails, or input is exhausted</li>
     *   <li>Return all collected results as a String</li>
     * </ul>
     * <p>
     * The implementation includes a check to prevent infinite loops in cases where the parser
     * succeeds but doesn't advance the input position.
     *
     * @param condition a predicate that returns a boolean indicating whether to continue collecting
     * @return a parser that collects characters while the condition is true
     * @throws IllegalArgumentException if the condition predicate is null
     */
    public static Taker<String> takeWhile(CharPredicate condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition parser cannot be null");
        }

        return new Taker<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            int current = start;
            int length = data.length();

            while (current < length && condition.test(data.charAt(current))) {
                current++;
            }
            return new Match<>(data.subSequence(start, current).toString(), in.skip(current - start));
        });
    }

    /**
     * Collects characters until the condition is met.
     *
     * @param condition the condition that stops the collection
     * @return characters until the condition is met
     */
    public static Taker<String> takeUntil(CharPredicate condition) {
        return takeWhile(condition.negate());
    }


    /**
     * Collects characters until the first occurrence of the given needle.
     * <pre>{@code
     * takeUntil("-->").parse("comment-->").value(); // "comment"
     * }</pre>
     *
     * @param needle delimiter string
     * @return characters before the needle
     */
    public static Taker<String> takeUntil(String needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) {
            return new Taker<>(in -> new Match<>("", in));
        }

        return new Taker<>(in -> {
            CharSequence data = in.data();
            int from = in.position();
            int idx = indexOf(data, needle, from);

            if (idx < 0) {
                // Not found: consume to EOF
                String out = data.subSequence(from, data.length()).toString();
                return new Match<>(out, in.skip(data.length() - from));
            } else {
                String out = data.subSequence(from, idx).toString();
                return new Match<>(out, in.skip(idx - from));
            }
        });
    }

    private static int indexOf(CharSequence haystack, String needle, int from) {
        if (haystack instanceof String s) {
            return s.indexOf(needle, from);
        }
        if (haystack instanceof CharBuffer cb && cb.hasArray()) {
            char[] array = cb.array();
            int offset = cb.arrayOffset();
            int start = Math.max(0, from);
            int n = cb.length();
            int m = needle.length();
            if (m == 0) return start;
            if (n < m) return -1;
            
            char first = needle.charAt(0);
            int max = n - m;
            for (int i = start; i <= max; i++) {
                if (array[offset + i] != first) {
                    while (++i <= max && array[offset + i] != first);
                }
                if (i <= max) {
                    int j = i + 1;
                    int end = j + m - 1;
                    for (int k = 1; j < end && array[offset + j] == needle.charAt(k); j++, k++);
                    if (j == end) return i;
                }
            }
            return -1;
        }

        char c0 = needle.charAt(0);
        int max = haystack.length() - needle.length();
        outer: for (int i = Math.max(0, from); i <= max; i++) {
            if (haystack.charAt(i) != c0) continue;
            for (int j = 1; j < needle.length(); j++) {
                if (haystack.charAt(i + j) != needle.charAt(j)) continue outer;
            }
            return i;
        }
        return -1;
    }


    /**
     * Matches an exact string of characters.
     * <pre>{@code
     * take("if").parse("if").value(); // "if"
     * }</pre>
     *
     * @param str exact string to match
     * @return a parser matching the string
     * @see #regex(String)
     * @see #chr(char)
     */
    public static Taker<String> take(String str) {
        if (str.isEmpty()) return pure("");
        return new Taker<>(in -> {
            if (in.data().toString().startsWith(str, in.position())) {
                return new Match<>(str, in.skip(str.length()));
            }
            return new NoMatch<>(in, str);
        });
    }


    /**
     * Matches input against a regular expression pattern.
     * <pre>{@code
     * regex("[a-z]+", Pattern.CASE_INSENSITIVE).parse("ABC").value(); // "ABC"
     * }</pre>
     *
     * @param regex regular expression pattern
     * @param flags Pattern flags
     * @return a parser matching the regex
     * @see Pattern
     */
    public static Taker<String> regex(String regex, int flags) {
        Pattern pattern = Pattern.compile(regex, flags);

        return new Taker<>(in -> {
            CharSequence data = in.data();
            int pos = in.position();

            Matcher matcher = pattern.matcher(data);
            matcher.region(pos, data.length());

            if (matcher.lookingAt()) {
                String match = matcher.group();
                return new Match<>(match, in.skip(match.length()));
            }

            return new NoMatch<>(in, regex);
        });
    }

    /**
     * Matches input against a regular expression pattern using default flags.
     * <pre>{@code
     * regex("\\d+").parse("123").value(); // "123"
     * }</pre>
     *
     * @param regex regular expression pattern
     * @return a parser matching the regex
     * @see #regex(String, int)
     */
    public static Taker<String> regex(String regex) {
        return regex(regex, 0);
    }

    /**
     * Matches a specific character.
     * <pre>{@code
     * chr(',').parse(",").value(); // ','
     * }</pre>
     *
     * @param c character to match
     * @return a parser matching the character
     * @see Combinators#is
     */
    public static Taker<Character> chr(char c) {
        return Combinators.is(c);
    }

    /**
     * Parses a string enclosed in quotes with support for escape sequences.
     * <p>
     * This version uses an {@link IntObjectMap} for escape character mappings to avoid boxing.
     *
     * @param quote   the character used for quoting (e.g., '"')
     * @param escape  the character used for escaping (e.g., '\\')
     * @param escapes an {@link IntObjectMap} of escape characters to their literal values
     * @return a parser that matches an escaped string and returns its unescaped content
     */
    public static Taker<String> escapedString(char quote, char escape, IntObjectMap<Character> escapes) {
        return new Taker<>(in -> {
            if (in.isEof() || in.current() != quote) {
                return new NoMatch<>(in, String.valueOf(quote));
            }

            CharSequence data = in.data();
            int startPos = in.position();
            int currentPos = startPos + 1; // skip opening quote
            int dataLen = data.length();
            StringBuilder sb = new StringBuilder();
            int lastCopiedPos = currentPos;

            while (currentPos < dataLen) {
                char c = data.charAt(currentPos);
                if (c == quote) {
                    sb.append(data, lastCopiedPos, currentPos);
                    return new Match<>(sb.toString(), in.skip(currentPos - startPos + 1));
                }
                if (c == escape) {
                    sb.append(data, lastCopiedPos, currentPos);
                    currentPos++;
                    if (currentPos >= dataLen) {
                        return new NoMatch<>(in.skip(currentPos - startPos), "closing quote after escape");
                    }
                    char escapedChar = data.charAt(currentPos);
                    Character replacement = escapes.get(escapedChar);
                    if (replacement != null) {
                        sb.append(replacement);
                    } else {
                        sb.append(escapedChar);
                    }
                    lastCopiedPos = currentPos + 1;
                }
                currentPos++;
            }

            return new NoMatch<>(in.skip(currentPos - startPos), "closing quote '" + quote + "'");
        });
    }

    /**
     * Parses a string enclosed in quotes with support for escape sequences.
     * <p>
     * This parser is optimized for performance by directly scanning the input data.
     * It handles:
     * <ul>
     *   <li>Finding the opening quote (must match exactly at the current position)</li>
     *   <li>Scanning for the closing quote</li>
     *   <li>Handling escape sequences defined in the provided map</li>
     *   <li>Correctly reporting errors for unclosed strings or invalid escape sequences</li>
     * </ul>
     *
     * @param quote   the character used for quoting (e.g., '"')
     * @param escape  the character used for escaping (e.g., '\\')
     * @param escapes a map of escape characters to their literal values (e.g., 'n' -> '\n')
     * @return a parser that matches an escaped string and returns its unescaped content
     */
    public static Taker<String> escapedString(char quote, char escape, Map<Character, Character> escapes) {
        IntObjectMap<Character> map = new IntObjectMap<>();
        escapes.forEach(map::put);
        return escapedString(quote, escape, map);
    }

    /**
     * Matches a single character matching the given predicate.
     * <pre>{@code
     * chr(Character::isDigit).parse("1").value(); // '1'
     * }</pre>
     *
     * @param predicate condition for the character
     * @return a parser matching characters by predicate
     * @see Combinators#satisfy
     */
    public static Taker<Character> chr(CharPredicate predicate) {
        return satisfy("<character>", predicate);
    }

    /**
     * Creates a parser that always succeeds with the given value without consuming input.
     * <pre>{@code
     * Taker.pure(42).parse("any").value(); // 42
     * }</pre>
     *
     * @param value value to return
     * @param <A>   result type
     * @return a parser that always succeeds with the given value
     */
    public static <A> Taker<A> pure(A value) {
        return new Taker<>(input -> new Match<>(value, input));
    }

    /** Matches any alphanumeric character. */
    public static final Taker<Character> alphaNumeric = satisfy("<alphanumeric>", Character::isLetterOrDigit);

    /** Matches a sequence of one or more letters. */
    public static final Taker<String> word = satisfy("<word>", Character::isLetter).oneOrMore().map(io.github.parseworks.taker.Lists::join);

    /** Matches one or more whitespace characters. */
    public static final Taker<String> whitespace = satisfy("<whitespace>", Character::isWhitespace).oneOrMore().map(io.github.parseworks.taker.Lists::join);
}
