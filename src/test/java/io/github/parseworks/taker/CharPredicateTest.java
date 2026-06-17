package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Chars;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Chars.take;
import static io.github.parseworks.taker.parsers.Chars.takeWhile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CharPredicateTest {

    @Test
    public void testIs() {
        CharPredicate p = CharPredicate.is('a');
        assertTrue(p.test('a'));
        assertFalse(p.test('b'));
    }

    @Test
    public void testIsIgnoreCase() {
        CharPredicate p = CharPredicate.isIgnoreCase('a');
        assertTrue(p.test('a'));
        assertTrue(p.test('A'));
        assertFalse(p.test('b'));
    }

    @Test
    public void testNotChar() {
        CharPredicate p = CharPredicate.not('a');
        assertFalse(p.test('a'));
        assertTrue(p.test('b'));
    }

    @Test
    public void testRange() {
        CharPredicate p = CharPredicate.range('a', 'c');
        assertTrue(p.test('a'));
        assertTrue(p.test('b'));
        assertTrue(p.test('c'));
        assertFalse(p.test('d'));
        assertFalse(p.test('`'));
    }

    @Test
    public void testAnyOf() {
        CharPredicate p = CharPredicate.anyOf("abc");
        assertTrue(p.test('a'));
        assertTrue(p.test('b'));
        assertTrue(p.test('c'));
        assertFalse(p.test('d'));
    }

    @Test
    public void testAnyOfIgnoreCase() {
        CharPredicate p = CharPredicate.anyOfIgnoreCase("abc");
        assertTrue(p.test('a'));
        assertTrue(p.test('B'));
        assertTrue(p.test('c'));
        assertFalse(p.test('d'));
    }

    @Test
    public void testNotAnyOf() {
        CharPredicate p = CharPredicate.notAnyOf("abc");
        assertFalse(p.test('a'));
        assertFalse(p.test('b'));
        assertFalse(p.test('c'));
        assertTrue(p.test('d'));
    }

    @Test
    public void testDelimiterAliases() {
        CharPredicate notComma = CharPredicate.not(',');
        assertFalse(notComma.test(','));
        assertTrue(notComma.test('a'));
        assertEquals("not ','", notComma.expected());

        CharPredicate notDelimiter = CharPredicate.notAnyOf(",;\n");
        assertFalse(notDelimiter.test(','));
        assertFalse(notDelimiter.test('\n'));
        assertTrue(notDelimiter.test('a'));
        assertEquals("none of ',', ';', '\\n'", notDelimiter.expected());
    }

    @Test
    public void testNotAnyOfIgnoreCase() {
        CharPredicate p = CharPredicate.notAnyOfIgnoreCase("abc");
        assertFalse(p.test('a'));
        assertFalse(p.test('B'));
        assertFalse(p.test('c'));
        assertTrue(p.test('d'));
    }


    @Test
    public void testAnd() {
        CharPredicate p1 = CharPredicate.range('a', 'z');
        CharPredicate p2 = CharPredicate.anyOf("aeiou");
        CharPredicate p = p1.and(p2);
        assertTrue(p.test('a'));
        assertFalse(p.test('b'));
        assertFalse(p.test('A'));
    }

    @Test
    public void testOr() {
        CharPredicate p1 = CharPredicate.digit;
        CharPredicate p2 = CharPredicate.anyOf("abc");
        CharPredicate p = p1.or(p2);
        assertTrue(p.test('0'));
        assertTrue(p.test('a'));
        assertFalse(p.test('d'));
    }

    @Test
    public void testAsciiPredicatesAreExplicitlyAscii() {
        assertTrue(CharPredicate.digit.test('١'));
        assertFalse(CharPredicate.asciiDigit.test('١'));
        assertTrue(CharPredicate.asciiDigit.test('1'));

        assertTrue(CharPredicate.letter.test('λ'));
        assertFalse(CharPredicate.asciiLetter.test('λ'));
        assertTrue(CharPredicate.asciiLetter.test('x'));
        assertTrue(CharPredicate.asciiLetterOrDigit.test('9'));
    }

    @Test
    public void testWhitespacePredicatesSeparateHorizontalAndLineBreaks() {
        assertTrue(CharPredicate.asciiWhitespace.test('\n'));
        assertTrue(CharPredicate.horizontalWhitespace.test('\t'));
        assertFalse(CharPredicate.horizontalWhitespace.test('\n'));
        assertTrue(CharPredicate.lineBreak.test('\n'));
        assertTrue(CharPredicate.lineBreak.test('\r'));
        assertFalse(CharPredicate.lineBreak.test(' '));
        assertFalse(CharPredicate.notLineBreak.test('\n'));
        assertTrue(CharPredicate.notLineBreak.test(' '));
    }

    @Test
    public void testIdentifierPredicatesAreJavaLikeAscii() {
        assertTrue(CharPredicate.identifierStart.test('a'));
        assertTrue(CharPredicate.identifierStart.test('Z'));
        assertTrue(CharPredicate.identifierStart.test('_'));
        assertFalse(CharPredicate.identifierStart.test('1'));
        assertFalse(CharPredicate.identifierStart.test('$'));

        assertTrue(CharPredicate.identifierPart.test('a'));
        assertTrue(CharPredicate.identifierPart.test('9'));
        assertTrue(CharPredicate.identifierPart.test('_'));
        assertFalse(CharPredicate.identifierPart.test('-'));
        assertFalse(CharPredicate.identifierPart.test('$'));
    }

    @Test
    public void testPredicateCombinators() {
        CharPredicate lowerHex = CharPredicate.anyOf(CharPredicate.asciiDigit, CharPredicate.range('a', 'f'));
        assertTrue(lowerHex.test('9'));
        assertTrue(lowerHex.test('c'));
        assertFalse(lowerHex.test('g'));
        assertEquals("ASCII digit or 'a'..'f'", lowerHex.expected());

        CharPredicate lowerAsciiLetter = CharPredicate.allOf(CharPredicate.asciiLetter, CharPredicate.asciiLowerCase);
        assertTrue(lowerAsciiLetter.test('a'));
        assertFalse(lowerAsciiLetter.test('A'));
        assertEquals("ASCII letter and ASCII lowercase letter", lowerAsciiLetter.expected());
        assertFalse(CharPredicate.not(CharPredicate.asciiDigit).test('3'));
    }

    @Test
    public void testExpectedDisplayEscapesCharacters() {
        assertEquals("'\\n'", CharPredicate.is('\n').expected());
        assertEquals("'\\\\'", CharPredicate.is('\\').expected());
        assertEquals("'\\''", CharPredicate.is('\'').expected());
        assertEquals("'\\\"'", CharPredicate.is('"').expected());
        assertEquals("one of '\\t', '\\n', '\\\\', '\\\"', '\\''", CharPredicate.anyOf("\t\n\\\"'").expected());
        assertEquals("one of 'a', 'b', 'c'", CharPredicate.anyOf("abc").expected());
        assertEquals("none of 'a', 'b', 'c'", CharPredicate.notAnyOf("abc").expected());
    }

    @Test
    public void testEmptySetExpectedDisplay() {
        assertFalse(CharPredicate.anyOf("").test('a'));
        assertEquals("no characters", CharPredicate.anyOf("").expected());
        assertTrue(CharPredicate.notAnyOf("").test('a'));
        assertEquals("any character", CharPredicate.notAnyOf("").expected());
    }

    @Test
    public void testNamedPredicatesImproveParserErrors() {
        Result<Character> charResult = take(CharPredicate.asciiDigit).parse("x");
        assertFalse(charResult.matches());
        assertTrue(charResult.error().contains("expected ASCII digit"));

        Result<String> takeWhileResult = takeWhile(CharPredicate.asciiLetter).parse("1");
        assertFalse(takeWhileResult.matches());
        assertTrue(takeWhileResult.error().contains("expected at least one ASCII letter"));
    }

}
