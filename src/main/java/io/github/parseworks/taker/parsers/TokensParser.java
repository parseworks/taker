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
import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.util.Objects;

/**
 * Token-oriented parser facade with a caller-defined ignored-input policy.
 * <p>
 * A {@code TokensParser} keeps {@link Input} raw and explicit while making
 * token grammars easier to write. Each token parser skips ignored input before
 * and after the wrapped parser. The ignored parser should consume input when it
 * succeeds; non-consuming success stops the skip loop.
 * <pre>{@code
 * TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);
 *
 * Taker<String> assignment = tokens.identifier()
 *     .thenSkip(tokens.chr('='))
 *     .then(tokens.identifier())
 *     .map((left, right) -> left + "=" + right);
 * }</pre>
 */
public final class TokensParser {

    private static final CharPredicate DEFAULT_IDENTIFIER_START =
        CharPredicate.identifierStart;
    private static final CharPredicate DEFAULT_IDENTIFIER_PART =
        CharPredicate.identifierPart;

    private final CharPredicate ignoredChars;
    private final Taker<?> ignoredParser;

    private TokensParser(CharPredicate ignoredChars, Taker<?> ignoredParser) {
        this.ignoredChars = ignoredChars;
        this.ignoredParser = ignoredParser;
    }

    /**
     * Creates a token facade that skips zero or more characters accepted by the
     * given predicate around each token.
     *
     * @param ignored predicate for ignored characters
     * @return a token parser facade
     */
    public static TokensParser skipping(CharPredicate ignored) {
        return new TokensParser(Objects.requireNonNull(ignored, "ignored"), null);
    }

    /**
     * Creates a token facade that repeatedly applies {@code ignored} around
     * each token.
     *
     * @param ignored parser for ignored input such as whitespace or comments
     * @return a token parser facade
     */
    public static TokensParser skipping(Taker<?> ignored) {
        return new TokensParser(null, Objects.requireNonNull(ignored, "ignored"));
    }

    /**
     * Wraps a raw parser so ignored input is skipped before and after it.
     *
     * @param parser raw parser
     * @param <A> parser result type
     * @return token parser
     */
    public <A> Taker<A> token(Taker<A> parser) {
        Objects.requireNonNull(parser, "parser");
        if (ignoredChars != null) {
            return new Taker<>(in -> {
                Input current = skipIgnoredChars(in);
                Result<A> result = parser.apply(current);
                if (result.matches()) {
                    return new Match<>(result.value(), skipIgnoredChars(result.input()));
                }
                return result;
            });
        }
        return Lexical.lexeme(parser, ignoredParser);
    }

    private Input skipIgnoredChars(Input in) {
        CharSequence data = in.data();
        int start = in.position();
        int current = start;
        int length = data.length();

        while (current < length && ignoredChars.test(data.charAt(current))) {
            current++;
        }
        return current == start ? in : in.skip(current - start);
    }

    /** Matches a character token. */
    public Taker<Character> chr(char value) {
        return token(Chars.chr(value));
    }

    /** Matches a character token ignoring case. */
    public Taker<Character> chrIgnoreCase(char value) {
        return token(Chars.chrIgnoreCase(value));
    }

    /** Matches a string token. */
    public Taker<String> string(String value) {
        return token(Lexical.string(value));
    }

    /** Matches a string token ignoring case. */
    public Taker<String> stringIgnoreCase(String value) {
        return token(Lexical.stringIgnoreCase(value));
    }

    /** Matches one character from the supplied token character set. */
    public Taker<Character> oneOf(String chars) {
        return token(Chars.oneOf(chars));
    }

    /** Matches one character from the supplied token character set ignoring case. */
    public Taker<Character> oneOfIgnoreCase(String chars) {
        return token(Chars.oneOfIgnoreCase(chars));
    }

    /**
     * Matches a keyword token using a Java-like ASCII identifier boundary.
     *
     * @param value keyword text
     * @return keyword token parser
     */
    public Taker<String> keyword(String value) {
        return keyword(value, DEFAULT_IDENTIFIER_PART);
    }

    /**
     * Matches a keyword token and rejects matches followed by an identifier
     * part character.
     *
     * @param value keyword text
     * @param identifierPart predicate for characters that may continue an identifier
     * @return keyword token parser
     */
    public Taker<String> keyword(String value, CharPredicate identifierPart) {
        return token(keywordRaw(value, identifierPart, false));
    }

    /**
     * Matches a keyword token ignoring case using a Java-like ASCII identifier
     * boundary.
     *
     * @param value keyword text
     * @return keyword token parser
     */
    public Taker<String> keywordIgnoreCase(String value) {
        return keywordIgnoreCase(value, DEFAULT_IDENTIFIER_PART);
    }

    /**
     * Matches a keyword token ignoring case and rejects matches followed by an
     * identifier part character.
     *
     * @param value keyword text
     * @param identifierPart predicate for characters that may continue an identifier
     * @return keyword token parser
     */
    public Taker<String> keywordIgnoreCase(String value, CharPredicate identifierPart) {
        return token(keywordRaw(value, identifierPart, true));
    }

    /**
     * Matches an identifier token using Java-like ASCII identifier characters:
     * a letter or underscore followed by letters, digits, or underscores.
     *
     * @return identifier token parser
     */
    public Taker<String> identifier() {
        return identifier(DEFAULT_IDENTIFIER_START, DEFAULT_IDENTIFIER_PART);
    }

    /**
     * Matches an identifier token.
     *
     * @param start predicate for the first identifier character
     * @param part predicate for subsequent identifier characters
     * @return identifier token parser
     */
    public Taker<String> identifier(CharPredicate start, CharPredicate part) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(part, "part");
        return token(new Taker<>(in -> {
            if (in.isEof() || !start.test(in.current())) {
                return new NoMatch<>(in, "identifier");
            }

            CharSequence data = in.data();
            int current = in.position() + 1;
            int length = data.length();
            while (current < length && part.test(data.charAt(current))) {
                current++;
            }

            int startPosition = in.position();
            return new Match<>(
                data.subSequence(startPosition, current).toString(),
                in.skip(current - startPosition)
            );
        }));
    }

    private static Taker<String> keywordRaw(String value, CharPredicate identifierPart, boolean ignoreCase) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(identifierPart, "identifierPart");
        Taker<String> stringParser = ignoreCase ? Lexical.stringIgnoreCase(value) : Lexical.string(value);
        String expectedKeyword = ignoreCase
            ? "keyword \"" + value + "\" ignoring case"
            : "keyword \"" + value + "\"";
        String expectedBoundary = "keyword boundary after \"" + value + "\"";

        return new Taker<>(in -> {
            Result<String> result = stringParser.apply(in);
            if (!result.matches()) {
                return new NoMatch<>(result.input(), expectedKeyword, (Failure<?>) result);
            }

            Input next = result.input();
            if (!next.isEof() && identifierPart.test(next.current())) {
                return new NoMatch<>(next, expectedBoundary);
            }
            return result;
        });
    }
}
