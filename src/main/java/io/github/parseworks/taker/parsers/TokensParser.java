package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;
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
        CharPredicate.asciiLetter.or(CharPredicate.is('_'));
    private static final CharPredicate DEFAULT_IDENTIFIER_PART =
        CharPredicate.asciiLetterOrDigit.or(CharPredicate.is('_'));

    private final Taker<?> ignored;

    private TokensParser(Taker<?> ignored) {
        this.ignored = Objects.requireNonNull(ignored, "ignored");
    }

    /**
     * Creates a token facade that skips zero or more characters accepted by the
     * given predicate around each token.
     *
     * @param ignored predicate for ignored characters
     * @return a token parser facade
     */
    public static TokensParser skipping(CharPredicate ignored) {
        return skipping(Lexical.skipWhile(ignored));
    }

    /**
     * Creates a token facade that repeatedly applies {@code ignored} around
     * each token.
     *
     * @param ignored parser for ignored input such as whitespace or comments
     * @return a token parser facade
     */
    public static TokensParser skipping(Taker<?> ignored) {
        return new TokensParser(ignored);
    }

    /**
     * Wraps a raw parser so ignored input is skipped before and after it.
     *
     * @param parser raw parser
     * @param <A> parser result type
     * @return token parser
     */
    public <A> Taker<A> token(Taker<A> parser) {
        return Lexical.lexeme(parser, ignored);
    }

    /** Matches a character token. */
    public Taker<Character> chr(char value) {
        return token(Lexical.chr(value));
    }

    /** Matches a character token ignoring case. */
    public Taker<Character> chrIgnoreCase(char value) {
        return token(Lexical.chrIgnoreCase(value));
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
        return token(Lexical.oneOf(chars));
    }

    /** Matches one character from the supplied token character set ignoring case. */
    public Taker<Character> oneOfIgnoreCase(String chars) {
        return token(Lexical.oneOfIgnoreCase(chars));
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

        return new Taker<>(in -> {
            Result<String> result = stringParser.apply(in);
            if (!result.matches()) {
                return result;
            }

            Input next = result.input();
            if (!next.isEof() && identifierPart.test(next.current())) {
                return new NoMatch<>(next, "keyword boundary after " + value);
            }
            return result;
        });
    }
}
