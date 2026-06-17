package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokensParserTest {

    @Test
    void tokenWrapsParserWithIgnoredInput() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        Result<String> result = tokens.token(string("hello")).parse(" \n hello \t world");

        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals('w', result.input().current());
    }

    @Test
    void characterAndStringTokensSkipIgnoredInput() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        Taker<String> parser = tokens.chr('(')
            .skipThen(tokens.string("name"))
            .thenSkip(tokens.chr(')'));

        Result<String> result = parser.parse("( name ) next");

        assertTrue(result.matches());
        assertEquals("name", result.value());
        assertEquals('n', result.input().current());
    }

    @Test
    void keywordRejectsIdentifierContinuations() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        assertTrue(tokens.keyword("if").parse("if (x)").matches());
        assertFalse(tokens.keyword("if").parse("ifdef").matches());
        assertFalse(tokens.keyword("if").parse("if_").matches());
        assertFalse(tokens.keyword("if").parse("if1").matches());
    }

    @Test
    void keywordFailuresReportKeywordExpectationAndBoundary() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        Failure<String> mismatch = (Failure<String>) tokens.keyword("if").parse("else");
        assertEquals("keyword \"if\"", mismatch.expected());
        assertEquals(0, mismatch.input().position());
        assertEquals("'i'", mismatch.cause().expected());

        Failure<String> boundary = (Failure<String>) tokens.keyword("if").parse("ifdef");
        assertEquals("keyword boundary after \"if\"", boundary.expected());
        assertEquals(2, boundary.input().position());
    }

    @Test
    void keywordIgnoreCaseRejectsIdentifierContinuations() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        Result<String> result = tokens.keywordIgnoreCase("select").parse(" SeLeCt *");

        assertTrue(result.matches());
        assertEquals("select", result.value());
        assertEquals('*', result.input().current());
        assertFalse(tokens.keywordIgnoreCase("select").parse("SELECTED").matches());
    }

    @Test
    void identifierUsesJavaLikeAsciiDefaults() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        Result<String> result = tokens.identifier().parse("  _name123 = value");

        assertTrue(result.matches());
        assertEquals("_name123", result.value());
        assertEquals('=', result.input().current());
        assertFalse(tokens.identifier().parse("  123name").matches());
    }

    @Test
    void customIgnoredParserCanSkipWhitespaceAndLineComments() {
        Taker<?> ignored = oneOf(
            Chars.whitespace.as(null),
            string("//").skipThen(Chars.takeUntil(CharPredicate.lineBreak))
                .thenSkip(chr('\n').optional())
                .as(null)
        );
        TokensParser tokens = TokensParser.skipping(ignored);

        Result<String> result = tokens.identifier().then(tokens.identifier())
            .map((left, right) -> left + ":" + right)
            .parse("first // comment\n second");

        assertTrue(result.matches());
        assertEquals("first:second", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void oneOfTokensSupportCaseSensitiveAndCaseInsensitiveSets() {
        TokensParser tokens = TokensParser.skipping(CharPredicate.whitespace);

        assertEquals('+', tokens.oneOf("+-").parse(" + ").value());
        assertFalse(tokens.oneOf("ab").parse(" A").matches());
        assertEquals('A', tokens.oneOfIgnoreCase("ab").parse(" A").value());
    }
}
