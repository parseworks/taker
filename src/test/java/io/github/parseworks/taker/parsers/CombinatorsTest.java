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
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.github.parseworks.taker.parsers.Combinators.*;
import static org.junit.jupiter.api.Assertions.*;

public class CombinatorsTest {

    @Test
    public void testAny() {
        Taker<Character> parser = any();
        
        // Match case
        Result<Character> result = parser.parse("a");
        assertTrue(result.matches());
        assertEquals('a', result.value());

        // EOF case should fail
        Result<Character> eofResult = parser.parse("");
        assertFalse(eofResult.matches());
    }

    @Test
    public void testThrowError() {
        Taker<Object> parser = throwError(() -> new IOException("Test exception"));
        assertThrows(IOException.class, () -> parser.parse("a"));
    }

    @Test
    public void testEof() {
        Taker<Void> parser = eof();

        // Match case (empty input)
        Result<Void> result = parser.parse("");
        assertTrue(result.matches());

        // Non-empty input should fail
        Result<Void> failResult = parser.parse("a");
        assertFalse(failResult.matches());
    }

    @Test
    public void testFail() {
        Taker<Object> parser = Combinators.fail();

        // Should always fail regardless of input
        assertFalse(parser.parse("a").matches());
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testNot() {
        Taker<Character> aParser = Chars.chr('a');
        Taker<Void> notAParser = not(aParser);

        // Match case (not 'a') succeeds without consuming input
        Result<Void> result = notAParser.parse("b");
        assertTrue(result.matches());
        assertEquals(0, result.input().position());

        // EOF is valid when the forbidden parser fails at EOF
        Result<Void> eofResult = notAParser.parse("");
        assertTrue(eofResult.matches());
        assertEquals(0, eofResult.input().position());

        // NoMatch case ('a' is present)
        Result<Void> failResult = notAParser.parse("a");
        assertFalse(failResult.matches());
    }

    @Test
    public void testIsNot() {
        Taker<Character> parser = isNot('a');

        // Match case (not 'a')
        Result<Character> result = parser.parse("b");
        assertTrue(result.matches());
        assertEquals('b', result.value());

        // NoMatch case ('a')
        assertFalse(parser.parse("a").matches());
        Failure<?> failure = (Failure<?>) parser.parse("a");
        assertEquals("any character except 'a'", failure.expected());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testOneOfList() {
        List<Taker<Character>> parsers = Arrays.asList(
                Chars.chr('a'), Chars.chr('b'), Chars.chr('c')
        );
        Taker<Character> parser = oneOf(parsers);

        // Match cases
        assertTrue(parser.parse("a").matches());
        assertTrue(parser.parse("b").matches());
        assertTrue(parser.parse("c").matches());

        // NoMatch case
        assertFalse(parser.parse("d").matches());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testOneOfVarargs() {
        // Test with 2 parsers
        Taker<Character> parser2 = oneOf(Chars.chr('a'), Chars.chr('b'));
        assertTrue(parser2.parse("a").matches());
        assertTrue(parser2.parse("b").matches());
        assertFalse(parser2.parse("c").matches());

        // Test with 3 parsers
        Taker<Character> parser3 = oneOf(Chars.chr('a'), Chars.chr('b'), Chars.chr('c'));
        assertTrue(parser3.parse("c").matches());
        assertFalse(parser3.parse("d").matches());

        // Additional tests for 4, 5, and 6 parser variants
        Taker<Character> parser4 = oneOf(Chars.chr('a'), Chars.chr('b'), Chars.chr('c'), Chars.chr('d'));
        assertTrue(parser4.parse("d").matches());

        Taker<Character> parser5 = oneOf(Chars.chr('a'), Chars.chr('b'), Chars.chr('c'), Chars.chr('d'), Chars.chr('e'));
        assertTrue(parser5.parse("e").matches());

        Taker<Character> parser6 = oneOf(Chars.chr('a'), Chars.chr('b'), Chars.chr('c'), Chars.chr('d'), Chars.chr('e'), Chars.chr('f'));
        assertTrue(parser6.parse("f").matches());
    }

    @Test
    public void testSequenceList() {
        List<Taker<Character>> parsers = Arrays.asList(
                Chars.chr('a'), Chars.chr('b'), Chars.chr('c')
        );
        Taker<List<Character>> parser = sequence(parsers);

        // Match case
        Result<List<Character>> result = parser.parse("abc");
        assertTrue(result.matches());
        assertEquals(List.of('a', 'b', 'c'), result.value());

        // NoMatch cases
        assertFalse(parser.parse("ab").matches());  // incomplete
        assertFalse(parser.parse("abd").matches()); // wrong sequence
    }

    @Test
    public void testSequenceVarargs() {
        // Test with 2 parsers
        Taker<String> parser2 = sequence(Chars.chr('a'), Chars.chr('b'))
                .map((a, b) -> String.valueOf(a) + b);
        assertTrue(parser2.parse("ab").matches());
        assertEquals("ab", parser2.parse("ab").value());

        // Test with 3 parsers
        Taker<String> parser3 = sequence(Chars.chr('a'), Chars.chr('b'), Chars.chr('c'))
                .map((a, b, c) -> String.valueOf(a) + b + c);
        assertTrue(parser3.parse("abc").matches());
        assertEquals("abc", parser3.parse("abc").value());
    }

    @Test
    public void testSatisfy() {
        CharPredicate isUppercase = Character::isUpperCase;
        Taker<Character> parser = satisfy("uppercase letter", isUppercase);

        // Match case
        assertTrue(parser.parse("A").matches());
        assertEquals('A', parser.parse("A").value());

        // NoMatch case
        assertFalse(parser.parse("a").matches());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testIs() {
        Taker<Character> parser = is('x');

        // Match case
        assertTrue(parser.parse("x").matches());
        assertEquals('x', parser.parse("x").value());

        // NoMatch case
        Result<Character> mismatch = parser.parse("y");
        assertFalse(mismatch.matches());
        assertEquals("'x'", ((Failure<?>) mismatch).expected());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testCombinedNotIsNot() {
        // Test combining not and isNot
        Taker<Void> notDigit = not(Chars.chr(Character::isDigit));
        Taker<Character> letter = Chars.chr(Character::isLetter);

        // Taker that accepts a letter that's followed by a non-digit
        Taker<Character> letterFollowedByNonDigit = letter.peek(notDigit);

        var firstResult = letterFollowedByNonDigit.parse("a");
        var secondResult = letterFollowedByNonDigit.parse("a1");
        var thirdResult = letterFollowedByNonDigit.parse("aX");

        assertTrue(firstResult.matches());
        assertFalse(secondResult.matches());
        assertTrue(thirdResult.matches());
    }
}
