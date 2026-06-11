package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.impl.IntObjectMap;
import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.parseworks.taker.parsers.Combinators.satisfy;

/**
 * Common text parsers for characters, strings, and whitespace.
 * <pre>{@code
 * Taker<String> greeting =
 *     Lexical.string("Hello").thenSkip(Taker.takeWhile(CharPredicate.horizontalWhitespace)).then(Lexical.word);
 * }</pre>
 */
public class Lexical {


    /** Matches a single alphabetical character (a-z, A-Z). */
    public static final Taker<Character> alpha = satisfy(CharPredicate.asciiLetter.expected(), CharPredicate.asciiLetter);

    /**
     * Matches a single alphanumeric character.
     */
    public static final Taker<Character> alphaNumeric = satisfy(CharPredicate.asciiLetterOrDigit.expected(), CharPredicate.asciiLetterOrDigit);

    /**
     * Matches one or more ASCII space characters ({@code ' '}).
     * <p>
     * This parser does not match tabs, newlines, or other whitespace characters.
     */
    public static final Taker<String> spaces = Taker.takeWhile(CharPredicate.is(' '));

    /**
     * Matches one or more Java whitespace characters according to
     * {@link Character#isWhitespace(char)}.
     * <p>
     * This includes line separators such as {@code '\n'} and {@code '\r'}, so
     * use it only when crossing line boundaries is intended by the grammar.
     */
    public static final Taker<String> whitespace = Taker.takeWhile(CharPredicate.whitespace);

    /**
     * Trims ASCII spaces around the given parser.
     * <p>
     * This is intentionally conservative: it skips only {@code ' '} and does
     * not skip tabs, newlines, or other Unicode whitespace. Use
     * {@link #trimWhitespace(Taker)} when line-breaking whitespace should be
     * ignored too.
     */
    public static <A> Taker<A> trim(Taker<A> parser) {
        return trimSpaces(parser);
    }

    /**
     * Trims ASCII spaces around the given parser.
     * <p>
     * This is an explicit alias for {@link #trim(Taker)}. It is useful in
     * grammars where newlines are meaningful and should not be skipped by token
     * parsing.
     */
    public static <A> Taker<A> trimSpaces(Taker<A> parser) {
        return new Taker<>(in -> {
            Input trimmedInput = skipSpaces(in);
            Result<A> result = parser.apply(trimmedInput);
            if (result.matches()) {
                trimmedInput = skipSpaces(result.input());
                return new Match<>(result.value(), trimmedInput);
            }
            return result;
        });
    }

    /**
     * Trims Java whitespace around the given parser.
     * <p>
     * This skips every character accepted by {@link Character#isWhitespace(char)},
     * including tabs and line separators. Prefer {@link #trim(Taker)} or
     * {@link #trimSpaces(Taker)} when newlines have grammatical meaning.
     */
    public static <A> Taker<A> trimWhitespace(Taker<A> parser) {
        return new Taker<>(in -> {
            Input trimmedInput = skipCharacterWhitespace(in);
            Result<A> result = parser.apply(trimmedInput);
            if (result.matches()) {
                trimmedInput = skipCharacterWhitespace(result.input());
                return new Match<>(result.value(), trimmedInput);
            }
            return result;
        });
    }

    /**
     * Trims caller-defined ignored input around the given parser.
     * <p>
     * The {@code ignored} parser is applied repeatedly before and after
     * {@code parser}. It should consume at least one character when it succeeds.
     * If it succeeds without advancing, trimming stops to avoid an infinite
     * loop.
     */
    public static <A> Taker<A> lexeme(Taker<A> parser, Taker<?> ignored) {
        return new Taker<>(in -> {
            Input trimmedInput = skipIgnored(in, ignored);
            Result<A> result = parser.apply(trimmedInput);
            if (result.matches()) {
                trimmedInput = skipIgnored(result.input(), ignored);
                return new Match<>(result.value(), trimmedInput);
            }
            return result;
        });
    }

    private static Input skipSpaces(Input in) {
        while (!in.isEof() && in.current() == ' ') {
            in = in.next();
        }
        return in;
    }

    private static Input skipCharacterWhitespace(Input in) {
        while (!in.isEof() && Character.isWhitespace(in.current())) {
            in = in.next();
        }
        return in;
    }

    private static Input skipIgnored(Input in, Taker<?> ignored) {
        Input current = in;
        while (true) {
            Result<?> result = ignored.apply(current);
            if (!result.matches() || result.input().position() == current.position()) {
                return current;
            }
            current = result.input();
        }
    }

    /** Matches a sequence of letters. */
    public static final Taker<String> word = Taker.takeWhile(CharPredicate.letter);

    /**
     * Matches characters until a newline.
     */
    public static final Taker<String> line = Taker.takeUntil(CharPredicate.is('\n'));


