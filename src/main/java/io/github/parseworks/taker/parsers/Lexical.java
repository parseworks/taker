/*
 * Copyright (c) 2026 jason bailey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common text parsers for strings, regular expressions, escaping, and
 * whitespace trimming.
 * <p>
 * Use {@link Chars} for character-level parsers and scanner fast paths.
 * <pre>{@code
 * Taker<String> greeting =
 *     Lexical.string("Hello").thenSkip(Chars.skipWhile(CharPredicate.horizontalWhitespace)).then(Chars.word);
 * }</pre>
 */
public class Lexical {

    /**
     * Trims ASCII spaces around the given parser.
     * <p>
     * This is intentionally conservative: it skips only {@code ' '} and does
     * not skip tabs, newlines, or other Unicode whitespace. Use
     * {@link #trimWhitespace(Taker)} when line-breaking whitespace should be
     * ignored too.
     */
    public static <A> Taker<A> trim(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
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
        Objects.requireNonNull(parser, "parser");
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
        Objects.requireNonNull(parser, "parser");
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
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(ignored, "ignored");
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

    /** Collects characters until the first occurrence of {@code needle}. */
    public static Taker<String> takeUntil(String needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) {
            return new Taker<>(in -> new Match<>("", in));
        }

        return new Taker<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            int idx = indexOf(data, needle, start);
            if (idx < 0) {
                String out = data.subSequence(start, data.length()).toString();
                return new Match<>(out, in.skip(data.length() - start));
            }
            String out = data.subSequence(start, idx).toString();
            return new Match<>(out, in.skip(idx - start));
        });
    }

    /**
     * Returns the first index of {@code needle} at or after {@code from}.
     *
     * @param haystack text to search
     * @param needle non-empty text to find
     * @param from starting index
     * @return the first matching index, or {@code -1}
     */
    public static int indexOf(CharSequence haystack, String needle, int from) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) {
            throw new IllegalArgumentException("needle must not be empty");
        }
        if (haystack instanceof String s) {
            return s.indexOf(needle, from);
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




    /** Matches {@code str} exactly at the current input position. */
    public static Taker<String> string(String str) {
        Objects.requireNonNull(str, "str");
        String[] expectedChars = expectedChars(str);
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
                return new NoMatch<>(in.skip(matched), expectedChars[matched]);
            }

            for (int i = 0; i < strLen; i++) {
                if (str.charAt(i) != data.charAt(start + i)) {
                    return new NoMatch<>(in.skip(i), expectedChars[i]);
                }
            }

            return new Match<>(str, in.skip(strLen));
        });
    }

    /** Matches {@code str} at the current input position, ignoring case. */
    public static Taker<String> stringIgnoreCase(String str) {
        Objects.requireNonNull(str, "str");
        String[] expectedChars = expectedChars(str);
        return new Taker<>(in -> {
            if (str.isEmpty()) {
                return new Match<>("", in);
            }

            CharSequence data = in.data();
            int start = in.position();
            int strLen = str.length();

            if (start + strLen > data.length()) {
                int matched = 0;
                while (matched < data.length() - start
                        && charsEqualIgnoreCase(str.charAt(matched), data.charAt(start + matched))) {
                    matched++;
                }
                return new NoMatch<>(in.skip(matched), expectedChars[matched]);
            }

            for (int i = 0; i < strLen; i++) {
                if (!charsEqualIgnoreCase(str.charAt(i), data.charAt(start + i))) {
                    return new NoMatch<>(in.skip(i), expectedChars[i]);
                }
            }

            return new Match<>(str, in.skip(strLen));
        });
    }

    /** Matches a regular expression at the current input position. */
    public static Taker<String> regex(String regex, int flags) {
        Objects.requireNonNull(regex, "regex");
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

    /** Matches a regular expression at the current input position using default flags. */
    public static Taker<String> regex(String regex) {
        return regex(regex, 0);
    }


    /** Parses a string enclosed in quotes with support for escape sequences. */
    private static Taker<String> escapedStringImpl(char quote, char escape, Map<Character, Character> escapes) {

        return new Taker<>(in -> {
            if (in.isEof() || in.current() != quote) {
                return new NoMatch<>(in, expectedChar(quote));
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

            return new NoMatch<>(in.skip(currentPos - startPos), "closing quote " + expectedChar(quote));
        });
    }

    /** Parses a quoted string with caller-supplied escape replacements. */
    public static Taker<String> escapedString(char quote, char escape, Map<Character, Character> escapes) {
        Objects.requireNonNull(escapes, "escapes");
        return escapedStringImpl(quote, escape, new HashMap<>(escapes));
    }

    private static boolean charsEqualIgnoreCase(char expected, char actual) {
        return expected == actual
                || Character.toLowerCase(expected) == Character.toLowerCase(actual)
                || Character.toUpperCase(expected) == Character.toUpperCase(actual);
    }

    private static String[] expectedChars(String str) {
        String[] expected = new String[str.length()];
        for (int i = 0; i < str.length(); i++) {
            expected[i] = expectedChar(str.charAt(i));
        }
        return expected;
    }

    private static String expectedChar(char c) {
        return "'" + display(c) + "'";
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
