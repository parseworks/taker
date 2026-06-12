package io.github.parseworks.taker;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Lexical.take;
import static io.github.parseworks.taker.parsers.Lexical.takeWhile;
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
    public void testIsNot() {
        CharPredicate p = CharPredicate.isNot('a');
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
    public void testNoneOf() {
        CharPredicate p = CharPredicate.noneOf("abc");
        assertFalse(p.test('a'));
        assertFalse(p.test('b'));
        assertFalse(p.test('c'));
        assertTrue(p.test('d'));
    }

    @Test
    public void testNoneOfIgnoreCase() {
        CharPredicate p = CharPredicate.noneOfIgnoreCase("abc");
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
    }

    @Test
    public void testPredicateCombinators() {
        CharPredicate lowerHex = CharPredicate.anyOf(CharPredicate.asciiDigit, CharPredicate.range('a', 'f'));
        assertTrue(lowerHex.test('9'));
        assertTrue(lowerHex.test('c'));
        assertFalse(lowerHex.test('g'));

        CharPredicate lowerAsciiLetter = CharPredicate.allOf(CharPredicate.asciiLetter, CharPredicate.asciiLowerCase);
        assertTrue(lowerAsciiLetter.test('a'));
        assertFalse(lowerAsciiLetter.test('A'));
        assertFalse(CharPredicate.not(CharPredicate.asciiDigit).test('3'));
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