    /** Collects characters until the first occurrence of the given needle. */
    public static Taker<String> takeUntil(String needle) {
        return Taker.takeUntil(needle);
    }

    public static int indexOf(CharSequence haystack, String needle, int from) {
        // Use the platform’s indexOf for CharSequence (via toString() only if necessary)
        if (haystack instanceof String s) {
            return s.indexOf(needle, from);
        }
        // Avoid copying when possible: manual scan using first-char filter
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




    /** Matches an exact string of characters. */
    public static Taker<String> string(String str) {
        return new Taker<>(in -> {
            if (str.isEmpty()) {
                return new Match<>("", in);
            }

            CharSequence data = in.data();
            int start = in.position();
            int strLen = str.length();

            if (start + strLen > data.length()) {
                // Find how many characters matched
                int matched = 0;
                while (matched < data.length() - start && str.charAt(matched) == data.charAt(start + matched)) {
                    matched++;
                }
                return new NoMatch<>(in.skip(matched), str.substring(0, 1));
            }

            for (int i = 0; i < strLen; i++) {
                if (str.charAt(i) != data.charAt(start + i)) {
                    return new NoMatch<>(in.skip(i), str.substring(i, i + 1));
                }
            }

            return new Match<>(str, in.skip(strLen));
        });
    }

    /** Matches any single character from the provided string. */
    public static Taker<Character> oneOf(String str) {
        if (str == null || str.isEmpty()) {
            return Combinators.fail("any character in empty string");
        }
        // For small strings (under 10 chars), this approach is efficient
        if (str.length() < 10) {
            return satisfy("<oneOf> " + str, c -> str.indexOf(c) != -1);
        }

        // For larger character sets, use a Set for O(1) lookups
        Set<Character> charSet = new HashSet<>();
        for (int i = 0; i < str.length(); i++) {
            charSet.add(str.charAt(i));
        }

        return satisfy("character in set [" + str + "]", charSet::contains);
    }

    /** Matches input against a regular expression pattern. */
    public static Taker<String> regex(String regex, int flags) {
        Pattern pattern = Pattern.compile(regex, flags);

        return new Taker<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            Matcher matcher = pattern.matcher(data);
            matcher.region(start, data.length());

            if (matcher.lookingAt()) {
                String match = matcher.group();
                return new Match<>(match, in.skip(match.length()));
            }

            return new NoMatch<>(in, regex);
        });
    }

    /** Matches input against a regular expression pattern using default flags. */
    public static Taker<String> regex(String regex) {
        return regex(regex, 0);
    }


    /** Parses a string enclosed in quotes with support for escape sequences. */
    private static Taker<String> escapedString(char quote, char escape, IntObjectMap<Character> escapes) {

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
                if (c == escape) {
                    int escapePos = currentPos;
                    currentPos++;
                    if (currentPos >= dataLen) {
                        if (escape == quote) {
                            sb.append(data, lastCopiedPos, escapePos);
                            return new Match<>(sb.toString(), in.skip(escapePos - startPos + 1));
                        }
                        return new NoMatch<>(in.skip(currentPos - startPos), "closing quote after escape");
                    }
                    char escapedChar = data.charAt(currentPos);
                    Character replacement = escapes.get(escapedChar);
                    if (replacement != null) {
                        sb.append(data, lastCopiedPos, escapePos);
                        sb.append(replacement);
                        lastCopiedPos = currentPos + 1;
                    } else if (escape == quote) {
                        // Rollback and treat as closing quote
                        sb.append(data, lastCopiedPos, escapePos);
                        return new Match<>(sb.toString(), in.skip(escapePos - startPos + 1));
                    } else {
                        sb.append(data, lastCopiedPos, escapePos);
                        sb.append(escapedChar);
                        lastCopiedPos = currentPos + 1;
                    }
                } else if (c == quote) {
                    sb.append(data, lastCopiedPos, currentPos);
                    return new Match<>(sb.toString(), in.skip(currentPos - startPos + 1));
                }
                currentPos++;
            }

            return new NoMatch<>(in.skip(currentPos - startPos), "closing quote '" + quote + "'");
        });
    }

    /** Parses a string enclosed in quotes with support for escape sequences. */
    public static Taker<String> escapedString(char quote, char escape, Map<Character, Character> escapes) {
        IntObjectMap<Character> map = new IntObjectMap<>();
        escapes.forEach(map::put);
        return escapedString(quote, escape, map);
    }



    /** Matches a specific character. */
    public static Taker<Character> chr(char c) {
        return Combinators.is(c);
    }

    /** Matches a single character matching the given predicate. */
    public static Taker<Character> chr(CharPredicate predicate) {
        return satisfy(predicate.expected(), predicate);
    }
}
