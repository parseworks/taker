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

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.util.Objects;

import static io.github.parseworks.taker.parsers.Combinators.satisfy;

/**
 * Character-level parsers and scanner fast paths.
 * <p>
 * Use this class when parsing individual characters or contiguous runs of
 * characters. Scanner methods such as {@link #takeWhile(CharPredicate)},
 * {@link #collectChars(CharPredicate)}, {@link #skipWhile(CharPredicate)}, and
 * {@link #countWhile(CharPredicate)} consume raw input directly and avoid the
 * per-character overhead of repeated single-character parsers.
 */
public final class Chars {

    private Chars() {
    }

    /** Matches a single ASCII letter ({@code a-z} or {@code A-Z}). */
    public static final Taker<Character> alpha = satisfy(CharPredicate.asciiLetter.expected(), CharPredicate.asciiLetter);

    /** Matches a single ASCII letter or digit. */
    public static final Taker<Character> alphaNumeric = satisfy(CharPredicate.asciiLetterOrDigit.expected(), CharPredicate.asciiLetterOrDigit);

    /**
     * Matches one or more ASCII space characters ({@code ' '}).
     * <p>
     * This parser does not match tabs, newlines, or other whitespace characters.
     */
    public static final Taker<String> spaces = takeWhile(CharPredicate.is(' '));

    /**
     * Matches one or more Java whitespace characters according to
     * {@link Character#isWhitespace(char)}.
     * <p>
     * This includes line separators such as {@code '\n'} and {@code '\r'}, so
     * use it only when crossing line boundaries is intended by the grammar.
     */
    public static final Taker<String> whitespace = takeWhile(CharPredicate.whitespace);

    /** Matches a sequence of letters. */
    public static final Taker<String> word = takeWhile(CharPredicate.letter);

    /** Matches characters until a newline without consuming the newline. */
    public static final Taker<String> line = takeUntil(CharPredicate.is('\n'));

    /**
     * Matches a character satisfying the given predicate.
     *
     * @param condition predicate to satisfy
     * @return a parser for one matching character
     */
    public static Taker<Character> take(CharPredicate condition) {
        Objects.requireNonNull(condition, "condition");
        return chr(condition);
    }

    /**
     * Matches one or more consecutive characters while {@code condition} is true.
     *
     * @param condition predicate to satisfy
     * @return a parser returning the matched text
     */
    public static Taker<String> takeWhile(CharPredicate condition) {
        Objects.requireNonNull(condition, "condition");

        return new Taker<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            int current = start;
            int length = data.length();

            while (current < length && condition.test(data.charAt(current))) {
                current++;
            }
            int count = current - start;
            if (count == 0) {
                return new NoMatch<>(in, "at least one " + condition.expected());
            }
            return new Match<>(data.subSequence(start, current).toString(), in.skip(count));
        });
    }

    /**
     * Alias for {@link #takeWhile(CharPredicate)} with collection-oriented naming.
     *
     * @param condition predicate to satisfy
     * @return a parser returning the matched text
     */
    public static Taker<String> collectChars(CharPredicate condition) {
        return takeWhile(condition);
    }

    /**
     * Skips zero or more matching input characters without materializing text.
     *
     * @param condition predicate to satisfy
     * @return a parser that consumes matching characters
     */
    public static Taker<Void> skipWhile(CharPredicate condition) {
        Objects.requireNonNull(condition, "condition");
        return new Taker<>(in -> {
            int count = countMatchingChars(in, condition);
            return new Match<>(null, in.skip(count));
        });
    }

    /**
     * Counts and consumes zero or more matching input characters.
     *
     * @param condition predicate to satisfy
     * @return a parser returning the consumed character count
     */
    public static Taker<Integer> countWhile(CharPredicate condition) {
        Objects.requireNonNull(condition, "condition");
        return new Taker<>(in -> {
            int count = countMatchingChars(in, condition);
            return new Match<>(count, in.skip(count));
        });
    }

    /**
     * Collects characters until {@code condition} succeeds.
     *
     * @param condition terminator predicate
     * @return a parser returning text before the terminator
     */
    public static Taker<String> takeUntil(CharPredicate condition) {
        Objects.requireNonNull(condition, "condition");
        return new Taker<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            int len = data.length();
            for (int i = start; i < len; i++) {
                if (condition.test(data.charAt(i))) {
                    int count = i - start;
                    String out = data.subSequence(start, i).toString();
                    return new Match<>(out, in.skip(count));
                }
            }
            int count = len - start;
            String out = data.subSequence(start, len).toString();
            return new Match<>(out, in.skip(count));
        });
    }

    /**
     * Matches any single character from {@code chars}.
     *
     * @param chars accepted characters
     * @return a parser for one matching character
     */
    public static Taker<Character> oneOf(String chars) {
        Objects.requireNonNull(chars, "chars");
        if (chars.isEmpty()) {
            throw new IllegalArgumentException("chars must not be empty");
        }
        return chr(CharPredicate.anyOf(chars));
    }

    /**
     * Matches any single character from {@code chars}, ignoring case.
     *
     * @param chars accepted characters
     * @return a parser for one matching character
     */
    public static Taker<Character> oneOfIgnoreCase(String chars) {
        Objects.requireNonNull(chars, "chars");
        if (chars.isEmpty()) {
            throw new IllegalArgumentException("chars must not be empty");
        }
        return chr(CharPredicate.anyOfIgnoreCase(chars));
    }

    /**
     * Matches a specific character.
     *
     * @param c character to match
     * @return a parser for the character
     */
    public static Taker<Character> chr(char c) {
        return Combinators.is(c);
    }

    /**
     * Matches a specific character, ignoring case.
     *
     * @param c character to match
     * @return a parser for the character
     */
    public static Taker<Character> chrIgnoreCase(char c) {
        return chr(CharPredicate.isIgnoreCase(c));
    }

    /**
     * Matches a single character matching the given predicate.
     *
     * @param predicate predicate to satisfy
     * @return a parser for one matching character
     */
    public static Taker<Character> chr(CharPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return satisfy(predicate.expected(), predicate);
    }

    private static int countMatchingChars(Input in, CharPredicate condition) {
        CharSequence data = in.data();
        int start = in.position();
        int current = start;
        int length = data.length();

        while (current < length && condition.test(data.charAt(current))) {
            current++;
        }
        return current - start;
    }
}
